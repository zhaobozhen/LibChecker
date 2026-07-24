package com.absinthe.libchecker.utils.dex

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DexStatsCollectorTest {

  @get:Rule
  val temporaryFolder = TemporaryFolder()

  @Test
  fun collectsRootDexAndResourcesAcrossBaseAndSplits() {
    val base = createArchive(
      "base.apk",
      mapOf(
        "classes.dex" to byteArrayOf(1, 2, 3),
        "resources.arsc" to byteArrayOf(1, 2, 3, 4)
      )
    )
    val split = createArchive(
      "feature.apk",
      mapOf(
        "classes.dex" to byteArrayOf(1, 2),
        "resources.arsc" to byteArrayOf(1, 2, 3, 4, 5)
      )
    )

    val stats = DexStatsCollector.collect(
      listOf(
        DexStatsCollector.DexSource("base", base.path),
        DexStatsCollector.DexSource("split:feature", split.path)
      )
    )

    assertEquals(
      listOf("base/classes.dex", "split:feature/classes.dex"),
      stats.entries.map(DexEntryInfo::name)
    )
    assertEquals(listOf(3L, 2L), stats.entries.map(DexEntryInfo::size))
    assertEquals(9L, stats.resourcesSize)
  }

  @Test
  fun ignoresNestedDexEntries() {
    val archive = createArchive(
      "nested.apk",
      mapOf("assets/classes.dex" to byteArrayOf(1, 2, 3))
    )

    val stats = DexStatsCollector.collect(archive.path)

    assertTrue(stats.entries.isEmpty())
  }

  @Test
  fun rejectsArchivesWithTooManyDexEntries() {
    val entries = (1..101).associate { index ->
      "classes$index.dex" to byteArrayOf(1)
    }
    val archive = createArchive("many.apk", entries)

    val stats = DexStatsCollector.collect(archive.path)

    assertEquals(DexStatsCollector.DexStats(emptyList(), 0, false), stats)
  }

  @Test
  fun rejectsTooManyDexEntriesAcrossSources() {
    val first = createArchive(
      "first.apk",
      (1..60).associate { index -> "classes$index.dex" to byteArrayOf(1) }
    )
    val second = createArchive(
      "second.apk",
      (61..101).associate { index -> "classes$index.dex" to byteArrayOf(1) }
    )

    val stats = DexStatsCollector.collect(
      listOf(
        DexStatsCollector.DexSource("base", first.path),
        DexStatsCollector.DexSource("split:second", second.path)
      )
    )

    assertEquals(DexStatsCollector.DexStats(emptyList(), 0, false), stats)
  }

  @Test
  fun collectsCompatibilityResolvedSplitsForFrozenPackage() {
    val base = createArchive("base.apk", mapOf("classes.dex" to byteArrayOf(1)))
    createArchive("split_feature.apk", mapOf("classes.dex" to byteArrayOf(2)))
    val packageInfo = PackageInfo().apply {
      applicationInfo = ApplicationInfo().apply {
        sourceDir = base.path
        enabled = false
      }
    }

    val stats = DexStatsCollector.collect(packageInfo)

    assertEquals(
      listOf("base/classes.dex", "split:split_feature/classes.dex"),
      stats.entries.map(DexEntryInfo::name)
    )
  }

  private fun createArchive(
    name: String,
    entries: Map<String, ByteArray>
  ): File {
    val file = temporaryFolder.newFile(name)
    ZipOutputStream(file.outputStream()).use { output ->
      for ((entryName, content) in entries) {
        output.putNextEntry(ZipEntry(entryName))
        output.write(content)
        output.closeEntry()
      }
    }
    return file
  }
}
