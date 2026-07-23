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
}
