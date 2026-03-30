package com.absinthe.libchecker.data.appscan

import android.content.pm.ComponentInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.DEX
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PACKAGE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.annotation.SHARED_UID
import com.absinthe.libchecker.domain.appscan.PackageReferenceReader
import com.absinthe.libchecker.utils.IntentFilterUtils
import com.absinthe.libchecker.utils.LCAppUtils
import com.absinthe.libchecker.utils.PackageUtils
import timber.log.Timber

class PackageUtilsReferenceReader : PackageReferenceReader {
  override fun getReferences(
    packageName: String,
    @LibType type: Int
  ): Collection<String> {
    return runCatching {
      when (type) {
        NATIVE -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          val list = PackageUtils.getNativeDirLibs(packageInfo)
          list.filter { LCAppUtils.checkNativeLibValidation(packageName, it.name, list) }
            .map { it.name }
        }

        SERVICE -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_SERVICES)
          getComponentNames(packageName, packageInfo.services)
        }

        ACTIVITY -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
          getComponentNames(packageName, packageInfo.activities)
        }

        RECEIVER -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_RECEIVERS)
          getComponentNames(packageName, packageInfo.receivers)
        }

        PROVIDER -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_PROVIDERS)
          getComponentNames(packageName, packageInfo.providers)
        }

        DEX -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          PackageUtils.getDexList(packageInfo)
            .filter { it.name.startsWith(packageName).not() }
            .map { it.name }
        }

        PERMISSION -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
          packageInfo.requestedPermissions?.toList().orEmpty()
        }

        METADATA -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName, PackageManager.GET_META_DATA)
          packageInfo.applicationInfo?.metaData?.keySet()?.toList().orEmpty()
        }

        PACKAGE -> {
          val split = packageName.split(".")
          listOf(split.subList(0, split.size.coerceAtMost(2)).joinToString("."))
        }

        SHARED_UID -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          packageInfo.sharedUserId?.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty()
        }

        ACTION -> {
          val packageInfo = PackageUtils.getPackageInfo(packageName)
          IntentFilterUtils.parseComponentsFromApk(packageInfo.applicationInfo!!.sourceDir)
            .asSequence()
            .flatMap { component ->
              component.intentFilters.asSequence()
                .flatMap { filter -> filter.actions }
            }
            .toSet()
            .toList()
        }

        else -> emptyList()
      }
    }.onFailure {
      Timber.e(it)
    }.getOrDefault(emptyList())
  }

  private fun getComponentNames(
    packageName: String,
    components: Array<out ComponentInfo>?
  ): List<String> {
    return components.orEmpty()
      .filter { it.name.startsWith(packageName).not() }
      .map { it.name }
  }
}
