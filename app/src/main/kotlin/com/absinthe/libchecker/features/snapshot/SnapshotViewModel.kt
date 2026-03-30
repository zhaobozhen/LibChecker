package com.absinthe.libchecker.features.snapshot

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.absinthe.libchecker.R
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.core.di.AppGraph
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.model.RestoreSnapshotsResult
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDetailItem
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.ui.base.BaseAlertDialogBuilder
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.toJson
import com.absinthe.libraries.utils.manager.TimeRecorder
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

const val CURRENT_SNAPSHOT = -1L

class SnapshotViewModel : ViewModel() {
  private val compareSnapshotDiff = AppGraph.compareSnapshotDiffUseCase
  private val snapshotDiffEngine = AppGraph.snapshotDiffEngine
  private val buildSnapshotDetailItems = AppGraph.buildSnapshotDetailItemsUseCase
  private val backupSnapshots = AppGraph.backupSnapshotsUseCase
  private val restoreSnapshots = AppGraph.restoreSnapshotsUseCase

  val repository = Repositories.lcRepository
  val allSnapshots = repository.allSnapshotItemsFlow
  val snapshotDiffItemsFlow: MutableSharedFlow<List<SnapshotDiffItem>> = MutableSharedFlow()
  val snapshotDetailItemsFlow: MutableSharedFlow<List<SnapshotDetailItem>> = MutableSharedFlow()

  private val _effect: MutableSharedFlow<Effect> = MutableSharedFlow()
  val effect = _effect.asSharedFlow()

  private var compareDiffJob: Job? = null

  var currentTimeStamp: Long = GlobalValues.snapshotTimestamp
    private set

  fun isComparingActive(): Boolean = compareDiffJob == null || compareDiffJob?.isActive == true

  fun compareDiff(
    context: Context,
    preTimeStamp: Long,
    currTimeStamp: Long = CURRENT_SNAPSHOT,
    shouldClearDiff: Boolean = false
  ) {
    if (compareDiffJob?.isActive == true) {
      compareDiffJob?.cancel()
    }
    compareDiffJob = viewModelScope.launch(Dispatchers.IO) {
      currentTimeStamp = preTimeStamp
      val timer = TimeRecorder().apply { start() }
      val diffList = if (currTimeStamp == CURRENT_SNAPSHOT) {
        compareSnapshotDiff.compareWithInstalledApps(
          packageManager = context.packageManager,
          preTimeStamp = preTimeStamp,
          shouldClearDiff = shouldClearDiff,
          onProgress = ::changeComparingProgress
        )
      } else {
        compareSnapshotDiff.compareWithStoredSnapshots(preTimeStamp, currTimeStamp)
      }

      snapshotDiffItemsFlow.emit(diffList)
      if (diffList.isNotEmpty() && preTimeStamp != -1L) {
        updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
      }
      timer.end()
      Timber.d("compareDiff: $timer")
    }.also {
      it.start()
    }
  }

