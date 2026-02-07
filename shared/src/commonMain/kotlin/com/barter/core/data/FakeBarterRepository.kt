package com.barter.core.data

import com.barter.core.domain.model.*
import com.barter.core.domain.repo.BarterRepository
import com.barter.core.util.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.random.Random

class FakeBarterRepository : BarterRepository {

    private data class UserRecord(val email: String, val password: String, val profile: UserProfile)

    private val userDb = mutableMapOf<String, UserRecord>()
    private var loggedInUserId: String? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow(UserProfile("", "Guest", "", 0.0))
    override val currentUser: Flow<UserProfile> = _currentUser.asStateFlow()

    private val listings: MutableList<Listing> = mutableListOf()
    private val matchesFlow = MutableStateFlow<List<Match>>(emptyList())
    private val messagesByMatch = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val dealsByMatch = mutableMapOf<String, MutableStateFlow<List<Deal>>>()
    private val notifications = mutableListOf<Notification>()

    init {
        val testUser = UserProfile(
            id = "me", displayName = "Ion", location = "Chi\u0219in\u0103u",
            rating = 4.8, email = "ion@barter.md",
            interests = emptyList(), hasSelectedInterests = false,
            balance = 50.0,
        )
        userDb["ion@barter.md"] = UserRecord("ion@barter.md", "1234", testUser)
        listings.addAll(buildFakeListings())

        // Seed notifications
        val now = currentTimeMillis()
        notifications.addAll(listOf(
            Notification(
                id = "n1", recipientUserId = "me",
                type = NotificationType.LIKE_RECEIVED,
                title = "New Like!", body = "Alina liked your listing",
                relatedListingId = "l2", timestampMs = now - 3600000,
            ),
            Notification(
                id = "n2", recipientUserId = "me",
                type = NotificationType.MATCH_CREATED,
                title = "It's a Match!", body = "You matched with Sergiu",
                relatedMatchId = "match_seed", timestampMs = now - 1800000,
            ),
            Notification(
                id = "n3", recipientUserId = "me",
                type = NotificationType.LIKE_RECEIVED,
                title = "New Like!", body = "Ana liked your listing",
                relatedListingId = "l4", timestampMs = now - 600000,
            ),
        ))
    }

    private fun me(): UserProfile = _currentUser.value

    // ── Auth ─────────────────────────────────────────────────
    override suspend fun login(email: String, password: String): Result<UserProfile> {
        val record = userDb[email] ?: return Result.failure(Exception("No account found with this email"))
        if (record.password != password) return Result.failure(Exception("Invalid password"))
        loggedInUserId = record.profile.id
        _currentUser.value = record.profile
        _authState.value = AuthState.Authenticated(record.profile)
        return Result.success(record.profile)
    }

    override suspend fun register(request: RegistrationRequest): Result<UserProfile> {
        if (userDb.containsKey(request.email)) return Result.failure(Exception("An account with this email already exists"))
        val userId = "u_${currentTimeMillis()}"
        val profile = UserProfile(
            id = userId, displayName = request.displayName, location = request.location,
            rating = 5.0, email = request.email, interests = emptyList(), hasSelectedInterests = false,
        )
        userDb[request.email] = UserRecord(request.email, request.password, profile)
        loggedInUserId = userId
        _currentUser.value = profile
        _authState.value = AuthState.Authenticated(profile)
        return Result.success(profile)
    }

    override suspend fun logout() {
        loggedInUserId = null
        _currentUser.value = UserProfile("", "Guest", "", 0.0)
        _authState.value = AuthState.Unauthenticated
    }

    // ── Interests ────────────────────────────────────────────
    override suspend fun updateInterests(interests: List<String>) {
        val uid = loggedInUserId ?: return
        val updated = _currentUser.value.copy(interests = interests, hasSelectedInterests = true)
        _currentUser.value = updated
        _authState.value = AuthState.Authenticated(updated)
        val email = userDb.values.firstOrNull { it.profile.id == uid }?.email ?: return
        userDb[email] = userDb[email]!!.copy(profile = updated)
    }

