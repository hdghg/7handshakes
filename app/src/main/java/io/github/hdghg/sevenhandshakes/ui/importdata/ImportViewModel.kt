package io.github.hdghg.sevenhandshakes.ui.importdata

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdghg.sevenhandshakes.data.YamlFormat
import io.github.hdghg.sevenhandshakes.data.readYamlFormat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.reader.UnicodeReader
import timber.log.Timber

sealed interface ImportEvent {
    /** The picked file can be imported; the screen that offers what to import should open. */
    class OpenSelection(val uri: Uri) : ImportEvent

    /** The picked file is not one the app can import. */
    data object InvalidFile : ImportEvent
}

class ImportViewModel
    constructor(
        private val context: Context,
        private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _events = Channel<ImportEvent>(Channel.BUFFERED)
        val events: Flow<ImportEvent> = _events.receiveAsFlow()

        /** Looks at the start of the picked file to tell whether the app can import it. */
        fun onFilePicked(uri: Uri) {
            viewModelScope.launch {
                val format = withContext(ioDispatcher) { formatOf(uri) }
                Timber.d("Picked file has format %s", format)
                // The other formats have no import yet.
                _events.send(
                    if (format == YamlFormat.SHARE_V1) ImportEvent.OpenSelection(uri) else ImportEvent.InvalidFile,
                )
            }
        }

        private fun formatOf(uri: Uri): YamlFormat? = try {
            context.contentResolver.openInputStream(uri)?.use { readYamlFormat(UnicodeReader(it)) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The file is gone or cannot be read.
            Timber.w(e, "Cannot read the picked file")
            null
        }
    }
