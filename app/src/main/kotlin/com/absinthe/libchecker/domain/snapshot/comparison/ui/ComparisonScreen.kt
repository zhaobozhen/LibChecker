@file:Suppress("FunctionName")

package com.absinthe.libchecker.domain.snapshot.comparison.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardSideState
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardState
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemDisplayData
import com.absinthe.libchecker.domain.snapshot.model.SnapshotPackageIconSource
import com.absinthe.libchecker.ui.compose.LibCheckerTheme

internal data class ComparisonRouteState(
  val dashboard: ComparisonDashboardState,
  val items: List<ComparisonResultItem> = emptyList(),
  val isLoading: Boolean = false,
  val hasCompared: Boolean = false
)

internal data class ComparisonResultItem(
  val key: String,
  val displayData: SnapshotItemDisplayData,
  val onClickToken: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ComparisonRouteScreen(
  state: ComparisonRouteState,
  onNavigateUp: () -> Unit,
  onSelectSide: (SnapshotComparisonSide) -> Unit,
  onCompare: () -> Unit,
  onResultClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.album_item_comparison_title)) },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              painter = painterResource(R.drawable.ic_navigate_up),
              contentDescription = stringResource(R.string.navigate_up)
            )
          }
        },
        actions = {
          IconButton(
            onClick = onCompare,
            enabled = !state.isLoading
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_compare),
              contentDescription = stringResource(R.string.advanced_menu)
            )
          }
        }
      )
    }
  ) { contentPadding ->
    ComparisonScreen(
      state = state,
      onSelectSide = onSelectSide,
      onResultClick = onResultClick,
      modifier = Modifier.padding(contentPadding)
    )
  }
}

@Composable
internal fun ComparisonScreen(
  state: ComparisonRouteState,
  onSelectSide: (SnapshotComparisonSide) -> Unit,
  onResultClick: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  if (state.isLoading) {
    Box(
      modifier = modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      val loadingDescription = stringResource(R.string.loading)
      CircularProgressIndicator(
        modifier = Modifier.semantics { contentDescription = loadingDescription }
      )
    }
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      horizontal = dimensionResource(R.dimen.album_item_margin_horizontal),
      vertical = dimensionResource(R.dimen.album_item_margin_vertical)
    )
  ) {
    item(key = "dashboard") {
      ComparisonDashboard(
        state = state.dashboard,
        onSelectSide = onSelectSide
      )
    }
    if (state.hasCompared && state.items.isEmpty()) {
      item(key = "empty") {
        ComparisonEmptyState()
      }
    } else {
      items(state.items, key = ComparisonResultItem::key) { item ->
        ComparisonResultCard(
          data = item.displayData,
          onClick = { onResultClick(item.onClickToken) }
        )
      }
    }
  }
}

@Composable
private fun ComparisonDashboard(
  state: ComparisonDashboardState,
  onSelectSide: (SnapshotComparisonSide) -> Unit
) {
  OutlinedCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = dimensionResource(R.dimen.album_item_margin_vertical)),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(IntrinsicSize.Min)
        .padding(dimensionResource(R.dimen.normal_padding)),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ComparisonDashboardSide(
        state = state.left,
        horizontalAlignment = Alignment.Start,
        onClick = { onSelectSide(SnapshotComparisonSide.LEFT) },
        modifier = Modifier.weight(1f)
      )
      VerticalDivider(modifier = Modifier.fillMaxHeight())
      ComparisonDashboardSide(
        state = state.right,
        horizontalAlignment = Alignment.End,
        onClick = { onSelectSide(SnapshotComparisonSide.RIGHT) },
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun ComparisonDashboardSide(
  state: ComparisonDashboardSideState,
  horizontalAlignment: Alignment.Horizontal,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val interactionSource = androidx.compose.runtime.remember { MutableInteractionSource() }
  Column(
    modifier = modifier
      .semantics { contentDescription = state.contentDescription }
      .clickable(
        interactionSource = interactionSource,
        indication = ripple(bounded = false),
        onClick = onClick
      )
      .padding(vertical = dimensionResource(R.dimen.album_item_margin_vertical)),
    horizontalAlignment = horizontalAlignment
  ) {
    Text(
      text = stringResource(R.string.snapshot_current_timestamp),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = state.timestampText,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black),
      maxLines = 1,
      overflow = TextOverflow.MiddleEllipsis
    )
    Spacer(Modifier.size(dimensionResource(R.dimen.album_item_margin_vertical)))
    Text(
      text = stringResource(R.string.comparison_snapshot_apps_count),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
      text = state.appsCountText,
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Black)
    )
  }
}

@Composable
private fun ComparisonResultCard(
  data: SnapshotItemDisplayData,
  onClick: () -> Unit
) {
  val iconModel = when (val source = data.iconSource) {
    is SnapshotPackageIconSource.InstalledPackage -> source.packageInfo

    SnapshotPackageIconSource.Fallback,
    null -> R.drawable.ic_icon_blueprint
  }
  androidx.compose.material3.Card(
    modifier = Modifier
      .fillMaxWidth()
      .alpha(if (data.isDeleted) 0.7f else 1f)
      .semantics { contentDescription = data.contentDescription }
      .clickable(onClick = onClick),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RectangleShape
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(dimensionResource(R.dimen.main_card_padding)),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = iconModel,
        contentDescription = null,
        modifier = Modifier.size(dimensionResource(R.dimen.app_icon_size)),
        contentScale = ContentScale.Fit
      )
      Spacer(Modifier.width(dimensionResource(R.dimen.main_card_padding)))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(
          dimensionResource(R.dimen.album_item_margin_vertical)
        )
      ) {
        Text(
          text = data.appName.text.toString(),
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = data.packageName,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = buildList {
            add(data.versionInfo.toString())
            data.packageSize?.text?.toString()?.takeIf(String::isNotBlank)?.let(::add)
            data.apiText.toString().takeIf(String::isNotBlank)?.let(::add)
            data.abi.abiDisplayData.old.text.takeIf(String::isNotBlank)?.let(::add)
          }.joinToString(separator = "  "),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

@Composable
private fun ComparisonEmptyState() {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 96.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
  ) {
    Image(
      painter = painterResource(R.drawable.ic_notebook),
      contentDescription = null,
      modifier = Modifier.size(200.dp)
    )
    Text(
      text = stringResource(R.string.snapshot_empty_list_title),
      modifier = Modifier.offset(y = (-16).dp),
      style = MaterialTheme.typography.headlineSmall
    )
  }
}

@Preview(showBackground = true)
@Composable
private fun ComparisonScreenPreview() {
  val side = ComparisonDashboardSideState(
    timestampText = "2026-08-26 16:44",
    appsCountText = "415",
    contentDescription = "Snapshot, 415 apps"
  )
  LibCheckerTheme {
    ComparisonScreen(
      state = ComparisonRouteState(
        dashboard = ComparisonDashboardState(side, side)
      ),
      onSelectSide = {},
      onResultClick = {}
    )
  }
}
