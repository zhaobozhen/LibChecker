package com.absinthe.libchecker.data.snapshot

import com.absinthe.libchecker.database.entity.SnapshotItem
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtoSnapshotArchiveCodecTest {

  @Test
  fun `round trip preserves archived state`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()

    codec.write(snapshotItem(), output)
    val restored = codec.read(ByteArrayInputStream(output.toByteArray()))

    assertTrue(restored?.isArchived == true)
    assertEquals("", restored?.versionName)
    assertEquals(
      "[{\"name\":\"base/classes.dex\",\"size\":10,\"classCount\":1}]",
      restored?.dexInfo
    )
    assertEquals(42L, restored?.resourcesSize)
    assertEquals(SnapshotItem.CURRENT_STATS_VERSION, restored?.statsVersion)
  }

  @Test
  fun `invalid stored stats are downgraded on restore`() {
    val codec = ProtoSnapshotArchiveCodec()
    val output = ByteArrayOutputStream()
    val invalid = snapshotItem().copy(
      dexInfo = "[{\"name\":\"../classes.dex\",\"size\":10,\"classCount\":1}]",
      resourcesSize = Long.MAX_VALUE
    )

    codec.write(invalid, output)
    val restored = codec.read(ByteArrayInputStream(output.toByteArray()))

    assertFalse(restored == null)
    assertEquals("[]", restored?.dexInfo)
    assertEquals(0L, restored?.resourcesSize)
    assertEquals(0, restored?.statsVersion)
  }

  private fun snapshotItem(): SnapshotItem {
    return SnapshotItem(
      id = null,
      packageName = "com.example",
      timeStamp = 1L,
      label = "Example",
      versionName = "",
      versionCode = 3022L,
      isArchived = true,
      installedTime = 2L,
      lastUpdatedTime = 3L,
      isSystem = false,
      abi = 3,
      targetApi = 35,
      nativeLibs = "[]",
      services = "[]",
      activities = "[]",
      receivers = "[]",
      providers = "[]",
      permissions = "[]",
      metadata = "[]",
      packageSize = 0L,
      compileSdk = 35,
      minSdk = 24,
      dexInfo = "[{\"name\":\"base/classes.dex\",\"size\":10,\"classCount\":1}]",
      resourcesSize = 42L,
      statsVersion = SnapshotItem.CURRENT_STATS_VERSION
    )
  }
}
