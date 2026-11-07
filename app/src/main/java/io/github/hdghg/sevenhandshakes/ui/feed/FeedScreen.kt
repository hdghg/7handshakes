package io.github.hdghg.sevenhandshakes.ui.feed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
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
        showAll = uiState.showAll,
        onShowAllChange = viewModel::setShowAll,
        openDrawer = openDrawer,
        newGratitude = newGratitude,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun FeedContent(
    reviews: List<Review>,
    showAll: Boolean,
    onShowAllChange: (Boolean) -> Unit,
    openDrawer: () -> Unit,
    newGratitude: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { FeedTopAppBar(openDrawer, showAll, onShowAllChange) },
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
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = margin, vertical = margin / 2),
    ) {
        Icon(
            imageVector = Icons.Filled.Handshake,
            contentDescription = null,
            modifier = Modifier.padding(end = margin),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = review.text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val unnamed = stringResource(id = R.string.contact_unnamed)
            Text(
                text = stringResource(
                    id = R.string.feed_from_to,
                    review.sourceName ?: unnamed,
                    review.targetName ?: unnamed,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "★".repeat(review.rate.toInt().coerceAtLeast(0)),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = margin),
        )
    }
}

@Preview
@Composable
fun FeedScreenPreview() {
    Surface {
        FeedContent(
            reviews =
                listOf(
                    Review(id = 1, uuid = "1", text = "Bra jobbat", rate = 3, sourceName = "Me", targetName = "Alice"),
                    Review(id = 2, uuid = "2", text = "Other text", rate = 2, sourceName = "Me", targetName = "Bob"),
                    Review(id = 3, uuid = "3", text = "kul", rate = 1, sourceName = "Me"),
                ),
            showAll = false,
            onShowAllChange = {},
            openDrawer = {},
            newGratitude = {},
        )
    }
}
