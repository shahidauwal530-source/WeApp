package com.example.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat

data class PermissionStatusSummary(
    val cameraGranted: Boolean = false,
    val micGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val notificationsGranted: Boolean = true
)

object WeAppPermissions {
    fun requiredRuntimePermissions(): List<String> = buildList {
        add(Manifest.permission.CAMERA)
        add(Manifest.permission.RECORD_AUDIO)
        add(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun isGranted(context: Context, permission: String): Boolean = runCatching {
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    fun missingRuntimePermissions(context: Context): List<String> =
        requiredRuntimePermissions().filter { !isGranted(context, it) }

    fun checkStatuses(context: Context): PermissionStatusSummary = PermissionStatusSummary(
        cameraGranted = isGranted(context, Manifest.permission.CAMERA),
        micGranted = isGranted(context, Manifest.permission.RECORD_AUDIO),
        contactsGranted = isGranted(context, Manifest.permission.READ_CONTACTS),
        notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }
    )

    fun openAppSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
