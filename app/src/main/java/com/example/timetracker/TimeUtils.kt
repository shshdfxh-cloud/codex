package com.example.timetracker

import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

val presetColors = listOf(
    Color(0xFF5B8CFF),
    Color(0xFFFF8A5B),
    Color(0xFF68B984),
    Color(0xFF8E6CFF),
    Color(0xFFF2C94C),
    Color(0xFF28BDB8),
    Color(0xFFE57373),
    Color(0xFF7CC576),
    Color(0xFFFFB74D),
    Color(0xFF4DB6AC),
    Color(0xFF9575CD),
    Color(0xFF7986CB),
    Color(0xFFA1887F),
    Color(0xFF90A4AE),
    Color(0xFFD4A373),
    Color(0xFFEC407A),
    Color(0xFF26A69A)
)

fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return "${hours}\u65F6${minutes}\u5206${seconds}\u79D2"
}

fun Duration.toCompactDurationLabel(alwaysShowSeconds: Boolean = true): String {
    val totalSeconds = seconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val remainingSeconds = totalSeconds % 60

    return buildString {
        append(hours)
        append("h")
        append(minutes)
        append("min")
        if (alwaysShowSeconds || remainingSeconds > 0 || (hours == 0L && minutes == 0L)) {
            append(remainingSeconds)
            append("s")
        }
    }
}

fun parseTime(text: String): LocalTime? {
    return runCatching { LocalTime.parse(text, timeFormatter) }.getOrNull()
}

fun formatTimeRange(start: LocalTime, end: LocalTime): String {
    return "${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
}

fun formatTimeRange(start: LocalDateTime, end: LocalDateTime): String {
    val endText = if (end.toLocalTime() == LocalTime.MIDNIGHT && end.toLocalDate().isAfter(start.toLocalDate())) {
        "24:00:00"
    } else {
        end.toLocalTime().format(timeFormatter)
    }
    return "${start.toLocalTime().format(timeFormatter)} - $endText"
}
