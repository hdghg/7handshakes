package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import io.github.hdghg.sevenhandshakes.R

/** One value of a detail screen: a small [label] with [content] under it. */
@Composable
fun Field(label: String, content: @Composable () -> Unit) {
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
