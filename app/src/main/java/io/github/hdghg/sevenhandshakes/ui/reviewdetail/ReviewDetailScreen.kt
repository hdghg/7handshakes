package io.github.hdghg.sevenhandshakes.ui.reviewdetail

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.ui.QrCode
import io.github.hdghg.sevenhandshakes.ui.RateStar
import io.github.hdghg.sevenhandshakes.ui.ReviewDetailTopAppBar
import io.github.hdghg.sevenhandshakes.ui.displayNameOrUnknown
import io.github.hdghg.sevenhandshakes.util.UuidV7
import timber.log.Timber

/** The giver parts with one star and the receiver gains one. */
private const val STAR_GIVEN = "−1"
private const val STAR_RECEIVED = "+1"

@Composable
fun ReviewDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val shareLink = uiState.shareLink
    var showQr by rememberSaveable { mutableStateOf(false) }
    if (showQr && shareLink != null) {
        ReviewQrDialog(link = shareLink, onDismiss = { showQr = false })
    }
    ReviewDetailContent(
        review = uiState.review,
        isLoading = uiState.isLoading,
        onBack = onBack,
        onShare = shareLink?.let { link -> { shareLink(context, link) } },
        onShareQr = shareLink?.let { { showQr = true } },
        modifier = modifier,
    )
}

/** Shows [link] as a QR code to scan on another device; it opens there as the link itself would. */
@Composable
private fun ReviewQrDialog(link: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.review_details)) },
        text = {
            QrCode(
                content = link,
                contentDescription = stringResource(id = R.string.review_qr_description),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.close)) }
        },
    )
}

/** Opens the system share menu for [link], sent as plain text. */
private fun shareLink(context: Context, link: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, link)
    }
    try {
        context.startActivity(Intent.createChooser(send, null))
    } catch (e: ActivityNotFoundException) {
        Timber.w(e, "No app to share a review link with")
    }
}

@Composable
private fun ReviewDetailContent(
    review: Review?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onShare: (() -> Unit)?,
    onShareQr: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ReviewDetailTopAppBar(onBack = onBack, onShare = onShare, onShareQr = onShareQr) },
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
        ReviewDetailBody(review = review, modifier = Modifier.padding(paddingValues))
    }
}

/** The fields of [review] in a scrolling column, with [footer] below them. */
@Composable
internal fun ReviewDetailBody(
    review: Review,
    modifier: Modifier = Modifier,
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
            )
        }
        Field(label = stringResource(id = R.string.review_receiver)) {
            Participant(
                name = displayNameOrUnknown(review.targetName),
                starChange = STAR_RECEIVED,
                starChangeColor = MaterialTheme.colorScheme.secondary,
                rate = review.rate,
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

/** A contact's name, followed by the star it gave away ("−1") or received ("+1"). */
@Composable
private fun Participant(name: String, starChange: String, starChangeColor: Color, rate: Byte) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = starChange,
            style = MaterialTheme.typography.titleMedium,
            color = starChangeColor,
            modifier = Modifier.padding(start = margin),
        )
        RateStar(rate = rate)
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
            onShare = {},
            onShareQr = {},
        )
    }
}
