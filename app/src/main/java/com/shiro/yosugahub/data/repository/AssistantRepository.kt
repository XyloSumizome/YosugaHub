package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.toDomain
import com.shiro.yosugahub.domain.model.Recommendation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * よすが(ChatGPT)提案の取得元を吸収する Repository。
 * 現状は Room(仮データでシード)。Phase 2 で回答JSONの取り込み結果へ、
 * Phase 7 で OpenAI API データソースへ差し替える(設計書7章)。
 */
class AssistantRepository(private val dao: RecommendationDao) {

    fun recommendations(): Flow<List<Recommendation>> =
        dao.observeAll().map { recommendations -> recommendations.map { it.toDomain() } }
}
