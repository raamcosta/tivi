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

package app.tivi.home.recommended

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import app.tivi.common.compose.EntryGrid
import com.ramcosta.composedestinations.annotation.Destination

interface RecommendedShowsNavigator {
    fun navigateUp()

    fun openShowDetails(showId: Long)
}

@Destination
@Composable
fun RecommendedShows(
    navigator: RecommendedShowsNavigator
) {
    RecommendedShows(
        viewModel = hiltViewModel(),
        navigator = navigator
    )
}

@Composable
internal fun RecommendedShows(
    viewModel: RecommendedShowsViewModel,
    navigator: RecommendedShowsNavigator
) {
    EntryGrid(
        lazyPagingItems = viewModel.pagedList.collectAsLazyPagingItems(),
        title = stringResource(R.string.discover_recommended_title),
        onOpenShowDetails = navigator::openShowDetails,
        onNavigateUp = navigator::navigateUp,
        modifier = Modifier.fillMaxSize()
    )
}
