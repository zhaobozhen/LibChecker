package com.absinthe.libchecker.core.di

import com.absinthe.libchecker.data.appscan.LCIndexedAppRepository
import com.absinthe.libchecker.data.appscan.LCRulesRuleProvider
import com.absinthe.libchecker.data.appscan.LocalInstalledPackageSource
import com.absinthe.libchecker.data.appscan.PackageInfoAppItemFactory
import com.absinthe.libchecker.data.appscan.PackageUtilsAbiLabelFormatter
import com.absinthe.libchecker.data.appscan.PackageUtilsReferenceReader
import com.absinthe.libchecker.data.rules.LCCloudRulesRepository
import com.absinthe.libchecker.data.snapshot.LCSnapshotArchiveRepository
import com.absinthe.libchecker.data.snapshot.LCSnapshotCompareRepository
import com.absinthe.libchecker.data.snapshot.SnapshotDetailComposer
import com.absinthe.libchecker.data.snapshot.SnapshotDiffEngine
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.domain.appscan.usecase.ComputeLibReferenceNodesUseCase
import com.absinthe.libchecker.domain.appscan.usecase.ExportIndexedAppsReportUseCase
import com.absinthe.libchecker.domain.appscan.usecase.InitializeIndexedAppsUseCase
import com.absinthe.libchecker.domain.appscan.usecase.MatchLibReferencesUseCase
import com.absinthe.libchecker.domain.appscan.usecase.ObserveIndexedAppsUseCase
import com.absinthe.libchecker.domain.appscan.usecase.SyncIndexedAppsUseCase
import com.absinthe.libchecker.domain.rules.usecase.GetCloudRulesVersionStateUseCase
import com.absinthe.libchecker.domain.rules.usecase.UpdateCloudRulesBundleUseCase
import com.absinthe.libchecker.domain.snapshot.usecase.BackupSnapshotsUseCase
import com.absinthe.libchecker.domain.snapshot.usecase.BuildSnapshotDetailItemsUseCase
import com.absinthe.libchecker.domain.snapshot.usecase.CompareSnapshotDiffUseCase
import com.absinthe.libchecker.domain.snapshot.usecase.RestoreSnapshotsUseCase

object AppGraph {
  private val indexedAppRepository by lazy {
    LCIndexedAppRepository(Repositories.lcRepository)
  }

  private val installedPackageSource by lazy {
    LocalInstalledPackageSource()
  }

  private val appItemFactory by lazy {
    PackageInfoAppItemFactory()
  }

  private val abiLabelFormatter by lazy {
    PackageUtilsAbiLabelFormatter()
  }

  private val packageReferenceReader by lazy {
    PackageUtilsReferenceReader()
  }

  private val ruleProvider by lazy {
    LCRulesRuleProvider()
  }

  private val cloudRulesRepository by lazy {
    LCCloudRulesRepository()
  }

  private val snapshotCompareRepository by lazy {
    LCSnapshotCompareRepository(Repositories.lcRepository)
  }

  private val snapshotArchiveRepository by lazy {
    LCSnapshotArchiveRepository(Repositories.lcRepository)
  }

  val snapshotDiffEngine by lazy {
    SnapshotDiffEngine()
  }

  private val snapshotDetailComposer by lazy {
    SnapshotDetailComposer()
  }

  val observeIndexedAppsUseCase by lazy {
    ObserveIndexedAppsUseCase(indexedAppRepository)
  }

  val initializeIndexedAppsUseCase by lazy {
    InitializeIndexedAppsUseCase(
      installedPackageSource = installedPackageSource,
      indexedAppRepository = indexedAppRepository,
      appItemFactory = appItemFactory
    )
  }

  val syncIndexedAppsUseCase by lazy {
    SyncIndexedAppsUseCase(
      installedPackageSource = installedPackageSource,
      indexedAppRepository = indexedAppRepository,
      appItemFactory = appItemFactory
    )
  }

  val exportIndexedAppsReportUseCase by lazy {
    ExportIndexedAppsReportUseCase(
      indexedAppRepository = indexedAppRepository,
      abiLabelFormatter = abiLabelFormatter
    )
  }

  val computeLibReferenceNodesUseCase by lazy {
    ComputeLibReferenceNodesUseCase(
      installedPackageSource = installedPackageSource,
      packageReferenceReader = packageReferenceReader
    )
  }

  val matchLibReferencesUseCase by lazy {
    MatchLibReferencesUseCase(ruleProvider = ruleProvider)
  }

  val getCloudRulesVersionStateUseCase by lazy {
    GetCloudRulesVersionStateUseCase(cloudRulesRepository = cloudRulesRepository)
  }

  val updateCloudRulesBundleUseCase by lazy {
    UpdateCloudRulesBundleUseCase(cloudRulesRepository = cloudRulesRepository)
  }

  val compareSnapshotDiffUseCase by lazy {
    CompareSnapshotDiffUseCase(
      snapshotCompareRepository = snapshotCompareRepository,
      installedPackageSource = installedPackageSource,
      snapshotDiffEngine = snapshotDiffEngine
    )
  }

  val backupSnapshotsUseCase by lazy {
    BackupSnapshotsUseCase(snapshotArchiveRepository = snapshotArchiveRepository)
  }

  val restoreSnapshotsUseCase by lazy {
    RestoreSnapshotsUseCase(snapshotArchiveRepository = snapshotArchiveRepository)
  }

  val buildSnapshotDetailItemsUseCase by lazy {
    BuildSnapshotDetailItemsUseCase(snapshotDetailComposer = snapshotDetailComposer)
  }
}
