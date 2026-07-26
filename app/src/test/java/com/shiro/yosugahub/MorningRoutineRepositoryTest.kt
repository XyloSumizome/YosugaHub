package com.shiro.yosugahub

import com.shiro.yosugahub.data.calendar.CalendarSyncResult
import com.shiro.yosugahub.data.repository.MorningRoutineRepository
import com.shiro.yosugahub.data.repository.MorningStep
import com.shiro.yosugahub.data.repository.NoteImportSummary
import com.shiro.yosugahub.data.repository.StatusRefreshAllResult
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.domain.model.Project
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 朝の準備は**順序と、転んでも止まらないこと**が仕様。
 * 各段は関数で受けているので、実際の通信・DB・Vault なしで確認できる。
 */
class MorningRoutineRepositoryTest {

    private val project = Project(
        id = "anri",
        name = "ANRI",
        currentGoal = "",
        inProgress = "",
        nextTask = "",
        lastUpdated = "",
        health = "on_track",
    )

    private fun repository(
        calendar: CalendarSyncResult = CalendarSyncResult.Success(3),
        status: StatusRefreshAllResult = StatusRefreshAllResult(fetches = emptyList()),
        notes: NoteImportSummary = NoteImportSummary(outcomes = emptyList()),
        sync: SyncResult = SyncResult.Success(7),
        order: MutableList<String> = mutableListOf(),
        seenProjects: MutableList<List<Project>> = mutableListOf(),
    ) = MorningRoutineRepository(
        projects = { flowOf(listOf(project)) },
        syncCalendar = { order += "calendar"; calendar },
        refreshStatus = { list -> order += "status"; seenProjects += list; status },
        importNotes = { order += "notes"; notes },
        syncServer = { order += "sync"; sync },
    )

    @Test
    fun `カレンダーを最初に、サーバー同期を最後に走らせる`() = runBlocking {
        val order = mutableListOf<String>()
        repository(order = order).run()

        // カレンダーが後だと、古い予定をサーバーへ送ってしまう。
        // サーバー同期が最後でないと、その朝の更新がレコルへ届かない。
        assertEquals(listOf("calendar", "status", "notes", "sync"), order)
    }

    @Test
    fun `status の更新には現在のプロジェクト一覧を渡す`() = runBlocking {
        val seen = mutableListOf<List<Project>>()
        repository(seenProjects = seen).run()

        assertEquals(listOf(listOf(project)), seen)
    }

    @Test
    fun `カレンダーが権限なしでも残りの段を走らせる`() = runBlocking {
        val order = mutableListOf<String>()
        val result = repository(
            calendar = CalendarSyncResult.PermissionDenied,
            order = order,
        ).run()

        // 朝に1つ転んだせいで残り全部が動かないほうが困る。
        assertEquals(listOf("calendar", "status", "notes", "sync"), order)
        assertTrue(result.calendar is CalendarSyncResult.PermissionDenied)
        assertTrue(result.sync is SyncResult.Success)
    }

    @Test
    fun `サーバー同期が失敗しても結果を返す`() = runBlocking {
        val result = repository(sync = SyncResult.Unauthorized).run()

        assertEquals(SyncResult.Unauthorized, result.sync)
        assertTrue(result.calendar is CalendarSyncResult.Success)
    }

    @Test
    fun `各段の結果を順に通知する`() = runBlocking {
        val steps = mutableListOf<MorningStep>()
        repository().run(onStep = { steps += it })

        assertEquals(4, steps.size)
        assertTrue(steps[0] is MorningStep.Calendar)
        assertTrue(steps[1] is MorningStep.Status)
        assertTrue(steps[2] is MorningStep.Notes)
        assertTrue(steps[3] is MorningStep.Sync)
    }
}
