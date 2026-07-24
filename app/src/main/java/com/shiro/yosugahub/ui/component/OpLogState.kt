package com.shiro.yosugahub.ui.component

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 端末ログの流し込みを担う再利用ヘルパ(v5 UI: システムが動いている演出)。
 *
 * ViewModel が1つ持ち、`run { emit -> ... }` の中で本物の処理を回しながら
 * `emit(line)` でログを1行ずつ流す。各行の後に少しタメる。
 * どの操作(取り込み/保存/生成/同期)でも同じ見え方になる。
 */
class OpLogState(private val lineDelayMs: Long = 150L) {

    private val _lines = MutableStateFlow<List<LogLine>>(emptyList())
    val lines: StateFlow<List<LogLine>> = _lines.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    /** 1行ずつ流すための送信口。 */
    fun interface Emitter {
        suspend fun emit(line: LogLine)
    }

    /**
     * 演出付きで [block] を実行する。多重起動は無視する。
     * @return block の戻り値(結果を後段で使いたいとき用)
     */
    suspend fun <T> run(block: suspend (Emitter) -> T): T? {
        if (_running.value) return null
        _running.value = true
        _lines.value = emptyList()
        try {
            return block { line ->
                _lines.value = _lines.value + line
                delay(lineDelayMs)
            }
        } finally {
            _running.value = false
        }
    }

    fun clear() {
        _lines.value = emptyList()
    }
}
