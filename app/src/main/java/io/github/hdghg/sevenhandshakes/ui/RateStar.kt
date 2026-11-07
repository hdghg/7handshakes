package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.rateLevelOf

private val SilverStar = listOf(Color(0xFFE3E6EA), Color(0xFF9EA5AE))
private val GoldStar = listOf(Color(0xFFFFE27A), Color(0xFFE0A800))
private val DiamondStar = listOf(Color(0xFFE8FDFF), Color(0xFF7FDBFF), Color(0xFFB9A8FF))

/** One star whose material shows the rate: 1 silver, 2 gold, 3 diamond. */
@Composable
fun RateStar(rate: Byte, modifier: Modifier = Modifier) {
    val (colors, description) = when (rateLevelOf(rate).toInt()) {
        3 -> DiamondStar to R.string.feed_rate_diamond
        2 -> GoldStar to R.string.feed_rate_gold
        else -> SilverStar to R.string.feed_rate_silver
    }
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = stringResource(id = description),
        modifier = modifier
            // Paint a diagonal gradient over the star shape only, for a metallic / gem look.
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val brush = Brush.linearGradient(colors, start = Offset.Zero, end = Offset(size.width, size.height))
                onDrawWithContent {
                    drawContent()
                    drawRect(brush, blendMode = BlendMode.SrcAtop)
                }
            },
    )
}
