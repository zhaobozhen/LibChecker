package com.absinthe.libchecker.domain.app.detail.ui.view

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceAction
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceBottomSheetDisplay
import com.absinthe.libchecker.domain.app.detail.model.AppInstallSourceItemDisplay
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.view.app.BottomSheetScaffoldView

class AppInstallSourceBottomSheetView(context: Context) : BottomSheetScaffoldView(context) {

  private val originatingView = AppInstallSourceItemView(
    context,
    context.getString(R.string.lib_detail_app_install_source_originating_package)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val installingView = AppInstallSourceItemView(
    context,
    context.getString(R.string.lib_detail_app_install_source_installing_package)
  ).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val installedTimeView = AppInstallTimeItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val dexoptView = AppDexoptItemView(context).apply {
    layoutParams = LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.WRAP_CONTENT
    )
  }

  private val contentViews: List<View> = listOf(
    originatingView,
    installingView,
    installedTimeView,
    dexoptView
  )

  init {
    setPadding(24.dp, 16.dp, 24.dp, 16.dp)
    header.title.text = context.getString(R.string.lib_detail_app_install_source_title)
    (header.layoutParams as LayoutParams).apply {
      marginStart = -paddingStart
      marginEnd = -paddingEnd
    }
    contentViews.forEach { addView(it) }
  }

  fun bind(
    display: AppInstallSourceBottomSheetDisplay,
    onAction: (AppInstallSourceAction) -> Unit
  ) {
    bindAppItem(originatingView, display.originatingApp, onAction)
    bindAppItem(installingView, display.installingApp, onAction)

    installedTimeView.isVisible = display.installedTime != null
    display.installedTime?.let(installedTimeView::bind)

    dexoptView.isVisible = display.dexoptInfo != null
    display.dexoptInfo?.let(dexoptView::bind)
    requestLayout()
  }

  private fun bindAppItem(
    view: AppInstallSourceItemView,
    display: AppInstallSourceItemDisplay?,
    onAction: (AppInstallSourceAction) -> Unit
  ) {
    view.isVisible = display != null
    display?.let { view.bind(it, onAction) }
  }
}
