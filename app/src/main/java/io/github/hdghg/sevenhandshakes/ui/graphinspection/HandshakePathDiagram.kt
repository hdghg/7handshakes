package io.github.hdghg.sevenhandshakes.ui.graphinspection

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.hdghg.sevenhandshakes.data.HandshakePath
import io.github.hdghg.sevenhandshakes.ui.rateStarColor

private val VertexHeight = 20.dp
private val EdgeHeight = 22.dp
private val WeightWidth = 24.dp
private val RailWidth = 14.dp
private val LineStroke = 2.dp
private val MissingLineStroke = 0.5.dp

/**
 * Vertical chain of contacts for one rate level, read top to bottom in review direction:
 * the end contact at the top, the [received] path down to the middle contact, then the [given] path
 * down to the end contact again at the bottom. Each edge shows how many reviews it stands for;
 * the weakest edge, which sets the path's total, is highlighted. A missing half is drawn as
 * one thin line of weight 0.
 *
 * Each edge is drawn in the star color of the best rate level counted in its weight.
 *
 * @param lineColor color of the lines, where a path does not say its levels or is missing: the star color of the rate level.
 * @param handshakes edges per path, so a missing half keeps the middle contact in the middle.
 * @param middleLabel name shown for the middle contact.
 * @param labelOf name shown for a contact id on the path.
 */
@Composable
internal fun HandshakePathDiagram(
    received: HandshakePath?,
    given: HandshakePath?,
    handshakes: Int,
    lineColor: Color,
    middleLabel: String,
    labelOf: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
) {
    fun HandshakePath.edgeColor(edge: Int): Color = levels.getOrNull(edge)?.let(::rateStarColor) ?: lineColor
    Column(modifier = modifier.fillMaxWidth()) {
        if (received == null) {
            MissingHalf(handshakes, lineColor)
        } else {
            received.weights.forEachIndexed { i, weight ->
                val id = received.contactIds[i]
                Vertex(
                    label = labelOf(id),
                    kind = if (i == 0) VertexKind.End else VertexKind.Middle,
                    colorAbove = received.edgeColor(maxOf(i - 1, 0)),
                    colorBelow = received.edgeColor(i),
                    strokeAbove = if (i > 0) LineStroke else null,
                    strokeBelow = LineStroke,
                )
                Edge(weight = weight, weakest = weight == received.weight, lineColor = received.edgeColor(i))
            }
        }
        Vertex(
            label = middleLabel,
            kind = VertexKind.Center,
            colorAbove = received?.run { edgeColor(weights.lastIndex) } ?: lineColor,
            colorBelow = given?.edgeColor(0) ?: lineColor,
            strokeAbove = if (received != null) LineStroke else MissingLineStroke,
            strokeBelow = if (given != null) LineStroke else MissingLineStroke,
        )
        if (given == null) {
            MissingHalf(handshakes, lineColor)
        } else {
            given.weights.forEachIndexed { i, weight ->
                val id = given.contactIds[i + 1]
                Edge(weight = weight, weakest = weight == given.weight, lineColor = given.edgeColor(i))
                Vertex(
                    label = labelOf(id),
                    kind = if (i == given.weights.lastIndex) VertexKind.End else VertexKind.Middle,
                    colorAbove = given.edgeColor(i),
                    colorBelow = given.edgeColor(minOf(i + 1, given.weights.lastIndex)),
                    strokeAbove = LineStroke,
                    strokeBelow = if (i < given.weights.lastIndex) LineStroke else null,
                )
            }
        }
    }
}

private enum class VertexKind { Center, End, Middle }

/** @param strokeAbove width of the line entering the dot from above, null for none; same for [strokeBelow]. */
@Composable
private fun Vertex(
    label: String,
    kind: VertexKind,
    colorAbove: Color,
    colorBelow: Color,
    strokeAbove: Dp?,
    strokeBelow: Dp?,
) {
    val dotColor = when (kind) {
        VertexKind.Center -> MaterialTheme.colorScheme.primary
        VertexKind.End -> MaterialTheme.colorScheme.onSurface
        VertexKind.Middle -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(VertexHeight)) {
        Box(modifier = Modifier.width(WeightWidth))
        Canvas(modifier = Modifier.width(RailWidth).fillMaxHeight()) {
            val x = size.width / 2
            val y = size.height / 2
            strokeAbove?.let { drawLine(colorAbove, Offset(x, 0f), Offset(x, y), it.toPx()) }
            strokeBelow?.let { drawLine(colorBelow, Offset(x, y), Offset(x, size.height), it.toPx()) }
            drawCircle(dotColor, radius = (if (kind == VertexKind.Center) 5 else 4).dp.toPx(), center = Offset(x, y))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (kind == VertexKind.Center) FontWeight.Bold else FontWeight.Normal,
            color = if (kind == VertexKind.Middle) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

/** A downward edge with its weight on the left. */
@Composable
private fun Edge(weight: Int, weakest: Boolean, lineColor: Color) {
    val weightColor = if (weakest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(EdgeHeight)) {
        Text(
            text = weight.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (weakest) FontWeight.Bold else FontWeight.Normal,
            color = weightColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(WeightWidth),
        )
        Canvas(modifier = Modifier.width(RailWidth).fillMaxHeight()) {
            val x = size.width / 2
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), LineStroke.toPx())
            drawArrowHead(x, size.height / 2 + 3.dp.toPx(), 3.dp.toPx(), if (weakest) weightColor else lineColor)
        }
    }
}

private fun DrawScope.drawArrowHead(x: Float, tipY: Float, half: Float, color: Color) {
    val path = Path().apply {
        moveTo(x - half, tipY - half * 1.5f)
        lineTo(x + half, tipY - half * 1.5f)
        lineTo(x, tipY)
        close()
    }
    drawPath(path, color)
}

/**
 * A half with no path: one thin line of weight 0. Takes the space of a whole half path,
 * so the middle contact stays in the middle of the column.
 */
@Composable
private fun MissingHalf(handshakes: Int, lineColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height((VertexHeight + EdgeHeight) * handshakes),
    ) {
        Text(
            text = "0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(WeightWidth),
        )
        Canvas(modifier = Modifier.width(RailWidth).fillMaxHeight()) {
            val x = size.width / 2
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), MissingLineStroke.toPx())
        }
    }
}
