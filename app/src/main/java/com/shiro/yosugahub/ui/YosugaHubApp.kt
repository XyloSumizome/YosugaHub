package com.shiro.yosugahub.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiro.yosugahub.ui.navigation.ObsidianContextRoute
import com.shiro.yosugahub.ui.navigation.ProjectDetailRoute
import com.shiro.yosugahub.ui.navigation.YosugaDestination
import com.shiro.yosugahub.ui.screen.assistant.AssistantScreen
import com.shiro.yosugahub.ui.screen.calendar.CalendarScreen
import com.shiro.yosugahub.ui.screen.home.HomeScreen
import com.shiro.yosugahub.ui.screen.obsidiancontext.ObsidianContextScreen
import com.shiro.yosugahub.ui.screen.projectdetail.ProjectDetailScreen
import com.shiro.yosugahub.ui.screen.projects.ProjectsScreen
import com.shiro.yosugahub.ui.screen.records.RecordsScreen
import com.shiro.yosugahub.ui.screen.settings.SettingsScreen

@Composable
fun YosugaHubApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val navLineColor = MaterialTheme.colorScheme.outline

    Scaffold(
        bottomBar = {
            // 上辺に細い線を1本引いた計器風のバー(ピル型の選択インジケータは消す)。
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = navLineColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f,
                    )
                },
            ) {
                YosugaDestination.entries.forEach { destination ->
                    // 詳細画面(ネストルート)では親タブを選択状態のままにする
                    val selected = currentRoute == destination.route ||
                        (destination == YosugaDestination.Projects &&
                            currentRoute == ProjectDetailRoute.PATTERN) ||
                        (destination == YosugaDestination.Assistant &&
                            currentRoute == ObsidianContextRoute.PATTERN)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = YosugaDestination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(YosugaDestination.Home.route) { HomeScreen() }
            composable(YosugaDestination.Calendar.route) { CalendarScreen() }
            composable(YosugaDestination.Projects.route) {
                ProjectsScreen(
                    onProjectClick = { projectId ->
                        navController.navigate(ProjectDetailRoute.create(projectId))
                    },
                )
            }
            composable(ProjectDetailRoute.PATTERN) {
                ProjectDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(YosugaDestination.Records.route) { RecordsScreen() }
            composable(YosugaDestination.Assistant.route) {
                AssistantScreen(
                    onOpenObsidianContext = {
                        navController.navigate(ObsidianContextRoute.PATTERN)
                    },
                )
            }
            composable(ObsidianContextRoute.PATTERN) {
                ObsidianContextScreen(onBack = { navController.popBackStack() })
            }
            composable(YosugaDestination.Settings.route) { SettingsScreen() }
        }
    }
}
