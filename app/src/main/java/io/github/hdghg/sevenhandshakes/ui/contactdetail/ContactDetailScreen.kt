package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.ContactDetailTopAppBar

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ContactDetailContent(
        contact = uiState.contact,
        isLoading = uiState.isLoading,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ContactDetailContent(
    contact: ContactRecord?,
    isLoading: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ContactDetailTopAppBar(onBack) },
    ) { paddingValues ->
        if (contact == null) {
            if (!isLoading) {
                Text(
                    text = stringResource(id = R.string.contact_not_found),
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(all = margin),
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(all = margin),
        ) {
            Field(label = stringResource(id = R.string.contact_name)) {
                Text(
                    text = contact.name ?: stringResource(id = R.string.contact_unnamed),
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Field(label = stringResource(id = R.string.contact_public_key)) {
                SelectionContainer {
                    Text(text = contact.publicKey, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (contact.isFavorite) {
                Flag(Icons.Filled.Star, stringResource(id = R.string.contact_favorite), MaterialTheme.colorScheme.secondary)
            }
            if (contact.isIgnored) {
                Flag(Icons.Filled.Block, stringResource(id = R.string.contact_ignored), MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = margin)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun Flag(icon: ImageVector, label: String, tint: Color) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = margin / 2),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.padding(end = margin / 2))
        Text(text = label)
    }
}

@Preview
@Composable
fun ContactDetailScreenPreview() {
    Surface {
        ContactDetailContent(
            contact = ContactRecord(
                id = 1,
                publicKey = "MCowBQYDK2VwAyEAabcdefghijklmnopqrstuvwxyz0123456789ABCD",
                name = "Alice",
                isFavorite = true,
                isIgnored = true,
            ),
            isLoading = false,
            onBack = {},
        )
    }
}
