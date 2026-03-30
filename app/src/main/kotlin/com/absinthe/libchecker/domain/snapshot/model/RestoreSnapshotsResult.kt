package com.absinthe.libchecker.domain.snapshot.model

data class RestoreSnapshotsResult(
  val restoredCountsByTimestamp: Map<Long, Int>,
  val latestTimestamp: Long?
)
