package com.shiro.yosugahub.ui.navigation

/** プロジェクト詳細のネストルート(下部ナビ外。v3-Step 1-b で初導入)。 */
object ProjectDetailRoute {
    const val ARG_PROJECT_ID = "projectId"
    const val PATTERN = "project_detail/{$ARG_PROJECT_ID}"

    fun create(projectId: String): String = "project_detail/$projectId"
}
