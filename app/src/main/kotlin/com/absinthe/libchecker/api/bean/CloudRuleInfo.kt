package com.absinthe.libchecker.api.bean

import kotlinx.serialization.Serializable

@Serializable
data class CloudRuleInfo(
  val version: Int,
  val count: Int
)
