package io.github.hdghg.sevenhandshakes.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.FeedTopAppBar

@Composable
fun FeedScreen(
    openDrawer: () -> Unit,
    newGratitude: () -> Unit,
    modifier: Modifier = Modifier,
//    viewModel: StatisticsViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { FeedTopAppBar(openDrawer) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = { newGratitude() }) {
                Icon(Icons.Filled.Add, stringResource(id = R.string.edit_task))
            }
        },
    ) { paddingValues ->
        val screenPadding =
            Modifier.padding(
                horizontal = dimensionResource(id = R.dimen.horizontal_margin),
                vertical = dimensionResource(id = R.dimen.vertical_margin),
            )
        val commonModifier =
            modifier
                .fillMaxWidth()
                .then(screenPadding)

        LazyColumn(
            horizontalAlignment = Alignment.Start,
            modifier = modifier.padding(paddingValues),
            verticalArrangement = Arrangement.Center,
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Handshake,
                        contentDescription = null,
                        modifier = Modifier.padding(end = dimensionResource(id = R.dimen.horizontal_margin)),
                    )
                    Column {
                        Text(text = "Bra jobbat", style = MaterialTheme.typography.headlineSmall)
                        Text(text = "Anders", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Handshake,
                        contentDescription = null,
                        modifier = Modifier.padding(end = dimensionResource(id = R.dimen.horizontal_margin)),
                    )
                    Column {
                        Text(text = "Other text", style = MaterialTheme.typography.headlineSmall)
                        Text(text = "Anne", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin)),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Handshake,
                        contentDescription = null,
                        modifier = Modifier.padding(end = dimensionResource(id = R.dimen.horizontal_margin)),
                    )
                    Column {
                        Text(text = "kul", style = MaterialTheme.typography.headlineSmall)
                        Text(text = "Katarina", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun FeedScreenPreview() {
    Surface {
        FeedScreen({}, {})
    }
}
