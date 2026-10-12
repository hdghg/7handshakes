@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.hdghg.sevenhandshakes.MainTheme
import io.github.hdghg.sevenhandshakes.R

@Composable
fun StatisticsTopAppBar(openDrawer: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.statistics_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun FeedTopAppBar(openDrawer: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.feed_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun NewGratitudeTopAppBar(onBack: () -> Unit, onComplete: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.new_gratitude)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
            }
        },
        actions = {
            IconButton(onClick = onComplete) {
                Icon(Icons.Filled.Check, stringResource(id = R.string.menu_complete_gratitude))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
private fun StatisticsTopAppBarPreview() {
    MainTheme {
        Surface {
            StatisticsTopAppBar { }
        }
    }
}

@Preview
@Composable
private fun FeedTopAppBarPreview() {
    MainTheme {
        Surface {
            FeedTopAppBar { }
        }
    }
}

@Preview
@Composable
private fun NewGratitudeTopAppBarPreview() {
    MainTheme {
        Surface {
            NewGratitudeTopAppBar(onBack = {}, onComplete = {})
        }
    }
}
