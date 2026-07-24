package com.shiro.yosugahub

import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.local.db.dao.ImportedNoteDao
import com.shiro.yosugahub.data.local.db.entity.ImportedNoteEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.obsidian.VaultWriteResult
import com.shiro.yosugahub.data.obsidian.VaultWriter
import com.shiro.yosugahub.data.repository.NoteImportRepository
import com.shiro.yosugahub.data.repository.ProjectRepository
import com.shiro.yosugahub.data.repository.RepoNoteRepository
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteImportRepositoryTest {

    private val anri = ProjectEntity(
        id = "anri",
        name = "ANRI",
        currentGoal = "",
        inProgress = "",
        nextTask = "",
        lastUpdated = "",
        health = "on_track",
        repoOwner = "shiro",
        repoName = "anri",
        repoBranch = "main",
    )

    private class FakeProjectDao(projects: List<ProjectEntity>) : ProjectDao {
        private val flow = MutableStateFlow(projects)
        override fun observeAll(): Flow<List<ProjectEntity>> = flow
        override suspend fun count(): Int = flow.value.size
        override suspend fun countById(id: String): Int = flow.value.count { it.id == id }
        override suspend fun insertAll(projects: List<ProjectEntity>) = Unit
        override suspend fun upsert(project: ProjectEntity) = Unit
        override suspend fun updateHealth(id: String, health: String, lastUpdated: String) = 0
        override suspend fun deleteById(id: String): Int = 0
        override suspend fun countByIds(ids: List<String>): Int = 0
        override suspend fun clearSeededText(
            id: String,
            currentGoal: String,
            inProgress: String,
            nextTask: String,
        ): Int = 0

        override suspend fun countWithSeededText(
            id: String,
            currentGoal: String,
            inProgress: String,
            nextTask: String,
        ): Int = 0
    }

    private class FakeImportedNoteDao : ImportedNoteDao {
        val rows = mutableListOf<ImportedNoteEntity>()
        override fun observeAll(): Flow<List<ImportedNoteEntity>> = MutableStateFlow(rows.toList())
        override suspend fun shasForProject(projectId: String): List<String> =
            rows.filter { it.projectId == projectId }.map { it.sha }

        override suspend fun count(): Int = rows.size
        override suspend fun insert(note: ImportedNoteEntity) {
            if (rows.none { it.sha == note.sha }) rows += note
        }

        override suspend fun recent(limit: Int): List<ImportedNoteEntity> = rows.take(limit)
    }

    private class RecordingWriter(
        private val result: (String, String) -> VaultWriteResult = { dir, name ->
            VaultWriteResult.Written("$dir/$name")
        },
    ) : VaultWriter {
        val writes = mutableListOf<Pair<String, String>>()
        override suspend fun write(
            directory: String,
            fileName: String,
            content: String,
        ): VaultWriteResult {
            writes += directory to fileName
            return result(directory, fileName)
        }
    }

    private fun note(name: String, body: String) =
        """{"name":"$name","path":".yosuga/notes/$name","sha":"sha-$name","size":10,"type":"file"}""" to body

    /** 一覧と本文を返す MockEngine。本文はファイル名で引く。 */
    private fun repoNotes(vararg entries: Pair<String, String>): RepoNoteRepository {
        val listing = entries.joinToString(",", "[", "]") { it.first }
        val bodies = entries.associate { (meta, body) ->
            meta.substringAfter("\"name\":\"").substringBefore('"') to body
        }
        val engine = MockEngine { request ->
            val url = request.url.toString()
            if (url.contains("/contents/.yosuga/notes?") || url.endsWith("/contents/.yosuga/notes")) {
                respond(listing, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            } else {
                val name = url.substringAfterLast('/').substringBefore('?')
                respond(bodies[name].orEmpty(), HttpStatusCode.OK)
            }
        }
        return RepoNoteRepository(GitHubApi(engine = engine)) { "token" }
    }

    private fun frontmatter(type: String, body: String = "本文") =
        "---\ntype: $type\nproject_id: anri\ngame: ANRI\n---\n\n$body"

    @Test
    fun notes_are_written_to_the_folder_their_type_maps_to() = runBlocking {
        val writer = RecordingWriter()
        val dao = FakeImportedNoteDao()
        val repository = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri))),
            repoNoteRepository = repoNotes(
                note("2026-07-24-a.md", frontmatter("design")),
                note("2026-07-24-b.md", frontmatter("development-log")),
            ),
            vaultWriter = writer,
            dao = dao,
            now = { "2026-07-24T10:00:00+09:00" },
        )

        val summary = repository.importAll()

        assertEquals(2, summary.imported)
        assertEquals(0, summary.toInbox)
        assertEquals(
            listOf("Games/ANRI/Design", "Games/ANRI/Development Logs"),
            writer.writes.map { it.first },
        )
    }

    @Test
    fun imported_notes_are_recorded_so_they_are_not_fetched_again() = runBlocking {
        val dao = FakeImportedNoteDao()
        fun build() = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri))),
            repoNoteRepository = repoNotes(note("2026-07-24-a.md", frontmatter("design"))),
            vaultWriter = RecordingWriter(),
            dao = dao,
            now = { "2026-07-24T10:00:00+09:00" },
        )

        assertEquals(1, build().importAll().imported)
        assertEquals(1, dao.rows.size)
        assertEquals("Games/ANRI/Design/2026-07-24-a.md", dao.rows.single().vaultPath)

        // 2回目は取得済みとして飛ばす
        val second = build().importAll()
        assertEquals(0, second.imported)
        assertEquals(1, second.skipped)
    }

    @Test
    fun unroutable_notes_go_to_inbox_but_still_count_as_imported() = runBlocking {
        val writer = RecordingWriter()
        val repository = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri))),
            repoNoteRepository = repoNotes(note("memo.md", "# Frontmatter なし")),
            vaultWriter = writer,
            dao = FakeImportedNoteDao(),
            now = { "2026-07-24T10:00:00+09:00" },
        )

        val summary = repository.importAll()

        assertEquals(1, summary.imported)
        assertEquals(1, summary.toInbox)
        assertEquals("Inbox", writer.writes.single().first)
    }

    @Test
    fun a_failed_write_is_reported_and_not_recorded() = runBlocking {
        val dao = FakeImportedNoteDao()
        val repository = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri))),
            repoNoteRepository = repoNotes(note("2026-07-24-a.md", frontmatter("design"))),
            vaultWriter = RecordingWriter { _, _ -> VaultWriteResult.Failed("書けません") },
            dao = dao,
            now = { "2026-07-24T10:00:00+09:00" },
        )

        val summary = repository.importAll()

        assertEquals(0, summary.imported)
        assertEquals(1, summary.failed)
        // 記録が残らないので次回やり直せる
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun an_unconfigured_vault_stops_early_and_is_surfaced() = runBlocking {
        val writer = RecordingWriter { _, _ -> VaultWriteResult.NotConfigured }
        val dao = FakeImportedNoteDao()
        val repository = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri))),
            repoNoteRepository = repoNotes(
                note("2026-07-24-a.md", frontmatter("design")),
                note("2026-07-24-b.md", frontmatter("design")),
            ),
            vaultWriter = writer,
            dao = dao,
            now = { "2026-07-24T10:00:00+09:00" },
        )

        val summary = repository.importAll()

        assertTrue(summary.vaultNotConfigured)
        assertEquals(0, summary.imported)
        // 1件目で打ち切る(2件目を無駄に試さない)
        assertEquals(1, writer.writes.size)
        assertTrue(dao.rows.isEmpty())
    }

    @Test
    fun a_project_without_a_repository_is_reported_not_failed() = runBlocking {
        val local = anri.copy(id = "local", repoOwner = null, repoName = null)
        val repository = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(local))),
            repoNoteRepository = repoNotes(),
            vaultWriter = RecordingWriter(),
            dao = FakeImportedNoteDao(),
            now = { "2026-07-24T10:00:00+09:00" },
        )

        val summary = repository.importAll()

        assertEquals(0, summary.imported)
        assertEquals("リポジトリ未設定", summary.outcomes.single().message)
        assertFalse(summary.vaultNotConfigured)
    }
}
