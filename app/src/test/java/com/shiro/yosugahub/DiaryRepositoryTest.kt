package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.dao.DiaryDao
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import com.shiro.yosugahub.data.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/** 観測日記の保存(2026-07-25: 同じ日付は上書き)。 */
class DiaryRepositoryTest {

    private class FakeDiaryDao : DiaryDao {
        val rows = mutableListOf<DiaryEntryEntity>()
        override fun observeAll(): Flow<List<DiaryEntryEntity>> = MutableStateFlow(rows.toList())
        override suspend fun count(): Int = rows.size
        override suspend fun findByDate(date: String): DiaryEntryEntity? =
            rows.firstOrNull { it.date == date }

        override suspend fun insert(entry: DiaryEntryEntity) {
            rows.removeAll { it.id == entry.id }
            rows += entry
        }

        override suspend fun insertAll(entries: List<DiaryEntryEntity>) = entries.forEach { insert(it) }
        override suspend fun deleteById(id: String) {
            rows.removeAll { it.id == id }
        }

        override suspend fun countByIds(ids: List<String>): Int = rows.count { it.id in ids }
    }

    private fun repository(dao: FakeDiaryDao) = DiaryRepository(
        dao = dao,
        newId = { "id-${dao.rows.size + 1}" },
        now = { "2026-07-25T09:00:00+09:00" },
    )

    @Test
    fun different_dates_are_kept_side_by_side() = runBlocking {
        val dao = FakeDiaryDao()
        val repo = repository(dao)
        repo.add("2026-07-24", "初日")
        repo.add("2026-07-25", "翌日")
        assertEquals(2, dao.rows.size)
    }

    /** 観察日記は一日につき1件。同じ日付が来たら差し替える(シロさんの運用確認)。 */
    @Test
    fun the_same_date_replaces_the_body_instead_of_piling_up() = runBlocking {
        val dao = FakeDiaryDao()
        val repo = repository(dao)
        repo.add("2026-07-24", "初版")
        repo.add("2026-07-24", "書き直した版")

        val entry = dao.rows.single()
        assertEquals("書き直した版", entry.body)
        // id と createdAt は初版を引き継ぐ(いつ最初に書かれたかを失わない)。
        assertEquals("id-1", entry.id)
    }
}
