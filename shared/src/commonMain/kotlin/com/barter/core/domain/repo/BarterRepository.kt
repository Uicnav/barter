package com.barter.core.domain.repo

import com.barter.core.domain.model.*
import kotlinx.coroutines.flow.Flow

interface BarterRepository {
    // Auth
    val authState: Flow<AuthState>
    val currentUser: Flow<UserProfile>
    suspend fun login(email: String, password: String): Result<UserProfile>
    suspend fun register(request: RegistrationRequest): Result<UserProfile>
    suspend fun logout()

    // Interests
    suspend fun updateInterests(interests: List<String>)

    // Listings
    suspend fun createListing(
        title: String, description: String, kind: ListingKind,
        tags: List<String>, estimatedValue: Double? = null,
        imageUrl: String = "", validUntilMs: Long? = null,
    ): Listing
    suspend fun updateListing(
        listingId: String, title: String, description: String,
        kind: ListingKind, tags: List<String>, estimatedValue: Double? = null,
        imageUrl: String = "", validUntilMs: Long? = null,
    ): Listing
    suspend fun deleteListing(listingId: String)
    suspend fun getListingById(listingId: String): Listing?
    suspend fun getMyListings(): List<Listing>
    suspend fun toggleListingVisibility(listingId: String): Listing
    suspend fun renewListing(listingId: String, newValidUntilMs: Long): Listing

    // Balance / credits
    suspend fun topUpBalance(amount: Double): UserProfile
    suspend fun getBalance(): Double

    // Availability
    suspend fun updateAvailability(listingId: String, availability: AvailabilityStatus): Listing

    // Search / Browse
    suspend fun searchListings(query: String, category: String? = null, sortBy: SortOption = SortOption.NEWEST): List<Listing>

    // Discovery
    suspend fun getDiscovery(interestFilter: List<String> = emptyList()): List<Listing>

    // Swipe
    suspend fun swipe(listingId: String, action: SwipeAction): Match?

    // Matches
    suspend fun getMatches(): List<Match>
    suspend fun getEnrichedMatches(): List<EnrichedMatch>

    // Chat
    fun observeMessages(matchId: String): Flow<List<Message>>
    suspend fun sendMessage(matchId: String, text: String)

    // Deals
    fun observeDeals(matchId: String): Flow<List<Deal>>
    suspend fun proposeDeal(
        matchId: String, offer: List<DealItem>, request: List<DealItem>,
        cashTopUp: Double = 0.0, note: String = "",
    )
    suspend fun updateDealStatus(dealId: String, status: DealStatus)

    // Stats & badges
    suspend fun getProfileStats(): ProfileStats
    suspend fun getUnreadMatchCount(): Int
    suspend fun getTotalUnreadMessageCount(): Int

    // Geocode
    suspend fun autocompleteLocation(query: String): List<GeocodeSuggestion>

    // Notifications
    suspend fun getNotifications(): List<Notification>
    suspend fun getUnreadNotificationCount(): Int
    suspend fun markNotificationRead(notificationId: String)
}
