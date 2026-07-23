package com.shiro.yosugahub.data.file.model

import kotlinx.serialization.Serializable

/**
 * アプリ → ChatGPT(ヨスガ)へ渡す状況JSON(設計書2.3「アプリからChatGPTへ」)。
 * schemaVersion を必ず含める(設計書15章)。
 */
@Serializable
data class ContextExport(
    val schemaVersion: Int = SCHEMA_VERSION,
    val generatedAt: String,
    val userContext: UserContext,
    val calendar: CalendarExport,
    val projects: List<ProjectExport>,
    val recentAssistantExchange: String? = null,
) {
    companion object {
        /** このアプリが出力する状況JSONのスキーマ版。 */
        const val SCHEMA_VERSION = 1
    }
}

@Serializable
data class UserContext(
    val purpose: String,
)

@Serializable
data class CalendarExport(
    val pastDays: Int,
    val futureDays: Int,
    val events: List<EventExport>,
)

@Serializable
data class EventExport(
    val title: String,
    val start: String,
    val end: String,
    val calendarName: String,
    val description: String = "",
)

@Serializable
data class ProjectExport(
    val id: String,
    val name: String,
    val statusMarkdown: String,
    val lastUpdated: String,
)
