package io.github.hdghg.sevenhandshakes.ui.graphinspection

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.HandshakePath
import io.github.hdghg.sevenhandshakes.data.MAX_HANDSHAKE_DEPTH
import io.github.hdghg.sevenhandshakes.data.ReviewExchange
import io.github.hdghg.sevenhandshakes.data.source.contact.ContactRecord
import io.github.hdghg.sevenhandshakes.ui.ContactPickerDialog
import io.github.hdghg.sevenhandshakes.ui.Field
import io.github.hdghg.sevenhandshakes.ui.FieldSourceButton
import io.github.hdghg.sevenhandshakes.ui.GraphInspectionTopAppBar
import io.github.hdghg.sevenhandshakes.ui.GoldStar
import io.github.hdghg.sevenhandshakes.ui.GoldStarColor
import io.github.hdghg.sevenhandshakes.ui.SummaryRating
import io.github.hdghg.sevenhandshakes.ui.contactLabel
import io.github.hdghg.sevenhandshakes.ui.displayNameOrUnknown
import kotlin.math.roundToInt

/**
 * Handshake ratings between any two contacts: one in the middle of every path, one at both ends.
 * Also where the app-wide handshake depth is set.
 */
@Composable
fun GraphInspectionScreen(
    openDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    /** When set, the top bar shows a back arrow calling it in place of the drawer button. */
    onBack: (() -> Unit)? = null,
    viewModel: GraphInspectionViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GraphInspectionContent(
        uiState = uiState,
        openDrawer = openDrawer,
        onBack = onBack,
        onMiddleKeyChange = viewModel::updateMiddleKey,
        onEndKeyChange = viewModel::updateEndKey,
        onHandshakeDepthChange = viewModel::setHandshakeDepth,
        modifier = modifier,
    )
}

