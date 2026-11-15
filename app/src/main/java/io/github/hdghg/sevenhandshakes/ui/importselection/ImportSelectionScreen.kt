package io.github.hdghg.sevenhandshakes.ui.importselection

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.share.ContactsImportResult
import io.github.hdghg.sevenhandshakes.data.share.ReviewsImportResult
import io.github.hdghg.sevenhandshakes.data.share.ShareImportResult
import io.github.hdghg.sevenhandshakes.ui.ImportSelectionTopAppBar

/** Shows what the picked share file holds and lets the user choose what to import from it. */
@Composable
fun ImportSelectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportSelectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ImportSelectionContent(
        uiState = uiState,
        onBack = onBack,
        importContacts = viewModel::importContacts,
        importReviews = viewModel::importReviews,
        importAll = viewModel::importAll,
        modifier = modifier,
    )
}

@Composable
private fun ImportSelectionContent(
    uiState: ImportSelectionUiState,
    onBack: () -> Unit,
    importContacts: () -> Unit,
    importReviews: () -> Unit,
    importAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ImportSelectionTopAppBar(onBack) },
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(margin / 2),
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(margin),
        ) {
            val file = uiState.file
            when {
                uiState.isLoading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                file == null -> Text(
                    text = stringResource(id = R.string.import_invalid_file),
                    color = MaterialTheme.colorScheme.error,
                )
                else -> {
                    FileInfo(file)
                    val enabled = !uiState.isImporting
                    Button(onClick = importContacts, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.import_contacts))
                    }
                    Button(onClick = importReviews, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.import_reviews))
                    }
                    Button(onClick = importAll, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(id = R.string.import_all))
                    }
                    if (uiState.isImporting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    if (uiState.importFailed) {
                        Text(
                            text = stringResource(id = R.string.import_failed),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    uiState.result?.let { ImportResult(it) }
                }
            }
        }
    }
}

@Composable
private fun FileInfo(file: ImportFileInfo) {
    val context = LocalContext.current
    Column {
        if (file.name != null) {
            Text(text = file.name, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = stringResource(
                id = R.string.import_file_size,
                Formatter.formatShortFileSize(context, file.sizeBytes),
            ),
        )
        Text(text = stringResource(id = R.string.import_file_contacts, file.contacts))
        Text(text = stringResource(id = R.string.import_file_reviews, file.reviews))
    }
}

@Composable
private fun ImportResult(result: ShareImportResult) {
    Column {
        result.contacts?.let {
            Text(
                text = stringResource(
                    id = R.string.import_result_contacts,
                    it.added,
                    it.updated,
                    it.unchanged,
                    it.rejected,
                ),
            )
        }
        result.reviews?.let {
            Text(text = stringResource(id = R.string.import_result_reviews, it.added, it.alreadyPresent, it.rejected))
        }
    }
}

@Preview
@Composable
private fun ImportSelectionScreenPreview() {
    Surface {
        ImportSelectionContent(
            uiState = ImportSelectionUiState(
                isLoading = false,
                file = ImportFileInfo(
                    name = "sevenhandshakes-export.yaml",
                    sizeBytes = 48_213,
                    contacts = 12,
                    reviews = 140,
                ),
                result = ShareImportResult(
                    contacts = ContactsImportResult(added = 9, updated = 1, unchanged = 2),
                    reviews = ReviewsImportResult(added = 131, alreadyPresent = 6, rejected = 3),
                ),
            ),
            onBack = {},
            importContacts = {},
            importReviews = {},
            importAll = {},
        )
    }
}
