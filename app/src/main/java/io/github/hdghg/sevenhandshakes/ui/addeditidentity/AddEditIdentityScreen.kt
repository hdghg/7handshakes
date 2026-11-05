package io.github.hdghg.sevenhandshakes.ui.addeditidentity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.AddEditIdentityTopAppBar

@Composable
fun AddEditIdentityScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddEditIdentityViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AddEditIdentityContent(
        uiState = uiState,
        onBack = onBack,
        onSave = viewModel::complete,
        onPrivateKeyChange = viewModel::updatePrivateKey,
        onGenerate = viewModel::generatePrivateKey,
        onNameChange = viewModel::updateName,
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
private fun AddEditIdentityContent(
    uiState: AddEditIdentityUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPrivateKeyChange: (String) -> Unit,
    onGenerate: () -> Unit,
    onNameChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { AddEditIdentityTopAppBar(onBack = onBack, onComplete = onSave) },
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
                OutlinedTextField(
                    value = uiState.privateKey,
                    onValueChange = onPrivateKeyChange,
                    label = { Text(stringResource(id = R.string.identity_private_key)) },
                    isError = uiState.isPrivateKeyError,
                    supportingText = when {
                        uiState.isPrivateKeyEncrypted -> {
                            { Text(stringResource(id = R.string.identity_private_key_encrypted)) }
                        }
                        uiState.isPrivateKeyError -> {
                            { Text(stringResource(id = R.string.identity_private_key_invalid)) }
                        }
                        else -> null
                    },
                    // Multi-line so pasted PEM / OpenSSH keys stay readable.
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onGenerate) {
                    Text(stringResource(id = R.string.identity_generate))
                }
            }
            OutlinedTextField(
                value = uiState.publicKey.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(id = R.string.identity_public_key)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            OutlinedTextField(
                value = uiState.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(id = R.string.identity_name)) },
                isError = uiState.isNameError,
                supportingText = if (uiState.isNameError) {
                    { Text(stringResource(id = R.string.identity_name_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Preview
@Composable
fun AddEditIdentityScreenPreview() {
    Surface {
        AddEditIdentityContent(
            uiState = AddEditIdentityUiState(
                privateKey = "MC4CAQAwBQYDK2VwBCIEINTuctv5E1hK1bbY8fdp+K06/nwoy/HU++CXqI9EdVhC",
                publicKey = "MCowBQYDK2VwAyEAGb9ECWmEzf6FQbrBZ9w7lshQhqowtrbLDFw4rXAxZuE=",
                name = "",
            ),
            onBack = {},
            onSave = {},
            onPrivateKeyChange = {},
            onGenerate = {},
            onNameChange = {},
        )
    }
}
