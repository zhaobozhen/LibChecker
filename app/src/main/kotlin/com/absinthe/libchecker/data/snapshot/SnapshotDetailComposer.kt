package com.absinthe.libchecker.data.snapshot

import android.content.Context
import android.graphics.Color
import android.text.style.ForegroundColorSpan
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import com.absinthe.libchecker.annotation.ACTIVITY
import com.absinthe.libchecker.annotation.LibType
import com.absinthe.libchecker.annotation.METADATA
import com.absinthe.libchecker.annotation.NATIVE
import com.absinthe.libchecker.annotation.PERMISSION
import com.absinthe.libchecker.annotation.PROVIDER
import com.absinthe.libchecker.annotation.RECEIVER
import com.absinthe.libchecker.annotation.SERVICE
import com.absinthe.libchecker.features.snapshot.detail.bean.ADDED
import com.absinthe.libchecker.features.snapshot.detail.bean.CHANGED
import com.absinthe.libchecker.features.snapshot.detail.bean.MOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.REMOVED
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDetailItem
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem
import com.absinthe.libchecker.features.snapshot.ui.adapter.ARROW
import com.absinthe.libchecker.features.statistics.bean.LibStringItem
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.extensions.sizeToString
import com.absinthe.libchecker.utils.fromJson
import java.util.Locale
import kotlin.math.abs

class SnapshotDetailComposer {
  fun compose(
    context: Context,
    entity: SnapshotDiffItem
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    list.addAll(
      getNativeDiffList(
        context,
        entity.nativeLibsDiff.old.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        ) ?: emptyList(),
        entity.nativeLibsDiff.new?.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      )
    )
    addComponentDiffInfoFromJson(list, entity.servicesDiff, SERVICE)
    addComponentDiffInfoFromJson(list, entity.activitiesDiff, ACTIVITY)
    addComponentDiffInfoFromJson(list, entity.receiversDiff, RECEIVER)
    addComponentDiffInfoFromJson(list, entity.providersDiff, PROVIDER)

    list.addAll(
      getPermissionsDiffList(
        entity.permissionsDiff.old.fromJson<List<String>>(
          List::class.java,
          String::class.java
        ).orEmpty().toSet(),
        entity.permissionsDiff.new?.fromJson<List<String>>(
          List::class.java,
          String::class.java
        )?.toSet()
      )
    )

