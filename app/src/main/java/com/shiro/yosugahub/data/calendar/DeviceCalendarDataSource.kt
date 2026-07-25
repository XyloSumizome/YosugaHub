package com.shiro.yosugahub.data.calendar

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.shiro.yosugahub.data.local.db.entity.CalendarEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** 端末カレンダー読み取りの結果。 */
sealed interface CalendarSyncResult {
    data class Success(val eventCount: Int) : CalendarSyncResult
    object PermissionDenied : CalendarSyncResult
    data class Failed(val message: String) : CalendarSyncResult
}

/**
 * 端末に同期済みのカレンダー(Googleカレンダーを含む)を CalendarContract から読む。
 * READ_CALENDAR のみで動作し、OAuth や Google Cloud の設定を必要としない。
 *
 * `Instances` テーブルを使うことで、繰り返し予定を期間内の個別予定として展開できる。
 */
class DeviceCalendarDataSource(
    private val context: Context,
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * [today] を基準に過去 [pastDays] 日〜未来 [futureDays] 日の予定を読む。
     * 取得できた予定を Room 用エンティティへ変換して返す。
     */
    suspend fun loadEvents(
        today: LocalDate,
        // 近況報告(Morning Brief 等)で「前後2週間」を見せるため ±14 日
        // (2026-07-25 に ±7 から拡張)。
        pastDays: Long = 14,
        futureDays: Long = 14,
    ): Result<List<CalendarEventEntity>> = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            return@withContext Result.failure(SecurityException("READ_CALENDAR 権限がありません"))
        }

        val startMillis = today.minusDays(pastDays)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = today.plusDays(futureDays + 1)
            .atStartOfDay(zoneId).toInstant().toEpochMilli()

        runCatching {
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .let { builder ->
                    ContentUris.appendId(builder, startMillis)
                    ContentUris.appendId(builder, endMillis)
                    builder.build()
                }

            context.contentResolver.query(
                uri,
                PROJECTION,
                null,
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cursor -> cursor.toEntities(today) } ?: emptyList()
        }
    }

    private fun Cursor.toEntities(today: LocalDate): List<CalendarEventEntity> {
        val titleIndex = getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
        val beginIndex = getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
        val endIndex = getColumnIndexOrThrow(CalendarContract.Instances.END)
        val allDayIndex = getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
        val calendarNameIndex = getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)
        val descriptionIndex = getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)

        val events = mutableListOf<CalendarEventEntity>()
        while (moveToNext()) {
            val beginMillis = getLong(beginIndex)
            val endMillis = getLong(endIndex)
            val allDay = getInt(allDayIndex) == 1
            val start = beginMillis.toLocalDateTime()
            val end = endMillis.toLocalDateTime()
            val startDate = start.toLocalDate()

            events += CalendarEventEntity(
                bucket = EventFormatting.bucketOf(startDate, today),
                // タイトル未設定の予定は空欄のままにせず、存在が分かる表示にする。
                title = getString(titleIndex).orEmpty().ifBlank { "(タイトルなし)" },
                start = EventFormatting.formatEventTime(start, today, allDay),
                end = if (allDay) "" else EventFormatting.formatEventTime(end, today, false),
                calendarName = getString(calendarNameIndex).orEmpty(),
                description = getString(descriptionIndex).orEmpty(),
            )
        }
        return events
    }

    private fun Long.toLocalDateTime() =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDateTime()

    private companion object {
        val PROJECTION = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.DESCRIPTION,
        )
    }
}
