package com.absinthe.libchecker.data.appscan

import android.content.pm.PackageInfo
import com.absinthe.libchecker.data.app.LocalAppDataSource
import com.absinthe.libchecker.domain.appscan.InstalledPackageSource

class LocalInstalledPackageSource : InstalledPackageSource {
  override fun getApplicationList(forceUpdate: Boolean): List<PackageInfo> {
    return LocalAppDataSource.getApplicationList(forceUpdate)
  }

  override fun getApplicationMap(forceUpdate: Boolean): Map<String, PackageInfo> {
    return LocalAppDataSource.getApplicationMap(forceUpdate)
  }
}
