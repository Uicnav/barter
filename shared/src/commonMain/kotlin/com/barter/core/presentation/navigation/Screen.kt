package com.barter.core.presentation.navigation

sealed class Screen {
    // Auth
    data object Login : Screen()
    data object Register : Screen()
    data object SelectInterests : Screen()

    // Main
    data object Browse : Screen()
    data object Discovery : Screen()
    data object Matches : Screen()
    data object Profile : Screen()

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
}
