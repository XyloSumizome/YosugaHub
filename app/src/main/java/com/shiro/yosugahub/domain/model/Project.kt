package com.shiro.yosugahub.domain.model

/** ゲーム制作プロジェクトの進捗概要。Phase 3 で GitHub 由来の status.json に置き換える。 */
data class Project(
    val id: String,
    val name: String,
    val currentGoal: String,
    val inProgress: String,
    val nextTask: String,
    val lastUpdated: String,
    val health: String,
)
