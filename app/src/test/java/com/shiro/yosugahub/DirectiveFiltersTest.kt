package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.Directive
import com.shiro.yosugahub.domain.model.DirectiveStatus
import com.shiro.yosugahub.domain.model.Project
import com.shiro.yosugahub.ui.screen.records.directivePriorityLabel
import com.shiro.yosugahub.ui.screen.records.directiveTargetName
import com.shiro.yosugahub.ui.screen.records.directiveTargetsPresent
import com.shiro.yosugahub.ui.screen.records.filterDirectivesByProject
import com.shiro.yosugahub.ui.screen.records.sortDirectivesForDisplay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 記録タブ「指示」セクションの並び替え・絞り込み(純粋関数)。 */
class DirectiveFiltersTest {

    private fun directive(
        id: String,
        projectId: String = "anri",
        status: DirectiveStatus = DirectiveStatus.OPEN,
        createdAt: String = "2026-07-23T17:00:00+09:00",
    ) = Directive(
        id = id,
        projectId = projectId,
        title = "指示 $id",
        body = "本文",
        priority = "medium",
        status = status,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    private val anri = Project(
        id = "anri", name = "ANRI", currentGoal = "", inProgress = "",
        nextTask = "", lastUpdated = "", health = "on_track",
    )
    private val frog = Project(
        id = "paper-armor-frog", name = "紙装甲主人公と不死身のカエル", currentGoal = "",
        inProgress = "", nextTask = "", lastUpdated = "", health = "on_track",
    )

    /** 対応が必要なもの(配信中)を先に、その中は新しい順。 */
    @Test
    fun open_directives_come_first_then_newest() {
        val sorted = sortDirectivesForDisplay(
            listOf(
                directive("done-new", status = DirectiveStatus.DONE, createdAt = "2026-07-23T19:00:00+09:00"),
                directive("open-old", createdAt = "2026-07-23T10:00:00+09:00"),
                directive("open-new", createdAt = "2026-07-23T18:00:00+09:00"),
            )
        )
        assertEquals(listOf("open-new", "open-old", "done-new"), sorted.map { it.id })
    }

    @Test
    fun filters_by_project() {
        val directives = listOf(directive("a"), directive("b", projectId = "paper-armor-frog"))
        assertEquals(2, filterDirectivesByProject(directives, null).size)
        assertEquals(listOf("b"), filterDirectivesByProject(directives, "paper-armor-frog").map { it.id })
    }

    @Test
    fun target_name_uses_project_display_name() {
        assertEquals("ANRI", directiveTargetName(directive("a"), listOf(anri, frog)))
    }

    /** プロジェクトを消しても宛先が空欄にならない。 */
    @Test
    fun target_name_falls_back_to_project_id() {
        assertEquals(
            "gengenkyo",
            directiveTargetName(directive("a", projectId = "gengenkyo"), listOf(anri)),
        )
    }

    /** 絞り込みチップは指示書が実際にある宛先だけを、プロジェクト一覧の順で出す。 */
    @Test
    fun lists_only_targets_that_have_directives() {
        val directives = listOf(directive("a", projectId = "paper-armor-frog"))
        assertEquals(listOf(frog), directiveTargetsPresent(directives, listOf(anri, frog)))
        assertTrue(directiveTargetsPresent(emptyList(), listOf(anri, frog)).isEmpty())
    }

    /** ヨスガが自由記述しても壊さない。 */
    @Test
    fun priority_label_passes_through_unknown_values() {
        assertEquals("高", directivePriorityLabel("high"))
        assertEquals("中", directivePriorityLabel("medium"))
        assertEquals("低", directivePriorityLabel("low"))
        assertEquals("至急", directivePriorityLabel("至急"))
    }
}
