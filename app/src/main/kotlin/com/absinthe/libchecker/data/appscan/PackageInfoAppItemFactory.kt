package com.absinthe.libchecker.data.appscan

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.appscan.AppItemFactory
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getFeatures
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.isArchivedPackage
import com.absinthe.libchecker.utils.harmony.ApplicationDelegate
import com.absinthe.libchecker.utils.harmony.HarmonyOsUtil
import ohos.bundle.IBundleManager

class PackageInfoAppItemFactory : AppItemFactory {
  private val bundleManager by lazy {
    ApplicationDelegate(LibCheckerApp.app).iBundleManager
  }

  override fun create(
    packageManager: PackageManager,
    packageInfo: PackageInfo,
    delayInitFeatures: Boolean
  ): LCItem {
    val isHarmony = HarmonyOsUtil.isHarmonyOs()
    val variant = if (
      isHarmony &&
      bundleManager?.getBundleInfo(packageInfo.packageName, IBundleManager.GET_BUNDLE_DEFAULT) != null
    ) {
      Constants.VARIANT_HAP
    } else {
      Constants.VARIANT_APK
    }

    val applicationInfo = packageInfo.applicationInfo
      ?: throw IllegalArgumentException("ApplicationInfo is null")

    return LCItem(
      packageName = packageInfo.packageName,
      label = packageInfo.getAppName(packageManager).toString(),
      versionName = if (packageInfo.isArchivedPackage()) "Archived" else packageInfo.versionName.toString(),
      versionCode = packageInfo.getVersionCode(),
      installedTime = packageInfo.firstInstallTime,
      lastUpdatedTime = packageInfo.lastUpdateTime,
      isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) > 0,
      abi = PackageUtils.getAbi(packageInfo).toShort(),
      features = if (delayInitFeatures) -1 else packageInfo.getFeatures(),
      targetApi = applicationInfo.targetSdkVersion.toShort(),
      variant = variant
    )
  }
}
