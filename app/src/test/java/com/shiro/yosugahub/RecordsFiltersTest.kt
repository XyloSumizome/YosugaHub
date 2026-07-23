package com.shiro.yosugahub

import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem
import com.shiro.yosugahub.ui.screen.records.decisionsOf
import com.shiro.yosugahub.ui.screen.records.filterItemsByTag
import org.junit.Assert.assertEquals
import org.junit.Test

class RecordsFiltersTest {

    private fun item(id: String, kind: ItemKind = ItemKind.MEMO, tags: List<String> = emptyList()) =
        KnowledgeItem(
            id = id,
            kind = kind,
            title = id,
            body = "",
            tags = tags,
            entities = emptyList(),
            createdAt = "2026-07-23T09:00:00+09:00",
            updatedAt = "2026-07-23T09:00:00+09:00",
            source = "manual",
        )

    @Test
    fun null_tag_returns_all_items() {
        val items = listOf(item("a"), item("b", tags = listOf("買い物")))
        assertEquals(items, filterItemsByTag(items, null))
    }

    @Test
    fun tag_filter_returns_only_matching_items() {
        val items = listOf(
            item("a", tags = listOf("買い物", "展示会準備")),
            item("b", tags = listOf("展示会準備")),
            item("c"),
        )
        assertEquals(listOf("a"), filterItemsByTag(items, "買い物").map { it.id })
        assertEquals(listOf("a", "b"), filterItemsByTag(items, "展示会準備").map { it.id })
        assertEquals(emptyList<String>(), filterItemsByTag(items, "存在しないタグ").map { it.id })
    }

    @Test
    fun decisions_extracts_only_decision_kind_preserving_order() {
        val items = listOf(
            item("d1", kind = ItemKind.DECISION),
            item("m1", kind = ItemKind.MEMO),
            item("d2", kind = ItemKind.DECISION),
            item("i1", kind = ItemKind.IDEA),
        )
        assertEquals(listOf("d1", "d2"), decisionsOf(items).map { it.id })
    }
}
