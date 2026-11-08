package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.MAX_HANDSHAKE_DEPTH
import kotlin.math.roundToInt

/**
 * Bottom sheet with a slider for how many handshake levels to calculate, 0..[MAX_HANDSHAKE_DEPTH].
 * [onDepthChange] is called when the user lets go of the slider, not on every step of a drag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HandshakeDepthSheet(depth: Int, onDepthChange: (Int) -> Unit, onDismiss: () -> Unit) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    var value by remember(depth) { mutableFloatStateOf(depth.toFloat()) }
    val selected = value.roundToInt()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = margin, end = margin, bottom = margin * 2),
        ) {
            Text(
                text = stringResource(id = R.string.handshake_depth_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (selected == 0) {
                    stringResource(id = R.string.handshake_depth_off)
                } else {
                    pluralStringResource(id = R.plurals.handshake_depth_value, count = selected, selected)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = margin / 2),
            )
            Slider(
                value = value,
                onValueChange = { value = it },
                onValueChangeFinished = { onDepthChange(value.roundToInt()) },
                valueRange = 0f..MAX_HANDSHAKE_DEPTH.toFloat(),
                // Stops between the ends, so every integer is a stop.
                steps = MAX_HANDSHAKE_DEPTH - 1,
            )
            Text(
                text = stringResource(id = R.string.handshake_depth_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
