package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.SampleDataSource
import com.shiro.yosugahub.domain.model.Recommendation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * よすが(ChatGPT)提案の取得元を吸収する Repository。
 * 現状はインメモリの仮データ。Phase 2 で回答JSONの取り込み結果へ、
 * Phase 7 で OpenAI API データソースへ差し替える(設計書7章)。
 */
class AssistantRepository(private val source: SampleDataSource) {

    fun recommendations(): Flow<List<Recommendation>> = flowOf(source.recommendations)
}
