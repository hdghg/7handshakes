package io.github.hdghg.sevenhandshakes.ui.newcontact

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.NewContactTopAppBar
import io.github.hdghg.sevenhandshakes.util.QrScanner
import kotlinx.coroutines.launch

/** Adds a new contact, or edits an existing one when opened with a contact id. */
@Composable
fun NewContactScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewContactViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    NewContactContent(
        uiState = uiState,
        onBack = onBack,
        onSave = viewModel::complete,
        onPublicKeyChange = viewModel::updatePublicKey,
        onScanQr = {
            QrScanner.scanQr(
                context = context,
                onSuccess = viewModel::updatePublicKey,
                onFail = { e -> scope.launch { snackbarHostState.showSnackbar("QR scan failed: ${e.message}") } },
            )
        },
        onNameChange = viewModel::updateName,
        onEmailChange = viewModel::updateEmail,
        onFavoriteChange = viewModel::setFavorite,
        onIgnoredChange = viewModel::setIgnored,
        modifier = modifier,
        snackbarHostState = snackbarHostState,
    )

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

@Composable
private fun NewContactContent(
    uiState: NewContactUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPublicKeyChange: (String) -> Unit,
    onScanQr: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onIgnoredChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NewContactTopAppBar(isEdit = uiState.isEdit, onBack = onBack, onComplete = onSave) },
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                // The public key identifies the contact, so it is fixed once the contact exists.
                OutlinedTextField(
                    value = uiState.publicKey,
                    onValueChange = onPublicKeyChange,
                    label = { Text(stringResource(id = R.string.new_contact_public_key)) },
                    isError = uiState.isPublicKeyError,
                    supportingText = if (uiState.isPublicKeyError) {
                        { Text(stringResource(id = R.string.new_contact_empty_public_key)) }
                    } else {
                        null
                    },
                    readOnly = uiState.isEdit,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                if (!uiState.isEdit) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onScanQr) {
                        Text(stringResource(id = R.string.new_contact_scan_qr))
                    }
                }
            }
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(id = R.string.new_contact_name)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            OutlinedTextField(
                value = uiState.email,
                onValueChange = onEmailChange,
                label = { Text(stringResource(id = R.string.new_contact_email)) },
                isError = uiState.isEmailError,
                supportingText = if (uiState.isEmailError) {
                    { Text(stringResource(id = R.string.new_contact_invalid_email)) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            SwitchRow(
                label = stringResource(id = R.string.new_contact_favorite),
                checked = uiState.isFavorite,
                onCheckedChange = onFavoriteChange,
            )
            SwitchRow(
                label = stringResource(id = R.string.new_contact_ignored),
                checked = uiState.isIgnored,
                onCheckedChange = onIgnoredChange,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Preview
@Composable
fun NewContactScreenPreview() {
    Surface {
        NewContactContent(
            uiState = NewContactUiState(
                publicKey = "MCowBQYDK2VwAyEAGb9ECWmEzf6FQbrBZ9w7lshQhqowtrbLDFw4rXAxZuE=",
                name = "Alice",
                email = "alice@",
                saveAttempted = true,
            ),
            onBack = {},
            onSave = {},
            onPublicKeyChange = {},
            onScanQr = {},
            onNameChange = {},
            onEmailChange = {},
            onFavoriteChange = {},
            onIgnoredChange = {},
        )
    }
}
