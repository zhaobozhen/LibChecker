@file:Suppress("FunctionName")

package com.absinthe.libchecker.domain.snapshot.album.ui

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.absinthe.libchecker.R
import com.absinthe.libchecker.ui.compose.LibCheckerTheme
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import kotlinx.coroutines.CancellationException

internal enum class AlbumRoute(
  @StringRes val titleRes: Int
) {
  Home(R.string.title_album),
  Comparison(R.string.album_item_comparison_title),
  Management(R.string.album_item_management_title),
  BackupRestore(R.string.album_item_backup_restore_title),
  Track(R.string.album_item_track_title)
}

@Composable
internal fun AlbumRouteHost(
  route: AlbumRoute,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable (AlbumRoute) -> Unit
) {
  val predictiveBackProgress = remember { Animatable(0f) }
  var predictiveBackActive by remember { mutableStateOf(false) }
  var predictiveBackCommitted by remember { mutableStateOf(false) }
  var predictiveBackEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

  PredictiveBackHandler(enabled = route != AlbumRoute.Home) { backEvents ->
    try {
      predictiveBackActive = true
      backEvents.collect { backEvent ->
        predictiveBackEdge = backEvent.swipeEdge
        predictiveBackProgress.snapTo(backEvent.progress)
      }
      predictiveBackCommitted = true
      onNavigateHome()
    } catch (_: CancellationException) {
      predictiveBackProgress.animateTo(
        targetValue = 0f,
        animationSpec = tween(durationMillis = PREDICTIVE_BACK_CANCEL_DURATION_MILLIS)
      )
      predictiveBackActive = false
    }
  }

  LaunchedEffect(route) {
    if (route == AlbumRoute.Home && predictiveBackCommitted) {
      withFrameNanos { }
      predictiveBackProgress.snapTo(0f)
      predictiveBackActive = false
      predictiveBackCommitted = false
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    if (predictiveBackActive && route != AlbumRoute.Home) {
      content(AlbumRoute.Home)
    }
    AnimatedContent(
      targetState = route,
      transitionSpec = {
        when {
          predictiveBackCommitted && targetState == AlbumRoute.Home ->
            EnterTransition.None togetherWith ExitTransition.None

          initialState == AlbumRoute.Home ->
            (
              slideInHorizontally(
                animationSpec = tween(ROUTE_ANIMATION_DURATION_MILLIS),
                initialOffsetX = { width -> width }
              ) + fadeIn(tween(ROUTE_FADE_DURATION_MILLIS))
              ) togetherWith
              (
                slideOutHorizontally(
                  animationSpec = tween(ROUTE_ANIMATION_DURATION_MILLIS),
                  targetOffsetX = { width -> -width / ROUTE_PARALLAX_DIVISOR }
                ) + fadeOut(tween(ROUTE_FADE_DURATION_MILLIS))
                )

          targetState == AlbumRoute.Home ->
            (
              slideInHorizontally(
                animationSpec = tween(ROUTE_ANIMATION_DURATION_MILLIS),
                initialOffsetX = { width -> -width / ROUTE_PARALLAX_DIVISOR }
              ) + fadeIn(tween(ROUTE_FADE_DURATION_MILLIS))
              ) togetherWith
              (
                slideOutHorizontally(
                  animationSpec = tween(ROUTE_ANIMATION_DURATION_MILLIS),
                  targetOffsetX = { width -> width }
                ) + fadeOut(tween(ROUTE_FADE_DURATION_MILLIS))
                )

          else -> EnterTransition.None togetherWith ExitTransition.None
        }.using(SizeTransform(clip = false))
      },
      label = "album-route"
    ) { targetRoute ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            if (predictiveBackActive && targetRoute != AlbumRoute.Home) {
              val direction = if (predictiveBackEdge == BackEventCompat.EDGE_RIGHT) -1f else 1f
              translationX = size.width * predictiveBackProgress.value * direction
            }
          }
      ) {
        content(targetRoute)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlbumHomeRoute(
  onNavigate: (AlbumRoute) -> Unit,
  onNavigateUp: () -> Unit,
  modifier: Modifier = Modifier
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
      TopAppBar(
        title = { Text(stringResource(R.string.title_album)) },
        navigationIcon = {
          IconButton(onClick = onNavigateUp) {
            Icon(
              painter = painterResource(R.drawable.ic_navigate_up),
              contentDescription = stringResource(R.string.navigate_up)
            )
          }
        }
      )
    }
  ) { contentPadding ->
    AlbumHomeScreen(
      onNavigate = onNavigate,
      modifier = Modifier.padding(contentPadding)
    )
  }
}

@Composable
internal fun AlbumHomeScreen(
  onNavigate: (AlbumRoute) -> Unit,
  modifier: Modifier = Modifier
) {
  val isDarkMode = isSystemInDarkTheme()
  val items = listOf(
    AlbumEntry(
      iconRes = R.drawable.ic_compare,
      iconBackgroundColor = colorResource(
        if (isDarkMode) R.color.material_red_900 else R.color.material_red_300
      ),
      titleRes = R.string.album_item_comparison_title,
      subtitleRes = R.string.album_item_comparison_subtitle,
      route = AlbumRoute.Comparison
    ),
    AlbumEntry(
      iconRes = R.drawable.ic_manage,
      iconBackgroundColor = colorResource(
        if (isDarkMode) R.color.material_blue_900 else R.color.material_blue_300
      ),
      titleRes = R.string.album_item_management_title,
      subtitleRes = R.string.album_item_management_subtitle,
      route = AlbumRoute.Management
    ),
    AlbumEntry(
      iconRes = R.drawable.ic_backup,
      iconBackgroundColor = colorResource(
        if (isDarkMode) R.color.material_green_900 else R.color.material_green_300
      ),
      titleRes = R.string.album_item_backup_restore_title,
      subtitleRes = R.string.album_item_backup_restore_subtitle,
      route = AlbumRoute.BackupRestore
    ),
    AlbumEntry(
      iconRes = R.drawable.ic_track,
      iconBackgroundColor = colorResource(
        if (isDarkMode) R.color.material_orange_900 else R.color.material_orange_300
      ),
      titleRes = R.string.album_item_track_title,
      subtitleRes = R.string.album_item_track_subtitle,
      route = AlbumRoute.Track
    )
  )

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(
      start = dimensionResource(R.dimen.album_item_margin_horizontal),
      top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
      end = dimensionResource(R.dimen.album_item_margin_horizontal),
      bottom = dimensionResource(R.dimen.album_item_margin_vertical)
    )
  ) {
    items(items, key = AlbumEntry::route) { item ->
      AlbumEntryCard(
        entry = item,
        onClick = { onNavigate(item.route) }
      )
    }
  }
}

