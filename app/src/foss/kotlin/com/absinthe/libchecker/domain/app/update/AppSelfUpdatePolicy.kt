package com.absinthe.libchecker.domain.app.update

import android.os.Build

object AppSelfUpdatePolicy {

  fun supportsUserActionNotRequiredInstall(sdkInt: Int): Boolean {
    return sdkInt >= Build.VERSION_CODES.S
  }
}
