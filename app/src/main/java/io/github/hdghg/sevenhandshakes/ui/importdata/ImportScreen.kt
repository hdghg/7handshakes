package io.github.hdghg.sevenhandshakes.ui.importdata

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.ImportTopAppBar

/** Lets the user pick a file to import. */
@Composable
fun ImportScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // TODO: import the picked file; uri is null when the user picked nothing.
    }
    ImportContent(
        openDrawer = openDrawer,
        // Android has no dependable MIME type for YAML, so any file can be picked.
        import = { pickFile.launch(arrayOf("*/*")) },
        modifier = modifier,
    )
}

@Composable
private fun ImportContent(
    openDrawer: () -> Unit,
    import: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
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
