/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.tivi.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.material.NavigationRailDefaults
import androidx.compose.material.NavigationRailItem
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import app.tivi.AppNavigation
import app.tivi.NavGraphs
import app.tivi.R
import app.tivi.common.compose.theme.AppBarAlphas
import app.tivi.debugLabel
import app.tivi.print
import app.tivi.util.Analytics
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.BottomNavigation
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.ramcosta.composedestinations.navigation.navigateTo
import com.ramcosta.composedestinations.spec.NavGraphSpec
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterialApi::class)
@Composable
internal fun Home(
    analytics: Analytics,
    onOpenSettings: () -> Unit,
) {
    val navController = rememberAnimatedNavController()

    // Launch an effect to track changes to the current back stack entry, and push them
    // as a screen views to analytics
    LaunchedEffect(navController, analytics) {
        navController.currentBackStackEntryFlow.collect { entry ->
            println("debugLabel ${entry.debugLabel}")

            analytics.trackScreenView(
                label = entry.debugLabel,
                route = entry.destination.route,
                arguments = entry.arguments,
            )
        }
    }

    val configuration = LocalConfiguration.current
    val useBottomNavigation by remember {
        derivedStateOf { configuration.smallestScreenWidthDp < 600 }
    }

    Scaffold(
        bottomBar = {
            if (useBottomNavigation) {
                val currentSelectedItem by navController.currentScreenAsState()
                HomeBottomNavigation(
                    selectedNavigation = currentSelectedItem,
                    onNavigationSelected = { selected ->
                        navController.navigateTo(selected) {
                            launchSingleTop = true
                            restoreState = true

                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Spacer(
                    Modifier
                        .navigationBarsHeight()
                        .fillMaxWidth()
                )
            }
        }
    ) {
        Row(Modifier.fillMaxSize()) {
            if (!useBottomNavigation) {
                val currentSelectedItem by navController.currentScreenAsState()
                HomeNavigationRail(
                    selectedNavigation = currentSelectedItem,
                    onNavigationSelected = { selected ->
                        navController.navigate(selected.route) {
                            launchSingleTop = true
                            restoreState = true

                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxHeight(),
                )

                Divider(
                    Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
            }

            AppNavigation(
                navController = navController,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/**
 * Adds an [NavController.OnDestinationChangedListener] to this [NavController] and updates the
 * returned [State] which is updated as the destination changes.
 */
@Stable
@Composable
private fun NavController.currentScreenAsState(): State<NavGraphSpec> {
    val selectedItem = remember { mutableStateOf(NavGraphs.discover) }

    DisposableEffect(this) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->

            backQueue.print()

            selectedItem.value = destination.navGraph()
        }
        addOnDestinationChangedListener(listener)

        onDispose {
            removeOnDestinationChangedListener(listener)
        }
    }

    return selectedItem
}

fun NavDestination.navGraph(): NavGraphSpec {
    hierarchy.forEach { destination ->
        NavGraphs.root.nestedNavGraphs.forEach { navGraph ->
            if (destination.route == navGraph.route) {
                return navGraph
            }
        }
    }

    throw RuntimeException("Unknown nav graph for destination $route")
}

@Composable
internal fun HomeBottomNavigation(
    selectedNavigation: NavGraphSpec,
    onNavigationSelected: (NavGraphSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomNavigation(
        backgroundColor = MaterialTheme.colors.surface.copy(alpha = AppBarAlphas.translucentBarAlpha()),
        contentColor = contentColorFor(MaterialTheme.colors.surface),
        contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars),
        modifier = modifier
    ) {
        HomeNavigationItems.forEach { item ->
            BottomNavigationItem(
                icon = {
                    HomeNavigationItemIcon(
                        item = item,
                        selected = selectedNavigation == item.screen
                    )
                },
                label = { Text(text = stringResource(item.labelResId)) },
                selected = selectedNavigation == item.screen,
                onClick = { onNavigationSelected(item.screen) },
            )
        }
    }
}

@ExperimentalMaterialApi
@Composable
internal fun HomeNavigationRail(
    selectedNavigation: NavGraphSpec,
    onNavigationSelected: (NavGraphSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colors.surface,
        elevation = NavigationRailDefaults.Elevation,
        modifier = modifier,
    ) {
        NavigationRail(
            backgroundColor = Color.Transparent,
            contentColor = MaterialTheme.colors.onSurface,
            elevation = 0.dp,
            modifier = Modifier.padding(
                rememberInsetsPaddingValues(
                    LocalWindowInsets.current.systemBars,
                    applyEnd = false
                )
            )
        ) {
            HomeNavigationItems.forEach { item ->
                NavigationRailItem(
                    icon = {
                        HomeNavigationItemIcon(
                            item = item,
                            selected = selectedNavigation == item.screen
                        )
                    },
                    alwaysShowLabel = false,
                    label = { Text(text = stringResource(item.labelResId)) },
                    selected = selectedNavigation == item.screen,
                    onClick = { onNavigationSelected(item.screen) },
                )
            }
        }
    }
}

@Composable
private fun HomeNavigationItemIcon(item: HomeNavigationItem, selected: Boolean) {
    val painter = when (item) {
        is HomeNavigationItem.ResourceIcon -> painterResource(item.iconResId)
        is HomeNavigationItem.ImageVectorIcon -> rememberVectorPainter(item.iconImageVector)
    }
    val selectedPainter = when (item) {
        is HomeNavigationItem.ResourceIcon -> item.selectedIconResId?.let { painterResource(it) }
        is HomeNavigationItem.ImageVectorIcon -> item.selectedImageVector?.let { rememberVectorPainter(it) }
    }

    if (selectedPainter != null) {
        Crossfade(targetState = selected) {
            Icon(
                painter = if (it) selectedPainter else painter,
                contentDescription = stringResource(item.contentDescriptionResId),
            )
        }
    } else {
        Icon(
            painter = painter,
            contentDescription = stringResource(item.contentDescriptionResId),
        )
    }
}

private sealed class HomeNavigationItem(
    val screen: NavGraphSpec,
    @StringRes val labelResId: Int,
    @StringRes val contentDescriptionResId: Int,
) {
    class ResourceIcon(
        screen: NavGraphSpec,
        @StringRes labelResId: Int,
        @StringRes contentDescriptionResId: Int,
        @DrawableRes val iconResId: Int,
        @DrawableRes val selectedIconResId: Int? = null,
    ) : HomeNavigationItem(screen, labelResId, contentDescriptionResId)

    class ImageVectorIcon(
        screen: NavGraphSpec,
        @StringRes labelResId: Int,
        @StringRes contentDescriptionResId: Int,
        val iconImageVector: ImageVector,
        val selectedImageVector: ImageVector? = null,
    ) : HomeNavigationItem(screen, labelResId, contentDescriptionResId)
}

private val HomeNavigationItems = listOf(
    HomeNavigationItem.ImageVectorIcon(
        screen = NavGraphs.discover,
        labelResId = R.string.discover_title,
        contentDescriptionResId = R.string.cd_discover_title,
        iconImageVector = Icons.Outlined.Weekend,
        selectedImageVector = Icons.Default.Weekend,
    ),
    HomeNavigationItem.ImageVectorIcon(
        screen = NavGraphs.following,
        labelResId = R.string.following_shows_title,
        contentDescriptionResId = R.string.cd_following_shows_title,
        iconImageVector = Icons.Default.FavoriteBorder,
        selectedImageVector = Icons.Default.Favorite,
    ),
    HomeNavigationItem.ImageVectorIcon(
        screen = NavGraphs.watched,
        labelResId = R.string.watched_shows_title,
        contentDescriptionResId = R.string.cd_watched_shows_title,
        iconImageVector = Icons.Outlined.Visibility,
        selectedImageVector = Icons.Default.Visibility,
    ),
    HomeNavigationItem.ImageVectorIcon(
        screen = NavGraphs.search,
        labelResId = R.string.search_navigation_title,
        contentDescriptionResId = R.string.cd_search_navigation_title,
        iconImageVector = Icons.Default.Search,
    ),
)
