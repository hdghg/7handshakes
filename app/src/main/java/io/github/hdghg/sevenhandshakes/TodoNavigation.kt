package io.github.hdghg.sevenhandshakes

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.CONTACT_ID_ARG
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.TASK_ID_ARG
import io.github.hdghg.sevenhandshakes.MyScreens.ADD_EDIT_TASK_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.ADD_GRATITUDE_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.TASK_DETAIL_SCREEN

/**
 * Screens used in [MyAppDestinations]
 */
private object MyScreens {
    const val TASKS_SCREEN = "tasks"
    const val FEED_SCREEN = "feed"
    const val TASK_DETAIL_SCREEN = "task"
    const val ADD_EDIT_TASK_SCREEN = "addEditTask"
    const val ADD_GRATITUDE_SCREEN = "addGratitude"
    const val CONTACTS_SCREEN = "contacts"
    const val ADD_CONTACT_SCREEN = "addContact"
    const val CONTACT_DETAIL_SCREEN = "contact"
    const val IDENTITIES_SCREEN = "identities"
    const val ADD_EDIT_IDENTITY_SCREEN = "addEditIdentity"
}

/**
 * Arguments used in [MyAppDestinations] routes
 */
object MyAppDestinationsArgs {
    const val USER_MESSAGE_ARG = "userMessage"
    const val TASK_ID_ARG = "taskId"
    const val TITLE_ARG = "title"
    const val CONTACT_ID_ARG = "contactId"
}

/**
 * Destinations used in the [MainActivity]
 */
object MyAppDestinations {
    const val FEED_ROUTE = MyScreens.FEED_SCREEN
    const val ADD_GRATITUDE_ROUTE = MyScreens.ADD_GRATITUDE_SCREEN
    const val CONTACTS_ROUTE = MyScreens.CONTACTS_SCREEN
    const val ADD_CONTACT_ROUTE = MyScreens.ADD_CONTACT_SCREEN
    const val CONTACT_DETAIL_ROUTE = "${MyScreens.CONTACT_DETAIL_SCREEN}/{$CONTACT_ID_ARG}"
    const val IDENTITIES_ROUTE = MyScreens.IDENTITIES_SCREEN
    const val ADD_EDIT_IDENTITY_ROUTE = MyScreens.ADD_EDIT_IDENTITY_SCREEN
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

    fun navigateToAddEditIdentity() {
        navController.navigate(MyAppDestinations.ADD_EDIT_IDENTITY_ROUTE)
    }

    fun navigateToAddContact() {
        navController.navigate(MyAppDestinations.ADD_CONTACT_ROUTE)
    }

    fun navigateToContactDetail(contactId: Int) {
        navController.navigate("${MyScreens.CONTACT_DETAIL_SCREEN}/$contactId")
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
            ADD_GRATITUDE_SCREEN,
        )
    }
}
