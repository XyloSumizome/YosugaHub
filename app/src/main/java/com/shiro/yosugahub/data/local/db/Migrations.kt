package com.shiro.yosugahub.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room マイグレーション定義。
 * スキーマJSONは app/schemas/ に出力される(build.gradle.kts の room.schemaLocation)。
 * 新しいマイグレーションを書いたら、期待スキーマと一致するか出力JSONで確認すること。
 */

/** v1 → v2: tasks テーブルを追加(v3-Step 1)。既存テーブルは変更しない。 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `tasks` (
                `id` TEXT NOT NULL,
                `projectId` TEXT,
                `title` TEXT NOT NULL,
                `detail` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `dueDate` TEXT,
                `createdAt` TEXT NOT NULL,
                `updatedAt` TEXT NOT NULL,
                `completedAt` TEXT,
                `source` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_projectId` ON `tasks` (`projectId`)")
    }
}
