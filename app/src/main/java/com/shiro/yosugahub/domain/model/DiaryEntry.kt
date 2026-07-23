package com.shiro.yosugahub.domain.model

/**
 * 観察日記(v3.1 8章)。Yosuga(AI)がシロを観察して書く日記で、主語は Yosuga。
 * AI が書き、承認を経て保存される。Hub は保存・表示のみで内容を生成しない。
 */
data class DiaryEntry(
    val id: String,
    val date: String,       // "yyyy-MM-dd"
    val body: String,
    val createdAt: String,  // ISO 8601
)
