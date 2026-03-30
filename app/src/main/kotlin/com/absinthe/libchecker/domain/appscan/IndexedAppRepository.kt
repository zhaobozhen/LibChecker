package com.absinthe.libchecker.domain.appscan

import com.absinthe.libchecker.database.entity.LCItem
import kotlinx.coroutines.flow.Flow

interface IndexedAppRepository {
  fun observeIndexedApps(): Flow<List<LCItem>>

  suspend fun getIndexedApps(): List<LCItem>

  suspend fun insert(item: LCItem)

  suspend fun insert(items: List<LCItem>)

  suspend fun update(item: LCItem)

  suspend fun deleteByPackageName(packageName: String)

  suspend fun deleteAllItems()
}
