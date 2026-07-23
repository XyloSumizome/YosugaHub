package com.shiro.yosugahub.ui.component

import com.shiro.yosugahub.domain.model.ClassificationOrigin
import com.shiro.yosugahub.domain.model.DocumentStatus
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

/** 文書の状態ラベル(v4.1)。ユーザーから見た意味で表示する。 */
fun documentStatusLabel(status: DocumentStatus): String = when (status) {
    DocumentStatus.UNCLASSIFIED -> "未整理"
    DocumentStatus.CLASSIFICATION_PENDING -> "分類待ち"
    DocumentStatus.NEEDS_REVIEW -> "確認待ち"
    DocumentStatus.CLASSIFIED -> "分類済み"
    DocumentStatus.ARCHIVED -> "アーカイブ"
}

/** 分類レコードの適用者ラベル(v4.1)。履歴でAI結果とユーザー修正を見分ける。 */
fun classificationOriginLabel(origin: ClassificationOrigin): String = when (origin) {
    ClassificationOrigin.AI -> "ヨスガの分類"
    ClassificationOrigin.USER -> "ユーザーの修正"
}
