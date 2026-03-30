package com.absinthe.libchecker.domain.rules.usecase

import android.content.Context
import com.absinthe.libchecker.domain.rules.CloudRulesRepository

class UpdateCloudRulesBundleUseCase(
  private val cloudRulesRepository: CloudRulesRepository
) {
  suspend operator fun invoke(
    context: Context,
    targetVersion: Int
  ): Boolean {
    return cloudRulesRepository.updateRulesBundle(context, targetVersion)
  }
}
