package io.github.hdghg.sevenhandshakes

import android.net.Uri
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.hdghg.sevenhandshakes.data.share.ContactLink
import io.github.hdghg.sevenhandshakes.ui.contactdetail.ContactDetailScreen
import io.github.hdghg.sevenhandshakes.ui.contactlink.ContactLinkScreen
import io.github.hdghg.sevenhandshakes.ui.contacts.ContactsScreen
import io.github.hdghg.sevenhandshakes.ui.export.ExportScreen
import io.github.hdghg.sevenhandshakes.ui.feed.FeedScreen
import io.github.hdghg.sevenhandshakes.ui.graphinspection.GraphInspectionScreen
import io.github.hdghg.sevenhandshakes.ui.identities.IdentitiesScreen
import io.github.hdghg.sevenhandshakes.ui.importdata.ImportScreen
import io.github.hdghg.sevenhandshakes.ui.importselection.ImportSelectionScreen
import io.github.hdghg.sevenhandshakes.ui.newcontact.NO_CONTACT_ID
import io.github.hdghg.sevenhandshakes.ui.newcontact.NewContactScreen
import io.github.hdghg.sevenhandshakes.ui.newgratitude.NewGratitudeScreen
import io.github.hdghg.sevenhandshakes.ui.reviewdetail.ReviewDetailScreen
import io.github.hdghg.sevenhandshakes.ui.reviewlink.ReviewLinkScreen
import io.github.hdghg.sevenhandshakes.ui.AppModalDrawer
import io.github.hdghg.sevenhandshakes.ui.addeditidentity.AddEditIdentityScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun AppMainNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    startDestination: String = MyAppDestinations.FEED_ROUTE,
    navActions: MyAppNavigationActions = remember(navController) {
        MyAppNavigationActions(navController)
    },
    /** A file another app asked to open; the import screen is opened for it. */
    fileToImport: Uri? = null,
    /** Called once the import screen for [fileToImport] is open, so it is not opened twice. */
    onFileToImportOpened: () -> Unit = {},
    /** A review or contact link (see Hs7Link) another app asked to open; its screen is opened for it. */
    linkToOpen: String? = null,
    /** Called once the screen for [linkToOpen] is open, so it is not opened twice. */
    onLinkOpened: () -> Unit = {},
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination
    LaunchedEffect(currentRoute) { Timber.d("Screen: %s", currentRoute) }

    // One drawer for the whole app, so its close animation survives the screen change it started.
    AppModalDrawer(drawerState, currentRoute, navActions, coroutineScope) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
        ) {
            composable(MyAppDestinations.FEED_ROUTE) {
                FeedScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    newGratitude = { navActions.navigateToAddGratitude() },
                    openReview = { review -> navActions.navigateToReviewDetail(review.id) },
                )
            }
            composable(
                MyAppDestinations.REVIEW_DETAIL_ROUTE,
                arguments = listOf(navArgument(MyAppDestinationsArgs.REVIEW_ID_ARG) { type = NavType.IntType }),
            ) {
                ReviewDetailScreen(
                    onBack = { navController.popBackStack() },
                    openContact = { contactId -> navActions.navigateToContactDetail(contactId) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                MyAppDestinations.REVIEW_LINK_ROUTE,
                arguments = listOf(navArgument(MyAppDestinationsArgs.REVIEW_LINK_ARG) { type = NavType.StringType }),
            ) {
                ReviewLinkScreen(
                    onBack = { navController.popBackStack() },
                    openReview = { reviewId -> navActions.navigateFromReviewLinkToDetail(reviewId) },
                )
            }
            composable(MyAppDestinations.CONTACTS_ROUTE) {
                ContactsScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    newContact = { navActions.navigateToAddContact() },
                    openContact = { contact -> navActions.navigateToContactDetail(contact.id) },
                )
            }
            composable(
                MyAppDestinations.CONTACT_DETAIL_ROUTE,
                arguments = listOf(navArgument(MyAppDestinationsArgs.CONTACT_ID_ARG) { type = NavType.IntType }),
            ) {
                ContactDetailScreen(
                    onBack = { navController.popBackStack() },
                    onEdit = { contactId -> navActions.navigateToEditContact(contactId) },
                    onInspectConnection = { publicKey -> navActions.navigateToGraphInspection(publicKey) },
                    onDeleted = { navController.popBackStack() },
                )
            }
            composable(
                MyAppDestinations.CONTACT_LINK_ROUTE,
                arguments = listOf(navArgument(MyAppDestinationsArgs.CONTACT_LINK_ARG) { type = NavType.StringType }),
            ) {
                ContactLinkScreen(
                    onBack = { navController.popBackStack() },
                    openContact = { contactId -> navActions.navigateFromContactLinkToDetail(contactId) },
                )
            }
            composable(MyAppDestinations.IDENTITIES_ROUTE) {
                IdentitiesScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    addIdentity = { navActions.navigateToAddEditIdentity() },
                    // Identities is a drawer destination, so the feed is reached as from the drawer.
                    openFeed = { navActions.navigateToFeed(fromDrawer = true) },
                )
            }
            composable(MyAppDestinations.EXPORT_ROUTE) {
                ExportScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                )
            }
            composable(MyAppDestinations.IMPORT_ROUTE) {
                ImportScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    openImportSelection = { fileUri -> navActions.navigateToImportSelection(fileUri) },
                )
            }
            composable(
                MyAppDestinations.GRAPH_INSPECTION_ROUTE,
                arguments = listOf(
                    navArgument(MyAppDestinationsArgs.END_KEY_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                // Opened from a contact it sits on top of that screen and goes back to it; from the drawer it is top level.
                val fromContact = backStackEntry.arguments?.getString(MyAppDestinationsArgs.END_KEY_ARG) != null
                GraphInspectionScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    onBack = if (fromContact) {
                        { navController.popBackStack() }
                    } else {
                        null
                    },
                )
            }
            composable(
                MyAppDestinations.IMPORT_SELECTION_ROUTE,
                arguments = listOf(navArgument(MyAppDestinationsArgs.FILE_URI_ARG) { type = NavType.StringType }),
            ) {
                ImportSelectionScreen(onBack = { navController.popBackStack() })
            }
            composable(MyAppDestinations.ADD_EDIT_IDENTITY_ROUTE) {
                AddEditIdentityScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
                )
            }
            composable(
                MyAppDestinations.ADD_EDIT_CONTACT_ROUTE,
                arguments = listOf(
                    navArgument(MyAppDestinationsArgs.CONTACT_ID_ARG) {
                        type = NavType.IntType
                        defaultValue = NO_CONTACT_ID
                    },
                ),
            ) {
                NewContactScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
                )
            }
            composable(MyAppDestinations.ADD_GRATITUDE_ROUTE) {
                NewGratitudeScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        navActions.navigateToFeed(fromDrawer = false)
                    },
                )
            }
        }
    }

    // Placed after NavHost, which sets up the graph that navigating needs.
    LaunchedEffect(fileToImport) {
        if (fileToImport != null) {
            navActions.navigateToImportSelection(fileToImport)
            onFileToImportOpened()
        }
    }
    LaunchedEffect(linkToOpen) {
        if (linkToOpen != null) {
            if (ContactLink.isContactLink(linkToOpen)) {
                navActions.navigateToContactLink(linkToOpen)
            } else {
                // Anything else is shown as a review link, which says so if it holds no review.
                navActions.navigateToReviewLink(linkToOpen)
            }
            onLinkOpened()
        }
    }
}
