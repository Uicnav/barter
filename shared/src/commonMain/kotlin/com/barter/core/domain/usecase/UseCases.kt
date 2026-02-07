package com.barter.core.domain.usecase

import com.barter.core.domain.model.*
import com.barter.core.domain.repo.BarterRepository

class LoadDiscoveryUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(interestFilter: List<String> = emptyList()): List<Listing> =
        repo.getDiscovery(interestFilter)
}

class SwipeUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(listingId: String, action: SwipeAction): Match? =
        repo.swipe(listingId, action)
}

class LoadMatchesUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): List<Match> = repo.getMatches()
}

class SendMessageUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(matchId: String, text: String) =
        repo.sendMessage(matchId, text)
}

class ProposeDealUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(
        matchId: String, offer: List<DealItem>, request: List<DealItem>,
        cashTopUp: Double = 0.0, note: String = "",
    ) = repo.proposeDeal(matchId, offer, request, cashTopUp, note)
}

class UpdateDealStatusUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(dealId: String, status: DealStatus) =
        repo.updateDealStatus(dealId, status)
}

// ── Auth ──────────────────────────────────────────────────
class LoginUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(email: String, password: String): Result<UserProfile> =
        repo.login(email, password)
}

class RegisterUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(request: RegistrationRequest): Result<UserProfile> =
        repo.register(request)
}

class LogoutUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke() = repo.logout()
}

// ── Interests ─────────────────────────────────────────────
class UpdateInterestsUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(interests: List<String>) =
        repo.updateInterests(interests)
}

// ── Listings ──────────────────────────────────────────────
class CreateListingUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(
        title: String, description: String, kind: ListingKind,
        tags: List<String>, estimatedValue: Double? = null,
        imageUrl: String = "", validUntilMs: Long? = null,
    ): Listing = repo.createListing(title, description, kind, tags, estimatedValue, imageUrl, validUntilMs)
}

class UpdateListingUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(
        listingId: String, title: String, description: String,
        kind: ListingKind, tags: List<String>, estimatedValue: Double? = null,
        imageUrl: String = "", validUntilMs: Long? = null,
    ): Listing = repo.updateListing(listingId, title, description, kind, tags, estimatedValue, imageUrl, validUntilMs)
}

class DeleteListingUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(listingId: String) = repo.deleteListing(listingId)
}

class ToggleListingVisibilityUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(listingId: String): Listing = repo.toggleListingVisibility(listingId)
}

class RenewListingUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(listingId: String, newValidUntilMs: Long): Listing =
        repo.renewListing(listingId, newValidUntilMs)
}

class GetListingByIdUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(listingId: String): Listing? = repo.getListingById(listingId)
}

class LoadMyListingsUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): List<Listing> = repo.getMyListings()
}

class UpdateAvailabilityUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(listingId: String, availability: AvailabilityStatus): Listing =
        repo.updateAvailability(listingId, availability)
}

class SearchListingsUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(
        query: String, category: String? = null, sortBy: SortOption = SortOption.NEWEST,
    ): List<Listing> = repo.searchListings(query, category, sortBy)
}

// ── Balance / credits ─────────────────────────────────────
class TopUpBalanceUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(amount: Double): UserProfile = repo.topUpBalance(amount)
}

class GetBalanceUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): Double = repo.getBalance()
}

// ── Enriched matches & stats ─────────────────────────────
class LoadEnrichedMatchesUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): List<EnrichedMatch> = repo.getEnrichedMatches()
}

class LoadProfileStatsUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): ProfileStats = repo.getProfileStats()
}

class GetUnreadCountsUseCase(private val repo: BarterRepository) {
    suspend fun matchCount(): Int = repo.getUnreadMatchCount()
    suspend fun messageCount(): Int = repo.getTotalUnreadMessageCount()
}

// ── Geocode ──────────────────────────────────────────────
class AutocompleteLocationUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(query: String): List<GeocodeSuggestion> =
        repo.autocompleteLocation(query)
}

// ── Notifications ────────────────────────────────────────
class LoadNotificationsUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): List<Notification> = repo.getNotifications()
}

class GetUnreadNotificationCountUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(): Int = repo.getUnreadNotificationCount()
}

class MarkNotificationReadUseCase(private val repo: BarterRepository) {
    suspend operator fun invoke(notificationId: String) = repo.markNotificationRead(notificationId)
}
