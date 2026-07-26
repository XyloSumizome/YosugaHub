package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/** 朝の準備の各段。UI はこれを端末ログの1行に変える。 */
sealed interface MorningStep {
    data class Calendar(val result: CalendarSyncResult) : MorningStep
    data class Status(val result: StatusRefreshAllResult) : MorningStep
    data class Notes(val summary: NoteImportSummary) : MorningStep
    data class Sync(val result: SyncResult) : MorningStep
}

/** 朝の準備の結果。どの段も**止めずに最後まで走る**ので、全段の結果が揃う。 */
data class MorningRoutineResult(
    val calendar: CalendarSyncResult,
    val status: StatusRefreshAllResult,
    val notes: NoteImportSummary,
    val sync: SyncResult,
)

/**
 * 朝にやることを1つにまとめる(2026-07-26)。
 *
 * それまで朝の操作は**4箇所に散っていた**。カレンダーは `CALENDAR` 画面、
 * サーバー同期は `SETTINGS` の奥、GitHub 取得は `PROJECTS` と コンソールに分かれていた。
 * 毎朝使うものが設定画面の奥にあるのは設計として誤りなので、順序ごと固定して束ねる。
 *
 * **順序に意味がある。**
 * 1. カレンダー … 端末 → Room。**最初に置く**。ここが古いと 4. で古い予定を送ってしまう
 * 2. GitHub の status … `recentChanges` が Morning Brief の「昨日の成果」の材料になる
 * 3. GitHub の notes … Vault へ書き出す(知識の正本は Obsidian 側)
 * 4. サーバー同期 … レコルが読むのはこれ。**最後に必ず1回**
 *
 * 4 を最後に必ず置くのは、2 の内部同期が**取得に成功したときしか走らない**ため。
 * GitHub が落ちている朝でも、カレンダーだけは確実にレコルへ届く必要がある。
 *
 * **途中で失敗しても止めない。** 朝に1つ転んだせいで残り全部が動かないほうが困る。
 * 各段の結果はそのまま返し、何が転んだかは UI が表示する。
 */
class MorningRoutineRepository(
    /**
     * 各段は**関数で受ける**([ProjectStatusRepository] と同じ方針)。
     * 具象 Repository を直接持つとテストで差し替えられない。
     */
    private val projects: () -> Flow<List<Project>>,
    private val syncCalendar: suspend () -> CalendarSyncResult,
    private val refreshStatus: suspend (List<Project>) -> StatusRefreshAllResult,
    private val importNotes: suspend (suspend (ImportEvent) -> Unit) -> NoteImportSummary,
    private val syncServer: suspend () -> SyncResult,
) {

    suspend fun run(
        onStep: suspend (MorningStep) -> Unit = {},
        onNoteEvent: suspend (ImportEvent) -> Unit = {},
    ): MorningRoutineResult {
        val calendar = syncCalendar()
        onStep(MorningStep.Calendar(calendar))

        val status = refreshStatus(projects().first())
        onStep(MorningStep.Status(status))

        val notes = importNotes(onNoteEvent)
        onStep(MorningStep.Notes(notes))

        val sync = syncServer()
        onStep(MorningStep.Sync(sync))

        return MorningRoutineResult(
            calendar = calendar,
            status = status,
            notes = notes,
            sync = sync,
        )
    }
}
