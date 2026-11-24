package io.github.hdghg.sevenhandshakes.ui.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdghg.sevenhandshakes.data.ContactRepository
import io.github.hdghg.sevenhandshakes.data.ReviewRepository
import io.github.hdghg.sevenhandshakes.data.writeShareYaml
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

data class ExportUiState(
    val isExporting: Boolean = false,
)

sealed interface ExportEvent {
    /** The export file is written; [uri] can be handed to another app. */
    class Share(val uri: Uri) : ExportEvent

    data object Failed : ExportEvent
}

class ExportViewModel
    constructor(
        private val contactRepository: ContactRepository,
        private val reviewRepository: ReviewRepository,
        private val context: Context,
        private val ioDispatcher: CoroutineDispatcher,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ExportUiState())
        val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

        private val _events = Channel<ExportEvent>(Channel.BUFFERED)
        val events: Flow<ExportEvent> = _events.receiveAsFlow()

        /** Writes every contact and review to a YAML file and asks the screen to share it. */
        fun exportAll() {
            if (_uiState.value.isExporting) return
            _uiState.update { it.copy(isExporting = true) }
            viewModelScope.launch {
                val event = try {
                    ExportEvent.Share(writeExportFile())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Export failed")
                    ExportEvent.Failed
                } finally {
                    _uiState.update { it.copy(isExporting = false) }
                }
                _events.send(event)
            }
        }

        private suspend fun writeExportFile(): Uri {
            val contacts = contactRepository.getContactsStream().first()
            val reviews = reviewRepository.getGratitudesStream().first()
            return withContext(ioDispatcher) {
                // Must match the directory shared in res/xml/file_paths.xml.
                val directory = File(context.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
                val file = File(directory, EXPORT_FILE_NAME)
                file.bufferedWriter().use { writeShareYaml(contacts, reviews, it) }
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        }

        private companion object {
            const val EXPORT_DIRECTORY = "exports"
            const val EXPORT_FILE_NAME = "sevenhandshakes-export.yaml"
        }
    }
