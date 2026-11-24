package io.github.hdghg.sevenhandshakes.ui.importselection

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs
import io.github.hdghg.sevenhandshakes.data.share.ShareImportResult
import io.github.hdghg.sevenhandshakes.data.share.ShareImporter
import io.github.hdghg.sevenhandshakes.data.share.ShareSection
import io.github.hdghg.sevenhandshakes.data.share.readShareSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.yaml.snakeyaml.reader.UnicodeReader
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.FilterInputStream
import java.io.InputStream

/** What the screen shows about the picked file. */
data class ImportFileInfo(
    /** The file's name, or null if its provider does not tell. */
    val name: String?,
    val sizeBytes: Long,
    val contacts: Int,
    val reviews: Int,
)

data class ImportSelectionUiState(
    /** True while the file is being read for [file]. */
    val isLoading: Boolean = true,
    /** Null while loading, and when the file turned out not to be importable. */
    val file: ImportFileInfo? = null,
    val isImporting: Boolean = false,
    /** Outcome of the last import, or null if nothing was imported yet. */
    val result: ShareImportResult? = null,
    /** True when the last import stopped with an error. */
    val importFailed: Boolean = false,
) {
    val isFileInvalid: Boolean get() = !isLoading && file == null
}

class ImportSelectionViewModel
    constructor(
        private val importer: ShareImporter,
        private val context: Context,
        private val ioDispatcher: CoroutineDispatcher,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val uri: Uri = Uri.parse(checkNotNull(savedStateHandle.get<String>(MyAppDestinationsArgs.FILE_URI_ARG)))

        private val _uiState = MutableStateFlow(ImportSelectionUiState())
        val uiState: StateFlow<ImportSelectionUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val file = try {
                    withContext(ioDispatcher) { readFileInfo() }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.w(e, "Cannot read the file to import")
                    null
                }
                _uiState.update { it.copy(isLoading = false, file = file) }
            }
        }

        fun importContacts() = import(setOf(ShareSection.CONTACTS))

        fun importReviews() = import(setOf(ShareSection.REVIEWS))

        fun importAll() = import(setOf(ShareSection.CONTACTS, ShareSection.REVIEWS))

        private fun import(sections: Set<ShareSection>) {
            val state = _uiState.value
            if (state.isImporting || state.file == null) return
            _uiState.update { it.copy(isImporting = true, importFailed = false, result = null) }
            viewModelScope.launch {
                val result = try {
                    withContext(ioDispatcher) { importer.import(sections) { UnicodeReader(openFile()) } }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "Import failed")
                    null
                }
                Timber.d("Import of %s finished: %s", sections, result)
                _uiState.update { it.copy(isImporting = false, result = result, importFailed = result == null) }
            }
        }

        /** Reads the whole file once, counting its entries and its bytes. */
        private fun readFileInfo(): ImportFileInfo {
            val counted = CountingInputStream(openFile())
            val summary = counted.use { readShareSummary(UnicodeReader(it)) }
            return ImportFileInfo(
                name = fileName(),
                sizeBytes = counted.count,
                contacts = summary.contacts,
                reviews = summary.reviews,
            )
        }

        private fun openFile(): InputStream =
            context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException("Cannot open $uri")

        private fun fileName(): String? =
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst() && !cursor.isNull(0)) cursor.getString(0) else null
            }
    }

/** Counts the bytes read through it. */
private class CountingInputStream(input: InputStream) : FilterInputStream(input) {
    var count = 0L
        private set

    override fun read(): Int = super.read().also { if (it != -1) count++ }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int =
        super.read(buffer, offset, length).also { if (it > 0) count += it }
}