    list.addAll(
      getMetadataDiffList(
        entity.metadataDiff.old.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        ) ?: emptyList(),
        entity.metadataDiff.new?.fromJson<List<LibStringItem>>(
          List::class.java,
          LibStringItem::class.java
        )
      )
    )

    return list
  }

  private fun addComponentDiffInfoFromJson(
    list: MutableList<SnapshotDetailItem>,
    diffNode: SnapshotDiffItem.DiffNode<String>,
    @LibType libType: Int
  ) {
    val old =
      diffNode.old.fromJson<List<String>>(List::class.java, String::class.java).orEmpty().toSet()
    val new =
      diffNode.new?.fromJson<List<String>>(List::class.java, String::class.java)?.toSet()
    list.addAll(getComponentsDiffList(old, new, libType))
  }

  private fun getNativeDiffList(
    context: Context,
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()
    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val intersectList = mutableListOf<LibStringItem>()

    for (item in tempNewList) {
      oldList.find { it.name == item.name }?.let {
        if (it.size != item.size) {
          val diffSize = item.size - it.size
          val extra = buildSpannedString {
            append("${it.size.sizeToString(context)} $ARROW ${item.size.sizeToString(context)}")
            appendLine()
            inSpans(ForegroundColorSpan(ColorUtils.setAlphaComponent(Color.BLACK, 165))) {
              if (diffSize > 0) {
                append("+")
              }
              append(diffSize.sizeToString(context))
              append(", ")
              if (diffSize > 0) {
                append("+")
              }
              val percentage = diffSize.toFloat() / it.size
              if (abs(percentage) < 0.001f) {
                if (percentage < 0) {
                  append("-")
                }
                append("<0.1%")
              } else {
                append(String.format(Locale.getDefault(), "%.1f%%", percentage * 100))
              }
            }
          }
          list.add(
            SnapshotDetailItem(
              name = it.name,
              title = it.name,
              extra = extra,
              diffType = CHANGED,
              itemType = NATIVE
            )
          )
        }
        intersectList.add(item)
      }
    }

    for (item in intersectList) {
      tempOldList.remove(tempOldList.find { it.name == item.name })
      tempNewList.remove(tempNewList.find { it.name == item.name })
    }

    for (item in tempOldList) {
      list.add(
        SnapshotDetailItem(
          name = item.name,
          title = item.name,
          extra = PackageUtils.sizeToString(context, item),
          diffType = REMOVED,
          itemType = NATIVE
        )
      )
    }
    for (item in tempNewList) {
      list.add(
        SnapshotDetailItem(
          name = item.name,
          title = item.name,
          extra = PackageUtils.sizeToString(context, item),
          diffType = ADDED,
          itemType = NATIVE
        )
      )
    }

    return list
  }

  private fun getComponentsDiffList(
    oldSet: Set<String>,
    newSet: Set<String>?,
    @LibType type: Int
  ): List<SnapshotDetailItem> {
    if (newSet == null) {
      return emptyList()
    }

    val list = mutableListOf<SnapshotDetailItem>()
    val removeList = (oldSet - newSet).toMutableSet()
    val addList = (newSet - oldSet).toMutableSet()
    val pendingRemovedOldSet = mutableSetOf<String>()
    val pendingRemovedNewSet = mutableSetOf<String>()

    for (item in addList) {
      removeList.find { it.substringAfterLast(".") == item.substringAfterLast(".") }?.let {
        list.add(
          SnapshotDetailItem(item, String.format("%s\n$ARROW\n%s", it, item), "", MOVED, type)
        )
        pendingRemovedOldSet.add(it)
        pendingRemovedNewSet.add(item)
      }
    }
    removeList.removeAll(pendingRemovedOldSet)
    addList.removeAll(pendingRemovedNewSet)

    removeList.forEach {
      list.add(SnapshotDetailItem(it, it, "", REMOVED, type))
    }
    addList.forEach {
      list.add(SnapshotDetailItem(it, it, "", ADDED, type))
    }

    return list
  }

  private fun getPermissionsDiffList(
    oldSet: Set<String>,
    newSet: Set<String>?
  ): List<SnapshotDetailItem> {
    if (newSet == null) {
      return emptyList()
    }

    val list = mutableListOf<SnapshotDetailItem>()
    val removeList = oldSet - newSet
    val addList = newSet - oldSet

    removeList.forEach {
      list.add(SnapshotDetailItem(it, it, "", REMOVED, PERMISSION))
    }
    addList.forEach {
      list.add(SnapshotDetailItem(it, it, "", ADDED, PERMISSION))
    }

    return list
  }

  private fun getMetadataDiffList(
    oldList: List<LibStringItem>,
    newList: List<LibStringItem>?
  ): List<SnapshotDetailItem> {
    val list = mutableListOf<SnapshotDetailItem>()

    if (newList == null) {
      return list
    }

    val tempOldList = oldList.toMutableList()
    val tempNewList = newList.toMutableList()
    val intersectList = mutableListOf<LibStringItem>()

    for (item in tempNewList) {
      oldList.find { it.name == item.name }?.let {
        if (it.source != item.source) {
          list.add(
            SnapshotDetailItem(
              name = it.name,
              title = it.name,
              extra = "${it.source.orEmpty()} $ARROW ${item.source.orEmpty()}",
              diffType = CHANGED,
              itemType = METADATA
            )
          )
        }
        intersectList.add(item)
      }
    }

    for (item in intersectList) {
      tempOldList.remove(tempOldList.find { it.name == item.name })
      tempNewList.remove(tempNewList.find { it.name == item.name })
    }

    for (item in tempOldList) {
      list.add(SnapshotDetailItem(item.name, item.name, item.source.orEmpty(), REMOVED, METADATA))
    }
    for (item in tempNewList) {
      list.add(SnapshotDetailItem(item.name, item.name, item.source.orEmpty(), ADDED, METADATA))
    }

    return list
  }
}
