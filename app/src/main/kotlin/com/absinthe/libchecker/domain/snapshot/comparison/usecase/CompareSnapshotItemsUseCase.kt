package com.absinthe.libchecker.domain.snapshot.comparison.usecase

import com.absinthe.libchecker.database.entity.SnapshotItem
import com.absinthe.libchecker.domain.app.detail.model.LibStringItem
import com.absinthe.libchecker.domain.snapshot.model.ADDED
import com.absinthe.libchecker.domain.snapshot.model.CHANGED
import com.absinthe.libchecker.domain.snapshot.model.MOVED
import com.absinthe.libchecker.domain.snapshot.model.REMOVED
import com.absinthe.libchecker.domain.snapshot.model.SnapshotDiffItem
import com.absinthe.libchecker.utils.dex.DexEntryInfo
import com.absinthe.libchecker.utils.dex.ResourceEntryInfo
import com.absinthe.libchecker.utils.fromJson

class CompareSnapshotItemsUseCase {

  operator fun invoke(
    oldInfo: SnapshotItem?,
    newInfo: SnapshotItem?,
    trackPackageNames: Set<String>
  ): SnapshotDiffItem? {
    if (oldInfo == null && newInfo == null) {
      return null
    } else if (newInfo == null || oldInfo == null) {
      val targetInfo = newInfo ?: oldInfo!!
      val newInstalled = newInfo != null
      return SnapshotDiffItem(
        targetInfo.packageName,
        targetInfo.lastUpdatedTime,
        SnapshotDiffItem.DiffNode(targetInfo.label),
        SnapshotDiffItem.DiffNode(targetInfo.versionName),
        SnapshotDiffItem.DiffNode(targetInfo.versionCode),
        SnapshotDiffItem.DiffNode(targetInfo.abi),
        SnapshotDiffItem.DiffNode(targetInfo.targetApi),
        SnapshotDiffItem.DiffNode(targetInfo.compileSdk),
        SnapshotDiffItem.DiffNode(targetInfo.minSdk),
        SnapshotDiffItem.DiffNode(targetInfo.nativeLibs),
        SnapshotDiffItem.DiffNode(targetInfo.services),
        SnapshotDiffItem.DiffNode(targetInfo.activities),
        SnapshotDiffItem.DiffNode(targetInfo.receivers),
        SnapshotDiffItem.DiffNode(targetInfo.providers),
        SnapshotDiffItem.DiffNode(targetInfo.permissions),
        SnapshotDiffItem.DiffNode(targetInfo.metadata),
        SnapshotDiffItem.DiffNode(targetInfo.packageSize),
        SnapshotDiffItem.DiffNode(targetInfo.dexInfo),
        SnapshotDiffItem.DiffNode(targetInfo.resourcesSize),
        resourceInfoDiff = SnapshotDiffItem.DiffNode(targetInfo.resourceInfo),
        newInstalled = newInstalled,
        deleted = !newInstalled,
        isTrackItem = targetInfo.packageName in trackPackageNames,
        archivedDiff = SnapshotDiffItem.DiffNode(targetInfo.isArchived)
      )
    } else {
      val hasComparableDexStats = oldInfo.hasDexStats() && newInfo.hasDexStats()
      val hasComparableResourceStats =
        oldInfo.hasResourceStats() && newInfo.hasResourceStats()
      return SnapshotDiffItem(
        packageName = newInfo.packageName,
        updateTime = newInfo.lastUpdatedTime,
        labelDiff = SnapshotDiffItem.DiffNode(oldInfo.label, newInfo.label),
        versionNameDiff = SnapshotDiffItem.DiffNode(oldInfo.versionName, newInfo.versionName),
        versionCodeDiff = SnapshotDiffItem.DiffNode(oldInfo.versionCode, newInfo.versionCode),
        abiDiff = SnapshotDiffItem.DiffNode(oldInfo.abi, newInfo.abi),
        targetApiDiff = SnapshotDiffItem.DiffNode(oldInfo.targetApi, newInfo.targetApi),
        compileSdkDiff = SnapshotDiffItem.DiffNode(oldInfo.compileSdk, newInfo.compileSdk),
        minSdkDiff = SnapshotDiffItem.DiffNode(oldInfo.minSdk, newInfo.minSdk),
        nativeLibsDiff = SnapshotDiffItem.DiffNode(oldInfo.nativeLibs, newInfo.nativeLibs),
        servicesDiff = SnapshotDiffItem.DiffNode(oldInfo.services, newInfo.services),
        activitiesDiff = SnapshotDiffItem.DiffNode(oldInfo.activities, newInfo.activities),
        receiversDiff = SnapshotDiffItem.DiffNode(oldInfo.receivers, newInfo.receivers),
        providersDiff = SnapshotDiffItem.DiffNode(oldInfo.providers, newInfo.providers),
        permissionsDiff = SnapshotDiffItem.DiffNode(oldInfo.permissions, newInfo.permissions),
        metadataDiff = SnapshotDiffItem.DiffNode(oldInfo.metadata, newInfo.metadata),
        packageSizeDiff = SnapshotDiffItem.DiffNode(oldInfo.packageSize, newInfo.packageSize),
        dexInfoDiff = if (hasComparableDexStats) {
          SnapshotDiffItem.DiffNode(oldInfo.dexInfo, newInfo.dexInfo)
        } else {
          SnapshotDiffItem.DiffNode("")
        },
        resourcesSizeDiff = if (hasComparableResourceStats) {
          SnapshotDiffItem.DiffNode(oldInfo.resourcesSize, newInfo.resourcesSize)
        } else {
          SnapshotDiffItem.DiffNode(0L)
        },
        resourceInfoDiff = if (hasComparableResourceStats) {
          SnapshotDiffItem.DiffNode(oldInfo.resourceInfo, newInfo.resourceInfo)
        } else {
          SnapshotDiffItem.DiffNode("")
        },
        isTrackItem = newInfo.packageName in trackPackageNames,
        archivedDiff = SnapshotDiffItem.DiffNode(oldInfo.isArchived, newInfo.isArchived)
      ).apply {
        val diffIndicator = compareDiffIndicator(this)
        added = diffIndicator.added
        removed = diffIndicator.removed
        changed = diffIndicator.changed
        moved = diffIndicator.moved
      }
    }
  }

