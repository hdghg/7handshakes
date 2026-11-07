@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
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
fun FeedTopAppBar(openDrawer: () -> Unit, showAll: Boolean, onShowAllChange: (Boolean) -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.feed_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        actions = {
            IconToggleButton(checked = showAll, onCheckedChange = onShowAllChange) {
                Icon(Icons.Filled.Public, stringResource(id = R.string.feed_show_all))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ContactsTopAppBar(openDrawer: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.contacts_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ContactDetailTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.contact_details)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun IdentitiesTopAppBar(openDrawer: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.identities_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun AddEditIdentityTopAppBar(onBack: () -> Unit, onComplete: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.add_identity)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
            }
        },
        actions = {
            IconButton(onClick = onComplete) {
                Icon(Icons.Filled.Check, stringResource(id = R.string.menu_save_identity))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun NewContactTopAppBar(onBack: () -> Unit, onComplete: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.new_contact)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
            }
        },
        actions = {
            IconButton(onClick = onComplete) {
                Icon(Icons.Filled.Check, stringResource(id = R.string.menu_save_contact))
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
private fun FeedTopAppBarPreview() {
    MainTheme {
        Surface {
            FeedTopAppBar(openDrawer = {}, showAll = false, onShowAllChange = {})
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
