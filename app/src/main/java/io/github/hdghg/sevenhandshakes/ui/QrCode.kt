package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import kotlin.math.floor

/** Blank modules around the code that scanners need, per the QR specification. */
private const val QUIET_ZONE_MODULES = 4

/**
 * Draws [content] as a square QR code in [color] on white, whatever the theme, so it stays scannable.
 * [color] must be dark enough to scan on white.
 */
@Composable
fun QrCode(
    content: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Black,
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
