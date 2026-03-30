package com.absinthe.libchecker.data.appscan

import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.LCItem
import com.absinthe.libchecker.domain.appscan.IndexedAppRepository
import kotlinx.coroutines.flow.Flow

class LCIndexedAppRepository(
  private val repository: LCRepository
) : IndexedAppRepository {
  override fun observeIndexedApps(): Flow<List<LCItem>> = repository.allLCItemsFlow

  override suspend fun getIndexedApps(): List<LCItem> = repository.getLCItems()

  override suspend fun insert(item: LCItem) {
    repository.insert(item)
  }

  override suspend fun insert(items: List<LCItem>) {
    repository.insert(items)
  }

  override suspend fun update(item: LCItem) {
    repository.update(item)
  }

  override suspend fun deleteByPackageName(packageName: String) {
    repository.deleteLCItemByPackageName(packageName)
  }

  override suspend fun deleteAllItems() {
    repository.deleteAllItems()
  }
}
