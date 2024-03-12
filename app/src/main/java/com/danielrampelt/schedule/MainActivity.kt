package com.danielrampelt.schedule

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danielrampelt.schedule.ui.theme.WeekScheduleTheme
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeekScheduleTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    Schedule(events = sampleEvents)
                }
            }
        }
    }
}

data class Event(
    val name: String,
    val initialName: String? = "",
    val color: Color,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val description: String? = null,
    val type: Int? = null,
    val eventProps: EventProps? = null,
)
//type = 1 event
//2, Training/Break/DayOff


data class EventProps(
    val textColor: String? = null,
    val borderColor: String? = null,
    val priority: Boolean = false,
    val initialBgColor: String? = null
)

inline class SplitType private constructor(val value: Int) {
    companion object {
        val None = SplitType(0)
        val Start = SplitType(1)
        val End = SplitType(2)
        val Both = SplitType(3)
    }
}

data class PositionedEvent(
    val event: Event,
    val splitType: SplitType,
    val date: LocalDate,
    val start: LocalTime,
    val end: LocalTime,
    val col: Int = 0,
    val colSpan: Int = 1,
    val colTotal: Int = 1,
)

//TODO MAKE IT DYNAMIC FOR TODAY

val daysToAdd: Long = 2
//val daysToAdd = ViewType.ThreeDayView
val EventTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun BasicEvent(
    positionedEvent: PositionedEvent,
    modifier: Modifier = Modifier,
) {
    val event = positionedEvent.event

    if (event.type?.equals(2) == true) {
//        Training/Break/Continue
        OtherMiscEvents(modifier, positionedEvent, event)

    } else {
//        Event
        if (isEndWithinHalfHour(event.start, event.end, 30)) {
            HalfHourEventStrips(event)
        } else {
            val context = LocalContext.current
            val modifier = Modifier.clickable {
                Toast.makeText(context, "Event: ${event.name}", Toast.LENGTH_SHORT).show()

            }
            UpdateEvent(positionedEvent, event, modifier)
        }


    }

}

@Composable
fun OtherMiscEvents(
    modifier: Modifier,
    positionedEvent: PositionedEvent,
    event: Event,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                end = 2.dp,
                bottom = if (positionedEvent.splitType == SplitType.End) 0.dp else 2.dp
            )
            .clipToBounds()
            .background(
                event.color,
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 2.dp,
                color = Color(android.graphics.Color.parseColor(event.eventProps?.borderColor)),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(4.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // TODO Change 5 to some value
            val endY = maxOf(size.width, size.height) * 5
            val diagonalLength = sqrt((size.width * size.width) + (size.height * size.height))
            val numberOfLines = 35
            val spacing = diagonalLength / numberOfLines
            for (i in 0 until numberOfLines) {
                val startX = i * spacing
                val endX = startX - size.width // Extend lines beyond the right side of the box
                drawLine(
                    color = Color(android.graphics.Color.parseColor("#E0E0E0")),
                    start = Offset(startX, 0f),
                    end = Offset(endX, endY),
                    strokeWidth = 2f
                )
            }
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            color = Color(android.graphics.Color.parseColor(event.eventProps?.textColor.toString())),
            text = event.name,
            style = MaterialTheme.typography.body1,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UpdateEvent(positionedEvent: PositionedEvent, event: Event, modifier: Modifier) {
    Box(
        modifier = modifier.then(
            Modifier
                .fillMaxSize()
                .padding(
                    end = 2.dp,
                    bottom = if (positionedEvent.splitType == SplitType.End) 0.dp else 2.dp
                )
                .clipToBounds()
                .background(
                    Color(android.graphics.Color.parseColor("#FFFFFF")),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = 2.dp,
                    color = Color(android.graphics.Color.parseColor("#D9D9D9")),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(4.dp)
        )
    ) {

//            ------------------SLA BREACH LINE Start-----------------
        if (event.end < LocalDateTime.now()) {
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .clipToBounds()
                    .background(Color.Red)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(16.dp))
            )
        }
//            ------------------SLA BREACH LINE End-----------------

        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .fillMaxHeight()
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(event.eventProps?.initialBgColor?:"#E69F00")))
                ) {
                    Text(
                        text = event.initialName ?: "",
                        style = MaterialTheme.typography.body1.copy(fontSize = 12.sp),
                        color = Color(android.graphics.Color.parseColor("#FFFFFF")),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                if (event.eventProps?.priority == true) {
                    Spacer(modifier = Modifier.width(6.dp))

                    Image(
                        painter = painterResource(id = R.drawable.ic_priority),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }

            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                color = Color(android.graphics.Color.parseColor("#2D2D2D")),
                text = event.name,
                style = MaterialTheme.typography.body1,
                fontWeight = FontWeight.Normal,
                overflow = TextOverflow.Ellipsis,
            )

        }
    }
}

