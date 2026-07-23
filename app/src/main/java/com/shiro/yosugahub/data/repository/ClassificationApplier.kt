package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.file.model.ClassificationProposal
import com.shiro.yosugahub.domain.model.RelatedRef

/**
 * 回答JSON v2 の分類結果を文書へ適用する(v4.1)。
 *
 * 他の提案と違い pending_proposals には積まない — 文書は「確認待ち」になり、
 * 承認・修正は文書の詳細画面で行う(承認を二重に求めない)。
 * 適用できないものは読み飛ばして件数だけ返す(取り込み全体を失敗させない)。
 *
 * ファイル入出力を持たないため、ImportRepository から切り出してテスト可能にしている。
 */
class ClassificationApplier(
    private val documentRepository: DocumentRepository,
) {

    /** applied = 文書へ反映できた件数 / skipped = 読み飛ばした件数。 */
    data class Outcome(val applied: Int, val skipped: Int)

    suspend fun apply(classifications: List<ClassificationProposal>): Outcome {
        var applied = 0
        var skipped = 0
        for (classification in classifications) {
            if (classification.documentId.isBlank()) {
                skipped++
                continue
            }
            val result = documentRepository.applyAiClassification(
                documentId = classification.documentId,
                summary = classification.summary,
                documentType = classification.documentType,
                confidence = classification.confidence,
                projectIds = classification.projectIds,
                categories = classification.categories,
                tags = classification.tags,
                relatedEntities = classification.relatedEntities
                    // type / id のどちらかが欠けた関連は意味を成さないので落とす。
                    .filter { it.type.isNotBlank() && it.id.isNotBlank() }
                    .map { RelatedRef(type = it.type, id = it.id) },
            )
            // 宛先の文書が無い、またはユーザーが決着させた文書(確定済み・アーカイブ済み)。
            if (result != null) applied++ else skipped++
        }
        return Outcome(applied = applied, skipped = skipped)
    }
}
