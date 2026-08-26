package com.absinthe.libchecker.domain.snapshot.album.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.database.backup.RoomBackup
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.home.ui.MainActivity
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotBackupBottomSheetDialogFragment
import com.absinthe.libchecker.domain.snapshot.backup.ui.SnapshotRoomBackupOwner
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardLabels
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardState
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonPlan
import com.absinthe.libchecker.domain.snapshot.comparison.model.SnapshotComparisonSide
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.ComparisonShareIntentParser
import com.absinthe.libchecker.domain.snapshot.comparison.presentation.SnapshotComparisonViewModel
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonResultItem
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonRouteScreen
import com.absinthe.libchecker.domain.snapshot.comparison.ui.ComparisonRouteState
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_ENTITY
import com.absinthe.libchecker.domain.snapshot.detail.ui.EXTRA_ICON
import com.absinthe.libchecker.domain.snapshot.detail.ui.SnapshotDetailActivity
import com.absinthe.libchecker.domain.snapshot.list.model.SnapshotItemCardPresentation
import com.absinthe.libchecker.domain.snapshot.list.presentation.SnapshotViewModel
import com.absinthe.libchecker.domain.snapshot.list.usecase.BuildSnapshotItemDisplayDataUseCase
import com.absinthe.libchecker.domain.snapshot.list.usecase.GetSnapshotPackageIconSourcesUseCase
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.domain.snapshot.timenode.ui.TimeNodeBottomSheetDialogFragment
import com.absinthe.libchecker.domain.snapshot.track.presentation.TrackViewModel
import com.absinthe.libchecker.domain.snapshot.track.ui.TrackRouteScreen
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.ui.base.BaseComposeActivity
import com.absinthe.libchecker.ui.compose.LibCheckerTheme
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.addBackStateHandler
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.requireAvailableCacheDir
import com.absinthe.libchecker.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class AlbumActivity :
  BaseComposeActivity(),
  SnapshotRoomBackupOwner {

  private val snapshotViewModel: SnapshotViewModel by viewModel()
  private val comparisonViewModel: SnapshotComparisonViewModel by viewModel()
  private val trackViewModel: TrackViewModel by viewModel()
  private val buildSnapshotItemDisplayData: BuildSnapshotItemDisplayDataUseCase by inject()
  private val getSnapshotPackageIconSources: GetSnapshotPackageIconSourcesUseCase by inject()

  override val snapshotRoomBackup = RoomBackup(this)

  private var route by mutableStateOf(AlbumRoute.Home)
  private var comparisonState by mutableStateOf<ComparisonRouteState?>(null)
  private var comparisonItems = emptyList<SnapshotDiffItem>()
  private lateinit var comparisonLabels: ComparisonDashboardLabels
  private lateinit var chooseApkResultLauncher: ActivityResultLauncher<Array<String>>
  private var archiveChoosingSide = SnapshotComparisonSide.LEFT
  private var pendingRestoreUri: Uri? = null

  private val fragmentLifecycleCallbacks =
    object : FragmentManager.FragmentLifecycleCallbacks() {
      override fun onFragmentDetached(fragmentManager: FragmentManager, fragment: Fragment) {
        if (fragment is SnapshotBackupBottomSheetDialogFragment) {
          window.decorView.post(::dispatchPendingRestoreUri)
        }
      }
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    comparisonLabels = ComparisonDashboardLabels(
      timestampTitle = getString(R.string.snapshot_current_timestamp),
      chooseTimestampText = getString(R.string.album_click_to_choose),
      appsCountTitle = getString(R.string.comparison_snapshot_apps_count),
      defaultAppsCountText = DEFAULT_DASHBOARD_APPS_COUNT
    )
    route = savedInstanceState
      ?.getString(STATE_ROUTE)
      ?.let { savedRoute -> AlbumRoute.entries.find { it.name == savedRoute } }
      ?: AlbumRoute.Home
    if (route == AlbumRoute.Track) {
      trackViewModel.loadTrackList()
    } else if (route == AlbumRoute.Comparison) {
      invalidateComparisonDashboard()
    }
    registerComparisonCallbacks()
    registerComparisonObservers()
    supportFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleCallbacks, false)
    renderComposeContent()
    handleIntent(intent, isInitial = savedInstanceState == null)
    onBackPressedDispatcher.addBackStateHandler(
      lifecycleOwner = this,
      enabledState = {
        route == AlbumRoute.Home && intent?.action == Intent.ACTION_VIEW && intent?.data != null
      },
      handler = {
        startActivity(
          Intent(this, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
        finish()
      }
    )
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleIntent(intent, isInitial = false)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putString(STATE_ROUTE, route.name)
    super.onSaveInstanceState(outState)
  }

  override fun onResumeFragments() {
    super.onResumeFragments()
    dispatchPendingRestoreUri()
  }

  override fun onDestroy() {
    supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentLifecycleCallbacks)
    comparisonViewModel.clearSnapshotComparisonArchiveCache(externalCacheDir)
    super.onDestroy()
  }

  private fun renderComposeContent() {
    setContent {
      LibCheckerTheme {
        val trackState by trackViewModel.uiState.collectAsStateWithLifecycle()
        AlbumRouteHost(
          route = route,
          onNavigateHome = ::navigateHome
        ) { targetRoute ->
          when (targetRoute) {
            AlbumRoute.Home,
            AlbumRoute.Management,
            AlbumRoute.BackupRestore -> AlbumHomeRoute(
              onNavigate = ::handleAlbumEntry,
              onNavigateUp = onBackPressedDispatcher::onBackPressed
            )

            AlbumRoute.Comparison -> comparisonState?.let { state ->
              ComparisonRouteScreen(
                state = state,
                onNavigateUp = ::navigateHome,
                onSelectSide = ::showTimeNodePicker,
                onCompare = ::compareSelectedItems,
                onResultClick = ::navigateToComparisonResult
              )
            }

            AlbumRoute.Track -> TrackRouteScreen(
              state = trackState,
              onQueryChanged = trackViewModel::setQuery,
              onTrackedChange = trackViewModel::setPackageTracked,
              onNavigateUp = ::navigateHome
            )
          }
        }
      }
    }
  }

  private fun handleAlbumEntry(route: AlbumRoute) {
    when (route) {
      AlbumRoute.Comparison -> openComparison()

      AlbumRoute.Management -> showSnapshotManagementDialog()

      AlbumRoute.BackupRestore -> showBackupBottomSheet()

      AlbumRoute.Track -> {
        this.route = AlbumRoute.Track
        trackViewModel.loadTrackList()
      }

      AlbumRoute.Home -> navigateHome()
    }
  }

  private fun navigateHome() {
    route = AlbumRoute.Home
    trackViewModel.setQuery("")
  }

  private fun openComparison() {
    route = AlbumRoute.Comparison
    invalidateComparisonDashboard()
  }

  private fun registerComparisonCallbacks() {
    chooseApkResultLauncher =
      registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
          return@registerForActivityResult
        }
        comparisonViewModel.selectArchive(archiveChoosingSide, uri)
        invalidateComparisonDashboard()
      }
  }

  private fun registerComparisonObservers() {
    comparisonViewModel.snapshotDiffItemsFlow.onEach { items ->
      comparisonItems = items
      val iconSources = getSnapshotPackageIconSources(items.map(SnapshotDiffItem::packageName))
      val displayItems = items.mapIndexed { index, item ->
        ComparisonResultItem(
          key = "${item.packageName}:${item.updateTime}:$index",
          displayData = buildSnapshotItemDisplayData(
            BuildSnapshotItemDisplayDataUseCase.Request(
              item = item,
              cardPresentation = SnapshotItemCardPresentation.Normal,
              iconSource = iconSources[item.packageName],
              showUpdateTime = false,
              isApexPackage = false,
              animateStateIndicator = false,
              tintChangedAbiBadge = false,
              highlightDiffColor = getColorByAttr(androidx.appcompat.R.attr.colorPrimary),
              emphasizeDiffs = true,
              highlightText = ""
            )
          ),
          onClickToken = index
        )
      }
      comparisonState = currentComparisonState().copy(
        items = displayItems,
        isLoading = false,
        hasCompared = true
      )
    }.launchIn(lifecycleScope)

    comparisonViewModel.effect.onEach { effect ->
      when (effect) {
        is SnapshotComparisonViewModel.Effect.DashboardCountChange -> {
          val side = SnapshotComparisonSide.fromIsLeft(effect.isLeft)
          val state = currentComparisonState()
          comparisonState = state.copy(
            dashboard = state.dashboard.withAppsCountText(
              side = side,
              appsCountText = effect.snapshotCount.toString(),
              labels = comparisonLabels
            )
          )
        }
      }
    }.launchIn(lifecycleScope)
  }

  private fun currentComparisonState(): ComparisonRouteState {
    return comparisonState ?: ComparisonRouteState(
      dashboard = comparisonViewModel.buildDashboardState(comparisonLabels)
    )
  }

  private fun invalidateComparisonDashboard() {
    val dashboard = comparisonViewModel.buildDashboardState(comparisonLabels)
    comparisonState = currentComparisonState().copy(dashboard = dashboard)
    requestDashboardCount(dashboard, SnapshotComparisonSide.LEFT)
    requestDashboardCount(dashboard, SnapshotComparisonSide.RIGHT)
  }

  private fun requestDashboardCount(
    dashboard: ComparisonDashboardState,
    side: SnapshotComparisonSide
  ) {
    dashboard.getSide(side).dashboardCountTimestamp?.let {
      comparisonViewModel.getDashboardCount(it, side == SnapshotComparisonSide.LEFT)
    }
  }

  private fun showTimeNodePicker(side: SnapshotComparisonSide) {
    lifecycleScope.launch(Dispatchers.IO) {
      val timeStampList = comparisonViewModel.getTimeStamps()
      withContext(Dispatchers.Main) {
        TimeNodeBottomSheetDialogFragment
          .newInstance(ArrayList(timeStampList))
          .apply {
            setCompareMode(true)
            setLeftMode(side == SnapshotComparisonSide.LEFT)
            setOnAddApkClickListener { isLeft ->
              archiveChoosingSide = SnapshotComparisonSide.fromIsLeft(isLeft)
              chooseApkResultLauncher.launch(
                arrayOf("application/vnd.android.package-archive", "application/octet-stream")
              )
            }
            setOnItemClickListener { position ->
              val item = timeStampList.getOrNull(position) ?: return@setOnItemClickListener
              comparisonViewModel.selectSnapshot(side, item.timestamp)
              invalidateComparisonDashboard()
              dismiss()
            }
          }
          .show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
      }
    }
  }

  private fun compareSelectedItems() {
    if (comparisonState?.isLoading == true) {
      return
    }
    if (!comparisonViewModel.canCompare()) {
      showToast(R.string.album_item_comparison_invalid_compare)
      return
    }
    lifecycleScope.launch(Dispatchers.IO) {
      withContext(Dispatchers.Main) {
        comparisonState = currentComparisonState().copy(isLoading = true)
      }
      val compareAction = comparisonViewModel.buildCompareAction(
        cacheDir = requireAvailableCacheDir(),
        iconSize = resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
      )
      withContext(Dispatchers.Main) {
        if (compareAction.hasNotEnoughStorageSpace) {
          showToast(R.string.toast_not_enough_storage_space)
        }
      }
      when (compareAction) {
        is SnapshotComparisonViewModel.CompareAction.Invalid -> withContext(Dispatchers.Main) {
          comparisonState = currentComparisonState().copy(isLoading = false)
          showToast(R.string.album_item_comparison_invalid_compare)
        }

        is SnapshotComparisonViewModel.CompareAction.Ready -> handleComparePlan(compareAction.plan)
      }
    }
  }

  private suspend fun handleComparePlan(plan: SnapshotComparisonPlan) {
    when (plan) {
      is SnapshotComparisonPlan.TimestampRange -> comparisonViewModel.compareDiff(
        plan.previousTimestamp,
        plan.currentTimestamp
      )

      is SnapshotComparisonPlan.SnapshotLists -> comparisonViewModel.compareDiffWithSnapshotList(
        plan.lists.left,
        plan.lists.right
      )

      is SnapshotComparisonPlan.ArchivePair -> withContext(Dispatchers.Main) {
        comparisonState = currentComparisonState().copy(isLoading = false)
        showArchiveComparison(plan)
      }
    }
  }

  private fun showArchiveComparison(plan: SnapshotComparisonPlan.ArchivePair) {
    if (plan.requiresDifferentPackageConfirmation) {
      BaseAlertDialogBuilder(this)
        .setTitle(R.string.dialog_title_compare_diff_apk)
        .setMessage(R.string.dialog_message_compare_diff_apk)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          navigateToSnapshotDetail(
            plan.left.snapshotItem,
            plan.right.snapshotItem,
            plan.left.icon,
            plan.right.icon
          )
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    } else {
      navigateToSnapshotDetail(
        plan.left.snapshotItem,
        plan.right.snapshotItem,
        plan.left.icon,
        plan.right.icon
      )
    }
  }

  private fun navigateToComparisonResult(position: Int) {
    val item = comparisonItems.getOrNull(position) ?: return
    startActivity(
      Intent(this, SnapshotDetailActivity::class.java)
        .putExtra(EXTRA_ENTITY, item)
    )
  }

  private fun navigateToSnapshotDetail(
    left: SnapshotItem,
    right: SnapshotItem,
    leftIcon: Bitmap,
    rightIcon: Bitmap
  ) {
    startActivity(
      Intent(this, SnapshotDetailActivity::class.java)
        .putExtras(
          Bundle().apply {
            putSerializable(EXTRA_ENTITY, comparisonViewModel.buildSnapshotPairDiff(left, right))
            putParcelable(EXTRA_ICON, getIconsCombo(leftIcon, rightIcon))
          }
        )
    )
  }

  private fun getIconsCombo(leftIconOrigin: Bitmap, rightIconOrigin: Bitmap): Bitmap {
    val iconSize = resources.getDimensionPixelSize(R.dimen.lib_detail_icon_size)
    val leftIcon = Bitmap.createBitmap(
      leftIconOrigin,
      0,
      0,
      leftIconOrigin.width / 2,
      leftIconOrigin.height
    )
    val rightIcon = Bitmap.createBitmap(
      rightIconOrigin,
      rightIconOrigin.width / 2,
      0,
      rightIconOrigin.width / 2,
      rightIconOrigin.height
    )
    val comboIcon = createBitmap(iconSize, iconSize)
    val isSameIcon = leftIconOrigin.sameAs(rightIconOrigin)
    Canvas(comboIcon).apply {
      drawBitmap(leftIcon, 0f, 0f, null)
      drawBitmap(rightIcon, iconSize / 2f, 0f, null)
      if (!isSameIcon) {
        drawLine(
          iconSize / 2f,
          0f,
          iconSize / 2f,
          iconSize.toFloat(),
          Paint().apply {
            color = getColorByAttr(com.google.android.material.R.attr.colorOnSurface)
            strokeWidth = 2.dp.toFloat()
          }
        )
      }
    }
    return comboIcon
  }

  private fun handleIntent(intent: Intent?, isInitial: Boolean) {
    if (intent == null) {
      return
    }
    when (intent.action) {
      Intent.ACTION_VIEW -> {
        val restoreUri = intent.data ?: return
        if (isInitial) {
          showBackupBottomSheet(restoreUri)
        } else {
          pendingRestoreUri = restoreUri
          dispatchPendingRestoreUri()
        }
      }

      Intent.ACTION_SEND_MULTIPLE -> {
        openComparison()
        parseComparisonIntent(intent)
      }
    }
  }

  private fun parseComparisonIntent(intent: Intent) {
    when (val result = ComparisonShareIntentParser.parse(intent)) {
      ComparisonShareIntentParser.Result.None -> Unit

      ComparisonShareIntentParser.Result.InvalidSharedItems ->
        showToast(R.string.album_item_comparison_invalid_shared_items)

      is ComparisonShareIntentParser.Result.PackagePair -> {
        result.leftUri?.let {
          comparisonViewModel.selectArchive(SnapshotComparisonSide.LEFT, it)
        }
        result.rightUri?.let {
          comparisonViewModel.selectArchive(SnapshotComparisonSide.RIGHT, it)
        }
        repeat(result.invalidItemCount) {
          showToast(R.string.album_item_comparison_invalid_shared_items)
        }
        invalidateComparisonDashboard()
        compareSelectedItems()
      }
    }
  }

  private fun dispatchPendingRestoreUri() {
    val restoreUri = pendingRestoreUri ?: return
    if (supportFragmentManager.isStateSaved) {
      return
    }
    val tag = SnapshotBackupBottomSheetDialogFragment::class.java.name
    val currentSheet =
      supportFragmentManager.findFragmentByTag(tag) as? SnapshotBackupBottomSheetDialogFragment
    if (currentSheet?.isRemoving == true) {
      return
    }
    if (currentSheet != null) {
      currentSheet.restoreFromLaunchUri(restoreUri)
      pendingRestoreUri = null
    } else if (showBackupBottomSheet(restoreUri)) {
      pendingRestoreUri = null
    }
  }

  private fun showBackupBottomSheet(restoreUri: Uri? = null): Boolean {
    val tag = SnapshotBackupBottomSheetDialogFragment::class.java.name
    if (supportFragmentManager.findFragmentByTag(tag) != null) {
      return false
    }
    return runCatching {
      SnapshotBackupBottomSheetDialogFragment
        .newInstance(restoreUri)
        .showNow(supportFragmentManager, tag)
    }.onFailure(Timber::e).isSuccess
  }

  private fun showSnapshotManagementDialog() {
    lifecycleScope.launch(Dispatchers.IO) {
      val timeStampList = snapshotViewModel.getTimeStamps().toMutableList()
      withContext(Dispatchers.Main) {
        val dialog = TimeNodeBottomSheetDialogFragment
          .newInstance(ArrayList(timeStampList)).apply {
            setTitle(this@AlbumActivity.getString(R.string.dialog_title_select_to_delete))
            setOnItemClickListener { position ->
              val item = timeStampList.getOrNull(position) ?: return@setOnItemClickListener
              BaseAlertDialogBuilder(this@AlbumActivity)
                .setTitle(R.string.dialog_title_confirm_to_delete)
                .setMessage(
                  getString(
                    R.string.dialog_message_confirm_to_delete,
                    snapshotViewModel.getFormatDateString(item.timestamp)
                  )
                )
                .setPositiveButton(R.string.dialog_action_delete) { _, _ ->
                  lifecycleScope.launch(Dispatchers.IO) {
                    val loadingDialog: AlertDialog = withContext(Dispatchers.Main) {
                      UiUtils.createLoadingDialog(this@AlbumActivity).also { it.show() }
                    }
                    val remaining = snapshotViewModel.deleteSnapshotTimeStamp(item.timestamp)
                    timeStampList.clear()
                    timeStampList.addAll(remaining)
                    withContext(Dispatchers.Main) {
                      removeItem(position)
                      loadingDialog.dismiss()
                      if (timeStampList.isEmpty()) {
                        dismiss()
                      }
                    }
                  }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
            }
          }
        dialog.show(supportFragmentManager, TimeNodeBottomSheetDialogFragment::class.java.name)
      }
    }
  }

  private companion object {
    const val DEFAULT_DASHBOARD_APPS_COUNT = "0"
    const val STATE_ROUTE = "album_route"
  }
}
