package io.github.hdghg.sevenhandshakes.ui.reviewrequest

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.BackTopAppBar
import io.github.hdghg.sevenhandshakes.ui.ContactPickerDialog
import io.github.hdghg.sevenhandshakes.ui.FieldSourceButton
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ReviewRequestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewRequestViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pickableContacts by viewModel.pickableContacts.collectAsStateWithLifecycle()
    val targetContact by viewModel.targetContact.collectAsStateWithLifecycle()
    ReviewRequestContent(
        uiState = uiState,
        pickableContacts = pickableContacts,
        targetContact = targetContact,
        onBack = onBack,
        onMethodChange = viewModel::setMethod,
        onTargetKeyChange = viewModel::updateTargetKey,
        onTargetNameChange = viewModel::updateTargetName,
        onTargetEmailChange = viewModel::updateTargetEmail,
        onCopyName = viewModel::copyNameFromContact,
        onCopyEmail = viewModel::copyEmailFromContact,
        onSuggestedTextChange = viewModel::updateSuggestedText,
        onIncludeFavoritesChange = viewModel::setIncludeFavorites,
        modifier = modifier,
    )
}

@Composable
private fun ReviewRequestContent(
    uiState: ReviewRequestUiState,
    pickableContacts: List<ContactRecord>,
    targetContact: ContactRecord?,
    onBack: () -> Unit,
    onMethodChange: (RequestMethod) -> Unit,
    onTargetKeyChange: (String) -> Unit,
    onTargetNameChange: (String) -> Unit,
    onTargetEmailChange: (String) -> Unit,
    onCopyName: () -> Unit,
    onCopyEmail: () -> Unit,
    onSuggestedTextChange: (String) -> Unit,
    onIncludeFavoritesChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    var showAdditional by rememberSaveable { mutableStateOf(false) }
    var showContactPicker by rememberSaveable { mutableStateOf(false) }
    if (showContactPicker) {
        ContactPickerDialog(
            title = stringResource(id = R.string.new_gratitude_pick_contact),
            contacts = pickableContacts,
            onSelect = { contact ->
                onTargetKeyChange(contact.publicKey)
                showContactPicker = false
            },
            onDismiss = { showContactPicker = false },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { BackTopAppBar(title = stringResource(id = R.string.review_request_title), onBack = onBack) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(all = margin),
        ) {
            MethodField(
                selected = uiState.method,
                onSelect = onMethodChange,
                modifier = Modifier.fillMaxWidth().padding(bottom = margin),
            )
            OutlinedTextField(
                value = uiState.targetKey,
                onValueChange = onTargetKeyChange,
                label = { Text(stringResource(id = R.string.review_request_target_key)) },
                isError = uiState.isTargetKeyBlank || uiState.isTargetKeyInvalid,
                supportingText = {
                    Text(
                        stringResource(
                            id = if (uiState.isTargetKeyInvalid) {
                                R.string.review_request_invalid_key
                            } else {
                                R.string.review_request_required
                            },
                        ),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            )
            FieldSourceButton(
                icon = Icons.Filled.Contacts,
                text = stringResource(id = R.string.new_gratitude_from_contacts),
                onClick = { showContactPicker = true },
                modifier = Modifier.padding(bottom = margin / 2),
            )
            TextButton(onClick = { showAdditional = !showAdditional }) {
                Text(text = stringResource(id = R.string.review_request_additional))
                Icon(
                    imageVector = if (showAdditional) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    // The button's text already names it; expanded or not is plain from what shows below.
                    contentDescription = null,
                )
            }
            AnimatedVisibility(visible = showAdditional) {
                AdditionalFields(
                    uiState = uiState,
                    targetContact = targetContact,
                    onTargetNameChange = onTargetNameChange,
                    onTargetEmailChange = onTargetEmailChange,
                    onCopyName = onCopyName,
                    onCopyEmail = onCopyEmail,
                    onSuggestedTextChange = onSuggestedTextChange,
                    onIncludeFavoritesChange = onIncludeFavoritesChange,
                )
            }
        }
    }
}

/** A read-only field that opens the list of request methods. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MethodField(selected: RequestMethod, onSelect: (RequestMethod) -> Unit, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = stringResource(id = selected.label),
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(stringResource(id = R.string.review_request_method)) },
            leadingIcon = { Icon(selected.icon, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RequestMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = method.label)) },
                    leadingIcon = { Icon(method.icon, contentDescription = null) },
                    onClick = {
                        onSelect(method)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@get:StringRes
private val RequestMethod.label: Int
    get() = when (this) {
        RequestMethod.Qr -> R.string.review_request_method_qr
        RequestMethod.DeepLink -> R.string.review_request_method_deep_link
        RequestMethod.RichTextLink -> R.string.review_request_method_rich_text
        RequestMethod.File -> R.string.review_request_method_file
    }

/** The same icons as the share menus use for these ways. */
private val RequestMethod.icon: ImageVector
    get() = when (this) {
        RequestMethod.Qr -> Icons.Filled.QrCode
        RequestMethod.DeepLink -> Icons.Filled.Link
        RequestMethod.RichTextLink -> Icons.Filled.ContentCopy
        RequestMethod.File -> Icons.AutoMirrored.Filled.InsertDriveFile
    }

@Composable
private fun AdditionalFields(
    uiState: ReviewRequestUiState,
    targetContact: ContactRecord?,
    onTargetNameChange: (String) -> Unit,
    onTargetEmailChange: (String) -> Unit,
    onCopyName: () -> Unit,
    onCopyEmail: () -> Unit,
    onSuggestedTextChange: (String) -> Unit,
    onIncludeFavoritesChange: (Boolean) -> Unit,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Column {
        OutlinedTextField(
            value = uiState.targetName,
            onValueChange = onTargetNameChange,
            label = { Text(stringResource(id = R.string.review_request_target_name)) },
            singleLine = true,
            trailingIcon = {
                CopyFromContactButton(
                    description = R.string.review_request_copy_name,
                    enabled = !targetContact?.name.isNullOrBlank(),
                    onClick = onCopyName,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = margin / 2),
        )
        OutlinedTextField(
            value = uiState.targetEmail,
            onValueChange = onTargetEmailChange,
            label = { Text(stringResource(id = R.string.review_request_target_email)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            trailingIcon = {
                CopyFromContactButton(
                    description = R.string.review_request_copy_email,
                    enabled = !targetContact?.email.isNullOrBlank(),
                    onClick = onCopyEmail,
                )
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = margin / 2),
        )
        OutlinedTextField(
            value = uiState.suggestedText,
            onValueChange = onSuggestedTextChange,
            label = { Text(stringResource(id = R.string.review_request_suggested_text)) },
            // A review is one run of text, so the keyboard offers Done in place of a line break.
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth().padding(bottom = margin / 2),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = uiState.includeFavorites,
                    enabled = uiState.canIncludeFavorites,
                    role = Role.Checkbox,
                    onValueChange = onIncludeFavoritesChange,
                ),
        ) {
            // The row handles the tap, so the whole line is a target.
            Checkbox(checked = uiState.includeFavorites, onCheckedChange = null, enabled = uiState.canIncludeFavorites)
            Column(modifier = Modifier.padding(start = margin / 2)) {
                Text(
                    text = stringResource(id = R.string.review_request_include_favorites),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (uiState.canIncludeFavorites) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
                if (!uiState.canIncludeFavorites) {
                    Text(
                        text = stringResource(id = R.string.review_request_favorites_file_only),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** Fills a field from the contact with the target key; disabled when there is no such contact or value. */
@Composable
private fun CopyFromContactButton(@StringRes description: Int, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(Icons.Filled.Contacts, contentDescription = stringResource(id = description))
    }
}

@Preview
@Composable
private fun ReviewRequestScreenPreview() {
    Surface {
        ReviewRequestContent(
            uiState = ReviewRequestUiState(
                method = RequestMethod.File,
                targetKey = "MCowBQYDK2VwAyEAabcdefghijklmnopqrstuvwxyz0123456789ABCD",
            ),
            pickableContacts = emptyList(),
            targetContact = null,
            onBack = {},
            onMethodChange = {},
            onTargetKeyChange = {},
            onTargetNameChange = {},
            onTargetEmailChange = {},
            onCopyName = {},
            onCopyEmail = {},
            onSuggestedTextChange = {},
            onIncludeFavoritesChange = {},
        )
    }
}
