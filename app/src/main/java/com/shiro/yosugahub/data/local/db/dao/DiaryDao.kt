package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {

    @Query("SELECT * FROM diary_entries ORDER BY date DESC, createdAt DESC")
    fun observeAll(): Flow<List<DiaryEntryEntity>>

    @Query("SELECT COUNT(*) FROM diary_entries")
    suspend fun count(): Int

    /** 同じ日の既存日記(2026-07-25)。観察日記は一日につき1件なので、上書きの判定に使う。 */
    @Query("SELECT * FROM diary_entries WHERE date = :date LIMIT 1")
    suspend fun findByDate(date: String): DiaryEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DiaryEntryEntity>)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM diary_entries WHERE id IN (:ids)")
    suspend fun countByIds(ids: List<String>): Int
}
