package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.DiaryDao
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.DiaryEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.OffsetDateTime

/**
 * 観察日記の Repository(v3-Step 2)。
 * 日記は AI(よすが)が書き、承認を経てここへ保存される。Hub は内容を生成しない。
 */
class DiaryRepository(
    private val dao: DiaryDao,
    private val now: () -> String = { OffsetDateTime.now().toString() },
    private val newId: () -> String = { java.util.UUID.randomUUID().toString() },
) {

    fun entries(): Flow<List<DiaryEntry>> =
        dao.observeAll().map { entries -> entries.map { it.toDomain() } }

    /** 承認された日記を保存する。 */
    suspend fun add(date: String, body: String): DiaryEntry {
        val entry = DiaryEntryEntity(
            id = newId(),
            date = date,
            body = body,
            createdAt = now(),
        )
        dao.insert(entry)
        return entry.toDomain()
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }
}
