package com.barter.core.domain.model

import kotlinx.serialization.Serializable

// ── Listing types ─────────────────────────────────────────
@Serializable
enum class ListingKind { GOODS, SERVICES, BOTH }

// ── User ──────────────────────────────────────────────────
@Serializable
data class UserProfile(
    val id: String,
    val displayName: String,
    val location: String,
    val rating: Double = 0.0,
    val avatarUrl: String = "",
    val email: String = "",
    val interests: List<String> = emptyList(),
    val hasSelectedInterests: Boolean = false,
    val balance: Double = 0.0,
)

// ── Platform pricing ─────────────────────────────────────
const val RENEWAL_COST_MDL = 10.0
const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000

// ── Auth ──────────────────────────────────────────────────
@Serializable
data class RegistrationRequest(
    val displayName: String,
    val location: String,
    val email: String,
    val password: String,
)

@Serializable
sealed class AuthState {
    @Serializable
    data object Unauthenticated : AuthState()
    @Serializable
    data object Loading : AuthState()
    @Serializable
    data class Authenticated(val user: UserProfile) : AuthState()
    @Serializable
    data class Error(val message: String) : AuthState()
}

// ── Interest categories ───────────────────────────────────
@Serializable
data class InterestCategory(
    val id: String,
    val label: String,
    val emoji: String,
)

object PredefinedCategories {
    val all = listOf(
        InterestCategory("tech", "Tech", "\uD83D\uDCBB"),
        InterestCategory("sport", "Sport", "\u26BD"),
        InterestCategory("beauty", "Beauty", "\uD83D\uDC84"),
        InterestCategory("music", "Music", "\uD83C\uDFB5"),
        InterestCategory("fashion", "Fashion", "\uD83D\uDC57"),
        InterestCategory("photo", "Photo", "\uD83D\uDCF7"),
        InterestCategory("games", "Games", "\uD83C\uDFAE"),
        InterestCategory("design", "Design", "\uD83C\uDFA8"),
        InterestCategory("auto", "Auto", "\uD83D\uDE97"),
        InterestCategory("home", "Home", "\uD83C\uDFE0"),
        InterestCategory("art", "Art", "\uD83D\uDD8C\uFE0F"),
        InterestCategory("creative", "Creative", "\u2728"),
        InterestCategory("web", "Web", "\uD83C\uDF10"),
        InterestCategory("food", "Food", "\uD83C\uDF55"),
        InterestCategory("books", "Books", "\uD83D\uDCDA"),
        InterestCategory("fitness", "Fitness", "\uD83C\uDFCB\uFE0F"),
    )
}

// ── Listing status ────────────────────────────────────────
@Serializable
enum class ListingStatus { ACTIVE, EXPIRED, HIDDEN }

// ── Availability lifecycle ────────────────────────────────
@Serializable
enum class AvailabilityStatus { AVAILABLE, RESERVED, SOLD }

// ── Sort options for browse ───────────────────────────────
@Serializable
enum class SortOption { NEWEST, PRICE_LOW_HIGH, PRICE_HIGH_LOW }

// ── Listing ───────────────────────────────────────────────
@Serializable
data class Listing(
    val id: String,
    val owner: UserProfile,
    val kind: ListingKind,
    val title: String,
    val description: String,
    val photos: List<String> = emptyList(),
    val imageUrl: String = "",
    val tags: List<String> = emptyList(),
    val estimatedValue: Double? = null,
    val createdAtMs: Long = 0L,
    val validUntilMs: Long? = null,
    val isHidden: Boolean = false,
    val availability: AvailabilityStatus = AvailabilityStatus.AVAILABLE,
)

val Listing.isExpired: Boolean
    get() = validUntilMs != null && validUntilMs < System.currentTimeMillis()

val Listing.daysRemaining: Int?
    get() {
        val until = validUntilMs ?: return null
        val diff = until - System.currentTimeMillis()
        return if (diff > 0) (diff / 86_400_000).toInt() else 0
    }

val Listing.status: ListingStatus
    get() = when {
        isHidden -> ListingStatus.HIDDEN
        isExpired -> ListingStatus.EXPIRED
        else -> ListingStatus.ACTIVE
    }

// ── Swipe ─────────────────────────────────────────────────
@Serializable
enum class SwipeAction { LIKE, PASS }

@Serializable
data class Swipe(
    val fromUserId: String,
    val targetListingId: String,
    val action: SwipeAction,
    val timestampMs: Long,
)

// ── Match ─────────────────────────────────────────────────
@Serializable
data class Match(
    val id: String,
    val userA: UserProfile,
    val userB: UserProfile,
    val createdAtMs: Long,
)

// ── Chat ──────────────────────────────────────────────────
@Serializable
data class Message(
    val id: String,
    val matchId: String,
    val fromUserId: String,
    val text: String,
    val timestampMs: Long,
)

// ── Deal ──────────────────────────────────────────────────
@Serializable
enum class DealStatus { PROPOSED, ACCEPTED, REJECTED, CANCELLED, COMPLETED }

@Serializable
data class DealItem(
    val id: String,
    val title: String,
    val kind: ListingKind,
    val estimatedValue: Double? = null,
)

@Serializable
data class Deal(
    val id: String,
    val matchId: String,
    val proposerUserId: String,
    val offer: List<DealItem>,
    val request: List<DealItem>,
    val status: DealStatus,
    val createdAtMs: Long,
    val cashTopUp: Double = 0.0,
    val note: String = "",
)

// ── Deal value helper ────────────────────────────────────
data class DealValueSummary(
    val offerTotal: Double,
    val requestTotal: Double,
    val difference: Double,
    val suggestedTopUp: Double,
    val isFair: Boolean,
)

// ── Profile stats ────────────────────────────────────────
@Serializable
data class ProfileStats(
    val activeListingsCount: Int = 0,
    val completedDealsCount: Int = 0,
    val matchesCount: Int = 0,
)

// ── Enriched match (with last message preview) ───────────
data class EnrichedMatch(
    val match: Match,
    val lastMessage: Message? = null,
    val unreadCount: Int = 0,
)

// ── Notifications ────────────────────────────────────────
@Serializable
enum class NotificationType { LIKE_RECEIVED, MATCH_CREATED }

@Serializable
data class Notification(
    val id: String,
    val recipientUserId: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val relatedListingId: String? = null,
    val relatedMatchId: String? = null,
    val timestampMs: Long,
    val isRead: Boolean = false,
)
