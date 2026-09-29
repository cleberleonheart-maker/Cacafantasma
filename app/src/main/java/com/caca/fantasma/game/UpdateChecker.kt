package com.caca.fantasma.game

import com.caca.fantasma.BuildConfig
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val tag: String,
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
    val apkName: String
) {
    fun isNewerThan(current: String): Boolean = UpdateChecker.compareVersions(versionName, current) > 0
}

sealed class UpdateResult {
    data class Available(val update: UpdateInfo) : UpdateResult()
    object UpToDate : UpdateResult()
    data class Failed(val reason: String) : UpdateResult()
}

sealed class DownloadResult {
    data class Done(val file: File) : DownloadResult()
    data class Failed(val reason: String) : DownloadResult()
}

object UpdateChecker {

    const val REPO_OWNER = "cleberleonheart-maker"
    const val REPO_NAME = "Cacafantasma"
    private const val API_LATEST = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"
    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 20000

    fun check(currentVersion: String): UpdateResult {
        val json = try {
            request(API_LATEST)
        } catch (e: Exception) {
            return UpdateResult.Failed(e.message ?: "sem conexão com a internet")
        }

        return try {
            val release = JSONObject(json)
            val tag = release.optString("tag_name").ifBlank { release.optString("name") }
            if (tag.isBlank()) return UpdateResult.Failed("a release não tem nome nem tag")

            val versionName = normalizeVersion(tag)
            val apk = release.optJSONArray("assets")?.let { assets ->
                (0 until assets.length()).map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            } ?: return UpdateResult.Failed("a release $tag não tem APK anexado")

            val update = UpdateInfo(
                tag = tag,
                versionName = versionName,
                notes = release.optString("body").trim(),
                apkUrl = apk.optString("browser_download_url"),
                apkSize = apk.optLong("size", 0L),
                apkName = apk.optString("name").ifBlank { "caca-fantasma.apk" }
            )
            if (update.apkUrl.isBlank()) return UpdateResult.Failed("link de download inválido")

            if (update.isNewerThan(currentVersion)) UpdateResult.Available(update)
            else UpdateResult.UpToDate
        } catch (e: Exception) {
            UpdateResult.Failed("resposta inesperada do GitHub")
        }
    }

    fun download(update: UpdateInfo, targetDir: File, onProgress: (Int) -> Unit): DownloadResult {
        val target = File(targetDir, update.apkName)
        return try {
            if (!targetDir.exists()) targetDir.mkdirs()
            val partial = File(targetDir, update.apkName + ".part")
            if (partial.exists()) partial.delete()

            val conn = (URL(update.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT
                instanceFollowRedirects = true
            }
            conn.connect()

            val code = conn.responseCode
            if (code != 200) return DownloadResult.Failed("servidor respondeu $code")

            val total = conn.contentLengthLong
            var downloaded = 0L
            BufferedInputStream(conn.inputStream).use { input ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var read = input.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                        read = input.read(buffer)
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            if (total > 0 && downloaded != total) {
                partial.delete()
                return DownloadResult.Failed("download incompleto")
            }
            if (partial.exists() && !partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            onProgress(100)
            DownloadResult.Done(target)
        } catch (e: Exception) {
            target.delete()
            DownloadResult.Failed(e.message ?: "falha no download")
        }
    }

    private fun request(url: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "CacaFantasma-Android/${BuildConfig.VERSION_NAME}")
        }
        conn.connect()
        try {
            val code = conn.responseCode
            if (code == 404) throw IllegalStateException("nenhuma release publicada ainda")
            if (code != 200) throw IllegalStateException("GitHub respondeu $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun normalizeVersion(raw: String): String = raw.trim().removePrefix("v").removePrefix("V")

    fun compareVersions(a: String, b: String): Int {
        val pa = normalizeVersion(a).split(".", "-")
        val pb = normalizeVersion(b).split(".", "-")
        val size = maxOf(pa.size, pb.size)
        for (i in 0 until size) {
            val na = pa.getOrNull(i)?.trim()?.toIntOrNull() ?: 0
            val nb = pb.getOrNull(i)?.trim()?.toIntOrNull() ?: 0
            if (na != nb) return na.compareTo(nb)
        }
        return 0
    }
}
