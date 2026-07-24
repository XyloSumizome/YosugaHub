package com.shiro.yosugahub

import com.shiro.yosugahub.ui.component.inProgressLine
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectHealthTest {

    @Test
    fun in_progress_line_shows_the_value() {
        assertEquals("作業中: 第2章の執筆", inProgressLine("第2章の執筆"))
    }

    @Test
    fun a_blank_in_progress_falls_back_to_a_dash() {
        // A運用ではローカルの作業中が空のことが多い。「作業中: 」(後ろが空)を出さない。
        assertEquals("作業中: -", inProgressLine(""))
        assertEquals("作業中: -", inProgressLine("   "))
    }
}
