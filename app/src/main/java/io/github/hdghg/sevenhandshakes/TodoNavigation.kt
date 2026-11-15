package io.github.hdghg.sevenhandshakes

import android.net.Uri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.CONTACT_ID_ARG
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.FILE_URI_ARG
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.REVIEW_ID_ARG
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.TASK_ID_ARG
import io.github.hdghg.sevenhandshakes.MyScreens.ADD_EDIT_TASK_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.TASK_DETAIL_SCREEN

/**
 * Screens used in [MyAppDestinations]
 */
private object MyScreens {
    const val FEED_SCREEN = "feed"
    const val TASK_DETAIL_SCREEN = "task"
    const val ADD_EDIT_TASK_SCREEN = "addEditTask"
    const val ADD_GRATITUDE_SCREEN = "addGratitude"
    const val CONTACTS_SCREEN = "contacts"
    const val ADD_EDIT_CONTACT_SCREEN = "addEditContact"
    const val CONTACT_DETAIL_SCREEN = "contact"
    const val REVIEW_DETAIL_SCREEN = "review"
    const val IDENTITIES_SCREEN = "identities"
    const val ADD_EDIT_IDENTITY_SCREEN = "addEditIdentity"
    const val EXPORT_SCREEN = "export"
    const val IMPORT_SCREEN = "import"
    const val IMPORT_SELECTION_SCREEN = "importSelection"
}

/**
 * Arguments used in [MyAppDestinations] routes
 */
object MyAppDestinationsArgs {
    const val TASK_ID_ARG = "taskId"
    const val CONTACT_ID_ARG = "contactId"

    /** Id of a review in the review table. */
    const val REVIEW_ID_ARG = "reviewId"

    /** Content uri of the file picked for import. */
    const val FILE_URI_ARG = "fileUri"
}

/**
 * Destinations used in the [MainActivity]
 */
object MyAppDestinations {
    const val FEED_ROUTE = MyScreens.FEED_SCREEN
    const val ADD_GRATITUDE_ROUTE = MyScreens.ADD_GRATITUDE_SCREEN
    const val CONTACTS_ROUTE = MyScreens.CONTACTS_SCREEN

    /** Adds a contact, or edits the one with the optional [CONTACT_ID_ARG]. */
    const val ADD_EDIT_CONTACT_ROUTE = "${MyScreens.ADD_EDIT_CONTACT_SCREEN}?$CONTACT_ID_ARG={$CONTACT_ID_ARG}"
    const val CONTACT_DETAIL_ROUTE = "${MyScreens.CONTACT_DETAIL_SCREEN}/{$CONTACT_ID_ARG}"
    const val REVIEW_DETAIL_ROUTE = "${MyScreens.REVIEW_DETAIL_SCREEN}/{$REVIEW_ID_ARG}"
    const val IDENTITIES_ROUTE = MyScreens.IDENTITIES_SCREEN
    const val ADD_EDIT_IDENTITY_ROUTE = MyScreens.ADD_EDIT_IDENTITY_SCREEN
    const val EXPORT_ROUTE = MyScreens.EXPORT_SCREEN
    const val IMPORT_ROUTE = MyScreens.IMPORT_SCREEN
    const val IMPORT_SELECTION_ROUTE = "${MyScreens.IMPORT_SELECTION_SCREEN}?$FILE_URI_ARG={$FILE_URI_ARG}"
}

/**
 * Models the navigation actions in the app.
 */
class MyAppNavigationActions(private val navController: NavHostController) {
    fun navigateToContacts() {
        navController.navigate(MyAppDestinations.CONTACTS_ROUTE) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
    }

    fun navigateToIdentities() {
        navController.navigate(MyAppDestinations.IDENTITIES_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToExport() {
        navController.navigate(MyAppDestinations.EXPORT_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToImport() {
        navController.navigate(MyAppDestinations.IMPORT_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToImportSelection(fileUri: Uri) {
        navController.navigate("${MyScreens.IMPORT_SELECTION_SCREEN}?$FILE_URI_ARG=${Uri.encode(fileUri.toString())}")
    }

    fun navigateToAddEditIdentity() {
        navController.navigate(MyAppDestinations.ADD_EDIT_IDENTITY_ROUTE)
    }

    fun navigateToAddContact() {
        navController.navigate(MyScreens.ADD_EDIT_CONTACT_SCREEN)
    }

    fun navigateToEditContact(contactId: Int) {
        navController.navigate("${MyScreens.ADD_EDIT_CONTACT_SCREEN}?$CONTACT_ID_ARG=$contactId")
    }

    fun navigateToContactDetail(contactId: Int) {
        navController.navigate("${MyScreens.CONTACT_DETAIL_SCREEN}/$contactId")
    }

    fun navigateToReviewDetail(reviewId: Int) {
        navController.navigate("${MyScreens.REVIEW_DETAIL_SCREEN}/$reviewId")
    }

    fun navigateToFeed(fromDrawer: Boolean) {
        navController.navigate(MyAppDestinations.FEED_ROUTE) {
            popUpTo(navController.graph.findStartDestination().id) {
                inclusive = !fromDrawer
                saveState = fromDrawer
            }
            launchSingleTop = true
            restoreState = fromDrawer
        }
    }

    fun navigateToTaskDetail(taskId: String) {
        navController.navigate("$TASK_DETAIL_SCREEN/$taskId")
    }

    fun navigateToAddEditTask(title: Int, taskId: String?) {
        navController.navigate(
            "$ADD_EDIT_TASK_SCREEN/$title".let {
                if (taskId != null) "$it?$TASK_ID_ARG=$taskId" else it
            },
        )
    }

    fun navigateToAddGratitude() {
        navController.navigate(
            MyScreens.ADD_GRATITUDE_SCREEN,
        )
    }
}
