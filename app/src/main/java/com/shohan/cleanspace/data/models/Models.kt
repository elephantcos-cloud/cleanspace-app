package com.shohan.cleanspace.data.models

data class StorageOverview(
    val totalBytes: Long = 0,
    val usedBytes: Long = 0,
    val freeBytes: Long = 0
)

data class CategorySize(
    val name: String,
    val bytes: Long
)

data class JunkFile(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val reason: String,
    val selected: Boolean = true
)

data class LargeFile(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val extension: String
)

data class AppStorageInfo(
    val packageName: String,
    val appName: String,
    val appBytes: Long,
    val cacheBytes: Long,
    val dataBytes: Long,
    val isSystemApp: Boolean
) {
    val totalBytes: Long get() = appBytes + cacheBytes + dataBytes
}

enum class ThemeMode { LIGHT, DARK, SYSTEM }

data class DuplicateFile(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val keepThisOne: Boolean
)

data class DuplicateGroup(
    val files: List<DuplicateFile>,
    val sizeBytes: Long
) {
    val wastedBytes: Long get() = sizeBytes * (files.size - 1)
}

data class OrphanedItem(
    val path: String,
    val packageName: String,
    val sizeBytes: Long,
    val location: String,
    val selected: Boolean = true
)

data class MediaCategory(
    val name: String,
    val bytes: Long,
    val fileCount: Int
)

data class MediaAppInfo(
    val packageName: String,
    val displayName: String,
    val totalBytes: Long,
    val categories: List<MediaCategory>
)

data class AppPermissions(
    val allFilesAccess: Boolean = false,
    val usageAccess: Boolean = false,
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuPermission: Boolean = false
)
