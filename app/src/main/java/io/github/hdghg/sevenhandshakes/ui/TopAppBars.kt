@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.hdghg.sevenhandshakes.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.hdghg.sevenhandshakes.MainTheme
import io.github.hdghg.sevenhandshakes.R

@Composable
fun FeedTopAppBar(
    openDrawer: () -> Unit,
    showAll: Boolean,
    onShowAllChange: (Boolean) -> Unit,
    requestReview: () -> Unit,
) {
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
            IconButton(onClick = requestReview) {
                Icon(Icons.Filled.RateReview, stringResource(id = R.string.review_request_title))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ContactsTopAppBar(openDrawer: () -> Unit, showUnknown: Boolean, onShowUnknownChange: (Boolean) -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.contacts_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        actions = {
            IconToggleButton(checked = showUnknown, onCheckedChange = onShowUnknownChange) {
                Icon(
                    imageVector = if (showUnknown) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                    contentDescription = stringResource(id = R.string.contacts_show_unknown),
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ContactDetailTopAppBar(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    share: ShareActions? = null,
) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.contact_details)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
            }
        },
        actions = {
            // Absent when the contact cannot be shared.
            if (share != null) ShareMenuButton(stringResource(id = R.string.contact_share), share)
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, stringResource(id = R.string.menu_edit_contact))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete_contact))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ReviewDetailTopAppBar(onBack: () -> Unit, share: ShareActions? = null, onDelete: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.review_details)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
            }
        },
        actions = {
            // Absent when the review cannot be shared.
            if (share != null) ShareMenuButton(stringResource(id = R.string.review_share), share)
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, stringResource(id = R.string.menu_delete_review))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** A share icon that opens a menu of the ways to share: as a QR code, as a link, as formatted text on the clipboard, as a file. */
@Composable
private fun ShareMenuButton(description: String, actions: ShareActions) {
    var expanded by remember { mutableStateOf(false) }
    // The box anchors the menu to the button.
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Share, description)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ShareMenuItem(Icons.Filled.QrCode, R.string.review_share_qr) {
                expanded = false
                actions.asQr()
            }
            ShareMenuItem(Icons.Filled.Link, R.string.share_as_link) {
                expanded = false
                actions.asLink()
            }
            ShareMenuItem(Icons.Filled.ContentCopy, R.string.copy_as_rich_text) {
                expanded = false
                actions.asRichText()
            }
            ShareMenuItem(Icons.AutoMirrored.Filled.InsertDriveFile, R.string.share_as_file) {
                expanded = false
                actions.asFile()
            }
        }
    }
}

@Composable
private fun ShareMenuItem(icon: ImageVector, @StringRes text: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text = stringResource(id = text)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

/** A top bar with nothing but [title] and the back arrow. */
@Composable
fun BackTopAppBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = title) },
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
fun ExportTopAppBar(openDrawer: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.export_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ImportTopAppBar(openDrawer: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.import_title)) },
        navigationIcon = {
            IconButton(onClick = openDrawer) {
                Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun GraphInspectionTopAppBar(openDrawer: () -> Unit, onBack: (() -> Unit)? = null) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.graph_inspection_title)) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
                }
            } else {
                IconButton(onClick = openDrawer) {
                    Icon(Icons.Filled.Menu, stringResource(id = R.string.open_drawer))
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ImportSelectionTopAppBar(onBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = R.string.import_selection_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(id = R.string.menu_back))
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
fun NewContactTopAppBar(isEdit: Boolean, onBack: () -> Unit, onComplete: () -> Unit) {
    TopAppBar(
        title = { Text(text = stringResource(id = if (isEdit) R.string.edit_contact else R.string.new_contact)) },
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
            FeedTopAppBar(openDrawer = {}, showAll = false, onShowAllChange = {}, requestReview = {})
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
