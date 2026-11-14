package io.github.hdghg.sevenhandshakes.ui.export

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
import io.github.hdghg.sevenhandshakes.ui.ExportTopAppBar

/** Exports data from the reviews table. */
@Composable
fun ExportScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExportContent(
        openDrawer = openDrawer,
        // TODO: export every row of the reviews table.
        exportAll = {},
        modifier = modifier,
    )
}

@Composable
private fun ExportContent(
    openDrawer: () -> Unit,
    exportAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ExportTopAppBar(openDrawer) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(dimensionResource(id = R.dimen.horizontal_margin)),
        ) {
            Button(onClick = exportAll, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.export_all))
            }
        }
    }
}

@Preview
@Composable
private fun ExportScreenPreview() {
    Surface {
        ExportContent(openDrawer = {}, exportAll = {})
    }
}
