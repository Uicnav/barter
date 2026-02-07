package com.barter.core.presentation.navigation

sealed class Screen {
    // Auth
    data object Login : Screen()
    data object Register : Screen()
    data object SelectInterests : Screen()

    // Main tabs (bottom nav)
    data object Swipe : Screen()
    data object Matches : Screen()
    data object Publish : Screen()
    data object ChatList : Screen()
    data object Profile : Screen()

    // Secondary (accessible from main screens)
    data object Browse : Screen()

    // Detail
    data class Chat(val matchId: String) : Screen()
    data class Deal(val matchId: String) : Screen()

    // Notifications
    data object Notifications : Screen()

    // Listing & Interests management
    data object CreateListing : Screen()
    data object MyListings : Screen()
    data object EditInterests : Screen()
    data class EditListing(val listingId: String) : Screen()
    data class ListingDetail(val listingId: String) : Screen()
    data class UserReviews(val userId: String) : Screen()
}
