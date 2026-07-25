package com.shiro.yosugahub.ui.screen.obsidiancontext

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.shiro.yosugahub.YosugaHubApplication
import com.shiro.yosugahub.data.file.TextDocumentWriter
import com.shiro.yosugahub.data.obsidian.ContextFormat
import com.shiro.yosugahub.data.obsidian.NoteFilter
import com.shiro.yosugahub.data.obsidian.TagIndex
import com.shiro.yosugahub.data.obsidian.VaultListing
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.obsidian.VaultNoteFilters
import com.shiro.yosugahub.data.repository.ContextBuildResult
import com.shiro.yosugahub.data.repository.ContextHistoryEntry
import com.shiro.yosugahub.data.repository.ContextHistoryRepository
import com.shiro.yosugahub.data.repository.ExportRepository
import com.shiro.yosugahub.data.repository.VaultRepository
import com.shiro.yosugahub.ui.component.LogLine
import com.shiro.yosugahub.ui.component.LogTone
import com.shiro.yosugahub.ui.component.OpLogState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Vault 読み込みの状態。画面の出し分けに使う。 */
enum class VaultLoadState { IDLE, LOADING, LOADED, NOT_CONFIGURED, FAILED }

data class ObsidianContextUiState(
    val loadState: VaultLoadState = VaultLoadState.IDLE,
    val notes: List<VaultNote> = emptyList(),
    /** 選択されたノートの相対パス。順序は一覧の並びに従うので Set で持つ。 */
    val selected: Set<String> = emptySet(),
    val filter: NoteFilter = NoteFilter(),
    /** 出力形式。正本は Markdown、JSON は機械処理向けの追加(設計書v5 §6)。 */
    val format: ContextFormat = ContextFormat.MARKDOWN,
    /** [filter] を適用した表示対象。選択は絞り込みに影響されない。 */
    val visible: List<VaultNote> = emptyList(),
    /** タグ索引。作るまで空(タグ絞り込みは使えないが他の条件は動く)。 */
    val tagIndex: TagIndex = TagIndex.EMPTY,
    val isIndexing: Boolean = false,
    val errorMessage: String = "",
    val isBuilding: Boolean = false,
    /** 生成済みのコンテキスト。null ならプレビュー未生成。 */
    val preview: ContextBuildResult? = null,
    /**
     * 現況(Hub がいま持っている状態)を含めるか(2026-07-25)。
     * **既定でオン**——ヨスガへ渡すとき、まず要るのは「いまどうなっているか」。
     * 過去ログ(Obsidian のノート)は必要なときだけ足す。
     */
    val includeStatus: Boolean = true,
) {
    val selectedCount: Int get() = selected.size

    /** 現況だけでも渡す価値があるので、ノート未選択でも生成できる。 */
    val canBuild: Boolean get() = (includeStatus || selected.isNotEmpty()) && !isBuilding

    /** 絞り込みで隠れているだけの選択があるか(件数の食い違いを画面で説明するため)。 */
    val hiddenSelectedCount: Int
        get() = selected.count { path -> visible.none { it.relativePath == path } }

    /** フォルダごとにまとめた表示用の一覧(Vault 直下は空文字キー)。 */
    val grouped: List<Pair<String, List<VaultNote>>>
        get() = visible.groupBy { it.folder }.toList().sortedBy { it.first }
}

/**
 * Obsidian からヨスガへ渡すコンテキストを組み立てる画面の ViewModel(設計書v5 Phase 1-b)。
 * 判断は行わない。**ユーザーが選んだものを、そのまま連結する**だけ。
 */
