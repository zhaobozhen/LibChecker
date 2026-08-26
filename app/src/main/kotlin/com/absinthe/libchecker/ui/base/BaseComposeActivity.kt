package com.absinthe.libchecker.ui.base

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import android.text.method.TextKeyListener
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.absinthe.libchecker.constant.GlobalValues
import com.absinthe.libchecker.utils.OsUtils
import java.util.Locale
import timber.log.Timber

abstract class BaseComposeActivity : AppCompatActivity() {

  private var appliedLocale: Locale? = null

  override fun attachBaseContext(newBase: Context) {
    val locale = GlobalValues.locale
    appliedLocale = locale
    Locale.setDefault(locale)
    val configuration = Configuration(newBase.resources.configuration).apply {
      setLocale(locale)
      setLayoutDirection(locale)
    }
    super.attachBaseContext(newBase.createConfigurationContext(configuration))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    onApplyUserThemeResource(theme)
    enableEdgeToEdge()
    super.onCreate(savedInstanceState.discardIfContainsUnreadableParcelable(javaClass.classLoader))
    ThemeTransitionController.animateEnterIfNeeded(this)
  }

  override fun onResume() {
    super.onResume()
    if (OsUtils.atLeastT() && appliedLocale != GlobalValues.locale) {
      recreate()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    ThemeTransitionController.onActivityDestroyed(this)
    releaseTextKeyListeners()
  }

  open fun onApplyUserThemeResource(theme: Resources.Theme) {
    theme.applyStyle(
      resolveUserThemeOverlay(GlobalValues.isAmoledTheme, resources.configuration.uiMode),
      true
    )
  }

  private fun releaseTextKeyListeners() {
    runCatching {
      TextKeyListener.Capitalize.values().forEach { capitalize ->
        TextKeyListener.getInstance(false, capitalize).release()
        TextKeyListener.getInstance(true, capitalize).release()
      }
    }.onFailure {
      Timber.w(it)
    }
  }
}
