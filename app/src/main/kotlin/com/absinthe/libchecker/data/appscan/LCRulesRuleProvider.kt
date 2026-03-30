package com.absinthe.libchecker.data.appscan

import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.domain.appscan.RuleProvider
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Rule

class LCRulesRuleProvider : RuleProvider {
  override suspend fun getRule(
    ruleName: String,
    @LibType type: Int,
    markMatched: Boolean
  ): Rule? {
    return LCRules.getRule(ruleName, type, markMatched)
  }
}
