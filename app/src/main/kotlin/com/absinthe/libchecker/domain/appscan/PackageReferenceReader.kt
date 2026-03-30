package com.absinthe.libchecker.domain.appscan

import com.absinthe.libchecker.annotation.LibType

interface PackageReferenceReader {
  fun getReferences(
    packageName: String,
    @LibType type: Int
  ): Collection<String>
}
