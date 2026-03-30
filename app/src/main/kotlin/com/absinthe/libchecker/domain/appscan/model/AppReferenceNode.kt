package com.absinthe.libchecker.domain.appscan.model

import com.absinthe.libchecker.annotation.LibType

data class AppReferenceNode(
  val name: String,
  val referredPackages: Set<String>,
  @LibType val type: Int
)
