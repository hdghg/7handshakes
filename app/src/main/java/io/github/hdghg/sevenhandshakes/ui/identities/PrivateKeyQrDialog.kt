package io.github.hdghg.sevenhandshakes.ui.identities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.QrCode

/** Red marks a private key QR code, so it is not mistaken for a public one. Dark enough to scan on white. */
private val PrivateKeyQrColor = Color(0xFFD50000)

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
