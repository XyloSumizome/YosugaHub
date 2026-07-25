package com.shiro.yosugahub.domain.model

/** status.json 由来の一行表示(作業中・次タスク・ブロッカーで共用)。 */
data class StatusLine(
    val title: String,
    val detail: String = "",
)

/**
 * status.json の `blockers` / `risks`(進行を妨げているもの)。
 *
 * [StatusLine] と分けているのは **いつから起きているかを保つ**ため(2026-07-25)。
 * 「先週から止まっている」と「今朝出たばかり」は、読み手にとって別の情報。
 */
data class StatusBlockerLine(
    val title: String,
    val detail: String = "",
    val severity: String = "",
    /** "yyyy-MM-dd"。ゲーム側が書かなければ空。 */
    val since: String = "",
)

/**
 * status.json の `recentChanges`(各ゲームの Claude Code が書く修正のログ)。
 *
 * [StatusLine] と分けているのは **日付で絞れるようにする**ため。
 * 近況報告では「直近2週間」だけを載せたい(2026-07-25)。
 */
data class StatusChangeLine(
    /** "yyyy-MM-dd"。ゲーム側が書かなければ空。 */
    val date: String = "",
    val summary: String,
    val commit: String = "",
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
    val blockers: List<StatusBlockerLine>,
    /**
     * ゲーム側で確定した設計判断(status.json の decisions)。
     * AIがこれに矛盾する提案をしないよう、表示とAI向けJSONの両方へ流す。
     */
    val decisions: List<StatusLine> = emptyList(),
    /**
     * ゲーム側が書いた修正のログ(status.json の recentChanges)。
     * 近況報告で「何がどう変わったか」を事実として渡すための材料(2026-07-25)。
     */
    val recentChanges: List<StatusChangeLine> = emptyList(),
    val questionsForYosuga: List<String>,
    val generatedAt: String,
    val sourceCommit: String,
    /** アプリが取得した時刻(ISO 8601)。status 側の generatedAt とは別物。 */
    val fetchedAt: String,
)
