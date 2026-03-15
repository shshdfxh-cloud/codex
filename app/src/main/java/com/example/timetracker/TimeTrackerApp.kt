package com.example.timetracker

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val CAT_WORK = "工作"
private const val CAT_FUN = "娱乐"
private const val CAT_STUDY = "学习"
private const val CAT_SPORT = "运动"

private const val TXT_TAP_TO_END = "再点一下结束"
private const val TXT_TAP_TO_START = "点一下开始"
private const val TXT_COMPLETE = "完成"
private const val TXT_SETTINGS = "设置"
private const val TXT_SETTINGS_DESC = "管理分类和颜色。"
private const val TXT_ADD_CATEGORY = "新增分类"
private const val TXT_EDIT_CATEGORY = "编辑分类"
private const val TXT_CATEGORY_NAME = "分类名"
private const val TXT_EDIT = "编辑"
private const val TXT_DELETE = "删除"
private const val TXT_CANCEL = "取消"
private const val TXT_SAVE = "保存"

@Composable
fun TimeTrackerApp() {
    val context = LocalContext.current
    val storage = remember(context) { TimeTrackerStorage(context) }
    val restoredState = remember(storage) { storage.load() }
    val defaultCategories = remember {
        listOf(
            Category(CAT_WORK, presetColors[0]),
            Category(CAT_FUN, presetColors[1]),
            Category(CAT_STUDY, presetColors[2]),
            Category(CAT_SPORT, presetColors[3])
        )
    }
    val initialCategories = remember(restoredState) {
        restoredState?.categories?.takeIf { it.isNotEmpty() } ?: defaultCategories
    }
    val initialEntries = remember(restoredState) { restoredState?.entries ?: emptyList() }

    val categories = remember {
        mutableStateListOf<Category>().apply { addAll(initialCategories) }
    }
    val entries = remember {
        mutableStateListOf<TimeEntry>().apply { addAll(initialEntries) }
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Timer) }
    var selectedCategory by rememberSaveable {
        mutableStateOf(
            restoredState?.selectedCategory?.takeIf { saved -> initialCategories.any { it.name == saved } }
                ?: initialCategories.first().name
        )
    }
    var selectedOverviewDateText by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var isRunning by rememberSaveable { mutableStateOf(restoredState?.isRunning ?: false) }
    var activeStart by remember { mutableStateOf(restoredState?.activeStart) }
    var elapsedMillis by remember {
        mutableLongStateOf(
            if (restoredState?.isRunning == true && restoredState.activeStart != null) {
                Duration.between(restoredState.activeStart, LocalDateTime.now()).toMillis().coerceAtLeast(0L)
            } else {
                0L
            }
        )
    }
    var pendingDraft by remember { mutableStateOf(restoredState?.pendingDraft) }
    var editingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedRingEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingCategory by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }

    LaunchedEffect(isRunning, activeStart) {
        while (isRunning) {
            activeStart?.let { start ->
                elapsedMillis = Duration.between(start, LocalDateTime.now()).toMillis().coerceAtLeast(0L)
            }
            delay(1000)
        }
    }

    LaunchedEffect(storage) {
        snapshotFlow {
            PersistedAppState(
                categories = categories.toList(),
                entries = entries.toList(),
                selectedCategory = selectedCategory,
                isRunning = isRunning,
                activeStart = activeStart,
                pendingDraft = pendingDraft
            )
        }.collectLatest { snapshot ->
            storage.save(snapshot)
        }
    }

    val colorForCategory: (String) -> Color = { name ->
        categories.firstOrNull { it.name == name }?.color ?: Color(0xFF999999)
    }

    Scaffold(
        containerColor = Color(0xFFF7F4EF),
        bottomBar = {
            BottomAppBar(containerColor = Color.White) {
                listOf(
                    Tab.Timer to Icons.Outlined.PlayCircle,
                    Tab.Overview to Icons.Outlined.BarChart,
                    Tab.Settings to Icons.Outlined.Settings
                ).forEach { (item, icon) ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = {
                            if (item == Tab.Overview) {
                                selectedOverviewDateText = LocalDate.now().toString()
                                selectedRingEntryId = null
                                expandedEntryId = null
                            }
                            tab = item
                        },
                        icon = { Icon(icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (tab) {
                Tab.Timer -> TimerScreen(
                    categories = categories,
                    isRunning = isRunning,
                    elapsedMillis = elapsedMillis,
                    pendingDraft = pendingDraft,
                    onCircleTap = {
                        when {
                            pendingDraft != null -> Unit
                            isRunning -> {
                                val start = activeStart ?: LocalDateTime.now()
                                val end = LocalDateTime.now()
                                isRunning = false
                                elapsedMillis = Duration.between(start, end).toMillis().coerceAtLeast(0L)
                                pendingDraft = DraftEntry(selectedCategory, "", start, end)
                                activeStart = null
                            }

                            else -> {
                                activeStart = LocalDateTime.now()
                                elapsedMillis = 0L
                                isRunning = true
                            }
                        }
                    },
                    onDraftChange = { draft ->
                        pendingDraft = draft
                        selectedCategory = draft.category
                    },
                    onSaveDraft = {
                        pendingDraft?.let { draft ->
                            val (normalizedStart, normalizedEnd) = normalizeDraftRange(draft.start, draft.end)
                            val splitEntries = splitRangeByDay(normalizedStart, normalizedEnd).map { (segmentStart, segmentEnd) ->
                                TimeEntry(
                                    category = draft.category,
                                    note = draft.note.trim(),
                                    start = segmentStart,
                                    end = segmentEnd
                                )
                            }
                            entries.addAll(splitEntries)
                            selectedCategory = draft.category
                            pendingDraft = null
                            elapsedMillis = 0L
                        }
                    },
                    onCancelDraft = {
                        pendingDraft = null
                        elapsedMillis = 0L
                    }
                )

                Tab.Overview -> OverviewScreen(
                    entries = entries.toList(),
                    selectedDate = LocalDate.parse(selectedOverviewDateText),
                    onSelectedDate = {
                        selectedOverviewDateText = it.toString()
                        selectedRingEntryId = null
                        expandedEntryId = null
                    },
                    colorForCategory = colorForCategory,
                    expandedEntryId = expandedEntryId,
                    onExpandedToggle = { id -> expandedEntryId = if (expandedEntryId == id) null else id },
                    selectedRingEntryId = selectedRingEntryId,
                    onSelectedRingEntry = { selectedRingEntryId = it },
                    onEdit = { editingEntryId = it },
                    onDelete = { id ->
                        entries.removeAll { it.id == id }
                        if (expandedEntryId == id) expandedEntryId = null
                        if (selectedRingEntryId == id) selectedRingEntryId = null
                        if (editingEntryId == id) editingEntryId = null
                    },
                    onDeleteEntries = { ids ->
                        entries.removeAll { it.id in ids }
                        if (expandedEntryId in ids) expandedEntryId = null
                        if (selectedRingEntryId in ids) selectedRingEntryId = null
                        if (editingEntryId in ids) editingEntryId = null
                    }
                )

                Tab.Settings -> SettingsScreen(
                    categories = categories,
                    onAdd = { addingCategory = true },
                    onEdit = { editingCategory = it },
                    onDelete = { name ->
                        if (categories.size > 1) {
                            val fallback = categories.firstOrNull { it.name != name }?.name ?: categories.first().name
                            categories.removeAll { it.name == name }
                            entries.indices.forEach { index ->
                                val entry = entries[index]
                                if (entry.category == name) {
                                    entries[index] = entry.copy(category = fallback)
                                }
                            }
                            if (selectedCategory == name) {
                                selectedCategory = fallback
                            }
                            pendingDraft?.let { draft ->
                                if (draft.category == name) {
                                    pendingDraft = draft.copy(category = fallback)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    entries.firstOrNull { it.id == editingEntryId }?.let { entry ->
        EditEntryDialog(
            entry = entry,
            categories = categories,
            onDismiss = { editingEntryId = null },
            onSave = { updated ->
                val index = entries.indexOfFirst { it.id == updated.id }
                if (index >= 0) {
                    entries[index] = updated
                }
                editingEntryId = null
            }
        )
    }

    if (addingCategory) {
        CategoryDialog(
            title = TXT_ADD_CATEGORY,
            initialName = "",
            initialColor = presetColors.first(),
            onDismiss = { addingCategory = false },
            onConfirm = { name, color ->
                if (categories.none { it.name == name }) {
                    categories.add(Category(name, color))
                }
                addingCategory = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryDialog(
            title = TXT_EDIT_CATEGORY,
            initialName = category.name,
            initialColor = category.color,
            onDismiss = { editingCategory = null },
            onConfirm = { name, color ->
                val duplicateExists = categories.any { it.name == name && it.name != category.name }
                if (!duplicateExists) {
                    val index = categories.indexOfFirst { it.name == category.name }
                    if (index >= 0) {
                        categories[index] = Category(name, color)
                    }
                    entries.indices.forEach { idx ->
                        val entry = entries[idx]
                        if (entry.category == category.name) {
                            entries[idx] = entry.copy(category = name)
                        }
                    }
                    if (selectedCategory == category.name) {
                        selectedCategory = name
                    }
                    pendingDraft?.let { draft ->
                        if (draft.category == category.name) {
                            pendingDraft = draft.copy(category = name)
                        }
                    }
                }
                editingCategory = null
            }
        )
    }
}

@Composable
private fun TimerScreen(
    categories: List<Category>,
    isRunning: Boolean,
    elapsedMillis: Long,
    pendingDraft: DraftEntry?,
    onCircleTap: () -> Unit,
    onDraftChange: (DraftEntry) -> Unit,
    onSaveDraft: () -> Unit,
    onCancelDraft: () -> Unit
) {
    val shownMillis = pendingDraft?.let { Duration.between(it.start, it.end).toMillis() } ?: elapsedMillis

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(if (pendingDraft == null) 0.32f else 0.14f))

        Box(
            modifier = Modifier
                .size(312.dp)
                .clip(CircleShape)
                .border(2.dp, Color(0xFF333333), CircleShape)
                .clickable(enabled = pendingDraft == null, onClick = onCircleTap),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatDuration(shownMillis), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isRunning) TXT_TAP_TO_END else TXT_TAP_TO_START,
                    color = Color(0xFF777777),
                    fontSize = 16.sp
                )
            }
        }

        AnimatedVisibility(visible = pendingDraft != null) {
            pendingDraft?.let { draft ->
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    CompactDraftEditor(
                        draft = draft,
                        categories = categories,
                        onDraftChange = onDraftChange,
                        onSaveDraft = onSaveDraft,
                        onCancelDraft = onCancelDraft
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CompactDraftEditor(
    draft: DraftEntry,
    categories: List<Category>,
    onDraftChange: (DraftEntry) -> Unit,
    onSaveDraft: () -> Unit,
    onCancelDraft: () -> Unit
) {
    var startFields by remember(draft.start) { mutableStateOf(draft.start.toLocalTime().toDraftHmsFields()) }
    var endFields by remember(draft.end) { mutableStateOf(draft.end.toLocalTime().toDraftHmsFields()) }

    fun updateStart(updated: DraftHmsFields) {
        startFields = updated
        updated.toLocalTimeOrNull()?.let { localTime ->
            onDraftChange(draft.copy(start = draft.start.withClockTime(localTime)))
        }
    }

    fun updateEnd(updated: DraftHmsFields) {
        endFields = updated
        updated.toLocalTimeOrNull()?.let { localTime ->
            onDraftChange(draft.copy(end = draft.end.withClockTime(localTime)))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DraftHmsEditorRow(
                label = "开始",
                value = startFields,
                onValueChange = ::updateStart,
                modifier = Modifier.weight(1f)
            )
            DraftHmsEditorRow(
                label = "结束",
                value = endFields,
                onValueChange = ::updateEnd,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryDropdownField(
                selectedCategory = draft.category,
                categories = categories,
                modifier = Modifier.width(74.dp),
                onSelected = { onDraftChange(draft.copy(category = it)) }
            )
            NoteInputField(
                value = draft.note,
                onValueChange = { onDraftChange(draft.copy(note = it)) },
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancelDraft) {
                Text(TXT_CANCEL)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSaveDraft,
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(TXT_COMPLETE)
            }
        }
    }
}


private data class DraftHmsFields(
    val hour: String,
    val minute: String,
    val second: String
)

private fun LocalTime.toDraftHmsFields(): DraftHmsFields = DraftHmsFields(
    hour = hour.toString(),
    minute = minute.toString(),
    second = second.toString()
)

private fun DraftHmsFields.toLocalTimeOrNull(): LocalTime? {
    val h = hour.toIntOrNull() ?: return null
    val m = minute.toIntOrNull() ?: return null
    val s = second.toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59 || s !in 0..59) return null
    return LocalTime.of(h, m, s)
}

@Composable
private fun DraftHmsEditorRow(
    label: String,
    value: DraftHmsFields,
    onValueChange: (DraftHmsFields) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, fontSize = 11.sp, color = Color(0xFF757575), modifier = Modifier.padding(start = 2.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DraftSmallNumberField(
                value = value.hour,
                fieldLabel = "时",
                width = 46.dp,
                maxLength = 2,
                onValueChange = { onValueChange(value.copy(hour = it)) }
            )
            DraftSmallNumberField(
                value = value.minute,
                fieldLabel = "分",
                width = 46.dp,
                maxLength = 2,
                onValueChange = { onValueChange(value.copy(minute = it)) }
            )
            DraftSmallNumberField(
                value = value.second,
                fieldLabel = "秒",
                width = 46.dp,
                maxLength = 2,
                onValueChange = { onValueChange(value.copy(second = it)) }
            )
        }
    }
}

@Composable
private fun DraftSmallNumberField(
    value: String,
    fieldLabel: String,
    width: Dp,
    maxLength: Int,
    onValueChange: (String) -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onValueChange(input.filter(Char::isDigit).take(maxLength)) },
        modifier = Modifier
            .width(width)
            .onFocusChanged { state ->
                val wasFocused = isFocused
                isFocused = state.isFocused
                if (wasFocused && !state.isFocused && value.isBlank()) {
                    onValueChange("0".padStart(maxLength, '0'))
                }
            },
        singleLine = true,
        label = { Text(fieldLabel) },
        textStyle = TextStyle(fontSize = 13.sp)
    )
}

@Composable
private fun NoteInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isFocused) Color(0xFF262626) else Color(0xFFB8B0A6)),
        modifier = modifier
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 15.sp, color = Color(0xFF1E1E1E), lineHeight = 20.sp),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { innerTextField ->
                if (value.isBlank()) {
                    Text("具体事项", color = Color(0xFF9E9E9E), fontSize = 15.sp)
                }
                innerTextField()
            }
        )
    }
}



private fun normalizeDraftRange(start: LocalDateTime, end: LocalDateTime): Pair<LocalDateTime, LocalDateTime> {
    val normalizedEnd = if (end.isBefore(start)) end.plusDays(1) else end
    return start to normalizedEnd
}

private fun splitRangeByDay(start: LocalDateTime, end: LocalDateTime): List<Pair<LocalDateTime, LocalDateTime>> {
    if (!end.isAfter(start)) return emptyList()

    val segments = mutableListOf<Pair<LocalDateTime, LocalDateTime>>()
    var cursor = start

    while (cursor.toLocalDate().isBefore(end.toLocalDate())) {
        val midnight = cursor.toLocalDate().plusDays(1).atStartOfDay()
        segments.add(cursor to midnight)
        cursor = midnight
    }

    segments.add(cursor to end)
    return segments
}

private fun LocalDateTime.withClockTime(time: LocalTime): LocalDateTime =
    withHour(time.hour).withMinute(time.minute).withSecond(time.second).withNano(0)

@Composable
private fun CategoryDropdownField(
    selectedCategory: String,
    categories: List<Category>,
    modifier: Modifier = Modifier,
    onSelected: (String) -> Unit
) {
    var menuExpanded by remember(selectedCategory, categories.size) { mutableStateOf(false) }
    val currentCategory = categories.firstOrNull { it.name == selectedCategory } ?: categories.first()
    val contentColor = if (currentCategory.color.luminance() > 0.55f) Color(0xFF1E1E1E) else Color.White

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clickable { menuExpanded = true },
            shape = RoundedCornerShape(14.dp),
            color = currentCategory.color
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentCategory.name,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
            offset = DpOffset(0.dp, 2.dp)
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(category.color.copy(alpha = 0.22f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category.name,
                                color = Color(0xFF1E1E1E),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    },
                    onClick = {
                        menuExpanded = false
                        onSelected(category.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    categories: List<Category>,
    onAdd: () -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(TXT_SETTINGS, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(TXT_SETTINGS_DESC, color = Color(0xFF777777))
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAdd,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(TXT_ADD_CATEGORY)
        }
        Spacer(modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(categories, key = { it.name }) { category ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(category.color)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(category.name, modifier = Modifier.weight(1f), fontSize = 19.sp)
                        TextButton(onClick = { onEdit(category) }) {
                            Text(TXT_EDIT)
                        }
                        TextButton(
                            onClick = { onDelete(category.name) },
                            enabled = categories.size > 1
                        ) {
                            Text(TXT_DELETE)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryDialog(
    title: String,
    initialName: String,
    initialColor: Color,
    onDismiss: () -> Unit,
    onConfirm: (String, Color) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var selectedColor by remember(initialColor) { mutableStateOf(initialColor) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(TXT_CATEGORY_NAME) }
                )
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    presetColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (selectedColor == color) 3.dp else 0.dp,
                                    color = Color.Black,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = color }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedColor) }) {
                Text(TXT_SAVE)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(TXT_CANCEL)
            }
        }
    )
}
