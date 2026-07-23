package com.shiro.yosugahub.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** entities テーブル(v3.1 の「実体」)。(name, type) の組で unique。 */
@Entity(
    tableName = "entities",
    indices = [Index(value = ["name", "type"], unique = true)],
)
data class TrackedEntityEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,  // project / person / tech / gear / event / other
)
