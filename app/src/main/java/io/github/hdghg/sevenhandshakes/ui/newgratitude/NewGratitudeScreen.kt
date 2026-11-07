package io.github.hdghg.sevenhandshakes.ui.newgratitude

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.RATE_LEVELS
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.NewGratitudeTopAppBar
import io.github.hdghg.sevenhandshakes.ui.RateStar
import io.github.hdghg.sevenhandshakes.util.QrScanner
import kotlinx.coroutines.launch

@Composable
fun NewGratitudeScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewGratitudeViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NewGratitudeTopAppBar(onBack = onBack, onComplete = viewModel::complete) },
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val contacts by viewModel.contacts.collectAsStateWithLifecycle()
        var showContactPicker by rememberSaveable { mutableStateOf(false) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val scanQr: () -> Unit = {
            QrScanner.scanQr(
                context = context,
                onSuccess = viewModel::updateReceiver,
                onFail = { e -> scope.launch { snackbarHostState.showSnackbar("QR scan failed: ${e.message}") } },
            )
        }
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = modifier.padding(paddingValues),
        ) {
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::updateText,
                label = { Text("Your comment") },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            )

            StarPicker(
                selected = uiState.stars,
                onSelect = viewModel::setStars,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.receiver,
                    label = { Text("Receiver") },
                    onValueChange = viewModel::updateReceiver,
                    isError = uiState.isReceiverInvalid,
                    supportingText =
                        if (uiState.isReceiverInvalid) {
                            { Text(stringResource(id = R.string.new_gratitude_invalid_receiver)) }
                        } else {
                            null
                        },
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                // Stacked so the receiver field keeps a usable width on phones.
                Column(modifier = Modifier.width(IntrinsicSize.Max)) {
                    Button(
                        onClick = scanQr,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Scan QR")
                    }
                    Button(
                        onClick = { showContactPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("From contacts")
                    }
                }
            }
            if (showContactPicker) {
                ContactPickerDialog(
                    contacts = contacts,
                    onSelect = { contact ->
                        viewModel.updateReceiver(contact.publicKey)
                        showContactPicker = false
                    },
                    onDismiss = { showContactPicker = false },
                )
            }
            uiState.userMessage?.let { messageId ->
                val message = stringResource(id = messageId)
                LaunchedEffect(messageId, message) {
                    snackbarHostState.showSnackbar(message)
                    viewModel.userMessageShown()
                }
            }
            LaunchedEffect(uiState.editComplete) {
                if (uiState.editComplete) {
                    onComplete()
                }
            }
        }
    }
}

@Composable
private fun ContactPickerDialog(
    contacts: List<ContactRecord>,
    onSelect: (ContactRecord) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.new_gratitude_pick_contact)) },
        text = {
            if (contacts.isEmpty()) {
                Text(stringResource(id = R.string.new_gratitude_no_contacts))
            } else {
                LazyColumn {
                    items(contacts, key = { it.id }) { contact ->
                        ListItem(
                            headlineContent = {
                                Text(contact.name ?: stringResource(id = R.string.contact_unnamed))
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

/** Rate picker laid out like the contact screen's review table: diamond, gold, silver. */
@Composable
private fun StarPicker(selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = stringResource(id = R.string.new_gratitude_stars),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .selectableGroup(),
        ) {
            RATE_LEVELS.sortedDescending().forEach { level ->
                StarOption(
                    level = level,
                    selected = selected == level.toInt(),
                    onClick = { onSelect(level.toInt()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = stringResource(id = rateDescriptionOf(selected)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun StarOption(level: Byte, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val background by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        label = "starOptionBackground",
    )
    val contentAlpha by animateFloatAsState(targetValue = if (selected) 1f else 0.4f, label = "starOptionAlpha")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 8.dp)
            .alpha(contentAlpha),
    ) {
        RateStar(rate = level, modifier = Modifier.size(32.dp))
        Text(
            text = stringResource(id = rateNameOf(level.toInt())),
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun rateNameOf(level: Int): Int = when (level) {
    3 -> R.string.rate_diamond
    2 -> R.string.rate_gold
    else -> R.string.rate_silver
}

private fun rateDescriptionOf(level: Int): Int = when (level) {
    3 -> R.string.feed_rate_diamond
    2 -> R.string.feed_rate_gold
    else -> R.string.feed_rate_silver
}

@Preview
@Composable
fun NewGratitudeScreenPreview() {
    Surface {
        NewGratitudeScreen({}, {})
    }
}
