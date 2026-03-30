package com.absinthe.libchecker.domain.appscan

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.absinthe.libchecker.database.entity.LCItem

interface AppItemFactory {
  fun create(
    packageManager: PackageManager,
    packageInfo: PackageInfo,
    delayInitFeatures: Boolean = false
  ): LCItem
}
