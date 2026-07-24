package com.shiro.yosugahub.data.obsidian

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/**
 * SAF で選択されたツリーを読む [VaultReader] 実装(設計書v5 Phase 1-a)。
 *
 * `DocumentFile.listFiles()` は 1 件ごとに URI を組み立てるため Vault 規模では遅い。
 * ここでは [DocumentsContract] で子ドキュメントをまとめて query し、必要な列だけ取る。
 * ツリーURIは書き込み側([ObsidianVaultStore])と同じ DataStore の値を毎回読む。
 */
class SafVaultReader(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : VaultReader {

    override suspend fun listNotes(): VaultListing = withContext(Dispatchers.IO) {
        val uriString = userPreferencesRepository.obsidianVaultUri.first()
        if (uriString.isBlank()) return@withContext VaultListing.NotConfigured

        try {
            val tree = Uri.parse(uriString)
            val rootId = DocumentsContract.getTreeDocumentId(tree)
            val notes = mutableListOf<VaultNote>()
            // 深さ優先だと巨大な Vault で再帰が深くなるため、幅優先で明示的に回す。
            val queue = ArrayDeque(listOf(Folder(rootId, "")))

            while (queue.isNotEmpty() && notes.size < MAX_NOTES) {
                coroutineContext.ensureActive()
                val folder = queue.removeFirst()
                if (folder.depth > MAX_DEPTH) continue
                queue.addAll(collectChildren(tree, folder, notes))
            }
            VaultListing.Success(notes.sortedBy { it.relativePath.lowercase() })
        } catch (e: SecurityException) {
            // 権限が失効している(Vault の再選択が必要)。
            VaultListing.Failed("Vaultへのアクセス権限が切れています。設定で選び直してください。")
        } catch (e: Exception) {
            VaultListing.Failed("Vaultの読み取りに失敗しました: ${e.message.orEmpty()}")
        }
    }

    /** [folder] 直下を 1 回の query で走査し、`.md` を [into] へ足してサブフォルダを返す。 */
    private fun collectChildren(
        tree: Uri,
        folder: Folder,
        into: MutableList<VaultNote>,
    ): List<Folder> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folder.documentId)
        val subFolders = mutableListOf<Folder>()

        context.contentResolver.query(childrenUri, PROJECTION, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val documentId = cursor.getString(0)
                val displayName = cursor.getString(1).orEmpty()
                val mimeType = cursor.getString(2).orEmpty()
                // Obsidian の設定・ゴミ箱(.obsidian / .trash)などは対象外。
                if (displayName.isEmpty() || displayName.startsWith(".")) continue

                val relativePath =
                    if (folder.path.isEmpty()) displayName else "${folder.path}/$displayName"

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    subFolders += Folder(documentId, relativePath, folder.depth + 1)
                    continue
                }
                if (!displayName.endsWith(VaultNote.EXTENSION, ignoreCase = true)) continue
                if (into.size >= MAX_NOTES) break

                into += VaultNote(
                    relativePath = relativePath,
                    name = displayName,
                    documentUri = DocumentsContract
                        .buildDocumentUriUsingTree(tree, documentId).toString(),
                    lastModified = if (cursor.isNull(3)) 0L else cursor.getLong(3),
                    size = if (cursor.isNull(4)) 0L else cursor.getLong(4),
                )
            }
        }
        return subFolders
    }

    override suspend fun readNote(documentUri: String): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(Uri.parse(documentUri))?.use { input ->
                // 巨大ファイルで OOM を起こさないよう上限を設ける。
                val bytes = input.readUpTo(MAX_NOTE_BYTES)
                String(bytes, Charsets.UTF_8)
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun vaultName(): String {
        val uriString = userPreferencesRepository.obsidianVaultUri.first()
        return vaultNameOf(uriString)
    }

    private data class Folder(
        val documentId: String,
        val path: String,
        val depth: Int = 0,
    )

    companion object {
        private val PROJECTION = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_SIZE,
        )

        /** 暴走防止の上限。実運用の Vault はこれより十分小さい想定。 */
        private const val MAX_NOTES = 5_000
        private const val MAX_DEPTH = 12
        private const val MAX_NOTE_BYTES = 2 * 1024 * 1024

        /** ツリーURIから Vault 名を取り出す(例: `primary:Obsidian/YosugaVault` → `YosugaVault`)。 */
        fun vaultNameOf(uriString: String): String {
            if (uriString.isBlank()) return ""
            val decoded = Uri.decode(uriString)
            return decoded.substringAfterLast(':').substringAfterLast('/')
                .ifBlank { decoded.substringAfterLast('/') }
        }
    }
}

/** 先頭から最大 [limit] バイトだけ読む(それ以上は切り捨てる)。 */
private fun java.io.InputStream.readUpTo(limit: Int): ByteArray {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8 * 1024)
    while (buffer.size() < limit) {
        val read = read(chunk)
        if (read <= 0) break
        buffer.write(chunk, 0, minOf(read, limit - buffer.size()))
    }
    return buffer.toByteArray()
}
