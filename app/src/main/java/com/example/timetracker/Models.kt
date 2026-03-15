package com.example.timetracker

import androidx.compose.ui.graphics.Color
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

data class Category(
    val name: String,
    val color: Color
)

data class TimeEntry(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val note: String,
    val start: LocalDateTime,
    val end: LocalDateTime
) {
    val duration: Duration
        get() = Duration.between(start, end)
}

data class DraftEntry(
    val category: String,
    val note: String,
    val start: LocalDateTime,
    val end: LocalDateTime
)

enum class Tab(val label: String) {
    Timer("\u8BA1\u65F6"),
    Overview("\u6982\u89C8"),
    Settings("\u8BBE\u7F6E")
}
