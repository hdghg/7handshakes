package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow

/**
 * A button placed under a text field that fills the field from another source, such as a QR scan.
 *
 * Medium emphasis (tonal) with a leading icon, because the screen's main action is Save in the top bar.
 */
@Composable
fun FieldSourceButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilledTonalButton(
        onClick = onClick,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        modifier = modifier,
    ) {
        Icon(
            imageVector = icon,
            // The text beside the icon already names the action.
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(modifier = Modifier.size(ButtonDefaults.IconSpacing))
        Text(text = text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
