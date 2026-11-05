package io.github.hdghg.sevenhandshakes

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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.hdghg.sevenhandshakes.ui.contacts.ContactsScreen
import io.github.hdghg.sevenhandshakes.ui.feed.FeedScreen
import io.github.hdghg.sevenhandshakes.ui.identities.IdentitiesScreen
import io.github.hdghg.sevenhandshakes.ui.newcontact.NewContactScreen
import io.github.hdghg.sevenhandshakes.ui.newgratitude.NewGratitudeScreen
import io.github.hdghg.sevenhandshakes.ui.AppModalDrawer
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
) {
    val currentNavBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentNavBackStackEntry?.destination?.route ?: startDestination
    LaunchedEffect(currentRoute) { Timber.d("Screen: %s", currentRoute) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable(MyAppDestinations.FEED_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                FeedScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    newGratitude = { navActions.navigateToAddGratitude() },
                )
            }
        }
        composable(MyAppDestinations.CONTACTS_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                ContactsScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                    newContact = { navActions.navigateToAddContact() },
                )
            }
        }
        composable(MyAppDestinations.IDENTITIES_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                IdentitiesScreen(
                    openDrawer = { coroutineScope.launch { drawerState.open() } },
                )
            }
        }
        composable(MyAppDestinations.ADD_CONTACT_ROUTE) {
            AppModalDrawer(drawerState, currentRoute, navActions) {
                NewContactScreen(
                    onBack = { navController.popBackStack() },
                    onComplete = { navController.popBackStack() },
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
