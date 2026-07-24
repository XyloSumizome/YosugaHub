package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.github.FetchResult
import com.shiro.yosugahub.data.github.GitHubApi
import com.shiro.yosugahub.data.github.NoteListParser
import com.shiro.yosugahub.data.github.RemoteNote
import com.shiro.yosugahub.domain.model.Project

/** 本文まで取得できた知識ノート。 */
data class FetchedNote(
    val projectId: String,
    val note: RemoteNote,
    val content: String,
)

/** 1プロジェクト分の取得結果(v5 Phase 3-a)。 */
sealed interface NoteFetchResult {
    /**
     * 取得できたもの。
     * @param fetched 今回取得した本文つきノート
     * @param skipped 取得済み(sha が既知)で飛ばした件数
     * @param failed 一覧には出たが本文を取得できなかったパス
     */
    data class Success(
        val projectId: String,
        val fetched: List<FetchedNote>,
        val skipped: Int,
        val failed: List<String>,
    ) : NoteFetchResult

    /** リポジトリ未設定(取得対象外)。 */
    data class NotConfigured(val projectId: String) : NoteFetchResult

    data class TokenMissing(val projectId: String) : NoteFetchResult
    data class AuthFailed(val projectId: String) : NoteFetchResult

    /** `.yosuga/notes/` がまだ無い。**エラーではない**(そのゲームがまだ書いていないだけ)。 */
    data class NoNotesDirectory(val projectId: String) : NoteFetchResult

    data class NetworkError(val projectId: String) : NoteFetchResult
    data class HttpError(val projectId: String, val statusCode: Int) : NoteFetchResult
    data class InvalidListing(val projectId: String, val message: String) : NoteFetchResult
}

/**
 * 各ゲームのリポジトリから `.yosuga/notes/` の知識ノートを取得する(v5 Phase 3-a)。
 *
 * **ここは取得までしか行わない。** Vault への書き込みと保存先の振り分けは 3-b。
 * 取得済み判定はブロブSHAで行い、**判定材料は呼び出し側が渡す**
 * (永続化の持ち方に依存しないようにするため)。
 */
class RepoNoteRepository(
    private val api: GitHubApi,
    private val tokenProvider: suspend () -> String?,
) {

    /**
     * @param knownShas 取得済みのブロブSHA。ここに含まれるノートは本文を取りに行かない。
     */
    suspend fun fetchNewNotes(
        project: Project,
        knownShas: Set<String>,
    ): NoteFetchResult {
        if (!project.hasRepository) return NoteFetchResult.NotConfigured(project.id)
        val token = tokenProvider() ?: return NoteFetchResult.TokenMissing(project.id)

        val listing = api.listDirectory(
            owner = project.repoOwner.orEmpty(),
            repo = project.repoName.orEmpty(),
            path = GitHubApi.NOTES_PATH,
            branch = project.repoBranch,
            token = token,
        )

        val body = when (listing) {
            is FetchResult.Success -> listing.content
            is FetchResult.Unauthorized -> return NoteFetchResult.AuthFailed(project.id)
            // ディレクトリが無いだけ。まだノートを書いていないゲームでは普通に起こる。
            FetchResult.NotFound -> return NoteFetchResult.NoNotesDirectory(project.id)
            FetchResult.NetworkError -> return NoteFetchResult.NetworkError(project.id)
            is FetchResult.HttpError ->
                return NoteFetchResult.HttpError(project.id, listing.statusCode)
        }

        val notes = when (val parsed = NoteListParser.parse(body)) {
            is NoteListParser.Result.Success -> parsed.notes
            is NoteListParser.Result.InvalidJson ->
                return NoteFetchResult.InvalidListing(project.id, parsed.message)
        }

        val targets = notes.filter { it.sha !in knownShas }
        val skipped = notes.size - targets.size

        val fetched = mutableListOf<FetchedNote>()
        val failed = mutableListOf<String>()

        // 一度に取りすぎない。残りは次回の取得へ回す(件数は skipped では数えない)。
        targets.take(MAX_NOTES_PER_RUN).forEach { note ->
            val result = api.fetchFile(
                owner = project.repoOwner.orEmpty(),
                repo = project.repoName.orEmpty(),
                path = note.path,
                branch = project.repoBranch,
                token = token,
            )
            when (result) {
                is FetchResult.Success ->
                    fetched += FetchedNote(project.id, note, result.content)
                // 1件の失敗で全体を止めない。次回また拾える。
                else -> failed += note.path
            }
        }

        return NoteFetchResult.Success(
            projectId = project.id,
            fetched = fetched,
            skipped = skipped,
            failed = failed,
        )
    }

    private companion object {
        /** 1回の取得で本文を取りに行く上限。API を叩きすぎないための歯止め。 */
        const val MAX_NOTES_PER_RUN = 30
    }
}
