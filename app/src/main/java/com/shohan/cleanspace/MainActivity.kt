package com.shohan.cleanspace

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.shohan.cleanspace.shizuku.ShizukuHelper
import com.shohan.cleanspace.ui.CleanSpaceRoot
import com.shohan.cleanspace.ui.theme.CleanSpaceTheme
import com.shohan.cleanspace.viewmodel.MainViewModel
import rikka.shizuku.Shizuku

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == ShizukuHelper.REQUEST_CODE) {
                viewModel.onShizukuPermissionResult(grantResult == PackageManager.PERMISSION_GRANTED)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            // Shizuku not reachable, app still works without it
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            CleanSpaceTheme(themeMode = themeMode) {
                CleanSpaceRoot(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    override fun onDestroy() {
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (e: Throwable) {
            // ignore
        }
        ShizukuHelper.unbindService(this)
        super.onDestroy()
    }
}
