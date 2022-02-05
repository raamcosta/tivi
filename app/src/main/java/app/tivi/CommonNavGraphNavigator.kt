/*
 * Copyright 2022 Google LLC
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

import androidx.navigation.NavController
import app.tivi.account.AccountUiNavigator
import app.tivi.account.destinations.AccountUiDestination
import app.tivi.episodedetails.EpisodeDetailsNavigator
import app.tivi.episodedetails.destinations.EpisodeDetailsDestination
import app.tivi.home.discover.DiscoverNavigator
import app.tivi.home.followed.FollowedNavigator
import app.tivi.home.popular.PopularShowsNavigator
import app.tivi.home.popular.destinations.PopularShowsDestination
import app.tivi.home.recommended.RecommendedShowsNavigator
import app.tivi.home.recommended.destinations.RecommendedShowsDestination
import app.tivi.home.search.SearchNavigator
import app.tivi.home.trending.TrendingShowsNavigator
import app.tivi.home.trending.destinations.TrendingShowsDestination
import app.tivi.home.watched.WatchedNavigator
import app.tivi.showdetails.details.ShowDetailsNavigator
import app.tivi.showdetails.details.destinations.ShowDetailsDestination
import app.tivi.showdetails.seasons.ShowSeasonsNavigator
import app.tivi.showdetails.seasons.destinations.ShowSeasonsDestination
import com.ramcosta.composedestinations.dynamic.within
import com.ramcosta.composedestinations.navigation.navigateTo
import com.ramcosta.composedestinations.spec.NavGraphSpec

class CommonNavGraphNavigator(
    private val navGraph: NavGraphSpec,
    private val navController: NavController,
    private val openSettings: () -> Unit
) : RecommendedShowsNavigator,
    ShowSeasonsNavigator,
    ShowDetailsNavigator,
    DiscoverNavigator,
    TrendingShowsNavigator,
    PopularShowsNavigator,
    FollowedNavigator,
    WatchedNavigator,
    SearchNavigator,
    AccountUiNavigator,
    EpisodeDetailsNavigator {

    override fun openSettings() {
        openSettings.invoke()
    }

    override fun openTrendingShows() {
        navController.navigateTo(TrendingShowsDestination within navGraph)
    }

    override fun openPopularShows() {
        navController.navigateTo(PopularShowsDestination within navGraph)
    }

    override fun openRecommendedShows() {
        navController.navigateTo(RecommendedShowsDestination within navGraph)
    }

    override fun openShowDetails(showId: Long, seasonId: Long?, episodeId: Long?) {
        navController.navigateTo(ShowDetailsDestination(showId = showId) within navGraph)

        // If we have an season id, we also open that
        if (seasonId != null) {
            navController.navigateTo(
                ShowSeasonsDestination(
                    showId = showId,
                    seasonId = seasonId.toString()
                ) within navGraph
            )
        }
        // If we have an episodeId, we also open that
        if (episodeId != null) {
            navController.navigateTo(EpisodeDetailsDestination(episodeId = episodeId) within navGraph)
        }
    }

    override fun navigateUp() {
        navController.navigateUp()
    }

    override fun openEpisodeDetails(episodeId: Long) {
        navController.navigateTo(EpisodeDetailsDestination(episodeId = episodeId) within navGraph)
    }

    override fun openUser() {
        navController.navigateTo(AccountUiDestination within navGraph)
    }

    override fun openShowDetails(showId: Long) {
        navController.navigateTo(ShowDetailsDestination(showId = showId) within navGraph)
    }

    override fun openSeasons(showId: Long, seasonId: Long) {
        navController.navigateTo(
            ShowSeasonsDestination(
                showId = showId,
                seasonId = seasonId.toString()
            ) within navGraph
        )
    }
}