package io.github.hdghg.sevenhandshakes.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.ui.FeedTopAppBar

@Composable
fun FeedScreen(
    openDrawer: () -> Unit,
    newGratitude: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FeedContent(
        reviews = uiState.items,
        openDrawer = openDrawer,
        newGratitude = newGratitude,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun FeedContent(
    reviews: List<Review>,
    openDrawer: () -> Unit,
    newGratitude: () -> Unit,
    modifier: Modifier = Modifier,
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
        LazyColumn(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.padding(paddingValues),
            verticalArrangement = Arrangement.Center,
        ) {
            items(reviews, key = { it.id }) { review ->
                FeedItem(review)
            }
        }
    }
}

@Composable
private fun FeedItem(review: Review) {
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
            Text(text = review.text, style = MaterialTheme.typography.headlineSmall)
            Text(text = "★".repeat(review.rate.toInt().coerceAtLeast(0)), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Preview
@Composable
fun FeedScreenPreview() {
    Surface {
        FeedContent(
            reviews =
                listOf(
                    Review(id = 1, uuid = "1", text = "Bra jobbat", rate = 3),
                    Review(id = 2, uuid = "2", text = "Other text", rate = 2),
                    Review(id = 3, uuid = "3", text = "kul", rate = 1),
                ),
            openDrawer = {},
            newGratitude = {},
        )
    }
}
