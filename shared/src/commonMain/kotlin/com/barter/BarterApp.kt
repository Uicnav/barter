package com.barter

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.barter.core.di.AppDI
import com.barter.core.domain.model.AuthState
import com.barter.core.presentation.navigation.Screen
import com.barter.core.presentation.screens.*
import com.barter.core.presentation.theme.BarterTeal
import com.barter.core.presentation.theme.BarterTheme
import com.barter.core.presentation.vm.AuthViewModel
import com.barter.core.presentation.vm.BadgeViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

@Composable
fun BarterAppRoot(platformModule: Module = module {}) {
    remember { AppDI.init(platformModule) }

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    val authVm: AuthViewModel = remember { AppDI.get() }
    val authState by authVm.authState.collectAsState()

    BarterTheme {
        when (val state = authState) {
            is AuthState.Unauthenticated, is AuthState.Error -> {
                AuthFlow()
            }
            is AuthState.Loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BarterTeal)
                }
            }
            is AuthState.Authenticated -> {
                if (!state.user.hasSelectedInterests) {
                    SelectInterestsScreen(
                        isOnboarding = true,
                        onDone = {
                            // Interests saved — authState will update with hasSelectedInterests = true
                        },
                    )
                } else {
                    MainApp()
                }
            }
        }
    }
}

@Composable
private fun AuthFlow() {
    var screen by remember { mutableStateOf<Screen>(Screen.Login) }

    when (screen) {
        Screen.Login -> LoginScreen(
            onNavigateToRegister = { screen = Screen.Register },
        )
        Screen.Register -> RegisterScreen(
            onBack = { screen = Screen.Login },
        )
        else -> LoginScreen(
            onNavigateToRegister = { screen = Screen.Register },
        )
    }
}

