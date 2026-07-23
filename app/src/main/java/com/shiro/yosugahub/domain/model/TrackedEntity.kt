package com.shiro.yosugahub.domain.model

/** 実体の種別(v3.1 4章)。 */
enum class EntityType(val dbValue: String) {
    PROJECT("project"),
    PERSON("person"),
    TECH("tech"),
    GEAR("gear"),
    EVENT("event"),
    OTHER("other");

    companion object {
        fun fromDb(value: String): EntityType =
            entries.firstOrNull { it.dbValue == value } ?: OTHER
    }
}

/**
 * タグとは別に管理する「実体」(プロジェクト / 人物 / 技術 / 機材 / イベント)。
 * 会話から AI が関連付ける。Room の Entity と紛らわしいため Tracked を冠する。
 */
data class TrackedEntity(
    val id: String,
    val name: String,
    val type: EntityType,
)
