package com.absinthe.libchecker.domain.appscan

interface AbiLabelFormatter {
  fun getAbiLabel(abi: Int): String
}
