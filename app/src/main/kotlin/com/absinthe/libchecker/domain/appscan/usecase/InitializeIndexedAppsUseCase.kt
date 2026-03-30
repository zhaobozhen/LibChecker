package com.absinthe.libchecker.domain.appscan.usecase

import android.content.pm.PackageManager
import com.absinthe.libchecker.domain.appscan.AppItemFactory
import com.absinthe.libchecker.domain.appscan.IndexedAppRepository
import com.absinthe.libchecker.domain.appscan.InstalledPackageSource
import timber.log.Timber

class InitializeIndexedAppsUseCase(
  private val installedPackageSource: InstalledPackageSource,
  private val indexedAppRepository: IndexedAppRepository,
  private val appItemFactory: AppItemFactory
) {
  suspend operator fun invoke(
    packageManager: PackageManager,
    onProgress: (Int) -> Unit = {}
  ) {
    val appList = installedPackageSource.getApplicationList(forceUpdate = true)
    indexedAppRepository.deleteAllItems()

    if (appList.isEmpty()) {
      onProgress(100)
      return
    }

    val chunk = mutableListOf<com.absinthe.libchecker.database.entity.LCItem>()

    appList.forEachIndexed { index, packageInfo ->
      runCatching {
        appItemFactory.create(
          packageManager = packageManager,
          packageInfo = packageInfo,
          delayInitFeatures = true
        )
      }.onSuccess {
        chunk.add(it)
      }.onFailure {
        Timber.e(it, "InitializeIndexedAppsUseCase: ${packageInfo.packageName}")
      }

      if (chunk.size == 50) {
        indexedAppRepository.insert(chunk.toList())
        chunk.clear()
      }

      onProgress((index + 1) * 100 / appList.size)
    }

    if (chunk.isNotEmpty()) {
      indexedAppRepository.insert(chunk)
    }
  }
}
