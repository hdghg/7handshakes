package io.github.hdghg.sevenhandshakes.ui.reviewdetail

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.data.ReviewDirection
import io.github.hdghg.sevenhandshakes.data.rateLevelOf
import io.github.hdghg.sevenhandshakes.ui.Field
import io.github.hdghg.sevenhandshakes.ui.LinkQrDialog
import io.github.hdghg.sevenhandshakes.ui.GoldStar
import io.github.hdghg.sevenhandshakes.ui.ShareActions
import io.github.hdghg.sevenhandshakes.ui.ReviewDetailTopAppBar
import io.github.hdghg.sevenhandshakes.ui.copyLinkAsRichText
import io.github.hdghg.sevenhandshakes.ui.displayNameOrUnknown
import io.github.hdghg.sevenhandshakes.ui.rateDescriptionOf
import io.github.hdghg.sevenhandshakes.ui.reviewDirectionColor
import io.github.hdghg.sevenhandshakes.ui.shareAsFile
import io.github.hdghg.sevenhandshakes.ui.shareLinkAsText
import io.github.hdghg.sevenhandshakes.util.UuidV7

@Composable
fun ReviewDetailScreen(
    onBack: () -> Unit,
    openContact: (contactId: Int) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewDetailViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteReviewDialog(
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteReview()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
    val context = LocalContext.current
    val shareLink = uiState.shareLink
    // What a link copied as formatted text reads: just "Review".
    val shareLabel = stringResource(id = R.string.review_details)
    var showQr by rememberSaveable { mutableStateOf(false) }
    if (showQr && shareLink != null) {
        LinkQrDialog(
            title = stringResource(id = R.string.review_details),
            link = shareLink,
            qrDescription = stringResource(id = R.string.review_qr_description),
            onDismiss = { showQr = false },
        )
    }
    ReviewDetailContent(
        review = uiState.review,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onGiverClick = uiState.giverContactId?.let { id -> { openContact(id) } },
        onReceiverClick = uiState.receiverContactId?.let { id -> { openContact(id) } },
        onDelete = { if (uiState.review != null) showDeleteDialog = true },
        share = shareLink?.let { link ->
            ShareActions(
                asQr = { showQr = true },
                asLink = { shareLinkAsText(context, link) },
                asRichText = { copyLinkAsRichText(context, link, shareLabel) },
                asFile = ::shareAsFile,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun ReviewDetailContent(
    review: Review?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onGiverClick: (() -> Unit)?,
    onReceiverClick: (() -> Unit)?,
    onDelete: () -> Unit,
    share: ShareActions?,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ReviewDetailTopAppBar(onBack = onBack, share = share, onDelete = if (review != null) onDelete else null)
        },
    ) { paddingValues ->
        if (review == null) {
            if (!isLoading) {
                Text(
                    text = stringResource(id = R.string.review_not_found),
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(all = margin),
                )
            }
            return@Scaffold
        }
        ReviewDetailBody(
            review = review,
            onGiverClick = onGiverClick,
            onReceiverClick = onReceiverClick,
            modifier = Modifier.padding(paddingValues),
        )
    }
}

/**
 * The fields of [review] in a scrolling column, with [footer] below them.
 * [onGiverClick] and [onReceiverClick], when set, make that contact's row tappable.
 */
@Composable
internal fun ReviewDetailBody(
    review: Review,
    modifier: Modifier = Modifier,
    onGiverClick: (() -> Unit)? = null,
    onReceiverClick: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(all = margin),
    ) {
        Field(label = stringResource(id = R.string.review_date_time)) {
            Text(text = dateTimeLabel(review.createdAt), style = MaterialTheme.typography.bodyLarge)
        }
        // The giver parts with as many stars as the review is rated, and the receiver gains them.
        val stars = rateLevelOf(review.rate)
        Field(label = stringResource(id = R.string.review_giver)) {
            Participant(
                name = displayNameOrUnknown(review.sourceName),
                starChange = "−$stars",
                starChangeColor = MaterialTheme.colorScheme.error,
                rate = review.rate,
                // Same colors as the feed: orange for the side that gives, green for the side that receives.
                background = reviewDirectionColor(ReviewDirection.Outgoing),
                onClick = onGiverClick,
            )
        }
        Field(label = stringResource(id = R.string.review_receiver)) {
            Participant(
                name = displayNameOrUnknown(review.targetName),
                starChange = "+$stars",
                starChangeColor = MaterialTheme.colorScheme.secondary,
                rate = review.rate,
                background = reviewDirectionColor(ReviewDirection.Incoming),
                onClick = onReceiverClick,
            )
        }
        Field(label = stringResource(id = R.string.review_text)) {
            SelectionContainer {
                Text(text = review.text, style = MaterialTheme.typography.bodyLarge)
            }
        }
        footer()
    }
}

/** Date and time written the way the device's region prefers, or "Date unknown". */
@Composable
private fun dateTimeLabel(createdAt: Long?): String {
    if (createdAt == null) return stringResource(id = R.string.feed_unknown_date)
    return DateUtils.formatDateTime(
        LocalContext.current,
        createdAt,
        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or
            DateUtils.FORMAT_SHOW_YEAR or DateUtils.FORMAT_SHOW_TIME,
    )
}

/**
 * A contact's name, followed by the stars it gave away ("−2") or received ("+2"), on [background].
 * Like a feed item, the background has square corners and spans the whole screen width,
 * past the side margins of the column it is in.
 * With [onClick] the row is tappable and the name is drawn as a link, with a chevron after the star.
 */
@Composable
private fun Participant(
    name: String,
    starChange: String,
    starChangeColor: Color,
    rate: Byte,
    background: Color,
    onClick: (() -> Unit)? = null,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    val clickLabel = stringResource(id = R.string.review_open_contact)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .bleed(margin)
            .fillMaxWidth()
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = clickLabel, onClick = onClick) else Modifier)
            .padding(horizontal = margin, vertical = margin / 2),
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = starChange,
            style = MaterialTheme.typography.titleMedium,
            color = starChangeColor,
            modifier = Modifier.padding(start = margin),
        )
        // One star only: the number before it already says how many.
        GoldStar(contentDescription = stringResource(id = rateDescriptionOf(rate)))
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Widens the element by [horizontal] on both sides, over its parent's padding. */
private fun Modifier.bleed(horizontal: Dp): Modifier = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + extra,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout(placeable.width - extra, placeable.height) {
        placeable.place(-extra / 2, 0)
    }
}

@Composable
private fun DeleteReviewDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.delete_review_title)) },
        text = { Text(text = stringResource(id = R.string.delete_review_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(id = R.string.delete_contact_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = android.R.string.cancel)) }
        },
    )
}

@Preview
@Composable
fun ReviewDetailScreenPreview() {
    val createdAt = System.currentTimeMillis() - 3 * DateUtils.HOUR_IN_MILLIS
    Surface {
        ReviewDetailContent(
            review = Review(
                id = 1,
                uuid = UuidV7.generate(createdAt).toString(),
                text = "Helped me move the piano up three floors. Bra jobbat!",
                rate = 2,
                sourceName = "Me",
                targetName = "Alice",
                createdAt = createdAt,
            ),
            isLoading = false,
            onBack = {},
            onGiverClick = {},
            onReceiverClick = {},
            onDelete = {},
            share = ShareActions(asQr = {}, asLink = {}, asRichText = {}, asFile = {}),
        )
    }
}
