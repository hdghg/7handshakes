package io.github.hdghg.sevenhandshakes.ui.newcontact

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.ui.NewContactTopAppBar
import io.github.hdghg.sevenhandshakes.util.QrScanner
import kotlinx.coroutines.launch

@Composable
fun NewContactScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewContactViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { NewContactTopAppBar(onBack = onBack, onComplete = viewModel::complete) },
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val scanQr: () -> Unit = {
            QrScanner.scanQr(
                context = context,
                onSuccess = viewModel::updatePublicKey,
                onFail = { e -> scope.launch { snackbarHostState.showSnackbar("QR scan failed: ${e.message}") } },
            )
        }
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(id = R.string.new_contact_public_key),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                OutlinedTextField(
                    value = uiState.publicKey,
                    onValueChange = viewModel::updatePublicKey,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = scanQr) {
                    Text("Scan QR")
                }
            }
            Text(
                stringResource(id = R.string.new_contact_name),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            OutlinedTextField(
                value = uiState.name,
                onValueChange = viewModel::updateName,
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
            SwitchRow(
                label = stringResource(id = R.string.new_contact_favorite),
                checked = uiState.isFavorite,
                onCheckedChange = viewModel::setFavorite,
            )
            SwitchRow(
                label = stringResource(id = R.string.new_contact_ignored),
                checked = uiState.isIgnored,
                onCheckedChange = viewModel::setIgnored,
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
        NewContactScreen({}, {})
    }
}