@Composable
private fun MainApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Swipe) }
    var previousMainScreen by remember { mutableStateOf<Screen>(Screen.Swipe) }

    val badgeVm: BadgeViewModel = remember { AppDI.get() }
    val badgeState by badgeVm.state.collectAsState()

    // Refresh badges on screen change
    LaunchedEffect(screen) { badgeVm.refresh() }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            val isMain = screen is Screen.Swipe ||
                    screen is Screen.Matches ||
                    screen is Screen.Publish ||
                    screen is Screen.ChatList ||
                    screen is Screen.Profile
            if (isMain) {
                Surface(shadowElevation = 8.dp) {
                    BarterBottomBar(
                        current = screen,
                        matchBadgeCount = badgeState.matchCount,
                        unreadMessageCount = badgeState.notificationCount,
                        onNavigate = { screen = it },
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val s = screen) {
                Screen.Swipe -> DiscoveryScreen(
                    onOpenMatches = { screen = Screen.Matches },
                    onOpenChat = { screen = Screen.Chat(it) },
                    onOpenBrowse = { screen = Screen.Browse },
                    onOpenListingDetail = {
                        previousMainScreen = Screen.Swipe
                        screen = Screen.ListingDetail(it)
                    },
                )
                Screen.Matches -> MatchesScreen(
                    onOpenChat = { screen = Screen.Chat(it) },
                    onOpenDeal = { screen = Screen.Deal(it) },
                )
                Screen.Publish -> MyListingsScreen(
                    onBack = null,
                    onCreateListing = { screen = Screen.CreateListing },
                    onEditListing = { screen = Screen.EditListing(it) },
                )
                Screen.ChatList -> ChatListScreen(
                    onOpenChat = { screen = Screen.Chat(it) },
                )
                Screen.Profile -> {
                    val authVmLocal: AuthViewModel = remember { AppDI.get() }
                    val currentUser by authVmLocal.currentUser.collectAsState()
                    ProfileScreen(
                        onNavigateToEditInterests = { screen = Screen.EditInterests },
                        onNavigateToReviews = { screen = Screen.UserReviews(currentUser.id) },
                        onNavigateToNotifications = { screen = Screen.Notifications },
                        onLogout = { /* authState will go to Unauthenticated */ },
                    )
                }
                Screen.Browse -> BrowseScreen(
                    onBack = { screen = Screen.Swipe },
                    onOpenListingDetail = {
                        previousMainScreen = Screen.Swipe
                        screen = Screen.ListingDetail(it)
                    },
                    onOpenChat = { screen = Screen.Chat(it) },
                )
                is Screen.Chat -> ChatScreen(
                    matchId = s.matchId,
                    onBack = { screen = Screen.ChatList },
                    onOpenDeal = { screen = Screen.Deal(s.matchId) },
                )
                is Screen.Deal -> DealScreen(
                    matchId = s.matchId,
                    onBack = { screen = Screen.Chat(s.matchId) },
                )
                Screen.CreateListing -> CreateListingScreen(
                    onBack = { screen = Screen.Publish },
                )
                Screen.MyListings -> MyListingsScreen(
                    onBack = { screen = Screen.Profile },
                    onCreateListing = { screen = Screen.CreateListing },
                    onEditListing = { screen = Screen.EditListing(it) },
                )
                is Screen.EditListing -> EditListingScreen(
                    listingId = s.listingId,
                    onBack = { screen = Screen.Publish },
                    onDeleted = { screen = Screen.Publish },
                )
                is Screen.ListingDetail -> ListingDetailScreen(
                    listingId = s.listingId,
                    onBack = { screen = previousMainScreen },
                    onOpenChat = { screen = Screen.Chat(it) },
                )
                Screen.EditInterests -> SelectInterestsScreen(
                    isOnboarding = false,
                    onDone = { screen = Screen.Profile },
                    onBack = { screen = Screen.Profile },
                )
                is Screen.UserReviews -> UserReviewsScreen(
                    userId = s.userId,
                    onBack = { screen = Screen.Profile },
                )
                Screen.Notifications -> NotificationsScreen(
                    onBack = { screen = Screen.Profile },
                )
                // Auth screens handled by AuthFlow, not MainApp
                Screen.Login, Screen.Register, Screen.SelectInterests -> {}
            }
        }
    }
}

@Composable
private fun BarterBottomBar(
    current: Screen,
    matchBadgeCount: Int,
    unreadMessageCount: Int,
    onNavigate: (Screen) -> Unit,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        tonalElevation = 0.dp,
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = BarterTeal,
            selectedTextColor = BarterTeal,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            indicatorColor = BarterTeal.copy(alpha = 0.12f),
        )

        NavigationBarItem(
            selected = current is Screen.Swipe,
            onClick = { onNavigate(Screen.Swipe) },
            icon = { Icon(Icons.Default.Explore, "Swipe") },
            label = { Text("Swipe") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current is Screen.Matches,
            onClick = { onNavigate(Screen.Matches) },
            icon = {
                BadgedBox(
                    badge = {
                        if (matchBadgeCount > 0) {
                            Badge(containerColor = BarterTeal) {
                                Text("$matchBadgeCount")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Favorite, "Matches")
                }
            },
            label = { Text("Matches") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current is Screen.Publish,
            onClick = { onNavigate(Screen.Publish) },
            icon = { Icon(Icons.Default.AddCircle, "Publish") },
            label = { Text("Publish") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current is Screen.ChatList,
            onClick = { onNavigate(Screen.ChatList) },
            icon = {
                BadgedBox(
                    badge = {
                        if (unreadMessageCount > 0) {
                            Badge(containerColor = BarterTeal) {
                                Text("$unreadMessageCount")
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, "Chat")
                }
            },
            label = { Text("Chat") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current is Screen.Profile,
            onClick = { onNavigate(Screen.Profile) },
            icon = { Icon(Icons.Default.Person, "Profile") },
            label = { Text("Profile") },
            colors = colors,
        )
    }
}
