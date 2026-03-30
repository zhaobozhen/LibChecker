package com.absinthe.libchecker.domain.rules.model

data class CloudRulesVersionState(
  val localVersion: Int,
  val remoteVersion: Int,
  val hasUpdate: Boolean
)
