package io.github.hdghg.sevenhandshakes.ui.reviewdetail

import android.text.format.DateUtils
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.ui.Field
import io.github.hdghg.sevenhandshakes.ui.LinkQrDialog
import io.github.hdghg.sevenhandshakes.ui.RateStars
import io.github.hdghg.sevenhandshakes.ui.ShareActions
import io.github.hdghg.sevenhandshakes.ui.ReviewDetailTopAppBar
import io.github.hdghg.sevenhandshakes.ui.copyLinkAsRichText
import io.github.hdghg.sevenhandshakes.ui.displayNameOrUnknown
import io.github.hdghg.sevenhandshakes.ui.shareAsFile
import io.github.hdghg.sevenhandshakes.ui.shareLinkAsText
import io.github.hdghg.sevenhandshakes.util.UuidV7

/** The giver parts with one star and the receiver gains one. */
private const val STAR_GIVEN = "−1"
private const val STAR_RECEIVED = "+1"

@Composable
fun ReviewDetailScreen(
    onBack: () -> Unit,
    openContact: (contactId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    share: ShareActions?,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ReviewDetailTopAppBar(onBack = onBack, share = share) },
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
        Field(label = stringResource(id = R.string.review_giver)) {
            Participant(
                name = displayNameOrUnknown(review.sourceName),
                starChange = STAR_GIVEN,
                starChangeColor = MaterialTheme.colorScheme.error,
                rate = review.rate,
                onClick = onGiverClick,
            )
        }
        Field(label = stringResource(id = R.string.review_receiver)) {
            Participant(
                name = displayNameOrUnknown(review.targetName),
                starChange = STAR_RECEIVED,
                starChangeColor = MaterialTheme.colorScheme.secondary,
                rate = review.rate,
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
 * A contact's name, followed by the star it gave away ("−1") or received ("+1").
 * With [onClick] the row is tappable and the name is drawn as a link, with a chevron after the star.
 */
@Composable
private fun Participant(
    name: String,
    starChange: String,
    starChangeColor: Color,
    rate: Byte,
    onClick: (() -> Unit)? = null,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    val clickLabel = stringResource(id = R.string.review_open_contact)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = clickLabel, onClick = onClick) else Modifier)
            .padding(vertical = 4.dp),
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
        RateStars(rate = rate)
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
            share = ShareActions(asQr = {}, asLink = {}, asRichText = {}, asFile = {}),
        )
    }
}
