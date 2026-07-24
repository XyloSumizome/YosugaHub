package com.shiro.yosugahub.data.repository

import android.content.Context
import com.shiro.yosugahub.data.file.ContextHistoryNames
import com.shiro.yosugahub.data.obsidian.ContextFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 出力履歴の1件。 */
data class ContextHistoryEntry(
    val fileName: String,
    /** 表示用の保存時刻。名前から解釈できなければ空文字。 */
    val savedAt: String,
    /** "Markdown" / "JSON"。 */
    val format: String,
    val sizeBytes: Long,
)

/**
 * ヨスガへ渡したコンテキストの控え(設計書v5 Phase 2「出力履歴」)。
 *
 * **記録するのはプレビューを作った時ではなく、実際に外へ出した時**
 * (コピー / 保存 / 共有)。プレビューは何度も作り直すため、
 * それを全部残すと「何を渡したか」が history から読み取れなくなる。
 */
class ContextHistoryRepository(private val context: Context) {

    /** 出力内容を控えとして保存し、ファイル名を返す。失敗しても null を返すだけで止めない。 */
    suspend fun record(content: String, format: ContextFormat): String? =
        withContext(Dispatchers.IO) {
            try {
                val stamp = LocalDateTime.now().format(FILE_TIMESTAMP)
                val fileName = "context_$stamp${format.extension}"
                val dir = File(context.filesDir, HISTORY_DIR).apply { mkdirs() }
                File(dir, fileName).writeText(content)
                prune(dir)
                fileName
            } catch (e: IOException) {
                null
            }
        }

    /** 新しい順の一覧。ファイル名が時刻そのものなので名前の降順が時系列の降順になる。 */
    suspend fun history(limit: Int = HISTORY_LIMIT): List<ContextHistoryEntry> =
        withContext(Dispatchers.IO) {
            File(context.filesDir, HISTORY_DIR).listFiles()
                ?.filter { it.isFile && ContextHistoryNames.isValidHistoryName(it.name) }
                ?.sortedByDescending { it.name }
                ?.take(limit)
                ?.map { file ->
                    ContextHistoryEntry(
                        fileName = file.name,
                        savedAt = ContextHistoryNames.formatSavedAt(file.name),
                        format = ContextHistoryNames.formatLabel(file.name),
                        sizeBytes = file.length(),
                    )
                }
                .orEmpty()
        }

    /** 控えの中身を読む。想定外のファイル名は受け付けない(ディレクトリ外を読ませない)。 */
    suspend fun read(fileName: String): String? = withContext(Dispatchers.IO) {
        if (!ContextHistoryNames.isValidHistoryName(fileName)) return@withContext null
        val file = File(File(context.filesDir, HISTORY_DIR), fileName)
        try {
            if (file.isFile) file.readText() else null
        } catch (e: IOException) {
            null
        }
    }

    suspend fun delete(fileName: String): Boolean = withContext(Dispatchers.IO) {
        if (!ContextHistoryNames.isValidHistoryName(fileName)) return@withContext false
        File(File(context.filesDir, HISTORY_DIR), fileName).delete()
    }

    /** 古い控えを捨てる。コンテキストは大きくなりうるので溜め続けない。 */
    private fun prune(dir: File) {
        val files = dir.listFiles()
            ?.filter { it.isFile && ContextHistoryNames.isValidHistoryName(it.name) }
            ?.sortedByDescending { it.name }
            ?: return
        files.drop(HISTORY_LIMIT).forEach { it.delete() }
    }

    private companion object {
        const val HISTORY_DIR = "context_history"
        const val HISTORY_LIMIT = 20
        val FILE_TIMESTAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
    }
}
