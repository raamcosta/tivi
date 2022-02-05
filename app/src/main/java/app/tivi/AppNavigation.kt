/*
 * Copyright 2021 Google LLC
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

package app.tivi

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import app.tivi.account.destinations.AccountUiDestination
import app.tivi.episodedetails.destinations.EpisodeDetailsDestination
import app.tivi.home.discover.destinations.DiscoverDestination
import app.tivi.home.followed.destinations.FollowedDestination
import app.tivi.home.navGraph
import app.tivi.home.popular.destinations.PopularShowsDestination
import app.tivi.home.recommended.destinations.RecommendedShowsDestination
import app.tivi.home.search.destinations.SearchDestination
import app.tivi.home.trending.destinations.TrendingShowsDestination
import app.tivi.home.watched.destinations.WatchedDestination
import app.tivi.showdetails.details.destinations.ShowDetailsDestination
import app.tivi.showdetails.seasons.destinations.ShowSeasonsDestination
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.animations.rememberAnimatedNavHostEngine
import com.ramcosta.composedestinations.dynamic.routedIn
import com.ramcosta.composedestinations.manualcomposablecalls.DestinationScope
import com.ramcosta.composedestinations.navigation.dependency
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.NavGraphSpec

object NavGraphs {

    val search = object: NavGraphSpec {
        override val route = "search"

        override val startRoute = SearchDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            SearchDestination,
            AccountUiDestination,
            ShowDetailsDestination,
            ShowSeasonsDestination,
            EpisodeDetailsDestination,
        ).routedIn(this)
            .associateBy { it.route }
    }

    val watched = object: NavGraphSpec {
        override val route = "watched"

        override val startRoute = WatchedDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            WatchedDestination,
            AccountUiDestination,
            ShowDetailsDestination,
            ShowSeasonsDestination,
            EpisodeDetailsDestination,
        ).routedIn(this)
            .associateBy { it.route }
    }

    val following = object: NavGraphSpec {
        override val route = "following"

        override val startRoute = FollowedDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            FollowedDestination,
            AccountUiDestination,
            ShowDetailsDestination,
            ShowSeasonsDestination,
            EpisodeDetailsDestination,
        ).routedIn(this)
            .associateBy { it.route }
    }

    val discover = object: NavGraphSpec {
        override val route = "discover"

        override val startRoute = DiscoverDestination routedIn this

        override val destinationsByRoute = listOf<DestinationSpec<*>>(
            DiscoverDestination,
            AccountUiDestination,
            ShowDetailsDestination,
            ShowSeasonsDestination,
            EpisodeDetailsDestination,
            RecommendedShowsDestination,
            TrendingShowsDestination,
            PopularShowsDestination,
        ).routedIn(this)
            .associateBy { it.route }
    }

    val root = object: NavGraphSpec {
        override val route = "root"
        override val startRoute = discover
        override val destinationsByRoute = emptyMap<String, DestinationSpec<*>>()
        override val nestedNavGraphs = listOf(
            discover,
            following,
            watched,
            search
        )
    }
}

fun ArrayDeque<NavBackStackEntry>.print(prefix: String = "stack") {
    val stack = toMutableList()
        .map { it.destination.route }
        .toTypedArray().contentToString()
    println("$prefix = $stack")
}

fun DestinationScope<*>.currentNavigator(openSettings: () -> Unit): CommonNavGraphNavigator{
    return CommonNavGraphNavigator(
        navBackStackEntry.destination.navGraph(),
        navController,
        openSettings
    )
}

@OptIn(ExperimentalMaterialNavigationApi::class)
@ExperimentalAnimationApi
@Composable
internal fun AppNavigation(
    navController: NavHostController,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DestinationsNavHost(
        engine = rememberAnimatedNavHostEngine(
            rootDefaultAnimations = RootNavGraphDefaultAnimations(
                enterTransition = { defaultTiviEnterTransition(initialState, targetState) },
                exitTransition = { defaultTiviExitTransition(initialState, targetState) },
                popEnterTransition = { defaultTiviPopEnterTransition() },
                popExitTransition = { defaultTiviPopExitTransition() },
            )
        ),
        navController = navController,
        navGraph = NavGraphs.root,
        modifier = modifier,
        dependenciesContainerBuilder = {
            dependency(currentNavigator(onOpenSettings))
        }
    )
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultTiviEnterTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): EnterTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeIn()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.Start)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultTiviExitTransition(
    initial: NavBackStackEntry,
    target: NavBackStackEntry,
): ExitTransition {
    val initialNavGraph = initial.destination.hostNavGraph
    val targetNavGraph = target.destination.hostNavGraph
    // If we're crossing nav graphs (bottom navigation graphs), we crossfade
    if (initialNavGraph.id != targetNavGraph.id) {
        return fadeOut()
    }
    // Otherwise we're in the same nav graph, we can imply a direction
    return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.Start)
}

private val NavDestination.hostNavGraph: NavGraph
    get() = hierarchy.first { it is NavGraph } as NavGraph

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultTiviPopEnterTransition(): EnterTransition {
    return fadeIn() + slideIntoContainer(AnimatedContentScope.SlideDirection.End)
}

@ExperimentalAnimationApi
private fun AnimatedContentScope<*>.defaultTiviPopExitTransition(): ExitTransition {
    return fadeOut() + slideOutOfContainer(AnimatedContentScope.SlideDirection.End)
}
