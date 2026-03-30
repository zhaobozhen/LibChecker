package com.absinthe.libchecker.data.appscan

import com.absinthe.libchecker.LibCheckerApp
import com.absinthe.libchecker.domain.appscan.AbiLabelFormatter
import com.absinthe.libchecker.utils.PackageUtils

class PackageUtilsAbiLabelFormatter : AbiLabelFormatter {
  override fun getAbiLabel(abi: Int): String {
    return PackageUtils.getAbiString(LibCheckerApp.app, abi, false)
  }
}
