package com.shiro.yosugahub.ui.component

import com.shiro.yosugahub.domain.model.ItemKind

/** 情報アイテム種別の表示ラベル(提案カードと記録タブで共用)。 */
fun itemKindLabel(kind: ItemKind): String = when (kind) {
    ItemKind.MEMO -> "メモ"
    ItemKind.IDEA -> "アイデア"
    ItemKind.DECISION -> "決定事項"
    ItemKind.SHOPPING -> "買い物"
    ItemKind.TECH -> "技術"
    ItemKind.OTHER -> "その他"
}
