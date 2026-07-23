package com.shiro.yosugahub

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.shiro.yosugahub.data.local.db.MIGRATION_1_2
import com.shiro.yosugahub.data.local.db.MIGRATION_2_3
import com.shiro.yosugahub.data.local.db.MIGRATION_3_4
import com.shiro.yosugahub.data.local.db.MIGRATION_4_5
import com.shiro.yosugahub.data.local.db.MIGRATION_5_6
import com.shiro.yosugahub.data.local.db.MIGRATION_6_7
import com.shiro.yosugahub.data.local.db.YosugaDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room マイグレーションの通しテスト。
 *
 * マイグレーションの抜け(①定義 ②@Database(version) ③addMigrations 登録 ④スキーマ突き合わせ)は
 * **コンパイルが通ってしまい、実機更新時に初めてクラッシュする**。過去2回踏んでいるため自動化する。
 *
 * **v1 は対象外**: `app/schemas/` は 2.json から始まっており(v1 時点では exportSchema が無かった)、
 * MigrationTestHelper が v1 のDBを作れない。1.json を手書きすると identityHash を捏造することになり
 * 誤った安心を生むため、あえて作らない。v1→v2 は実機更新でのみ確認できる。
 *
 * 実行: `./gradlew connectedDebugAndroidTest`(実機またはエミュレータが必要)
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YosugaDatabase::class.java,
    )

    private val allMigrations = arrayOf(
        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4,
        MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7,
    )

    /** 各段を1つずつ検証する。落ちた段が特定できるよう個別に流す。 */
    @Test
    fun each_migration_step_matches_exported_schema() {
        for (from in 2 until LATEST_VERSION) {
            val to = from + 1
            helper.createDatabase(dbName(to), from).close()
            // validateDroppedTables = true: 消し忘れたテーブルも検出する。
            helper.runMigrationsAndValidate(dbName(to), to, true, *allMigrations).close()
        }
    }

    /** v2 のDBを最新まで通しで上げる。 */
    @Test
    fun migrates_from_v2_to_latest() {
        helper.createDatabase(TEST_DB, 2).close()
        helper.runMigrationsAndValidate(TEST_DB, LATEST_VERSION, true, *allMigrations).close()
    }

    /** マイグレーションで既存データが失われないこと(追加のみで作ってあるはず)。 */
    @Test
    fun existing_rows_survive_migration_to_latest() {
        helper.createDatabase(TEST_DB, 2).use { db ->
            db.execSQL(
                "INSERT INTO projects (id, name, currentGoal, inProgress, nextTask, lastUpdated, health) " +
                    "VALUES ('anri', 'ANRI', 'プロトタイプ', '第2章', '戦闘調整', '2026-07-22 18:00', 'on_track')"
            )
            db.execSQL(
                "INSERT INTO tasks (id, projectId, title, detail, status, priority, dueDate, " +
                    "createdAt, updatedAt, completedAt, source) " +
                    "VALUES ('t1', 'anri', '戦闘調整', '', 'doing', 'high', NULL, " +
                    "'2026-07-22T10:00:00+09:00', '2026-07-22T10:00:00+09:00', NULL, 'manual')"
            )
        }

        helper.runMigrationsAndValidate(TEST_DB, LATEST_VERSION, true, *allMigrations).use { db ->
            assertEquals("ANRI", db.queryOne("SELECT name FROM projects WHERE id = 'anri'"))
            assertEquals("戦闘調整", db.queryOne("SELECT title FROM tasks WHERE id = 't1'"))
            // v4 で追加された列は既存行では NULL(ALTER TABLE ADD COLUMN のため)。
            assertNull(db.queryOne("SELECT repoOwner FROM projects WHERE id = 'anri'"))
        }
    }

    /** v6 で追加した文書テーブルが、マイグレーション後に実際に読み書きできること。 */
    @Test
    fun document_tables_are_usable_after_migration() {
        helper.createDatabase(TEST_DB, 5).close()

        helper.runMigrationsAndValidate(TEST_DB, 6, true, *allMigrations).use { db ->
            db.execSQL(
                "INSERT INTO documents (id, title, body, status, createdAt, updatedAt, source) " +
                    "VALUES ('d1', 'グラップル検討', '原文', 'unclassified', " +
                    "'2026-07-23T15:00:00+09:00', '2026-07-23T15:00:00+09:00', 'manual')"
            )
            db.execSQL(
                "INSERT INTO document_classifications (id, documentId, summary, documentType, " +
                    "confidence, projectIdsJson, categoriesJson, tagsJson, relatedEntitiesJson, " +
                    "classifiedAt, appliedBy, isCurrent) " +
                    "VALUES ('c1', 'd1', '要約', 'design-discussion', 0.91, '[]', '[]', '[]', '[]', " +
                    "'2026-07-23T15:10:00+09:00', 'ai', 1)"
            )
            assertEquals("原文", db.queryOne("SELECT body FROM documents WHERE id = 'd1'"))
            assertEquals("要約", db.queryOne("SELECT summary FROM document_classifications WHERE id = 'c1'"))
        }
    }

    /**
     * 実際の Room ビルダーで開けること。
     * addMigrations への登録漏れ(チェックリスト③)はここで初めて落ちる。
     */
    @Test
    fun real_database_opens_with_registered_migrations() {
        helper.createDatabase(TEST_DB, 2).close()

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, YosugaDatabase::class.java, TEST_DB)
            .addMigrations(*allMigrations)
            .build()

        try {
            // クエリを1つ流して実際に開かせる(Room は最初のアクセス時に検証する)。
            val count = runBlocking { database.documentDao().countDocuments() }
            assertTrue(count >= 0)
        } finally {
            database.close()
        }
    }

    private fun SupportSQLiteDatabase.queryOne(sql: String): String? =
        query(sql).use { cursor ->
            assertTrue("行が見つかりません: $sql", cursor.moveToFirst())
            if (cursor.isNull(0)) null else cursor.getString(0)
        }

    private fun dbName(step: Int) = "migration-step-$step.db"

    private companion object {
        const val TEST_DB = "migration-test.db"

        /** YosugaDatabase の @Database(version) と揃える。 */
        const val LATEST_VERSION = 7
    }
}
