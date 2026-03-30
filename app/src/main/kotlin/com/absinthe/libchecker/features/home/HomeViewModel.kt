package com.absinthe.libchecker.features.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.annotation.STATUS_INIT_END
import com.absinthe.libchecker.annotation.STATUS_NOT_START
import com.absinthe.libchecker.annotation.STATUS_START_INIT
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE
import com.absinthe.libchecker.annotation.STATUS_START_REQUEST_CHANGE_END
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.core.di.AppGraph
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.data.app.PackageChangeState
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.appscan.model.AppReferenceNode
import com.absinthe.libchecker.features.statistics.bean.LibReference
import com.absinthe.libchecker.services.IWorkerService
import com.absinthe.libchecker.ui.base.IListController
import com.absinthe.libchecker.utils.OsUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libraries.utils.manager.TimeRecorder
import com.absinthe.rulesbundle.IconResMap
import java.io.File
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okio.buffer
import okio.sink
import timber.log.Timber

class HomeViewModel : ViewModel() {
  private val observeIndexedApps = AppGraph.observeIndexedAppsUseCase
  private val initializeIndexedApps = AppGraph.initializeIndexedAppsUseCase
  private val syncIndexedApps = AppGraph.syncIndexedAppsUseCase
  private val exportIndexedAppsReport = AppGraph.exportIndexedAppsReportUseCase
  private val computeLibReferenceNodes = AppGraph.computeLibReferenceNodesUseCase
  private val matchLibReferences = AppGraph.matchLibReferencesUseCase

  val dbItemsFlow: Flow<List<LCItem>> = observeIndexedApps()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private val _libReference: MutableSharedFlow<List<LibReference>?> = MutableSharedFlow()
  val libReference = _libReference.asSharedFlow()

  private var _savedRefList: List<LibReference>? = null
  val savedRefList: List<LibReference>?
    get() = _savedRefList

  private var referenceNodes: List<AppReferenceNode>? = null
  var savedThreshold = GlobalValues.libReferenceThreshold

  var controller: IListController? = null
  var appListStatus: Int = STATUS_NOT_START
  var workerBinder: IWorkerService? = null
  var checkPackagesPermission: Boolean = false

  // Simple menu state management
  var isSearchMenuExpanded: Boolean = false
  var currentSearchQuery: String = ""

  private val pendingChangedPackages = ArrayDeque<PackageChangeState>()

  fun reloadApps() {
    if (appListStatus != STATUS_NOT_START || (initJob?.isActive == false && requestChangeJob?.isActive == false)) {
      Timber.d("reloadApps: ignore, appListStatus: $appListStatus")
      return
    }
    setEffect {
      Effect.ReloadApps()
    }
  }

  private fun refreshList() {
    setEffect {
      Effect.RefreshList()
    }
  }

  fun packageChanged(packageChangeState: PackageChangeState) {
    setEffect {
      Effect.PackageChanged(packageChangeState)
    }
  }

  fun sphereTextureAvailable() {
    setEffect {
      Effect.SphereTextureAvailable()
    }
  }

  private fun updateInitProgress(progress: Int) {
    setEffect {
      Effect.UpdateInitProgress(progress)
    }
  }

  private fun updateAppListStatus(status: Int) {
    setEffect {
      Effect.UpdateAppListStatus(status)
    }
    appListStatus = status
  }

  private fun updateLibRefProgress(progress: Int) {
    setEffect {
      Effect.UpdateLibRefProgress(progress)
    }
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  private var initJob: Job? = null

  fun initItems(context: Context) {
    if (initJob?.isActive == true) {
      return
    }
    viewModelScope.launch {
      initJob = initItemsImpl(context)
    }
  }

  private fun initItemsImpl(context: Context) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("initItems: START")

    val timeRecorder = TimeRecorder()
    timeRecorder.start()

    try {
      updateAppListStatus(STATUS_START_INIT)
      updateInitProgress(0)
      initializeIndexedApps(
        packageManager = context.packageManager,
        onProgress = ::updateInitProgress
      )
      updateAppListStatus(STATUS_INIT_END)
      timeRecorder.end()
      Timber.d("initItems: END, $timeRecorder")
    } finally {
      updateAppListStatus(STATUS_NOT_START)
      initJob = null
    }
  }

  private var requestChangeJob: Job? = null

  fun requestChange(context: Context, packageChangeState: PackageChangeState? = null) {
    viewModelScope.launch {
      if (appListStatus == STATUS_START_INIT) {
        Timber.d("Request change canceled: STATUS_START_INIT")
        return@launch
      }
      packageChangeState?.let { pendingChangedPackages.add(it) }
      requestChangeJob?.cancel()
      requestChangeJob = requestChangeImpl(context, packageChangeState == null)
    }
  }

  private fun requestChangeImpl(context: Context, forceUpdate: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("Request change: START")
    val timeRecorder = TimeRecorder()

    timeRecorder.start()
    try {
      updateAppListStatus(STATUS_START_REQUEST_CHANGE)
      val pendingPackageChanges = buildList {
        if (!forceUpdate) {
          while (pendingChangedPackages.isNotEmpty()) {
            add(pendingChangedPackages.removeFirst())
          }
        }
      }
      syncIndexedApps(
        packageManager = context.packageManager,
        forceUpdate = forceUpdate,
        pendingPackageChanges = pendingPackageChanges
      )
      refreshList()
      updateAppListStatus(STATUS_START_REQUEST_CHANGE_END)
      timeRecorder.end()
      Timber.d("Request change: END, $timeRecorder")
    } finally {
      updateAppListStatus(STATUS_NOT_START)
    }
  }

