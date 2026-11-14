package io.github.hdghg.sevenhandshakes.ui.export

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.ExportTopAppBar

/** Exports the contacts and reviews tables as a YAML file through the system share menu. */
@Composable
fun ExportScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExportViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val exportFailed = stringResource(id = R.string.export_failed)
    LaunchedEffect(viewModel, context) {
        viewModel.events.collect { event ->
            val shared = when (event) {
                is ExportEvent.Share -> shareFile(context, event.uri)
                ExportEvent.Failed -> false
            }
            if (!shared) snackbarHostState.showSnackbar(exportFailed)
        }
    }
    ExportContent(
        isExporting = uiState.isExporting,
        openDrawer = openDrawer,
        exportAll = viewModel::exportAll,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

/** Opens the system share menu for the file at [uri]. Returns false if no app can receive it. */
private fun shareFile(context: Context, uri: Uri): Boolean {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/yaml"
        putExtra(Intent.EXTRA_STREAM, uri)
        // ClipData carries the read permission to the share menu's own preview as well as to the receiver.
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(send, null))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

@Composable
private fun ExportContent(
    isExporting: Boolean,
    openDrawer: () -> Unit,
    exportAll: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { ExportTopAppBar(openDrawer) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.horizontal_margin)),
        ) {
            Button(onClick = exportAll, enabled = !isExporting, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.export_all))
            }
        }
    }
}

@Preview
@Composable
private fun ExportScreenPreview() {
    Surface {
        ExportContent(isExporting = false, openDrawer = {}, exportAll = {})
    }
}
