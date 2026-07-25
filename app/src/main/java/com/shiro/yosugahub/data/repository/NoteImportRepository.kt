package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.ImportedNoteDao
import com.shiro.yosugahub.data.local.db.entity.ImportedNoteEntity
import com.shiro.yosugahub.data.obsidian.Frontmatter
import com.shiro.yosugahub.data.obsidian.NoteRouter
import com.shiro.yosugahub.data.obsidian.VaultWriteResult
import com.shiro.yosugahub.data.obsidian.VaultWriter
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.first
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/** 1プロジェクト分の取り込み結果。 */
data class ProjectImportOutcome(
    val projectId: String,
    val projectName: String,
    /** Vault へ書けた件数(Inbox 行きを含む)。 */
    val imported: Int = 0,
    /** そのうち Inbox へ入れた件数。 */
    val toInbox: Int = 0,
    /** 既存ノートの新しい版で**上書きした**件数(2026-07-25)。 */
    val updated: Int = 0,
    /** 取り込み記録にあるのにリポジトリの一覧から消えたノート(Vault 側は残したまま)。 */
    val missing: List<String> = emptyList(),
    /** 取得済みで飛ばした件数。 */
    val skipped: Int = 0,
    /** 取得または書き込みに失敗したもの。 */
    val failed: List<String> = emptyList(),
    /** 取得自体ができなかった理由。正常時は空文字。 */
    val message: String = "",
)

/** 全プロジェクトの取り込み結果。 */
data class NoteImportSummary(
    val outcomes: List<ProjectImportOutcome>,
    /** Vault が未設定で何もできなかった。 */
    val vaultNotConfigured: Boolean = false,
) {
    val imported: Int get() = outcomes.sumOf { it.imported }
    val toInbox: Int get() = outcomes.sumOf { it.toInbox }
    val updated: Int get() = outcomes.sumOf { it.updated }
    val skipped: Int get() = outcomes.sumOf { it.skipped }
    val failed: Int get() = outcomes.sumOf { it.failed.size }
    val missing: Int get() = outcomes.sumOf { it.missing.size }
}

/**
 * GitHub から取ってきた知識ノートを Obsidian Vault へ収める(v5 Phase 3-b)。
 *
 * 取得([RepoNoteRepository])・振り分け([NoteRouter])・書き込み([VaultWriter])は
 * それぞれ独立していて、ここは**順番に呼ぶだけ**。判断はしない。
 * 書けたものは `imported_notes` に記録し、二度と取り込まない。
 */
