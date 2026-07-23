package com.shiro.yosugahub.ui.component

import com.shiro.yosugahub.domain.model.ClassificationOrigin
import com.shiro.yosugahub.domain.model.DirectiveStatus
import com.shiro.yosugahub.domain.model.DocumentStatus
import com.shiro.yosugahub.domain.model.EntityType
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

/** 指示書の状態ラベル(v4.2)。配信中かどうかが一目で分かる言葉にする。 */
fun directiveStatusLabel(status: DirectiveStatus): String = when (status) {
    DirectiveStatus.OPEN -> "配信中"
    DirectiveStatus.DONE -> "対応済み"
}

/** 実体の種別ラベル(v3.1 4章)。 */
fun entityTypeLabel(type: EntityType): String = when (type) {
    EntityType.PROJECT -> "プロジェクト"
    EntityType.PERSON -> "人物"
    EntityType.TECH -> "技術"
    EntityType.GEAR -> "機材"
    EntityType.EVENT -> "イベント"
    EntityType.OTHER -> "その他"
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
