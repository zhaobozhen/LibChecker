package com.absinthe.libchecker.domain.appscan

import android.content.pm.PackageInfo

interface InstalledPackageSource {
  fun getApplicationList(forceUpdate: Boolean = false): List<PackageInfo>

  fun getApplicationMap(forceUpdate: Boolean = false): Map<String, PackageInfo>
}
