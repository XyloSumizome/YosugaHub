package com.shiro.yosugahub

import com.shiro.yosugahub.data.file.PastedJson
import org.junit.Assert.assertEquals
import org.junit.Test

/** 貼り付けJSONの正規化(コードブロックの囲い外し)。 */
class PastedJsonTest {

    @Test
    fun plain_json_passes_through() {
        assertEquals("""{"schemaVersion": 2}""", PastedJson.normalize("""{"schemaVersion": 2}"""))
    }

    /** ChatGPTの回答をコードブロックごとコピーした形。 */
    @Test
    fun strips_fenced_code_block_with_language_tag() {
        val pasted = """
            ```json
            {"schemaVersion": 2}
            ```
        """.trimIndent()
        assertEquals("""{"schemaVersion": 2}""", PastedJson.normalize(pasted))
    }

    @Test
    fun strips_fence_without_language_tag() {
        val pasted = "```\n{\"a\": 1}\n```"
        assertEquals("""{"a": 1}""", PastedJson.normalize(pasted))
    }

    /** 閉じ側の ``` を選択し損ねたコピーでも中身は残す。 */
    @Test
    fun tolerates_missing_closing_fence() {
        val pasted = "```json\n{\"a\": 1}"
        assertEquals("""{"a": 1}""", PastedJson.normalize(pasted))
    }

    @Test
    fun trims_surrounding_whitespace() {
        assertEquals("""{"a": 1}""", PastedJson.normalize("\n  {\"a\": 1}  \n"))
    }

    /** JSON文字列の中身(バッククォートを含む文章など)には手を加えない。 */
    @Test
    fun does_not_touch_backticks_inside_json() {
        val json = """{"summary": "`code` を含む要約"}"""
        assertEquals(json, PastedJson.normalize(json))
    }

    @Test
    fun blank_input_becomes_empty() {
        assertEquals("", PastedJson.normalize("   \n  "))
        assertEquals("", PastedJson.normalize("```json\n```"))
    }

    // ── 共有シートから来る形(回答まるごと)──

    /** 「巡回」は JSON の後ろに実行報告を書くので、この形が必ず来る。 */
    @Test
    fun extracts_json_from_message_with_report_after_it() {
        val shared = """
            巡回しました。

            ```json
            {"schemaVersion": 2}
            ```

            - 読んだファイル: documents / knowledge / projects
            - 分類: 3件 / 未処理: 0件
            - 要判断: なし
        """.trimIndent()
        assertEquals("""{"schemaVersion": 2}""", PastedJson.normalize(shared))
    }

    @Test
    fun extracts_json_when_prose_comes_before_the_fence() {
        val shared = "以下が回答JSONです。\n```json\n{\"a\": 1}\n```"
        assertEquals("""{"a": 1}""", PastedJson.normalize(shared))
    }

    /** 囲いを付け忘れた回答でも、地の文から JSON を拾う。 */
    @Test
    fun extracts_bare_json_surrounded_by_prose() {
        val shared = "できました:\n{\"a\": 1}\nご確認ください。"
        assertEquals("""{"a": 1}""", PastedJson.normalize(shared))
    }

    /** 説明用のブロックが先にあっても、JSON のブロックを選ぶ。 */
    @Test
    fun prefers_the_fenced_block_that_holds_json() {
        val shared = """
            使い方:

            ```
            > IMPORT RESPONSE
            ```

            ```json
            {"schemaVersion": 2}
            ```
        """.trimIndent()
        assertEquals("""{"schemaVersion": 2}""", PastedJson.normalize(shared))
    }

    /** 入れ子の波括弧を数え違えない。 */
    @Test
    fun handles_nested_objects() {
        val json = """{"proposals": {"items": [{"kind": "memo"}]}}"""
        assertEquals(json, PastedJson.normalize("どうぞ:\n$json\nおわり"))
    }

    /** 文字列リテラルの中の `}` で閉じたと誤認しない。 */
    @Test
    fun ignores_braces_inside_json_strings() {
        val json = """{"body": "閉じ括弧 } を含む本文"}"""
        assertEquals(json, PastedJson.normalize("結果:\n$json"))
    }

    /** エスケープされた引用符で文字列の外に出たと誤認しない。 */
    @Test
    fun ignores_escaped_quotes_inside_json_strings() {
        val json = """{"body": "引用 \" のあとに } がある"}"""
        assertEquals(json, PastedJson.normalize("結果:\n$json"))
    }

    /** 本文に ``` を持つ JSON(指示書の body など)を囲いと誤認しない。 */
    @Test
    fun does_not_mistake_fences_inside_json_for_a_code_block() {
        val json = """{"directives": [{"body": "```kotlin\nval a = 1\n```"}]}"""
        assertEquals(json, PastedJson.normalize(json))
    }

    /** 閉じていない JSON は原文のまま返し、解析側でエラーにさせる。 */
    @Test
    fun unclosed_json_is_left_for_the_parser_to_reject() {
        val broken = "結果:\n{\"a\": 1"
        assertEquals(broken, PastedJson.normalize(broken))
    }

    @Test
    fun text_without_any_json_is_left_as_is() {
        assertEquals("分類対象はありませんでした。", PastedJson.normalize("分類対象はありませんでした。"))
    }
}