class NoteImportRepository(
    private val projectRepository: ProjectRepository,
    private val repoNoteRepository: RepoNoteRepository,
    private val vaultWriter: VaultWriter,
    private val dao: ImportedNoteDao,
    private val now: () -> String = {
        OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },
) {

    /**
     * @param onEvent 進捗を1件ずつ通知する(v5 UI: ハッキング演出)。
     *   **本物の処理イベント**を流すだけで、既定は何もしない(演出無しの呼び出しに影響しない)。
     *   suspend なので、UI 側でここにタメ(delay)を入れて1行ずつ見せられる。
     */
    suspend fun importAll(
        onEvent: suspend (ImportEvent) -> Unit = {},
    ): NoteImportSummary {
        onEvent(ImportEvent.Connect)
        val projects = projectRepository.projects().first()
        // 振り分けにはゲームフォルダ名が要る。表示名を使うが、判定キーは projectId。
        val gameFolders = projects.associate { it.id to it.name }

        val outcomes = projects.map { project -> importProject(project, gameFolders, onEvent) }

        // Vault 未設定なら、どのプロジェクトも1件も書けていない。
        val notConfigured = outcomes.isNotEmpty() &&
            outcomes.all { it.message == VAULT_NOT_CONFIGURED }
        val summary = NoteImportSummary(outcomes = outcomes, vaultNotConfigured = notConfigured)
        onEvent(
            ImportEvent.Done(
                imported = summary.imported,
                toInbox = summary.toInbox,
                updated = summary.updated,
                skipped = summary.skipped,
                failed = summary.failed,
                missing = summary.missing,
            )
        )
        return summary
    }

    private suspend fun importProject(
        project: Project,
        gameFolders: Map<String, String>,
        onEvent: suspend (ImportEvent) -> Unit,
    ): ProjectImportOutcome {
        onEvent(ImportEvent.Target(project.name))
        val known = dao.shasForProject(project.id).toSet()

        return when (val fetched = repoNoteRepository.fetchNewNotes(project, known)) {
            is NoteFetchResult.Success -> {
                onEvent(ImportEvent.Scan(fetched.fetched.size + fetched.skipped))
                writeNotes(project, fetched, gameFolders, onEvent)
            }

            is NoteFetchResult.NotConfigured -> note(project, "リポジトリ未設定", onEvent)
            is NoteFetchResult.TokenMissing -> note(project, "GitHubトークン未設定", onEvent)
            is NoteFetchResult.AuthFailed -> note(project, "GitHubの認証に失敗", onEvent)
            // まだノートを書いていないだけ。エラーとして見せない。
            is NoteFetchResult.NoNotesDirectory -> note(project, "ノートなし", onEvent)
            is NoteFetchResult.NetworkError -> note(project, "通信できません", onEvent)
            is NoteFetchResult.HttpError ->
                note(project, "GitHubエラー(${fetched.statusCode})", onEvent)

            is NoteFetchResult.InvalidListing ->
                note(project, "一覧を解釈できません: ${fetched.message}", onEvent)
        }
    }

    private suspend fun writeNotes(
        project: Project,
        fetched: NoteFetchResult.Success,
        gameFolders: Map<String, String>,
        onEvent: suspend (ImportEvent) -> Unit,
    ): ProjectImportOutcome {
        var imported = 0
        var toInbox = 0
        var updated = 0
        val failed = fetched.failed.toMutableList()
        var message = ""

        for (note in fetched.fetched) {
            onEvent(ImportEvent.Fetch(note.note.name))

            // 同じ元ファイルの記録があれば「更新」。sha は変わっていても
            // sourcePath で同一と判る。ノートは Claude Code だけが書くので、
            // Vault 側の同じ場所を**新しい版で上書き**する(枝番で増やさない)。
            val previous = dao.findBySource(project.id, note.note.path)
            if (previous != null) {
                onEvent(ImportEvent.Update(note.note.name, previous.vaultPath))
                when (val written = vaultWriter.overwrite(previous.vaultPath, note.content)) {
                    is VaultWriteResult.Written -> {
                        updated++
                        // 記録は新しい sha へ置き換える(場所は据え置き)。
                        dao.deleteBySha(previous.sha)
                        dao.insert(previous.copy(sha = note.note.sha, importedAt = now()))
                        onEvent(ImportEvent.Written(written.path))
                    }

                    VaultWriteResult.NotConfigured -> {
                        message = VAULT_NOT_CONFIGURED
                        onEvent(ImportEvent.Note(VAULT_NOT_CONFIGURED))
                        break
                    }

                    is VaultWriteResult.Failed -> {
                        failed += note.note.path
                        onEvent(ImportEvent.Fail(note.note.path))
                    }
                }
                continue
            }

            val parsed = Frontmatter.parse(note.content)
            val destination = NoteRouter.route(
                parsed = parsed,
                sourceFileName = note.note.name,
                repoProjectId = project.id,
                gameFolders = gameFolders,
            )
            onEvent(
                ImportEvent.Route(
                    fileName = note.note.name,
                    destination = destination.directory,
                    isInbox = destination.isInbox,
                )
            )

            when (val written = vaultWriter.write(
                directory = destination.directory,
                fileName = destination.fileName,
                content = note.content,
            )) {
                is VaultWriteResult.Written -> {
                    imported++
                    if (destination.isInbox) toInbox++
                    dao.insert(
                        ImportedNoteEntity(
                            sha = note.note.sha,
                            projectId = project.id,
                            sourcePath = note.note.path,
                            vaultPath = written.path,
                            noteType = destination.noteType,
                            importedAt = now(),
                        )
                    )
                    onEvent(ImportEvent.Written(written.path))
                }

                VaultWriteResult.NotConfigured -> {
                    // Vault が無いなら以降も全部失敗する。記録も残さない(次回やり直せる)。
                    message = VAULT_NOT_CONFIGURED
                    onEvent(ImportEvent.Note(VAULT_NOT_CONFIGURED))
                    break
                }

                is VaultWriteResult.Failed -> {
                    failed += note.note.path
                    onEvent(ImportEvent.Fail(note.note.path))
                }
            }
        }

        if (fetched.skipped > 0) onEvent(ImportEvent.Skip(fetched.skipped))
        fetched.failed.forEach { onEvent(ImportEvent.Fail(it)) }

        // 記録にあるのに一覧から消えたノート。**Vault 側は消さない**(報告だけ)。
        // Vault はシロさんと Obsidian の領分なので、消すかどうかは人が決める。
        val missing = dao.notesForProject(project.id)
            .filter { it.sourcePath !in fetched.listedPaths }
            .map { it.vaultPath }
        missing.forEach { onEvent(ImportEvent.Missing(it)) }

        return ProjectImportOutcome(
            projectId = project.id,
            projectName = project.name,
            imported = imported,
            toInbox = toInbox,
            updated = updated,
            skipped = fetched.skipped,
            failed = failed,
            missing = missing,
            message = message,
        )
    }

    private fun outcome(project: Project, message: String) = ProjectImportOutcome(
        projectId = project.id,
        projectName = project.name,
        message = message,
    )

    /** 取得できなかったプロジェクトの結果を作りつつ、理由をログへ流す。 */
    private suspend fun note(
        project: Project,
        message: String,
        onEvent: suspend (ImportEvent) -> Unit,
    ): ProjectImportOutcome {
        onEvent(ImportEvent.Note(message))
        return outcome(project, message)
    }

    private companion object {
        const val VAULT_NOT_CONFIGURED = "Vault未設定"
    }
}
