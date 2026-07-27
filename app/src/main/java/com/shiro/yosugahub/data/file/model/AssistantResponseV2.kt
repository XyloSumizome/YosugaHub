package com.shiro.yosugahub.data.file.model

import kotlinx.serialization.SerialName
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
    /** 文書の分類結果(v4.1)。他の提案と違い、取込時に文書へ適用して「確認待ち」にする。 */
    val classifications: List<ClassificationProposal> = emptyList(),
    /** 各ゲームの Claude Code への指示書(v4.2)。承認するとサーバーへ配信される。 */
    val directives: List<DirectiveProposal> = emptyList(),
    /**
     * 1日のセッション記録(2026-07-27)。他の提案と違い、**承認待ちに積まず
     * その場で Obsidian へ書く**。理由は下の [SessionProposal] を参照。
     */
    val session: List<SessionProposal> = emptyList(),
)

/**
 * 1日のセッション記録(2026-07-27)。ヨスガが会話の終わりに出す。
 *
 * **承認を挟まない。** これは「Hub のデータを変える提案」ではなく
 * **その日の記録そのもの**で、v5 の方針どおり正本は Obsidian にある。
 * 承認待ちに積んでも、シロさんは中身を読まずに承認するだけになる
 * ——判断の余地が無いものに承認画面を通させない。
 * (`classifications` を pending_proposals に積まないのと同じ理由。)
 *
 * **Frontmatter は Hub が組む**([ConversationNoteBuilder.buildSession])。
 * ヨスガには構造化された値だけを出させ、YAML を書かせない。
 * 引用符やコロンの扱いで壊れると Obsidian 側が読めなくなるため。
 */
@Serializable
data class SessionProposal(
    /** `2026-07-27`。空なら取り込んだ日を使う。 */
    val date: String = "",
    /** 触れたゲームの表示名(ANRI / Kamieru / GengeKyou / Yosuga Hub)。 */
    val games: List<String> = emptyList(),
    /** `game-dev` / `chat` / `mixed`。 */
    val category: String = "",
    val tags: List<String> = emptyList(),
    /** 見出し付きの Markdown 本文。Frontmatter は含めない。 */
    val body: String = "",
)

/**
 * ゲーム側の Claude Code への指示書(v4.2)。
 * 承認されるまで配信しない(勝手に作業を始めさせない)。
 * body は Markdown。Claude Code がそのまま読める粒度で書かせる。
 */
@Serializable
data class DirectiveProposal(
    val projectId: String = "",
    val title: String = "",
    val body: String = "",
    val priority: String = "medium",
)

/**
 * AI分類結果(v4.1)。設計書の例に合わせて snake_case で受ける。
 * 承認は文書の詳細画面で行うため、pending_proposals には積まない。
 */
@Serializable
data class ClassificationProposal(
    @SerialName("document_id") val documentId: String = "",
    @SerialName("project_ids") val projectIds: List<String> = emptyList(),
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    @SerialName("document_type") val documentType: String = "",
    val summary: String = "",
    @SerialName("related_entities") val relatedEntities: List<RelatedRefImport> = emptyList(),
    val confidence: Double? = null,
)

/** 分類が参照する関連実体。type は自由文字列(既存 EntityType は拡張しない)。 */
@Serializable
data class RelatedRefImport(
    val type: String = "",
    val id: String = "",
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

/** 観察日誌の提案(ヨスガ視点の本文)。承認で diary_entries へ。 */
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
