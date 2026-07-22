package com.shiro.yosugahub.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shiro.yosugahub.ui.navigation.YosugaDestination
import com.shiro.yosugahub.ui.screen.assistant.AssistantScreen
import com.shiro.yosugahub.ui.screen.calendar.CalendarScreen
import com.shiro.yosugahub.ui.screen.home.HomeScreen
import com.shiro.yosugahub.ui.screen.projects.ProjectsScreen
import com.shiro.yosugahub.ui.screen.settings.SettingsScreen

@Composable
fun YosugaHubApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                YosugaDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
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
            composable(YosugaDestination.Projects.route) { ProjectsScreen() }
            composable(YosugaDestination.Assistant.route) { AssistantScreen() }
            composable(YosugaDestination.Settings.route) { SettingsScreen() }
        }
    }
}
