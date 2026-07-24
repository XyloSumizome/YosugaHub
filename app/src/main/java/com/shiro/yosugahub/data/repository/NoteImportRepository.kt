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
    val skipped: Int get() = outcomes.sumOf { it.skipped }
    val failed: Int get() = outcomes.sumOf { it.failed.size }
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

    suspend fun importAll(): NoteImportSummary {
        val projects = projectRepository.projects().first()
        // 振り分けにはゲームフォルダ名が要る。表示名を使うが、判定キーは projectId。
        val gameFolders = projects.associate { it.id to it.name }

        val outcomes = projects.map { project -> importProject(project, gameFolders) }

        // Vault 未設定なら、どのプロジェクトも1件も書けていない。
        val notConfigured = outcomes.isNotEmpty() &&
            outcomes.all { it.message == VAULT_NOT_CONFIGURED }
        return NoteImportSummary(outcomes = outcomes, vaultNotConfigured = notConfigured)
    }

    private suspend fun importProject(
        project: Project,
        gameFolders: Map<String, String>,
    ): ProjectImportOutcome {
        val known = dao.shasForProject(project.id).toSet()

        return when (val fetched = repoNoteRepository.fetchNewNotes(project, known)) {
            is NoteFetchResult.Success -> writeNotes(project, fetched, gameFolders)

            is NoteFetchResult.NotConfigured -> outcome(project, message = "リポジトリ未設定")
            is NoteFetchResult.TokenMissing -> outcome(project, message = "GitHubトークン未設定")
            is NoteFetchResult.AuthFailed -> outcome(project, message = "GitHubの認証に失敗")
            // まだノートを書いていないだけ。エラーとして見せない。
            is NoteFetchResult.NoNotesDirectory -> outcome(project, message = "ノートなし")
            is NoteFetchResult.NetworkError -> outcome(project, message = "通信できません")
            is NoteFetchResult.HttpError ->
                outcome(project, message = "GitHubエラー(${fetched.statusCode})")

            is NoteFetchResult.InvalidListing ->
                outcome(project, message = "一覧を解釈できません: ${fetched.message}")
        }
    }

    private suspend fun writeNotes(
        project: Project,
        fetched: NoteFetchResult.Success,
        gameFolders: Map<String, String>,
    ): ProjectImportOutcome {
        var imported = 0
        var toInbox = 0
        val failed = fetched.failed.toMutableList()
        var message = ""

        for (note in fetched.fetched) {
            val parsed = Frontmatter.parse(note.content)
            val destination = NoteRouter.route(
                parsed = parsed,
                sourceFileName = note.note.name,
                repoProjectId = project.id,
                gameFolders = gameFolders,
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
                }

                VaultWriteResult.NotConfigured -> {
                    // Vault が無いなら以降も全部失敗する。記録も残さない(次回やり直せる)。
                    message = VAULT_NOT_CONFIGURED
                    break
                }

                is VaultWriteResult.Failed -> failed += note.note.path
            }
        }

        return ProjectImportOutcome(
            projectId = project.id,
            projectName = project.name,
            imported = imported,
            toInbox = toInbox,
            skipped = fetched.skipped,
            failed = failed,
            message = message,
        )
    }

    private fun outcome(project: Project, message: String) = ProjectImportOutcome(
        projectId = project.id,
        projectName = project.name,
        message = message,
    )

    private companion object {
        const val VAULT_NOT_CONFIGURED = "Vault未設定"
    }
}
