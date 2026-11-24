package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.hdghg.sevenhandshakes.data.ReviewDirection

/**
 * Background for a review given or received: orange for [ReviewDirection.Outgoing],
 * green for [ReviewDirection.Incoming], none for null. Soft enough for the stars and text to stay readable.
 */
@Composable
fun reviewDirectionColor(direction: ReviewDirection?): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return when (direction) {
        ReviewDirection.Outgoing -> if (dark) OutgoingOnDark else OutgoingOnLight
        ReviewDirection.Incoming -> if (dark) IncomingOnDark else IncomingOnLight
        null -> Color.Transparent
    }
}

private val OutgoingOnLight = Color(0xFFFFE8CC)
private val OutgoingOnDark = Color(0xFF4A3216)
private val IncomingOnLight = Color(0xFFDCEFDC)
private val IncomingOnDark = Color(0xFF1F3B22)
