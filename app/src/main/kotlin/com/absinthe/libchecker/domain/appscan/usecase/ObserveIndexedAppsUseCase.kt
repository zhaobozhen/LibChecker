package com.absinthe.libchecker.domain.appscan.usecase

import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.appscan.IndexedAppRepository
import kotlinx.coroutines.flow.Flow

class ObserveIndexedAppsUseCase(
  private val indexedAppRepository: IndexedAppRepository
) {
  operator fun invoke(): Flow<List<LCItem>> = indexedAppRepository.observeIndexedApps()
}
