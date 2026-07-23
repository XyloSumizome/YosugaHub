package com.shiro.yosugahub.domain.model

/** ヨスガ(ChatGPT)から受け取った提案。Phase 2 で回答JSONの取り込み結果に置き換える。 */
data class Recommendation(
    val projectId: String,
    val title: String,
    val detail: String,
    val priority: String,
)