    // ── Listings ─────────────────────────────────────────────
    override suspend fun createListing(
        title: String, description: String, kind: ListingKind,
        tags: List<String>, estimatedValue: Double?,
        imageUrl: String, validUntilMs: Long?,
    ): Listing {
        val now = currentTimeMillis()
        val listing = Listing(
            id = "l_${now}", owner = me(), kind = kind,
            title = title, description = description, tags = tags,
            estimatedValue = estimatedValue, createdAtMs = now,
            imageUrl = imageUrl,
            validUntilMs = now + THIRTY_DAYS_MS, // Platform-imposed 30-day validity
        )
        listings.add(listing)
        return listing
    }

    override suspend fun updateListing(
        listingId: String, title: String, description: String,
        kind: ListingKind, tags: List<String>, estimatedValue: Double?,
        imageUrl: String, validUntilMs: Long?,
    ): Listing {
        val index = listings.indexOfFirst { it.id == listingId }
        if (index == -1) throw Exception("Listing not found")
        val old = listings[index]
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        if (old.owner.id != uid) throw Exception("Not your listing")
        val updated = old.copy(
            title = title, description = description, kind = kind,
            tags = tags, estimatedValue = estimatedValue,
            imageUrl = imageUrl, validUntilMs = validUntilMs,
        )
        listings[index] = updated
        return updated
    }

    override suspend fun toggleListingVisibility(listingId: String): Listing {
        val index = listings.indexOfFirst { it.id == listingId }
        if (index == -1) throw Exception("Listing not found")
        val old = listings[index]
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        if (old.owner.id != uid) throw Exception("Not your listing")
        val updated = old.copy(isHidden = !old.isHidden)
        listings[index] = updated
        return updated
    }

    override suspend fun renewListing(listingId: String, newValidUntilMs: Long): Listing {
        val index = listings.indexOfFirst { it.id == listingId }
        if (index == -1) throw Exception("Listing not found")
        val old = listings[index]
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        if (old.owner.id != uid) throw Exception("Not your listing")

        // Charge renewal cost
        val currentBalance = _currentUser.value.balance
        if (currentBalance < RENEWAL_COST_MDL) {
            throw Exception("Insufficient balance. You need ${RENEWAL_COST_MDL.toInt()} MDL to renew. Current balance: ${currentBalance.toInt()} MDL")
        }
        val updatedUser = _currentUser.value.copy(balance = currentBalance - RENEWAL_COST_MDL)
        _currentUser.value = updatedUser
        _authState.value = AuthState.Authenticated(updatedUser)
        val email = userDb.values.firstOrNull { it.profile.id == uid }?.email
        if (email != null) userDb[email] = userDb[email]!!.copy(profile = updatedUser)

        val updated = old.copy(validUntilMs = newValidUntilMs)
        listings[index] = updated
        return updated
    }

    // ── Balance / credits ───────────────────────────────────
    override suspend fun topUpBalance(amount: Double): UserProfile {
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        val updatedUser = _currentUser.value.copy(balance = _currentUser.value.balance + amount)
        _currentUser.value = updatedUser
        _authState.value = AuthState.Authenticated(updatedUser)
        val email = userDb.values.firstOrNull { it.profile.id == uid }?.email
        if (email != null) userDb[email] = userDb[email]!!.copy(profile = updatedUser)
        return updatedUser
    }

    override suspend fun getBalance(): Double {
        return _currentUser.value.balance
    }

    override suspend fun deleteListing(listingId: String) {
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        val listing = listings.firstOrNull { it.id == listingId } ?: throw Exception("Listing not found")
        if (listing.owner.id != uid) throw Exception("Not your listing")
        listings.removeAll { it.id == listingId }
    }

    override suspend fun getListingById(listingId: String): Listing? =
        listings.firstOrNull { it.id == listingId }

    override suspend fun getMyListings(): List<Listing> {
        val uid = loggedInUserId ?: return emptyList()
        return listings.filter { it.owner.id == uid }
    }

    // ── Availability ──────────────────────────────────────────
    override suspend fun updateAvailability(listingId: String, availability: AvailabilityStatus): Listing {
        val index = listings.indexOfFirst { it.id == listingId }
        if (index == -1) throw Exception("Listing not found")
        val old = listings[index]
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        if (old.owner.id != uid) throw Exception("Not your listing")
        val updated = old.copy(availability = availability)
        listings[index] = updated
        return updated
    }

