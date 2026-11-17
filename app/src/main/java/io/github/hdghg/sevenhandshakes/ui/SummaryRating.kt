package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.RATE_LEVELS
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import kotlin.math.absoluteValue

private val OrangeOnLight = Color(0xFFE65100)
private val OrangeOnDark = Color(0xFFFFA726)

/**
 * Received minus given for each rate level, summed over all handshake [ratings] calculated so far:
 * one big star per level, diamond first, with the size of the difference below it.
 *
 * The number is shown without its sign. Its color says the direction: [MaterialTheme] secondary when
 * the middle contact receives more, orange when it gives more, so giving more does not read as an error.
 *
 * @param showCaptions also explains the direction in words under each star.
 * @param isCalculating more levels are still on their way, so the sums can still grow.
 */
@Composable
fun SummaryRating(
    ratings: List<ReviewExchange>,
    showCaptions: Boolean,
    isCalculating: Boolean,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Column(modifier = modifier.fillMaxWidth().padding(vertical = margin / 2)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            RATE_LEVELS.sortedDescending().forEach { level ->
                SummaryColumn(
                    difference = ratings.sumOf { it.differenceOf(level) },
                    level = level,
                    showCaption = showCaptions,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (isCalculating) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = margin / 2),
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Text(
                    text = stringResource(id = R.string.handshake_rating_calculating),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = margin / 2),
                )
            }
        }
    }
}

@Composable
private fun SummaryColumn(difference: Int, level: Byte, showCaption: Boolean, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = 4.dp)) {
        // Twice the size of the per-handshake stars.
        RateStar(rate = level, modifier = Modifier.size(64.dp))
        Text(
            text = difference.absoluteValue.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = when {
                difference > 0 -> MaterialTheme.colorScheme.secondary
                difference < 0 -> givesMoreColor()
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        if (showCaption) {
            Text(
                text = stringResource(
                    id = when {
                        difference > 0 -> R.string.summary_rating_receives_more
                        difference < 0 -> R.string.summary_rating_gives_more
                        else -> R.string.summary_rating_balanced
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Orange that stays readable on the current background. */
@Composable
private fun givesMoreColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) OrangeOnDark else OrangeOnLight
