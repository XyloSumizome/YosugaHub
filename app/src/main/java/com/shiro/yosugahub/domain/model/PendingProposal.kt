package com.shiro.yosugahub.domain.model

/** 提案の種別(回答JSON v2 の proposals に対応)。 */
enum class ProposalType(val dbValue: String) {
    TASK("task"),
    ITEM("item"),
    DIARY("diary"),
    HEALTH("health"),
    /** 各ゲームの Claude Code への指示書(v4.2)。承認で directives へ。 */
    DIRECTIVE("directive");

    companion object {
        fun fromDb(value: String): ProposalType? =
            entries.firstOrNull { it.dbValue == value }
    }
}

/** 提案の状態。棄却しても行は残し、履歴として参照できるようにする。 */
enum class ProposalStatus(val dbValue: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun fromDb(value: String): ProposalStatus =
            entries.firstOrNull { it.dbValue == value } ?: PENDING
    }
}

/**
 * 承認待ち提案(v3-Step 2)。回答JSONの取り込みはまずここへ入り、
 * ユーザーの承認で本テーブルへ反映される(自動更新しない・安全優先)。
 * payloadJson は提案種別ごとのJSON断片(解釈は 2-b の Importer / 2-c のレビューUIが行う)。
 */
data class PendingProposal(
    val id: String,
    val type: ProposalType,
    val payloadJson: String,
    val status: ProposalStatus,
    val receivedAt: String,  // ISO 8601
)
