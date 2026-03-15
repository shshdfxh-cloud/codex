package com.example.timetracker

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDateTime

data class PersistedAppState(
    val categories: List<Category>,
    val entries: List<TimeEntry>,
    val selectedCategory: String,
    val isRunning: Boolean,
    val activeStart: LocalDateTime?,
    val pendingDraft: DraftEntry?
)

class TimeTrackerStorage(context: Context) {
    private val prefs = context.getSharedPreferences("time_tracker_store", Context.MODE_PRIVATE)

    fun load(): PersistedAppState? {
        val raw = prefs.getString(KEY_STATE, null) ?: return null
        return runCatching {
            val root = JSONObject(raw)

            val categories = root.getJSONArray("categories").toCategoryList()
            val entries = root.getJSONArray("entries").toEntryList()
            val selectedCategory = root.optString("selectedCategory", categories.firstOrNull()?.name.orEmpty())
            val isRunning = root.optBoolean("isRunning", false)
            val activeStart = if (root.has("activeStart") && !root.isNull("activeStart")) {
                root.getString("activeStart").takeIf { it.isNotBlank() }?.let(LocalDateTime::parse)
            } else {
                null
            }
            val pendingDraft = root.optJSONObject("pendingDraft")?.toDraftEntry()

            PersistedAppState(
                categories = categories,
                entries = entries,
                selectedCategory = selectedCategory,
                isRunning = isRunning,
                activeStart = activeStart,
                pendingDraft = pendingDraft
            )
        }.getOrNull()
    }

    fun save(state: PersistedAppState) {
        val root = JSONObject().apply {
            put("categories", JSONArray().apply {
                state.categories.forEach { category ->
                    put(
                        JSONObject().apply {
                            put("name", category.name)
                            put("color", category.color.toArgb())
                        }
                    )
                }
            })
            put("entries", JSONArray().apply {
                state.entries.forEach { entry ->
                    put(
                        JSONObject().apply {
                            put("id", entry.id)
                            put("category", entry.category)
                            put("note", entry.note)
                            put("start", entry.start.toString())
                            put("end", entry.end.toString())
                        }
                    )
                }
            })
            put("selectedCategory", state.selectedCategory)
            put("isRunning", state.isRunning)
            put("activeStart", state.activeStart?.toString())
            put(
                "pendingDraft",
                state.pendingDraft?.let { draft ->
                    JSONObject().apply {
                        put("category", draft.category)
                        put("note", draft.note)
                        put("start", draft.start.toString())
                        put("end", draft.end.toString())
                    }
                }
            )
        }

        prefs.edit().putString(KEY_STATE, root.toString()).apply()
    }

    private fun JSONArray.toCategoryList(): List<Category> {
        return buildList {
            for (index in 0 until length()) {
                val item = getJSONObject(index)
                add(
                    Category(
                        name = item.getString("name"),
                        color = Color(item.getInt("color"))
                    )
                )
            }
        }
    }

    private fun JSONArray.toEntryList(): List<TimeEntry> {
        return buildList {
            for (index in 0 until length()) {
                val item = getJSONObject(index)
                add(
                    TimeEntry(
                        id = item.getString("id"),
                        category = item.getString("category"),
                        note = item.optString("note", ""),
                        start = LocalDateTime.parse(item.getString("start")),
                        end = LocalDateTime.parse(item.getString("end"))
                    )
                )
            }
        }
    }

    private fun JSONObject.toDraftEntry(): DraftEntry {
        return DraftEntry(
            category = getString("category"),
            note = optString("note", ""),
            start = LocalDateTime.parse(getString("start")),
            end = LocalDateTime.parse(getString("end"))
        )
    }

    companion object {
        private const val KEY_STATE = "app_state"
    }
}
