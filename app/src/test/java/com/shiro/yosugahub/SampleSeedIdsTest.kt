package com.shiro.yosugahub

import com.shiro.yosugahub.data.local.db.SampleSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 仮データを ID 指定で消せることの前提を守る(v5 / 選択A)。
 * ID が抜けたり重複したりすると、消し残しや実データの巻き込みが起きる。
 */
class SampleSeedIdsTest {

    @Test
    fun every_seeded_row_is_covered_by_an_id_list() {
        assertEquals(SampleSeed.projects.size, SampleSeed.projectIds.size)
        assertEquals(SampleSeed.tasks.size, SampleSeed.taskIds.size)
        assertEquals(SampleSeed.knowledgeItems.size, SampleSeed.itemIds.size)
        assertEquals(SampleSeed.diaryEntries.size, SampleSeed.diaryIds.size)
    }

    @Test
    fun ids_are_unique_and_not_blank() {
        listOf(
            SampleSeed.projectIds,
            SampleSeed.taskIds,
            SampleSeed.itemIds,
            SampleSeed.diaryIds,
        ).forEach { ids ->
            assertEquals(ids.size, ids.distinct().size)
            assertTrue(ids.none { it.isBlank() })
        }
    }

    @Test
    fun seeded_tasks_point_at_seeded_projects_or_nothing() {
        // プロジェクトを消すときにタスクが孤児にならないことの前提。
        SampleSeed.tasks.forEach { task ->
            val projectId = task.projectId
            assertTrue(projectId == null || projectId in SampleSeed.projectIds)
        }
    }

    @Test
    fun seeded_recommendations_can_be_identified_without_an_id() {
        // recommendations は自動採番のため projectId + title で特定する。
        val keys = SampleSeed.recommendations.map { it.projectId to it.title }
        assertEquals(keys.size, keys.distinct().size)
        assertTrue(SampleSeed.recommendations.all { it.title.isNotBlank() })
    }
}