    // ── Search / Browse ───────────────────────────────────────
    override suspend fun searchListings(query: String, category: String?, sortBy: SortOption): List<Listing> {
        val uid = loggedInUserId
        var result = listings.filter {
            !it.isHidden && !it.isExpired && it.availability != AvailabilityStatus.SOLD
        }
        if (uid != null) {
            result = result.filter { it.owner.id != uid }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            result = result.filter {
                it.title.lowercase().contains(q) || it.description.lowercase().contains(q)
            }
        }
        if (category != null) {
            result = result.filter { listing -> listing.tags.any { it == category } }
        }
        return when (sortBy) {
            SortOption.NEWEST -> result.sortedByDescending { it.createdAtMs }
            SortOption.PRICE_LOW_HIGH -> result.sortedBy { it.estimatedValue ?: Double.MAX_VALUE }
            SortOption.PRICE_HIGH_LOW -> result.sortedByDescending { it.estimatedValue ?: 0.0 }
        }
    }

    // ── Discovery ────────────────────────────────────────────
    override suspend fun getDiscovery(interestFilter: List<String>): List<Listing> {
        val uid = loggedInUserId
        val others = if (uid != null) listings.filter { it.owner.id != uid } else listings.toList()
        val visible = others.filter { !it.isHidden && !it.isExpired && it.availability != AvailabilityStatus.SOLD }
        if (interestFilter.isEmpty()) return visible
        val filterLower = interestFilter.map { it.lowercase() }
        return visible.sortedByDescending { listing ->
            listing.tags.count { tag -> tag.lowercase() in filterLower }
        }
    }

    // ── Swipe ────────────────────────────────────────────────
    override suspend fun swipe(listingId: String, action: SwipeAction): Match? {
        if (action == SwipeAction.PASS) return null
        val listing = listings.firstOrNull { it.id == listingId } ?: return null
        val now = currentTimeMillis()

        // Always create "liked your listing" notification for the owner
        notifications.add(
            Notification(
                id = "n_${now}", recipientUserId = listing.owner.id,
                type = NotificationType.LIKE_RECEIVED,
                title = "New Like!",
                body = "${me().displayName} liked your listing \"${listing.title}\"",
                relatedListingId = listingId, timestampMs = now,
            )
        )

        val isMutual = Random.nextBoolean()
        if (!isMutual) return null

        val match = Match(
            id = "match_${listing.owner.id}_$now",
            userA = me(), userB = listing.owner, createdAtMs = now,
        )
        matchesFlow.update { it + match }
        messagesByMatch.getOrPut(match.id) { MutableStateFlow(emptyList()) }
        dealsByMatch.getOrPut(match.id) { MutableStateFlow(emptyList()) }
        val hello = Message(
            id = "m_$now", matchId = match.id,
            fromUserId = listing.owner.id,
            text = "Salut! Am v\u0103zut like-ul t\u0103u. Vrei s\u0103 discut\u0103m schimbul?",
            timestampMs = now,
        )
        messagesByMatch[match.id]!!.update { it + hello }

        // Match notification for both users
        notifications.add(
            Notification(
                id = "n_match_$now", recipientUserId = listing.owner.id,
                type = NotificationType.MATCH_CREATED,
                title = "It's a Match!",
                body = "You matched with ${me().displayName}!",
                relatedMatchId = match.id, timestampMs = now,
            )
        )
        notifications.add(
            Notification(
                id = "n_match_me_$now", recipientUserId = me().id,
                type = NotificationType.MATCH_CREATED,
                title = "It's a Match!",
                body = "You matched with ${listing.owner.displayName}!",
                relatedMatchId = match.id, timestampMs = now,
            )
        )

        return match
    }

    // ── Matches ──────────────────────────────────────────────
    override suspend fun getMatches(): List<Match> = matchesFlow.value

    override suspend fun getEnrichedMatches(): List<EnrichedMatch> {
        return matchesFlow.value.map { match ->
            val messages = messagesByMatch[match.id]?.value ?: emptyList()
            EnrichedMatch(match = match, lastMessage = messages.lastOrNull(), unreadCount = messages.size)
        }
    }

