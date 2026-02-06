package com.barter.server.db

import org.jetbrains.exposed.sql.Table

object Users : Table("users") {
    val id = varchar("id", 36)
    val displayName = varchar("display_name", 100)
    val location = varchar("location", 200)
    val rating = double("rating").default(0.0)
    val avatarUrl = varchar("avatar_url", 500).default("")
    val email = varchar("email", 200).default("")
    val password = varchar("password", 200).default("")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object Listings : Table("listings") {
    val id = varchar("id", 36)
    val ownerId = varchar("owner_id", 36).references(Users.id)
    val kind = varchar("kind", 20)       // GOODS, SERVICES, BOTH
    val title = varchar("title", 200)
    val description = text("description")
    val imageUrl = varchar("image_url", 500).default("")
    val createdAt = long("created_at")

    override val primaryKey = PrimaryKey(id)
}

object ListingTags : Table("listing_tags") {
    val listingId = varchar("listing_id", 36).references(Listings.id)
    val tag = varchar("tag", 50)

    override val primaryKey = PrimaryKey(listingId, tag)
}

object Swipes : Table("swipes") {
    val id = varchar("id", 36)
    val fromUserId = varchar("from_user_id", 36).references(Users.id)
    val targetListingId = varchar("target_listing_id", 36).references(Listings.id)
    val action = varchar("action", 10) // LIKE, PASS
    val timestampMs = long("timestamp_ms")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(fromUserId, targetListingId)
    }
}

object Matches : Table("matches") {
    val id = varchar("id", 36)
    val userAId = varchar("user_a_id", 36).references(Users.id)
    val userBId = varchar("user_b_id", 36).references(Users.id)
    val createdAtMs = long("created_at_ms")

    override val primaryKey = PrimaryKey(id)
}

object Messages : Table("messages") {
    val id = varchar("id", 36)
    val matchId = varchar("match_id", 36).references(Matches.id)
    val fromUserId = varchar("from_user_id", 36).references(Users.id)
    val text = text("text")
    val timestampMs = long("timestamp_ms")

    override val primaryKey = PrimaryKey(id)
}

object Deals : Table("deals") {
    val id = varchar("id", 36)
    val matchId = varchar("match_id", 36).references(Matches.id)
    val proposerUserId = varchar("proposer_user_id", 36).references(Users.id)
    val status = varchar("status", 20) // PROPOSED, ACCEPTED, REJECTED, CANCELLED, COMPLETED
    val createdAtMs = long("created_at_ms")

    override val primaryKey = PrimaryKey(id)
}

object DealItems : Table("deal_items") {
    val id = varchar("id", 36)
    val dealId = varchar("deal_id", 36).references(Deals.id)
    val title = varchar("title", 200)
    val kind = varchar("kind", 20)
    val isOffer = bool("is_offer") // true = offered, false = requested

    override val primaryKey = PrimaryKey(id)
}
