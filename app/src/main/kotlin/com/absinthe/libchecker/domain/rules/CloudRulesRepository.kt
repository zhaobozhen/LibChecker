package com.absinthe.libchecker.domain.rules

import android.content.Context

interface CloudRulesRepository {
  suspend fun getLocalVersion(context: Context): Int

  suspend fun getRemoteVersion(): Int?

  suspend fun updateRulesBundle(
    context: Context,
    targetVersion: Int
  ): Boolean
}
