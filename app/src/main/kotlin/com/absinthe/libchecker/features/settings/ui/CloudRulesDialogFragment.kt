package com.absinthe.libchecker.features.settings.ui

import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.absinthe.libchecker.R
import com.absinthe.libchecker.app.SystemServices
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.core.di.AppGraph
import com.absinthe.libchecker.utils.extensions.addPaddingTop
import com.absinthe.libchecker.utils.extensions.dp
import com.absinthe.libchecker.utils.showToast
import com.absinthe.libraries.utils.base.BaseBottomSheetViewDialogFragment
import com.absinthe.libraries.utils.view.BottomSheetHeaderView
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.launch
import timber.log.Timber

class CloudRulesDialogFragment : BaseBottomSheetViewDialogFragment<CloudRulesDialogView>() {
  private val getCloudRulesVersionState = AppGraph.getCloudRulesVersionStateUseCase
  private val updateCloudRulesBundle = AppGraph.updateCloudRulesBundleUseCase

  override fun initRootView(): CloudRulesDialogView = CloudRulesDialogView(requireContext())

  override fun getHeaderView(): BottomSheetHeaderView = root.getHeaderView()

  override fun init() {
    root.addPaddingTop(16.dp)
    root.cloudRulesContentView.updateButton.setOnClickListener {
      requestBundle()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
        runCatching {
          getCloudRulesVersionState(requireContext())
        }.onSuccess { state ->
          state ?: return@onSuccess
          root.cloudRulesContentView.localVersion.version.text = state.localVersion.toString()
          root.cloudRulesContentView.remoteVersion.version.text = state.remoteVersion.toString()
          root.cloudRulesContentView.setUpdateButtonStatus(state.hasUpdate)
          root.showContent()
        }.onFailure { throwable ->
          Timber.e(throwable)
          context?.showToast(R.string.toast_cloud_rules_update_error)
        }
      }
    }
  }

  private fun requestBundle() {
    val remoteVersion = root.cloudRulesContentView.remoteVersion.version.text.toString().toIntOrNull()
      ?: return
    lifecycleScope.launch {
      runCatching {
        updateCloudRulesBundle(requireContext(), remoteVersion)
      }.onSuccess { updated ->
        if (!updated) {
          context?.showToast(R.string.toast_cloud_rules_update_error)
          return@onSuccess
        }

        root.cloudRulesContentView.localVersion.version.text = remoteVersion.toString()
        root.cloudRulesContentView.setUpdateButtonStatus(false)
        restartApp()
      }.onFailure {
        Timber.e(it)
        context?.showToast(R.string.toast_cloud_rules_update_error)
      }
    }
  }

  private fun restartApp() {
    context?.let {
      val intent = SystemServices.packageManager.getLaunchIntentForPackage(it.packageName)
        ?.apply { putExtra(Constants.PP_FROM_CLOUD_RULES_UPDATE, true) }
        ?: return
      ProcessPhoenix.triggerRebirth(it, intent)
    }
  }
}
