package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.dao.DirectiveDao
import com.shiro.yosugahub.data.local.db.entity.DirectiveEntity
import com.shiro.yosugahub.data.repository.DirectiveRepository
import com.shiro.yosugahub.domain.model.DirectiveStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 承認済み指示書の保管と配信対象の絞り込み(v4.2)。 */
class DirectiveRepositoryTest {

    private class FakeDirectiveDao : DirectiveDao {
        val rows = mutableMapOf<String, DirectiveEntity>()

        override fun observeAll(): Flow<List<DirectiveEntity>> =
            flowOf(rows.values.sortedByDescending { it.createdAt })

        override suspend fun getByStatus(status: String): List<DirectiveEntity> =
            rows.values.filter { it.status == status }.sortedBy { it.createdAt }

        override fun observeCountByStatus(status: String): Flow<Int> =
            flowOf(rows.values.count { it.status == status })

        override suspend fun upsert(directive: DirectiveEntity) {
            rows[directive.id] = directive
        }

        override suspend fun updateStatus(id: String, status: String, updatedAt: String) {
            rows[id]?.let { rows[id] = it.copy(status = status, updatedAt = updatedAt) }
        }

        override suspend fun delete(id: String) {
            rows.remove(id)
        }
    }

    private var tick = 0
    private var idCounter = 0

    private fun repository(dao: FakeDirectiveDao) = DirectiveRepository(
        dao,
        now = { "2026-07-23T17:00:%02d+09:00".format(tick++) },
        newId = { "dir-${idCounter++}" },
    )

    @Test
    fun created_directive_starts_open_and_is_delivered() = runBlocking {
        val dao = FakeDirectiveDao()
        val repo = repository(dao)

        val directive = repo.create(
            projectId = "anri",
            title = "戦闘の当たり判定を見直す",
            body = "## 目的\n手触りの改善",
            priority = "high",
        )!!

        assertEquals(DirectiveStatus.OPEN, directive.status)
        assertEquals(listOf(directive.id), repo.openDirectives().map { it.id })
        assertEquals(1, repo.openCount().first())
    }

    /** 宛先か本文が無い指示は Claude Code が動けないので作らない。 */
    @Test
    fun rejects_directive_without_project_or_body() = runBlocking {
        val dao = FakeDirectiveDao()
        val repo = repository(dao)

        assertNull(repo.create(projectId = "", title = "宛先なし", body = "本文", priority = "medium"))
        assertNull(repo.create(projectId = "anri", title = "本文なし", body = "  ", priority = "medium"))
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun done_directive_is_no_longer_delivered_but_kept_as_a_record() = runBlocking {
        val dao = FakeDirectiveDao()
        val repo = repository(dao)
        val directive = repo.create("anri", "対応済みにする", "本文", "medium")!!

        repo.markDone(directive.id)

        assertTrue(repo.openDirectives().isEmpty())
        assertEquals(0, repo.openCount().first())
        // 記録としては残る
        assertEquals(1, repo.directives().first().size)
        assertEquals(DirectiveStatus.DONE, repo.directives().first().single().status)
    }

    /** 間違えて完了にしても配信へ戻せる。 */
    @Test
    fun reopen_puts_it_back_in_delivery() = runBlocking {
        val dao = FakeDirectiveDao()
        val repo = repository(dao)
        val directive = repo.create("anri", "戻す", "本文", "low")!!
        repo.markDone(directive.id)

        repo.reopen(directive.id)

        assertEquals(listOf(directive.id), repo.openDirectives().map { it.id })
    }

    /** 配信は出した順(優先度で並べ替えない — 読む順序を勝手に変えない)。 */
    @Test
    fun open_directives_keep_creation_order() = runBlocking {
        val dao = FakeDirectiveDao()
        val repo = repository(dao)
        val first = repo.create("anri", "1つ目", "本文", "low")!!
        val second = repo.create("anri", "2つ目", "本文", "high")!!

        assertEquals(listOf(first.id, second.id), repo.openDirectives().map { it.id })
    }

    @Test
    fun delete_removes_the_record() = runBlocking {
        val dao = FakeDirectiveDao()
        val repo = repository(dao)
        val directive = repo.create("anri", "消す", "本文", "medium")!!

        repo.delete(directive.id)

        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun unknown_status_falls_back_to_open() {
        assertEquals(DirectiveStatus.OPEN, DirectiveStatus.fromDb("宇宙"))
    }
}
