package io.github.hdghg.sevenhandshakes.ui.feed

import android.content.Context
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.displayNameOrUnknown
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.ui.FeedTopAppBar
import io.github.hdghg.sevenhandshakes.ui.RateStar
import io.github.hdghg.sevenhandshakes.util.UuidV7
import java.util.Calendar
import java.util.Date

@Composable
fun FeedScreen(
    openDrawer: () -> Unit,
    newGratitude: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeedContent(
        reviews = uiState.items,
        showAll = uiState.showAll,
        onShowAllChange = viewModel::setShowAll,
        openDrawer = openDrawer,
        newGratitude = newGratitude,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** Reviews created on one local day. [dayStart] is null for reviews whose uuid has no timestamp. */
private class FeedDay(val dayStart: Long?, val reviews: List<Review>)

/** Splits [reviews] by local calendar day, keeping their order. */
private fun groupByDay(reviews: List<Review>): List<FeedDay> {
    val calendar = Calendar.getInstance()
    val byDay = reviews.groupBy { review ->
        review.createdAt?.let { startOfDay(calendar, it) }
    }
    // Reviews without a date go last.
    val dated = byDay.filterKeys { it != null }.map { (day, items) -> FeedDay(day, items) }
    val undated = byDay[null]?.let { listOf(FeedDay(null, it)) }.orEmpty()
    return dated + undated
}

private fun startOfDay(calendar: Calendar, millis: Long): Long {
    calendar.timeInMillis = millis
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}

/** "Today", "Yesterday", or the date written the way the device's region prefers. */
private fun dayLabel(context: Context, dayStart: Long?): String {
    if (dayStart == null) return context.getString(R.string.feed_unknown_date)
    if (DateUtils.isToday(dayStart)) return context.getString(R.string.feed_today)
    if (DateUtils.isToday(dayStart + DateUtils.DAY_IN_MILLIS)) return context.getString(R.string.feed_yesterday)
    return DateUtils.formatDateTime(
        context,
        dayStart,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_SHOW_YEAR,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedContent(
    reviews: List<Review>,
    showAll: Boolean,
    onShowAllChange: (Boolean) -> Unit,
    openDrawer: () -> Unit,
    newGratitude: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { FeedTopAppBar(openDrawer, showAll, onShowAllChange) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { newGratitude() }) {
                Icon(Icons.Filled.Add, stringResource(id = R.string.new_gratitude))
            }
        },
    ) { paddingValues ->
        val days = remember(reviews) { groupByDay(reviews) }
        LazyColumn(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.Center,
        ) {
            days.forEach { day ->
                stickyHeader(key = "day-${day.dayStart}") {
                    DayHeader(day.dayStart)
                }
                items(day.reviews, key = { it.id }) { review ->
                    FeedItem(review)
                }
            }
        }
    }
}

@Composable
private fun DayHeader(dayStart: Long?) {
    val context = LocalContext.current
    Text(
        text = dayLabel(context, dayStart),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(
                horizontal = dimensionResource(id = R.dimen.horizontal_margin),
                vertical = dimensionResource(id = R.dimen.horizontal_margin) / 2,
            ),
    )
}

@Composable
private fun FeedItem(review: Review) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    val context = LocalContext.current
    // Follows the device's locale and its 12/24-hour setting.
    val time = review.createdAt?.let { DateFormat.getTimeFormat(context).format(Date(it)) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = margin, vertical = margin / 2),
    ) {
        RateStar(rate = review.rate, modifier = Modifier.padding(end = margin))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = review.text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(
                    id = R.string.feed_from_to,
                    displayNameOrUnknown(review.sourceName),
                    displayNameOrUnknown(review.targetName),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (time != null) {
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = margin),
            )
        }
    }
}

@Preview
@Composable
fun FeedScreenPreview() {
    Surface {
        FeedContent(
            reviews =
                listOf(
                    previewReview(1, "Bra jobbat", 3, "Alice", hoursAgo = 1),
                    previewReview(2, "Other text", 2, "Bob", hoursAgo = 26),
                    previewReview(3, "kul", 1, null, hoursAgo = 80),
                ),
            showAll = false,
            onShowAllChange = {},
            openDrawer = {},
            newGratitude = {},
        )
    }
}

private fun previewReview(id: Int, text: String, rate: Byte, targetName: String?, hoursAgo: Int): Review {
    val createdAt = System.currentTimeMillis() - hoursAgo * DateUtils.HOUR_IN_MILLIS
    return Review(
        id = id,
        uuid = UuidV7.generate(createdAt).toString(),
        text = text,
        rate = rate,
        sourceName = "Me",
        targetName = targetName,
        createdAt = createdAt,
    )
}
