package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.DirectiveDao
import com.shiro.yosugahub.data.local.db.entity.DirectiveEntity
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.Directive
import com.shiro.yosugahub.domain.model.DirectiveStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime

/**
 * 各ゲームの Claude Code への指示書(v4.2)。
 * ここに入るのは**ユーザーが承認したものだけ**(提案の段階では pending_proposals にいる)。
 * now / newId はテスト容易性のため注入可能。
 */
class DirectiveRepository(
    private val dao: DirectiveDao,
    private val now: () -> String = { OffsetDateTime.now().toString() },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun directives(): Flow<List<Directive>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    /** 配信中(未完了)の件数。ホーム表示用。 */
    fun openCount(): Flow<Int> = dao.observeCountByStatus(DirectiveStatus.OPEN.dbValue)

    /** サーバーへ配信する対象(未完了のみ)。 */
    suspend fun openDirectives(): List<Directive> =
        dao.getByStatus(DirectiveStatus.OPEN.dbValue).map { it.toDomain() }

    /** 承認時に作成する。projectId と本文が無い指示は意味を成さないので作らない。 */
    suspend fun create(
        projectId: String,
        title: String,
        body: String,
        priority: String,
    ): Directive? {
        if (projectId.isBlank() || body.isBlank()) return null
        val timestamp = now()
        val entity = DirectiveEntity(
            id = newId(),
            projectId = projectId,
            title = title,
            body = body,
            priority = priority,
            status = DirectiveStatus.OPEN.dbValue,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
        dao.upsert(entity)
        return entity.toDomain()
    }

    /** 対応済みにする(配信対象から外れる。記録は残す)。 */
    suspend fun markDone(id: String) {
        dao.updateStatus(id, DirectiveStatus.DONE.dbValue, now())
    }

    /** 配信中へ戻す(間違えて完了にしたときのため)。 */
    suspend fun reopen(id: String) {
        dao.updateStatus(id, DirectiveStatus.OPEN.dbValue, now())
    }

    suspend fun delete(id: String) {
        dao.delete(id)
    }
}
