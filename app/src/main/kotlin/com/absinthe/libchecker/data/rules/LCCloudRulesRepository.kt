package com.absinthe.libchecker.data.rules

import android.content.Context
import com.absinthe.libchecker.api.ApiManager
import com.absinthe.libchecker.api.request.CloudRuleBundleRequest
import com.absinthe.libchecker.constant.Constants
import com.absinthe.libchecker.database.Repositories
import com.absinthe.libchecker.domain.rules.CloudRulesRepository
import com.absinthe.libchecker.utils.DownloadUtils
import com.absinthe.libchecker.utils.extensions.md5
import com.absinthe.rulesbundle.LCRules
import com.absinthe.rulesbundle.Repositories as RulesBundleRepo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import rikka.core.os.FileUtils
import kotlin.coroutines.resume

class LCCloudRulesRepository(
  private val request: CloudRuleBundleRequest = ApiManager.create()
) : CloudRulesRepository {
  override suspend fun getLocalVersion(context: Context): Int = withContext(Dispatchers.IO) {
    RulesBundleRepo.getLocalRulesVersion(context.applicationContext)
  }

  override suspend fun getRemoteVersion(): Int? = withContext(Dispatchers.IO) {
    request.requestCloudRuleInfo()?.version
  }

  override suspend fun updateRulesBundle(
    context: Context,
    targetVersion: Int
  ): Boolean = withContext(Dispatchers.IO) {
    val appContext = context.applicationContext
    val saveFile = File(appContext.cacheDir, Constants.RULES_DB_FILE_NAME)
    if (!downloadRulesBundle(saveFile)) {
      return@withContext false
    }

    LCRules.closeDb()
    Repositories.deleteRulesDatabase()

    val databaseDir = requireNotNull(appContext.getDatabasePath(Constants.RULES_DATABASE_NAME).parentFile)
    if (!databaseDir.exists()) {
      databaseDir.mkdirs()
    }

    val databaseFile = File(databaseDir, Constants.RULES_DATABASE_NAME)
    FileUtils.copy(saveFile, databaseFile)
    if (databaseFile.md5() != saveFile.md5()) {
      return@withContext false
    }

    RulesBundleRepo.setLocalRulesVersion(appContext, targetVersion)
    true
  }

  private suspend fun downloadRulesBundle(saveFile: File): Boolean =
    suspendCancellableCoroutine { continuation ->
      DownloadUtils.download(
        ApiManager.rulesBundleUrl,
        saveFile,
        object : DownloadUtils.OnDownloadListener {
          override fun onDownloadSuccess() {
            if (continuation.isActive) {
              continuation.resume(true)
            }
          }

          override fun onDownloading(progress: Int) {
          }

          override fun onDownloadFailed() {
            if (continuation.isActive) {
              continuation.resume(false)
            }
          }
        }
      )
    }
}
