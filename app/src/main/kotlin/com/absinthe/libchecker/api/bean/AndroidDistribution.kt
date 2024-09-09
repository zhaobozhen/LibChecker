package com.absinthe.libchecker.api.bean

import kotlinx.serialization.Serializable

@Serializable
data class AndroidDistribution(
  val name: String,
  val version: String,
  val apiLevel: Int,
  val distributionPercentage: Double,
  val url: String,
  val descriptionBlocks: List<DescriptionBlock>
) {
  @Serializable
  data class DescriptionBlock(val title: String, val body: String)
}
