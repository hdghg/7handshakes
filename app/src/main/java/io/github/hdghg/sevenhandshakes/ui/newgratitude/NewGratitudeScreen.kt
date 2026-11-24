package io.github.hdghg.sevenhandshakes.ui.newgratitude

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.FieldSourceButton
import io.github.hdghg.sevenhandshakes.ui.ContactPickerDialog
import io.github.hdghg.sevenhandshakes.data.RATE_LEVELS
import io.github.hdghg.sevenhandshakes.ui.NewGratitudeTopAppBar
import io.github.hdghg.sevenhandshakes.ui.GoldStar
import io.github.hdghg.sevenhandshakes.ui.rateDescriptionOf
import io.github.hdghg.sevenhandshakes.util.QrScanner
import kotlinx.coroutines.launch

@Composable
fun NewGratitudeScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewGratitudeViewModel = koinViewModel(),
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
        val resources = LocalResources.current
        val scope = rememberCoroutineScope()
        val scanQr: () -> Unit = {
            QrScanner.scanQr(
                context = context,
                onSuccess = viewModel::receiverScanned,
                onFail = { e ->
                    val message = resources.getString(R.string.qr_scan_failed, e.message.orEmpty())
                    scope.launch { snackbarHostState.showSnackbar(message) }
                },
            )
        }
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = modifier.padding(paddingValues),
        ) {
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::updateText,
                label = { Text(stringResource(id = R.string.new_gratitude_comment)) },
                // A review is one run of text, so the keyboard offers Done in place of a line break.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
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
            OutlinedTextField(
                value = uiState.receiver,
                label = { Text(stringResource(id = R.string.new_gratitude_receiver)) },
                onValueChange = viewModel::updateReceiver,
                isError = uiState.isReceiverInvalid,
                supportingText =
                    if (uiState.isReceiverInvalid) {
                        { Text(stringResource(id = R.string.new_gratitude_invalid_receiver)) }
                    } else {
                        null
                    },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp),
            )
            // Two equal ways to fill the receiver.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FieldSourceButton(
                    icon = Icons.Filled.QrCodeScanner,
                    text = stringResource(id = R.string.new_gratitude_scan_qr),
                    onClick = scanQr,
                    modifier = Modifier.weight(1f),
                )
                FieldSourceButton(
                    icon = Icons.Filled.Contacts,
                    text = stringResource(id = R.string.new_gratitude_from_contacts),
                    onClick = { showContactPicker = true },
                    modifier = Modifier.weight(1f),
                )
            }
            if (showContactPicker) {
                ContactPickerDialog(
                    title = stringResource(id = R.string.new_gratitude_pick_contact),
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

/** Rate picker: 1 to 3 golden stars in a row. Tapping a star selects that many stars. */
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
                .padding(top = 8.dp)
                .selectableGroup(),
        ) {
            RATE_LEVELS.forEach { stars ->
                StarOption(
                    stars = stars,
                    filled = stars <= selected,
                    selected = stars.toInt() == selected,
                    onClick = { onSelect(stars.toInt()) },
                )
            }
        }
        Text(
            text = stringResource(id = rateDescriptionOf(selected.toByte())),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** One star of the picker: golden up to the selected number, grey after it. */
@Composable
private fun StarOption(stars: Byte, filled: Boolean, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (filled) 1f else 0.85f, label = "starOptionScale")
    val description = stringResource(id = rateDescriptionOf(stars))
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = description },
    ) {
        val starModifier = Modifier.size(44.dp).scale(scale)
        if (filled) {
            GoldStar(modifier = starModifier)
        } else {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = starModifier,
            )
        }
    }
}

@Preview
@Composable
fun NewGratitudeScreenPreview() {
    Surface {
        NewGratitudeScreen({}, {})
    }
}
