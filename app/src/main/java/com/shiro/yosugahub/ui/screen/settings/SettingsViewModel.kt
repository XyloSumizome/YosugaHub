package com.shiro.yosugahub.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import com.shiro.yosugahub.data.obsidian.VaultListing
import com.shiro.yosugahub.data.repository.GitHubSettingsRepository
import com.shiro.yosugahub.data.repository.ImportHistoryEntry
import com.shiro.yosugahub.data.repository.ImportRepository
import com.shiro.yosugahub.data.repository.SampleDataRepository
import com.shiro.yosugahub.data.repository.SampleDataStatus
import com.shiro.yosugahub.data.repository.ServerSyncRepository
import com.shiro.yosugahub.data.repository.SyncResult
import com.shiro.yosugahub.data.repository.SyncSettingsRepository
import com.shiro.yosugahub.data.repository.VaultRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 設定画面が監視するUI状態(Obsidian Vault / GitHub トークン / サーバー同期)。 */
data class SettingsUiState(
    val obsidianVaultUri: String = "",
    val hasGitHubToken: Boolean = false,
    val syncBaseUrl: String = "",
    val hasSyncToken: Boolean = false,
    val isSyncing: Boolean = false,
)

/**
 * Vault の読み取り確認の結果(v5 Phase 1-a の実機確認用)。
 * 「選べたか」だけでなく「実際に読めるか・実用的な速度か」を見るためのもの。
 */
data class VaultCheckResult(
    val message: String,
    val samplePaths: List<String> = emptyList(),
    val isError: Boolean = false,
)

