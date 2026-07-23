package com.shiro.yosugahub.data.local.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room マイグレーション定義。
 * スキーマJSONは app/schemas/ に出力される(build.gradle.kts の room.schemaLocation)。
 * 新しいマイグレーションを書いたら、期待スキーマと一致するか出力JSONで確認すること。
 */

/** v4 → v5: GitHub の status.json キャッシュを追加(GitHub連携 3-c)。 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `project_status_cache` (`projectId` TEXT NOT NULL, " +
                "`statusJson` TEXT NOT NULL, `fetchedAt` TEXT NOT NULL, PRIMARY KEY(`projectId`))"
        )
    }
}

/** v3 → v4: projects に GitHub リポジトリ情報を追加(v3-Step 3 / GitHub連携)。 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repoOwner` TEXT")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repoName` TEXT")
        db.execSQL("ALTER TABLE `projects` ADD COLUMN `repoBranch` TEXT")
    }
}

/** v2 → v3: 知識ベース関連の7テーブルを追加(v3-Step 2)。既存テーブルは変更しない。 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `knowledge_items` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` TEXT NOT NULL, " +
                "`updatedAt` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `tags` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tags_name` ON `tags` (`name`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `item_tags` (`itemId` TEXT NOT NULL, `tagId` TEXT NOT NULL, " +
                "PRIMARY KEY(`itemId`, `tagId`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_item_tags_tagId` ON `item_tags` (`tagId`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `entities` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`type` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_entities_name_type` ON `entities` (`name`, `type`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `item_entities` (`itemId` TEXT NOT NULL, `entityId` TEXT NOT NULL, " +
                "PRIMARY KEY(`itemId`, `entityId`))"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_item_entities_entityId` ON `item_entities` (`entityId`)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `diary_entries` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, " +
                "`body` TEXT NOT NULL, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entries_date` ON `diary_entries` (`date`)")
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_proposals` (`id` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                "`payloadJson` TEXT NOT NULL, `status` TEXT NOT NULL, `receivedAt` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
    }
}

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
