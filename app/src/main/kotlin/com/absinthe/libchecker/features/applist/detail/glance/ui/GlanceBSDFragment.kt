package com.absinthe.libchecker.features.applist.detail.glance.ui

import android.content.DialogInterface
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.palette.graphics.Palette
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.features.applist.detail.glance.ui.view.GlanceBSDView
import com.absinthe.libchecker.features.applist.detail.ui.EXTRA_PACKAGE_NAME
import com.absinthe.libchecker.utils.PackageUtils
import com.absinthe.libchecker.utils.UiUtils
import com.absinthe.libchecker.utils.extensions.copyToClipboard
import com.absinthe.libchecker.utils.extensions.getAppName
import com.absinthe.libchecker.utils.extensions.getVersionCode
import com.absinthe.libchecker.utils.extensions.putArguments
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GlanceBSDFragment : BaseBottomSheetViewDialogFragment<GlanceBSDView>() {

  private val packageName by lazy {
    arguments?.getString(EXTRA_PACKAGE_NAME).orEmpty()
      .substringBefore(" ") // remove maxSdkVersion suffix
  }

  override fun initRootView(): GlanceBSDView = GlanceBSDView(requireContext())

  override fun init() {
    val pi = runCatching { PackageUtils.getPackageInfo(packageName) }.getOrNull() ?: return
    lifecycleScope.launch(Dispatchers.Default) {
      val icon = pi.applicationInfo!!.loadIcon(SystemServices.packageManager).toBitmap()
      val palette = Palette.from(icon).generate()
      val color = UiUtils.desaturateColor(palette.dominantSwatch!!.rgb, 0.618f)
      root.image.setOnLongClickListener { view ->
        root.image.copyToClipboard()
        true
      }

      withContext(Dispatchers.Main) {
        val info = GlanceBSDView.Info(
          appName = pi.getAppName() ?: "null",
          packageName = packageName,
          versionName = pi.versionName ?: "null",
          versionCode = pi.getVersionCode(),
          capsules = listOf(
            GlanceBSDView.CapsuleInfo(
              R.drawable.ic_gradle,
              "Gradle",
              "8.8"
            ),
            GlanceBSDView.CapsuleInfo(
              com.absinthe.lc.rulesbundle.R.drawable.ic_lib_android,
              "Android",
              "15"
            ),
            GlanceBSDView.CapsuleInfo(
              com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
              "Kotlin",
              "1.9.20"
            ),
            GlanceBSDView.CapsuleInfo(
              com.absinthe.lc.rulesbundle.R.drawable.ic_lib_kotlin,
              "Kotlin",
              "1.9.20"
            )
          )
        )
        root.drawImage(info, color)
      }
    }
  }

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun show(manager: FragmentManager, tag: String?) {
    if (!isShowing) {
      isShowing = true
      super.show(manager, tag)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    super.onDismiss(dialog)
    isShowing = false
  }

  companion object {
    fun newInstance(packageName: String): GlanceBSDFragment {
      return GlanceBSDFragment().putArguments(EXTRA_PACKAGE_NAME to packageName)
    }

    var isShowing = false
  }
}
