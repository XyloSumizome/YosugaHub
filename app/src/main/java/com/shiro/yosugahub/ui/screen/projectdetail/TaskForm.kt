package com.shiro.yosugahub.ui.screen.projectdetail

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * 締切入力の検証(純粋関数)。空欄は「締切なし」で有効、
 * それ以外は "yyyy-MM-dd" として実在する日付のみ有効。
 */
fun isValidDueDateInput(input: String): Boolean {
    if (input.isBlank()) return true
    return try {
        LocalDate.parse(input.trim())
        true
    } catch (e: DateTimeParseException) {
        false
    }
}

/** 入力文字列を保存用の dueDate へ変換(空欄 → null)。 */
fun dueDateForSave(input: String): String? = input.trim().ifEmpty { null }
