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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: DiaryEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<DiaryEntryEntity>)

    @Query("DELETE FROM diary_entries WHERE id = :id")
    suspend fun deleteById(id: String)
}
