package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.hdghg.sevenhandshakes.MyAppDestinations
import io.github.hdghg.sevenhandshakes.MyAppNavigationActions
import io.github.hdghg.sevenhandshakes.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun AppModalDrawer(
    drawerState: DrawerState,
    currentRoute: String,
    navigationActions: MyAppNavigationActions,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                currentRoute = currentRoute,
                navigateToStatistics = { navigationActions.navigateToStatistics() },
                navigateToExample = { navigationActions.navigateToFeed(fromDrawer = true) },
                closeDrawer = { coroutineScope.launch { drawerState.close() } },
            )
        },
    ) {
        content()
    }
}

@Composable
private fun AppDrawer(
    currentRoute: String,
    navigateToStatistics: () -> Unit,
    navigateToExample: () -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = modifier.fillMaxSize()) {
            DrawerHeader()
            DrawerButton(
                label = stringResource(id = R.string.statistics_title),
                isSelected = currentRoute == MyAppDestinations.STATISTICS_ROUTE,
                action = {
                    navigateToStatistics()
                    closeDrawer()
                },
            )
            DrawerButton(
                label = stringResource(id = R.string.feed_title),
                isSelected = currentRoute == MyAppDestinations.FEED_ROUTE,
                action = {
                    navigateToExample()
                    closeDrawer()
                },
            )
        }
    }
}

@Composable
private fun DrawerHeader(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(primaryDarkColor)
            .height(dimensionResource(id = R.dimen.header_height))
            .padding(dimensionResource(id = R.dimen.header_padding)),
    ) {
        Image(
            imageVector = Icons.Filled.Edit,
            contentDescription =
                stringResource(id = R.string.tasks_header_image_content_description),
            modifier = Modifier.width(dimensionResource(id = R.dimen.header_image_width)),
        )
        Text(
            text = stringResource(id = R.string.navigation_view_header_title),
            color = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun DrawerButton(label: String, isSelected: Boolean, action: () -> Unit, modifier: Modifier = Modifier) {
    val tintColor = if (isSelected) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    TextButton(
        onClick = action,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.horizontal_margin)),
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null, // decorative
                tint = tintColor,
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = tintColor,
            )
        }
    }
}
