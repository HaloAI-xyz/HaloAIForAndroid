package xyz.haloai.haloai_android_productivity.data.ui.compose_components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.haloai.haloai_android_productivity.data.ui.theme.HaloAI_Android_ProductivityTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class EventDataForUI(
    val name: String,
    val color: Color,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val description: String? = null,
    val id: Long
)

@JvmInline
value class SplitType private constructor(val value: Int) {
    companion object {
        val None = SplitType(0)
        val Start = SplitType(1)
        val End = SplitType(2)
        val Both = SplitType(3)
    }
}

data class PositionedEvent(
    val event: EventDataForUI,
    val splitType: SplitType,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val col: Int = 0,
    val colSpan: Int = 1,
    val colTotal: Int = 1,
)

val EventTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun BasicEvent(
    positionedEvent: PositionedEvent,
    modifier: Modifier = Modifier,
) {
    val event = positionedEvent.event
    val topRadius = if (positionedEvent.splitType == SplitType.Start || positionedEvent.splitType == SplitType.Both) 0.dp else 4.dp
    val bottomRadius = if (positionedEvent.splitType == SplitType.End || positionedEvent.splitType == SplitType.Both) 0.dp else 4.dp
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                end = 2.dp,
                bottom = if (positionedEvent.splitType == SplitType.End) 0.dp else 2.dp
            )
            .clipToBounds()
            .background(
                event.color,
                shape = RoundedCornerShape(
                    topStart = topRadius,
                    topEnd = topRadius,
                    bottomEnd = bottomRadius,
                    bottomStart = bottomRadius,
                )
            )
            .padding(4.dp)
    ) {
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodyMedium,
            // fontWeight = FontWeight.Thin,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = "${event.start.format(EventTimeFormatter)} - ${event.end.format(
                EventTimeFormatter
            )}",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )

        if (event.description != null) {
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

var currentDate = LocalDate.now()

private val sampleEvents = listOf(
    // Today: 9 AM - 11 AM
    EventDataForUI(
        name = "Google I/O Keynote",
        color = Color(0xFFAFBBF2),
        start = currentDate.atTime(9, 0),
        end = currentDate.atTime(11, 0),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
        id = 0
    ),
    EventDataForUI(
        name = "Developer Keynote",
        color = Color(0xFFAFBBF2),
        start = currentDate.atTime(9, 0),
        end = currentDate.atTime(10, 0),
        description = "Learn about the latest updates to our developer products and platforms from Google Developers.",
        id = 0
    ),
    EventDataForUI(
        name = "What's new in Android",
        color = Color(0xFF1B998B),
        start = currentDate.atTime(10, 0),
        end = currentDate.atTime(12, 0),
        description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
        id = 0
    ),
    EventDataForUI(
        name = "What's new in Material Design",
        color = Color(0xFF6DD3CE),
        start = currentDate.atTime(13, 0),
        end = currentDate.atTime(13, 45),
        description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design.",
        id = 0
    ),
    EventDataForUI(
        name = "What's new in Machine Learning",
        color = Color(0xFFF4BFDB),
        start = currentDate.atTime(17, 0),
        end = currentDate.atTime(17, 30),
        description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        id = 0
    ),
    EventDataForUI(
        name = "What's new in Machine Learning",
        color = Color(0xFFF4BFDB),
        start = currentDate.atTime(18, 0),
        end = currentDate.atTime(20, 0),
        description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        id = 0
    ),
    EventDataForUI(
        name = "Jetpack Compose Basics",
        color = Color(0xFF1B998B),
        start = currentDate.atTime(21, 0),
        end = currentDate.atTime(23, 59),
        description = "This Workshop will take you through the basics of building your first app with Jetpack Compose, Android's new modern UI toolkit that simplifies and accelerates UI development on Android.",
        id = 0
    ),
)

class EventsProvider : PreviewParameterProvider<EventDataForUI> {
    override val values = sampleEvents.asSequence()
}

@Preview(showBackground = true)
@Composable
fun EventPreview(
    @PreviewParameter(EventsProvider::class) event: EventDataForUI,
) {
    HaloAI_Android_ProductivityTheme {
        BasicEvent(
            PositionedEvent(event, SplitType.None, event.start.toLocalDate(), event.start.toLocalTime(), event.end.toLocalTime()),
            modifier = Modifier.sizeIn(maxHeight = 64.dp)
        )
    }
}

private class EventDataModifier(
    val positionedEvent: PositionedEvent,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = positionedEvent
}

private fun Modifier.eventData(positionedEvent: PositionedEvent) = this.then(EventDataModifier(positionedEvent))

private val DayFormatter = DateTimeFormatter.ofPattern("EE, MMM d")

@Composable
fun BasicDayHeader(
    day: LocalDate,
    modifier: Modifier = Modifier,
) {
    Text(
        text = day.format(DayFormatter),
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun BasicDayHeaderPreview() {
    HaloAI_Android_ProductivityTheme {
        BasicDayHeader(day = LocalDate.now())
    }
}

@Composable
fun ScheduleHeader(
    minDate: LocalDate,
    maxDate: LocalDate,
    dayWidth: Dp,
    modifier: Modifier = Modifier,
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) },
) {
    Row(modifier = modifier) {
        val numDays = ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1
        repeat(numDays) { i ->
            Box(modifier = Modifier.width(dayWidth)) {
                dayHeader(minDate.plusDays(i.toLong()))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleHeaderPreview() {
    HaloAI_Android_ProductivityTheme {
        ScheduleHeader(
            minDate = LocalDate.now(),
            maxDate = LocalDate.now().plusDays(5),
            dayWidth = 256.dp,
        )
    }
}

private val HourFormatter = DateTimeFormatter.ofPattern("h a")

@Composable
fun BasicSidebarLabel(
    time: LocalTime,
    modifier: Modifier = Modifier,
) {
    Text(
        text = time.format(HourFormatter),
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp),
        fontSize = 12.sp,
    )
}

@Preview(showBackground = true)
@Composable
fun BasicSidebarLabelPreview() {
    HaloAI_Android_ProductivityTheme {
        BasicSidebarLabel(time = LocalTime.NOON, Modifier.sizeIn(maxHeight = 64.dp))
    }
}

@Composable
fun ScheduleSidebar(
    hourHeight: Dp,
    modifier: Modifier = Modifier,
    minTime: LocalTime = LocalTime.MIN,
    maxTime: LocalTime = LocalTime.MAX,
    label: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
) {
    val numMinutes = ChronoUnit.MINUTES.between(minTime, maxTime).toInt() + 1
    val numHours = numMinutes / 60
    val firstHour = minTime.truncatedTo(ChronoUnit.HOURS)
    val firstHourOffsetMinutes = if (firstHour == minTime) 0 else ChronoUnit.MINUTES.between(minTime, firstHour.plusHours(1))
    val firstHourOffset = hourHeight * (firstHourOffsetMinutes / 60f)
    val startTime = if (firstHour == minTime) firstHour else firstHour.plusHours(1)
    Column(modifier = modifier) {
        Spacer(modifier = Modifier.height(firstHourOffset))
        repeat(numHours) { i ->
            Box(modifier = Modifier.height(hourHeight)) {
                label(startTime.plusHours(i.toLong()))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ScheduleSidebarPreview() {
    HaloAI_Android_ProductivityTheme {
        ScheduleSidebar(hourHeight = 64.dp)
    }
}

private fun splitEvents(events: List<EventDataForUI>): List<PositionedEvent> {
    return events
        .map { event ->
            val startDate = event.start.toLocalDate()
            val endDate = event.end.toLocalDate()
            if (startDate == endDate) {
                listOf(PositionedEvent(event, SplitType.None, event.start.toLocalDate(), event.start.toLocalTime(), event.end.toLocalTime()))
            } else {
                val days = ChronoUnit.DAYS.between(startDate, endDate)
                val splitEvents = mutableListOf<PositionedEvent>()
                for (i in 0..days) {
                    val date = startDate.plusDays(i)
                    splitEvents += PositionedEvent(
                        event,
                        splitType = if (date == startDate) SplitType.End else if (date == endDate) SplitType.Start else SplitType.Both,
                        date = date,
                        start = if (date == startDate) event.start.toLocalTime() else LocalTime.MIN,
                        end = if (date == endDate) event.end.toLocalTime() else LocalTime.MAX,
                    )
                }
                splitEvents
            }
        }
        .flatten()
}

private fun PositionedEvent.overlapsWith(other: PositionedEvent): Boolean {
    // Return true if the events are on the same day and overlap in time
    var overlap = false
    if (date == other.date) {
        if (start < other.start && end > other.start) { // Event starts before other event and ends after other event starts
            overlap = true
        }
        else if (start > other.start && start < other.end) { // Event starts after other event starts and before other event ends
            overlap = true
        }
    }

    return overlap
}

private fun List<PositionedEvent>.timesOverlapWith(event: PositionedEvent): Boolean {
    return any { it.overlapsWith(event) }
}

private fun arrangeEvents(events: List<PositionedEvent>): List<PositionedEvent> {
    val positionedEvents = mutableListOf<PositionedEvent>()
    val groupEvents: MutableList<MutableList<PositionedEvent>> = mutableListOf()

    fun resetGroup() {
        groupEvents.forEachIndexed { colIndex, col ->
            col.forEach { e ->
                positionedEvents.add(e.copy(col = colIndex, colTotal = groupEvents.size))
            }
        }
        groupEvents.clear()
    }

    events.forEach { event ->
        var firstFreeCol = -1
        var numFreeCol = 0
        for (i in 0 until groupEvents.size) {
            val col = groupEvents[i]
            if (col.timesOverlapWith(event)) {
                if (firstFreeCol < 0) continue else break
            }
            if (firstFreeCol < 0) firstFreeCol = i
            numFreeCol++
        }

        when {
            // Overlaps with all, add a new column
            firstFreeCol < 0 -> {
                groupEvents += mutableListOf(event)
                // Expand anything that spans into the previous column and doesn't overlap with this event
                for (ci in 0 until groupEvents.size - 1) {
                    val col = groupEvents[ci]
                    col.forEachIndexed { ei, e ->
                        if (ci + e.colSpan == groupEvents.size - 1 && !e.overlapsWith(event)) {
                            col[ei] = e.copy(colSpan = e.colSpan + 1)
                        }
                    }
                }
            }
            // No overlap with any, start a new group
            numFreeCol == groupEvents.size -> {
                resetGroup()
                groupEvents += mutableListOf(event)
            }
            // At least one column free, add to first free column and expand to as many as possible
            else -> {
                groupEvents[firstFreeCol] += event.copy(colSpan = numFreeCol)
            }
        }
    }
    resetGroup()
    return positionedEvents
}

sealed class ScheduleSize {
    class FixedSize(val size: Dp) : ScheduleSize()
    class FixedCount(val count: Float) : ScheduleSize() {
        constructor(count: Int) : this(count.toFloat())
    }
    class Adaptive(val minSize: Dp) : ScheduleSize()
}

// Extension function to convert ScheduleSize to pixels

private fun ScheduleSize.toPxSize(density: Density): Float {
    return with(density) {
        when (this@toPxSize) {
            is ScheduleSize.FixedSize -> this@toPxSize.size.toPx()
            is ScheduleSize.FixedCount -> 64.dp.toPx() // Default value, should be handled based on your logic
            is ScheduleSize.Adaptive -> this@toPxSize.minSize.toPx()
        }
    }
}

@Composable
fun Schedule(
    events: List<EventDataForUI>,
    modifier: Modifier = Modifier,
    eventContent: @Composable (positionedEvent: PositionedEvent) -> Unit = { BasicEvent(positionedEvent = it) },
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) },
    timeLabel: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
    minDate: LocalDate = events.minByOrNull(EventDataForUI::start)?.start?.toLocalDate() ?: LocalDate.now(),
    maxDate: LocalDate = events.maxByOrNull(EventDataForUI::end)?.end?.toLocalDate() ?: LocalDate.now(),
    minTime: LocalTime = LocalTime.MIN,
    maxTime: LocalTime = LocalTime.MAX,
    daySize: ScheduleSize = ScheduleSize.FixedSize(256.dp),
    hourSize: ScheduleSize = ScheduleSize.FixedSize(64.dp),
    onEventClickFunction: (Long) -> Unit = { },
) {
    val numDays = ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1
    val numMinutes = ChronoUnit.MINUTES.between(minTime, maxTime).toInt() + 1
    val numHours = numMinutes.toFloat() / 60f
    val density = LocalDensity.current

    val initialScrollPosition = if (minDate == LocalDate.now()) {
        val now = LocalTime.now()
        val roundedNow = now.withMinute((now.minute / 30) * 30).withSecond(0).withNano(0)
        val minutesFromMinTime = ChronoUnit.MINUTES.between(minTime, roundedNow).toInt()
        (minutesFromMinTime * hourSize.toPxSize(density) / 60).toInt()
    } else {
        (8 * hourSize.toPxSize(density)).toInt() // 8 AM
    }
    val verticalScrollState = rememberScrollState(initialScrollPosition) // Reset if minDate changes
    LaunchedEffect(minDate) {
        verticalScrollState.scrollTo(initialScrollPosition)
    }

    val horizontalScrollState = rememberScrollState()
    var sidebarWidth by remember { mutableStateOf(0) }
    var headerHeight by remember { mutableStateOf(0) }

    BoxWithConstraints(modifier = modifier) {
        val dayWidth: Dp = when (daySize) {
            is ScheduleSize.FixedSize -> daySize.size
            is ScheduleSize.FixedCount -> with(LocalDensity.current) { ((constraints.maxWidth - sidebarWidth) / daySize.count).toDp() }
            is ScheduleSize.Adaptive -> with(LocalDensity.current) { maxOf(((constraints.maxWidth - sidebarWidth) / numDays).toDp(), daySize.minSize) }
        }
        val hourHeight: Dp = when (hourSize) {
            is ScheduleSize.FixedSize -> hourSize.size
            is ScheduleSize.FixedCount -> with(LocalDensity.current) { ((constraints.maxHeight - headerHeight) / hourSize.count).toDp() }
            is ScheduleSize.Adaptive -> with(LocalDensity.current) { maxOf(((constraints.maxHeight - headerHeight) / numHours).toDp(), hourSize.minSize) }
        }
        Column(modifier = modifier) {
            /*ScheduleHeader(
                minDate = minDate,
                maxDate = maxDate,
                dayWidth = dayWidth,
                dayHeader = dayHeader,
                modifier = Modifier
                    .padding(start = with(LocalDensity.current) { sidebarWidth.toDp() })
                    .horizontalScroll(horizontalScrollState)
                    .onGloballyPositioned { headerHeight = it.size.height }
            )*/
            Row(modifier = Modifier
                .weight(1f)
                .align(Alignment.Start)) {
                ScheduleSidebar(
                    hourHeight = hourHeight,
                    minTime = minTime,
                    maxTime = maxTime,
                    label = timeLabel,
                    modifier = Modifier
                        .verticalScroll(verticalScrollState)
                        .onGloballyPositioned { sidebarWidth = it.size.width }
                )
                BasicSchedule(
                    events = events,
                    eventContent = eventContent,
                    minDate = minDate,
                    maxDate = maxDate,
                    minTime = minTime,
                    maxTime = maxTime,
                    dayWidth = dayWidth,
                    hourHeight = hourHeight,
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState),
                    onEventClickFunction = onEventClickFunction
                )
            }
        }
    }
}

@Composable
fun BasicSchedule(
    events: List<EventDataForUI>,
    modifier: Modifier = Modifier,
    eventContent: @Composable (positionedEvent: PositionedEvent) -> Unit = { BasicEvent(positionedEvent = it) },
    minDate: LocalDate = events.minByOrNull(EventDataForUI::start)?.start?.toLocalDate() ?: LocalDate.now(),
    maxDate: LocalDate = events.maxByOrNull(EventDataForUI::end)?.end?.toLocalDate() ?: LocalDate.now(),
    minTime: LocalTime = LocalTime.MIN,
    maxTime: LocalTime = LocalTime.MAX,
    dayWidth: Dp,
    hourHeight: Dp,
    onEventClickFunction: (Long) -> Unit = { },
) {
    val numDays = ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1
    val numMinutes = ChronoUnit.MINUTES.between(minTime, maxTime).toInt() + 1
    val numHours = numMinutes / 60
    val dividerColor = Color.LightGray // TODO: Hardcoding for now, need to account for dark mode
    // val positionedEvents = remember(events) { arrangeEvents(splitEvents(events.sortedBy
        // (EventDataForUI::start))).filter { it.end > minTime && it.start < maxTime } }
    // val positionedEvents = remember(events) {
    val sortedEvents = events.sortedBy(EventDataForUI::start)
    val splitEvents = splitEvents(sortedEvents)
    val arrangedEvents = arrangeEvents(splitEvents)
    val filteredEvents = arrangedEvents.filter { event ->
        val eventStartTime = event.start
        val eventEndTime = event.end
        eventEndTime > minTime && eventStartTime < maxTime
    }
    val positionedEvents = filteredEvents
        /*arrangeEvents(splitEvents(events.sortedBy(EventDataForUI::start)))
            .filter { event ->
                val eventStartTime = event.start
                val eventEndTime = event.end
                eventEndTime > minTime && eventStartTime < maxTime
            }*/
    // }
    Layout(
        content = {
            positionedEvents.forEach { positionedEvent ->
                Box(modifier = Modifier
                    .eventData(positionedEvent)
                    .clickable { onEventClickFunction(positionedEvent.event.id) }
                    ) {
                    eventContent(positionedEvent)
                }
            }
        },
        modifier = modifier
            .drawBehind {
                val firstHour = minTime.truncatedTo(ChronoUnit.HOURS)
                val firstHourOffsetMinutes = if (firstHour == minTime) 0 else ChronoUnit.MINUTES.between(minTime, firstHour.plusHours(1))
                val firstHourOffset = (firstHourOffsetMinutes / 60f) * hourHeight.toPx()
                repeat(numHours) {
                    drawLine(
                        dividerColor,
                        start = Offset(0f, it * hourHeight.toPx() + firstHourOffset),
                        end = Offset(size.width, it * hourHeight.toPx() + firstHourOffset),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                repeat(numDays - 1) {
                    drawLine(
                        dividerColor,
                        start = Offset((it + 1) * dayWidth.toPx(), 0f),
                        end = Offset((it + 1) * dayWidth.toPx(), size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
    ) { measureables, constraints ->
        val height = (hourHeight.toPx() * (numMinutes / 60f)).roundToInt()
        val width = dayWidth.roundToPx() * numDays
        val placeablesWithEvents = measureables.map { measurable ->
            val splitEvent = measurable.parentData as PositionedEvent
            val eventDurationMinutes = ChronoUnit.MINUTES.between(splitEvent.start, minOf(splitEvent.end, maxTime))
            val eventHeight = ((eventDurationMinutes / 60f) * hourHeight.toPx()).roundToInt()
            val eventWidth = ((splitEvent.colSpan.toFloat() / splitEvent.colTotal.toFloat()) * dayWidth.toPx()).roundToInt()
            val placeable = measurable.measure(constraints.copy(minWidth = eventWidth, maxWidth = eventWidth, minHeight = eventHeight, maxHeight = eventHeight))
            Pair(placeable, splitEvent)
        }
        layout(width, height) {
            placeablesWithEvents.forEach { (placeable, splitEvent) ->
                val eventOffsetMinutes = if (splitEvent.start > minTime) ChronoUnit.MINUTES.between(minTime, splitEvent.start) else 0
                val eventY = ((eventOffsetMinutes / 60f) * hourHeight.toPx()).roundToInt()
                val eventOffsetDays = ChronoUnit.DAYS.between(minDate, splitEvent.date).toInt()
                val eventX = eventOffsetDays * dayWidth.roundToPx() + (splitEvent.col * (dayWidth.toPx() / splitEvent.colTotal.toFloat())).roundToInt()
                placeable.place(eventX, eventY)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SchedulePreview() {
    HaloAI_Android_ProductivityTheme {
        Schedule(sampleEvents)
    }
}