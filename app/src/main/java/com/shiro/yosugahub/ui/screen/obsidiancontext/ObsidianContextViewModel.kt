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
import com.shiro.yosugahub.data.obsidian.VaultListing
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.repository.ContextBuildResult
import com.shiro.yosugahub.data.repository.VaultRepository
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
    val errorMessage: String = "",
    val isBuilding: Boolean = false,
    /** 生成済みのコンテキスト。null ならプレビュー未生成。 */
    val preview: ContextBuildResult? = null,
) {
    val selectedCount: Int get() = selected.size
    val canBuild: Boolean get() = selected.isNotEmpty() && !isBuilding

    /** フォルダごとにまとめた一覧(Vault 直下は空文字キー)。 */
    val grouped: List<Pair<String, List<VaultNote>>>
        get() = notes.groupBy { it.folder }.toList().sortedBy { it.first }
}

/**
 * Obsidian からヨスガへ渡すコンテキストを組み立てる画面の ViewModel(設計書v5 Phase 1-b)。
 * 判断は行わない。**ユーザーが選んだものを、そのまま連結する**だけ。
 */
class ObsidianContextViewModel(
    private val vaultRepository: VaultRepository,
    private val documentWriter: TextDocumentWriter,
) : ViewModel() {

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

    /** フォルダ単位のまとめて選択・解除。全部入っていれば解除、そうでなければ全選択。 */
    fun toggleFolder(folder: String) {
        val paths = _uiState.value.notes
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

    /** 選択されたノートを 1 本のコンテキスト Markdown にする。 */
    fun buildPreview() {
        val state = _uiState.value
        if (!state.canBuild) return
        val targets = state.notes.filter { it.relativePath in state.selected }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBuilding = true)
            val result = runCatching { vaultRepository.buildContext(targets) }
            _uiState.value = _uiState.value.copy(
                isBuilding = false,
                preview = result.getOrNull(),
                errorMessage = result.exceptionOrNull()
                    ?.let { "コンテキストの生成に失敗しました: ${it.message.orEmpty()}" }
                    .orEmpty(),
            )
        }
    }

    fun dismissPreview() {
        _uiState.value = _uiState.value.copy(preview = null)
    }

    /** SAF で作成されたファイルへプレビュー内容を書き出す(Phase 1-c)。 */
    fun saveTo(uri: Uri, onResult: (Boolean) -> Unit) {
        val markdown = _uiState.value.preview?.markdown
        if (markdown == null) {
            onResult(false)
            return
        }
        viewModelScope.launch { onResult(documentWriter.write(uri, markdown)) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as YosugaHubApplication
                ObsidianContextViewModel(
                    vaultRepository = app.container.vaultRepository,
                    documentWriter = app.container.documentWriter,
                )
            }
        }
    }
}
