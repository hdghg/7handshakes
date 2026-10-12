package io.github.hdghg.sevenhandshakes

import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.hdghg.sevenhandshakes.ui.feed.FeedScreen
import io.github.hdghg.sevenhandshakes.ui.newgratitude.NewGratitudeScreen
import io.github.hdghg.sevenhandshakes.ui.statistics.StatisticsScreen
import io.github.hdghg.sevenhandshakes.ui.AppModalDrawer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(MyAppDestinations.STATISTICS_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                StatisticsScreen(openDrawer = { coroutineScope.launch { drawerState.open() } })
            }
        }
        composable(MyAppDestinations.FEED_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                FeedScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    newGratitude = { navActions.navigateToAddGratitude() },
                )
            }
        }
        composable(MyAppDestinations.ADD_GRATITUDE_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                NewGratitudeScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = {
                        navActions.navigateToFeed(fromDrawer = false)
                    },
                )
            }
        }
    }
}
