package io.github.hdghg.sevenhandshakes.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
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
                navigateToExample = { navigationActions.navigateToFeed(fromDrawer = true) },
                navigateToContacts = { navigationActions.navigateToContacts() },
                navigateToIdentities = { navigationActions.navigateToIdentities() },
                navigateToExport = { navigationActions.navigateToExport() },
                navigateToImport = { navigationActions.navigateToImport() },
                navigateToGraphInspection = { navigationActions.navigateToGraphInspection() },
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
    navigateToExample: () -> Unit,
    navigateToContacts: () -> Unit,
    navigateToIdentities: () -> Unit,
    navigateToExport: () -> Unit,
    navigateToImport: () -> Unit,
    navigateToGraphInspection: () -> Unit,
    closeDrawer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(modifier = modifier.fillMaxSize()) {
            DrawerHeader()
            DrawerButton(
                label = stringResource(id = R.string.feed_title),
                isSelected = currentRoute == MyAppDestinations.FEED_ROUTE,
                action = {
                    navigateToExample()
                    closeDrawer()
                },
            )
            DrawerButton(
                label = stringResource(id = R.string.contacts_title),
                isSelected = currentRoute == MyAppDestinations.CONTACTS_ROUTE,
                action = {
                    navigateToContacts()
                    closeDrawer()
                },
            )
            DrawerButton(
                label = stringResource(id = R.string.identities_title),
                isSelected = currentRoute == MyAppDestinations.IDENTITIES_ROUTE,
                action = {
                    navigateToIdentities()
                    closeDrawer()
                },
            )
            val otherRoutes = setOf(
                MyAppDestinations.EXPORT_ROUTE,
                MyAppDestinations.IMPORT_ROUTE,
                MyAppDestinations.GRAPH_INSPECTION_ROUTE,
            )
            var otherExpanded by rememberSaveable { mutableStateOf(currentRoute in otherRoutes) }
            DrawerButton(
                label = stringResource(id = R.string.drawer_other),
                isSelected = !otherExpanded && currentRoute in otherRoutes,
                icon = if (otherExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                action = { otherExpanded = !otherExpanded },
            )
            AnimatedVisibility(visible = otherExpanded) {
                Column(modifier = Modifier.padding(start = dimensionResource(id = R.dimen.horizontal_margin))) {
                    DrawerButton(
                        label = stringResource(id = R.string.graph_inspection_title),
                        isSelected = currentRoute == MyAppDestinations.GRAPH_INSPECTION_ROUTE,
                        action = {
                            navigateToGraphInspection()
                            closeDrawer()
                        },
                    )
                    DrawerButton(
                        label = stringResource(id = R.string.export_title),
                        isSelected = currentRoute == MyAppDestinations.EXPORT_ROUTE,
                        action = {
                            navigateToExport()
                            closeDrawer()
                        },
                    )
                    DrawerButton(
                        label = stringResource(id = R.string.import_title),
                        isSelected = currentRoute == MyAppDestinations.IMPORT_ROUTE,
                        action = {
                            navigateToImport()
                            closeDrawer()
                        },
                    )
                }
            }
        }
    }
}

/**
 * The app's picture, as on its store page: the ring of seven handshakes in a wider network.
 * Fills the drawer's width and keeps its proportions; the top lies under the status bar.
 */
@Composable
private fun DrawerHeader(modifier: Modifier = Modifier) {
    val header = painterResource(id = R.drawable.drawer_header)
    Image(
        painter = header,
        contentDescription = stringResource(id = R.string.app_name),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(header.intrinsicSize.width / header.intrinsicSize.height),
    )
}

@Composable
private fun DrawerButton(
    label: String,
    isSelected: Boolean,
    action: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Add,
) {
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
                imageVector = icon,
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
