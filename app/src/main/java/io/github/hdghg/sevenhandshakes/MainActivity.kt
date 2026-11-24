package io.github.hdghg.sevenhandshakes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.IntentCompat
import androidx.fragment.app.FragmentActivity
import io.github.hdghg.sevenhandshakes.data.share.Hs7Link
import timber.log.Timber

/**
 * Main activity for the app. A FragmentActivity because the device unlock prompt needs one.
 *
 * Another app can open a YAML file with it, or share one to it (see the manifest);
 * the import screen is then shown for that file. It also opens review and contact links; see [Hs7Link].
 */
class MainActivity : FragmentActivity() {
    /** A file another app asked to open or shared, until the import screen has been opened for it. */
    private var fileToImport by mutableStateOf<Uri?>(null)

    /** A review or contact link another app asked to open, until its screen has been opened. */
    private var linkToOpen by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("MainActivity created (restored=%b)", savedInstanceState != null)
        // When the activity is recreated the same intent comes again; its screen is already on the back stack.
        if (savedInstanceState == null) open(intent)
        enableEdgeToEdge()
        setContent {
            MainTheme {
                AppMainNavGraph(
                    fileToImport = fileToImport,
                    onFileToImportOpened = { fileToImport = null },
                    linkToOpen = linkToOpen,
                    onLinkOpened = { linkToOpen = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        open(intent)
    }

    private fun open(intent: Intent?) {
        val link = intent?.data?.takeIf { intent.action == Intent.ACTION_VIEW && it.scheme == Hs7Link.SCHEME }
        if (link != null) {
            Timber.d("Asked to open a %s link", link.host)
            linkToOpen = link.toString()
        } else {
            fileToImport = fileToOpen(intent)
        }
    }

    /** The file of an "open with" or a "share" request, or null if [intent] is neither. */
    private fun fileToOpen(intent: Intent?): Uri? {
        val file = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            // Shared text has no file; nothing is opened for it.
            Intent.ACTION_SEND -> IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
            else -> return null
        }
        Timber.d("Asked to %s a file of type %s (file given: %b)", intent.action, intent.type, file != null)
        return file
    }
}