@Composable
fun HalfHourEventStrips(event: Event) {
    val RoundedShape = MaterialTheme.shapes.medium
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .background(
                Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(0.dp),
        content = {
            Column(
                modifier = Modifier.fillMaxSize(),
                content = {
                    val durationMinutes = Duration.between(event.start, event.end).toMinutes()

                    val numSurfaces = when {
                        durationMinutes <= 10 -> 1
                        durationMinutes <= 20 -> 2
                        else -> 3
                    }

                    repeat(numSurfaces) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(1.dp),
                            color = event.color ?: Color(android.graphics.Color.parseColor("#CF9044")),
                            shape = RoundedShape,
                            border = BorderStroke(
                                1.dp,
                                Color(android.graphics.Color.parseColor(event.eventProps?.borderColor?:"#E8E9E8"))
                            )
                        ) {}
                    }
                }
            )
        }
    )
}

fun isEndWithinHalfHour(start: LocalDateTime, end: LocalDateTime, minutes: Long): Boolean {
    val halfHour = Duration.ofMinutes(minutes)
    val duration = Duration.between(start, end)
    return duration <= halfHour
}

private val sampleEvents = listOf(

    Event(
        name = "Broadband, Install test data",
        initialName = "FR",
        color = Color(0xFFAFBBF2),
        start = LocalDateTime.parse("2024-03-13T09:00:00"),
        end = LocalDateTime.parse("2024-03-13T11:00:00"),
        description = "Tune in to find out about how we're furthering our mission to organize the world’s information and make it universally accessible and useful.",
        type = 1,
        eventProps = EventProps(
            priority = true,
            initialBgColor = "#469C76"
        )
    ),
    Event(
        name = "Training",
        color = Color(android.graphics.Color.parseColor("#FFE0B2")),
        start = LocalDateTime.parse("2024-03-12T17:00:00"),
        end = LocalDateTime.parse("2024-03-12T18:30:00"),
        description = "Learn about the latest updates to our developer products and platforms from Google Developers.",
        type = 2,
        eventProps = EventProps(
            textColor = "#E19236",
            borderColor = "#E19236"
        )
    ),
    Event(
        name = "What's new in Android",
        color = Color(android.graphics.Color.parseColor("#CF9044")),
        start = LocalDateTime.parse("2024-03-12T09:00:00"),
        end = LocalDateTime.parse("2024-03-12T09:30:00"),
        description = "In this Keynote, Chet Haase, Dan Sandler, and Romain Guy discuss the latest Android features and enhancements for developers.",
        eventProps = EventProps(borderColor = "#E8E9E8")
    ),
    Event(
        name = "Broadband, Fault Repair",
        initialName = "I",
        color = Color(0xFF6DD3CE),
        start = LocalDateTime.parse("2024-03-12T13:00:00"),
        end = LocalDateTime.parse("2024-03-12T13:32:00"),
        description = "Learn about the latest design improvements to help you build personal dynamic experiences with Material Design.",
        eventProps = EventProps(initialBgColor = "#0072B2")
    ),
    Event(
        name = "15 mins view",
        color = Color(android.graphics.Color.parseColor("#CF9044")),
        start = LocalDateTime.parse("2024-03-12T15:00:00"),
        end = LocalDateTime.parse("2024-03-12T15:20:00"),
        description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
        eventProps = EventProps(borderColor = "#E8E9E8")
    ),
    Event(
        name = "What's new in Machine Learning",
        color = Color(0xFFF4BFDB),
        start = LocalDateTime.parse("2021-05-18T10:30:00"),
        end = LocalDateTime.parse("2021-05-18T11:30:00"),
        description = "Learn about the latest and greatest in ML from Google. We’ll cover what’s available to developers when it comes to creating, understanding, and deploying models for a variety of different applications.",
    ),
    Event(
        name = "Jetpack Compose Basics",
        color = Color(0xFF1B998B),
        start = LocalDateTime.parse("2021-05-20T12:00:00"),
        end = LocalDateTime.parse("2021-05-20T13:00:00"),
        description = "This Workshop will take you through the basics of building your first app with Jetpack Compose, Android's new modern UI toolkit that simplifies and accelerates UI development on Android.",
    ),
)

