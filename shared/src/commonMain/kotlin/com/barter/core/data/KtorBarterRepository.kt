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

class KtorBarterRepository(
    private val client: HttpClient,
    private val baseUrl: String = "http://192.168.0.166",
) : BarterRepository {

    private var authenticatedUserId: String? = null

    private fun HttpRequestBuilder.withUserId() {
        authenticatedUserId?.let { header("X-User-Id", it) }
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    override val authState: Flow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow(
        UserProfile("me", "...", "", 0.0)
    )
    override val currentUser: Flow<UserProfile> = _currentUser.asStateFlow()

    private val messageCache = mutableMapOf<String, MutableStateFlow<List<Message>>>()
    private val dealCache = mutableMapOf<String, MutableStateFlow<List<Deal>>>()

    // ── Auth ──────────────────────────────────────────────────

    override suspend fun login(email: String, password: String): Result<UserProfile> {
        return try {
            @Serializable data class LoginReq(val email: String, val password: String)
            @Serializable data class AuthResp(val user: UserProfile)

            val response = client.post("$baseUrl/api/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginReq(email, password))
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Login failed: ${response.status}"))
            }
            val user = response.body<AuthResp>().user
            authenticatedUserId = user.id
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun register(request: RegistrationRequest): Result<UserProfile> {
        return try {
            @Serializable data class AuthResp(val user: UserProfile)

            val response = client.post("$baseUrl/api/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(Exception("Registration failed: ${response.status}"))
            }
            val user = response.body<AuthResp>().user
            authenticatedUserId = user.id
            _currentUser.value = user
            _authState.value = AuthState.Authenticated(user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        client.post("$baseUrl/api/auth/logout")
        authenticatedUserId = null
        _authState.value = AuthState.Unauthenticated
        _currentUser.value = UserProfile("me", "...", "", 0.0)
    }

    // ── Interests ─────────────────────────────────────────────

    override suspend fun updateInterests(interests: List<String>) {
        @Serializable data class Req(val interests: List<String>)

        val user: UserProfile = client.put("$baseUrl/api/user/interests") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(interests))
        }.body()
        _currentUser.value = user
        val auth = _authState.value
        if (auth is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(user)
        }
    }

    // ── Listings ──────────────────────────────────────────────

    override suspend fun createListing(
        title: String, description: String, kind: ListingKind,
        tags: List<String>, estimatedValue: Double?,
        imageUrl: String, validUntilMs: Long?,
    ): Listing {
        @Serializable data class Req(
            val title: String, val description: String, val kind: String,
            val tags: List<String>, val estimatedValue: Double?,
            val imageUrl: String, val validUntilMs: Long?,
        )

        return client.post("$baseUrl/api/listings") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(title, description, kind.name, tags, estimatedValue, imageUrl, validUntilMs))
        }.body()
    }

    override suspend fun updateListing(
        listingId: String, title: String, description: String,
        kind: ListingKind, tags: List<String>, estimatedValue: Double?,
        imageUrl: String, validUntilMs: Long?,
    ): Listing {
        @Serializable data class Req(
            val title: String, val description: String, val kind: String,
            val tags: List<String>, val estimatedValue: Double?,
            val imageUrl: String, val validUntilMs: Long?,
        )

        return client.put("$baseUrl/api/listings/$listingId") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(title, description, kind.name, tags, estimatedValue, imageUrl, validUntilMs))
        }.body()
    }

    override suspend fun deleteListing(listingId: String) {
        client.delete("$baseUrl/api/listings/$listingId") { withUserId() }
    }

    override suspend fun getListingById(listingId: String): Listing? {
        val response = client.get("$baseUrl/api/listings/$listingId") { withUserId() }
        return if (response.status.isSuccess()) response.body() else null
    }

    override suspend fun getMyListings(): List<Listing> =
        client.get("$baseUrl/api/listings/mine") { withUserId() }.body()

    override suspend fun toggleListingVisibility(listingId: String): Listing =
        client.patch("$baseUrl/api/listings/$listingId/visibility") { withUserId() }.body()

    override suspend fun renewListing(listingId: String, newValidUntilMs: Long): Listing {
        @Serializable data class Req(val newValidUntilMs: Long)

        return client.patch("$baseUrl/api/listings/$listingId/renew") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(newValidUntilMs))
        }.body()
    }

    override suspend fun topUpBalance(amount: Double): UserProfile {
        @Serializable data class Req(val amount: Double)

        val user: UserProfile = client.post("$baseUrl/api/user/balance/topup") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(amount))
        }.body()
        _currentUser.value = user
        return user
    }

    override suspend fun getBalance(): Double {
        @Serializable data class Resp(val balance: Double)

        return client.get("$baseUrl/api/user/balance") { withUserId() }.body<Resp>().balance
    }

    override suspend fun updateAvailability(listingId: String, availability: AvailabilityStatus): Listing {
        @Serializable data class Req(val availability: String)

        return client.patch("$baseUrl/api/listings/$listingId/availability") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(availability.name))
        }.body()
    }

    override suspend fun searchListings(query: String, category: String?, sortBy: SortOption): List<Listing> =
        client.get("$baseUrl/api/listings/search") {
            withUserId()
            parameter("q", query)
            if (category != null) parameter("category", category)
            parameter("sort", sortBy.name)
        }.body()

    // ── Discovery & Swipe ─────────────────────────────────────

    override suspend fun getDiscovery(interestFilter: List<String>): List<Listing> =
        client.get("$baseUrl/api/discovery") {
            withUserId()
            val user = _currentUser.value
            user.latitude?.let { parameter("lat", it) }
            user.longitude?.let { parameter("lng", it) }
        }.body()

    override suspend fun swipe(listingId: String, action: SwipeAction): Match? {
        @Serializable data class SwipeReq(val action: String)
        @Serializable data class SwipeResp(val match: Match?)

        return client.post("$baseUrl/api/swipe/$listingId") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(SwipeReq(action.name))
        }.body<SwipeResp>().match
    }

    // ── Matches ───────────────────────────────────────────────

    override suspend fun getMatches(): List<Match> =
        client.get("$baseUrl/api/matches") { withUserId() }.body()

    override suspend fun getEnrichedMatches(): List<EnrichedMatch> =
        client.get("$baseUrl/api/matches/enriched") { withUserId() }.body()

    // ── Chat ──────────────────────────────────────────────────

    override fun observeMessages(matchId: String): Flow<List<Message>> {
        val flow = messageCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        return flow.asStateFlow()
    }

    override suspend fun sendMessage(matchId: String, text: String) {
        @Serializable data class Req(val text: String)

        val msg: Message = client.post("$baseUrl/api/matches/$matchId/messages") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(text))
        }.body()

        val flow = messageCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        flow.update { it + msg }
    }

    // ── Deals ─────────────────────────────────────────────────

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
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(offer, request, cashTopUp, note))
        }.body()

        val flow = dealCache.getOrPut(matchId) { MutableStateFlow(emptyList()) }
        flow.update { it + deal }
    }

    override suspend fun updateDealStatus(dealId: String, status: DealStatus) {
        @Serializable data class Req(val status: String)

        client.patch("$baseUrl/api/deals/$dealId") {
            withUserId()
            contentType(ContentType.Application.Json)
            setBody(Req(status.name))
        }

        dealCache.values.forEach { flow ->
            flow.update { list ->
                list.map { if (it.id == dealId) it.copy(status = status) else it }
            }
        }
    }

    // ── Stats & badges ────────────────────────────────────────

    override suspend fun getProfileStats(): ProfileStats =
        client.get("$baseUrl/api/user/stats") { withUserId() }.body()

    override suspend fun getUnreadMatchCount(): Int {
        @Serializable data class Resp(val count: Int)
        return client.get("$baseUrl/api/badges/matches") { withUserId() }.body<Resp>().count
    }

    override suspend fun getTotalUnreadMessageCount(): Int {
        @Serializable data class Resp(val count: Int)
        return client.get("$baseUrl/api/badges/messages") { withUserId() }.body<Resp>().count
    }

    // ── Notifications ─────────────────────────────────────────

    override suspend fun getNotifications(): List<Notification> =
        client.get("$baseUrl/api/notifications") { withUserId() }.body()

    override suspend fun getUnreadNotificationCount(): Int {
        @Serializable data class Resp(val count: Int)
        return client.get("$baseUrl/api/notifications/unread-count") { withUserId() }.body<Resp>().count
    }

    override suspend fun markNotificationRead(notificationId: String) {
        client.patch("$baseUrl/api/notifications/$notificationId/read") { withUserId() }
    }
}
