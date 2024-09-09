package com.absinthe.libchecker.api.bean

import kotlinx.serialization.Serializable

@Serializable
data class GetAppUpdateInfo(
  val app: App
) {
  @Serializable
  data class App(
    val version: String,
    val versionCode: Int,
    val extra: Extra,
    val link: String,
    val note: String?
  ) {
    @Serializable
    data class Extra(
      val target: Int,
      val min: Int,
      val compile: Int,
      val packageSize: Int
    )
  }
}
