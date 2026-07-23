package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * pending_proposals テーブル(v3-Step 2)。回答JSONの提案はまずここへ入り、
 * 承認で本テーブルへ反映される。棄却しても行は残す(履歴)。
 */
@Entity(tableName = "pending_proposals")
data class PendingProposalEntity(
    @PrimaryKey val id: String,
    val type: String,         // task / item / diary / health
    val payloadJson: String,  // 提案種別ごとのJSON断片
    val status: String,       // pending / approved / rejected
    val receivedAt: String,   // ISO 8601
)
