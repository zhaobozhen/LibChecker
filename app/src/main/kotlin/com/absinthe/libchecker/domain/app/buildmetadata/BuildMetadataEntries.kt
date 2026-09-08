package com.absinthe.libchecker.domain.app.buildmetadata

import com.absinthe.libchecker.compat.ZipFileCompat
import java.io.InputStreamReader
import java.util.Properties

internal val COMPOSE_VERSION_ENTRIES = arrayOf(
  "META-INF/androidx.compose.runtime_runtime.version",
  "META-INF/androidx.compose.ui_ui.version",
  "META-INF/androidx.compose.ui_ui-tooling-preview.version",
  "META-INF/androidx.compose.foundation_foundation.version",
  "META-INF/androidx.compose.animation_animation.version"
)

internal val DATA_BINDING_VERSION_ENTRIES = arrayOf(
  "META-INF/androidx.databinding_viewbinding.version",
  "META-INF/androidx.databinding_databindingKtx.version",
  "META-INF/androidx.databinding_library.version"
)

internal fun ZipFileCompat.readFirstPresentLine(entries: Array<String>): String? {
  entries.forEach { name ->
    getEntry(name)?.let { entry ->
      runCatching {
        InputStreamReader(getInputStream(entry), Charsets.UTF_8).buffered().use { it.readLine() }
          ?.takeIf { line -> line.isNotBlank() }
      }.getOrNull()?.let { return it }
    }
  }
  return null
}

internal fun ZipFileCompat.readAgpVersion(): String? {
  getEntry("META-INF/com/android/build/gradle/app-metadata.properties")?.let { entry ->
    runCatching {
      val properties = Properties()
      getInputStream(entry).use { properties.load(it) }
      properties.getProperty("androidGradlePluginVersion")?.takeIf { it.isNotBlank() }
    }.getOrNull()?.let { return it }
  }

  getEntry("META-INF/MANIFEST.MF")?.let { entry ->
    runCatching {
      InputStreamReader(getInputStream(entry), Charsets.UTF_8).buffered().useLines { lines ->
        lines.firstOrNull { it.startsWith("Created-By: Android Gradle ") }
          ?.removePrefix("Created-By: Android Gradle ")
          ?.takeIf { it.isNotBlank() }
      }
    }.getOrNull()?.let { return it }
  }

  return readFirstPresentLine(DATA_BINDING_VERSION_ENTRIES)
}
