package com.shohan.cleanspace.viewmodel

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shohan.cleanspace.data.PermissionHelper
import com.shohan.cleanspace.data.StorageRepository
import com.shohan.cleanspace.data.ThemePreference
import com.shohan.cleanspace.data.models.AppPermissions
import com.shohan.cleanspace.data.models.AppStorageInfo
import com.shohan.cleanspace.data.models.CategorySize
import com.shohan.cleanspace.data.models.DuplicateGroup
import com.shohan.cleanspace.data.models.JunkFile
import com.shohan.cleanspace.data.models.LargeFile
import com.shohan.cleanspace.data.models.MediaAppInfo
import com.shohan.cleanspace.data.models.OrphanedItem
import com.shohan.cleanspace.data.models.StorageOverview
import com.shohan.cleanspace.data.models.ThemeMode
import com.shohan.cleanspace.shizuku.ShizukuHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StorageRepository(application)
    private val themePreference = ThemePreference(application)

    private val _storageOverview = MutableStateFlow(StorageOverview())
    val storageOverview: StateFlow<StorageOverview> = _storageOverview.asStateFlow()

    private val _categoryBreakdown = MutableStateFlow<List<CategorySize>>(emptyList())
    val categoryBreakdown: StateFlow<List<CategorySize>> = _categoryBreakdown.asStateFlow()

    private val _junkFiles = MutableStateFlow<List<JunkFile>>(emptyList())
    val junkFiles: StateFlow<List<JunkFile>> = _junkFiles.asStateFlow()

    private val _largeFiles = MutableStateFlow<List<LargeFile>>(emptyList())
    val largeFiles: StateFlow<List<LargeFile>> = _largeFiles.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppStorageInfo>>(emptyList())
    val installedApps: StateFlow<List<AppStorageInfo>> = _installedApps.asStateFlow()

    private val _duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())
    val duplicateGroups: StateFlow<List<DuplicateGroup>> = _duplicateGroups.asStateFlow()

    private val _mediaApps = MutableStateFlow<List<MediaAppInfo>>(emptyList())
    val mediaApps: StateFlow<List<MediaAppInfo>> = _mediaApps.asStateFlow()

    private val _orphanedItems = MutableStateFlow<List<OrphanedItem>>(emptyList())
    val orphanedItems: StateFlow<List<OrphanedItem>> = _orphanedItems.asStateFlow()

    private val _permissions = MutableStateFlow(AppPermissions())
    val permissions: StateFlow<AppPermissions> = _permissions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _bulkClearProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val bulkClearProgress: StateFlow<Pair<Int, Int>?> = _bulkClearProgress.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        refreshPermissions()
        viewModelScope.launch {
            themePreference.themeMode.collect { _themeMode.value = it }
        }
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        _permissions.value = AppPermissions(
            allFilesAccess = PermissionHelper.hasAllFilesAccess(),
            usageAccess = PermissionHelper.hasUsageAccess(ctx),
            shizukuInstalled = ShizukuHelper.isShizukuInstalled(ctx),
            shizukuRunning = ShizukuHelper.isShizukuRunning(),
            shizukuPermission = ShizukuHelper.hasPermission()
        )
        if (_permissions.value.shizukuPermission) {
            ShizukuHelper.bindService(ctx)
        }
    }

    fun onShizukuPermissionResult(granted: Boolean) {
        refreshPermissions()
        if (granted) {
            ShizukuHelper.bindService(getApplication())
        }
    }

    fun requestShizukuPermission() {
        ShizukuHelper.requestPermission()
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreference.setThemeMode(mode) }
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _storageOverview.value = repository.getStorageOverview()
            _categoryBreakdown.value = repository.getCategoryBreakdown()
            _isLoading.value = false
        }
    }

    fun scanJunk() {
        viewModelScope.launch {
            _isLoading.value = true
            _junkFiles.value = repository.scanJunkFiles()
            _isLoading.value = false
        }
    }

    fun toggleJunkSelection(junkFile: JunkFile) {
        _junkFiles.value = _junkFiles.value.map {
            if (it.path == junkFile.path) it.copy(selected = !it.selected) else it
        }
    }

    fun deleteSelectedJunk() {
        viewModelScope.launch {
            _isLoading.value = true
            val selected = _junkFiles.value.filter { it.selected }
            var freed = 0L
            selected.forEach { if (repository.deleteJunkFile(it)) freed += it.sizeBytes }
            _statusMessage.value = "${selected.size} আইটেম মুছে ${formatBytes(freed)} খালি হয়েছে"
            _junkFiles.value = repository.scanJunkFiles()
            _isLoading.value = false
        }
    }

    fun clearOwnAppCache() {
        viewModelScope.launch {
            repository.clearOwnAppCache()
            _statusMessage.value = "অ্যাপের cache মুছে ফেলা হয়েছে"
            scanJunk()
        }
    }

    fun scanLargeFiles(thresholdMB: Long = 50) {
        viewModelScope.launch {
            _isLoading.value = true
            _largeFiles.value = repository.scanLargeFiles(thresholdMB * 1024 * 1024)
            _isLoading.value = false
        }
    }

    fun deleteLargeFile(file: LargeFile) {
        viewModelScope.launch {
            if (repository.deleteLargeFile(file)) {
                _largeFiles.value = _largeFiles.value.filter { it.path != file.path }
                _statusMessage.value = "${file.name} মুছে ফেলা হয়েছে"
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _installedApps.value = repository.getInstalledApps()
            _isLoading.value = false
        }
    }

    // ---------- Duplicate File Finder ----------

    fun scanDuplicates() {
        viewModelScope.launch {
            _isLoading.value = true
            _duplicateGroups.value = repository.scanDuplicates()
            _isLoading.value = false
        }
    }

    fun toggleDuplicateKeep(groupIndex: Int, filePath: String) {
        val groups = _duplicateGroups.value.toMutableList()
        val group = groups.getOrNull(groupIndex) ?: return
        val updatedFiles = group.files.map {
            if (it.path == filePath) it.copy(keepThisOne = !it.keepThisOne) else it
        }
        groups[groupIndex] = group.copy(files = updatedFiles)
        _duplicateGroups.value = groups
    }

    fun deleteUnselectedDuplicates() {
        viewModelScope.launch {
            _isLoading.value = true
            var freed = 0L
            var deletedCount = 0
            _duplicateGroups.value.forEach { group ->
                group.files.filter { !it.keepThisOne }.forEach { file ->
                    if (repository.deleteFile(file.path)) {
                        freed += file.sizeBytes
                        deletedCount++
                    }
                }
            }
            _statusMessage.value = "$deletedCount টা ডুপ্লিকেট মুছে ${formatBytes(freed)} খালি হয়েছে"
            _duplicateGroups.value = repository.scanDuplicates()
            _isLoading.value = false
        }
    }

    // ---------- Media App Cleaner (WhatsApp / Telegram etc.) ----------

    fun scanMediaApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _mediaApps.value = repository.scanMediaApps()
            _isLoading.value = false
        }
    }

    fun deleteMediaCategory(packageName: String, categoryName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val freed = repository.deleteMediaCategory(packageName, categoryName)
            _statusMessage.value = "${formatBytes(freed)} খালি হয়েছে"
            _mediaApps.value = repository.scanMediaApps()
            _isLoading.value = false
        }
    }

    // ---------- Orphaned Data Finder (Shizuku required) ----------

    fun scanOrphanedData() {
        viewModelScope.launch {
            val service = ShizukuHelper.cacheService
            if (service == null) {
                _statusMessage.value = "এই ফিচারের জন্য Shizuku চালু থাকা লাগবে"
                return@launch
            }
            _isLoading.value = true
            _orphanedItems.value = repository.scanOrphanedData(service)
            _isLoading.value = false
        }
    }

    fun toggleOrphanedSelection(item: OrphanedItem) {
        _orphanedItems.value = _orphanedItems.value.map {
            if (it.path == item.path) it.copy(selected = !it.selected) else it
        }
    }

    fun deleteSelectedOrphaned() {
        viewModelScope.launch {
            val service = ShizukuHelper.cacheService
            if (service == null) {
                _statusMessage.value = "Shizuku connected নেই"
                return@launch
            }
            _isLoading.value = true
            val selected = _orphanedItems.value.filter { it.selected }
            var freed = 0L
            var deletedCount = 0
            selected.forEach { item ->
                if (repository.deleteOrphanedItem(service, item.path)) {
                    freed += item.sizeBytes
                    deletedCount++
                }
            }
            _statusMessage.value = "$deletedCount টা ফোল্ডার মুছে ${formatBytes(freed)} খালি হয়েছে"
            _orphanedItems.value = repository.scanOrphanedData(service)
            _isLoading.value = false
        }
    }

    fun clearAllAppsCacheViaShizuku() {
        viewModelScope.launch {
            val service = ShizukuHelper.cacheService
            if (service == null) {
                _statusMessage.value = "Shizuku service connected নেই। Shizuku চালু আছে কিনা চেক করো।"
                return@launch
            }
            if (_installedApps.value.isEmpty()) {
                _installedApps.value = repository.getInstalledApps()
            }
            val targets = _installedApps.value.filter { !it.isSystemApp }
            var success = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                targets.forEachIndexed { index, app ->
                    try {
                        service.runCommand("pm clear --cache-only ${app.packageName}")
                        success++
                    } catch (e: Exception) {
                        // skip this app, continue with rest
                    }
                    _bulkClearProgress.value = Pair(index + 1, targets.size)
                }
            } else {
                try {
                    service.runCommand("pm trim-caches 999999999999")
                    success = targets.size
                } catch (e: Exception) {
                    // ignore
                }
                _bulkClearProgress.value = Pair(targets.size, targets.size)
            }
            _statusMessage.value = "$success টি App-এর cache clean করা হয়েছে"
            _bulkClearProgress.value = null
            loadApps()
        }
    }

    fun clearSingleAppCacheViaShizuku(app: AppStorageInfo) {
        viewModelScope.launch {
            val service = ShizukuHelper.cacheService
            if (service == null) {
                _statusMessage.value = "Shizuku চালু নেই"
                return@launch
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    service.runCommand("pm clear --cache-only ${app.packageName}")
                } else {
                    service.runCommand("pm trim-caches 999999999999")
                }
                _statusMessage.value = "${app.appName}-এর cache clean হয়েছে"
            } catch (e: Exception) {
                _statusMessage.value = "ব্যর্থ হয়েছে: ${e.message}"
            }
            loadApps()
        }
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var value = bytes.toDouble()
            var unitIndex = 0
            while (value >= 1024 && unitIndex < units.size - 1) {
                value /= 1024
                unitIndex++
            }
            return String.format("%.1f %s", value, units[unitIndex])
        }
    }
}
