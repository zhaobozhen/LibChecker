@file:Suppress("FunctionName")

package com.absinthe.libchecker.domain.snapshot.track.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.track.model.TrackedAppListItem
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackListUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackRouteScreen(
  state: TrackListUiState,
  onQueryChanged: (String) -> Unit,
  onTrackedChange: (String, Boolean) -> Unit,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isSearchActive by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  val searchFocusRequester = remember { FocusRequester() }
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

  fun closeSearch() {
    isSearchActive = false
    query = ""
  }

  BackHandler(enabled = isSearchActive, onBack = ::closeSearch)
  LaunchedEffect(query) {
    onQueryChanged(query)
  }
  LaunchedEffect(isSearchActive) {
    if (isSearchActive) {
      searchFocusRequester.requestFocus()
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = {
          if (isSearchActive) {
            TextField(
              value = query,
              onValueChange = { newQuery ->
                query = newQuery
              },
              modifier = Modifier
                .fillMaxWidth()
                .focusRequester(searchFocusRequester),
              placeholder = {
                Text(text = stringResource(R.string.search_hint))
              },
              singleLine = true,
              colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
              )
            )
          } else {
            Text(text = stringResource(R.string.album_item_track_title))
          }
        },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              painter = painterResource(R.drawable.ic_navigate_up),
              contentDescription = stringResource(R.string.navigate_up)
            )
          }
        },
        actions = {
          if (isSearchActive) {
            IconButton(onClick = ::closeSearch) {
              Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.close_search)
              )
            }
          } else if (state.isSearchVisible) {
            IconButton(onClick = { isSearchActive = true }) {
              Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = stringResource(R.string.menu_search)
              )
            }
          }
        },
        scrollBehavior = scrollBehavior
      )
    }
  ) { contentPadding ->
    val layoutDirection = LocalLayoutDirection.current
    TrackScreen(
      state = state,
      onTrackedChange = onTrackedChange,
      modifier = Modifier.padding(
        start = contentPadding.calculateStartPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateEndPadding(layoutDirection)
      )
    )
  }
}

@Composable
internal fun TrackScreen(
  state: TrackListUiState,
  onTrackedChange: (String, Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    when {
      state.isLoading -> TrackLoadingState()

      state.items.isEmpty() -> TrackEmptyState()

      else -> TrackList(
        items = state.items,
        onTrackedChange = onTrackedChange
      )
    }
  }
}

@Composable
private fun TrackLoadingState() {
  val loadingDescription = stringResource(R.string.loading)
  CircularProgressIndicator(
    modifier = Modifier.semantics {
      contentDescription = loadingDescription
    }
  )
}

@Composable
private fun TrackEmptyState() {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Image(
      painter = painterResource(R.drawable.ic_empty_list),
      contentDescription = null,
      modifier = Modifier.size(160.dp),
      contentScale = ContentScale.Fit
    )
    Text(
      text = stringResource(R.string.empty_list),
      style = MaterialTheme.typography.headlineSmall
    )
  }
}

@Composable
private fun TrackList(
  items: List<TrackedAppListItem>,
  onTrackedChange: (String, Boolean) -> Unit
) {
  val listState = rememberLazyListState()
  val navigationBarPadding =
    WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    state = listState,
    contentPadding = PaddingValues(bottom = navigationBarPadding)
  ) {
    items(
      items = items,
      key = TrackedAppListItem::packageName
    ) { item ->
      TrackListItem(
        item = item,
        onTrackedChange = onTrackedChange
      )
    }
  }
}

@Composable
private fun TrackListItem(
  item: TrackedAppListItem,
  onTrackedChange: (String, Boolean) -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clickable {
          onTrackedChange(item.packageName, !item.switchState)
        }
        .padding(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = item.packageInfo,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
        contentScale = ContentScale.Fit
      )
      Spacer(modifier = Modifier.width(8.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = item.label,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = item.packageName,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
      Checkbox(
        checked = item.switchState,
        onCheckedChange = { checked ->
          onTrackedChange(item.packageName, checked)
        },
        modifier = Modifier.semantics {
          contentDescription = item.description
        }
      )
    }
  }
}
