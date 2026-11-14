package com.moataz.motazalqmi.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.moataz.motazalqmi.ui.MainViewModel
import com.moataz.motazalqmi.ui.screens.*

sealed class Dest(val route: String, val labelAr: String, val icon: ImageVector) {
    data object Home : Dest("home", "الرئيسية", Icons.Outlined.MenuBook)
    data object Quotes : Dest("quotes", "اقتباسات", Icons.Outlined.FormatQuote)
    data object Search : Dest("search", "بحث", Icons.Outlined.Search)
    data object Bookmarks : Dest("bookmarks", "المحفوظات", Icons.Outlined.BookmarkBorder)
    data object Settings : Dest("settings", "الإعدادات", Icons.Outlined.Settings)
}

private val bottomDestinations = listOf(Dest.Home, Dest.Quotes, Dest.Search, Dest.Bookmarks, Dest.Settings)

@Composable
fun MotazNavGraph(vm: MainViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = bottomDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    bottomDestinations.forEach { dest ->
                        val selected = backStack?.destination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(Dest.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.labelAr) },
                            label = { Text(dest.labelAr, style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Dest.Home.route) {
                HomeScreen(
                    vm = vm,
                    onArticleClick = { slug -> navController.navigate("article/$slug") },
                    onCategoryClick = { id, name -> navController.navigate("category/$id/$name") },
                    onSearchClick = { navController.navigate(Dest.Search.route) }
                )
            }
            composable(Dest.Quotes.route) { QuotesScreen(vm = vm) }
            composable(Dest.Search.route) {
                SearchScreen(vm = vm, onArticleClick = { slug -> navController.navigate("article/$slug") })
            }
            composable(Dest.Bookmarks.route) {
                BookmarksScreen(vm = vm, onArticleClick = { slug -> navController.navigate("article/$slug") })
            }
            composable(Dest.Settings.route) { SettingsScreen(vm = vm) }
            composable(
                route = "category/{id}/{name}",
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { entry ->
                CategoryScreen(
                    categoryId = entry.arguments?.getString("id").orEmpty(),
                    categoryName = entry.arguments?.getString("name").orEmpty(),
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onArticleClick = { slug -> navController.navigate("article/$slug") }
                )
            }
            composable(
                route = "article/{slug}",
                arguments = listOf(navArgument("slug") { type = NavType.StringType })
            ) { entry ->
                ArticleReaderScreen(
                    slug = entry.arguments?.getString("slug").orEmpty(),
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onNavigateToArticle = { nextSlug ->
                        navController.navigate("article/$nextSlug") {
                            popUpTo("article/{slug}") { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
