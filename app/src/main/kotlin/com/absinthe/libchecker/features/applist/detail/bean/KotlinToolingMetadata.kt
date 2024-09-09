package com.absinthe.libchecker.features.applist.detail.bean

import kotlinx.serialization.Serializable

@Serializable
data class KotlinToolingMetadata(
  val buildSystem: String,
  val buildSystemVersion: String,
  val buildPlugin: String,
  val buildPluginVersion: String,
  val projectTargets: Array<ProjectTarget>?
) {
  @Serializable
  data class ProjectTarget(
    val target: String,
    val platformType: String,
    val extras: Extras?
  )

  @Serializable
  data class Extras(
    val android: AndroidExtras?
  )

  @Serializable
  data class AndroidExtras(
    val sourceCompatibility: String?,
    val targetCompatibility: String?
  )
}
