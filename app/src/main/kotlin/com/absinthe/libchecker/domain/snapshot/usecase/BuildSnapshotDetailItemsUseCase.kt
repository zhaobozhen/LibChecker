package com.absinthe.libchecker.domain.snapshot.usecase

import android.content.Context
import com.absinthe.libchecker.data.snapshot.SnapshotDetailComposer
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDetailItem
import com.absinthe.libchecker.features.snapshot.detail.bean.SnapshotDiffItem

class BuildSnapshotDetailItemsUseCase(
  private val snapshotDetailComposer: SnapshotDetailComposer
) {
  operator fun invoke(
    context: Context,
    entity: SnapshotDiffItem
  ): List<SnapshotDetailItem> {
    return snapshotDetailComposer.compose(context, entity)
  }
}