@Composable
private fun AlbumEntryCard(
  entry: AlbumEntry,
  onClick: () -> Unit
) {
  val title = stringResource(entry.titleRes)
  val subtitle = stringResource(entry.subtitleRes)
  val description = listOf(title, subtitle).joinToString()
  val context = LocalContext.current
  val iconTint = Color(context.getColorByAttr(android.R.attr.colorControlNormal))

  OutlinedCard(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = dimensionResource(R.dimen.album_item_margin_vertical))
      .semantics { contentDescription = description }
      .clickable(onClick = onClick),
    colors = CardDefaults.outlinedCardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ),
    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = dimensionResource(R.dimen.album_card_inset_horizontal),
          vertical = dimensionResource(R.dimen.album_card_inset_vertical)
        ),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(dimensionResource(R.dimen.album_card_icon_size))
          .background(entry.iconBackgroundColor, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          painter = painterResource(entry.iconRes),
          contentDescription = null,
          modifier = Modifier.size(21.dp),
          tint = iconTint
        )
      }
      Spacer(Modifier.width(dimensionResource(R.dimen.album_card_inset_horizontal)))
      Column(
        verticalArrangement = Arrangement.spacedBy(
          dimensionResource(R.dimen.album_item_margin_vertical)
        )
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}

private data class AlbumEntry(
  @DrawableRes val iconRes: Int,
  val iconBackgroundColor: Color,
  @StringRes val titleRes: Int,
  @StringRes val subtitleRes: Int,
  val route: AlbumRoute
)

private const val ROUTE_ANIMATION_DURATION_MILLIS = 300
private const val ROUTE_FADE_DURATION_MILLIS = 150
private const val ROUTE_PARALLAX_DIVISOR = 4
private const val PREDICTIVE_BACK_CANCEL_DURATION_MILLIS = 180

@Preview(showBackground = true)
@Composable
private fun AlbumHomeScreenPreview() {
  LibCheckerTheme {
    AlbumHomeScreen(onNavigate = {})
  }
}