@Composable
private fun GraphInspectionContent(
    uiState: GraphInspectionUiState,
    openDrawer: () -> Unit,
    onBack: (() -> Unit)?,
    onMiddleKeyChange: (String) -> Unit,
    onEndKeyChange: (String) -> Unit,
    onHandshakeDepthChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { GraphInspectionTopAppBar(openDrawer = openDrawer, onBack = onBack) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(all = margin),
        ) {
            ContactKeyField(
                label = stringResource(id = R.string.graph_inspection_middle),
                value = uiState.middleKey,
                contact = uiState.middleContact,
                contacts = uiState.contacts,
                onValueChange = onMiddleKeyChange,
            )
            ContactKeyField(
                label = stringResource(id = R.string.graph_inspection_ends),
                value = uiState.endKey,
                contact = uiState.endContact,
                contacts = uiState.contacts,
                onValueChange = onEndKeyChange,
            )
            HandshakeDepthSlider(depth = uiState.handshakeDepth, onDepthChange = onHandshakeDepthChange)
            val ratings = uiState.handshakeRatings
            val middle = uiState.middleContact
            if (ratings == null || middle == null) {
                Text(
                    text = stringResource(id = R.string.graph_inspection_choose_both),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val middleLabel = contactLabel(middle)
                if (uiState.handshakeDepth > 0) {
                    Field(label = stringResource(id = R.string.summary_rating_title)) {
                        SummaryRating(
                            ratings = ratings,
                            showCaptions = true,
                            isCalculating = ratings.size < uiState.handshakeDepth,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(bottom = margin))
                }
                for (level in 1..uiState.handshakeDepth) {
                    key(level) {
                        Field(label = handshakeTitle(level)) {
                            val rating = ratings.getOrNull(level - 1)
                            when {
                                rating != null -> ReviewExchangeTable(
                                    exchange = rating,
                                    handshakes = level,
                                    middleLabel = middleLabel,
                                    contactNames = uiState.contactNames,
                                )
                                level - 1 == ratings.size -> PendingRating(calculating = true)
                                else -> PendingRating(calculating = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A public key field with a button under it that fills it from the contact list.
 * Below the field it names the contact with that key, or says there is none.
 */
@Composable
private fun ContactKeyField(
    label: String,
    value: String,
    contact: ContactRecord?,
    contacts: List<ContactRecord>,
    onValueChange: (String) -> Unit,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val isUnknown = value.isNotBlank() && contact == null
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = margin)) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            singleLine = true,
            isError = isUnknown,
            supportingText = when {
                contact != null -> {
                    { Text(contactLabel(contact)) }
                }
                isUnknown -> {
                    { Text(stringResource(id = R.string.graph_inspection_unknown_key)) }
                }
                else -> null
            },
            modifier = Modifier.fillMaxWidth(),
        )
        FieldSourceButton(
            icon = Icons.Filled.Contacts,
            text = stringResource(id = R.string.new_gratitude_from_contacts),
            onClick = { showPicker = true },
            modifier = Modifier.padding(top = margin / 2),
        )
    }
    if (showPicker) {
        ContactPickerDialog(
            title = stringResource(id = R.string.graph_inspection_pick_contact),
            contacts = contacts,
            onSelect = { picked ->
                onValueChange(picked.publicKey)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

/**
 * Slider for how many handshake levels to calculate, 0..[MAX_HANDSHAKE_DEPTH].
 * [onDepthChange] is called when the user lets go of the slider, not on every step of a drag.
 */
@Composable
private fun HandshakeDepthSlider(depth: Int, onDepthChange: (Int) -> Unit) {
    var value by remember(depth) { mutableFloatStateOf(depth.toFloat()) }
    val selected = value.roundToInt()
    Field(label = stringResource(id = R.string.handshake_depth_title)) {
        Text(
            text = if (selected == 0) {
                stringResource(id = R.string.handshake_depth_off)
            } else {
                pluralStringResource(id = R.plurals.handshake_depth_value, count = selected, selected)
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = value,
            onValueChange = { value = it },
            onValueChangeFinished = { onDepthChange(value.roundToInt()) },
            valueRange = 0f..MAX_HANDSHAKE_DEPTH.toFloat(),
            // Stops between the ends, so every integer is a stop.
            steps = MAX_HANDSHAKE_DEPTH - 1,
        )
        Text(
            text = stringResource(id = R.string.handshake_depth_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The stars received minus given at one handshake level, the received and given stars,
 * and the paths behind them.
 */
@Composable
private fun ReviewExchangeTable(
    exchange: ReviewExchange,
    handshakes: Int,
    middleLabel: String,
    contactNames: Map<Int, String?>,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    val difference = exchange.difference
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = margin / 2),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GoldStar(modifier = Modifier.size(32.dp))
            Text(
                text = formatDifference(difference),
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    difference > 0 -> MaterialTheme.colorScheme.secondary
                    difference < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
        DetailLine(stringResource(id = R.string.contact_reviews_received_count, exchange.received))
        DetailLine(stringResource(id = R.string.contact_reviews_given_count, exchange.given))
        HandshakePathDiagram(
            received = exchange.receivedPath,
            given = exchange.givenPath,
            handshakes = handshakes,
            lineColor = GoldStarColor,
            middleLabel = middleLabel,
            labelOf = { id -> displayNameOrUnknown(contactNames[id]) },
            modifier = Modifier.padding(top = 8.dp),
        )
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

/**
 * "One handshake rating", "Two handshakes rating", ...; digits past the named levels.
 * Followed by one handshake emoji per handshake.
 */
@Composable
private fun handshakeTitle(level: Int): String {
    val named = stringArrayResource(id = R.array.handshake_rating_titles)
    val title = named.getOrNull(level - 1) ?: stringResource(id = R.string.handshake_rating_title_n, level)
    return "$title ${HANDSHAKE_EMOJI.repeat(level)}"
}

private const val HANDSHAKE_EMOJI = "🤝"

private fun formatDifference(value: Int): String = when {
    value > 0 -> "+$value"
    value < 0 -> "−${-value}"
    else -> "0"
}

@Preview
@Composable
private fun GraphInspectionScreenPreview() {
    fun contact(id: Int, name: String?) =
        ContactRecord(id = id, publicKey = "MCowBQYDK2VwAyEA$id", name = name, isFavorite = false, isIgnored = false)
    val contacts = listOf(contact(1, "Alice"), contact(2, "Bob"), contact(3, null), contact(9, "Me"))
    Surface {
        GraphInspectionContent(
            uiState = GraphInspectionUiState(
                middleKey = contacts[3].publicKey,
                endKey = contacts[0].publicKey,
                middleContact = contacts[3],
                endContact = contacts[0],
                contacts = contacts,
                handshakeRatings = listOf(
                    ReviewExchange(
                        receivedPath = HandshakePath(listOf(1, 9), listOf(6)),
                        givenPath = HandshakePath(listOf(9, 1), listOf(3)),
                    ),
                    ReviewExchange(
                        receivedPath = HandshakePath(listOf(1, 2, 9), listOf(4, 2)),
                        givenPath = HandshakePath(listOf(9, 3, 1), listOf(2, 5)),
                    ),
                ),
                handshakeDepth = 3,
            ),
            openDrawer = {},
            onBack = null,
            onMiddleKeyChange = {},
            onEndKeyChange = {},
            onHandshakeDepthChange = {},
        )
    }
}
