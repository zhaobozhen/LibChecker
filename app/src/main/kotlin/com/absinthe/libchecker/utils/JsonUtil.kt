package com.absinthe.libchecker.utils

import kotlinx.serialization.KSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language

object JsonUtil {
  @PublishedApi
  internal val json = Json { ignoreUnknownKeys = true } // Customize JSON configuration if needed

  inline fun <reified T> fromJson(@Language("JSON") string: String): T? = try {
    json.decodeFromString<T>(string)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }

  inline fun <reified T> fromJson(
    @Language("JSON") string: String,
    serializer: KSerializer<T>
  ): T? = try {
    json.decodeFromString(serializer, string)
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }

  @Language("JSON")
  inline fun <reified T> toJson(value: T?): String? = try {
    value?.let { json.encodeToString(it) }
  } catch (e: Exception) {
    e.printStackTrace()
    null
  }
}

inline fun <reified T> String.fromJson(): T? = JsonUtil.fromJson(this)

inline fun <reified T> String.fromJson(serializer: KSerializer<T>): T? = JsonUtil.fromJson(this, serializer)

@Language("JSON")
fun Any?.toJson(): String? = JsonUtil.toJson(this)

