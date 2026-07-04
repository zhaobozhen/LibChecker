package com.absinthe.libchecker.data.app.update

import android.content.Context
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.bean.GetAppUpdateInfo
import com.absinthe.libchecker.api.request.GetAppUpdateRequest
import com.absinthe.libchecker.domain.app.update.AppUpdateChannel
import com.absinthe.libchecker.domain.app.update.AppUpdateInstallResult
import com.absinthe.libchecker.domain.app.update.AppUpdateRepository

class AndroidAppUpdateRepository(
  @Suppress("UNUSED_PARAMETER") context: Context,
  private val request: GetAppUpdateRequest = ApiManager.create()
) : AppUpdateRepository {

  override suspend fun requestUpdateInfo(channel: AppUpdateChannel): GetAppUpdateInfo? {
    return request.requestAppUpdateInfo(channel.requestValue)
  }

  override suspend fun installUpdate(url: String): AppUpdateInstallResult {
    return AppUpdateInstallResult.Unsupported
  }

  private val AppUpdateChannel.requestValue: String
    get() = when (this) {
      AppUpdateChannel.STABLE -> "stable"
      AppUpdateChannel.CI -> "ci"
    }
}
