package com.absinthe.libchecker.api.bean

import kotlinx.serialization.Serializable

@Serializable
data class LibDetailBean(
  val data: List<Data>,
  val uuid: String
) {
  @Serializable
  data class Data(
    val locale: String,
    val data: DataBean
  )

  @Serializable
  data class DataBean(
    val label: String,
    val dev_team: String,
    val rule_contributors: List<String>,
    val description: String,
    val source_link: String
  )
}
