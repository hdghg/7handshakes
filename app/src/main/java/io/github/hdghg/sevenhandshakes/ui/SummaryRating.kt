package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.ReviewExchange

private val OrangeOnLight = Color(0xFFE65100)
private val OrangeOnDark = Color(0xFFFFA726)

/**
 * Stars received and given, summed over all handshake [ratings] calculated so far:
 * always two big golden stars, the first with the stars received below it, the second with the stars given.
 *
 * @param showCaptions also explains the difference in words under the stars.
 * @param middleIsMe the middle contact is the selected identity, so the caption speaks to "you".
 * @param isCalculating more levels are still on their way, so the sums can still grow.
 */
@Composable
fun SummaryRating(
    ratings: List<ReviewExchange>,
    showCaptions: Boolean,
    isCalculating: Boolean,
    modifier: Modifier = Modifier,
    middleIsMe: Boolean = false,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    val received = ratings.sumOf { it.received }
    val given = ratings.sumOf { it.given }
    Column(modifier = modifier.fillMaxWidth().padding(vertical = margin / 2)) {
        // Twice the size of the per-handshake stars.
        ReceivedGivenStars(received = received, given = given, starSize = 64.dp, modifier = Modifier.fillMaxWidth())
        if (showCaptions) {
            val difference = received - given
            Text(
                text = stringResource(
                    id = when {
                        difference > 0 && middleIsMe -> R.string.summary_rating_me_receives_more
                        difference < 0 && middleIsMe -> R.string.summary_rating_me_gives_more
                        difference > 0 -> R.string.summary_rating_receives_more
                        difference < 0 -> R.string.summary_rating_gives_more
                        else -> R.string.summary_rating_balanced
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = margin / 2),
            )
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

/**
 * Two golden stars side by side: the first with [received] below it, the second with [given].
 * Received is in [MaterialTheme] secondary, given in orange, so giving does not read as an error.
 */
@Composable
fun ReceivedGivenStars(received: Int, given: Int, starSize: Dp, modifier: Modifier = Modifier) {
    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = modifier) {
        StarColumn(
            value = received,
            caption = stringResource(id = R.string.summary_rating_received),
            color = MaterialTheme.colorScheme.secondary,
            starSize = starSize,
        )
        StarColumn(
            value = given,
            caption = stringResource(id = R.string.summary_rating_given),
            color = givesMoreColor(),
            starSize = starSize,
        )
    }
}

@Composable
private fun StarColumn(value: Int, caption: String, color: Color, starSize: Dp) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        GoldStar(modifier = Modifier.size(starSize))
        Text(
            text = value.toString(),
            style = if (starSize >= 48.dp) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
            color = color,
        )
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Orange that stays readable on the current background. */
@Composable
private fun givesMoreColor(): Color =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) OrangeOnDark else OrangeOnLight
