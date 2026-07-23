package com.shiro.yosugahub.data.file

/**
 * 貼り付けられた回答JSONの正規化(純粋ロジック)。
 * ChatGPTの回答はコードブロック(```json 〜 ```)ごとコピーされがちなので、
 * 囲いを外して中身だけにする。JSONそのものには手を加えない。
 */
object PastedJson {

    fun normalize(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            // 先頭行(``` または ```json 等の言語タグ付き)を落とす
            text = text.substringAfter('\n', missingDelimiterValue = "").trim()
            // 末尾の ``` を落とす(閉じ忘れのコピーでも中身は残す)
            if (text.endsWith("```")) {
                text = text.removeSuffix("```").trim()
            }
        }
        return text
    }
}
