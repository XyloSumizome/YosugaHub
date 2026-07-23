package com.shiro.yosugahub.data.obsidian

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * SAF で選択した Obsidian Vault へ書き込む KnowledgeStore 実装(v3-Step 3)。
 * Vault のツリーURIは DataStore に永続化されたものを毎回読む(設定変更に追従)。
 * ノートが無ければ作成し、あれば末尾へ追記する("wa" append モード)。
 */
class ObsidianVaultStore(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : KnowledgeStore {

    override suspend fun appendToNote(noteName: String, markdown: String): AppendOutcome =
        withContext(Dispatchers.IO) {
            val uriString = userPreferencesRepository.obsidianVaultUri.first()
            if (uriString.isBlank()) return@withContext AppendOutcome.NOT_CONFIGURED

            try {
                val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
                    ?: return@withContext AppendOutcome.FAILED
                if (!tree.canWrite()) return@withContext AppendOutcome.FAILED

                val fileName = ObsidianMarkdown.normalizeNoteName(noteName)
                val target = tree.findFile(fileName)
                    ?: tree.createFile("text/markdown", fileName)
                    ?: return@withContext AppendOutcome.FAILED

                context.contentResolver.openOutputStream(target.uri, "wa")?.use { output ->
                    output.write(markdown.toByteArray(Charsets.UTF_8))
                } ?: return@withContext AppendOutcome.FAILED

                AppendOutcome.WRITTEN
            } catch (e: SecurityException) {
                // 権限が失効した場合(Vault再選択が必要)。
                AppendOutcome.FAILED
            } catch (e: Exception) {
                AppendOutcome.FAILED
            }
        }
}
