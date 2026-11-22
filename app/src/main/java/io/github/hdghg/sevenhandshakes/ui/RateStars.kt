package io.github.hdghg.sevenhandshakes.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.rateLevelOf

private val GoldGradient = listOf(Color(0xFFFFE27A), Color(0xFFE0A800))

/** Flat gold matching the stars, for lines and other marks drawn next to them. */
val GoldStarColor = GoldGradient[1]

/** "2 stars: usual gratitude" and so on, for a review of [rate] stars. */
@StringRes
fun rateDescriptionOf(rate: Byte): Int = when (rateLevelOf(rate).toInt()) {
    3 -> R.string.rate_stars_3
    2 -> R.string.rate_stars_2
    else -> R.string.rate_stars_1
}

/** One golden star. */
@Composable
fun GoldStar(modifier: Modifier = Modifier, contentDescription: String? = null) {
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = contentDescription,
        modifier = modifier
            // Paint a diagonal gradient over the star shape only, for a metallic look.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val brush = Brush.linearGradient(GoldGradient, start = Offset.Zero, end = Offset(size.width, size.height))
                onDrawWithContent {
                    drawContent()
                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                }
            },
    )
}

/**
 * As many golden stars as the review of [rate] gives, read out as one description.
 * 1 and 2 stars stand in a row; 3 stars form a pyramid, one on top of two, so they take
 * no more width than 2.
 *
 * @param reserveSpace takes the width of 2 stars whatever [rate] is, so that rows of a list line up.
 */
@Composable
fun RateStars(rate: Byte, modifier: Modifier = Modifier, starSize: Dp = 24.dp, reserveSpace: Boolean = false) {
    val description = stringResource(id = rateDescriptionOf(rate))
    val stars = rateLevelOf(rate).toInt()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (reserveSpace) Modifier.width(starSize * 2) else Modifier)
            .semantics { contentDescription = description },
    ) {
        if (stars < 3) {
            Row {
                repeat(stars) { GoldStar(modifier = Modifier.size(starSize)) }
            }
        } else {
            // The rows overlap a little: the star shape leaves empty space at the top and bottom of its box.
            Box(modifier = Modifier.size(width = starSize * 2, height = starSize * PYRAMID_HEIGHT)) {
                GoldStar(modifier = Modifier.size(starSize).align(Alignment.TopCenter))
                Row(modifier = Modifier.align(Alignment.BottomCenter)) {
                    repeat(2) { GoldStar(modifier = Modifier.size(starSize)) }
                }
            }
        }
    }
}

/** Height of the 3 star pyramid, in star sizes. */
private const val PYRAMID_HEIGHT = 1.8f
