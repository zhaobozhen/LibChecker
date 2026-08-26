@file:Suppress("FunctionName")

package com.absinthe.libchecker.ui.compose

import android.content.Context
import androidx.appcompat.R as AppCompatR
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.absinthe.libchecker.utils.extensions.getColorByAttr
import com.google.android.material.R as MaterialR

@Composable
fun LibCheckerTheme(content: @Composable () -> Unit) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val colorScheme = remember(context, configuration.uiMode) {
    context.libCheckerColorScheme()
  }

  MaterialTheme(
    colorScheme = colorScheme,
    content = content
  )
}

private fun Context.libCheckerColorScheme() = lightColorScheme(
  primary = color(AppCompatR.attr.colorPrimary),
  onPrimary = color(MaterialR.attr.colorOnPrimary),
  primaryContainer = color(MaterialR.attr.colorPrimaryContainer),
  onPrimaryContainer = color(MaterialR.attr.colorOnPrimaryContainer),
  secondary = color(MaterialR.attr.colorSecondary),
  onSecondary = color(MaterialR.attr.colorOnSecondary),
  secondaryContainer = color(MaterialR.attr.colorSecondaryContainer),
  onSecondaryContainer = color(MaterialR.attr.colorOnSecondaryContainer),
  tertiary = color(MaterialR.attr.colorTertiary),
  onTertiary = color(MaterialR.attr.colorOnTertiary),
  tertiaryContainer = color(MaterialR.attr.colorTertiaryContainer),
  onTertiaryContainer = color(MaterialR.attr.colorOnTertiaryContainer),
  background = color(MaterialR.attr.colorSurface),
  onBackground = color(MaterialR.attr.colorOnSurface),
  surface = color(MaterialR.attr.colorSurface),
  onSurface = color(MaterialR.attr.colorOnSurface),
  surfaceVariant = color(MaterialR.attr.colorSurfaceVariant),
  onSurfaceVariant = color(MaterialR.attr.colorOnSurfaceVariant),
  surfaceTint = color(AppCompatR.attr.colorPrimary),
  surfaceDim = color(MaterialR.attr.colorSurfaceDim),
  surfaceBright = color(MaterialR.attr.colorSurfaceBright),
  surfaceContainerLowest = color(MaterialR.attr.colorSurfaceContainerLowest),
  surfaceContainerLow = color(MaterialR.attr.colorSurfaceContainerLow),
  surfaceContainer = color(MaterialR.attr.colorSurfaceContainer),
  surfaceContainerHigh = color(MaterialR.attr.colorSurfaceContainerHigh),
  surfaceContainerHighest = color(MaterialR.attr.colorSurfaceContainerHighest),
  error = color(AppCompatR.attr.colorError),
  onError = color(MaterialR.attr.colorOnError),
  errorContainer = color(MaterialR.attr.colorErrorContainer),
  onErrorContainer = color(MaterialR.attr.colorOnErrorContainer),
  outline = color(MaterialR.attr.colorOutline),
  outlineVariant = color(MaterialR.attr.colorOutlineVariant),
  inverseSurface = color(MaterialR.attr.colorSurfaceInverse),
  inverseOnSurface = color(MaterialR.attr.colorOnSurfaceInverse),
  inversePrimary = color(MaterialR.attr.colorPrimaryInverse),
  primaryFixed = color(MaterialR.attr.colorPrimaryFixed),
  primaryFixedDim = color(MaterialR.attr.colorPrimaryFixedDim),
  onPrimaryFixed = color(MaterialR.attr.colorOnPrimaryFixed),
  onPrimaryFixedVariant = color(MaterialR.attr.colorOnPrimaryFixedVariant),
  secondaryFixed = color(MaterialR.attr.colorSecondaryFixed),
  secondaryFixedDim = color(MaterialR.attr.colorSecondaryFixedDim),
  onSecondaryFixed = color(MaterialR.attr.colorOnSecondaryFixed),
  onSecondaryFixedVariant = color(MaterialR.attr.colorOnSecondaryFixedVariant),
  tertiaryFixed = color(MaterialR.attr.colorTertiaryFixed),
  tertiaryFixedDim = color(MaterialR.attr.colorTertiaryFixedDim),
  onTertiaryFixed = color(MaterialR.attr.colorOnTertiaryFixed),
  onTertiaryFixedVariant = color(MaterialR.attr.colorOnTertiaryFixedVariant)
)

private fun Context.color(attr: Int): Color = Color(getColorByAttr(attr))
