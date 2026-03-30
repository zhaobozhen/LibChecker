package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveRepository

class LCSnapshotArchiveRepository(
  private val repository: LCRepository
) : SnapshotArchiveRepository {
  override fun getTimeStamps(): List<TimeStampItem> {
    return repository.getTimeStamps()
  }

  override suspend fun getSnapshots(timeStamp: Long): List<SnapshotItem> {
    return repository.getSnapshots(timeStamp)
  }

  override suspend fun insertSnapshots(items: List<SnapshotItem>) {
    repository.insertSnapshots(items)
  }

  override suspend fun insertTimeStamp(item: TimeStampItem) {
    repository.insert(item)
  }

  override suspend fun deleteDuplicateSnapshotItems() {
    repository.deleteDuplicateSnapshotItems()
  }
}
