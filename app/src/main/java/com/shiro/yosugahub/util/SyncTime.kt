package com.shiro.yosugahub.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val SYNC_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/** 最終同期時刻の表示用フォーマット(例: 2026-07-22 20:05)。 */
fun formatSyncTime(dateTime: LocalDateTime): String = dateTime.format(SYNC_TIME_FORMATTER)
