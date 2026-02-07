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
    val interests = varchar("interests", 1000).default("")
    val hasSelectedInterests = bool("has_selected_interests").default(false)
    val balance = double("balance").default(0.0)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
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
    val isHidden = bool("is_hidden").default(false)
    val validUntilMs = long("valid_until_ms").nullable()
    val estimatedValue = double("estimated_value").nullable()
    val availability = varchar("availability", 20).default("AVAILABLE")
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

object Reviews : Table("reviews") {
    val id = varchar("id", 36)
    val dealId = varchar("deal_id", 36).references(Deals.id)
    val reviewerUserId = varchar("reviewer_user_id", 36).references(Users.id)
    val reviewedUserId = varchar("reviewed_user_id", 36).references(Users.id)
    val rating = integer("rating") // 1-5
    val comment = text("comment").default("")
    val timestampMs = long("timestamp_ms")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(dealId, reviewerUserId)
    }
}

object Notifications : Table("notifications") {
    val id = varchar("id", 36)
    val recipientUserId = varchar("recipient_user_id", 36).references(Users.id)
    val type = varchar("type", 30) // LIKE_RECEIVED, MATCH_CREATED
    val title = varchar("title", 200)
    val body = text("body")
    val relatedListingId = varchar("related_listing_id", 36).nullable()
    val relatedMatchId = varchar("related_match_id", 36).nullable()
    val timestampMs = long("timestamp_ms")
    val isRead = bool("is_read").default(false)

    override val primaryKey = PrimaryKey(id)
}
