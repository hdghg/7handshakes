package io.github.hdghg.sevenhandshakes.ui.contactdetail

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.HandshakePath

private val VertexHeight = 20.dp
private val EdgeHeight = 22.dp
private val WeightWidth = 24.dp
private val RailWidth = 14.dp

/**
 * Vertical chain of contacts for one rate level, read top to bottom in review direction:
 * the contact at the top, the [received] path down to you in the middle, then the [given] path
 * down to the contact again at the bottom. Each edge shows how many reviews it stands for;
 * the weakest edge, which sets the path's total, is highlighted.
 *
 * @param handshakes edges per path, so a missing half keeps you in the middle.
 * @param labelOf name shown for a contact id on the path.
 */
@Composable
internal fun HandshakePathDiagram(
    received: HandshakePath?,
    given: HandshakePath?,
    handshakes: Int,
    labelOf: @Composable (Int) -> String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (received == null) {
            MissingHalf(handshakes)
        } else {
            received.weights.forEachIndexed { i, weight ->
                val id = received.contactIds[i]
                Vertex(
                    label = labelOf(id),
                    kind = if (i == 0) VertexKind.End else VertexKind.Middle,
                    lineAbove = i > 0,
                    lineBelow = true,
                )
                Edge(weight = weight, weakest = weight == received.weight)
            }
        }
        Vertex(
            label = stringResource(id = R.string.handshake_path_me),
            kind = VertexKind.Me,
            lineAbove = received != null,
            lineBelow = given != null,
        )
        if (given == null) {
            MissingHalf(handshakes)
        } else {
            given.weights.forEachIndexed { i, weight ->
                val id = given.contactIds[i + 1]
                Edge(weight = weight, weakest = weight == given.weight)
                Vertex(
                    label = labelOf(id),
                    kind = if (i == given.weights.lastIndex) VertexKind.End else VertexKind.Middle,
                    lineAbove = true,
                    lineBelow = i < given.weights.lastIndex,
                )
            }
        }
    }
}

private enum class VertexKind { Me, End, Middle }

@Composable
private fun Vertex(label: String, kind: VertexKind, lineAbove: Boolean, lineBelow: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = when (kind) {
        VertexKind.Me -> MaterialTheme.colorScheme.primary
        VertexKind.End -> MaterialTheme.colorScheme.onSurface
        VertexKind.Middle -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(VertexHeight)) {
        Box(modifier = Modifier.width(WeightWidth))
        Canvas(modifier = Modifier.width(RailWidth).fillMaxHeight()) {
            val x = size.width / 2
            val y = size.height / 2
            val stroke = 2.dp.toPx()
            if (lineAbove) drawLine(lineColor, Offset(x, 0f), Offset(x, y), stroke)
            if (lineBelow) drawLine(lineColor, Offset(x, y), Offset(x, size.height), stroke)
            drawCircle(dotColor, radius = (if (kind == VertexKind.Me) 5 else 4).dp.toPx(), center = Offset(x, y))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (kind == VertexKind.Me) FontWeight.Bold else FontWeight.Normal,
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
private fun Edge(weight: Int, weakest: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
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
            val stroke = 2.dp.toPx()
            drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), stroke)
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

/** Takes the space of a whole half path, so you stay in the middle of the column. */
@Composable
private fun MissingHalf(handshakes: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height((VertexHeight + EdgeHeight) * handshakes),
    ) {
        Text(
            text = stringResource(id = R.string.handshake_path_none),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
