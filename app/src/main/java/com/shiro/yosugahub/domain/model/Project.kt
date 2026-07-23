package com.shiro.yosugahub.domain.model

/**
 * ゲーム制作プロジェクトの進捗概要。
 * repo* は GitHub の `.yosuga/status.json` 取得先(未設定なら null = 取得対象外)。
 */
data class Project(
    val id: String,
    val name: String,
    val currentGoal: String,
    val inProgress: String,
    val nextTask: String,
    val lastUpdated: String,
    val health: String,
    val repoOwner: String? = null,
    val repoName: String? = null,
    val repoBranch: String? = null,
) {
    /** GitHub 取得に必要な情報が揃っているか。 */
    val hasRepository: Boolean
        get() = !repoOwner.isNullOrBlank() && !repoName.isNullOrBlank()
}
