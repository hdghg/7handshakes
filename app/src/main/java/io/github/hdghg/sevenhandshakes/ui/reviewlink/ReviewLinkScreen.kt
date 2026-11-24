package io.github.hdghg.sevenhandshakes.ui.reviewlink

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.hdghg.sevenhandshakes.R
import io.github.hdghg.sevenhandshakes.data.Review
import io.github.hdghg.sevenhandshakes.ui.ReviewDetailTopAppBar
import io.github.hdghg.sevenhandshakes.ui.reviewdetail.ReviewDetailBody

/**
 * Shown when a review link is opened: the review it holds, and a button to save it.
 * Once the review is saved, or if the app has it already, [openReview] is called with its id.
 */
@Composable
fun ReviewLinkScreen(
    onBack: () -> Unit,
    openReview: (reviewId: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewLinkViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.savedReviewId) {
        uiState.savedReviewId?.let(openReview)
    }
    ReviewLinkContent(
        // A review the app has is shown by its own screen, opened above.
        review = uiState.review.takeIf { uiState.savedReviewId == null },
        isLinkInvalid = uiState.isLinkInvalid,
        isSaving = uiState.isSaving,
        onSave = viewModel::save,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun ReviewLinkContent(
    review: Review?,
    isLinkInvalid: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val margin = dimensionResource(id = R.dimen.horizontal_margin)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { ReviewDetailTopAppBar(onBack = onBack) },
    ) { paddingValues ->
        if (review == null) {
            if (isLinkInvalid) {
                Text(
                    text = stringResource(id = R.string.review_link_invalid),
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(all = margin),
                )
            }
            return@Scaffold
        }
        ReviewDetailBody(review = review, modifier = Modifier.padding(paddingValues)) {
            Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.review_link_save))
            }
        }
    }
}

@Preview
@Composable
private fun ReviewLinkScreenPreview() {
    Surface {
        ReviewLinkContent(
            review = Review(
                id = 0,
                uuid = "0190a6c8-7b1e-7c3a-9d2f-1a2b3c4d5e6f",
                text = "Helped me move the piano up three floors.",
                rate = 3,
                sourceName = null,
                targetName = "Alice",
                createdAt = 1_720_000_000_000,
            ),
            isLinkInvalid = false,
            isSaving = false,
            onSave = {},
            onBack = {},
        )
    }
}
