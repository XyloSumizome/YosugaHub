package com.shiro.yosugahub.data.file.model

import kotlinx.serialization.Serializable

/**
 * ChatGPT(ヨスガ)→ アプリへ返す回答JSON(設計書2.3「ChatGPTからアプリへ」)。
 * 未知の項目は無視できるようにする(設計書15章)。必須は schemaVersion のみ。
 */
@Serializable
data class AssistantResponse(
    val schemaVersion: Int,
    val generatedAt: String = "",
    val summary: String = "",
    val recommendations: List<RecommendationImport> = emptyList(),
    val suggestedTasks: List<SuggestedTaskImport> = emptyList(),
    val notes: List<String> = emptyList(),
)

@Serializable
data class RecommendationImport(
    val projectId: String = "",
    val title: String = "",
    val detail: String = "",
    val priority: String = "medium",
)

@Serializable
data class SuggestedTaskImport(
    val projectId: String = "",
    val title: String = "",
    val detail: String = "",
    val estimatedMinutes: Int? = null,
)

/** schemaVersion だけを先読みして対応可否を判定するための最小モデル。 */
@Serializable
data class SchemaProbe(
    val schemaVersion: Int? = null,
)
