package com.shiro.yosugahub.domain.model

/** 情報アイテムの種別(v3.1)。決定事項ログ=DECISION、保留アイデア=IDEA。 */
enum class ItemKind(val dbValue: String) {
    MEMO("memo"),
    IDEA("idea"),
    DECISION("decision"),
    SHOPPING("shopping"),
    TECH("tech"),
    OTHER("other");

    companion object {
        /** 未知の値は OTHER へフォールバックしクラッシュさせない。 */
        fun fromDb(value: String): ItemKind =
            entries.firstOrNull { it.dbValue == value } ?: OTHER
    }
}

/** アイテムに関連付ける実体への参照(名前 + 種別)。 */
data class EntityRef(
    val name: String,
    val type: EntityType,
)

/**
 * 情報アイテム(v3.1 の中核)。会話から AI が抽出し、承認を経て保存される。
 * タグ・エンティティは名前で参照する(タグ管理は AI の仕事、Hub は保存・表示のみ)。
 */
data class KnowledgeItem(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val body: String,
    val tags: List<String>,
    val entities: List<EntityRef>,
    val createdAt: String,   // ISO 8601
    val updatedAt: String,   // ISO 8601
    val source: String,      // assistant / manual
)