  private var computeLibReferenceJob: Job? = null

  fun computeLibReference() {
    computeLibReferenceJob?.cancel()
    matchingJob?.cancel()
    computeLibReferenceJob = viewModelScope.launch(Dispatchers.IO) {
      referenceNodes = null
      _libReference.emit(null)
      referenceNodes = computeLibReferenceNodes(
        showSystemApps = GlobalValues.isShowSystemApps,
        options = GlobalValues.libReferenceOptions,
        onProgress = ::updateLibRefProgress
      )
      if (isActive) {
        matchingRules()
      }
    }
  }

  private var matchingJob: Job? = null

  fun matchingRules() {
    matchingJob?.cancel()
    matchingJob = viewModelScope.launch(Dispatchers.IO) {
      val nodes = referenceNodes ?: return@launch
      val refList = matchLibReferences(
        references = nodes,
        threshold = GlobalValues.libReferenceThreshold,
        onlyNotMarked = GlobalValues.libReferenceOptions and LibReferenceOptions.ONLY_NOT_MARKED > 0,
        onProgress = ::updateLibRefProgress
      )
      _libReference.emit(refList)
      _savedRefList = refList
    }
  }

  fun cancelMatchingJob() {
    matchingJob?.cancel()
    matchingJob = null
  }

  fun refreshRef() = viewModelScope.launch(Dispatchers.IO) {
    _savedRefList?.let { ref ->
      val threshold = GlobalValues.libReferenceThreshold
      _libReference.emit(ref.filter { it.referredList.size >= threshold })
    }
  }

  fun clearApkCache() {
    LibCheckerApp.app.externalCacheDir?.deleteRecursively()
  }

  sealed class Effect {
    data class ReloadApps(val obj: Any? = null) : Effect()
    data class UpdateInitProgress(val progress: Int) : Effect()
    data class UpdateAppListStatus(val status: Int) : Effect()
    data class PackageChanged(val packageChangeState: PackageChangeState) : Effect()
    data class RefreshList(val obj: Any? = null) : Effect()
    data class UpdateLibRefProgress(val progress: Int) : Effect()
    data class SphereTextureAvailable(val obj: Any? = null) : Effect()
  }

  fun dumpAppsInfo(os: OutputStream, saveAsMarkDown: Boolean) {
    viewModelScope.launch(Dispatchers.IO) {
      exportIndexedAppsReport(os, saveAsMarkDown)
    }
  }

  fun clearMenuState() {
    isSearchMenuExpanded = false
    currentSearchQuery = ""
  }

  fun generateAppsListSphereTexture(context: Context) {
    viewModelScope.launch(Dispatchers.IO) {
      val baseDir = context.filesDir.resolve("sphere_texture")
      if (!baseDir.exists()) {
        baseDir.mkdirs()
      }
      if ((baseDir.listFiles()?.size ?: 0) >= 2) {
        return@launch
      }
      baseDir.resolve("apps").mkdirs()
      baseDir.resolve("libs").mkdirs()

      val icons = mutableListOf<Drawable>()

      // Apps icons
      val defaultIcon = context.packageManager.defaultActivityIcon
      val defaultMonoIcon = if (OsUtils.atLeastT() && defaultIcon is AdaptiveIconDrawable) defaultIcon.monochrome else null
      LocalAppDataSource.getApplicationList().forEach {
        if (icons.size >= 75) return@forEach
        val icon = context.packageManager.getApplicationIcon(it.applicationInfo!!)
        val result = if (OsUtils.atLeastT()) {
          (icon as? AdaptiveIconDrawable)?.monochrome.takeIf { icon -> !UiUtils.drawablesAreEqual(icon, defaultMonoIcon) }
            ?.apply { setTint(context.getColorByAttr(androidx.appcompat.R.attr.colorPrimary)) }
        } else {
          icon.takeIf { icon -> !UiUtils.drawablesAreEqual(icon, defaultIcon) }
        } ?: return@forEach
        icons.add(result)
      }
      val iconSize = 48.dp
      repeat(5) {
        val subIcons = icons.shuffled().take(25)
        val bitmap = UiUtils.getDrawableStrip(context, subIcons, iconSize, iconSize)
        val file = File(baseDir.resolve("apps"), "$it.png")
        file.sink().buffer().use { sink ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, sink.outputStream())
        }
      }

      // Libs icons
      icons.clear()
      repeat(50) {
        icons.add(ContextCompat.getDrawable(context, IconResMap.getIconRes(it))!!)
      }
      repeat(5) {
        val subIcons = icons.shuffled().take(25)
        val bitmap = UiUtils.getDrawableStrip(context, subIcons, iconSize, iconSize)
        val file = File(baseDir.resolve("libs"), "$it.png")
        file.sink().buffer().use { sink ->
          bitmap.compress(Bitmap.CompressFormat.PNG, 100, sink.outputStream())
        }
      }

      sphereTextureAvailable()
    }
  }
}
