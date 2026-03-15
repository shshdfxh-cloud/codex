package com.example.timetracker

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

private const val TXT_TIME_BLOCK = "时间块"
private const val TXT_ENTRY_NOTE = "具体事项"
private const val TXT_EMPTY_NOTE = "未填写"
private const val TXT_EDIT_ENTRY = "编辑时间块"
private const val TXT_START = "开始"
private const val TXT_END = "结束"
private const val TXT_DURATION = "时长"
private const val TXT_DATE = "日期"
private const val TXT_EMPTY_DAY = "这一天还没有记录。"
private const val TXT_NO_RECORD = "暂无记录"

private val overviewDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")
private val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月")

@Composable
fun OverviewScreen(
    entries: List<TimeEntry>,
    selectedDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit,
    colorForCategory: (String) -> Color,
    expandedEntryId: String?,
    onExpandedToggle: (String) -> Unit,
    selectedRingEntryId: String?,
    onSelectedRingEntry: (String?) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteEntries: (Set<String>) -> Unit
) {
    val dayEntries = remember(entries, selectedDate) {
        entries
            .filter { it.start.toLocalDate() == selectedDate }
            .sortedBy { it.start }
    }
    val activeSelectedRingId = selectedRingEntryId?.takeIf { id -> dayEntries.any { it.id == id } }
    var selectedEntryIds by rememberSaveable(selectedDate.toString()) { mutableStateOf(listOf<String>()) }
    var pendingDeleteIds by remember(selectedDate) { mutableStateOf<List<String>?>(null) }
    val selectionMode = selectedEntryIds.isNotEmpty()

    LaunchedEffect(dayEntries, selectedEntryIds) {
        val validIds = selectedEntryIds.filter { selectedId -> dayEntries.any { it.id == selectedId } }
        if (validIds.size != selectedEntryIds.size) {
            selectedEntryIds = validIds
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 4.dp)
    ) {
        DayRingCard(
            entries = dayEntries.sortedBy { it.start },
            selectedDate = selectedDate,
            onSelectedDate = onSelectedDate,
            colorForCategory = colorForCategory,
            selectedRingEntryId = activeSelectedRingId,
            onSelectedRingEntry = onSelectedRingEntry
        )
        Spacer(modifier = Modifier.height(6.dp))
        if (selectionMode) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${selectedEntryIds.size}项已选", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { pendingDeleteIds = selectedEntryIds }) {
                    Text("删除", color = Color(0xFFC44E3B))
                }
                TextButton(onClick = { selectedEntryIds = emptyList() }) {
                    Text("取消")
                }
            }
        } else {
            Text(TXT_TIME_BLOCK, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (dayEntries.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = TXT_EMPTY_DAY,
                    modifier = Modifier.padding(18.dp),
                    color = Color(0xFF7A7A7A)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(dayEntries, key = { it.id }) { entry ->
                    SwipeableEntryCard(
                        entry = entry,
                        color = colorForCategory(entry.category),
                        expanded = !selectionMode && expandedEntryId == entry.id,
                        selected = !selectionMode && activeSelectedRingId == entry.id,
                        selectionMode = selectionMode,
                        multiSelected = entry.id in selectedEntryIds,
                        onCardClick = {
                            if (selectionMode) {
                                selectedEntryIds = if (entry.id in selectedEntryIds) {
                                    selectedEntryIds - entry.id
                                } else {
                                    selectedEntryIds + entry.id
                                }
                            } else {
                                onSelectedRingEntry(entry.id)
                                onExpandedToggle(entry.id)
                            }
                        },
                        onCardLongPress = {
                            selectedEntryIds = if (entry.id in selectedEntryIds) {
                                selectedEntryIds - entry.id
                            } else {
                                selectedEntryIds + entry.id
                            }
                        },
                        onEdit = { onEdit(entry.id) },
                        onDelete = { onDelete(entry.id) }
                    )
                }
            }
        }
    }

    pendingDeleteIds?.let { ids ->
        AlertDialog(
            onDismissRequest = { pendingDeleteIds = null },
            title = { Text("删除选中的时间块") },
            text = { Text("将删除 ${ids.size} 条记录。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntries(ids.toSet())
                        selectedEntryIds = emptyList()
                        pendingDeleteIds = null
                    }
                ) {
                    Text("删除", color = Color(0xFFC44E3B))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteIds = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun DayRingCard(
    entries: List<TimeEntry>,
    selectedDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit,
    colorForCategory: (String) -> Color,
    selectedRingEntryId: String?,
    onSelectedRingEntry: (String?) -> Unit
) {
    val summaries = remember(entries) {
        entries
            .groupBy { it.category }
            .map { (category, categoryEntries) ->
                CategorySummary(
                    category = category,
                    duration = categoryEntries.fold(Duration.ZERO) { total, entry -> total.plus(entry.duration) }
                )
            }
            .sortedByDescending { it.duration }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            DateSelector(
                selectedDate = selectedDate,
                onSelectedDate = onSelectedDate
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(164.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(modifier = Modifier.padding(top = 2.dp)) {
                    RingChart(
                        entries = entries,
                        colorForCategory = colorForCategory,
                        selectedRingEntryId = selectedRingEntryId,
                        onSelectedRingEntry = onSelectedRingEntry
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (summaries.isEmpty()) {
                Text(
                    text = TXT_NO_RECORD,
                    color = Color(0xFF8A8A8A),
                    fontSize = 13.sp
                )
            } else {
                CategorySummaryLegend(
                    summaries = summaries,
                    colorForCategory = colorForCategory
                )
            }
        }
    }
}

@Composable
private fun RingChart(
    entries: List<TimeEntry>,
    colorForCategory: (String) -> Color,
    selectedRingEntryId: String?,
    onSelectedRingEntry: (String?) -> Unit
) {
    Canvas(
        modifier = Modifier
            .size(154.dp)
            .pointerInput(entries, selectedRingEntryId) {
                detectTapGestures { tapOffset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val minSize = minOf(size.width, size.height).toFloat()
                    val outerRadius = minSize * 0.30f
                    val innerRadius = minSize * 0.19f
                    val distance = hypot(tapOffset.x - center.x, tapOffset.y - center.y)
                    if (distance in innerRadius..outerRadius) {
                        val angle = ((Math.toDegrees(
                            atan2(
                                tapOffset.y - center.y,
                                tapOffset.x - center.x
                            ).toDouble()
                        ).toFloat() + 360f) % 360f)
                        val normalized = ((angle - 90f) + 360f) % 360f
                        val tappedSecond = (normalized / 360f) * 86400f
                        val selectedEntry = entries.firstOrNull { entry ->
                            val start = entry.start.toLocalTime().toSecondOfDay().toFloat()
                            val end = entry.endSecondOfDayForChart()
                            tappedSecond in start..end
                        }
                        onSelectedRingEntry(selectedEntry?.id)
                    } else {
                        onSelectedRingEntry(null)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.30f
        val innerRadius = size.minDimension * 0.19f
        val labelRadius = size.minDimension * 0.40f
        val outerRect = Rect(
            left = center.x - outerRadius,
            top = center.y - outerRadius,
            right = center.x + outerRadius,
            bottom = center.y + outerRadius
        )
        val innerRect = Rect(
            left = center.x - innerRadius,
            top = center.y - innerRadius,
            right = center.x + innerRadius,
            bottom = center.y + innerRadius
        )
        val labelPaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 12.sp.toPx()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        val infoPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 13.sp.toPx()
            isAntiAlias = true
        }
        val outlineWidth = 1.2.dp.toPx()

        drawCircle(Color(0xFFD9D9D9), radius = outerRadius, center = center)
        drawCircle(Color.White, radius = innerRadius, center = center)
        drawCircle(Color(0xFF1F1F1F), radius = outerRadius, center = center, style = Stroke(width = outlineWidth))
        drawCircle(Color(0xFF1F1F1F), radius = innerRadius, center = center, style = Stroke(width = outlineWidth))

        entries.forEach { entry ->
            val startAngle = secondsToAngle(entry.start.toLocalTime().toSecondOfDay().toFloat())
            val sweepAngle = secondsToSweep(
                entry.start.toLocalTime().toSecondOfDay().toFloat(),
                entry.endSecondOfDayForChart()
            )
            if (sweepAngle <= 0f) return@forEach

            val path = ringSegmentPath(
                outerRect = outerRect,
                innerRect = innerRect,
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
            drawPath(path = path, color = colorForCategory(entry.category))
            drawPath(path = path, color = Color(0xFF1F1F1F), style = Stroke(width = outlineWidth))

            if (selectedRingEntryId == entry.id) {
                val middleAngle = startAngle + sweepAngle / 2f
                val theta = angleToRadians(middleAngle)
                val lineStart = Offset(
                    x = center.x + outerRadius * cos(theta).toFloat(),
                    y = center.y + outerRadius * sin(theta).toFloat()
                )
                val lineEnd = Offset(
                    x = center.x + (outerRadius + 16.dp.toPx()) * cos(theta).toFloat(),
                    y = center.y + (outerRadius + 16.dp.toPx()) * sin(theta).toFloat()
                )
                drawLine(
                    color = Color(0xFF1F1F1F),
                    start = lineStart,
                    end = lineEnd,
                    strokeWidth = 1.6.dp.toPx()
                )
                val isRightSide = cos(theta) >= 0
                infoPaint.textAlign = if (isRightSide) Paint.Align.LEFT else Paint.Align.RIGHT
                val textOffset = if (isRightSide) 8.dp.toPx() else -8.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(
                    "${entry.category}: ${entry.duration.toCompactDurationLabel(alwaysShowSeconds = false)}",
                    lineEnd.x + textOffset,
                    lineEnd.y + 3.dp.toPx(),
                    infoPaint
                )
            }
        }

        for (hour in 0..23) {
            val theta = angleToRadians(secondsToAngle(hour * 3600f))
            val x = center.x + labelRadius * cos(theta).toFloat()
            val y = center.y + labelRadius * sin(theta).toFloat() + 4.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(hour.toString(), x, y, labelPaint)
        }
    }
}

@Composable
private fun CategorySummaryLegend(
    summaries: List<CategorySummary>,
    colorForCategory: (String) -> Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        summaries.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { summary ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(colorForCategory(summary.category))
                        )
                        Text(
                            text = "${summary.category}: ${summary.duration.toCompactDurationLabel(alwaysShowSeconds = false)}",
                            fontSize = 12.sp,
                            color = Color(0xFF252525)
                        )
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DateSelector(
    selectedDate: LocalDate,
    onSelectedDate: (LocalDate) -> Unit
) {
    var showCalendar by remember(selectedDate) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF6F1EA))
            .clickable { showCalendar = true }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = selectedDate.format(overviewDateFormatter),
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF282828)
        )
    }

    if (showCalendar) {
        MonthPickerDialog(
            selectedDate = selectedDate,
            onDismiss = { showCalendar = false },
            onSelectDate = {
                showCalendar = false
                onSelectedDate(it)
            }
        )
    }
}

@Composable
private fun MonthPickerDialog(
    selectedDate: LocalDate,
    onDismiss: () -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    var currentMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    val monthDates = remember(currentMonth) { buildMonthCells(currentMonth) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F4EF)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) { Text("<") }
                    Text(text = currentMonth.atDay(1).format(monthFormatter), fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) { Text(">") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { day ->
                        Text(day, fontSize = 12.sp, color = Color(0xFF7A7A7A), modifier = Modifier.width(36.dp))
                    }
                }
                monthDates.chunked(7).forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            DayCell(date = date, selectedDate = selectedDate, onSelectDate = onSelectDate)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val isSelected = date == selectedDate
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) Color(0xFF2B2B2B) else Color.Transparent)
            .clickable(enabled = date != null) { date?.let(onSelectDate) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date?.dayOfMonth?.toString().orEmpty(),
            color = when {
                date == null -> Color.Transparent
                isSelected -> Color.White
                else -> Color(0xFF262626)
            },
            fontSize = 13.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwipeableEntryCard(
    entry: TimeEntry,
    color: Color,
    expanded: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    multiSelected: Boolean,
    onCardClick: () -> Unit,
    onCardLongPress: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val actionSegmentWidth = 53.dp
    val actionWidth = 106.dp
    val density = LocalDensity.current
    val maxOffset = with(density) { actionWidth.toPx() }
    val offsetX = remember(entry.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val minCardHeight = 64.dp
    val contentColor = if (color.luminance() > 0.55f) Color(0xFF1C1C1C) else Color.White
    val barColor = when {
        multiSelected -> color.copy(alpha = 0.98f)
        selected -> color.copy(alpha = 0.92f)
        else -> color.copy(alpha = 0.76f)
    }
    val revealedWidthDp = with(density) { (-offsetX.value).coerceIn(0f, maxOffset).toDp() }

    LaunchedEffect(selectionMode) {
        if (selectionMode && offsetX.value != 0f) {
            offsetX.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minCardHeight)
            .clip(RoundedCornerShape(0.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .width(revealedWidthDp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(0.dp))
            ) {
                Row(
                    modifier = Modifier
                        .width(actionWidth)
                        .fillMaxHeight()
                ) {
                    ActionSegment(
                        label = "编辑",
                        contentDescription = "编辑",
                        color = Color(0xFF5B8CFF),
                        modifier = Modifier.width(actionSegmentWidth),
                        onClick = onEdit
                    )
                    ActionSegment(
                        label = "删除",
                        contentDescription = "删除",
                        color = Color(0xFFE68B7C),
                        modifier = Modifier.width(actionSegmentWidth),
                        onClick = onDelete
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .then(
                    if (selectionMode) {
                        Modifier
                    } else {
                        Modifier.pointerInput(entry.id) {
                            detectHorizontalDragGestures(
                                onHorizontalDrag = { _, dragAmount ->
                                    scope.launch {
                                        offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxOffset, 0f))
                                    }
                                },
                                onDragEnd = {
                                    scope.launch {
                                        val target = if (offsetX.value < -maxOffset * 0.35f) -maxOffset else 0f
                                        offsetX.animateTo(target, spring(stiffness = Spring.StiffnessMedium))
                                    }
                                }
                            )
                        }
                    }
                )
                .combinedClickable(
                    onClick = onCardClick,
                    onLongClick = onCardLongPress
                )
                .background(barColor)
                .then(
                    if (multiSelected) {
                        Modifier.border(1.5.dp, Color(0xFF1C1C1C))
                    } else {
                        Modifier
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectionMode) {
                    SelectionIndicator(
                        selected = multiSelected,
                        color = contentColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                entry.category,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = formatTimeRange(entry.start, entry.end),
                                color = contentColor.copy(alpha = 0.9f),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = entry.duration.toCompactDurationLabel(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = contentColor
                        )
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                entry.note.ifBlank { TXT_EMPTY_NOTE },
                                fontSize = 12.sp,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionSegment(
    label: String,
    contentDescription: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun SelectionIndicator(
    selected: Boolean,
    color: Color
) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .border(1.2.dp, color.copy(alpha = 0.95f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.95f))
            )
        }
    }
}

@Composable
fun EditEntryDialog(
    entry: TimeEntry,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (TimeEntry) -> Unit
) {
    var category by remember(entry.id) { mutableStateOf(entry.category) }
    var date by remember(entry.id) { mutableStateOf(entry.start.toLocalDate()) }
    var startFields by remember(entry.id) { mutableStateOf(entry.start.toLocalTime().toHmsFields()) }
    var endFields by remember(entry.id) { mutableStateOf(entry.toEndHmsFields()) }
    var durationFields by remember(entry.id) { mutableStateOf(entry.duration.toHmsFields()) }
    var note by remember(entry.id) { mutableStateOf(entry.note) }

    fun updateStart(updated: HmsFields) {
        startFields = updated
        val start = updated.toLocalTimeOrNull()
        val duration = durationFields.toDurationOrNull()
        if (start != null && duration != null) {
            endFields = start.plusSeconds(duration.seconds).toHmsFields()
        }
    }

    fun updateEnd(updated: HmsFields) {
        endFields = updated
        val startSeconds = startFields.toSecondsOfDayOrNull()
        val endSeconds = updated.toSecondsOfDayAllow24OrNull()
        if (startSeconds != null && endSeconds != null && endSeconds >= startSeconds) {
            durationFields = Duration.ofSeconds((endSeconds - startSeconds).toLong()).toHmsFields()
        }
    }

    fun updateDuration(updated: HmsFields) {
        durationFields = updated
        val start = startFields.toLocalTimeOrNull()
        val duration = updated.toDurationOrNull()
        if (start != null && duration != null) {
            endFields = start.plusSeconds(duration.seconds).toHmsFields()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(TXT_EDIT_ENTRY) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DialogCategoryDropdown(
                    selectedCategory = category,
                    categories = categories,
                    onSelected = { category = it }
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = TXT_DATE, color = Color(0xFF4A4A4A), fontSize = 13.sp)
                    DateSelector(
                        selectedDate = date,
                        onSelectedDate = { date = it }
                    )
                }
                HmsEditorRow(TXT_START, startFields, 2, ::updateStart)
                HmsEditorRow(TXT_END, endFields, 2, ::updateEnd)
                HmsEditorRow(TXT_DURATION, durationFields, 3, ::updateDuration)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(TXT_ENTRY_NOTE) },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startTime = startFields.toLocalTimeOrNull() ?: entry.start.toLocalTime()
                    val duration = durationFields.toDurationOrNull()
                    val startDateTime = LocalDateTime.of(date, startTime)
                    val candidateEnd = endFields.toEndDateTimeOrNull(date)
                    val endDateTime = when {
                        duration != null -> startDateTime.plusSeconds(duration.seconds)
                        candidateEnd != null && !candidateEnd.isBefore(startDateTime) -> candidateEnd
                        else -> startDateTime
                    }
                    onSave(
                        entry.copy(
                            category = category,
                            note = note.trim(),
                            start = startDateTime,
                            end = endDateTime
                        )
                    )
                }
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun DialogCategoryDropdown(
    selectedCategory: String,
    categories: List<Category>,
    onSelected: (String) -> Unit
) {
    var expanded by remember(selectedCategory, categories.size) { mutableStateOf(false) }
    val selected = categories.firstOrNull { it.name == selectedCategory } ?: categories.first()
    val contentColor = if (selected.color.luminance() > 0.55f) Color(0xFF1E1E1E) else Color.White

    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(selected.color)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(selected.name, color = contentColor, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
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
                            Text(category.name, color = Color(0xFF1E1E1E))
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelected(category.name)
                    }
                )
            }
        }
    }
}

@Composable
private fun HmsEditorRow(
    label: String,
    value: HmsFields,
    hourDigits: Int,
    onValueChange: (HmsFields) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, fontWeight = FontWeight.Medium, color = Color(0xFF505050))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallNumberField(
                value = value.hour,
                label = "时",
                width = if (hourDigits > 2) 76.dp else 68.dp,
                maxLength = hourDigits,
                onValueChange = { onValueChange(value.copy(hour = it)) }
            )
            SmallNumberField(
                value = value.minute,
                label = "分",
                width = 68.dp,
                maxLength = 2,
                onValueChange = { onValueChange(value.copy(minute = it)) }
            )
            SmallNumberField(
                value = value.second,
                label = "秒",
                width = 68.dp,
                maxLength = 2,
                onValueChange = { onValueChange(value.copy(second = it)) }
            )
        }
    }
}

@Composable
private fun SmallNumberField(
    value: String,
    label: String,
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
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun secondsToAngle(seconds: Float): Float {
    return (seconds / 86400f) * 360f + 90f
}

private fun secondsToSweep(startSeconds: Float, endSeconds: Float): Float {
    return ((endSeconds - startSeconds).coerceAtLeast(0f) / 86400f) * 360f
}

private fun TimeEntry.endSecondOfDayForChart(): Float {
    return if (end.toLocalTime() == LocalTime.MIDNIGHT && end.toLocalDate().isAfter(start.toLocalDate())) {
        86400f
    } else {
        end.toLocalTime().toSecondOfDay().toFloat()
    }
}

private fun angleToRadians(angle: Float): Double {
    return angle / 180f * PI
}

private fun ringSegmentPath(
    outerRect: Rect,
    innerRect: Rect,
    startAngle: Float,
    sweepAngle: Float
): Path {
    return Path().apply {
        arcTo(rect = outerRect, startAngleDegrees = startAngle, sweepAngleDegrees = sweepAngle, forceMoveTo = true)
        arcTo(
            rect = innerRect,
            startAngleDegrees = startAngle + sweepAngle,
            sweepAngleDegrees = -sweepAngle,
            forceMoveTo = false
        )
        close()
    }
}

private fun buildMonthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    val leadingEmptyCount = firstDay.dayOfWeek.value - 1
    val days = MutableList<LocalDate?>(leadingEmptyCount) { null }
    repeat(month.lengthOfMonth()) { index ->
        days += month.atDay(index + 1)
    }
    while (days.size % 7 != 0) {
        days += null
    }
    return days
}

private data class CategorySummary(
    val category: String,
    val duration: Duration
)

private data class HmsFields(
    val hour: String,
    val minute: String,
    val second: String
)

private fun LocalTime.toHmsFields(): HmsFields {
    return HmsFields(
        hour = hour.toString().padStart(2, '0'),
        minute = minute.toString().padStart(2, '0'),
        second = second.toString().padStart(2, '0')
    )
}

private fun Duration.toHmsFields(): HmsFields {
    val totalSeconds = seconds.coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secondsPart = totalSeconds % 60
    return HmsFields(
        hour = hours.toString(),
        minute = minutes.toString().padStart(2, '0'),
        second = secondsPart.toString().padStart(2, '0')
    )
}

private fun HmsFields.toLocalTimeOrNull(): LocalTime? {
    val hourValue = hour.toIntOrNull() ?: return null
    val minuteValue = minute.toIntOrNull() ?: return null
    val secondValue = second.toIntOrNull() ?: return null
    if (hourValue !in 0..23 || minuteValue !in 0..59 || secondValue !in 0..59) return null
    return LocalTime.of(hourValue, minuteValue, secondValue)
}

private fun HmsFields.toSecondsOfDayOrNull(): Int? {
    val hourValue = hour.toIntOrNull() ?: return null
    val minuteValue = minute.toIntOrNull() ?: return null
    val secondValue = second.toIntOrNull() ?: return null
    if (hourValue !in 0..23 || minuteValue !in 0..59 || secondValue !in 0..59) return null
    return hourValue * 3600 + minuteValue * 60 + secondValue
}

private fun HmsFields.toSecondsOfDayAllow24OrNull(): Int? {
    val hourValue = hour.toIntOrNull() ?: return null
    val minuteValue = minute.toIntOrNull() ?: return null
    val secondValue = second.toIntOrNull() ?: return null
    return when {
        hourValue in 0..23 && minuteValue in 0..59 && secondValue in 0..59 -> {
            hourValue * 3600 + minuteValue * 60 + secondValue
        }

        hourValue == 24 && minuteValue == 0 && secondValue == 0 -> 24 * 3600
        else -> null
    }
}

private fun HmsFields.toEndDateTimeOrNull(date: LocalDate): LocalDateTime? {
    val seconds = toSecondsOfDayAllow24OrNull() ?: return null
    return if (seconds == 24 * 3600) {
        date.plusDays(1).atStartOfDay()
    } else {
        LocalDateTime.of(date, LocalTime.ofSecondOfDay(seconds.toLong()))
    }
}

private fun TimeEntry.toEndHmsFields(): HmsFields {
    val isMidnightNextDay = end.toLocalTime() == LocalTime.MIDNIGHT && end.toLocalDate().isAfter(start.toLocalDate())
    return if (isMidnightNextDay) {
        HmsFields(hour = "24", minute = "00", second = "00")
    } else {
        end.toLocalTime().toHmsFields()
    }
}

private fun HmsFields.toDurationOrNull(): Duration? {
    val hourValue = hour.toIntOrNull() ?: return null
    val minuteValue = minute.toIntOrNull() ?: return null
    val secondValue = second.toIntOrNull() ?: return null
    if (hourValue < 0 || minuteValue !in 0..59 || secondValue !in 0..59) return null
    return Duration.ofHours(hourValue.toLong())
        .plusMinutes(minuteValue.toLong())
        .plusSeconds(secondValue.toLong())
}
