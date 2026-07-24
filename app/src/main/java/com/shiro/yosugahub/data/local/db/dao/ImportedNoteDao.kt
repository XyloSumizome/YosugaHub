package com.shiro.yosugahub.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.shiro.yosugahub.data.local.db.entity.ImportedNoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportedNoteDao {

    @Query("SELECT * FROM imported_notes ORDER BY importedAt DESC, vaultPath")
    fun observeAll(): Flow<List<ImportedNoteEntity>>

    /** 取得済み判定に使う SHA の一覧。 */
    @Query("SELECT sha FROM imported_notes WHERE projectId = :projectId")
    suspend fun shasForProject(projectId: String): List<String>

    @Query("SELECT COUNT(*) FROM imported_notes")
    suspend fun count(): Int

    /** 同じ SHA を二度記録しない(取り込みは1回きり)。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(note: ImportedNoteEntity)

    @Query("SELECT * FROM imported_notes ORDER BY importedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ImportedNoteEntity>
}