    // ── Chat ─────────────────────────────────────────────────
    override fun observeMessages(matchId: String): Flow<List<Message>> =
        messagesByMatch.getOrPut(matchId) { MutableStateFlow(emptyList()) }.asStateFlow()

    override suspend fun sendMessage(matchId: String, text: String) {
        val flow = messagesByMatch.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        val msg = Message(
            id = "m_${currentTimeMillis()}", matchId = matchId,
            fromUserId = me().id, text = text, timestampMs = currentTimeMillis(),
        )
        flow.update { it + msg }
    }

    // ── Deals ────────────────────────────────────────────────
    override fun observeDeals(matchId: String): Flow<List<Deal>> =
        dealsByMatch.getOrPut(matchId) { MutableStateFlow(emptyList()) }.asStateFlow()

    override suspend fun proposeDeal(
        matchId: String, offer: List<DealItem>, request: List<DealItem>,
        cashTopUp: Double, note: String,
    ) {
        val flow = dealsByMatch.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        val deal = Deal(
            id = "d_${currentTimeMillis()}", matchId = matchId,
            proposerUserId = me().id, offer = offer, request = request,
            status = DealStatus.PROPOSED, createdAtMs = currentTimeMillis(),
            cashTopUp = cashTopUp, note = note,
        )
        flow.update { it + deal }
    }

    override suspend fun updateDealStatus(dealId: String, status: DealStatus) {
        val entry = dealsByMatch.entries.firstOrNull { (_, flow) ->
            flow.value.any { it.id == dealId }
        } ?: return
        entry.value.update { list ->
            list.map { if (it.id == dealId) it.copy(status = status) else it }
        }
    }

    // ── Stats & badges ───────────────────────────────────────
    override suspend fun getProfileStats(): ProfileStats {
        val uid = loggedInUserId ?: return ProfileStats()
        return ProfileStats(
            activeListingsCount = listings.count { it.owner.id == uid },
            completedDealsCount = dealsByMatch.values.sumOf { flow ->
                flow.value.count { it.status == DealStatus.COMPLETED }
            },
            matchesCount = matchesFlow.value.size,
        )
    }

    override suspend fun getUnreadMatchCount(): Int = matchesFlow.value.size
    override suspend fun getTotalUnreadMessageCount(): Int =
        messagesByMatch.values.sumOf { it.value.size }

    // ── Notifications ───────────────────────────────────────
    override suspend fun getNotifications(): List<Notification> =
        notifications.sortedByDescending { it.timestampMs }

    override suspend fun getUnreadNotificationCount(): Int =
        notifications.count { !it.isRead }

    override suspend fun markNotificationRead(notificationId: String) {
        val index = notifications.indexOfFirst { it.id == notificationId }
        if (index != -1) {
            notifications[index] = notifications[index].copy(isRead = true)
        }
    }

    override suspend fun autocompleteLocation(query: String): List<GeocodeSuggestion> = emptyList()

    // ── Reviews ─────────────────────────────────────────────

    private val reviews = mutableListOf<Review>()

    override suspend fun submitReview(dealId: String, rating: Int, comment: String): Review {
        val uid = loggedInUserId ?: throw Exception("Not logged in")
        val review = Review(
            id = "review_${reviews.size + 1}",
            dealId = dealId,
            reviewerUserId = uid,
            reviewedUserId = "other",
            rating = rating,
            comment = comment,
            timestampMs = currentTimeMillis(),
            reviewerName = "You",
        )
        reviews.add(review)
        return review
    }

    override suspend fun getReviewsForUser(userId: String): List<Review> =
        reviews.filter { it.reviewedUserId == userId }

