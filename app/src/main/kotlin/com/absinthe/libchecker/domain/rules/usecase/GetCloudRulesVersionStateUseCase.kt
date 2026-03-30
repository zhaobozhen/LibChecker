package com.absinthe.libchecker.domain.rules.usecase

import android.content.Context
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.domain.rules.model.CloudRulesVersionState

class GetCloudRulesVersionStateUseCase(
  private val cloudRulesRepository: CloudRulesRepository
) {
  suspend operator fun invoke(context: Context): CloudRulesVersionState? {
    val localVersion = cloudRulesRepository.getLocalVersion(context)
    val remoteVersion = cloudRulesRepository.getRemoteVersion() ?: return null

    return CloudRulesVersionState(
      localVersion = localVersion,
      remoteVersion = remoteVersion,
      hasUpdate = localVersion < remoteVersion
    )
  }
}