class SettingsViewModel(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val gitHubSettingsRepository: GitHubSettingsRepository,
    private val syncSettingsRepository: SyncSettingsRepository,
    private val serverSyncRepository: ServerSyncRepository,
    private val importRepository: ImportRepository,
    private val vaultRepository: VaultRepository,
    private val sampleDataRepository: SampleDataRepository,
) : ViewModel() {

    private val syncing = MutableStateFlow(false)

    private val _vaultChecking = MutableStateFlow(false)
    val vaultChecking: StateFlow<Boolean> = _vaultChecking

    private val _vaultCheck = MutableStateFlow<VaultCheckResult?>(null)
    val vaultCheck: StateFlow<VaultCheckResult?> = _vaultCheck

    private val _sampleData = MutableStateFlow(SampleDataStatus())
    val sampleData: StateFlow<SampleDataStatus> = _sampleData

    private val _sampleDataMessage = MutableStateFlow("")
    val sampleDataMessage: StateFlow<String> = _sampleDataMessage

    /** 残っている仮データの件数を数え直す(画面を開いたとき・削除後)。 */
    fun refreshSampleData() {
        viewModelScope.launch { _sampleData.value = sampleDataRepository.status() }
    }

    /**
     * 仮データを削除する。ID 指定で消すので実データは巻き込まない。
     * 削除後は再シードが止まる。
     */
    fun deleteSampleData(includeProjects: Boolean) {
        viewModelScope.launch {
            val result = sampleDataRepository.deleteSampleData(includeProjects)
            _sampleData.value = sampleDataRepository.status()
            _sampleDataMessage.value = if (result.total == 0) {
                "削除するサンプルデータはありませんでした(再投入は停止しました)。"
            } else {
                buildString {
                    append("サンプルデータを削除しました: ")
                    append(
                        listOfNotNull(
                            "プロジェクト${result.projects}".takeIf { result.projects > 0 },
                            "タスク${result.tasks}".takeIf { result.tasks > 0 },
                            "アイテム${result.items}".takeIf { result.items > 0 },
                            "日記${result.diaries}".takeIf { result.diaries > 0 },
                            "提案${result.recommendations}".takeIf { result.recommendations > 0 },
                        ).joinToString(" / ")
                    )
                }
            }
        }
    }

    fun clearSampleDataMessage() {
        _sampleDataMessage.value = ""
    }

    /**
     * Vault を列挙してみて、件数と所要時間を返す。
     * SAF の提供元によっては「選べるが読めない」ことがあるため、選択とは別に確かめる。
     */
    fun checkVault() {
        viewModelScope.launch {
            _vaultChecking.value = true
            _vaultCheck.value = null
            val startedAt = System.currentTimeMillis()
            val listing = vaultRepository.refresh()
            val elapsedMs = System.currentTimeMillis() - startedAt

            _vaultCheck.value = when (listing) {
                is VaultListing.Success -> if (listing.notes.isEmpty()) {
                    VaultCheckResult(
                        message = "読み取れましたが .md が0件でした(${elapsedMs}ms)。" +
                            "フォルダを間違えていないか、提供元が中身を返しているか確認してください。",
                        isError = true,
                    )
                } else {
                    VaultCheckResult(
                        message = "${listing.notes.size}件の .md が見つかりました(${elapsedMs}ms)",
                        samplePaths = listing.notes.take(SAMPLE_COUNT).map { it.relativePath },
                    )
                }

                VaultListing.NotConfigured ->
                    VaultCheckResult("Vaultフォルダが未選択です。", isError = true)

                is VaultListing.Failed ->
                    VaultCheckResult(listing.reason, isError = true)
            }
            _vaultChecking.value = false
        }
    }

    /**
     * 取り込み履歴。ファイル一覧は Flow ではないので、
     * 画面を開いたとき・取り込み後に読み直す(uiState の combine は5フロー上限のため独立させる)。
     */
    private val _importHistory = MutableStateFlow<List<ImportHistoryEntry>>(emptyList())
    val importHistory: StateFlow<List<ImportHistoryEntry>> = _importHistory

    fun refreshImportHistory() {
        viewModelScope.launch { _importHistory.value = importRepository.history() }
    }

    /** 履歴の中身を読む(何を取り込んだかの確認用)。 */
    fun readImportHistory(fileName: String, onResult: (String?) -> Unit) {
        viewModelScope.launch { onResult(importRepository.readHistory(fileName)) }
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferencesRepository.obsidianVaultUri,
        gitHubSettingsRepository.hasToken,
        syncSettingsRepository.baseUrl,
        syncSettingsRepository.hasToken,
        syncing,
    ) { vaultUri, hasGitHubToken, syncUrl, hasSyncToken, isSyncing ->
        SettingsUiState(
            obsidianVaultUri = vaultUri,
            hasGitHubToken = hasGitHubToken,
            syncBaseUrl = syncUrl,
            hasSyncToken = hasSyncToken,
            isSyncing = isSyncing,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    /** サーバー同期のURL保存(末尾スラッシュは正規化)。 */
    fun saveSyncUrl(url: String) {
        viewModelScope.launch { syncSettingsRepository.saveBaseUrl(url) }
    }

    /** サーバー同期トークンを暗号化して保存。結果を UI へ返す。 */
    fun saveSyncToken(token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(syncSettingsRepository.saveToken(token)) }
    }

    /** config.php に貼るトークンを生成する(保存はしない。表示して控えてもらう)。 */
    fun generateSyncToken(): String = SyncSettingsRepository.generateToken()

    /** AI向けJSONを生成してサーバーへ同期する。 */
    fun syncNow(onResult: (SyncResult) -> Unit) {
        viewModelScope.launch {
            syncing.value = true
            try {
                onResult(serverSyncRepository.sync())
            } finally {
                syncing.value = false
            }
        }
    }

    /** SAF で選択された Vault のツリーURIを保存する(権限の永続化は画面側で実施済み)。 */
    fun saveVaultUri(uri: String) {
        viewModelScope.launch {
            userPreferencesRepository.setObsidianVaultUri(uri)
        }
    }

    /** GitHub トークンを暗号化して保存する。結果(成功/失敗)を UI へ返す。 */
    fun saveGitHubToken(token: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(gitHubSettingsRepository.saveToken(token))
        }
    }

    fun clearGitHubToken() {
        viewModelScope.launch { gitHubSettingsRepository.clearToken() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                SettingsViewModel(
                    userPreferencesRepository = app.container.userPreferencesRepository,
                    gitHubSettingsRepository = app.container.gitHubSettingsRepository,
                    syncSettingsRepository = app.container.syncSettingsRepository,
                    serverSyncRepository = app.container.serverSyncRepository,
                    importRepository = app.container.importRepository,
                    vaultRepository = app.container.vaultRepository,
                    sampleDataRepository = app.container.sampleDataRepository,
                )
            }
        }

        /** 確認結果に出す一覧の件数(全部出すと画面が埋まるため先頭のみ)。 */
        private const val SAMPLE_COUNT = 5
    }
}