    // ── Fake data ────────────────────────────────────────────
    private fun buildFakeListings(): MutableList<Listing> {
        val u1 = UserProfile("u1", "Mihai", "B\u0103l\u021Bi", 4.6)
        val u2 = UserProfile("u2", "Alina", "Chi\u0219in\u0103u", 4.9)
        val u3 = UserProfile("u3", "Sergiu", "Orhei", 4.4)
        val u4 = UserProfile("u4", "Ana", "Chi\u0219in\u0103u", 4.7)
        val u5 = UserProfile("u5", "Vlad", "Tiraspol", 4.3)
        val u6 = UserProfile("u6", "Elena", "Chi\u0219in\u0103u", 4.8)
        val u7 = UserProfile("u7", "Andrei", "B\u0103l\u021Bi", 4.5)
        val u8 = UserProfile("u8", "Maria", "Comrat", 4.9)
        val now = currentTimeMillis()

        return mutableListOf(
            Listing("l1", u1, ListingKind.GOODS, "Schimb biciclet\u0103",
                "Ofer biciclet\u0103 mountain bike, caut Apple Watch sau servicii IT.",
                tags = listOf("sport", "tech"), estimatedValue = 150.0, createdAtMs = now - 86400000,
                imageUrl = "https://picsum.photos/seed/bicycle/400/300", validUntilMs = now + THIRTY_DAYS_MS),
            Listing("l2", u2, ListingKind.SERVICES, "Machiaj \u0219i manicur\u0103",
                "Ofer servicii de beauty la domiciliu. Caut AirPods sau alte gadget-uri.",
                tags = listOf("beauty", "home"), estimatedValue = 80.0, createdAtMs = now - 72000000,
                imageUrl = "https://picsum.photos/seed/beauty/400/300", validUntilMs = now + THIRTY_DAYS_MS),
            Listing("l3", u3, ListingKind.BOTH, "Laptop + repara\u021Bii",
                "Ofer laptop vechi + ajutor tehnic. Caut telefon sau servicii auto.",
                tags = listOf("tech", "auto"), estimatedValue = 200.0, createdAtMs = now - 60000000,
                imageUrl = "https://picsum.photos/seed/laptop/400/300", validUntilMs = now + 15 * 24 * 60 * 60 * 1000,
                availability = AvailabilityStatus.RESERVED),
            Listing("l4", u4, ListingKind.SERVICES, "Lec\u021Bii de chitar\u0103",
                "Predau chitar\u0103 acustic\u0103 \u0219i electric\u0103. Caut echipament foto sau design grafic.",
                tags = listOf("music", "art"), estimatedValue = 50.0, createdAtMs = now - 48000000,
                imageUrl = "https://picsum.photos/seed/guitar/400/300", validUntilMs = now + THIRTY_DAYS_MS),
            Listing("l5", u5, ListingKind.GOODS, "Geac\u0103 de iarn\u0103",
                "Geac\u0103 North Face, m\u0103rimea L, stare excelent\u0103. Caut c\u0103r\u021Bi sau board games.",
                tags = listOf("fashion", "books"), estimatedValue = 120.0, createdAtMs = now - 36000000,
                imageUrl = "https://picsum.photos/seed/jacket/400/300", validUntilMs = now + THIRTY_DAYS_MS),
            Listing("l6", u6, ListingKind.SERVICES, "Sesiune foto",
                "Ofer sesiune foto profesional\u0103. Caut cursuri de programare sau design web.",
                tags = listOf("photo", "creative"), estimatedValue = 100.0, createdAtMs = now - 24000000,
                imageUrl = "https://picsum.photos/seed/camera/400/300", validUntilMs = now + THIRTY_DAYS_MS),
            Listing("l7", u7, ListingKind.GOODS, "Colec\u021Bie board games",
                "Ofer Catan, Ticket to Ride, Codenames. Caut echipament sport.",
                tags = listOf("games", "sport"), estimatedValue = 60.0, createdAtMs = now - 12000000,
                imageUrl = "https://picsum.photos/seed/boardgames/400/300", validUntilMs = now + THIRTY_DAYS_MS,
                availability = AvailabilityStatus.RESERVED),
            Listing("l8", u8, ListingKind.SERVICES, "Web design & branding",
                "Creez site-uri \u0219i identit\u0103\u021Bi vizuale. Caut servicii de traducere sau catering.",
                tags = listOf("design", "web"), estimatedValue = 250.0, createdAtMs = now - 6000000,
                imageUrl = "https://picsum.photos/seed/webdesign/400/300", validUntilMs = now + THIRTY_DAYS_MS),
        )
    }
}
