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

package app.tivi.home.followed

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.tivi.common.compose.Layout
import app.tivi.common.compose.LocalTiviTextCreator
import app.tivi.common.compose.bodyWidth
import app.tivi.common.compose.itemSpacer
import app.tivi.common.compose.itemsInGrid
import app.tivi.common.compose.rememberFlowWithLifecycle
import app.tivi.common.compose.theme.AppBarAlphas
import app.tivi.common.compose.ui.PosterCard
import app.tivi.common.compose.ui.RefreshButton
import app.tivi.common.compose.ui.SearchTextField
import app.tivi.common.compose.ui.SortMenuPopup
import app.tivi.common.compose.ui.SwipeDismissSnackbarHost
import app.tivi.common.compose.ui.UserProfileButton
import app.tivi.data.entities.ShowTmdbImage
import app.tivi.data.entities.SortOption
import app.tivi.data.entities.TiviShow
import app.tivi.data.entities.TraktUser
import app.tivi.data.resultentities.FollowedShowEntryWithShow
import app.tivi.trakt.TraktAuthState
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.Scaffold
import com.google.accompanist.insets.ui.TopAppBar
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.ramcosta.composedestinations.annotation.Destination

interface FollowedNavigator {
    fun openUser()

    fun openShowDetails(showId: Long)
}

@Destination
@Composable
fun Followed(
    navigator: FollowedNavigator
) {
    Followed(
        viewModel = hiltViewModel(),
        navigator = navigator
    )
}

@Composable
internal fun Followed(
    viewModel: FollowedViewModel,
    navigator: FollowedNavigator
) {
    val viewState by rememberFlowWithLifecycle(viewModel.state)
        .collectAsState(initial = FollowedViewState.Empty)
    val pagingItems = rememberFlowWithLifecycle(viewModel.pagedList)
        .collectAsLazyPagingItems()

    Followed(
        state = viewState,
        list = pagingItems,
        navigator = navigator,
        onMessageShown = { viewModel.clearMessage(it) },
        refresh = { viewModel.refresh() },
        onFilterChanged = { viewModel.setFilter(it) },
        onSortSelected = { viewModel.setSort(it) },
    )
}

@Composable
internal fun Followed(
    state: FollowedViewState,
    list: LazyPagingItems<FollowedShowEntryWithShow>,
    navigator: FollowedNavigator,
    onMessageShown: (id: Long) -> Unit,
    refresh: () -> Unit,
    onFilterChanged: (String) -> Unit,
    onSortSelected: (SortOption) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()

    state.message?.let { message ->
        LaunchedEffect(message) {
            scaffoldState.snackbarHostState.showSnackbar(message.message)
            // Notify the view model that the message has been dismissed
            onMessageShown(message.id)
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            FollowedAppBar(
                loggedIn = state.authState == TraktAuthState.LOGGED_IN,
                user = state.user,
                refreshing = state.isLoading,
                onRefreshActionClick = refresh,
                onUserActionClick = navigator::openUser,
                modifier = Modifier.fillMaxWidth()
            )
        },
        snackbarHost = { snackbarHostState ->
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .padding(horizontal = Layout.bodyMargin)
                    .fillMaxWidth()
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(state.isLoading),
            onRefresh = refresh,
            indicatorPadding = paddingValues,
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    scale = true
                )
            }
        ) {
            val columns = Layout.columns
            val bodyMargin = Layout.bodyMargin
            val gutter = Layout.gutter

            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier
                    .bodyWidth()
                    .fillMaxHeight()
            ) {
                item {
                    FilterSortPanel(
                        filterHint = stringResource(R.string.filter_shows, list.itemCount),
                        onFilterChanged = onFilterChanged,
                        sortOptions = state.availableSorts,
                        currentSortOption = state.sort,
                        onSortSelected = onSortSelected,
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(horizontal = bodyMargin)
                    )
                }

                itemsInGrid(
                    lazyPagingItems = list,
                    columns = columns / 4,
                    // We minus 8.dp off the grid padding, as we use content padding on the items below
                    contentPadding = PaddingValues(
                        horizontal = (bodyMargin - 8.dp).coerceAtLeast(0.dp),
                        vertical = (gutter - 8.dp).coerceAtLeast(0.dp),
                    ),
                    verticalItemPadding = (gutter - 8.dp).coerceAtLeast(0.dp),
                    horizontalItemPadding = (gutter - 8.dp).coerceAtLeast(0.dp),
                ) { entry ->
                    if (entry != null) {
                        FollowedShowItem(
                            show = entry.show,
                            poster = entry.poster,
                            watchedEpisodeCount = entry.stats?.watchedEpisodeCount ?: 0,
                            totalEpisodeCount = entry.stats?.episodeCount ?: 0,
                            onClick = { navigator.openShowDetails(entry.show.id) },
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    // TODO placeholder?
                }

                itemSpacer(16.dp)
            }
        }
    }
}

