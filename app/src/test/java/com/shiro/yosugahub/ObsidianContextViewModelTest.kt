package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.TextDocumentWriter
import com.shiro.yosugahub.data.obsidian.ContextFormat
import com.shiro.yosugahub.data.obsidian.VaultListing
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.obsidian.VaultReader
import com.shiro.yosugahub.data.repository.VaultRepository
import com.shiro.yosugahub.ui.screen.obsidiancontext.ObsidianContextViewModel
import com.shiro.yosugahub.ui.screen.obsidiancontext.VaultLoadState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ObsidianContextViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        // viewModelScope は Dispatchers.Main を使うためテスト用に差し替える。
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeReader(
        private val listing: VaultListing,
        private val bodies: Map<String, String> = emptyMap(),
    ) : VaultReader {
        override suspend fun listNotes(): VaultListing = listing
        override suspend fun readNote(documentUri: String): String? = bodies[documentUri]
        override suspend fun vaultName(): String = "TestVault"
    }

    /** 保存は Android の URI が要るためテストでは呼ばない。 */
    private val NoopWriter = TextDocumentWriter { _, _ -> true }

    private fun note(path: String) = VaultNote(
        relativePath = path,
        name = path.substringAfterLast('/'),
        documentUri = "uri://$path",
        lastModified = 0L,
        size = 0L,
    )

    private val lighting = note("Games/ANRI/Design/Lighting.md")
    private val overview = note("Games/ANRI/Design/Overview.md")
    private val log = note("Games/ANRI/Logs/2026-07-23.md")

    private fun viewModel(
        notes: List<VaultNote> = listOf(lighting, overview, log),
        bodies: Map<String, String> = notes.associate { "uri://${it.relativePath}" to "本文" },
    ) = ObsidianContextViewModel(
        vaultRepository = VaultRepository(FakeReader(VaultListing.Success(notes), bodies)),
        documentWriter = NoopWriter,
    )

    @Test
    fun loads_notes_on_start() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        assertEquals(VaultLoadState.LOADED, vm.uiState.value.loadState)
        assertEquals(3, vm.uiState.value.notes.size)
    }

    @Test
    fun notes_are_grouped_by_folder() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        val grouped = vm.uiState.value.grouped
        assertEquals(listOf("Games/ANRI/Design", "Games/ANRI/Logs"), grouped.map { it.first })
        assertEquals(2, grouped[0].second.size)
    }

    @Test
    fun toggle_selects_and_deselects() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.toggle(lighting)
        assertEquals(setOf(lighting.relativePath), vm.uiState.value.selected)
        assertTrue(vm.uiState.value.canBuild)

        vm.toggle(lighting)
        assertTrue(vm.uiState.value.selected.isEmpty())
        assertFalse(vm.uiState.value.canBuild)
    }

    @Test
    fun folder_toggle_selects_all_then_clears_all() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.toggleFolder("Games/ANRI/Design")
        assertEquals(
            setOf(lighting.relativePath, overview.relativePath),
            vm.uiState.value.selected,
        )

        vm.toggleFolder("Games/ANRI/Design")
        assertTrue(vm.uiState.value.selected.isEmpty())
    }

    @Test
    fun build_preview_joins_selected_notes_only() = runTest(dispatcher) {
        val vm = viewModel(
            bodies = mapOf(
                "uri://Games/ANRI/Design/Lighting.md" to "選んだ本文",
                "uri://Games/ANRI/Logs/2026-07-23.md" to "選ばない本文",
            ),
        )
        advanceUntilIdle()

        vm.toggle(lighting)
        vm.buildPreview()
        advanceUntilIdle()

        val preview = vm.uiState.value.preview
        requireNotNull(preview)
        assertEquals(1, preview.noteCount)
        assertTrue(preview.content.contains("選んだ本文"))
        assertFalse(preview.content.contains("選ばない本文"))
        assertFalse(vm.uiState.value.isBuilding)
    }

    @Test
    fun changing_selection_drops_a_stale_preview() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.toggle(lighting)
        vm.buildPreview()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.preview != null)

        vm.toggle(log)
        // 選択が変わった時点で古いプレビューは捨てる
        assertTrue(vm.uiState.value.preview == null)
    }

    @Test
    fun not_configured_vault_is_surfaced_to_the_screen() = runTest(dispatcher) {
        val vm = ObsidianContextViewModel(
            vaultRepository = VaultRepository(FakeReader(VaultListing.NotConfigured)),
            documentWriter = NoopWriter,
        )
        advanceUntilIdle()

        assertEquals(VaultLoadState.NOT_CONFIGURED, vm.uiState.value.loadState)
    }

    @Test
    fun query_filters_the_visible_list_but_not_the_selection() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.toggle(lighting)
        vm.setQuery("Logs")

        assertEquals(listOf(log), vm.uiState.value.visible)
        // 隠れても選択は残る。貼り忘れ・貼りすぎを防ぐため件数で知らせる。
        assertEquals(setOf(lighting.relativePath), vm.uiState.value.selected)
        assertEquals(1, vm.uiState.value.hiddenSelectedCount)
    }

    @Test
    fun folder_toggle_only_touches_visible_notes() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setQuery("Lighting")
        vm.toggleFolder("Games/ANRI/Design")

        // 同じフォルダの Overview.md は絞り込みで隠れているので選ばれない
        assertEquals(setOf(lighting.relativePath), vm.uiState.value.selected)
    }

    @Test
    fun selecting_the_same_recent_range_twice_clears_it() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setRecentDays(7)
        assertEquals(7, vm.uiState.value.filter.recentDays)

        vm.setRecentDays(7)
        assertEquals(null, vm.uiState.value.filter.recentDays)
    }

    @Test
    fun clear_filter_restores_the_full_list() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setQuery("Logs")
        assertEquals(1, vm.uiState.value.visible.size)

        vm.clearFilter()
        assertEquals(3, vm.uiState.value.visible.size)
        assertFalse(vm.uiState.value.filter.isActive)
    }

    @Test
    fun filter_survives_a_refresh() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setQuery("Logs")
        vm.refresh()
        advanceUntilIdle()

        assertEquals("Logs", vm.uiState.value.filter.query)
        assertEquals(listOf(log), vm.uiState.value.visible)
    }

    @Test
    fun switching_format_reformats_without_reading_files_again() = runTest(dispatcher) {
        var reads = 0
        val reader = object : VaultReader {
            override suspend fun listNotes(): VaultListing =
                VaultListing.Success(listOf(lighting))

            override suspend fun readNote(documentUri: String): String {
                reads++
                return "本文"
            }

            override suspend fun vaultName(): String = "TestVault"
        }
        val vm = ObsidianContextViewModel(
            vaultRepository = VaultRepository(reader),
            documentWriter = NoopWriter,
        )
        advanceUntilIdle()

        vm.toggle(lighting)
        vm.buildPreview()
        advanceUntilIdle()
        assertEquals(1, reads)
        assertTrue(vm.uiState.value.preview!!.content.startsWith("---"))

        vm.setFormat(ContextFormat.JSON)

        // 形式を変えてもファイルは読み直さない
        assertEquals(1, reads)
        assertEquals(ContextFormat.JSON, vm.uiState.value.format)
        val content = vm.uiState.value.preview!!.content
        assertTrue(content.trimStart().startsWith("{"))
        assertTrue(content.contains("yosuga-context"))
        assertTrue(vm.uiState.value.preview!!.fileName.endsWith(".json"))
    }

    @Test
    fun format_is_kept_for_the_next_build() = runTest(dispatcher) {
        val vm = viewModel()
        advanceUntilIdle()

        vm.setFormat(ContextFormat.JSON)
        vm.toggle(lighting)
        vm.buildPreview()
        advanceUntilIdle()

        assertTrue(vm.uiState.value.preview!!.content.trimStart().startsWith("{"))
    }

    @Test
    fun refresh_drops_selection_of_notes_that_disappeared() = runTest(dispatcher) {
        val reader = object : VaultReader {
            var notes = listOf(lighting, log)
            override suspend fun listNotes(): VaultListing = VaultListing.Success(notes)
            override suspend fun readNote(documentUri: String): String = "本文"
            override suspend fun vaultName(): String = "TestVault"
        }
        val vm = ObsidianContextViewModel(
            vaultRepository = VaultRepository(reader),
            documentWriter = NoopWriter,
        )
        advanceUntilIdle()

        vm.toggle(lighting)
        vm.toggle(log)
        assertEquals(2, vm.uiState.value.selectedCount)

        reader.notes = listOf(log)
        vm.refresh()
        advanceUntilIdle()

        assertEquals(setOf(log.relativePath), vm.uiState.value.selected)
    }
}
