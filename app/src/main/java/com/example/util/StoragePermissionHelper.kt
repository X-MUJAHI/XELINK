package com.example.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Utility helper to accurately verify and request external storage permissions
 * across all Android API versions (Android 6.0 Marshmallow through Android 15/16).
 */
object StoragePermissionHelper {

    const val STORAGE_PERMISSION_REQUEST_CODE = 4002

    /**
     * Accurately checks whether storage permission is currently granted.
     * On Android 11+ (API 30+), verifies Environment.isExternalStorageManager().
     * On Android 6 to 10 (API 23 to 29), checks WRITE_EXTERNAL_STORAGE and READ_EXTERNAL_STORAGE.
     * NEVER returns a fake 'true' on modern devices.
     */
    fun hasStoragePermission(context: Context): Boolean {
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                    Environment.isExternalStorageManager()
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                    val writeGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    val readGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                    writeGranted && readGranted
                }
                else -> true
            }
        } catch (_: Throwable) {
            // Return false if ungranted or if environment (such as Robolectric) has uninitialized AppOps
            false
        }
    }

    /**
     * Builds the Intent to request All Files Access on Android 11+ (API 30+).
     */
    fun createManageStorageIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (_: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    /**
     * Array of permissions needed for runtime permission requests on Android <= 10.
     */
    fun getLegacyStoragePermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * Direct request method when calling from an Activity.
     */
    fun requestStoragePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                activity.startActivity(intent)
            } catch (_: Exception) {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                activity,
                getLegacyStoragePermissions(),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * User-readable status description for UI display.
     */
    fun getStatusDescription(context: Context): String {
        return if (hasStoragePermission(context)) {
            "Permission Granted: Full write access to /Download/ directory"
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                "Not Granted: Tap to grant All Files Access in System Settings"
            } else {
                "Not Granted: Storage permission required to save files"
            }
        }
    }
}
