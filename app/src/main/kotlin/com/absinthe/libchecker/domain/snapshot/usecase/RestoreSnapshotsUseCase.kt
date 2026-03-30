package com.absinthe.libchecker.domain.snapshot.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.database.entity.TimeStampItem
import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveRepository
import com.absinthe.libchecker.domain.snapshot.model.RestoreSnapshotsResult
import com.absinthe.libchecker.protocol.Snapshot

class RestoreSnapshotsUseCase(
  private val snapshotArchiveRepository: SnapshotArchiveRepository
) {
  suspend operator fun invoke(inputStream: java.io.InputStream): RestoreSnapshotsResult {
    val restoredCountsByTimestamp = linkedMapOf<Long, Int>()

    inputStream.use { stream ->
      val batch = mutableListOf<Snapshot>()

      while (true) {
        val snapshot = Snapshot.parseDelimitedFrom(stream) ?: break
        batch += snapshot
        restoredCountsByTimestamp[snapshot.timeStamp] =
          restoredCountsByTimestamp.getOrDefault(snapshot.timeStamp, 0) + 1

        if (batch.size == 200) {
          restoreBatch(batch)
          batch.clear()
        }
      }

      if (batch.isNotEmpty()) {
        restoreBatch(batch)
        batch.clear()
      }
    }

    snapshotArchiveRepository.deleteDuplicateSnapshotItems()
    restoredCountsByTimestamp.keys
      .filter { it != 0L }
      .forEach { timestamp ->
        snapshotArchiveRepository.insertTimeStamp(TimeStampItem(timestamp, null, null))
      }

    return RestoreSnapshotsResult(
      restoredCountsByTimestamp = restoredCountsByTimestamp,
      latestTimestamp = restoredCountsByTimestamp.keys.maxOrNull()
    )
  }

  private suspend fun restoreBatch(list: List<Snapshot>) {
    val snapshotItems = list.map {
      SnapshotItem(
        id = null,
        packageName = it.packageName,
        timeStamp = it.timeStamp,
        label = it.label,
        versionName = it.versionName,
        versionCode = it.versionCode,
        installedTime = it.installedTime,
        lastUpdatedTime = it.lastUpdatedTime,
        isSystem = it.isSystem,
        abi = it.abi.toShort(),
        targetApi = it.targetApi.toShort(),
        nativeLibs = it.nativeLibs,
        services = it.services,
        activities = it.activities,
        receivers = it.receivers,
        providers = it.providers,
        permissions = it.permissions,
        metadata = it.metadata,
        packageSize = it.packageSize,
        compileSdk = it.compileSdk.toShort(),
        minSdk = it.minSdk.toShort()
      )
    }
    snapshotArchiveRepository.insertSnapshots(snapshotItems)
  }
}