  private fun compareDiffIndicator(item: SnapshotDiffItem): DiffIndicator {
    val native = compareNativeDiff(
      item.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      item.nativeLibsDiff.new?.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      )
    )
    val services = compareComponentsDiff(item.servicesDiff)
    val activities = compareComponentsDiff(item.activitiesDiff)
    val receivers = compareComponentsDiff(item.receiversDiff)
    val providers = compareComponentsDiff(item.providersDiff)
    val permissions = comparePermissionsDiff(
      item.permissionsDiff.old.fromJson<List<String>>(
        List::class.java,
        String::class.java
      ).orEmpty().toSet(),
      item.permissionsDiff.new?.fromJson<List<String>>(
        List::class.java,
        String::class.java
      )?.toSet()
    )
    val metadata = compareMetadataDiff(
      item.metadataDiff.old.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      ) ?: emptyList(),
      item.metadataDiff.new?.fromJson<List<LibStringItem>>(
        List::class.java,
        LibStringItem::class.java
      )
    )
    val dex = compareDexDiff(item.dexInfoDiff)
    val resources = compareResourceDiff(item.resourceInfoDiff)

    return DiffIndicator().apply {
      added =
        native.added + services.added + activities.added + receivers.added + providers.added + permissions.added + metadata.added + dex.added + resources.added
      removed =
        native.removed + services.removed + activities.removed + receivers.removed + providers.removed + permissions.removed + metadata.removed + dex.removed + resources.removed
      changed =
        native.changed + metadata.changed + dex.changed + resources.changed
      moved =
        services.moved + activities.moved + receivers.moved + providers.moved
    }
  }

  private fun compareDexDiff(
    diffNode: SnapshotDiffItem.DiffNode<String>
  ): DiffIndicator {
    val newJson = diffNode.new ?: return DiffIndicator()
    val oldByName = diffNode.old.fromJson<List<DexEntryInfo>>(
      List::class.java,
      DexEntryInfo::class.java
    ).orEmpty().associateBy(DexEntryInfo::name)
    val newByName = newJson.fromJson<List<DexEntryInfo>>(
      List::class.java,
      DexEntryInfo::class.java
    ).orEmpty().associateBy(DexEntryInfo::name)
    return DiffIndicator().apply {
      visitKeyedSnapshotDiff(oldByName, newByName) { status, _, _ -> record(status) }
    }
  }

  private fun compareResourceDiff(
    diffNode: SnapshotDiffItem.DiffNode<String>
  ): DiffIndicator {
    val newJson = diffNode.new ?: return DiffIndicator()
    val oldByName = diffNode.old.fromJson<List<ResourceEntryInfo>>(
      List::class.java,
      ResourceEntryInfo::class.java
    ).orEmpty().associateBy(ResourceEntryInfo::name)
    val newByName = newJson.fromJson<List<ResourceEntryInfo>>(
      List::class.java,
      ResourceEntryInfo::class.java
    ).orEmpty().associateBy(ResourceEntryInfo::name)
    return DiffIndicator().apply {
      visitKeyedSnapshotDiff(oldByName, newByName) { status, _, _ -> record(status) }
    }
  }

  private fun compareNativeDiff(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): DiffIndicator {
    if (newList == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    return compareNamedItems(oldList, newList) { old, new -> old.size != new.size }
  }

  private fun compareComponentsDiff(diffNode: SnapshotDiffItem.DiffNode<String>): DiffIndicator {
    if (diffNode.new == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    val oldSet = diffNode.old.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ).orEmpty().toSet()
    val newSet = diffNode.new.fromJson<List<String>>(
      List::class.java,
      String::class.java
    ).orEmpty().toSet()

    return DiffIndicator().apply {
      visitComponentSnapshotDiff(oldSet, newSet) { status, _, _ -> record(status) }
    }
  }

  private fun comparePermissionsDiff(
    oldSet: Set<String>,
    newSet: Set<String>?
  ): DiffIndicator {
    if (newSet == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    return DiffIndicator().apply {
      visitSetSnapshotDiff(oldSet, newSet) { status, _, _ -> record(status) }
    }
  }

  private fun compareMetadataDiff(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): DiffIndicator {
    if (newList == null) {
      return DiffIndicator(removed = Int.MAX_VALUE)
    }

    return compareNamedItems(oldList, newList) { old, new -> old.source != new.source }
  }

  private fun compareNamedItems(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>,
    changed: (LibStringItem, LibStringItem) -> Boolean
  ): DiffIndicator {
    return DiffIndicator().apply {
      visitNamedSnapshotDiff(oldList, newList, changed) { status, _, _ -> record(status) }
    }
  }

  private data class DiffIndicator(
    var added: Int = 0,
    var removed: Int = 0,
    var changed: Int = 0,
    var moved: Int = 0
  ) {
    fun record(status: Int) {
      when (status) {
        ADDED -> added++
        REMOVED -> removed++
        CHANGED -> changed++
        MOVED -> moved++
      }
    }
  }
}
