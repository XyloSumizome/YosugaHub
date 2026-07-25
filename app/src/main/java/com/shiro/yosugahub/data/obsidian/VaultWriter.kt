package com.shiro.yosugahub.data.obsidian

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.shiro.yosugahub.data.local.datastore.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Vault への書き込み結果。 */
sealed interface VaultWriteResult {
    /** 実際に書いた Vault ルートからの相対パス(名前が衝突した場合はここが変わる)。 */
    data class Written(val path: String) : VaultWriteResult

    data object NotConfigured : VaultWriteResult
    data class Failed(val reason: String) : VaultWriteResult
}

/**
 * Vault へノートを書き込む抽象(v5 Phase 3-b)。
 *
 * 既存の [KnowledgeStore] は Vault 直下への追記専用。こちらは
 * **サブフォルダを作って新規ファイルを置く**用途で、役割が違うため分けている。
 */
interface VaultWriter {

    /**
     * [directory](Vault ルートからの相対)配下へ [fileName] を作成する。
     * **既存ファイルは上書きしない**(設計書v5 §10)。
     */
    suspend fun write(directory: String, fileName: String, content: String): VaultWriteResult

    /**
     * [vaultPath](Vault ルートからの相対)のファイルを**新しい内容で置き換える**(2026-07-25)。
     *
     * GitHub のノートは Claude Code だけが書き、人が Obsidian 側で直すことはない
     * (シロさんの運用確認)。だから同じ元ファイルの新しい版は、枝番で増やさず
     * ここで上書きする。ファイルが無ければ作る(手で消されていた場合の復元)。
     */
    suspend fun overwrite(vaultPath: String, content: String): VaultWriteResult
}

/**
 * SAF 実装。無ければフォルダを作りながら降りていく。
 *
 * 同名ファイルがあるときは `-2`, `-3` … と枝番を付けて**別ファイルとして作る**。
 * 上書きは絶対にしない(人が Obsidian で書いたノートを壊さないため)。
 */
class SafVaultWriter(
    private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository,
) : VaultWriter {

    override suspend fun write(
        directory: String,
        fileName: String,
        content: String,
    ): VaultWriteResult = withContext(Dispatchers.IO) {
        val uriString = userPreferencesRepository.obsidianVaultUri.first()
        if (uriString.isBlank()) return@withContext VaultWriteResult.NotConfigured

        try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
                ?: return@withContext VaultWriteResult.Failed("Vaultを開けません")
            if (!tree.canWrite()) {
                return@withContext VaultWriteResult.Failed("Vaultへの書き込み権限がありません")
            }

            val target = directory.split('/')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .fold(tree) { parent, segment ->
                    val existing = parent.findFile(segment)
                    when {
                        existing != null && existing.isDirectory -> existing
                        existing != null -> return@withContext VaultWriteResult.Failed(
                            "$segment はフォルダではありません",
                        )

                        else -> parent.createDirectory(segment)
                            ?: return@withContext VaultWriteResult.Failed(
                                "フォルダを作成できません: $segment",
                            )
                    }
                }

            val finalName = availableName(target, fileName)
                ?: return@withContext VaultWriteResult.Failed("同名ファイルが多すぎます: $fileName")

            val file = target.createFile("text/markdown", finalName)
                ?: return@withContext VaultWriteResult.Failed("ファイルを作成できません: $finalName")

            // 提供元が名前を変えることがあるため、実際に作られた名前を採用する。
            val writtenName = file.name ?: finalName
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            } ?: return@withContext VaultWriteResult.Failed("書き込みに失敗しました")

            VaultWriteResult.Written(joinPath(directory, writtenName))
        } catch (e: SecurityException) {
            VaultWriteResult.Failed("Vaultへのアクセス権限が切れています。設定で選び直してください。")
        } catch (e: Exception) {
            VaultWriteResult.Failed("書き込みに失敗しました: ${e.message.orEmpty()}")
        }
    }

    override suspend fun overwrite(
        vaultPath: String,
        content: String,
    ): VaultWriteResult = withContext(Dispatchers.IO) {
        val uriString = userPreferencesRepository.obsidianVaultUri.first()
        if (uriString.isBlank()) return@withContext VaultWriteResult.NotConfigured

        val directory = vaultPath.substringBeforeLast('/', missingDelimiterValue = "")
        val fileName = vaultPath.substringAfterLast('/')
        if (fileName.isBlank()) return@withContext VaultWriteResult.Failed("パスが不正です: $vaultPath")

        try {
            val tree = DocumentFile.fromTreeUri(context, Uri.parse(uriString))
                ?: return@withContext VaultWriteResult.Failed("Vaultを開けません")
            if (!tree.canWrite()) {
                return@withContext VaultWriteResult.Failed("Vaultへの書き込み権限がありません")
            }

            val target = directory.split('/')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .fold(tree) { parent, segment ->
                    val existing = parent.findFile(segment)
                    when {
                        existing != null && existing.isDirectory -> existing
                        existing != null -> return@withContext VaultWriteResult.Failed(
                            "$segment はフォルダではありません",
                        )

                        else -> parent.createDirectory(segment)
                            ?: return@withContext VaultWriteResult.Failed(
                                "フォルダを作成できません: $segment",
                            )
                    }
                }

            // 無ければ作る(過去に手で消されていても、新しい版で復元される)。
            val file = target.findFile(fileName)
                ?: target.createFile("text/markdown", fileName)
                ?: return@withContext VaultWriteResult.Failed("ファイルを作成できません: $fileName")

            // "wt" = truncate。前の内容は残さず、新しい版で丸ごと置き換える。
            context.contentResolver.openOutputStream(file.uri, "wt")?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
            } ?: return@withContext VaultWriteResult.Failed("書き込みに失敗しました")

            VaultWriteResult.Written(joinPath(directory, file.name ?: fileName))
        } catch (e: SecurityException) {
            VaultWriteResult.Failed("Vaultへのアクセス権限が切れています。設定で選び直してください。")
        } catch (e: Exception) {
            VaultWriteResult.Failed("書き込みに失敗しました: ${e.message.orEmpty()}")
        }
    }

    /** 空いている名前を探す。既存があれば `-2`, `-3` … と枝番を付ける。 */
    private fun availableName(directory: DocumentFile, fileName: String): String? {
        if (directory.findFile(fileName) == null) return fileName
        val base = fileName.removeSuffix(VaultNote.EXTENSION)
        for (suffix in 2..MAX_NAME_ATTEMPTS) {
            val candidate = "$base-$suffix${VaultNote.EXTENSION}"
            if (directory.findFile(candidate) == null) return candidate
        }
        return null
    }

    private companion object {
        const val MAX_NAME_ATTEMPTS = 50
    }
}

/** 表示・記録用のパス結合(空のディレクトリはルート扱い)。 */
internal fun joinPath(directory: String, fileName: String): String =
    if (directory.isBlank()) fileName else "${directory.trimEnd('/')}/$fileName"
