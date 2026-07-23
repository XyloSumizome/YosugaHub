package com.shiro.yosugahub.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.shiro.yosugahub.data.local.db.dao.CalendarEventDao
import com.shiro.yosugahub.data.local.db.dao.DiaryDao
import com.shiro.yosugahub.data.local.db.dao.DirectiveDao
import com.shiro.yosugahub.data.local.db.dao.DocumentDao
import com.shiro.yosugahub.data.local.db.dao.KnowledgeDao
import com.shiro.yosugahub.data.local.db.dao.PendingProposalDao
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.dao.ProjectStatusDao
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.dao.TaskDao
import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import com.shiro.yosugahub.data.local.db.entity.DiaryEntryEntity
import com.shiro.yosugahub.data.local.db.entity.DirectiveEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentClassificationEntity
import com.shiro.yosugahub.data.local.db.entity.DocumentEntity
import com.shiro.yosugahub.data.local.db.entity.ItemEntityCrossRef
import com.shiro.yosugahub.data.local.db.entity.ItemTagCrossRef
import com.shiro.yosugahub.data.local.db.entity.KnowledgeItemEntity
import com.shiro.yosugahub.data.local.db.entity.PendingProposalEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectEntity
import com.shiro.yosugahub.data.local.db.entity.ProjectStatusCacheEntity
import com.shiro.yosugahub.data.local.db.entity.RecommendationEntity
import com.shiro.yosugahub.data.local.db.entity.TagEntity
import com.shiro.yosugahub.data.local.db.entity.TaskEntity
import com.shiro.yosugahub.data.local.db.entity.TrackedEntityEntity

/**
 * v2: tasks テーブルを追加(v3-Step 1)。
 * v3: 知識ベース関連(knowledge_items / tags / item_tags / entities / item_entities /
 *     diary_entries / pending_proposals)を追加(v3-Step 2)。
 * v4: projects に GitHub リポジトリ情報(repoOwner / repoName / repoBranch)を追加。
 * v5: project_status_cache(GitHub の status.json キャッシュ)を追加。
 * v6: AI分類ワークフロー(v4.1)の documents / document_classifications を追加。
 * v7: Claude Code への指示書(v4.2)の directives を追加。
 * マイグレーションは Migrations.kt。スキーマJSONは app/schemas/ に出力される。
 */
@Database(
    entities = [
        ProjectEntity::class,
        CalendarEventEntity::class,
        RecommendationEntity::class,
        TaskEntity::class,
        KnowledgeItemEntity::class,
        TagEntity::class,
        ItemTagCrossRef::class,
        TrackedEntityEntity::class,
        ItemEntityCrossRef::class,
        DiaryEntryEntity::class,
        PendingProposalEntity::class,
        ProjectStatusCacheEntity::class,
        DocumentEntity::class,
        DocumentClassificationEntity::class,
        DirectiveEntity::class,
    ],
    version = 7,
    exportSchema = true,
)
abstract class YosugaDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun taskDao(): TaskDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun diaryDao(): DiaryDao
    abstract fun pendingProposalDao(): PendingProposalDao
    abstract fun projectStatusDao(): ProjectStatusDao
    abstract fun documentDao(): DocumentDao
    abstract fun directiveDao(): DirectiveDao
}
