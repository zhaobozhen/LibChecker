package com.absinthe.libchecker.api.bean

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class RepoInfoResp(
  @JsonNames("pushed_at") val pushedAt: String
)
