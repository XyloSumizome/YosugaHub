package com.shiro.yosugahub.data.file.model

import kotlinx.serialization.Serializable

/**
 * ChatGPT(ヨスガ)→ アプリへ返す回答JSON v2(v3-Step 2 設計)。
 * v1 の recommendations に代わり、proposals で複数種類の提案を運ぶ。
 * 未知の項目は無視。必須は schemaVersion のみで、各項目はデフォルト値付き。
 */
@Serializable
data class AssistantResponseV2(
    val schemaVersion: Int,
    val generatedAt: String = "",
    val summary: String = "",
    val proposals: ProposalsImport = ProposalsImport(),
)

@Serializable
data class ProposalsImport(
    val tasks: List<TaskProposal> = emptyList(),
    val items: List<ItemProposal> = emptyList(),
    val diary: List<DiaryProposal> = emptyList(),
    val projectHealth: List<HealthProposal> = emptyList(),
)

/** タスク提案。承認で tasks テーブルへ(source=assistant)。 */
@Serializable
data class TaskProposal(
    val projectId: String? = null,
    val title: String = "",
    val detail: String = "",
    val priority: String = "medium",
    val dueDate: String? = null,
)

/**
 * 情報アイテム提案。承認で knowledge_items + タグ・実体へ。
 * targetNote が指定されていれば、承認時に Obsidian Vault の該当ノートへも追記する(v3-Step 3)。
 */
@Serializable
data class ItemProposal(
    val kind: String = "memo",
    val title: String = "",
    val body: String = "",
    val tags: List<String> = emptyList(),
    val entities: List<EntityRefImport> = emptyList(),
    val targetNote: String = "",
)

@Serializable
data class EntityRefImport(
    val name: String = "",
    val type: String = "other",
)

/** 観察日記の提案(ヨスガ視点の本文)。承認で diary_entries へ。 */
@Serializable
data class DiaryProposal(
    val date: String = "",
    val body: String = "",
)

/** プロジェクト健康状態の更新提案。承認で projects.health へ反映。 */
@Serializable
data class HealthProposal(
    val projectId: String = "",
    val health: String = "",
    val reason: String = "",
)
