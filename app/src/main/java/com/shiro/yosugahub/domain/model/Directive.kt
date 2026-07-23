package com.shiro.yosugahub.domain.model

/** 指示書の状態(v4.2)。配信を止めるのは「完了」にしたときだけ。 */
enum class DirectiveStatus(val dbValue: String) {
    /** 配信中。各ゲームの Claude Code が読む対象。 */
    OPEN("open"),
    /** 対応済み。配信対象から外れる(記録としては残す)。 */
    DONE("done");

    companion object {
        /** 未知の値は OPEN へフォールバックし、指示を握り潰さない。 */
        fun fromDb(value: String): DirectiveStatus =
            entries.firstOrNull { it.dbValue == value } ?: OPEN
    }
}

/**
 * 各ゲームの Claude Code への指示書(v4.2)。
 * ヨスガが提案し、ユーザーが承認したものだけがサーバーへ配信される
 * (承認前に作業を始めさせない = v3 以来の 提案→承認→保存 と同じ原則)。
 */
data class Directive(
    val id: String,
    val projectId: String,
    val title: String,
    val body: String,        // Markdown。Claude Code がそのまま読める粒度
    val priority: String,    // high / medium / low
    val status: DirectiveStatus,
    val createdAt: String,   // ISO 8601(承認した時刻)
    val updatedAt: String,   // ISO 8601
)
