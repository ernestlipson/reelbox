package dev.reelbox.ui

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.reelbox.data.Movie
import dev.reelbox.ui.theme.Accent
import dev.reelbox.ui.theme.Ink
import dev.reelbox.ui.theme.TextPrimary
import dev.reelbox.ui.theme.TextSecondary

data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

val BottomNavItems = listOf(
    NavItem("home", "Home", Icons.Filled.Home),
    NavItem("search", "Search", Icons.Filled.Search),
    NavItem("watchlist", "Watchlist", Icons.Filled.Bookmark),
)

@Composable
fun ReelboxApp() {
    val nav = rememberNavController()
    var selectedIndex by remember { mutableStateOf(0) }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isPlayerScreen = currentRoute?.startsWith("player") == true

    fun open(movie: Movie) {
        val type = if (movie.isTv) "tv" else "movie"
        nav.navigate("detail/$type/${movie.id}")
    }

    fun play(type: String, id: Int, title: String) {
        val encodedTitle = Uri.encode(title)
        nav.navigate("player/$type/$id?title=$encodedTitle")
    }

    // Height for Android system navigation bar (3-button or gesture)
    val navBarHeight = 56.dp

    Scaffold(
        bottomBar = {
            if (!isPlayerScreen) {
                BottomNavigation(
                    backgroundColor = Ink,
                    contentColor = TextPrimary,
                    modifier = Modifier.padding(bottom = navBarHeight),
                ) {
                    BottomNavItems.forEachIndexed { index, item ->
                        val isSelected = selectedIndex == index
                        BottomNavigationItem(
                            selected = isSelected,
                            onClick = {
                                selectedIndex = index
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) Accent else TextSecondary,
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Accent else TextSecondary,
                                )
                            },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.padding(if (isPlayerScreen) PaddingValues(0.dp) else innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    onOpen = { open(it) },
                    onSearch = { selectedIndex = 1; nav.navigate("search") },
                    onWatchlist = { selectedIndex = 2; nav.navigate("watchlist") },
                    onPlay = { movie -> play(if (movie.isTv) "tv" else "movie", movie.id, movie.displayTitle) },
                )
            }
            composable("search") {
                SearchScreen(
                    onOpen = { open(it) },
                    onBack = { selectedIndex = 0; nav.popBackStack() },
                )
            }
            composable("watchlist") {
                WatchlistScreen(
                    onOpen = { type, id -> nav.navigate("detail/$type/$id") },
                    onBack = { selectedIndex = 0; nav.popBackStack() },
                )
            }
            composable(
                route = "detail/{type}/{id}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.IntType },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "movie"
                val id = entry.arguments?.getInt("id") ?: 0
                DetailScreen(
                    type = type,
                    id = id,
                    onBack = { nav.popBackStack() },
                    onOpen = { t, i -> nav.navigate("detail/$t/$i") },
                    onPlay = { t, i, title -> play(t, i, title) },
                )
            }
            composable(
                route = "player/{type}/{id}?title={title}",
                arguments = listOf(
                    navArgument("type") { type = NavType.StringType },
                    navArgument("id") { type = NavType.IntType },
                    navArgument("title") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val type = entry.arguments?.getString("type") ?: "movie"
                val id = entry.arguments?.getInt("id") ?: 0
                val title = entry.arguments?.getString("title") ?: ""
                PlayerScreen(
                    type = type,
                    id = id,
                    title = title,
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
