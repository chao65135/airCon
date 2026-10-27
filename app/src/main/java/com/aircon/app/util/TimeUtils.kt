package com.aircon.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 时间解析工具。
 *
 * 处理两类接口返回的时间字符串：
 * - Open-Meteo：本地时间（无时区后缀），需配合 `utc_offset_seconds` 换算
 * - 和风天气：UTC（`Z` 后缀）或本地时间（`localTime=true`）
 */
object TimeUtils {

    private const val PATTERN_LOCAL_DATE_TIME = "yyyy-MM-dd'T'HH:mm"
    private const val PATTERN_LOCAL_DATE = "yyyy-MM-dd"

    /**
     * 解析接口返回的时间字符串为毫秒时间戳。
     *
     * @param time 时间字符串，如 `2024-05-31T03:00Z` 或 `2024-05-31T03:00`
     * @param defaultUtcOffsetSeconds 当字符串不含时区信息时使用的 UTC 偏移秒数
     */
    fun toEpochMillis(time: String, defaultUtcOffsetSeconds: Int = 0): Long {
        if (time.isBlank()) return 0L
        val trimmed = time.trim()
        val hasZone = trimmed.endsWith("Z") || trimmed.endsWith("+") || trimmed.lastIndexOf('-') > 10
        val base = if (hasZone) trimmed.removeSuffix("Z") else trimmed
        val pattern = if (base.length <= 10) PATTERN_LOCAL_DATE else PATTERN_LOCAL_DATE_TIME
        val parsed = parse(base, pattern) ?: return 0L
        if (hasZone) return parsed
        // 无时区信息：按接口给出的 UTC 偏移换算
        return parsed - defaultUtcOffsetSeconds * 1000L
    }

    private fun parse(time: String, pattern: String): Long? = try {
        val format = SimpleDateFormat(pattern, Locale.US)
        format.isLenient = false
        format.timeZone = TimeZone.getTimeZone("UTC")
        format.parse(time)?.time
    } catch (e: Exception) {
        null
    }

    /**
     * 格式化为本地时区的 `HH:mm`。
     */
    fun formatHour(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        if (epochMillis <= 0L) return "--:--"
        val format = SimpleDateFormat("HH:mm", Locale.getDefault())
        format.timeZone = timeZone
        return format.format(Date(epochMillis))
    }

    /**
     * 格式化为本地时区的 `MM-dd HH:mm`。
     */
    fun formatDateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        if (epochMillis <= 0L) return "--"
        val format = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        format.timeZone = timeZone
        return format.format(Date(epochMillis))
    }

    /**
     * 格式化为 `yyyy-MM-dd`。
     */
    fun formatDate(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        if (epochMillis <= 0L) return "--"
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        format.timeZone = timeZone
        return format.format(Date(epochMillis))
    }

    /**
     * 相对时间描述，如「刚刚」「12 分钟前」「3 小时前」。
     */
    fun formatRelative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        if (epochMillis <= 0L) return "未知"
        val diff = now - epochMillis
        return when {
            diff < 0 -> "刚刚"
            diff < 60_000L -> "刚刚"
            diff < 3_600_000L -> "${diff / 60_000L} 分钟前"
            diff < 86_400_000L -> "${diff / 3_600_000L} 小时前"
            else -> "${diff / 86_400_000L} 天前"
        }
    }

    /**
     * 判断时间戳是否为今天的零点（按指定时区）。
     */
    fun isStartOfDay(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        if (epochMillis <= 0L) return false
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = epochMillis
        val year = calendar.get(Calendar.YEAR)
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        val now = Calendar.getInstance(timeZone)
        return year == now.get(Calendar.YEAR) && day == now.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 判断时间戳是否为明天。
     */
    fun isTomorrow(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): Boolean {
        if (epochMillis <= 0L) return false
        val calendar = Calendar.getInstance(timeZone)
        calendar.timeInMillis = epochMillis
        val now = Calendar.getInstance(timeZone)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return calendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    /**
     * 星期几的简短名称，例如「周一」。
     */
    fun formatWeekday(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
        if (epochMillis <= 0L) return ""
        val format = SimpleDateFormat("EEE", Locale.CHINA)
        format.timeZone = timeZone
        return format.format(Date(epochMillis))
    }
}
