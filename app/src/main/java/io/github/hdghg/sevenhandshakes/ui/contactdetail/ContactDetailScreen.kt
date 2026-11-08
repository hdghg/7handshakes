package io.github.hdghg.sevenhandshakes.ui.contactdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.HandshakePath
import io.github.hdghg.sevenhandshakes.data.RATE_LEVELS
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.ContactDetailTopAppBar
import io.github.hdghg.sevenhandshakes.ui.RateStar

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onEdit: (contactId: Int) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteContactDialog(
            isUsedByIdentity = uiState.isUsedByIdentity,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteContact()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    ContactDetailContent(
        contact = uiState.contact,
        isLoading = uiState.isLoading,
        handshakeRatings = uiState.handshakeRatings,
        myContactId = uiState.myContactId,
        contactNames = uiState.contactNames,
        handshakeDepth = uiState.handshakeDepth,
        onHandshakeDepthChange = viewModel::setHandshakeDepth,
        onEdit = { uiState.contact?.let { onEdit(it.id) } },
        onDelete = { if (uiState.contact != null) showDeleteDialog = true },
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ContactDetailContent(
    contact: ContactRecord?,
    isLoading: Boolean,
    handshakeRatings: List<ReviewExchange>?,
    myContactId: Int?,
    contactNames: Map<Int, String?>,
    handshakeDepth: Int,
    onHandshakeDepthChange: (Int) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    var showDepthSheet by rememberSaveable { mutableStateOf(false) }
    if (showDepthSheet) {
        HandshakeDepthSheet(
            depth = handshakeDepth,
            onDepthChange = onHandshakeDepthChange,
            onDismiss = { showDepthSheet = false },
        )
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ContactDetailTopAppBar(
                onBack = onBack,
                onEdit = onEdit,
                onDelete = onDelete,
                onHandshakeDepthClick = { showDepthSheet = true },
            )
        },
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
            if (handshakeRatings == null) {
                Field(label = handshakeTitle(level = 1)) {
                    Text(text = stringResource(id = R.string.contact_reviews_no_identity))
                }
            } else if (handshakeDepth == 0) {
                Field(label = stringResource(id = R.string.handshake_ratings)) {
                    Text(text = stringResource(id = R.string.handshake_ratings_off))
                }
            } else {
                for (level in 1..handshakeDepth) {
                    // Keyed so each table keeps its own revealed/hidden state.
                    key(level) {
                        Field(label = handshakeTitle(level)) {
                            val rating = handshakeRatings.getOrNull(level - 1)
                            when {
                                rating != null -> ReviewExchangeTable(
                                    exchange = rating,
                                    handshakes = level,
                                    myContactId = myContactId,
                                    contactNames = contactNames,
                                )
                                level - 1 == handshakeRatings.size -> PendingRating(calculating = true)
                                else -> PendingRating(calculating = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Asks before deleting; for a contact an identity uses, only explains why it cannot be deleted. */
@Composable
private fun DeleteContactDialog(isUsedByIdentity: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.delete_contact_title)) },
        text = {
            Text(
                text = stringResource(
                    id = if (isUsedByIdentity) R.string.delete_contact_used_by_identity else R.string.delete_contact_message,
                ),
            )
        },
        confirmButton = {
            if (isUsedByIdentity) {
                TextButton(onClick = onDismiss) { Text(text = stringResource(id = android.R.string.ok)) }
            } else {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(id = R.string.delete_contact_confirm), color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = if (isUsedByIdentity) {
            null
        } else {
            { TextButton(onClick = onDismiss) { Text(text = stringResource(id = android.R.string.cancel)) } }
        },
    )
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

/**
 * One column per rate level, diamond first. Each shows its star and received minus given below it.
 * Tapping the table reveals or hides the received and given counts and the paths behind them.
 */
@Composable
private fun ReviewExchangeTable(
    exchange: ReviewExchange,
    handshakes: Int,
    myContactId: Int?,
    contactNames: Map<Int, String?>,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val toggleLabel = stringResource(
        id = if (showDetails) R.string.contact_reviews_hide_details else R.string.contact_reviews_show_details,
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = margin / 2)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClickLabel = toggleLabel) { showDetails = !showDetails }
            .padding(vertical = margin / 2),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            RATE_LEVELS.sortedDescending().forEach { level ->
                RateColumn(
                    level = level,
                    exchange = exchange,
                    handshakes = handshakes,
                    myContactId = myContactId,
                    contactNames = contactNames,
                    showDetails = showDetails,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Text(
            text = toggleLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = margin / 2),
        )
    }
}

@Composable
private fun RateColumn(
    level: Byte,
    exchange: ReviewExchange,
    handshakes: Int,
    myContactId: Int?,
    contactNames: Map<Int, String?>,
    showDetails: Boolean,
    modifier: Modifier = Modifier,
) {
    val difference = exchange.differenceOf(level)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        RateStar(rate = level, modifier = Modifier.size(32.dp))
        Text(
            text = formatDifference(difference),
            style = MaterialTheme.typography.titleMedium,
            color = when {
                difference > 0 -> MaterialTheme.colorScheme.secondary
                difference < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        AnimatedVisibility(visible = showDetails) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DetailLine(stringResource(id = R.string.contact_reviews_received_count, exchange.receivedOf(level)))
                DetailLine(stringResource(id = R.string.contact_reviews_given_count, exchange.givenOf(level)))
                if (myContactId != null) {
                    val unnamed = stringResource(id = R.string.contact_unnamed)
                    HandshakePathDiagram(
                        received = exchange.receivedPaths[level],
                        given = exchange.givenPaths[level],
                        handshakes = handshakes,
                        labelOf = { id -> contactNames[id] ?: unnamed },
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** Placeholder for a rating that is being calculated, or waiting for the previous one. */
@Composable
private fun PendingRating(calculating: Boolean) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = margin / 2),
    ) {
        if (calculating) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
        } else {
            Icon(
                imageVector = Icons.Filled.HourglassEmpty,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = stringResource(
                id = if (calculating) R.string.handshake_rating_calculating else R.string.handshake_rating_waiting,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = margin / 2),
        )
    }
}

/** "One handshake rating", "Two handshakes rating", ...; digits past the named levels. */
@Composable
private fun handshakeTitle(level: Int): String {
    val named = stringArrayResource(id = R.array.handshake_rating_titles)
    return named.getOrNull(level - 1) ?: stringResource(id = R.string.handshake_rating_title_n, level)
}

private fun formatDifference(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
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
            handshakeRatings = listOf(
                ReviewExchange(
                    receivedPaths = mapOf(3.toByte() to HandshakePath(listOf(1, 9), listOf(2))),
                    givenPaths = mapOf(2.toByte() to HandshakePath(listOf(9, 1), listOf(3))),
                ),
                ReviewExchange(
                    receivedPaths = mapOf(
                        3.toByte() to HandshakePath(listOf(1, 2, 9), listOf(4, 1)),
                        1.toByte() to HandshakePath(listOf(1, 3, 9), listOf(2, 5)),
                    ),
                    givenPaths = mapOf(3.toByte() to HandshakePath(listOf(9, 3, 1), listOf(2, 2))),
                ),
            ),
            myContactId = 9,
            contactNames = mapOf(1 to "Alice", 2 to "Bob", 3 to null, 9 to "Me"),
            handshakeDepth = 3,
            onHandshakeDepthChange = {},
            onEdit = {},
            onDelete = {},
            onBack = {},
        )
    }
}
