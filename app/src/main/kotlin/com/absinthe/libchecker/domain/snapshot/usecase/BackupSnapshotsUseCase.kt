package com.absinthe.libchecker.domain.snapshot.usecase

import com.absinthe.libchecker.domain.snapshot.SnapshotArchiveRepository
import com.absinthe.libchecker.protocol.Snapshot
import timber.log.Timber

class BackupSnapshotsUseCase(
  private val snapshotArchiveRepository: SnapshotArchiveRepository
) {
  suspend operator fun invoke(outputStream: java.io.OutputStream) {
    outputStream.use { os ->
      val snapshotBuilder: Snapshot.Builder = Snapshot.newBuilder()
      snapshotArchiveRepository.getTimeStamps().forEach { (timestamp, _, _) ->
        val backupList = snapshotArchiveRepository.getSnapshots(timestamp)
        Timber.d("backup: timestamps=$timestamp, count=${backupList.size}")
        backupList.forEach {
          snapshotBuilder.apply {
            packageName = it.packageName
            timeStamp = it.timeStamp
            label = it.label
            versionName = it.versionName
            versionCode = it.versionCode
            installedTime = it.installedTime
            lastUpdatedTime = it.lastUpdatedTime
            isSystem = it.isSystem
            abi = it.abi.toInt()
            targetApi = it.targetApi.toInt()
            nativeLibs = it.nativeLibs
            services = it.services
            activities = it.activities
            receivers = it.receivers
            providers = it.providers
            permissions = it.permissions
            metadata = it.metadata
            packageSize = it.packageSize
            compileSdk = it.compileSdk.toInt()
            minSdk = it.minSdk.toInt()
          }

          snapshotBuilder.build().writeDelimitedTo(os)
        }
      }
    }
  }
}
