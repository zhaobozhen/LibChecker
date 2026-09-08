package com.absinthe.libchecker.domain.snapshot.comparison.ui.view

import android.content.Context
import android.text.TextUtils
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.snapshot.comparison.model.ComparisonDashboardSideState
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.absinthe.libchecker.utils.extensions.getResourceIdByAttr

class ComparisonDashboardHalfView(
  context: Context,
  horizontalGravity: Int
) : LinearLayout(context) {

  private val tvSnapshotTimestampTitle =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      text = context.getString(R.string.snapshot_current_timestamp)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    }

  private val tvSnapshotTimestampText =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      text = context.getString(R.string.album_click_to_choose)
      maxLines = 1
      ellipsize = TextUtils.TruncateAt.MIDDLE
    }

  private val tvSnapshotAppsCountTitle =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerif)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      ).also {
        it.topMargin = 5.dp
      }
      text = context.getString(R.string.comparison_snapshot_apps_count)
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
    }

  private val tvSnapshotAppsCountText =
    AppCompatTextView(ContextThemeWrapper(context, R.style.TextView_SansSerifBlack)).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
      )
      setTextColor(context.getColorByAttr(com.google.android.material.R.attr.colorOnSurface))
      setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      // noinspection AndroidLintSetTextI18n
      text = "0"
    }

  init {
    orientation = VERTICAL
    tvSnapshotTimestampTitle.gravity = horizontalGravity
    tvSnapshotTimestampText.gravity = horizontalGravity
    tvSnapshotAppsCountTitle.gravity = horizontalGravity
    tvSnapshotAppsCountText.gravity = horizontalGravity
    addView(tvSnapshotTimestampTitle)
    addView(tvSnapshotTimestampText)
    addView(tvSnapshotAppsCountTitle)
    addView(tvSnapshotAppsCountText)
    setBackgroundResource(context.getResourceIdByAttr(android.R.attr.selectableItemBackgroundBorderless))
  }

  internal fun bind(sideState: ComparisonDashboardSideState) {
    tvSnapshotTimestampText.text = sideState.timestampText
    tvSnapshotAppsCountText.text = sideState.appsCountText
    contentDescription = sideState.contentDescription
  }
}