class EventsProvider : PreviewParameterProvider<Event> {
    override val values = sampleEvents.asSequence()
}

@Preview(showBackground = true)
@Composable
fun EventPreview(
    @PreviewParameter(EventsProvider::class) event: Event,
) {
    WeekScheduleTheme {
        BasicEvent(
            PositionedEvent(
                event,
                SplitType.None,
                event.start.toLocalDate(),
                event.start.toLocalTime(),
                event.end.toLocalTime()
            ),
            modifier = Modifier.sizeIn(maxHeight = 64.dp)
        )
    }
}

private class EventDataModifier(
    val positionedEvent: PositionedEvent,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = positionedEvent
}

private fun Modifier.eventData(positionedEvent: PositionedEvent) =
    this.then(EventDataModifier(positionedEvent))

private val DayFormatter = DateTimeFormatter.ofPattern("EE, MMM d")

@Composable
fun BasicDayHeader(
    day: LocalDate,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp),
        verticalArrangement = Arrangement.Center
    ) {
        RoundedDateBox(date = day)
    }
}

@Composable
fun RoundedDateBox(date: LocalDate) {
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("EEE")
    val dateFormatter = DateTimeFormatter.ofPattern("dd")

    val backgroundColor =
        if (date == today) Color(android.graphics.Color.parseColor("#E1EFFF")) else Color(
            android.graphics.Color.parseColor("#F0F0F0")
        )
    val textColor =
        if (date == today) Color(android.graphics.Color.parseColor("#027BFC")) else Color(
            android.graphics.Color.parseColor("#292C31")
        )

    Box(
        modifier = Modifier
            .padding(6.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(top = 8.dp, bottom = 8.dp, start = 18.dp, end = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = dateFormatter.format(date),
                style = MaterialTheme.typography.h5.copy(fontSize = 24.sp, color = textColor)
            )
            Text(
                text = dayFormatter.format(date),
                style = MaterialTheme.typography.subtitle1.copy(fontSize = 16.sp, color = textColor)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BasicDayHeaderPreview() {
    WeekScheduleTheme {
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
    WeekScheduleTheme {
        ScheduleHeader(
            //Preview
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
        style = TextStyle(
            fontSize = 16.sp,
            color = Color(android.graphics.Color.parseColor("#0C0F12"))
        ),
        modifier = modifier
            .fillMaxHeight()
            .padding(4.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun BasicSidebarLabelPreview() {
    WeekScheduleTheme {
        BasicSidebarLabel(time = LocalTime.NOON, Modifier.sizeIn(maxHeight = 64.dp))
    }
}

@Composable
fun ScheduleSidebar(
    hourHeight: Dp,
    modifier: Modifier = Modifier,
    label: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
) {

//    --------------- SETUP START MIN TIME-------------------

    val minTimes = getMinMaxTimes().first
    val maxTimes = getMinMaxTimes().second

//    --------------- SETUP END MAX TIME-------------------

    val numMinutes = ChronoUnit.MINUTES.between(minTimes, maxTimes).toInt() + 1
    val numHours = numMinutes / 60
    val firstHour = minTimes.truncatedTo(ChronoUnit.HOURS)
    val firstHourOffsetMinutes =
        if (firstHour == minTimes) 0 else ChronoUnit.MINUTES.between(
            minTimes,
            firstHour.plusHours(1)
        )
    val firstHourOffset = hourHeight * (firstHourOffsetMinutes / 60f)
    val startTime = if (firstHour == minTimes) firstHour else firstHour.plusHours(1)
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
    WeekScheduleTheme {
        ScheduleSidebar(hourHeight = 64.dp)
    }
}

private fun splitEvents(events: List<Event>): List<PositionedEvent> {
    return events
        .map { event ->
            val startDate = event.start.toLocalDate()
            val endDate = event.end.toLocalDate()
            if (startDate == endDate) {
                listOf(
                    PositionedEvent(
                        event,
                        SplitType.None,
                        event.start.toLocalDate(),
                        event.start.toLocalTime(),
                        event.end.toLocalTime()
                    )
                )
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
    return date == other.date && start < other.end && end > other.start
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

@Composable
fun Schedule(
    events: List<Event>,
    modifier: Modifier = Modifier,
    eventContent: @Composable (positionedEvent: PositionedEvent) -> Unit = {
        BasicEvent(
            positionedEvent = it
        )
    },
    dayHeader: @Composable (day: LocalDate) -> Unit = { BasicDayHeader(day = it) },
    timeLabel: @Composable (time: LocalTime) -> Unit = { BasicSidebarLabel(time = it) },
    minDate: LocalDate = LocalDate.now(),
    maxDate: LocalDate = LocalDate.now().plusDays(daysToAdd),
    daySize: Dp = GridDimensions.height,
    hourSize: Dp = GridDimensions.width,
) {

    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    var sidebarWidth by remember { mutableStateOf(0) }
    var headerHeight by remember { mutableStateOf(0) }
    BoxWithConstraints(modifier = modifier) {

        Column(modifier = modifier) {
            ScheduleHeader(
                minDate = minDate,
                maxDate = maxDate,
                dayWidth = daySize,
                dayHeader = dayHeader,
                modifier = Modifier
                    .background(Color(android.graphics.Color.parseColor("#FAFAFA")))
                    .padding(start = with(LocalDensity.current) { sidebarWidth.toDp() })
                    .horizontalScroll(horizontalScrollState)
                    .onGloballyPositioned {
                        headerHeight = it.size.height
                    }
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.Start)
            ) {

                ScheduleSidebar(
                    hourHeight = hourSize,
                    modifier = Modifier
                        .background(Color(android.graphics.Color.parseColor("#E8E9E8")))
                        .verticalScroll(verticalScrollState)
                        .onGloballyPositioned { sidebarWidth = it.size.width },
                    label = timeLabel
                )
                BasicSchedule(
                    events = events,
                    modifier = Modifier
                        .weight(1f)
                        .background(Color(android.graphics.Color.parseColor("#FAFAFA")))
                        .verticalScroll(verticalScrollState)
                        .horizontalScroll(horizontalScrollState),
                    eventContent = eventContent,
                    minDate = minDate,
                    maxDate = maxDate,
                    dayWidth = daySize,
                    hourHeight = hourSize
                )

            }
        }
    }
}


@Composable
fun BasicSchedule(
    events: List<Event>,
    modifier: Modifier = Modifier,
    eventContent: @Composable (positionedEvent: PositionedEvent) -> Unit = {
        BasicEvent(
            positionedEvent = it
        )
    },
    minDate: LocalDate = events.minByOrNull(Event::start)?.start?.toLocalDate() ?: LocalDate.now(),
    maxDate: LocalDate = events.maxByOrNull(Event::end)?.end?.toLocalDate() ?: LocalDate.now(),
    dayWidth: Dp,
    hourHeight: Dp,
) {

//    --------------- SETUP START MIN TIME-------------------

    val minTimes = getMinMaxTimes().first
    val maxTimes = getMinMaxTimes().second

//    --------------- SETUP END MAX TIME-------------------

    val lastUpdateTime = remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(lastUpdateTime.value) {
        while (true) {
            delay(5 * 60 * 1000) // 5 minutes in milliseconds
            lastUpdateTime.value = LocalDateTime.now()
        }
    }

    val currentTime = LocalTime.now()

    val numDays = ChronoUnit.DAYS.between(minDate, maxDate).toInt() + 1
    val numMinutes = ChronoUnit.MINUTES.between(minTimes, maxTimes).toInt() + 1
    val numHours = numMinutes / 60
    val dividerColor = if (MaterialTheme.colors.isLight) Color.LightGray else Color.DarkGray
    val positionedEvents =
        remember(events) { arrangeEvents(splitEvents(events.sortedBy(Event::start))).filter { it.end > minTimes && it.start < maxTimes } }
    Layout(
        content = {
            positionedEvents.forEach { positionedEvent ->
                Box(modifier = Modifier.eventData(positionedEvent)) {
                    eventContent(positionedEvent)
                }
            }
        },
        modifier = modifier
            .drawBehind {
                val firstHour = minTimes.truncatedTo(ChronoUnit.HOURS)
                val firstHourOffsetMinutes =
                    if (firstHour == minTimes) 0 else ChronoUnit.MINUTES.between(
                        minTimes,
                        firstHour.plusHours(1)
                    )
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

//                ---------------Drawing current timeline Start--------------------------

                val currentTimeOffset = ChronoUnit.MINUTES.between(minTimes, currentTime).toFloat()
                val currentTimeY = (currentTimeOffset / 60f) * hourHeight.toPx()

                val triangleSize = 16.dp.toPx()
                val triangleHalfBase = triangleSize / 2

                val trianglePath = Path().apply {
                    moveTo(-triangleHalfBase, currentTimeY - triangleSize)
                    lineTo(triangleHalfBase, currentTimeY)
                    lineTo(-triangleHalfBase, currentTimeY + triangleSize)
                    close()
                }

                drawPath(
                    path = trianglePath,
                    color = Color.Red
                )

                drawLine(
                    color = Color.Red,
                    start = Offset(6.dp.toPx(), currentTimeY),
                    end = Offset(size.width, currentTimeY),
                    strokeWidth = 2.dp.toPx()
                )
//                ---------------Drawing current timeline End--------------------------

            }
    ) { measureables, constraints ->
        val height = (hourHeight.toPx() * (numMinutes / 60f)).roundToInt()
        val width = dayWidth.roundToPx() * numDays
        val placeablesWithEvents = measureables.map { measurable ->
            val splitEvent = measurable.parentData as PositionedEvent
            val eventDurationMinutes =
                ChronoUnit.MINUTES.between(splitEvent.start, minOf(splitEvent.end, maxTimes))
            val eventHeight = ((eventDurationMinutes / 60f) * hourHeight.toPx()).roundToInt()
            val eventWidth = dayWidth.toPx().roundToInt()
            val placeable = measurable.measure(
                constraints.copy(
                    minWidth = eventWidth,
                    maxWidth = eventWidth,
                    minHeight = eventHeight,
                    maxHeight = eventHeight
                )
            )
            Pair(placeable, splitEvent)
        }
        layout(width, height) {
            placeablesWithEvents.forEach { (placeable, splitEvent) ->
                val eventOffsetMinutes =
                    if (splitEvent.start > minTimes) ChronoUnit.MINUTES.between(
                        minTimes,
                        splitEvent.start
                    ) else 0
                val eventOffsetDays = ChronoUnit.DAYS.between(minDate, splitEvent.date).toInt()
                val eventX =
                    eventOffsetDays * dayWidth.roundToPx() + (splitEvent.col * (dayWidth.toPx() / splitEvent.colTotal.toFloat())).roundToInt()
                val eventY = ((eventOffsetMinutes / 60f) * hourHeight.toPx()).roundToInt()
                placeable.place(eventX, eventY)
            }
        }
    }
}

fun getMinMaxTimes(): Pair<LocalTime, LocalTime> {
    val minTime = LocalTime.parse("08:00", DateTimeFormatter.ofPattern("HH:mm")) ?: LocalTime.MIN
    val maxTime = LocalTime.parse("20:00", DateTimeFormatter.ofPattern("HH:mm")) ?: LocalTime.MAX
    return minTime to maxTime
}

@Preview(showBackground = true)
@Composable
fun SchedulePreview() {
    WeekScheduleTheme {
        Schedule(sampleEvents)
    }
}

object GridDimensions {
    val height = 120.dp //DaySize
    val width = 80.dp //HourSize
}

sealed class ViewType {
    object OneDayView : ViewType()
    object ThreeDayView : ViewType()
}
