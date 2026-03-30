package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.LCRepository
import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TrackItem
import com.absinthe.libchecker.domain.snapshot.SnapshotCompareRepository

class LCSnapshotCompareRepository(
  private val repository: LCRepository
) : SnapshotCompareRepository {
  override suspend fun getSnapshots(timeStamp: Long): List<SnapshotItem> {
    return repository.getSnapshots(timeStamp)
  }

  override suspend fun getTrackItems(): List<TrackItem> {
    return repository.getTrackItems()
  }

  override suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem? {
    return repository.getSnapshotDiff(packageName)
  }

  override suspend fun saveSnapshotDiff(item: SnapshotDiffStoringItem) {
    repository.insertSnapshotDiffItems(item)
  }

  override fun clearSnapshotDiffItems() {
    repository.deleteAllSnapshotDiffItems()
  }
}
