package io.github.hdghg.sevenhandshakes.ui.identities

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.github.hdghg.sevenhandshakes.R
import kotlin.math.floor

/** Red marks a private key QR code, so it is not mistaken for a public one. Dark enough to scan on white. */
private val PrivateKeyQrColor = Color(0xFFD50000)

/** Blank modules around the code that scanners need, per the QR specification. */
private const val QUIET_ZONE_MODULES = 4

/**
 * Shows [privateKey] as a red QR code to scan on another device.
 * The caller must verify the user before showing this. Screenshots and screen recording are blocked.
 */
@Composable
fun PrivateKeyQrDialog(
    name: String,
    privateKey: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column {
                QrCode(
                    content = privateKey,
                    color = PrivateKeyQrColor,
                    contentDescription = stringResource(id = R.string.identity_private_key),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(id = R.string.identity_qr_warning),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.close))
            }
        },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
    )
}

/**
 * Draws [content] as a square QR code in [color] on white, whatever the theme, so it stays scannable.
 */
@Composable
private fun QrCode(
    content: String,
    color: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val matrix = remember(content) {
        val hints = mapOf(
            EncodeHintType.MARGIN to QUIET_ZONE_MODULES,
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
        )
        // Size 0 gives one matrix cell per module, quiet zone included.
        QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
    }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color.White)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val cell = size.minDimension / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (!matrix[x, y]) continue
                // Snap to whole pixels so neighbouring modules leave no hairline gaps.
                val left = floor(x * cell)
                val top = floor(y * cell)
                drawRect(
                    color = color,
                    topLeft = Offset(left, top),
                    size = Size(floor((x + 1) * cell) - left, floor((y + 1) * cell) - top),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PrivateKeyQrDialogPreview() {
    Surface {
        PrivateKeyQrDialog(
            name = "Me",
            privateKey = "MC4CAQAwBQYDK2VwBCIEINTuctv5E1hK1bbY8fdp+K06/nwoy/HU++CXqI9EdVhC",
            onDismiss = {},
        )
    }
}
