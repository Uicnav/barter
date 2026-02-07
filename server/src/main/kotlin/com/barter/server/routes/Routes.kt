package com.barter.server.routes

import com.barter.server.db.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

// ── DTOs (mirror the client Models.kt) ────────────────────

@Serializable
data class UserProfileDto(
    val id: String,
    val displayName: String,
    val location: String,
    val rating: Double = 0.0,
    val avatarUrl: String = "",
    val email: String = "",
    val interests: List<String> = emptyList(),
    val hasSelectedInterests: Boolean = false,
    val balance: Double = 0.0,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class RegisterRequest(
    val displayName: String,
    val location: String,
    val email: String,
    val password: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class AuthResponse(
    val user: UserProfileDto,
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class ListingDto(
    val id: String,
    val owner: UserProfileDto,
    val kind: String,
    val title: String,
    val description: String,
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val estimatedValue: Double? = null,
    val createdAtMs: Long = 0L,
    val validUntilMs: Long? = null,
    val isHidden: Boolean = false,
    val availability: String = "AVAILABLE",
)

@Serializable
data class SwipeRequest(val action: String) // "LIKE" or "PASS"

@Serializable
data class MatchDto(
    val id: String,
    val userA: UserProfileDto,
    val userB: UserProfileDto,
    val createdAtMs: Long,
)

@Serializable
data class MessageDto(
    val id: String,
    val matchId: String,
    val fromUserId: String,
    val text: String,
    val timestampMs: Long,
)

@Serializable
data class SendMessageRequest(val text: String)

@Serializable
data class DealItemDto(
    val id: String,
    val title: String,
    val kind: String,
)

@Serializable
data class DealDto(
    val id: String,
    val matchId: String,
    val proposerUserId: String,
    val offer: List<DealItemDto>,
    val request: List<DealItemDto>,
    val status: String,
    val createdAtMs: Long,
)

@Serializable
data class ProposeDealRequest(
    val offer: List<DealItemDto>,
    val request: List<DealItemDto>,
)

@Serializable
data class UpdateDealStatusRequest(val status: String)

@Serializable
data class UpdateInterestsRequest(val interests: List<String>)

@Serializable
data class CreateListingRequest(
    val title: String,
    val description: String,
    val kind: String,
    val tags: List<String> = emptyList(),
    val estimatedValue: Double? = null,
    val imageUrl: String = "",
    val validUntilMs: Long? = null,
)

@Serializable
data class RenewRequest(val newValidUntilMs: Long)

@Serializable
data class UpdateAvailabilityRequest(val availability: String)

@Serializable
data class TopUpRequest(val amount: Double)

@Serializable
data class BalanceResponse(val balance: Double)

@Serializable
data class CountDto(val count: Int)

@Serializable
data class ProfileStatsDto(
    val activeListingsCount: Int,
    val completedDealsCount: Int,
    val matchesCount: Int,
)

@Serializable
data class EnrichedMatchDto(
    val match: MatchDto,
    val lastMessage: MessageDto? = null,
    val unreadCount: Int = 0,
)

@Serializable
data class NotificationDto(
    val id: String,
    val recipientUserId: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedListingId: String? = null,
    val relatedMatchId: String? = null,
    val timestampMs: Long,
    val isRead: Boolean = false,
)

// ── Review DTOs ──────────────────────────────────────────

@Serializable
data class ReviewDto(
    val id: String,
    val dealId: String,
    val reviewerUserId: String,
    val reviewedUserId: String,
    val rating: Int,
    val comment: String,
    val timestampMs: Long,
    val reviewerName: String = "",
)

@Serializable
data class SubmitReviewRequest(val dealId: String, val rating: Int, val comment: String = "")

@Serializable
data class UserRatingSummary(val averageRating: Double, val reviewCount: Int)

// ── Geocode DTOs ──────────────────────────────────────────

@Serializable
data class GeocodeSuggestionDto(
    val displayName: String,
    val city: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
data class NominatimResult(
    val lat: String,
    val lon: String,
    @SerialName("display_name") val displayName: String,
    val address: NominatimAddress? = null,
)

@Serializable
data class NominatimAddress(
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val country: String? = null,
)

// ── HTTP client for geocoding ─────────────────────────────

private val geocodeClient = HttpClient(io.ktor.client.engine.cio.CIO) {
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }
}

// ── Constants ─────────────────────────────────────────────

private const val RENEWAL_COST = 5.0

private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLng / 2) * Math.sin(dLng / 2)
    return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

// ── Helpers ────────────────────────────────────────────────

private fun readUser(row: ResultRow) = UserProfileDto(
    id = row[Users.id],
    displayName = row[Users.displayName],
    location = row[Users.location],
    rating = row[Users.rating],
    avatarUrl = row[Users.avatarUrl],
    email = row[Users.email],
    interests = row[Users.interests].split(",").filter { it.isNotBlank() },
    hasSelectedInterests = row[Users.hasSelectedInterests],
    balance = row[Users.balance],
    latitude = row[Users.latitude],
    longitude = row[Users.longitude],
)

private fun readListing(row: ResultRow, owner: UserProfileDto, tags: List<String>) = ListingDto(
    id = row[Listings.id],
    owner = owner,
    kind = row[Listings.kind],
    title = row[Listings.title],
    description = row[Listings.description],
    imageUrl = row[Listings.imageUrl],
    tags = tags,
    estimatedValue = row[Listings.estimatedValue],
    createdAtMs = row[Listings.createdAt],
    validUntilMs = row[Listings.validUntilMs],
    isHidden = row[Listings.isHidden],
    availability = row[Listings.availability],
)

/**
 * Read a full ListingDto for a given listing row inside a transaction.
 */
private fun readFullListing(row: ResultRow): ListingDto {
    val owner = Users.selectAll().where { Users.id eq row[Listings.ownerId] }.single()
    val tags = ListingTags.selectAll()
        .where { ListingTags.listingId eq row[Listings.id] }
        .map { it[ListingTags.tag] }
    return readListing(row, readUser(owner), tags)
}

// ── Route installation ─────────────────────────────────────

private fun RoutingCall.userId(): String =
    request.headers["X-User-Id"]?.takeIf { it.isNotBlank() } ?: "me"

fun Route.barterRoutes() {

    // POST /api/auth/register
    post("/api/auth/register") {
        val body = call.receive<RegisterRequest>()

        // Check if email already taken
        val existing = transaction {
            Users.selectAll().where { Users.email eq body.email }.singleOrNull()
        }
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, mapOf("error" to "Email already registered"))
            return@post
        }

        val userId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val user = transaction {
            Users.insert {
                it[id] = userId
                it[displayName] = body.displayName
                it[location] = body.location
                it[email] = body.email
                it[password] = body.password
                it[rating] = 0.0
                it[avatarUrl] = ""
                it[latitude] = body.latitude
                it[longitude] = body.longitude
                it[createdAt] = now
            }
            Users.selectAll().where { Users.id eq userId }.single()
        }

        call.respond(HttpStatusCode.Created, AuthResponse(user = readUser(user)))
    }

    // POST /api/auth/login
    post("/api/auth/login") {
        val body = call.receive<LoginRequest>()

        val user = transaction {
            Users.selectAll().where {
                (Users.email eq body.email) and (Users.password eq body.password)
            }.singleOrNull()
        }

        if (user == null) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
            return@post
        }

        call.respond(AuthResponse(user = readUser(user)))
    }

    // POST /api/auth/logout
    post("/api/auth/logout") {
        call.respond(HttpStatusCode.OK, mapOf("message" to "Logged out"))
    }

    // GET /api/discovery
    get("/api/discovery") {
        val userId = call.userId()
        val userLat = call.request.queryParameters["lat"]?.toDoubleOrNull()
        val userLng = call.request.queryParameters["lng"]?.toDoubleOrNull()

        val listings = transaction {
            // IDs of listings this user already swiped
            val swipedIds = Swipes.selectAll()
                .where { Swipes.fromUserId eq userId }
                .map { it[Swipes.targetListingId] }
                .toSet()

            val now = System.currentTimeMillis()

            val all = Listings.selectAll()
                .where {
                    (Listings.ownerId neq userId) and
                        (Listings.isHidden eq false) and
                        (Listings.availability neq "SOLD")
                }
                .map { row -> readFullListing(row) }
                .filter { it.id !in swipedIds }
                .filter { it.validUntilMs == null || it.validUntilMs > now }

            if (userLat != null && userLng != null) {
                all.sortedBy { listing ->
                    val oLat = listing.owner.latitude
                    val oLng = listing.owner.longitude
                    if (oLat != null && oLng != null) haversineDistance(userLat, userLng, oLat, oLng)
                    else Double.MAX_VALUE
                }
            } else all
        }
        call.respond(listings)
    }

    // POST /api/swipe/{listingId}
    post("/api/swipe/{listingId}") {
        val listingId = call.parameters["listingId"]!!
        val body = call.receive<SwipeRequest>()
        val now = System.currentTimeMillis()

        // Record swipe
        transaction {
            Swipes.insert {
                it[id] = UUID.randomUUID().toString()
                it[fromUserId] = call.userId()
                it[targetListingId] = listingId
                it[action] = body.action
                it[timestampMs] = now
            }
        }

        if (body.action != "LIKE") {
            call.respond(HttpStatusCode.OK, mapOf("match" to null))
            return@post
        }

        // Check mutual like (simplified: 50% random or check reverse swipe)
        val listing = transaction {
            Listings.selectAll().where { Listings.id eq listingId }.singleOrNull()
        }
        if (listing == null) {
            call.respond(HttpStatusCode.NotFound)
            return@post
        }

        val ownerId = listing[Listings.ownerId]
        // Check if owner liked any of current user's listings
        val mutualLike = transaction {
            Swipes.selectAll().where {
                (Swipes.fromUserId eq ownerId) and (Swipes.action eq "LIKE")
            }.any()
        }

        if (!mutualLike) {
            call.respond(mapOf("match" to null))
            return@post
        }

        // Create match
        val matchId = UUID.randomUUID().toString()
        val match = transaction {
            Matches.insert {
                it[id] = matchId
                it[userAId] = call.userId()
                it[userBId] = ownerId
                it[createdAtMs] = now
            }

            val userA = Users.selectAll().where { Users.id eq call.userId() }.single()
            val userB = Users.selectAll().where { Users.id eq ownerId }.single()

            MatchDto(
                id = matchId,
                userA = readUser(userA),
                userB = readUser(userB),
                createdAtMs = now,
            )
        }

        call.respond(mapOf("match" to match))
    }

    // GET /api/matches
    get("/api/matches") {
        val matches = transaction {
            Matches.selectAll().where {
                (Matches.userAId eq call.userId()) or (Matches.userBId eq call.userId())
            }.map { row ->
                val userA = Users.selectAll().where { Users.id eq row[Matches.userAId] }.single()
                val userB = Users.selectAll().where { Users.id eq row[Matches.userBId] }.single()
                MatchDto(
                    id = row[Matches.id],
                    userA = readUser(userA),
                    userB = readUser(userB),
                    createdAtMs = row[Matches.createdAtMs],
                )
            }
        }
        call.respond(matches)
    }

    // GET /api/matches/enriched
    get("/api/matches/enriched") {
        val enrichedMatches = transaction {
            Matches.selectAll().where {
                (Matches.userAId eq call.userId()) or (Matches.userBId eq call.userId())
            }.map { row ->
                val matchId = row[Matches.id]
                val userA = Users.selectAll().where { Users.id eq row[Matches.userAId] }.single()
                val userB = Users.selectAll().where { Users.id eq row[Matches.userBId] }.single()

                val matchDto = MatchDto(
                    id = matchId,
                    userA = readUser(userA),
                    userB = readUser(userB),
                    createdAtMs = row[Matches.createdAtMs],
                )

                // Last message
                val lastMessage = Messages.selectAll()
                    .where { Messages.matchId eq matchId }
                    .orderBy(Messages.timestampMs, SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
                    ?.let { msgRow ->
                        MessageDto(
                            id = msgRow[Messages.id],
                            matchId = msgRow[Messages.matchId],
                            fromUserId = msgRow[Messages.fromUserId],
                            text = msgRow[Messages.text],
                            timestampMs = msgRow[Messages.timestampMs],
                        )
                    }

                // Unread count: messages not from call.userId() that arrived after
                // the last message sent by call.userId()
                val lastSentTimestamp = Messages.selectAll()
                    .where { (Messages.matchId eq matchId) and (Messages.fromUserId eq call.userId()) }
                    .orderBy(Messages.timestampMs, SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
                    ?.get(Messages.timestampMs) ?: 0L

                val unreadCount = Messages.selectAll().where {
                    (Messages.matchId eq matchId) and
                        (Messages.fromUserId neq call.userId()) and
                        (Messages.timestampMs greater lastSentTimestamp)
                }.count().toInt()

                EnrichedMatchDto(
                    match = matchDto,
                    lastMessage = lastMessage,
                    unreadCount = unreadCount,
                )
            }
        }
        call.respond(enrichedMatches)
    }

    // GET /api/matches/{matchId}/messages
    get("/api/matches/{matchId}/messages") {
        val matchId = call.parameters["matchId"]!!
        val messages = transaction {
            Messages.selectAll().where { Messages.matchId eq matchId }
                .orderBy(Messages.timestampMs, SortOrder.ASC)
                .map { row ->
                    MessageDto(
                        id = row[Messages.id],
                        matchId = row[Messages.matchId],
                        fromUserId = row[Messages.fromUserId],
                        text = row[Messages.text],
                        timestampMs = row[Messages.timestampMs],
                    )
                }
        }
        call.respond(messages)
    }

    // POST /api/matches/{matchId}/messages
    post("/api/matches/{matchId}/messages") {
        val matchId = call.parameters["matchId"]!!
        val body = call.receive<SendMessageRequest>()
        val now = System.currentTimeMillis()

        val msg = transaction {
            val id = UUID.randomUUID().toString()
            Messages.insert {
                it[Messages.id] = id
                it[Messages.matchId] = matchId
                it[fromUserId] = call.userId()
                it[text] = body.text
                it[timestampMs] = now
            }
            MessageDto(id, matchId, call.userId(), body.text, now)
        }
        call.respond(HttpStatusCode.Created, msg)
    }

    // GET /api/matches/{matchId}/deals
    get("/api/matches/{matchId}/deals") {
        val matchId = call.parameters["matchId"]!!
        val deals = transaction {
            Deals.selectAll().where { Deals.matchId eq matchId }.map { row ->
                val items = DealItems.selectAll().where { DealItems.dealId eq row[Deals.id] }
                DealDto(
                    id = row[Deals.id],
                    matchId = row[Deals.matchId],
                    proposerUserId = row[Deals.proposerUserId],
                    offer = items.filter { it[DealItems.isOffer] }.map {
                        DealItemDto(it[DealItems.id], it[DealItems.title], it[DealItems.kind])
                    },
                    request = items.filter { !it[DealItems.isOffer] }.map {
                        DealItemDto(it[DealItems.id], it[DealItems.title], it[DealItems.kind])
                    },
                    status = row[Deals.status],
                    createdAtMs = row[Deals.createdAtMs],
                )
            }
        }
        call.respond(deals)
    }

    // POST /api/matches/{matchId}/deals
    post("/api/matches/{matchId}/deals") {
        val matchId = call.parameters["matchId"]!!
        val body = call.receive<ProposeDealRequest>()
        val now = System.currentTimeMillis()

        val deal = transaction {
            val dealId = UUID.randomUUID().toString()
            Deals.insert {
                it[id] = dealId
                it[Deals.matchId] = matchId
                it[proposerUserId] = call.userId()
                it[status] = "PROPOSED"
                it[createdAtMs] = now
            }
            body.offer.forEach { item ->
                DealItems.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[DealItems.dealId] = dealId
                    it[title] = item.title
                    it[kind] = item.kind
                    it[isOffer] = true
                }
            }
            body.request.forEach { item ->
                DealItems.insert {
                    it[id] = UUID.randomUUID().toString()
                    it[DealItems.dealId] = dealId
                    it[title] = item.title
                    it[kind] = item.kind
                    it[isOffer] = false
                }
            }
            DealDto(dealId, matchId, call.userId(), body.offer, body.request, "PROPOSED", now)
        }
        call.respond(HttpStatusCode.Created, deal)
    }

    // PATCH /api/deals/{dealId}
    patch("/api/deals/{dealId}") {
        val dealId = call.parameters["dealId"]!!
        val body = call.receive<UpdateDealStatusRequest>()

        transaction {
            Deals.update({ Deals.id eq dealId }) {
                it[status] = body.status
            }
        }
        call.respond(HttpStatusCode.OK, mapOf("status" to body.status))
    }

    // GET /api/users/me
    get("/api/users/me") {
        val user = transaction {
            Users.selectAll().where { Users.id eq call.userId() }.singleOrNull()
        }
        if (user == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(readUser(user))
        }
    }

    // ── Interests ──────────────────────────────────────────

    // PUT /api/user/interests
    put("/api/user/interests") {
        val body = call.receive<UpdateInterestsRequest>()

        val updatedUser = transaction {
            Users.update({ Users.id eq call.userId() }) {
                it[interests] = body.interests.joinToString(",")
                it[hasSelectedInterests] = true
            }
            Users.selectAll().where { Users.id eq call.userId() }.single()
        }

        call.respond(readUser(updatedUser))
    }

    // ── Listings CRUD ──────────────────────────────────────

    // GET /api/listings/mine (must be before /api/listings/{listingId} to avoid route collision)
    get("/api/listings/mine") {
        val listings = transaction {
            Listings.selectAll().where { Listings.ownerId eq call.userId() }
                .map { row -> readFullListing(row) }
        }
        call.respond(listings)
    }

    // GET /api/listings/search
    get("/api/listings/search") {
        val userId = call.userId()
        val q = call.request.queryParameters["q"]
        val category = call.request.queryParameters["category"]
        val sort = call.request.queryParameters["sort"]

        val listings = transaction {
            val now = System.currentTimeMillis()

            var query = Listings.selectAll().where {
                (Listings.ownerId neq userId) and
                    (Listings.isHidden eq false) and
                    (Listings.availability neq "SOLD")
            }

            // Text search on title
            if (!q.isNullOrBlank()) {
                query = query.andWhere { Listings.title.lowerCase() like "%${q.lowercase()}%" }
            }

            val allListings = query.map { row -> readFullListing(row) }
                .filter { it.validUntilMs == null || it.validUntilMs > now }

            // Filter by tag/category
            val filtered = if (!category.isNullOrBlank()) {
                allListings.filter { listing ->
                    listing.tags.any { it.equals(category, ignoreCase = true) }
                }
            } else {
                allListings
            }

            // Sort
            when (sort) {
                "NEWEST" -> filtered.sortedByDescending { it.createdAtMs }
                "PRICE_LOW_HIGH" -> filtered.sortedBy { it.estimatedValue ?: Double.MAX_VALUE }
                "PRICE_HIGH_LOW" -> filtered.sortedByDescending { it.estimatedValue ?: 0.0 }
                else -> filtered
            }
        }
        call.respond(listings)
    }

    // POST /api/listings
    post("/api/listings") {
        val userId = call.userId()

        // Verify user exists (stale session after DB reset)
        val userExists = transaction {
            Users.selectAll().where { Users.id eq userId }.singleOrNull() != null
        }
        if (!userExists) {
            call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found. Please log in again."))
            return@post
        }

        val body = call.receive<CreateListingRequest>()
        val now = System.currentTimeMillis()
        val listingId = UUID.randomUUID().toString()

        val listing = transaction {
            Listings.insert {
                it[id] = listingId
                it[ownerId] = userId
                it[kind] = body.kind
                it[title] = body.title
                it[description] = body.description
                it[imageUrl] = body.imageUrl
                it[estimatedValue] = body.estimatedValue
                it[validUntilMs] = body.validUntilMs
                it[createdAt] = now
            }
            body.tags.forEach { tag ->
                ListingTags.insert {
                    it[ListingTags.listingId] = listingId
                    it[ListingTags.tag] = tag
                }
            }
            Listings.selectAll().where { Listings.id eq listingId }.single().let { readFullListing(it) }
        }

        call.respond(HttpStatusCode.Created, listing)
    }

    // GET /api/listings/{listingId}
    get("/api/listings/{listingId}") {
        val listingId = call.parameters["listingId"]!!

        val listing = transaction {
            Listings.selectAll().where { Listings.id eq listingId }.singleOrNull()
                ?.let { readFullListing(it) }
        }

        if (listing == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Listing not found"))
        } else {
            call.respond(listing)
        }
    }

    // PUT /api/listings/{listingId}
    put("/api/listings/{listingId}") {
        val listingId = call.parameters["listingId"]!!
        val body = call.receive<CreateListingRequest>()

        val listing = transaction {
            Listings.update({ Listings.id eq listingId }) {
                it[kind] = body.kind
                it[title] = body.title
                it[description] = body.description
                it[imageUrl] = body.imageUrl
                it[estimatedValue] = body.estimatedValue
                it[validUntilMs] = body.validUntilMs
            }

            // Delete old tags and insert new ones
            ListingTags.deleteWhere { ListingTags.listingId eq listingId }
            body.tags.forEach { tag ->
                ListingTags.insert {
                    it[ListingTags.listingId] = listingId
                    it[ListingTags.tag] = tag
                }
            }

            Listings.selectAll().where { Listings.id eq listingId }.single().let { readFullListing(it) }
        }

        call.respond(listing)
    }

    // DELETE /api/listings/{listingId}
    delete("/api/listings/{listingId}") {
        val listingId = call.parameters["listingId"]!!

        transaction {
            ListingTags.deleteWhere { ListingTags.listingId eq listingId }
            Listings.deleteWhere { Listings.id eq listingId }
        }

        call.respond(HttpStatusCode.NoContent)
    }

    // PATCH /api/listings/{listingId}/visibility
    patch("/api/listings/{listingId}/visibility") {
        val listingId = call.parameters["listingId"]!!

        val listing = transaction {
            val current = Listings.selectAll().where { Listings.id eq listingId }.singleOrNull()
                ?: return@transaction null

            val newHidden = !current[Listings.isHidden]
            Listings.update({ Listings.id eq listingId }) {
                it[isHidden] = newHidden
            }

            Listings.selectAll().where { Listings.id eq listingId }.single().let { readFullListing(it) }
        }

        if (listing == null) {
            call.respond(HttpStatusCode.NotFound, mapOf("error" to "Listing not found"))
        } else {
            call.respond(listing)
        }
    }

    // PATCH /api/listings/{listingId}/renew
    patch("/api/listings/{listingId}/renew") {
        val listingId = call.parameters["listingId"]!!
        val body = call.receive<RenewRequest>()

        val listing = transaction {
            // Deduct renewal cost from user balance
            val userRow = Users.selectAll().where { Users.id eq call.userId() }.single()
            val currentBalance = userRow[Users.balance]
            Users.update({ Users.id eq call.userId() }) {
                it[balance] = currentBalance - RENEWAL_COST
            }

            Listings.update({ Listings.id eq listingId }) {
                it[validUntilMs] = body.newValidUntilMs
            }

            Listings.selectAll().where { Listings.id eq listingId }.single().let { readFullListing(it) }
        }

        call.respond(listing)
    }

    // PATCH /api/listings/{listingId}/availability
    patch("/api/listings/{listingId}/availability") {
        val listingId = call.parameters["listingId"]!!
        val body = call.receive<UpdateAvailabilityRequest>()

        val listing = transaction {
            Listings.update({ Listings.id eq listingId }) {
                it[availability] = body.availability
            }

            Listings.selectAll().where { Listings.id eq listingId }.single().let { readFullListing(it) }
        }

        call.respond(listing)
    }

    // ── Balance ────────────────────────────────────────────

    // POST /api/user/balance/topup
    post("/api/user/balance/topup") {
        val body = call.receive<TopUpRequest>()

        val updatedUser = transaction {
            val userRow = Users.selectAll().where { Users.id eq call.userId() }.single()
            val currentBalance = userRow[Users.balance]
            Users.update({ Users.id eq call.userId() }) {
                it[balance] = currentBalance + body.amount
            }
            Users.selectAll().where { Users.id eq call.userId() }.single()
        }

        call.respond(readUser(updatedUser))
    }

    // GET /api/user/balance
    get("/api/user/balance") {
        val balance = transaction {
            Users.selectAll().where { Users.id eq call.userId() }.single()[Users.balance]
        }
        call.respond(BalanceResponse(balance = balance))
    }

    // ── Stats ──────────────────────────────────────────────

    // GET /api/user/stats
    get("/api/user/stats") {
        val stats = transaction {
            val activeListingsCount = Listings.selectAll()
                .where { (Listings.ownerId eq call.userId()) and (Listings.isHidden eq false) }
                .count().toInt()

            val completedDealsCount = Deals.selectAll()
                .where { (Deals.proposerUserId eq call.userId()) and (Deals.status eq "COMPLETED") }
                .count().toInt()

            val matchesCount = Matches.selectAll()
                .where { (Matches.userAId eq call.userId()) or (Matches.userBId eq call.userId()) }
                .count().toInt()

            ProfileStatsDto(
                activeListingsCount = activeListingsCount,
                completedDealsCount = completedDealsCount,
                matchesCount = matchesCount,
            )
        }
        call.respond(stats)
    }

    // ── Badges ─────────────────────────────────────────────

    // GET /api/badges/matches
    get("/api/badges/matches") {
        val count = transaction {
            Matches.selectAll().where {
                (Matches.userAId eq call.userId()) or (Matches.userBId eq call.userId())
            }.count().toInt()
        }
        call.respond(CountDto(count = count))
    }

    // GET /api/badges/messages
    get("/api/badges/messages") {
        val count = transaction {
            // Get all match IDs for the current user
            val matchIds = Matches.selectAll().where {
                (Matches.userAId eq call.userId()) or (Matches.userBId eq call.userId())
            }.map { it[Matches.id] }

            // For each match, count messages not from call.userId() after last message from call.userId()
            matchIds.sumOf { matchId ->
                val lastSentTimestamp = Messages.selectAll()
                    .where { (Messages.matchId eq matchId) and (Messages.fromUserId eq call.userId()) }
                    .orderBy(Messages.timestampMs, SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
                    ?.get(Messages.timestampMs) ?: 0L

                Messages.selectAll().where {
                    (Messages.matchId eq matchId) and
                        (Messages.fromUserId neq call.userId()) and
                        (Messages.timestampMs greater lastSentTimestamp)
                }.count().toInt()
            }
        }
        call.respond(CountDto(count = count))
    }

    // ── Notifications ──────────────────────────────────────

    // GET /api/notifications
    get("/api/notifications") {
        val notifications = transaction {
            Notifications.selectAll()
                .where { Notifications.recipientUserId eq call.userId() }
                .orderBy(Notifications.timestampMs, SortOrder.DESC)
                .map { row ->
                    NotificationDto(
                        id = row[Notifications.id],
                        recipientUserId = row[Notifications.recipientUserId],
                        type = row[Notifications.type],
                        title = row[Notifications.title],
                        body = row[Notifications.body],
                        relatedListingId = row[Notifications.relatedListingId],
                        relatedMatchId = row[Notifications.relatedMatchId],
                        timestampMs = row[Notifications.timestampMs],
                        isRead = row[Notifications.isRead],
                    )
                }
        }
        call.respond(notifications)
    }

    // GET /api/notifications/unread-count
    get("/api/notifications/unread-count") {
        val count = transaction {
            Notifications.selectAll().where {
                (Notifications.recipientUserId eq call.userId()) and (Notifications.isRead eq false)
            }.count().toInt()
        }
        call.respond(CountDto(count = count))
    }

    // PATCH /api/notifications/{notificationId}/read
    patch("/api/notifications/{notificationId}/read") {
        val notificationId = call.parameters["notificationId"]!!

        transaction {
            Notifications.update({ Notifications.id eq notificationId }) {
                it[isRead] = true
            }
        }

        call.respond(HttpStatusCode.OK, mapOf("success" to true))
    }

    // ── Reviews ─────────────────────────────────────────────

    // POST /api/reviews
    post("/api/reviews") {
        val body = call.receive<SubmitReviewRequest>()
        val userId = call.userId()
        val now = System.currentTimeMillis()

        // Validate rating range
        if (body.rating !in 1..5) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Rating must be between 1 and 5"))
            return@post
        }

        val result = transaction {
            // Find the deal
            val dealRow = Deals.selectAll().where { Deals.id eq body.dealId }.singleOrNull()
                ?: return@transaction "Deal not found" to null

            // Deal must be COMPLETED
            if (dealRow[Deals.status] != "COMPLETED") {
                return@transaction "Deal is not completed" to null
            }

            // Find the match to determine participants
            val matchId = dealRow[Deals.matchId]
            val matchRow = Matches.selectAll().where { Matches.id eq matchId }.singleOrNull()
                ?: return@transaction "Match not found" to null

            val userAId = matchRow[Matches.userAId]
            val userBId = matchRow[Matches.userBId]

            // Caller must be a participant
            if (userId != userAId && userId != userBId) {
                return@transaction "You are not a participant in this deal" to null
            }

            // Determine who is being reviewed (the other party)
            val reviewedUserId = if (userId == userAId) userBId else userAId

            // Check if already reviewed
            val existing = Reviews.selectAll().where {
                (Reviews.dealId eq body.dealId) and (Reviews.reviewerUserId eq userId)
            }.singleOrNull()
            if (existing != null) {
                return@transaction "You have already reviewed this deal" to null
            }

            // Insert review
            val reviewId = UUID.randomUUID().toString()
            Reviews.insert {
                it[id] = reviewId
                it[Reviews.dealId] = body.dealId
                it[Reviews.reviewerUserId] = userId
                it[Reviews.reviewedUserId] = reviewedUserId
                it[rating] = body.rating
                it[comment] = body.comment
                it[timestampMs] = now
            }

            // Recalculate average rating for the reviewed user
            val allRatings = Reviews.selectAll()
                .where { Reviews.reviewedUserId eq reviewedUserId }
                .map { it[Reviews.rating] }
            val avgRating = if (allRatings.isNotEmpty()) allRatings.average() else 0.0
            Users.update({ Users.id eq reviewedUserId }) {
                it[Users.rating] = kotlin.math.round(avgRating * 10) / 10.0
            }

            // Get reviewer name
            val reviewerRow = Users.selectAll().where { Users.id eq userId }.single()
            val reviewerName = reviewerRow[Users.displayName]

            null to ReviewDto(
                id = reviewId,
                dealId = body.dealId,
                reviewerUserId = userId,
                reviewedUserId = reviewedUserId,
                rating = body.rating,
                comment = body.comment,
                timestampMs = now,
                reviewerName = reviewerName,
            )
        }

        val (error, review) = result
        if (error != null) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error))
        } else {
            call.respond(HttpStatusCode.Created, review!!)
        }
    }

    // GET /api/users/{userId}/reviews
    get("/api/users/{userId}/reviews") {
        val userId = call.parameters["userId"]!!

        val reviews = transaction {
            Reviews.selectAll()
                .where { Reviews.reviewedUserId eq userId }
                .orderBy(Reviews.timestampMs, SortOrder.DESC)
                .map { row ->
                    val reviewerRow = Users.selectAll()
                        .where { Users.id eq row[Reviews.reviewerUserId] }
                        .singleOrNull()
                    ReviewDto(
                        id = row[Reviews.id],
                        dealId = row[Reviews.dealId],
                        reviewerUserId = row[Reviews.reviewerUserId],
                        reviewedUserId = row[Reviews.reviewedUserId],
                        rating = row[Reviews.rating],
                        comment = row[Reviews.comment],
                        timestampMs = row[Reviews.timestampMs],
                        reviewerName = reviewerRow?.get(Users.displayName) ?: "Unknown",
                    )
                }
        }
        call.respond(reviews)
    }

    // GET /api/users/{userId}/rating
    get("/api/users/{userId}/rating") {
        val userId = call.parameters["userId"]!!

        val summary = transaction {
            val allRatings = Reviews.selectAll()
                .where { Reviews.reviewedUserId eq userId }
                .map { it[Reviews.rating] }
            UserRatingSummary(
                averageRating = if (allRatings.isNotEmpty()) {
                    kotlin.math.round(allRatings.average() * 10) / 10.0
                } else 0.0,
                reviewCount = allRatings.size,
            )
        }
        call.respond(summary)
    }

    // ── Geocode autocomplete ──────────────────────────────

    // GET /api/geocode/autocomplete?q=Chis
    get("/api/geocode/autocomplete") {
        val q = call.request.queryParameters["q"] ?: ""
        if (q.length < 2) {
            call.respond(emptyList<GeocodeSuggestionDto>())
            return@get
        }

        try {
            val results: List<NominatimResult> = geocodeClient.get(
                "https://nominatim.openstreetmap.org/search"
            ) {
                parameter("q", q)
                parameter("format", "json")
                parameter("addressdetails", "1")
                parameter("limit", "5")
                parameter("accept-language", "en")
                header("User-Agent", "BarterApp/1.0")
            }.body()

            val suggestions = results.map { r ->
                GeocodeSuggestionDto(
                    displayName = r.displayName,
                    city = r.address?.city
                        ?: r.address?.town
                        ?: r.address?.village
                        ?: r.displayName.split(",").first().trim(),
                    country = r.address?.country ?: "",
                    latitude = r.lat.toDouble(),
                    longitude = r.lon.toDouble(),
                )
            }
            call.respond(suggestions)
        } catch (e: Exception) {
            call.respond(emptyList<GeocodeSuggestionDto>())
        }
    }
}
