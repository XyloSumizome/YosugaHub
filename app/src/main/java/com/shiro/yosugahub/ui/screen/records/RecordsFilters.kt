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