  fun compareDiffWithSnapshotList(
    preTimeStamp: Long = -1L,
    preList: List<SnapshotItem>,
    currList: List<SnapshotItem>
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val diffList = compareSnapshotDiff.compareWithSnapshotLists(preList, currList)
      snapshotDiffItemsFlow.emit(diffList)
      if (diffList.isNotEmpty() && preTimeStamp != -1L) {
        updateTopApps(preTimeStamp, diffList.subList(0, (diffList.size - 1).coerceAtMost(5)))
      }
    }
  }

  suspend fun compareItemDiff(
    packageManager: PackageManager,
    timeStamp: Long = GlobalValues.snapshotTimestamp,
    packageName: String
  ) {
    val presentInfo = runCatching {
      val flags = PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS
      PackageUtils.getPackageInfo(packageName, flags)
    }.getOrNull()?.let { snapshotDiffEngine.buildSnapshotItem(packageManager, it) }
    val snapshotInfo = repository.getSnapshot(timeStamp, packageName)
    val allTrackItems = repository.getTrackItems()
    val diffItem = snapshotDiffEngine.createSnapshotDiffItem(snapshotInfo, presentInfo, allTrackItems)

    diffItem?.let {
      changeDiffItem(it)
    } ?: run {
      removeDiffItem(packageName)
    }
  }

  private suspend fun updateTopApps(timestamp: Long, list: List<SnapshotDiffItem>) {
    val systemProps = repository.getTimeStamp(timestamp)?.systemProps
    val appsList = list.asSequence()
      .map { it.packageName }
      .filter { PackageUtils.isAppInstalled(it) }
      .toList()
    repository.updateTimeStampItem(TimeStampItem(timestamp, appsList.toJson(), systemProps))
  }

  fun computeDiffDetail(context: Context, entity: SnapshotDiffItem) = viewModelScope.launch(Dispatchers.IO) {
    snapshotDetailItemsFlow.emit(
      buildSnapshotDetailItems(context, entity)
    )
  }

  fun backup(os: OutputStream, resultAction: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
    backupSnapshots(os)

    withContext(Dispatchers.Main) {
      resultAction()
    }
  }

  fun restore(
    context: Context,
    inputStream: InputStream,
    resultAction: (success: Boolean) -> Unit
  ) {
    viewModelScope.launch(Dispatchers.IO) {
      val restoreResult = runCatching {
        restoreSnapshots(inputStream)
      }.onFailure {
        Timber.e("restore with new format failed: $it")
        withContext(Dispatchers.Main) {
          resultAction(false)
        }
      }.getOrNull() ?: return@launch

      restoreResult.latestTimestamp?.let { GlobalValues.snapshotTimestamp = it }

      withContext(Dispatchers.Main) {
        resultAction(true)
      }

      showRestoreSummary(context, restoreResult, resultAction)
    }
  }

  private suspend fun showRestoreSummary(
    context: Context,
    restoreResult: RestoreSnapshotsResult,
    resultAction: (success: Boolean) -> Unit
  ) {
    val message = buildString {
      restoreResult.restoredCountsByTimestamp.forEach { (timestamp, count) ->
        append(
          context.getString(
            R.string.album_restore_detail,
            getFormatDateString(timestamp),
            count.toString()
          )
        )
      }
    }

    withContext(Dispatchers.Main) {
      BaseAlertDialogBuilder(context)
        .setTitle(R.string.album_restore)
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { _, _ ->
          resultAction(true)
        }
        .setCancelable(true)
        .show()
    }
  }

  fun getFormatDateString(timestamp: Long): String {
    val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd, HH:mm:ss", Locale.getDefault())
    val date = Date(timestamp)
    return simpleDateFormat.format(date)
  }

  fun chooseComparedApk(isLeftPart: Boolean) {
    setEffect {
      Effect.ChooseComparedApk(isLeftPart)
    }
  }

  fun changeTimeStamp(timestamp: Long) {
    setEffect {
      Effect.TimeStampChange(timestamp)
    }
  }

  fun getDashboardCount(timestamp: Long, isLeft: Boolean) = viewModelScope.launch(Dispatchers.IO) {
    Timber.d("getDashboardCount: $timestamp, $isLeft")
    val snapshotCount = repository.getSnapshots(timestamp).size
    val appCount = LocalAppDataSource.getApplicationMap().size
    setEffect {
      Effect.DashboardCountChange(snapshotCount, appCount, isLeft)
    }
  }

  private fun changeDiffItem(item: SnapshotDiffItem) {
    setEffect {
      Effect.DiffItemChange(item)
    }
  }

  private fun removeDiffItem(packageName: String) {
    setEffect {
      Effect.DiffItemRemove(packageName)
    }
  }

  private fun changeComparingProgress(progress: Int) {
    setEffect {
      Effect.ComparingProgressChange(progress)
    }
  }

  private fun setEffect(builder: () -> Effect) {
    val newEffect = builder()
    viewModelScope.launch {
      _effect.emit(newEffect)
    }
  }

  sealed class Effect {
    data class ChooseComparedApk(val isLeftPart: Boolean) : Effect()
    data class DashboardCountChange(
      val snapshotCount: Int,
      val appCount: Int,
      val isLeft: Boolean
    ) : Effect()

    data class DiffItemChange(val item: SnapshotDiffItem) : Effect()
    data class DiffItemRemove(val packageName: String) : Effect()
    data class TimeStampChange(val timestamp: Long) : Effect()
    data class ComparingProgressChange(val progress: Int) : Effect()
  }
}
