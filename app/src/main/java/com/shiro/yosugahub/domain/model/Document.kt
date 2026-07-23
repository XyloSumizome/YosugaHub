package com.shiro.yosugahub.domain.model

/**
 * 文書の状態(v4.1 AI分類ワークフロー)。
 * 保存 → unclassified → (アップロード) → classification_pending → (分類結果取込) → needs_review
 * → (承認・修正) → classified → (archived)。保留は needs_review のまま据え置き。
 */
enum class DocumentStatus(val dbValue: String) {
    UNCLASSIFIED("unclassified"),
    CLASSIFICATION_PENDING("classification_pending"),
    NEEDS_REVIEW("needs_review"),
    CLASSIFIED("classified"),
    ARCHIVED("archived");

    companion object {
        /** 未知の値は UNCLASSIFIED へフォールバックしクラッシュさせない。 */
        fun fromDb(value: String): DocumentStatus =
            entries.firstOrNull { it.dbValue == value } ?: UNCLASSIFIED
    }
}

/** 分類レコードの適用者。AI結果とユーザー修正を別レコードとして区別する。 */
enum class ClassificationOrigin(val dbValue: String) {
    AI("ai"),
    USER("user");

    companion object {
        /** 未知の値は AI(未確認扱い)へフォールバックしクラッシュさせない。 */
        fun fromDb(value: String): ClassificationOrigin =
            entries.firstOrNull { it.dbValue == value } ?: AI
    }
}

/**
 * 分類が参照する関連実体(v4.1 の related_entities)。
 * type は自由文字列(例: "feature")。既存 EntityType は拡張しない(論点5の合意)。
 */
data class RelatedRef(
    val type: String,
    val id: String,
)

/**
 * 分類レコード(v4.1)。AI結果・ユーザー修正の両方をこの形で積み、履歴になる。
 * 現行の1件だけ isCurrent = true。文書の原文はこのレコードでは一切変更しない。
 */
data class DocumentClassification(
    val id: String,
    val documentId: String,
    val summary: String,
    val documentType: String,     // 例: design-discussion(自由文字列)
    val confidence: Double?,      // AIの信頼度 0..1。ユーザー修正レコードは null
    val projectIds: List<String>,
    val categories: List<String>,
    val tags: List<String>,
    val relatedEntities: List<RelatedRef>,
    val classifiedAt: String,     // ISO 8601
    val origin: ClassificationOrigin,
    val isCurrent: Boolean,
)

/**
 * 未整理文書(v4.1)。原文を先に保存し、後から AI が分類する。
 * body は原文で、AIの判断によって上書きしない(最重要原則)。
 */
data class Document(
    val id: String,
    val title: String,
    val body: String,
    val status: DocumentStatus,
    val createdAt: String,   // ISO 8601
    val updatedAt: String,   // ISO 8601(状態・分類の変化で刻む。body は不変)
    val source: String,      // manual / share
    val currentClassification: DocumentClassification?,
    /** 分類履歴(新しい順)。現行分類も含む。 */
    val classificationHistory: List<DocumentClassification> = emptyList(),
)
