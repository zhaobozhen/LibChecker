package com.absinthe.libchecker.domain.app.update

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateFlavorBoundaryTest {

  @Test
  fun selfUpdatePermissionAndReceiverAreFossOnly() {
    val projectDir = findProjectDir()
    val mainManifest = projectDir.resolve("src/main/AndroidManifest.xml").readText()
    val marketManifest = projectDir.resolve("src/market/AndroidManifest.xml").readText()
    val fossManifestPath = projectDir.resolve("src/foss/AndroidManifest.xml")

    assertFalse(mainManifest.contains(SELF_UPDATE_PERMISSION))
    assertFalse(mainManifest.contains(INSTALL_RESULT_RECEIVER))
    assertFalse(marketManifest.contains(SELF_UPDATE_PERMISSION))
    assertFalse(marketManifest.contains(INSTALL_RESULT_RECEIVER))
    assertTrue(fossManifestPath.exists())

    val fossManifest = fossManifestPath.readText()
    assertTrue(fossManifest.contains(SELF_UPDATE_PERMISSION))
    assertTrue(fossManifest.contains(INSTALL_RESULT_RECEIVER))
  }

  private fun findProjectDir(): Path {
    return generateSequence(Path.of("").toAbsolutePath()) { it.parent }
      .map { it.resolve("app") }
      .first { Files.isDirectory(it.resolve("src/main")) }
  }

  private companion object {
    const val SELF_UPDATE_PERMISSION = "android.permission.UPDATE_PACKAGES_WITHOUT_USER_ACTION"
    const val INSTALL_RESULT_RECEIVER = ".data.app.update.AppUpdateInstallResultReceiver"
  }
}
