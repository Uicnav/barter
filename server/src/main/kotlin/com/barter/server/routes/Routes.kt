package com.barter.server.routes

import com.barter.server.db.*
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
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
)

@Serializable
data class RegisterRequest(
    val displayName: String,
    val location: String,
    val email: String,
    val password: String,
)

@Serializable
data class AuthResponse(
    val user: UserProfileDto,
)

@Serializable
data class ListingDto(
    val id: String,
    val owner: UserProfileDto,
    val kind: String,
    val title: String,
    val description: String,
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
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

// ── Helpers ────────────────────────────────────────────────

private fun readUser(row: ResultRow) = UserProfileDto(
    id = row[Users.id],
    displayName = row[Users.displayName],
    location = row[Users.location],
    rating = row[Users.rating],
    avatarUrl = row[Users.avatarUrl],
    email = row[Users.email],
)

// ── Route installation ─────────────────────────────────────

fun Route.barterRoutes() {
    val currentUserId = "me" // Simplified; real app uses auth tokens

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
                it[createdAt] = now
            }
            Users.selectAll().where { Users.id eq userId }.single()
        }

        call.respond(HttpStatusCode.Created, AuthResponse(user = readUser(user)))
    }

    // GET /api/discovery
    get("/api/discovery") {
        val listings = transaction {
            Listings.selectAll().map { row ->
                val owner = Users.selectAll().where { Users.id eq row[Listings.ownerId] }.single()
                val tags = ListingTags.selectAll()
                    .where { ListingTags.listingId eq row[Listings.id] }
                    .map { it[ListingTags.tag] }

                ListingDto(
                    id = row[Listings.id],
                    owner = readUser(owner),
                    kind = row[Listings.kind],
                    title = row[Listings.title],
                    description = row[Listings.description],
                    imageUrl = row[Listings.imageUrl],
                    tags = tags,
                )
            }
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
                it[fromUserId] = currentUserId
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
                it[userAId] = currentUserId
                it[userBId] = ownerId
                it[createdAtMs] = now
            }

            val userA = Users.selectAll().where { Users.id eq currentUserId }.single()
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
                (Matches.userAId eq currentUserId) or (Matches.userBId eq currentUserId)
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
                it[fromUserId] = currentUserId
                it[text] = body.text
                it[timestampMs] = now
            }
            MessageDto(id, matchId, currentUserId, body.text, now)
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
                it[proposerUserId] = currentUserId
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
            DealDto(dealId, matchId, currentUserId, body.offer, body.request, "PROPOSED", now)
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
            Users.selectAll().where { Users.id eq currentUserId }.singleOrNull()
        }
        if (user == null) {
            call.respond(HttpStatusCode.NotFound)
        } else {
            call.respond(readUser(user))
        }
    }
}
