package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord

/** Lists [contacts] with their public keys; tapping one calls [onSelect]. */
@Composable
fun ContactPickerDialog(
    title: String,
    contacts: List<ContactRecord>,
    onSelect: (ContactRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (contacts.isEmpty()) {
                Text(stringResource(id = R.string.new_gratitude_no_contacts))
            } else {
                LazyColumn {
                    items(contacts, key = { it.id }) { contact ->
                        ListItem(
                            headlineContent = {
                                Text(contactLabel(contact))
                            },
                            supportingContent = {
                                Text(
                                    text = contact.publicKey,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            leadingContent = { Icon(Icons.Filled.Person, contentDescription = null) },
                            trailingContent =
                                if (contact.isFavorite) {
                                    {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = stringResource(id = R.string.contact_favorite),
                                        )
                                    }
                                } else {
                                    null
                                },
                            modifier = Modifier.clickable { onSelect(contact) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.cancel))
            }
        },
    )
}
