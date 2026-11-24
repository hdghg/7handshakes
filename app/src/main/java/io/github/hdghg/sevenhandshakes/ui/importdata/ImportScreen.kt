package io.github.hdghg.sevenhandshakes.ui.importdata

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.ImportTopAppBar

/** Lets the user pick a file to import. A file the app can import opens the next screen. */
@Composable
fun ImportScreen(
    openDrawer: () -> Unit,
    openImportSelection: (Uri) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = koinViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // uri is null when the user picked nothing.
        if (uri != null) viewModel.onFilePicked(uri)
    }
    val invalidFile = stringResource(id = R.string.import_invalid_file)
    val currentOpenImportSelection by rememberUpdatedState(openImportSelection)
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ImportEvent.OpenSelection -> currentOpenImportSelection(event.uri)
                ImportEvent.InvalidFile -> snackbarHostState.showSnackbar(invalidFile)
            }
        }
    }
    ImportContent(
        openDrawer = openDrawer,
        // Android has no dependable MIME type for YAML, so any file can be picked.
        import = { pickFile.launch(arrayOf("*/*")) },
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun ImportContent(
    openDrawer: () -> Unit,
    import: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ImportTopAppBar(openDrawer) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.horizontal_margin)),
        ) {
            Button(onClick = import, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.import_action))
            }
        }
    }
}

@Preview
@Composable
private fun ImportScreenPreview() {
    Surface {
        ImportContent(openDrawer = {}, import = {})
    }
}
