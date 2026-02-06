package com.barter.core.data

import com.barter.core.domain.model.*
import com.barter.core.domain.repo.BarterRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

/**
 * Repository that talks to the Ktor server via REST.
 * Switch from FakeBarterRepository in AppDI to use this.
 *
 * Usage:
 *   single<BarterRepository> { KtorBarterRepository(get()) }
 */
class KtorBarterRepository(
    private val client: HttpClient,
    private val baseUrl: String = "http://192.168.0.166",
) : BarterRepository {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow(
        UserProfile("me", "...", "", 0.0)
    )
    override val currentUser: Flow<UserProfile> = _currentUser.asStateFlow()

    private val messageCache = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val dealCache = mutableMapOf<String, MutableStateFlow<List<Deal>>>()

    // ── Auth (TODO: wire to server) ──────────────────────────
    override suspend fun login(email: String, password: String): Result<UserProfile> {
        TODO("Wire to server POST /api/auth/login")
    }

    override suspend fun register(request: RegistrationRequest): Result<UserProfile> {
        return try {
            val response = client.post("$baseUrl/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Registration failed: ${response.status}"))
            }
            @Serializable data class AuthResponse(val user: UserProfile)
            val authResponse: AuthResponse = response.body()
            val user = authResponse.user
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        TODO("Wire to server POST /api/auth/logout")
    }

    // ── Interests (TODO: wire to server) ─────────────────────
    override suspend fun updateInterests(interests: List<String>) {
        TODO("Wire to server PUT /api/user/interests")
    }

    // ── Listings ─────────────────────────────────────────────
    override suspend fun createListing(
        title: String, description: String, kind: ListingKind,
        tags: List<String>, estimatedValue: Double?,
        imageUrl: String, validUntilMs: Long?,
    ): Listing {
        TODO("Wire to server POST /api/listings")
    }

    override suspend fun updateListing(
        listingId: String, title: String, description: String,
        kind: ListingKind, tags: List<String>, estimatedValue: Double?,
        imageUrl: String, validUntilMs: Long?,
    ): Listing {
        TODO("Wire to server PUT /api/listings/$listingId")
    }

    override suspend fun deleteListing(listingId: String) {
        TODO("Wire to server DELETE /api/listings/$listingId")
    }

    override suspend fun getListingById(listingId: String): Listing? {
        TODO("Wire to server GET /api/listings/$listingId")
    }

    override suspend fun getMyListings(): List<Listing> {
        TODO("Wire to server GET /api/listings/mine")
    }

    override suspend fun toggleListingVisibility(listingId: String): Listing {
        TODO("Wire to server PATCH /api/listings/$listingId/visibility")
    }

    override suspend fun renewListing(listingId: String, newValidUntilMs: Long): Listing {
        TODO("Wire to server PATCH /api/listings/$listingId/renew")
    }

    override suspend fun topUpBalance(amount: Double): UserProfile {
        TODO("Wire to server POST /api/user/balance/topup")
    }

    override suspend fun getBalance(): Double {
        TODO("Wire to server GET /api/user/balance")
    }

    override suspend fun updateAvailability(listingId: String, availability: AvailabilityStatus): Listing {
        TODO("Wire to server PATCH /api/listings/$listingId/availability")
    }

    override suspend fun searchListings(query: String, category: String?, sortBy: SortOption): List<Listing> {
        TODO("Wire to server GET /api/listings/search")
    }

    override suspend fun getDiscovery(interestFilter: List<String>): List<Listing> =
        client.get("$baseUrl/api/discovery").body()

    override suspend fun swipe(listingId: String, action: SwipeAction): Match? {
        @Serializable data class SwipeReq(val action: String)
        @Serializable data class SwipeResp(val match: Match?)

        val resp: SwipeResp = client.post("$baseUrl/api/swipe/$listingId") {
            contentType(ContentType.Application.Json)
            setBody(SwipeReq(action.name))
        }.body()

        return resp.match
    }

    // ── Matches ─────────────────────────────────────────────
    override suspend fun getMatches(): List<Match> =
        client.get("$baseUrl/api/matches").body()

    override suspend fun getEnrichedMatches(): List<EnrichedMatch> {
        TODO("Wire to server GET /api/matches/enriched")
    }

    // ── Chat ────────────────────────────────────────────────
    override fun observeMessages(matchId: String): Flow<List<Message>> {
        val flow = messageCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        return flow.asStateFlow()
    }

    override suspend fun sendMessage(matchId: String, text: String) {
        @Serializable data class Req(val text: String)

        val msg: Message = client.post("$baseUrl/api/matches/$matchId/messages") {
            contentType(ContentType.Application.Json)
            setBody(Req(text))
        }.body()

        val flow = messageCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        flow.update { it + msg }
    }

    // ── Deals ───────────────────────────────────────────────
    override fun observeDeals(matchId: String): Flow<List<Deal>> {
        val flow = dealCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        return flow.asStateFlow()
    }

    override suspend fun proposeDeal(
        matchId: String, offer: List<DealItem>, request: List<DealItem>,
        cashTopUp: Double, note: String,
    ) {
        @Serializable data class Req(
            val offer: List<DealItem>, val request: List<DealItem>,
            val cashTopUp: Double, val note: String,
        )

        val deal: Deal = client.post("$baseUrl/api/matches/$matchId/deals") {
            contentType(ContentType.Application.Json)
            setBody(Req(offer, request, cashTopUp, note))
        }.body()

        val flow = dealCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        flow.update { it + deal }
    }

    override suspend fun updateDealStatus(dealId: String, status: DealStatus) {
        @Serializable data class Req(val status: String)

        client.patch("$baseUrl/api/deals/$dealId") {
            contentType(ContentType.Application.Json)
            setBody(Req(status.name))
        }

        dealCache.values.forEach { flow ->
            flow.update { list ->
                list.map { if (it.id == dealId) it.copy(status = status) else it }
            }
        }
    }

    // ── Stats & badges ──────────────────────────────────────
    override suspend fun getProfileStats(): ProfileStats {
        TODO("Wire to server GET /api/user/stats")
    }

    override suspend fun getUnreadMatchCount(): Int {
        TODO("Wire to server GET /api/badges/matches")
    }

    override suspend fun getTotalUnreadMessageCount(): Int {
        TODO("Wire to server GET /api/badges/messages")
    }

    // ── Notifications ──────────────────────────────────────
    override suspend fun getNotifications(): List<Notification> {
        TODO("Wire to server GET /api/notifications")
    }

    override suspend fun getUnreadNotificationCount(): Int {
        TODO("Wire to server GET /api/notifications/unread-count")
    }

    override suspend fun markNotificationRead(notificationId: String) {
        TODO("Wire to server PATCH /api/notifications/$notificationId/read")
    }
}