class ObsidianContextViewModel(
    private val vaultRepository: VaultRepository,
    private val documentWriter: TextDocumentWriter,
    /** 現況(状況JSON)の生成。渡さなければ現況なしで動く。 */
    private val exportRepository: ExportRepository? = null,
    private val historyRepository: ContextHistoryRepository? = null,
    /** 「最近更新」の基準時刻。テストで固定できるようにする。 */
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    /** BUILD CONTEXT の端末ログ(v5 UI)。 */
    val opLog = OpLogState()

    private val _uiState = MutableStateFlow(ObsidianContextUiState())
    val uiState: StateFlow<ObsidianContextUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    /** Vault を再列挙する。選択状態は、消えたノートを除いて引き継ぐ。 */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadState = VaultLoadState.LOADING,
                errorMessage = "",
            )
            _uiState.value = when (val listing = vaultRepository.refresh()) {
                is VaultListing.Success -> {
                    val available = listing.notes.map { it.relativePath }.toSet()
                    _uiState.value.copy(
                        loadState = VaultLoadState.LOADED,
                        notes = listing.notes,
                        selected = _uiState.value.selected intersect available,
                        visible = filtered(listing.notes, _uiState.value.filter),
                    )
                }

                VaultListing.NotConfigured -> _uiState.value.copy(
                    loadState = VaultLoadState.NOT_CONFIGURED,
                    notes = emptyList(),
                )

                is VaultListing.Failed -> _uiState.value.copy(
                    loadState = VaultLoadState.FAILED,
                    errorMessage = listing.reason,
                )
            }
        }
    }

    /** パス・ファイル名での絞り込み。ファイルは読まないので即座に効く。 */
    fun setQuery(query: String) {
        applyFilter(_uiState.value.filter.copy(query = query))
    }

    /** 「最近更新」の絞り込み。同じ日数をもう一度選ぶと解除する。 */
    fun setRecentDays(days: Int?) {
        val current = _uiState.value.filter
        val next = if (current.recentDays == days) null else days
        applyFilter(current.copy(recentDays = next))
    }

    fun clearFilter() {
        applyFilter(NoteFilter())
    }

    private fun applyFilter(filter: NoteFilter) {
        val state = _uiState.value
        // 絞り込みは表示だけを変える。選択とプレビューには触らない。
        _uiState.value = state.copy(filter = filter, visible = filtered(state.notes, filter))
    }

    private fun filtered(notes: List<VaultNote>, filter: NoteFilter): List<VaultNote> =
        VaultNoteFilters.apply(notes, filter, nowMillis(), _uiState.value.tagIndex)

    /**
     * タグ索引を作る(全ノートを開くので明示的に呼ぶ)。
     * 作り終えたら、いま効いている絞り込みを適用し直す。
     */
    fun buildTagIndex() {
        if (_uiState.value.isIndexing) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isIndexing = true)
            val index = runCatching { vaultRepository.buildTagIndex(_uiState.value.notes) }
                .getOrDefault(TagIndex.EMPTY)
            val state = _uiState.value.copy(isIndexing = false, tagIndex = index)
            _uiState.value = state.copy(
                visible = VaultNoteFilters.apply(state.notes, state.filter, nowMillis(), index),
            )
        }
    }

    /** タグの選択を切り替える(複数選んだ場合は OR)。 */
    fun toggleTag(tag: String) {
        val current = _uiState.value.filter
        val next = if (tag in current.tags) current.tags - tag else current.tags + tag
        applyFilter(current.copy(tags = next))
    }

    fun toggle(note: VaultNote) {
        val current = _uiState.value.selected
        val next = if (note.relativePath in current) {
            current - note.relativePath
        } else {
            current + note.relativePath
        }
        // 選択を変えたらプレビューは古くなるので捨てる。
        _uiState.value = _uiState.value.copy(selected = next, preview = null)
    }

    /**
     * フォルダ単位のまとめて選択・解除。全部入っていれば解除、そうでなければ全選択。
     * 対象は**画面に出ているノートだけ**(絞り込みで隠れているものは動かさない)。
     */
    fun toggleFolder(folder: String) {
        val paths = _uiState.value.visible
            .filter { it.folder == folder }
            .map { it.relativePath }
            .toSet()
        val current = _uiState.value.selected
        val next = if (paths.all { it in current }) current - paths else current + paths
        _uiState.value = _uiState.value.copy(selected = next, preview = null)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selected = emptySet(), preview = null)
    }

    /** 選択されたノートを 1 本のコンテキストにまとめる。 */
    /** 現況を含めるかを切り替える。切り替えたら作り直しになるのでプレビューは消す。 */
    fun setIncludeStatus(include: Boolean) {
        _uiState.value = _uiState.value.copy(includeStatus = include, preview = null)
    }

    fun buildPreview() {
        val state = _uiState.value
        if (!state.canBuild) return
        val targets = state.notes.filter { it.relativePath in state.selected }
        val wantStatus = state.includeStatus

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBuilding = true)
            val result = opLog.run { emit ->
                emit.emit(LogLine("> SHARE TO YOSUGA", LogTone.ACCENT))
                if (wantStatus) emit.emit(LogLine("  STATUS 現況を取得 …", LogTone.INFO))
                emit.emit(LogLine("  READ ${targets.size} note(s) …", LogTone.INFO))
                val r = runCatching {
                    // 現況の生成に失敗しても過去ログは渡せるので、ここで全体を止めない。
                    val status = if (wantStatus) {
                        runCatching { exportRepository?.createContextExport()?.json }
                            .getOrNull().orEmpty()
                    } else {
                        ""
                    }
                    if (wantStatus && status.isBlank()) {
                        emit.emit(LogLine("  WARN 現況を取得できませんでした", LogTone.WARN))
                    }
                    val data = vaultRepository.loadContext(targets, status = status)
                    vaultRepository.format(data, _uiState.value.format)
                }
                r.onSuccess {
                    if (it.skipped.isNotEmpty()) {
                        emit.emit(LogLine("  SKIP ${it.skipped.size} unreadable", LogTone.WARN))
                    }
                    emit.emit(LogLine("  JOIN ${it.noteCount} note(s) → ${it.charCount} chars", LogTone.OK))
                    emit.emit(LogLine("  FORMAT ${it.format.label}", LogTone.INFO))
                }.onFailure {
                    emit.emit(LogLine("  FAIL 生成に失敗しました", LogTone.ERROR))
                }
                emit.emit(LogLine("> DONE", LogTone.ACCENT))
                r
            } ?: return@launch
            _uiState.value = _uiState.value.copy(
                isBuilding = false,
                preview = result.getOrNull(),
                errorMessage = result.exceptionOrNull()
                    ?.let { "コンテキストの生成に失敗しました: ${it.message.orEmpty()}" }
                    .orEmpty(),
            )
        }
    }

    /**
     * 出力形式を切り替える。**ファイルは読み直さない**
     * (中間表現から整形し直すだけ)。
     */
    fun setFormat(format: ContextFormat) {
        val state = _uiState.value
        if (state.format == format) return
        val preview = state.preview?.let { vaultRepository.format(it.data, format) }
        _uiState.value = state.copy(format = format, preview = preview)
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(preview = null)
    }

    /** SAF で作成されたファイルへプレビュー内容を書き出す(Phase 1-c)。 */
    fun saveTo(uri: Uri, onResult: (Boolean) -> Unit) {
        val preview = _uiState.value.preview
        if (preview == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val saved = documentWriter.write(uri, preview.content)
            if (saved) recordHistory()
            onResult(saved)
        }
    }

    /**
     * 実際に外へ出したときだけ控えを残す(Phase 2「出力履歴」)。
     * プレビューは何度も作り直すため、生成のたびに残すと何を渡したか分からなくなる。
     */
    fun recordHistory() {
        val preview = _uiState.value.preview ?: return
        val repository = historyRepository ?: return
        viewModelScope.launch {
            repository.record(preview.content, preview.format)
            _history.value = repository.history()
        }
    }

    private val _history = MutableStateFlow<List<ContextHistoryEntry>>(emptyList())
    val history: StateFlow<List<ContextHistoryEntry>> = _history.asStateFlow()

    fun refreshHistory() {
        val repository = historyRepository ?: return
        viewModelScope.launch { _history.value = repository.history() }
    }

    /** 控えの中身を読む(何を渡したかの確認用)。 */
    fun readHistory(fileName: String, onResult: (String?) -> Unit) {
        val repository = historyRepository
        if (repository == null) {
            onResult(null)
            return
        }
        viewModelScope.launch { onResult(repository.read(fileName)) }
    }

    fun deleteHistory(fileName: String) {
        val repository = historyRepository ?: return
        viewModelScope.launch {
            repository.delete(fileName)
            _history.value = repository.history()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                ObsidianContextViewModel(
                    vaultRepository = app.container.vaultRepository,
                    documentWriter = app.container.documentWriter,
                    exportRepository = app.container.exportRepository,
                    historyRepository = app.container.contextHistoryRepository,
                )
            }
        }
    }
}
