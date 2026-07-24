package com.shiro.yosugahub

import com.shiro.yosugahub.data.obsidian.NoteTransformer
import com.shiro.yosugahub.data.obsidian.VaultListing
import com.shiro.yosugahub.data.obsidian.VaultNote
import com.shiro.yosugahub.data.obsidian.VaultReader
import com.shiro.yosugahub.data.repository.VaultRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

/** SAF を使わずに Vault を差し替えるためのテスト用リーダー。 */
private class FakeVaultReader(
    private val listing: VaultListing,
    private val bodies: Map<String, String> = emptyMap(),
    private val name: String = "TestVault",
) : VaultReader {
    override suspend fun listNotes(): VaultListing = listing
    override suspend fun readNote(documentUri: String): String? = bodies[documentUri]
    override suspend fun vaultName(): String = name
}

class VaultRepositoryTest {

    private val now = OffsetDateTime.of(2026, 7, 24, 8, 0, 0, 0, ZoneOffset.ofHours(9))

    private fun note(path: String, uri: String, lastModified: Long = 0L) = VaultNote(
        relativePath = path,
        name = path.substringAfterLast('/'),
        documentUri = uri,
        lastModified = lastModified,
        size = 0L,
    )

    @Test
    fun refresh_caches_notes_on_success() = runTest {
        val notes = listOf(note("Games/ANRI/Design/Lighting.md", "uri://1"))
        val repository = VaultRepository(FakeVaultReader(VaultListing.Success(notes)))

        assertTrue(repository.notes.value.isEmpty())
        val listing = repository.refresh()

        assertTrue(listing is VaultListing.Success)
        assertEquals(notes, repository.notes.value)
    }

    @Test
    fun refresh_keeps_previous_cache_when_it_fails() = runTest {
        val repository = VaultRepository(FakeVaultReader(VaultListing.NotConfigured))

        val listing = repository.refresh()

        assertEquals(VaultListing.NotConfigured, listing)
        assertTrue(repository.notes.value.isEmpty())
    }

    @Test
    fun build_context_reads_selected_notes_and_joins_them() = runTest {
        val first = note("Games/ANRI/Design/Lighting.md", "uri://1")
        val second = note("Games/ANRI/Logs/2026-07-23.md", "uri://2")
        val repository = VaultRepository(
            FakeVaultReader(
                listing = VaultListing.Success(listOf(first, second)),
                bodies = mapOf(
                    "uri://1" to "---\ngame: ANRI\nupdated_at: 2026-07-23T21:15:00+09:00\n---\n\n減衰をゆるく。",
                    "uri://2" to "---\ngame: ANRI\n---\n\nカーブを差し替えた。",
                ),
            )
        )

        val result = repository.buildContext(listOf(first, second), now = now)

        assertEquals(2, result.noteCount)
        assertEquals("yosuga_context_2026-07-24.md", result.fileName)
        assertTrue(result.skipped.isEmpty())
        assertTrue(result.content.contains("減衰をゆるく。"))
        assertTrue(result.content.contains("カーブを差し替えた。"))
        assertTrue(result.content.contains("## ANRI / Lighting"))
        assertEquals(result.content.length, result.charCount)
    }

    @Test
    fun unreadable_notes_are_reported_but_do_not_stop_the_build() = runTest {
        val ok = note("A.md", "uri://ok")
        val broken = note("B.md", "uri://missing")
        val repository = VaultRepository(
            FakeVaultReader(
                listing = VaultListing.Success(listOf(ok, broken)),
                bodies = mapOf("uri://ok" to "本文A"),
            )
        )

        val result = repository.buildContext(listOf(ok, broken), now = now)

        assertEquals(1, result.noteCount)
        assertEquals(listOf("B.md"), result.skipped)
        assertTrue(result.content.contains("本文A"))
    }

    @Test
    fun file_timestamp_is_used_when_frontmatter_has_no_date() = runTest {
        // 2026-07-23T21:15:00+09:00 に相当するエポックミリ秒
        val epoch = OffsetDateTime.of(2026, 7, 23, 21, 15, 0, 0, ZoneOffset.ofHours(9))
            .toInstant().toEpochMilli()
        val target = note("A.md", "uri://a", lastModified = epoch)
        val repository = VaultRepository(
            reader = FakeVaultReader(
                listing = VaultListing.Success(listOf(target)),
                bodies = mapOf("uri://a" to "本文だけのノート"),
            ),
            zoneId = ZoneOffset.ofHours(9),
        )

        val result = repository.buildContext(listOf(target), now = now)

        assertTrue(result.content.contains("- Updated: `2026-07-23T21:15:00+09:00`"))
    }

    @Test
    fun transformer_is_the_only_place_that_touches_the_body() = runTest {
        val target = note("A.md", "uri://a")
        val repository = VaultRepository(
            reader = FakeVaultReader(
                listing = VaultListing.Success(listOf(target)),
                bodies = mapOf("uri://a" to "とても長い原文"),
            ),
            // Phase 4 の要約差し替えを想定した検証
            transformer = NoteTransformer { notes -> notes.map { it.copy(body = "要約") } },
        )

        val result = repository.buildContext(listOf(target), now = now)

        assertTrue(result.content.contains("要約"))
        assertTrue(!result.content.contains("とても長い原文"))
    }
}
