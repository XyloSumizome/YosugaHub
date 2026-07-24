package com.shiro.yosugahub.data.repository

import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.local.db.SampleSeed
import com.shiro.yosugahub.data.local.db.dao.DiaryDao
import com.shiro.yosugahub.data.local.db.dao.KnowledgeDao
import com.shiro.yosugahub.data.local.db.dao.ProjectDao
import com.shiro.yosugahub.data.local.db.dao.ProjectStatusDao
import com.shiro.yosugahub.data.local.db.dao.RecommendationDao
import com.shiro.yosugahub.data.local.db.dao.TaskDao

/** 残っている仮データの件数。0 件なら画面で削除ボタンを出す必要がない。 */
data class SampleDataStatus(
    val projects: Int = 0,
    val tasks: Int = 0,
    val items: Int = 0,
    val diaries: Int = 0,
    /** 再シードを止める指示が既に出ているか。 */
    val seedingDisabled: Boolean = false,
) {
    /** プロジェクトを除いた仮データが残っているか。 */
    val hasNonProjectData: Boolean get() = tasks > 0 || items > 0 || diaries > 0

    val hasAny: Boolean get() = hasNonProjectData || projects > 0
}

/** 削除結果。何をどれだけ消したかを画面へ返す。 */
data class SampleDataDeleteResult(
    val projects: Int = 0,
    val tasks: Int = 0,
    val items: Int = 0,
    val diaries: Int = 0,
    val recommendations: Int = 0,
) {
    val total: Int get() = projects + tasks + items + diaries + recommendations
}

/**
 * 初回起動時に投入される仮データ([SampleSeed])を後から片付ける(選択A)。
 *
 * **ID を指定して消す。**「テーブルを空にする」ではないので、
 * ユーザーが作った実データや AI 取り込みで増えた行は巻き込まない。
 * 消したあとは再シードを止めるフラグを立てる(消しても復活する問題への対処)。
 */
class SampleDataRepository(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val knowledgeDao: KnowledgeDao,
    private val diaryDao: DiaryDao,
    private val recommendationDao: RecommendationDao,
    private val projectStatusDao: ProjectStatusDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

    suspend fun status(): SampleDataStatus = SampleDataStatus(
        projects = projectDao.countByIds(SampleSeed.projectIds),
        tasks = taskDao.countByIds(SampleSeed.taskIds),
        items = knowledgeDao.countItemsByIds(SampleSeed.itemIds),
        diaries = diaryDao.countByIds(SampleSeed.diaryIds),
        seedingDisabled = userPreferencesRepository.isSeedingDisabled(),
    )

    /**
     * 仮データを削除する。
     *
     * @param includeProjects プロジェクトも消すか。プロジェクト名が実在するゲームの場合は
     *   残せるよう既定では消さない(消すと GitHub 設定などもやり直しになる)。
     */
    suspend fun deleteSampleData(includeProjects: Boolean): SampleDataDeleteResult {
        var tasks = 0
        var items = 0
        var diaries = 0
        var recommendations = 0
        var projects = 0

        SampleSeed.taskIds.forEach { id ->
            if (taskDao.countByIds(listOf(id)) > 0) {
                taskDao.deleteById(id)
                tasks++
            }
        }
        SampleSeed.itemIds.forEach { id ->
            if (knowledgeDao.countItemsByIds(listOf(id)) > 0) {
                // 中間テーブルの行も一緒に消える。
                knowledgeDao.deleteItemWithRefs(id)
                items++
            }
        }
        // タグ・エンティティは「どこからも参照されなくなったもの」だけ消す。
        SampleSeed.tags.forEach { knowledgeDao.deleteTagIfUnused(it.id) }
        SampleSeed.entities.forEach { knowledgeDao.deleteEntityIfUnused(it.id) }

        SampleSeed.diaryIds.forEach { id ->
            if (diaryDao.countByIds(listOf(id)) > 0) {
                diaryDao.deleteById(id)
                diaries++
            }
        }
        // 提案(recommendations)は自動採番のため projectId + title で特定する。
        SampleSeed.recommendations.forEach { recommendation ->
            recommendations += recommendationDao.deleteByProjectAndTitle(
                projectId = recommendation.projectId,
                title = recommendation.title,
            )
        }

        if (includeProjects) {
            SampleSeed.projectIds.forEach { id ->
                // プロジェクトに紐づくタスクと進捗キャッシュも残さない。
                taskDao.deleteByProject(id)
                projectStatusDao.deleteByProject(id)
                projects += projectDao.deleteById(id)
            }
        }

        // 消したものが復活しないようにする(seedIfEmpty はテーブルが空だと再投入するため)。
        userPreferencesRepository.setSeedingDisabled(true)

        return SampleDataDeleteResult(
            projects = projects,
            tasks = tasks,
            items = items,
            diaries = diaries,
            recommendations = recommendations,
        )
    }
}