@Composable
private fun FilterSortPanel(
    filterHint: String,
    onFilterChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
    sortOptions: List<SortOption>,
    currentSortOption: SortOption,
    onSortSelected: (SortOption) -> Unit,
) {
    Row(modifier.padding(horizontal = Layout.bodyMargin, vertical = Layout.gutter)) {
        var filter by remember { mutableStateOf(TextFieldValue()) }

        SearchTextField(
            value = filter,
            onValueChange = { value ->
                filter = value
                onFilterChanged(value.text)
            },
            hint = filterHint,
            modifier = Modifier.weight(1f)
        )

        SortMenuPopup(
            sortOptions = sortOptions,
            currentSortOption = currentSortOption,
            onSortSelected = onSortSelected,
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = stringResource(R.string.cd_sort_list),
            )
        }
    }
}

@Composable
private fun FollowedShowItem(
    show: TiviShow,
    poster: ShowTmdbImage?,
    watchedEpisodeCount: Int,
    totalEpisodeCount: Int,
    onClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val textCreator = LocalTiviTextCreator.current
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(contentPadding)
    ) {
        PosterCard(
            show = show,
            poster = poster,
            modifier = Modifier
                .fillMaxWidth(0.2f) // 20% of the width
                .aspectRatio(2 / 3f)
        )

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = textCreator.showTitle(show = show).toString(),
                style = MaterialTheme.typography.subtitle1,
            )

            Spacer(Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = when {
                    totalEpisodeCount > 0 -> watchedEpisodeCount / totalEpisodeCount.toFloat()
                    else -> 0f
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(4.dp))

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(
                    text = textCreator.followedShowEpisodeWatchStatus(
                        episodeCount = totalEpisodeCount,
                        watchedEpisodeCount = watchedEpisodeCount
                    ).toString(),
                    style = MaterialTheme.typography.caption
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FollowedAppBar(
    loggedIn: Boolean,
    user: TraktUser?,
    refreshing: Boolean,
    onRefreshActionClick: () -> Unit,
    onUserActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        backgroundColor = MaterialTheme.colors.surface.copy(
            alpha = AppBarAlphas.translucentBarAlpha()
        ),
        contentColor = MaterialTheme.colors.onSurface,
        contentPadding = rememberInsetsPaddingValues(
            insets = LocalWindowInsets.current.systemBars,
            applyBottom = false,
        ),
        modifier = modifier,
        title = { Text(text = stringResource(R.string.following_shows_title)) },
        actions = {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                // This button refresh allows screen-readers, etc to trigger a refresh.
                // We only show the button to trigger a refresh, not to indicate that
                // we're currently refreshing, otherwise we have 4 indicators showing the
                // same thing.
                Crossfade(
                    targetState = refreshing,
                    modifier = Modifier.align(Alignment.CenterVertically)
                ) { isRefreshing ->
                    if (!isRefreshing) {
                        RefreshButton(onClick = onRefreshActionClick)
                    }
                }
            }

            UserProfileButton(
                loggedIn = loggedIn,
                user = user,
                onClick = onUserActionClick,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        },
    )
}
