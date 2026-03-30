package com.absinthe.libchecker.domain.snapshot.usecase

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.data.snapshot.SnapshotDiffEngine
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.appscan.InstalledPackageSource
import com.absinthe.libchecker.domain.snapshot.SnapshotCompareRepository
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.utils.extensions.getPackageSize
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.fromJson
import com.absinthe.libchecker.utils.toJson
import java.io.IOException
import timber.log.Timber

class CompareSnapshotDiffUseCase(
  private val snapshotCompareRepository: SnapshotCompareRepository,
  private val installedPackageSource: InstalledPackageSource,
  private val snapshotDiffEngine: SnapshotDiffEngine
) {
  suspend fun compareWithInstalledApps(
    packageManager: PackageManager,
    preTimeStamp: Long,
    shouldClearDiff: Boolean,
    onProgress: (Int) -> Unit = {}
  ): List<SnapshotDiffItem> {
    if (shouldClearDiff) {
      snapshotCompareRepository.clearSnapshotDiffItems()
    }

    val preMap = snapshotCompareRepository.getSnapshots(preTimeStamp).associateBy { it.packageName }
    if (preMap.isEmpty() || preTimeStamp == 0L) {
      return emptyList()
    }

    val currMap = installedPackageSource.getApplicationMap(forceUpdate = true).toMutableMap()
    val prePackageSet = preMap.keys
    val currPackageSet = currMap.keys
    val removedPackageSet = prePackageSet - currPackageSet
    val addedPackageSet = currPackageSet - prePackageSet
    val commonPackageSet = prePackageSet intersect currPackageSet
    val diffList = mutableListOf<SnapshotDiffItem>()
    val allTrackItems = snapshotCompareRepository.getTrackItems()
    val total = (addedPackageSet.size + commonPackageSet.size).coerceAtLeast(1)

    var count = 0

    removedPackageSet.forEach { packageName ->
      snapshotDiffEngine.createSnapshotDiffItem(preMap[packageName], null, allTrackItems)?.let(diffList::add)
    }

    addedPackageSet.forEach { packageName ->
      try {
        val newInfo = snapshotDiffEngine.buildSnapshotItem(packageManager, currMap[packageName]!!)
        snapshotDiffEngine.createSnapshotDiffItem(null, newInfo, allTrackItems)?.let(diffList::add)
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        count++
        onProgress(count * 100 / total)
      }
    }

    commonPackageSet.forEach { packageName ->
      try {
        val snapshotItem = preMap[packageName]!!
        val presentItem = currMap[packageName]!!
        val snapshotDiffStoringItem = snapshotCompareRepository.getSnapshotDiff(snapshotItem.packageName)

        if (snapshotDiffStoringItem?.lastUpdatedTime != presentItem.lastUpdateTime) {
          getDiffItemByComparingStoredWithInstalled(
            packageManager = packageManager,
            snapshotItem = snapshotItem,
            packageInfo = presentItem,
            trackItems = allTrackItems
          )?.let { item ->
            diffList.add(item)
            saveSnapshotDiff(
              packageInfo = presentItem,
              item = item
            )
          }
        } else {
          try {
            snapshotDiffStoringItem.diffContent.fromJson<SnapshotDiffItem>()?.let(diffList::add)
          } catch (e: IOException) {
            Timber.e(e, "diffContent parsing failed")

            getDiffItemByComparingStoredWithInstalled(
              packageManager = packageManager,
              snapshotItem = snapshotItem,
              packageInfo = presentItem,
              trackItems = allTrackItems
            )?.let { item ->
              diffList.add(item)
              saveSnapshotDiff(
                packageInfo = presentItem,
                item = item
              )
            }
          }
        }
      } catch (e: Exception) {
        Timber.e(e)
      } finally {
        count++
        onProgress(count * 100 / total)
      }
    }

    return diffList
  }

  suspend fun compareWithStoredSnapshots(
    preTimeStamp: Long,
    currTimeStamp: Long
  ): List<SnapshotDiffItem> {
    val preMap = snapshotCompareRepository.getSnapshots(preTimeStamp).associateBy { it.packageName }
    if (preMap.isEmpty()) {
      return emptyList()
    }

    val currMap = snapshotCompareRepository.getSnapshots(currTimeStamp).associateBy { it.packageName }
    if (currMap.isEmpty()) {
      return emptyList()
    }

    return compareSnapshotMaps(preMap, currMap)
  }

  suspend fun compareWithSnapshotLists(
    preList: List<SnapshotItem>,
    currList: List<SnapshotItem>
  ): List<SnapshotDiffItem> {
    val preMap = preList.associateBy { it.packageName }
    if (preMap.isEmpty()) {
      return emptyList()
    }

    val currMap = currList.associateBy { it.packageName }
    if (currMap.isEmpty()) {
      return emptyList()
    }

    return compareSnapshotMaps(preMap, currMap)
  }

  private suspend fun compareSnapshotMaps(
    preMap: Map<String, SnapshotItem>,
    currMap: Map<String, SnapshotItem>
  ): List<SnapshotDiffItem> {
    if (preMap.isEmpty() || currMap.isEmpty()) {
      return emptyList()
    }

    val prePackageSet = preMap.keys
    val currPackageSet = currMap.keys
    val removedPackageSet = prePackageSet - currPackageSet
    val addedPackageSet = currPackageSet - prePackageSet
    val commonPackageSet = prePackageSet intersect currPackageSet
    val diffList = mutableListOf<SnapshotDiffItem>()
    val allTrackItems = snapshotCompareRepository.getTrackItems()

    removedPackageSet.forEach { packageName ->
      snapshotDiffEngine.createSnapshotDiffItem(preMap[packageName], null, allTrackItems)?.let(diffList::add)
    }

    addedPackageSet.forEach { packageName ->
      snapshotDiffEngine.createSnapshotDiffItem(null, currMap[packageName], allTrackItems)?.let(diffList::add)
    }

    commonPackageSet.forEach { packageName ->
      val preItem = preMap[packageName]!!
      val currItem = currMap[packageName]!!
      if (currItem.versionCode != preItem.versionCode || currItem.lastUpdatedTime != preItem.lastUpdatedTime) {
        snapshotDiffEngine.createSnapshotDiffItem(preItem, currItem, allTrackItems)?.let(diffList::add)
      }
    }

    return diffList
  }

  private suspend fun getDiffItemByComparingStoredWithInstalled(
    packageManager: PackageManager,
    snapshotItem: SnapshotItem,
    packageInfo: PackageInfo,
    trackItems: List<com.absinthe.libchecker.database.entity.TrackItem>
  ): SnapshotDiffItem? {
    if (packageInfo.getVersionCode() == snapshotItem.versionCode &&
      packageInfo.lastUpdateTime == snapshotItem.lastUpdatedTime &&
      packageInfo.getPackageSize(true) == snapshotItem.packageSize &&
      trackItems.any { trackItem -> trackItem.packageName == snapshotItem.packageName }.not()
    ) {
      return null
    }
    return snapshotDiffEngine.createSnapshotDiffItem(
      oldInfo = snapshotItem,
      newInfo = snapshotDiffEngine.buildSnapshotItem(packageManager, packageInfo),
      trackItems = trackItems
    )
  }

  private suspend fun saveSnapshotDiff(
    packageInfo: PackageInfo,
    item: SnapshotDiffItem
  ) {
    snapshotCompareRepository.saveSnapshotDiff(
      SnapshotDiffStoringItem(
        packageName = packageInfo.packageName,
        lastUpdatedTime = packageInfo.lastUpdateTime,
        diffContent = item.toJson().orEmpty()
      )
    )
  }
}
