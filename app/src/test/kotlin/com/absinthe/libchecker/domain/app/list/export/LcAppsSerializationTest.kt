package com.absinthe.libchecker.domain.app.list.export

import com.absinthe.libchecker.utils.JsonUtil
import com.squareup.moshi.JsonWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import okio.Buffer
import okio.buffer
import okio.sink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LcAppsSerializationTest {
  @Test
  fun preservesLegacyJsonIncludingNullsAndNestedFields() {
    val adapter = JsonUtil.moshi.adapter(LcAppsExporter.ExportReport::class.java)
    val json = JsonUtil.moshi.adapter(Any::class.java)
    for (fixture in listOf("full", "nullable")) {
      val input = javaClass.getResource("/lcapps/$fixture.json")!!.readText()
      val report = requireNotNull(adapter.fromJson(input))
      val expected = input
      val output = Buffer()
      val writer = JsonWriter.of(output)
      writer.beginArray()
      repeat(2) { LcAppsExporter.writeReport(writer, report) }
      writer.endArray()
      writer.flush()
      val actual = output.readUtf8()
      assertEquals(listOf(json.fromJson(expected), json.fromJson(expected)), json.fromJson(actual))
      assertFalse(actual.contains("\"all\":"))
      assertTrue(actual.contains("9007199254740993"))
    }
  }

  @Test
  fun streamsReportsWithoutClosingTheZipBeforeFollowingEntries() {
    val input = javaClass.getResource("/lcapps/nullable.json")!!.readText()
    val report = requireNotNull(JsonUtil.moshi.adapter(LcAppsExporter.ExportReport::class.java).fromJson(input))
    val bytes = ByteArrayOutputStream()
    ZipOutputStream(bytes).use { zip ->
      zip.putNextEntry(ZipEntry("apps.json"))
      val sink = zip.sink().buffer()
      val writer = JsonWriter.of(sink)
      writer.beginArray()
      repeat(16) { LcAppsExporter.writeReport(writer, report) }
      writer.endArray()
      writer.flush()
      sink.flush()
      zip.closeEntry()
      zip.putNextEntry(ZipEntry("icons/example.png"))
      zip.write(byteArrayOf(1, 2, 3))
      zip.closeEntry()
    }
    ZipInputStream(ByteArrayInputStream(bytes.toByteArray())).use { zip ->
      assertEquals("apps.json", zip.nextEntry.name)
      val reports = JsonUtil.moshi.adapter(Any::class.java).fromJson(zip.readBytes().decodeToString()) as List<*>
      assertEquals(16, reports.size)
      assertEquals("icons/example.png", zip.nextEntry.name)
      org.junit.Assert.assertArrayEquals(byteArrayOf(1, 2, 3), zip.readBytes())
    }
  }

  @Test
  fun iconConstructionKeepsLegacyConstants() {
    val adapter = JsonUtil.moshi.adapter(LcAppsExporter.IconEntry::class.java)
    val json = adapter.toJson(LcAppsExporter.IconEntry("icons/app.png", 123))
    assertTrue(json.contains("\"mimeType\":\"image/png\""))
    assertTrue(json.contains("\"dataUri\":\"\""))
    assertFalse(json.contains("resourceId"))
  }
}
