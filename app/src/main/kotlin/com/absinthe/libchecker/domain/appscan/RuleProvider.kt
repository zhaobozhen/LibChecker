package com.absinthe.libchecker.domain.appscan

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.rulesbundle.Rule

interface RuleProvider {
  suspend fun getRule(
    ruleName: String,
    @LibType type: Int,
    markMatched: Boolean
  ): Rule?
}
