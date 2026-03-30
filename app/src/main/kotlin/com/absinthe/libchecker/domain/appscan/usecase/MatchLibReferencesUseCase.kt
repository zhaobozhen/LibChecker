package com.absinthe.libchecker.domain.appscan.usecase

import com.absinthe.libchecker.annotation.ACTION
import com.absinthe.libchecker.annotation.ACTION_IN_RULES
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.domain.appscan.RuleProvider
import com.absinthe.libchecker.domain.appscan.model.AppReferenceNode
import com.absinthe.libchecker.features.statistics.bean.LibReference

class MatchLibReferencesUseCase(
  private val ruleProvider: RuleProvider
) {
  suspend operator fun invoke(
    references: List<AppReferenceNode>,
    threshold: Int,
    onlyNotMarked: Boolean,
    onProgress: (Int) -> Unit = {}
  ): List<LibReference> {
    if (references.isEmpty()) {
      onProgress(100)
      return emptyList()
    }

    val refList = mutableListOf<LibReference>()
    val total = references.size
    onProgress(0)

    references.forEachIndexed { index, reference ->
      if (reference.referredPackages.size >= threshold && reference.name.isNotBlank()) {
        val ruleType = if (reference.type == ACTION) ACTION_IN_RULES else reference.type
        val rule = if (reference.type != PERMISSION && reference.type != METADATA) {
          ruleProvider.getRule(reference.name, ruleType, markMatched = true)
        } else {
          null
        }

        if (!onlyNotMarked || rule == null) {
          refList += LibReference(
            libName = reference.name,
            rule = rule,
            referredList = reference.referredPackages,
            type = reference.type
          )
        }
      }

      onProgress((index + 1) * 100 / total)
    }

    return refList.sortedByDescending { it.referredList.size }
  }
}
