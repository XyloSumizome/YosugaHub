package com.shiro.yosugahub.ui.screen.records

import com.shiro.yosugahub.domain.model.ItemKind
import com.shiro.yosugahub.domain.model.KnowledgeItem

/** 記録タブの絞り込みロジック(純粋関数、ユニットテスト可能)。 */

/** タグで絞り込む。null は「すべて」。 */
fun filterItemsByTag(items: List<KnowledgeItem>, tag: String?): List<KnowledgeItem> =
    if (tag == null) items else items.filter { tag in it.tags }

/** 決定事項ログ(kind=DECISION)を時系列(新しい順は呼び出し元の並びに従う)で抜き出す。 */
fun decisionsOf(items: List<KnowledgeItem>): List<KnowledgeItem> =
    items.filter { it.kind == ItemKind.DECISION }

/** キーワード検索。空なら全件。タイトル・本文・タグを大文字小文字無視で部分一致。 */
fun searchItems(items: List<KnowledgeItem>, query: String): List<KnowledgeItem> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return items
    return items.filter { item ->
        item.title.contains(trimmed, ignoreCase = true) ||
            item.body.contains(trimmed, ignoreCase = true) ||
            item.tags.any { it.contains(trimmed, ignoreCase = true) }
    }
}

/** タグ入力(カンマ・読点区切り)をタグ名リストへ(trim・空除去・重複除去)。 */
fun parseTagsInput(input: String): List<String> =
    input.split(',', '、')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
