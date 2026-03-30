package com.absinthe.libchecker.domain.appscan.usecase

import android.content.pm.PackageManager
import com.absinthe.libchecker.data.app.PackageChangeState
import com.absinthe.libchecker.domain.appscan.AppItemFactory
import com.absinthe.libchecker.domain.appscan.IndexedAppRepository
import com.absinthe.libchecker.domain.appscan.InstalledPackageSource
import com.absinthe.libchecker.utils.extensions.getVersionCode
import timber.log.Timber

class SyncIndexedAppsUseCase(
  private val installedPackageSource: InstalledPackageSource,
  private val indexedAppRepository: IndexedAppRepository,
  private val appItemFactory: AppItemFactory
) {
  suspend operator fun invoke(
    packageManager: PackageManager,
    forceUpdate: Boolean,
    pendingPackageChanges: List<PackageChangeState>
  ) {
    val dbItems = indexedAppRepository.getIndexedApps()
    if (dbItems.isEmpty()) {
      return
    }

    if (!forceUpdate) {
      syncPendingPackageChanges(
        packageManager = packageManager,
        pendingPackageChanges = pendingPackageChanges
      )
      return
    }

    syncWholeIndex(
      packageManager = packageManager,
      dbItems = dbItems
    )
  }

  private suspend fun syncPendingPackageChanges(
    packageManager: PackageManager,
    pendingPackageChanges: List<PackageChangeState>
  ) {
    latestPackageChanges(pendingPackageChanges).forEach { state ->
      val packageInfo = state.getActualPackageInfo()
      runCatching {
        when (state) {
          is PackageChangeState.Added -> {
            indexedAppRepository.insert(
              appItemFactory.create(
                packageManager = packageManager,
                packageInfo = packageInfo
              )
            )
          }

          is PackageChangeState.Removed -> {
            indexedAppRepository.deleteByPackageName(packageInfo.packageName)
          }

          is PackageChangeState.Replaced -> {
            indexedAppRepository.update(
              appItemFactory.create(
                packageManager = packageManager,
                packageInfo = packageInfo
              )
            )
          }
        }
      }.onFailure {
        Timber.e(it, "SyncIndexedAppsUseCase: ${packageInfo.packageName}")
      }
    }
  }

  private suspend fun syncWholeIndex(
    packageManager: PackageManager,
    dbItems: List<com.absinthe.libchecker.database.entity.LCItem>
  ) {
    val dbItemsByPackageName = dbItems.associateBy { it.packageName }
    val dbApps = dbItemsByPackageName.keys
    var applications = installedPackageSource.getApplicationMap(forceUpdate = true)
    var localApps = applications.keys
    var newApps = localApps - dbApps
    var removedApps = dbApps - localApps

    if (newApps.size > 30 || removedApps.size > 30) {
      Timber.w("SyncIndexedAppsUseCase canceled because of large diff, re-request appMap")
      applications = installedPackageSource.getApplicationMap(forceUpdate = true)
      localApps = applications.keys
      newApps = localApps - dbApps
      removedApps = dbApps - localApps
    }

    newApps.forEach { packageName ->
      runCatching {
        val packageInfo = applications[packageName] ?: return@runCatching
        indexedAppRepository.insert(
          appItemFactory.create(
            packageManager = packageManager,
            packageInfo = packageInfo
          )
        )
      }.onFailure {
        Timber.e(it, "SyncIndexedAppsUseCase: $packageName")
      }
    }

    removedApps.forEach { packageName ->
      indexedAppRepository.deleteByPackageName(packageName)
    }

    localApps.intersect(dbApps).asSequence()
      .mapNotNull { applications[it] }
      .filter { packageInfo ->
        dbItemsByPackageName[packageInfo.packageName]?.let { dbItem ->
          dbItem.versionCode != packageInfo.getVersionCode() ||
            packageInfo.lastUpdateTime != dbItem.lastUpdatedTime ||
            dbItem.lastUpdatedTime == 0L
        } == true
      }
      .forEach { packageInfo ->
        runCatching {
          indexedAppRepository.update(
            appItemFactory.create(
              packageManager = packageManager,
              packageInfo = packageInfo
            )
          )
        }.onFailure {
          Timber.e(it, "SyncIndexedAppsUseCase: ${packageInfo.packageName}")
        }
      }
  }

  private fun latestPackageChanges(
    pendingPackageChanges: List<PackageChangeState>
  ): List<PackageChangeState> {
    return pendingPackageChanges.withIndex()
      .groupBy { it.value.getActualPackageInfo().packageName }
      .values
      .map { entries -> entries.maxBy { it.index } }
      .sortedBy { it.index }
      .map { it.value }
  }
}
