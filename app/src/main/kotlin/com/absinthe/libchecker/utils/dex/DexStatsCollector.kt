package com.absinthe.libchecker.utils.dex

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import com.absinthe.libchecker.compat.ZipFileCompat
import com.absinthe.libchecker.utils.PackageUtils
import com.squareup.moshi.JsonClass
import java.io.File

@JsonClass(generateAdapter = true)
data class DexEntryInfo(
  val name: String,
  val size: Long,
  val classCount: Int
)

object DexStatsCollector {

  internal data class DexSource(
    val name: String,
    val path: String
  )

  data class DexStats(
    val entries: List<DexEntryInfo>,
    val resourcesSize: Long,
    val isComplete: Boolean
  )

  fun collect(applicationInfo: ApplicationInfo): DexStats {
    val sourceDir = applicationInfo.sourceDir ?: return INCOMPLETE_STATS
    return collect(sourceDir, applicationInfo.splitSourceDirs.orEmpty())
  }

  fun collect(packageInfo: PackageInfo): DexStats {
    val sourceDir = packageInfo.applicationInfo?.sourceDir ?: return INCOMPLETE_STATS
    return collect(sourceDir, PackageUtils.getSplitsSourceDir(packageInfo).orEmpty())
  }

  private fun collect(
    sourceDir: String,
    splitSourceDirs: Array<out String>
  ): DexStats {
    val sources = buildList {
      add(DexSource(BASE_SOURCE_NAME, sourceDir))
      splitSourceDirs.forEachIndexed { index, path ->
        val fileName = File(path).nameWithoutExtension
          .ifBlank { (index + 1).toString() }
        add(DexSource("$SPLIT_SOURCE_PREFIX$fileName", path))
      }
    }
    return collect(sources)
  }

  fun collect(sourceDir: String): DexStats {
    return collect(listOf(DexSource(BASE_SOURCE_NAME, sourceDir)))
  }

  internal fun collect(sources: List<DexSource>): DexStats {
    return runCatching {
      require(sources.size <= MAX_SOURCE_COUNT)
      require(sources.distinctBy(DexSource::name).size == sources.size)
      var remainingDexEntries = MAX_DEX_ENTRY_COUNT
      val sourceStats = sources.map { source ->
        readSourceStats(source, remainingDexEntries).also { stats ->
          remainingDexEntries -= stats.dexEntries.size
        }
      }
      val dexEntries = sourceStats.flatMap(SourceStats::dexEntries)
      require(dexEntries.size <= MAX_DEX_ENTRY_COUNT)
      require(dexEntries.sumOf(DexEntry::size) <= MAX_TOTAL_DEX_SIZE)

      var isComplete = true
      val entries = sourceStats.flatMap { stats ->
        val entryNames = stats.dexEntries.map(DexEntry::entryName)
        val classCounts = countClassesPerDex(File(stats.source.path), entryNames).orEmpty()
        if (entryNames.isNotEmpty() && classCounts.isEmpty()) {
          isComplete = false
        }
        stats.dexEntries.map { entry ->
          DexEntryInfo(
            name = "${stats.source.name}/${entry.entryName}",
            size = entry.size,
            classCount = classCounts[entry.entryName] ?: 0
          )
        }
      }.sortedBy(DexEntryInfo::name)

      DexStats(
        entries = entries,
        resourcesSize = sourceStats.sumOf(SourceStats::resourcesSize),
        isComplete = isComplete
      )
    }.getOrDefault(INCOMPLETE_STATS)
  }

  internal fun isValidStoredStats(
    entries: List<DexEntryInfo>,
    resourcesSize: Long
  ): Boolean {
    if (entries.size > MAX_DEX_ENTRY_COUNT) return false
    if (entries.distinctBy(DexEntryInfo::name).size != entries.size) return false
    if (entries.any { entry ->
        !entry.name.matches(STORED_DEX_ENTRY_REGEX) ||
          entry.size !in 0..MAX_DEX_ENTRY_SIZE ||
          entry.classCount < 0
      }
    ) {
      return false
    }
    if (entries.sumOf(DexEntryInfo::size) > MAX_TOTAL_DEX_SIZE) return false
    return resourcesSize in 0..MAX_TOTAL_RESOURCES_SIZE
  }

  private fun readSourceStats(
    source: DexSource,
    remainingDexEntries: Int
  ): SourceStats {
    return ZipFileCompat(File(source.path)).use { zipFile ->
      val dexEntries = zipFile.getZipEntries()
        .asSequence()
        .filter { entry ->
          entry.name.matches(DEX_ENTRY_REGEX)
        }
        .take(remainingDexEntries + 1)
        .map { entry ->
          DexEntry(
            entryName = entry.name,
            size = entry.size
          )
        }
        .toList()
      require(dexEntries.size <= remainingDexEntries)
      require(dexEntries.distinctBy(DexEntry::entryName).size == dexEntries.size)
      require(dexEntries.all { it.size in 0..MAX_DEX_ENTRY_SIZE })

      val resourcesSize = zipFile.getEntry(RESOURCES_ARSC)?.size ?: 0L
      require(resourcesSize in 0..MAX_RESOURCES_SIZE)
      SourceStats(
        source = source,
        dexEntries = dexEntries,
        resourcesSize = resourcesSize
      )
    }
  }

  private fun countClassesPerDex(sourceFile: File, entryNames: List<String>): Map<String, Int>? {
    if (entryNames.isEmpty()) {
      return emptyMap()
    }
    return runCatching {
      val container = ZipDexContainer2(sourceFile, null, MAX_DEX_ENTRY_SIZE)
      entryNames.associateWith { entryName ->
        container.getEntry(entryName)?.dexFile?.classes?.size ?: 0
      }
    }.getOrNull()
  }

  private data class SourceStats(
    val source: DexSource,
    val dexEntries: List<DexEntry>,
    val resourcesSize: Long
  )

  private data class DexEntry(
    val entryName: String,
    val size: Long
  )

  private const val BASE_SOURCE_NAME = "base"
  private const val SPLIT_SOURCE_PREFIX = "split:"
  private const val MAX_SOURCE_COUNT = 64
  private const val MAX_DEX_ENTRY_COUNT = 100
  private const val MAX_DEX_ENTRY_SIZE = 128L * 1024 * 1024
  private const val MAX_TOTAL_DEX_SIZE = 512L * 1024 * 1024
  private const val MAX_RESOURCES_SIZE = 512L * 1024 * 1024
  private const val MAX_TOTAL_RESOURCES_SIZE = MAX_SOURCE_COUNT * MAX_RESOURCES_SIZE
  private val INCOMPLETE_STATS = DexStats(emptyList(), 0, false)
  private val DEX_ENTRY_REGEX = Regex("^classes(\\d*)\\.dex$")
  private val STORED_DEX_ENTRY_REGEX = Regex("^(base|split:[^/\\r\\n]+)/classes(\\d*)\\.dex$")
  private const val RESOURCES_ARSC = "resources.arsc"
}
