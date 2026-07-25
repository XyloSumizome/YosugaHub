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
        override suspend fun findBySource(projectId: String, sourcePath: String): ImportedNoteEntity? =
            rows.firstOrNull { it.projectId == projectId && it.sourcePath == sourcePath }

        override suspend fun deleteBySha(sha: String) {
            rows.removeAll { it.sha == sha }
        }

        override suspend fun notesForProject(projectId: String): List<ImportedNoteEntity> =
            rows.filter { it.projectId == projectId }
    }

    private class RecordingWriter(
        private val result: (String, String) -> VaultWriteResult = { dir, name ->
            VaultWriteResult.Written("$dir/$name")
        },
    ) : VaultWriter {
        val writes = mutableListOf<Pair<String, String>>()
        val overwrites = mutableListOf<String>()
        override suspend fun write(
            directory: String,
            fileName: String,
            content: String,
        ): VaultWriteResult {
            writes += directory to fileName
            return result(directory, fileName)
        }

        override suspend fun overwrite(vaultPath: String, content: String): VaultWriteResult {
            overwrites += vaultPath
            return VaultWriteResult.Written(vaultPath)
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
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
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
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
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

    /**
     * 更新されたノート(同じ元ファイル・新しい sha)は、枝番で増やさず
     * **記録済みの場所を上書き**する(2026-07-25 / シロさんの運用確認:
     * ノートは Claude Code だけが書き、人が Obsidian 側で直すことはない)。
     */
    @Test
    fun a_modified_note_overwrites_its_recorded_vault_path() = runBlocking {
        val dao = FakeImportedNoteDao()
        fun build(body: String, sha: String) = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
            repoNoteRepository = repoNotes(
                """{"name":"2026-07-24-a.md","path":".yosuga/notes/2026-07-24-a.md","sha":"$sha","size":10,"type":"file"}""" to body,
            ),
            vaultWriter = RecordingWriter(),
            dao = dao,
            now = { "2026-07-24T10:00:00+09:00" },
        )

        build(frontmatter("design", "初版"), "sha-1").importAll()
        assertEquals("sha-1", dao.rows.single().sha)

        val writer = RecordingWriter()
        val second = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
            repoNoteRepository = repoNotes(
                """{"name":"2026-07-24-a.md","path":".yosuga/notes/2026-07-24-a.md","sha":"sha-2","size":10,"type":"file"}""" to frontmatter("design", "改訂版"),
            ),
            vaultWriter = writer,
            dao = dao,
            now = { "2026-07-25T10:00:00+09:00" },
        ).importAll()

        assertEquals(0, second.imported)
        assertEquals(1, second.updated)
        // 新規作成(write)ではなく、記録済みの場所への上書き。
        assertTrue(writer.writes.isEmpty())
        assertEquals(listOf("Games/ANRI/Design/2026-07-24-a.md"), writer.overwrites)
        // 記録は新しい sha に置き換わり、場所は据え置き。
        assertEquals("sha-2", dao.rows.single().sha)
        assertEquals("Games/ANRI/Design/2026-07-24-a.md", dao.rows.single().vaultPath)
    }

    /** リポジトリから消えたノートは**報告だけ**して Vault 側は残す。 */
    @Test
    fun a_note_deleted_upstream_is_reported_but_kept_in_the_vault() = runBlocking {
        val dao = FakeImportedNoteDao()
        NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
            repoNoteRepository = repoNotes(note("2026-07-24-a.md", frontmatter("design"))),
            vaultWriter = RecordingWriter(),
            dao = dao,
            now = { "2026-07-24T10:00:00+09:00" },
        ).importAll()

        // 2回目の一覧にはもうこのノートが無い(別のノートだけがある)。
        val second = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
            repoNoteRepository = repoNotes(note("2026-07-25-b.md", frontmatter("design"))),
            vaultWriter = RecordingWriter(),
            dao = dao,
            now = { "2026-07-25T10:00:00+09:00" },
        ).importAll()

        assertEquals(listOf("Games/ANRI/Design/2026-07-24-a.md"), second.outcomes.single().missing)
        // 記録は消さない(次回も報告される。Vault 側で消されたら人の判断)。
        assertEquals(2, dao.rows.size)
    }

    @Test
    fun unroutable_notes_go_to_inbox_but_still_count_as_imported() = runBlocking {
        val writer = RecordingWriter()
        val repository = NoteImportRepository(
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
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
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
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
            projectRepository = ProjectRepository(FakeProjectDao(listOf(anri)), EmptyTaskDao()),
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
            projectRepository = ProjectRepository(FakeProjectDao(listOf(local)), EmptyTaskDao()),
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
