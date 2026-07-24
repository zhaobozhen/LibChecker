package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.snapshot.backup.archive.SnapshotArchiveCodec
import com.absinthe.libchecker.protocol.Snapshot
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.DexStatsCollector
import com.absinthe.libchecker.utils.fromJson
import java.io.InputStream
import java.io.OutputStream

class ProtoSnapshotArchiveCodec : SnapshotArchiveCodec {

  override fun read(inputStream: InputStream): SnapshotItem? {
    return Snapshot.parseDelimitedFrom(inputStream)?.toSnapshotItem()
  }

  override fun write(item: SnapshotItem, outputStream: OutputStream) {
    item.toSnapshotMessage().writeDelimitedTo(outputStream)
  }

  private fun SnapshotItem.toSnapshotMessage(): Snapshot {
    return Snapshot.newBuilder().apply {
      packageName = this@toSnapshotMessage.packageName
      timeStamp = this@toSnapshotMessage.timeStamp
      label = this@toSnapshotMessage.label
      versionName = this@toSnapshotMessage.versionName
      versionCode = this@toSnapshotMessage.versionCode
      isArchived = this@toSnapshotMessage.isArchived
      installedTime = this@toSnapshotMessage.installedTime
      lastUpdatedTime = this@toSnapshotMessage.lastUpdatedTime
      isSystem = this@toSnapshotMessage.isSystem
      abi = this@toSnapshotMessage.abi.toInt()
      targetApi = this@toSnapshotMessage.targetApi.toInt()
      nativeLibs = this@toSnapshotMessage.nativeLibs
      services = this@toSnapshotMessage.services
      activities = this@toSnapshotMessage.activities
      receivers = this@toSnapshotMessage.receivers
      providers = this@toSnapshotMessage.providers
      permissions = this@toSnapshotMessage.permissions
      metadata = this@toSnapshotMessage.metadata
      packageSize = this@toSnapshotMessage.packageSize
      compileSdk = this@toSnapshotMessage.compileSdk.toInt()
      minSdk = this@toSnapshotMessage.minSdk.toInt()
      dexInfo = this@toSnapshotMessage.dexInfo
      resourcesSize = this@toSnapshotMessage.resourcesSize
      statsVersion = this@toSnapshotMessage.statsVersion
    }.build()
  }

  private fun Snapshot.toSnapshotItem(): SnapshotItem {
    val restored = SnapshotItem(
      id = null,
      packageName = packageName,
      timeStamp = timeStamp,
      label = label,
      versionName = versionName,
      versionCode = versionCode,
      isArchived = isArchived,
      installedTime = installedTime,
      lastUpdatedTime = lastUpdatedTime,
      isSystem = isSystem,
      abi = abi.toShort(),
      targetApi = targetApi.toShort(),
      nativeLibs = nativeLibs,
      services = services,
      activities = activities,
      receivers = receivers,
      providers = providers,
      permissions = permissions,
      metadata = metadata,
      packageSize = packageSize,
      compileSdk = compileSdk.toShort(),
      minSdk = minSdk.toShort(),
      dexInfo = dexInfo,
      resourcesSize = resourcesSize,
      statsVersion = statsVersion
    )
    if (restored.statsVersion != SnapshotItem.CURRENT_STATS_VERSION) {
      return restored.copy(
        dexInfo = "[]",
        resourcesSize = 0,
        statsVersion = 0
      )
    }
    val dexEntries = restored.dexInfo.fromJson<List<DexEntryInfo>>(
      List::class.java,
      DexEntryInfo::class.java
    )
    return if (
      dexEntries != null &&
      DexStatsCollector.isValidStoredStats(dexEntries, restored.resourcesSize)
    ) {
      restored
    } else {
      restored.copy(
        dexInfo = "[]",
        resourcesSize = 0,
        statsVersion = 0
      )
    }
  }
}
