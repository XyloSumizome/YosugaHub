package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.ContextExporter
import com.shiro.yosugahub.data.file.model.ContextExport
import com.shiro.yosugahub.domain.model.CalendarEvent
import com.shiro.yosugahub.domain.model.DiaryEntry
import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.domain.model.ProjectStatusSnapshot
import com.shiro.yosugahub.domain.model.StatusBlockerLine
import com.shiro.yosugahub.domain.model.StatusChangeLine
import com.shiro.yosugahub.domain.model.StatusLine
import com.shiro.yosugahub.domain.model.Task
import com.shiro.yosugahub.domain.model.TaskStatus
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextExporterTest {

    private val projects = listOf(
        Project(
            id = "anri",
            name = "ANRI",
            currentGoal = "プロトタイプの完成",
            inProgress = "第2章の執筆",
            nextTask = "戦闘バランス調整",
            lastUpdated = "2026-07-22 18:00",
            health = "on_track",
        ),
    )
    private val events = listOf(
        CalendarEvent("歯医者", "10:00", "11:00", "個人"),
    )

    @Test
    fun build_sets_schema_version_and_maps_fields() {
        val export = ContextExporter.build(
            projects = projects,
            events = events,
            generatedAt = "2026-07-22T20:00:00+09:00",
        )
        assertEquals(ContextExport.SCHEMA_VERSION, export.schemaVersion)
        assertEquals("2026-07-22T20:00:00+09:00", export.generatedAt)
        assertEquals(1, export.projects.size)
        assertEquals("anri", export.projects.first().id)
        assertTrue(export.projects.first().statusMarkdown.contains("プロトタイプの完成"))
        assertEquals(1, export.calendar.events.size)
        assertEquals("歯医者", export.calendar.events.first().title)
    }

    @Test
    fun serialized_json_round_trips_and_contains_schema_version() {
        val export = ContextExporter.build(projects, events, "2026-07-22T20:00:00+09:00")
        val jsonText = ContextExporter.toJson(export)
        assertTrue(jsonText.contains("\"schemaVersion\": 2"))
        assertTrue(jsonText.contains("\"responseSchemaVersion\": 2"))

        val decoded = Json.decodeFromString<ContextExport>(jsonText)
        assertEquals(export, decoded)
    }

    @Test
    fun github_status_replaces_local_markdown_and_carries_blockers() {
        val snapshot = ProjectStatusSnapshot(
            projectId = "anri",
            summary = "第2章を執筆中",
            health = "attention",
            phase = "prototype",
            goalTitle = "プロトタイプ完成",
            goalDetail = "第2章まで",
            inProgress = listOf(StatusLine("第2章の執筆", "50%")),
            nextTasks = listOf(StatusLine("戦闘調整", "優先度: high")),
            blockers = listOf(
                StatusBlockerLine("素材待ち", detail = "発注済み", severity = "high", since = "2026-07-18"),
            ),
            decisions = listOf(StatusLine("リズム判定は3段階にする", "2026-07-20 / 操作を単純に保つため")),
            questionsForYosuga = listOf("難易度はどうすべきか"),
            generatedAt = "2026-07-23T21:00:00+09:00",
            sourceCommit = "abc123",
            fetchedAt = "2026-07-23T22:00:00+09:00",
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-23T22:00:00+09:00",
            statuses = mapOf("anri" to snapshot),
        )
        val project = export.projects.single()

        assertEquals("github", project.source)
        assertEquals("attention", project.health)          // status 側の health を優先
        assertEquals("2026-07-23T21:00:00+09:00", project.lastUpdated)  // generatedAt を優先
        // 構造化されていないものだけ Markdown に残す。
        assertTrue(project.statusMarkdown.contains("## Summary"))
        assertTrue(project.statusMarkdown.contains("第2章を執筆中"))
        assertTrue(project.statusMarkdown.contains("abc123"))

        // 構造化して渡す4節は Markdown に**書かない**(2026-07-26)。
        // 両方載せると同じ内容が散文と構造の二重で流れ、レコルが毎朝それを全部読む。
        assertFalse(project.statusMarkdown.contains("## Blockers"))
        assertFalse(project.statusMarkdown.contains("## Decisions"))
        assertFalse(project.statusMarkdown.contains("## Recent Changes"))
        assertFalse(project.statusMarkdown.contains("## Questions for Yosuga"))

        // 外した分は構造化フィールドに残っている(落としたのではない)。
        // ブロッカーは「いつから」を保つ(2026-07-25)。文字列に畳まない。
        val blocker = project.blockers.single()
        assertEquals("素材待ち", blocker.title)
        assertEquals("2026-07-18", blocker.since)
        assertEquals("high", blocker.severity)
        assertEquals(listOf("難易度はどうすべきか"), project.questionsForYosuga)
        // ゲーム側の確定事項もAIへ渡す(これに矛盾する提案をさせないため)
        assertEquals(
            listOf("リズム判定は3段階にする(2026-07-20 / 操作を単純に保つため)"),
            project.decisions,
        )
    }

    /**
     * 既存の語彙を渡す(2026-07-26)。**これが無いと受け手が毎回新しいタグを作る。**
     * レコルが優位だった唯一の点がこれだったので、ヨスガへ直接渡すために足した。
     */
    @Test
    fun existing_vocabulary_is_carried_so_the_reader_can_reuse_it() {
        val export = ContextExporter.build(
            projects = emptyList(),
            events = emptyList(),
            generatedAt = "2026-07-26T09:00:00+09:00",
            tagNames = listOf("Yosuga Hub", "UI"),
            entityNames = listOf("Ultraleap"),
        )

        assertEquals(listOf("Yosuga Hub", "UI"), export.vocabulary.tags)
        assertEquals(listOf("Ultraleap"), export.vocabulary.entities)
    }

    /**
     * 観測日記を現況に載せる(2026-07-26)。**ヨスガ本人が書いたものだが、
     * ヨスガが覚えているとは限らない**(ChatGPT は過去の日記全文を保持しない)。
     * 「シロさんの状態」の材料をメモリ頼みにしないため、Hub から渡す。
     */
    @Test
    fun recent_diary_is_carried_but_capped() {
        val entries = (1..5).map {
            DiaryEntry(id = "d$it", date = "2026-07-2$it", body = "本文$it", createdAt = "")
        }
        val export = ContextExporter.build(
            projects = emptyList(),
            events = emptyList(),
            generatedAt = "2026-07-26T09:00:00+09:00",
            diary = entries,
        )

        // 本文を持つので件数を絞る。傾向が分かればよく、履歴を渡す場ではない。
        assertEquals(ContextExporter.MAX_DIARY, export.recentDiary.size)
        assertEquals("2026-07-21", export.recentDiary.first().date)
        assertEquals("本文1", export.recentDiary.first().body)
    }

    /** 語彙が無い日もある。空は「渡し忘れ」ではなく「まだ何も無い」。 */
    @Test
    fun an_empty_vocabulary_is_valid() {
        val export = ContextExporter.build(
            projects = emptyList(),
            events = emptyList(),
            generatedAt = "2026-07-26T09:00:00+09:00",
        )

        assertTrue(export.vocabulary.tags.isEmpty())
        assertTrue(export.vocabulary.entities.isEmpty())
    }

    /**
     * 修正のログ(status.json の recentChanges)を**直近2週間分だけ**渡す(2026-07-25)。
     * 「最新の状態」だけでは何がどう変わったかが分からないため。
     */
    @Test
    fun recent_changes_are_carried_but_limited_to_two_weeks() {
        val snapshot = statusSnapshot(
            recentChanges = listOf(
                StatusChangeLine("2026-07-24", "当たり判定を修正", "abc1234"),
                StatusChangeLine("2026-07-12", "ちょうど2週間前", "def5678"),
                StatusChangeLine("2026-07-01", "3週間前なので落ちる", "old0000"),
            ),
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-26T09:00:00+09:00",
            statuses = mapOf("anri" to snapshot),
        )
        val project = export.projects.single()

        assertEquals(
            listOf("当たり判定を修正", "ちょうど2週間前"),
            project.recentChanges.map { it.summary },
        )
        // 日付を畳まず残す(AIが期間で絞れるように)。
        assertEquals("2026-07-24", project.recentChanges.first().date)
        assertEquals("abc1234", project.recentChanges.first().commit)
        // Markdown 側には出さない(構造化して渡しているものを二重に流さない / 2026-07-26)。
        assertFalse(project.statusMarkdown.contains("## Recent Changes"))
        // 2週間の絞り込みは構造化フィールド側で効いている。
        assertFalse(project.recentChanges.any { it.summary == "3週間前なので落ちる" })
    }

    /** 日付が空の行は落とさない。落とすと「変更が無かった」と読めてしまう。 */
    @Test
    fun recent_changes_without_a_date_survive_the_window() {
        val snapshot = statusSnapshot(
            recentChanges = listOf(StatusChangeLine(date = "", summary = "日付を書き忘れた修正")),
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-26T09:00:00+09:00",
            statuses = mapOf("anri" to snapshot),
        )
        assertEquals(listOf("日付を書き忘れた修正"), export.projects.single().recentChanges.map { it.summary })
    }

    /** generatedAt が壊れていても、ログを黙って捨てない。 */
    @Test
    fun unparseable_generated_at_keeps_every_change() {
        val snapshot = statusSnapshot(
            recentChanges = listOf(StatusChangeLine("2020-01-01", "ずっと前の修正")),
        )
        val export = ContextExporter.build(
            projects, events, "いつだか分からない",
            statuses = mapOf("anri" to snapshot),
        )
        assertEquals(1, export.projects.single().recentChanges.size)
    }

    /** カレンダーの窓は ±14 日(2026-07-25 に ±7 から拡張)。 */
    @Test
    fun calendar_window_is_two_weeks_each_way() {
        val export = ContextExporter.build(projects, events, "2026-07-26T09:00:00+09:00")
        assertEquals(14, export.calendar.pastDays)
        assertEquals(14, export.calendar.futureDays)
    }

    private fun statusSnapshot(recentChanges: List<StatusChangeLine>) = ProjectStatusSnapshot(
        projectId = "anri",
        summary = "第2章を執筆中",
        health = "attention",
        phase = "prototype",
        goalTitle = "プロトタイプ完成",
        goalDetail = "",
        inProgress = emptyList(),
        nextTasks = emptyList(),
        blockers = emptyList(),
        decisions = emptyList(),
        recentChanges = recentChanges,
        questionsForYosuga = emptyList(),
        generatedAt = "2026-07-26T08:00:00+09:00",
        sourceCommit = "",
        fetchedAt = "2026-07-26T09:00:00+09:00",
    )

    @Test
    fun falls_back_to_local_fields_when_status_not_fetched() {
        val export = ContextExporter.build(projects, events, "2026-07-23T22:00:00+09:00")
        val project = export.projects.single()
        assertEquals("local", project.source)
        assertEquals("on_track", project.health)
        assertEquals("2026-07-22 18:00", project.lastUpdated)
        assertTrue(project.statusMarkdown.contains("プロトタイプの完成"))
        assertTrue(project.blockers.isEmpty())
    }

    @Test
    fun build_maps_tasks_and_decisions_v2() {
        val tasks = listOf(
            Task(
                id = "task-1",
                projectId = "anri",
                title = "戦闘調整",
                detail = "係数見直し",
                status = TaskStatus.DOING,
                priority = "high",
                dueDate = "2026-07-30",
                createdAt = "2026-07-23T09:00:00+09:00",
                updatedAt = "2026-07-23T09:00:00+09:00",
                completedAt = null,
                source = "manual",
            ),
        )
        val decisions = listOf(
            KnowledgeItem(
                id = "item-1",
                kind = ItemKind.DECISION,
                title = "ビート表示を採用",
                body = "理由",
                tags = emptyList(),
                entities = emptyList(),
                createdAt = "2026-07-23T10:00:00+09:00",
                updatedAt = "2026-07-23T10:00:00+09:00",
                source = "assistant",
            ),
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-23T20:00:00+09:00",
            tasks = tasks, decisions = decisions,
        )
        val task = export.tasks.single()
        assertEquals("doing", task.status)
        assertEquals("anri", task.projectId)
        assertEquals("2026-07-30", task.dueDate)
        // 未完了は null のまま(「昨日の成果」に混ざらせない)。
        assertNull(task.completedAt)
        val decision = export.recentDecisions.single()
        assertEquals("2026-07-23", decision.date)
        assertEquals("ビート表示を採用", decision.title)
    }

    /**
     * 完了時刻を書き出す(2026-07-25)。これが無いと `status: "done"` しか見えず、
     * Morning Brief の「昨日の成果」に**いつ終わったか**を書けない。
     */
    @Test
    fun build_exports_completed_at_so_recency_is_knowable() {
        val done = Task(
            id = "task-2",
            projectId = "anri",
            title = "当たり判定の修正",
            detail = "",
            status = TaskStatus.DONE,
            priority = "medium",
            dueDate = null,
            createdAt = "2026-07-20T09:00:00+09:00",
            updatedAt = "2026-07-24T18:30:00+09:00",
            completedAt = "2026-07-24T18:30:00+09:00",
            source = "manual",
        )
        val export = ContextExporter.build(
            projects, events, "2026-07-25T08:00:00+09:00",
            tasks = listOf(done), decisions = emptyList(),
        )
        val task = export.tasks.single()
        assertEquals("done", task.status)
        assertEquals("2026-07-24T18:30:00+09:00", task.completedAt)
    }
}
