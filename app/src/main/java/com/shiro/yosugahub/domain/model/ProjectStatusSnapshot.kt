package com.shiro.yosugahub.domain.model

/** status.json 由来の一行表示(作業中・次タスク・ブロッカーで共用)。 */
data class StatusLine(
    val title: String,
    val detail: String = "",
)

/**
 * GitHub から取得した `.yosuga/status.json` の表示用スナップショット。
 * UI には常にこのドメインモデルを渡す(通信DTOを直接見せない)。
 */
data class ProjectStatusSnapshot(
    val projectId: String,
    val summary: String,
    val health: String,
    val phase: String,
    val goalTitle: String,
    val goalDetail: String,
    val inProgress: List<StatusLine>,
    val nextTasks: List<StatusLine>,
    val blockers: List<StatusLine>,
    /**
     * ゲーム側で確定した設計判断(status.json の decisions)。
     * AIがこれに矛盾する提案をしないよう、表示とAI向けJSONの両方へ流す。
     */
    val decisions: List<StatusLine> = emptyList(),
    val questionsForYosuga: List<String>,
    val generatedAt: String,
    val sourceCommit: String,
    /** アプリが取得した時刻(ISO 8601)。status 側の generatedAt とは別物。 */
    val fetchedAt: String,
)
