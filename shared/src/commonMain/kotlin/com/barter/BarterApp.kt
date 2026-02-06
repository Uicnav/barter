package com.barter

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import com.barter.core.presentation.theme.BarterCoral
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
    var screen by remember { mutableStateOf<Screen>(Screen.Discovery) }
    var previousMainScreen by remember { mutableStateOf<Screen>(Screen.Discovery) }

    val badgeVm: BadgeViewModel = remember { AppDI.get() }
    val badgeState by badgeVm.state.collectAsState()

    // Refresh badges on screen change
    LaunchedEffect(screen) { badgeVm.refresh() }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            val isMain = screen is Screen.Browse ||
                    screen is Screen.Discovery ||
                    screen is Screen.Matches ||
                    screen is Screen.Notifications ||
                    screen is Screen.Profile
            if (isMain) {
                Surface(shadowElevation = 8.dp) {
                    BarterBottomBar(
                        current = screen,
                        matchBadgeCount = badgeState.matchCount,
                        notificationBadgeCount = badgeState.notificationCount,
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
                Screen.Browse -> BrowseScreen(
                    onOpenListingDetail = {
                        previousMainScreen = Screen.Browse
                        screen = Screen.ListingDetail(it)
                    },
                    onOpenChat = { screen = Screen.Chat(it) },
                )
                Screen.Discovery -> DiscoveryScreen(
                    onOpenMatches = { screen = Screen.Matches },
                    onOpenChat = { screen = Screen.Chat(it) },
                    onCreateListing = { screen = Screen.CreateListing },
                    onOpenListingDetail = {
                        previousMainScreen = Screen.Discovery
                        screen = Screen.ListingDetail(it)
                    },
                )
                Screen.Matches -> MatchesScreen(
                    onOpenChat = { screen = Screen.Chat(it) },
                    onOpenDeal = { screen = Screen.Deal(it) },
                )
                is Screen.Chat -> ChatScreen(
                    matchId = s.matchId,
                    onBack = { screen = Screen.Matches },
                    onOpenDeal = { screen = Screen.Deal(s.matchId) },
                )
                is Screen.Deal -> DealScreen(
                    matchId = s.matchId,
                    onBack = { screen = Screen.Chat(s.matchId) },
                )
                Screen.Profile -> ProfileScreen(
                    onNavigateToMyListings = { screen = Screen.MyListings },
                    onNavigateToEditInterests = { screen = Screen.EditInterests },
                    onLogout = { /* authState will go to Unauthenticated */ },
                )
                Screen.CreateListing -> CreateListingScreen(
                    onBack = { screen = Screen.Discovery },
                )
                Screen.MyListings -> MyListingsScreen(
                    onBack = { screen = Screen.Profile },
                    onCreateListing = { screen = Screen.CreateListing },
                    onEditListing = { screen = Screen.EditListing(it) },
                )
                is Screen.EditListing -> EditListingScreen(
                    listingId = s.listingId,
                    onBack = { screen = Screen.MyListings },
                    onDeleted = { screen = Screen.MyListings },
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
                Screen.Notifications -> NotificationsScreen(
                    onBack = { screen = previousMainScreen },
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
    notificationBadgeCount: Int,
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
            selected = current is Screen.Browse,
            onClick = { onNavigate(Screen.Browse) },
            icon = { Icon(Icons.Default.GridView, "Browse") },
            label = { Text("Browse") },
            colors = colors,
        )
        NavigationBarItem(
            selected = current is Screen.Discovery,
            onClick = { onNavigate(Screen.Discovery) },
            icon = { Icon(Icons.Default.Search, "Discover") },
            label = { Text("Discover") },
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
            selected = current is Screen.Notifications,
            onClick = { onNavigate(Screen.Notifications) },
            icon = {
                BadgedBox(
                    badge = {
                        if (notificationBadgeCount > 0) {
                            Badge(containerColor = BarterCoral) {
                                Text("$notificationBadgeCount")
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, "Alerts")
                }
            },
            label = { Text("Alerts") },
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
