package io.github.hdghg.sevenhandshakes.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.TextUtils
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.hdghg.sevenhandshakes.R
import timber.log.Timber

/** The ways to share what a screen shows; see [ShareMenuButton]. */
class ShareActions(
    /** Shows a QR code of the link. */
    val asQr: () -> Unit,
    /** Sends the link as text. */
    val asLink: () -> Unit,
    /** Copies the link to the clipboard as formatted text: a few words that open the link when tapped. */
    val asRichText: () -> Unit,
    /** Sends a file. */
    val asFile: () -> Unit,
)

/** Not implemented yet: does nothing but write to the log. */
fun shareAsFile() {
    // TODO: write the review or contact to a share/v1 YAML file and open the share menu for it.
    Timber.d("Sharing as a file is not implemented yet")
}

/** Opens the system share menu for [link], sent as plain text. */
fun shareLinkAsText(context: Context, link: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app to share a link with")
    }
}

/**
 * Copies [link] to the clipboard as formatted text, to paste into a message: [label] as a link to tap.
 *
 * Not every app pastes formatted text. One that does not gets [label] with [link] on the line below it.
 */
fun copyLinkAsRichText(context: Context, link: String, label: String) {
    val plain = "$label\n$link"
    // The link holds only URL-safe characters, so it needs no escaping.
    val html = "<a href=\"$link\">${TextUtils.htmlEncode(label)}</a>"
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newHtmlText(label, plain, html))
    // From Android 13 the system itself shows what was copied.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Shows [link] as a QR code to scan on another device; it opens there as the link itself would.
 * [qrDescription] tells a screen reader what the code holds.
 */
@Composable
fun LinkQrDialog(title: String, link: String, qrDescription: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            QrCode(
                content = link,
                contentDescription = qrDescription,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.close)) }
        },
    )
}
