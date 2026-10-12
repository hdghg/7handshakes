package io.github.hdghg.sevenhandshakes

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.TASK_ID_ARG
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.TITLE_ARG
import io.github.hdghg.sevenhandshakes.MyAppDestinationsArgs.USER_MESSAGE_ARG
import io.github.hdghg.sevenhandshakes.MyScreens.ADD_EDIT_TASK_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.ADD_GRATITUDE_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.STATISTICS_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.TASKS_SCREEN
import io.github.hdghg.sevenhandshakes.MyScreens.TASK_DETAIL_SCREEN

/**
 * Screens used in [MyAppDestinations]
 */
private object MyScreens {
    const val TASKS_SCREEN = "tasks"
    const val STATISTICS_SCREEN = "statistics"
    const val FEED_SCREEN = "feed"
    const val TASK_DETAIL_SCREEN = "task"
    const val ADD_EDIT_TASK_SCREEN = "addEditTask"
    const val ADD_GRATITUDE_SCREEN = "addGratitude"
}

/**
 * Arguments used in [MyAppDestinations] routes
 */
object MyAppDestinationsArgs {
    const val USER_MESSAGE_ARG = "userMessage"
    const val TASK_ID_ARG = "taskId"
    const val TITLE_ARG = "title"
}

/**
 * Destinations used in the [MainActivity]
 */
object MyAppDestinations {
    const val STATISTICS_ROUTE = STATISTICS_SCREEN
    const val FEED_ROUTE = MyScreens.FEED_SCREEN
    const val ADD_GRATITUDE_ROUTE = MyScreens.ADD_GRATITUDE_SCREEN
}

/**
 * Models the navigation actions in the app.
 */
class MyAppNavigationActions(private val navController: NavHostController) {
    fun navigateToStatistics() {
        navController.navigate(MyAppDestinations.STATISTICS_ROUTE) {
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
