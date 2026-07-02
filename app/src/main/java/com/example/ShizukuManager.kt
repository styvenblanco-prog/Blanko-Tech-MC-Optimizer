package com.example

import android.content.pm.PackageManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuManager {
    private const val TAG = "ShizukuManager"

    /**
     * Checks if Shizuku is currently running on the device.
     */
    fun isShizukuRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            Log.e(TAG, "Shizuku pingBinder failed: ${e.message}")
            false
        }
    }

    /**
     * Checks if Shizuku permissions are already granted for our application.
     */
    fun isPermissionGranted(): Boolean {
        return try {
            if (!isShizukuRunning()) return false
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            Log.e(TAG, "Shizuku checkSelfPermission failed: ${e.message}")
            false
        }
    }

    /**
     * Requests the Shizuku permission.
     */
    fun requestPermission(requestCode: Int) {
        try {
            if (isShizukuRunning()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Shizuku requestPermission failed: ${e.message}")
        }
    }

    /**
     * Registers a listener to handle Shizuku permission request responses.
     */
    fun registerListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.addRequestPermissionResultListener(listener)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to register Shizuku permission listener: ${e.message}")
        }
    }

    /**
     * Unregisters a previously registered Shizuku permission listener.
     */
    fun unregisterListener(listener: Shizuku.OnRequestPermissionResultListener) {
        try {
            Shizuku.removeRequestPermissionResultListener(listener)
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to unregister Shizuku permission listener: ${e.message}")
        }
    }
}
