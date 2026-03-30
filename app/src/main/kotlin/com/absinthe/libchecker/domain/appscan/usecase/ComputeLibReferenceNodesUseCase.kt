package com.absinthe.libchecker.domain.appscan.usecase

import android.content.pm.ApplicationInfo
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.constant.options.LibReferenceOptions
import com.absinthe.libchecker.domain.appscan.InstalledPackageSource
import com.absinthe.libchecker.domain.appscan.PackageReferenceReader
import com.absinthe.libchecker.domain.appscan.model.AppReferenceNode

class ComputeLibReferenceNodesUseCase(
  private val installedPackageSource: InstalledPackageSource,
  private val packageReferenceReader: PackageReferenceReader
) {
  suspend operator fun invoke(
    showSystemApps: Boolean,
    options: Int,
    onProgress: (Int) -> Unit = {}
  ): List<AppReferenceNode> {
    val appMap = installedPackageSource.getApplicationMap()
    val selectedTypes = buildSelectedTypes(options)

    if (appMap.isEmpty() || selectedTypes.isEmpty()) {
      onProgress(100)
      return emptyList()
    }

    val referenceMap = linkedMapOf<String, Pair<MutableSet<String>, Int>>()
    val totalSteps = appMap.size * selectedTypes.size
    var progressCount = 0

    fun updateProgress() {
      onProgress(progressCount * 100 / totalSteps)
    }

    onProgress(0)

    selectedTypes.forEach { type ->
      appMap.values.forEach { packageInfo ->
        if (!showSystemApps &&
          ((packageInfo.applicationInfo!!.flags and ApplicationInfo.FLAG_SYSTEM) > 0)
        ) {
          progressCount++
          updateProgress()
          return@forEach
        }

        packageReferenceReader.getReferences(packageInfo.packageName, type).forEach { reference ->
          if (reference.isBlank()) {
            return@forEach
          }
          referenceMap.getOrPut(reference) { mutableSetOf<String>() to type }.first.add(packageInfo.packageName)
        }

        progressCount++
        updateProgress()
      }
    }

    return referenceMap.map { entry ->
      AppReferenceNode(
        name = entry.key,
        referredPackages = entry.value.first,
        type = entry.value.second
      )
    }
  }

  private fun buildSelectedTypes(options: Int): List<Int> {
    val selectedTypes = mutableListOf<Int>()
    if (options and LibReferenceOptions.NATIVE_LIBS > 0) {
      selectedTypes += NATIVE
    }
    if (options and LibReferenceOptions.SERVICES > 0) {
      selectedTypes += SERVICE
    }
    if (options and LibReferenceOptions.ACTIVITIES > 0) {
      selectedTypes += ACTIVITY
    }
    if (options and LibReferenceOptions.RECEIVERS > 0) {
      selectedTypes += RECEIVER
    }
    if (options and LibReferenceOptions.PROVIDERS > 0) {
      selectedTypes += PROVIDER
    }
    if (options and LibReferenceOptions.PERMISSIONS > 0) {
      selectedTypes += PERMISSION
    }
    if (options and LibReferenceOptions.METADATA > 0) {
      selectedTypes += METADATA
    }
    if (options and LibReferenceOptions.PACKAGES > 0) {
      selectedTypes += PACKAGE
    }
    if (options and LibReferenceOptions.SHARED_UID > 0) {
      selectedTypes += SHARED_UID
    }
    if (options and LibReferenceOptions.ACTION > 0) {
      selectedTypes += ACTION
    }
    return selectedTypes
  }
}
