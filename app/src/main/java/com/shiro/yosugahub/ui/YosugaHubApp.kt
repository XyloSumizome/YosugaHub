package com.shiro.yosugahub.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shiro.yosugahub.ui.component.SubScreenScaffold
import com.shiro.yosugahub.ui.navigation.ObsidianContextRoute
import com.shiro.yosugahub.ui.navigation.ProjectDetailRoute
import com.shiro.yosugahub.ui.navigation.YosugaDestination
import com.shiro.yosugahub.ui.screen.assistant.AssistantScreen
import com.shiro.yosugahub.ui.screen.calendar.CalendarScreen
import com.shiro.yosugahub.ui.screen.console.ConsoleScreen
import com.shiro.yosugahub.ui.screen.obsidiancontext.ObsidianContextScreen
import com.shiro.yosugahub.ui.screen.projectdetail.ProjectDetailScreen
import com.shiro.yosugahub.ui.screen.projects.ProjectsScreen
import com.shiro.yosugahub.ui.screen.records.RecordsScreen
import com.shiro.yosugahub.ui.screen.settings.SettingsScreen

/**
 * ルートは**下部ナビの無いコンソール**(v5 UI)。画面を移動して回るのではなく、
 * コンソールからコマンドを実行 / サブ画面を開く。サブ画面は上辺の [ < BACK ] で戻る。
 */
@Composable
fun YosugaHubApp() {
    val navController = rememberNavController()

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // 下部ナビ(Scaffold)を廃したので、ステータスバー等のインセットはここで確保する。
        NavHost(
            navController = navController,
            startDestination = YosugaDestination.Console.route,
            modifier = Modifier.systemBarsPadding(),
        ) {
            composable(YosugaDestination.Console.route) {
                ConsoleScreen(
                    onOpenProjects = { navController.navigate(YosugaDestination.Projects.route) },
                    onOpenRecords = { navController.navigate(YosugaDestination.Records.route) },
                    onOpenCalendar = { navController.navigate(YosugaDestination.Calendar.route) },
                    onOpenSettings = { navController.navigate(YosugaDestination.Settings.route) },
                    onOpenContext = { navController.navigate(ObsidianContextRoute.PATTERN) },
                    onOpenReview = { navController.navigate(YosugaDestination.Review.route) },
                )
            }

            composable(YosugaDestination.Projects.route) {
                SubScreenScaffold("PROJECTS", onBack = { navController.popBackStack() }) {
                    ProjectsScreen(
                        onProjectClick = { id -> navController.navigate(ProjectDetailRoute.create(id)) },
                    )
                }
            }
            composable(ProjectDetailRoute.PATTERN) {
                ProjectDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(YosugaDestination.Records.route) {
                SubScreenScaffold("RECORDS", onBack = { navController.popBackStack() }) {
                    RecordsScreen()
                }
            }
            composable(YosugaDestination.Calendar.route) {
                SubScreenScaffold("CALENDAR", onBack = { navController.popBackStack() }) {
                    CalendarScreen()
                }
            }
            composable(YosugaDestination.Settings.route) {
                SubScreenScaffold("SETTINGS", onBack = { navController.popBackStack() }) {
                    SettingsScreen()
                }
            }
            composable(YosugaDestination.Review.route) {
                SubScreenScaffold("REVIEW", onBack = { navController.popBackStack() }) {
                    AssistantScreen(
                        onOpenObsidianContext = { navController.navigate(ObsidianContextRoute.PATTERN) },
                    )
                }
            }
            composable(ObsidianContextRoute.PATTERN) {
                ObsidianContextScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
