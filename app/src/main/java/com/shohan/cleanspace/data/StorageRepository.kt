package com.shohan.cleanspace.data

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.shohan.cleanspace.data.models.AppStorageInfo
import com.shohan.cleanspace.data.models.CategorySize
import com.shohan.cleanspace.data.models.DuplicateFile
import com.shohan.cleanspace.data.models.DuplicateGroup
import com.shohan.cleanspace.data.models.JunkFile
import com.shohan.cleanspace.data.models.LargeFile
import com.shohan.cleanspace.data.models.MediaAppInfo
import com.shohan.cleanspace.data.models.MediaCategory
import com.shohan.cleanspace.data.models.OrphanedItem
import com.shohan.cleanspace.data.models.StorageOverview
import com.shohan.cleanspace.shizuku.ICacheService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

class StorageRepository(private val context: Context) {

    private val junkExtensions = setOf("tmp", "log", "bak", "old", "temp", "cache")
    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic")
    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "3gp", "webm", "flv")
    private val audioExt = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac")
    private val docExt = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")

    fun getStorageOverview(): StorageOverview {
        val stat = StatFs(Environment.getExternalStorageDirectory().path)
        val total = stat.totalBytes
        val free = stat.availableBytes
        return StorageOverview(totalBytes = total, usedBytes = total - free, freeBytes = free)
    }

    suspend fun getCategoryBreakdown(): List<CategorySize> = withContext(Dispatchers.IO) {
        var images = 0L
        var videos = 0L
        var audio = 0L
        var docs = 0L
        var others = 0L

        val root = Environment.getExternalStorageDirectory()
        if (root.exists() && root.canRead()) {
            root.walkTopDown()
                .onEnter { it.name != "Android" }
                .forEach { file ->
                    if (file.isFile) {
                        val ext = file.extension.lowercase()
                        val size = file.length()
                        when (ext) {
                            in imageExt -> images += size
                            in videoExt -> videos += size
                            in audioExt -> audio += size
                            in docExt -> docs += size
                            else -> others += size
                        }
                    }
                }
        }

        val appsBytes = getTotalAppsSize()

        listOf(
            CategorySize("Apps", appsBytes),
            CategorySize("Images", images),
            CategorySize("Videos", videos),
            CategorySize("Audio", audio),
            CategorySize("Documents", docs),
            CategorySize("Others", others)
        ).filter { it.bytes > 0 }
    }

    private fun getTotalAppsSize(): Long {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0L
        if (!PermissionHelper.hasUsageAccess(context)) return 0L
        return try {
            val storageStatsManager =
                context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val uuid = storageManager.getUuidForPath(context.dataDir)
            var total = 0L
            val apps = context.packageManager.getInstalledApplications(0)
            for (app in apps) {
                try {
                    val stats = storageStatsManager.queryStatsForUid(uuid, app.uid)
                    total += stats.appBytes + stats.cacheBytes + stats.dataBytes
                } catch (e: Exception) {
                    // skip apps we cannot query
                }
            }
            total
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun scanJunkFiles(): List<JunkFile> = withContext(Dispatchers.IO) {
        val results = mutableListOf<JunkFile>()
        val root = Environment.getExternalStorageDirectory()
        if (root.exists() && root.canRead()) {
            root.walkTopDown()
                .onEnter { it.name != "Android" }
                .forEach { file ->
                    if (file.isFile) {
                        val ext = file.extension.lowercase()
                        if (ext in junkExtensions) {
                            results.add(
                                JunkFile(
                                    path = file.absolutePath,
                                    name = file.name,
                                    sizeBytes = file.length(),
                                    reason = "Temporary file (.$ext)"
                                )
                            )
                        }
                    } else if (file.isDirectory && file != root) {
                        val children = file.listFiles()
                        if (children != null && children.isEmpty()) {
                            results.add(
                                JunkFile(
                                    path = file.absolutePath,
                                    name = file.name,
                                    sizeBytes = 0L,
                                    reason = "Empty folder"
                                )
                            )
                        }
                    }
                }
        }

        val ownCache = context.cacheDir
        if (ownCache.exists()) {
            val size = ownCache.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            if (size > 0) {
                results.add(
                    0,
                    JunkFile(
                        path = ownCache.absolutePath,
                        name = "CleanSpace App Cache",
                        sizeBytes = size,
                        reason = "This app's own cache"
                    )
                )
            }
        }
        results
    }

    fun deleteJunkFile(junkFile: JunkFile): Boolean {
        return try {
            val file = File(junkFile.path)
            if (file.isDirectory) file.deleteRecursively() else file.delete()
        } catch (e: Exception) {
            false
        }
    }

    fun clearOwnAppCache(): Boolean {
        return try {
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun scanLargeFiles(thresholdBytes: Long = 50L * 1024 * 1024): List<LargeFile> =
        withContext(Dispatchers.IO) {
            val results = mutableListOf<LargeFile>()
            val root = Environment.getExternalStorageDirectory()
            if (root.exists() && root.canRead()) {
                root.walkTopDown()
                    .onEnter { it.name != "Android" }
                    .forEach { file ->
                        if (file.isFile && file.length() >= thresholdBytes) {
                            results.add(
                                LargeFile(
                                    path = file.absolutePath,
                                    name = file.name,
                                    sizeBytes = file.length(),
                                    extension = file.extension
                                )
                            )
                        }
                    }
            }
            results.sortedByDescending { it.sizeBytes }.take(200)
        }

    fun deleteLargeFile(largeFile: LargeFile): Boolean {
        return try {
            File(largeFile.path).delete()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getInstalledApps(): List<AppStorageInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val results = mutableListOf<AppStorageInfo>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && PermissionHelper.hasUsageAccess(context)) {
            try {
                val storageStatsManager =
                    context.getSystemService(Context.STORAGE_STATS_SERVICE) as StorageStatsManager
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                val uuid = storageManager.getUuidForPath(context.dataDir)
                val apps = pm.getInstalledApplications(0)
                for (app in apps) {
                    try {
                        val stats = storageStatsManager.queryStatsForUid(uuid, app.uid)
                        val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        results.add(
                            AppStorageInfo(
                                packageName = app.packageName,
                                appName = pm.getApplicationLabel(app).toString(),
                                appBytes = stats.appBytes,
                                cacheBytes = stats.cacheBytes,
                                dataBytes = stats.dataBytes,
                                isSystemApp = isSystem
                            )
                        )
                    } catch (e: Exception) {
                        // skip
                    }
                }
            } catch (e: Exception) {
                // ignore
            }
        }
        results.sortedByDescending { it.totalBytes }
    }

    fun deleteFile(path: String): Boolean {
        return try {
            File(path).delete()
        } catch (e: Exception) {
            false
        }
    }

    // ---------- Duplicate File Finder ----------
    // Strategy: group files by size first (cheap), only hash files within a
    // size-matched group (expensive). This avoids hashing every file on the device.

    private val duplicateScanExtensions =
        imageExt + videoExt + audioExt + docExt

    suspend fun scanDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val root = Environment.getExternalStorageDirectory()
        val bySize = HashMap<Long, MutableList<File>>()

        if (root.exists() && root.canRead()) {
            root.walkTopDown()
                .onEnter { it.name != "Android" }
                .forEach { file ->
                    if (file.isFile && file.length() > 10_240L) { // skip files under 10KB
                        val ext = file.extension.lowercase()
                        if (ext in duplicateScanExtensions) {
                            bySize.getOrPut(file.length()) { mutableListOf() }.add(file)
                        }
                    }
                }
        }

        val results = mutableListOf<DuplicateGroup>()
        for ((size, files) in bySize) {
            if (files.size < 2) continue
            val byHash = HashMap<String, MutableList<File>>()
            for (file in files) {
                val hash = try { md5Of(file) } catch (e: Exception) { null } ?: continue
                byHash.getOrPut(hash) { mutableListOf() }.add(file)
            }
            for (group in byHash.values) {
                if (group.size < 2) continue
                val duplicateFiles = group.mapIndexed { index, file ->
                    DuplicateFile(
                        path = file.absolutePath,
                        name = file.name,
                        sizeBytes = size,
                        keepThisOne = index == 0
                    )
                }
                results.add(DuplicateGroup(files = duplicateFiles, sizeBytes = size))
            }
        }
        results.sortedByDescending { it.wastedBytes }
    }

    private fun md5Of(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // ---------- Media App Cleaner (WhatsApp / Telegram / Messenger etc.) ----------
    // Android/media/<package>/ is NOT subject to the same cross-app block as
    // Android/data/<package>/, so it stays reachable with All Files Access.

    private val knownMediaApps = listOf(
        "com.whatsapp" to "WhatsApp",
        "com.whatsapp.w4b" to "WhatsApp Business",
        "org.telegram.messenger" to "Telegram",
        "com.facebook.orca" to "Messenger",
        "com.facebook.katana" to "Facebook"
    )

    suspend fun scanMediaApps(): List<MediaAppInfo> = withContext(Dispatchers.IO) {
        val mediaRoot = File(Environment.getExternalStorageDirectory(), "Android/media")
        val results = mutableListOf<MediaAppInfo>()

        for ((packageName, displayName) in knownMediaApps) {
            val appFolder = File(mediaRoot, packageName)
            if (!appFolder.exists() || !appFolder.canRead()) continue

            var images = 0L; var imageCount = 0
            var videos = 0L; var videoCount = 0
            var audio = 0L; var audioCount = 0
            var docs = 0L; var docCount = 0
            var status = 0L; var statusCount = 0
            var others = 0L; var otherCount = 0

            appFolder.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val ext = file.extension.lowercase()
                    val isStatus = file.absolutePath.contains("/.Statuses/") ||
                        file.absolutePath.contains("/Statuses/")
                    val size = file.length()
                    when {
                        isStatus -> { status += size; statusCount++ }
                        ext in imageExt -> { images += size; imageCount++ }
                        ext in videoExt -> { videos += size; videoCount++ }
                        ext in audioExt -> { audio += size; audioCount++ }
                        ext in docExt -> { docs += size; docCount++ }
                        else -> { others += size; otherCount++ }
                    }
                }
            }

            val categories = listOfNotNull(
                if (status > 0) MediaCategory("Status (২৪ ঘণ্টার)", status, statusCount) else null,
                if (images > 0) MediaCategory("Images", images, imageCount) else null,
                if (videos > 0) MediaCategory("Videos", videos, videoCount) else null,
                if (audio > 0) MediaCategory("Audio/Voice Notes", audio, audioCount) else null,
                if (docs > 0) MediaCategory("Documents", docs, docCount) else null,
                if (others > 0) MediaCategory("Others", others, otherCount) else null
            )

            val total = images + videos + audio + docs + status + others
            if (total > 0) {
                results.add(MediaAppInfo(packageName, displayName, total, categories))
            }
        }
        results.sortedByDescending { it.totalBytes }
    }

    suspend fun deleteMediaCategory(packageName: String, categoryName: String): Long =
        withContext(Dispatchers.IO) {
            val mediaRoot = File(Environment.getExternalStorageDirectory(), "Android/media/$packageName")
            if (!mediaRoot.exists()) return@withContext 0L
            var freed = 0L
            mediaRoot.walkTopDown().forEach { file ->
                if (file.isFile) {
                    val ext = file.extension.lowercase()
                    val isStatus = file.absolutePath.contains("/.Statuses/") ||
                        file.absolutePath.contains("/Statuses/")
                    val matches = when (categoryName) {
                        "Status (২৪ ঘণ্টার)" -> isStatus
                        "Images" -> !isStatus && ext in imageExt
                        "Videos" -> !isStatus && ext in videoExt
                        "Audio/Voice Notes" -> !isStatus && ext in audioExt
                        "Documents" -> !isStatus && ext in docExt
                        else -> !isStatus && ext !in imageExt && ext !in videoExt &&
                            ext !in audioExt && ext !in docExt
                    }
                    if (matches) {
                        val size = file.length()
                        if (file.delete()) freed += size
                    }
                }
            }
            freed
        }

    // ---------- Orphaned Data Finder (requires Shizuku) ----------
    // Android/data/<package>/ is blocked from cross-app File access on
    // Android 11+, even with MANAGE_EXTERNAL_STORAGE. Only a shell-level
    // process (which Shizuku provides) can read it without root.

    suspend fun scanOrphanedData(service: ICacheService): List<OrphanedItem> =
        withContext(Dispatchers.IO) {
            val installedPackages = context.packageManager.getInstalledApplications(0)
                .map { it.packageName }
                .toSet()

            val results = mutableListOf<OrphanedItem>()
            for (location in listOf("Android/data", "Android/obb")) {
                val listOutput = try {
                    service.runCommand("ls /storage/emulated/0/$location")
                } catch (e: Exception) {
                    ""
                }
                val folderNames = listOutput.lines().map { it.trim() }.filter { it.isNotEmpty() }
                for (folder in folderNames) {
                    if (folder !in installedPackages && folder.contains(".")) {
                        val sizeOutput = try {
                            service.runCommand("du -sb /storage/emulated/0/$location/$folder")
                        } catch (e: Exception) {
                            ""
                        }
                        val sizeBytes = sizeOutput.trim().split(Regex("\\s+")).firstOrNull()
                            ?.toLongOrNull() ?: 0L
                        results.add(
                            OrphanedItem(
                                path = "/storage/emulated/0/$location/$folder",
                                packageName = folder,
                                sizeBytes = sizeBytes,
                                location = location
                            )
                        )
                    }
                }
            }
            results.sortedByDescending { it.sizeBytes }
        }

    suspend fun deleteOrphanedItem(service: ICacheService, path: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val result = service.runCommand("rm -rf '$path' && echo OK")
                result.contains("OK")
            } catch (e: Exception) {
                false
            }
        }
}
