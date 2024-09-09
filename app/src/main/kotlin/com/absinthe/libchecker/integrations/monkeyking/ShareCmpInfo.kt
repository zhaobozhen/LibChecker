package com.absinthe.libchecker.integrations.monkeyking

import kotlinx.serialization.Serializable

@Serializable
data class ShareCmpInfo(
  val pkg: String,
  val components: List<Component>
) {
  @Serializable
  data class Component(
    val type: String,
    val name: String,
    val block: Boolean
  )
}
