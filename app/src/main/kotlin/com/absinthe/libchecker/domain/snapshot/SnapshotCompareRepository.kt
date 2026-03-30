package com.absinthe.libchecker.domain.snapshot

import com.absinthe.libchecker.database.entity.SnapshotDiffStoringItem
import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TrackItem

interface SnapshotCompareRepository {
  suspend fun getSnapshots(timeStamp: Long): List<SnapshotItem>

  suspend fun getTrackItems(): List<TrackItem>

  suspend fun getSnapshotDiff(packageName: String): SnapshotDiffStoringItem?

  suspend fun saveSnapshotDiff(item: SnapshotDiffStoringItem)

  fun clearSnapshotDiffItems()
}
