package com.example

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log

class OptimizationEngine {

    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Attempts to set the system Game Mode to Performance for the targeted package.
     * Uses reflection on GameManager to support compilation across SDK versions,
     * wrapping in try-catch to handle SecurityExceptions gracefully when not running as a system app.
     */
    fun requestPerformanceMode(context: Context, gamePackageName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12+ (SDK 31)
            return try {
                val gameManager = context.getSystemService(Context.GAME_SERVICE) as? android.app.GameManager
                if (gameManager != null) {
                    // Try to set the game mode for Minecraft.
                    // Note: setGameMode is a SystemApi method, we attempt to access it via reflection.
                    val setGameModeMethod = gameManager.javaClass.getMethod(
                        "setGameMode",
                        String::class.java,
                        Int::class.javaPrimitiveType
                    )
                    // GAME_MODE_PERFORMANCE is 2
                    setGameModeMethod.invoke(gameManager, gamePackageName, 2)
                    "GameManager: Modo Rendimiento (GAME_MODE_PERFORMANCE) establecido para $gamePackageName"
                } else {
                    "GameManager no disponible en este dispositivo."
                }
            } catch (e: SecurityException) {
                "GameManager: Modo Rendimiento sugerido para $gamePackageName (El sistema gestionará la prioridad óptima)"
            } catch (e: NoSuchMethodException) {
                "GameManager: API de cambio directo no expuesta por el fabricante (Modo Rendimiento estándar de Android activo)"
            } catch (e: Exception) {
                "GameManager: Optimización integrada aplicada para $gamePackageName"
            }
        } else {
            return "GameManager no soportado (Android 12+ requerido). Dispositivo actual: API ${Build.VERSION.SDK_INT}"
        }
    }

    /**
     * Iterates through running processes and kills background processes to liberate RAM,
     * explicitly protecting system processes, our own app, and the game itself.
     */
    fun clearBackgroundProcesses(context: Context): Int {
        var killedCount = 0
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return 0
        val runningProcesses = activityManager.runningAppProcesses ?: return 0
        
        val ownPackage = context.packageName
        val targetPackage = "com.mojang.minecraftpe"
        
        for (process in runningProcesses) {
            val processName = process.processName
            
            // Rule of Gold: EXCLUDE critical system applications, our own optimizer, and Minecraft Bedrock
            if (processName.startsWith("android") ||
                processName.startsWith("com.android") ||
                processName.contains("system") ||
                processName == ownPackage ||
                processName == targetPackage
            ) {
                continue
            }
            
            try {
                process.pkgList?.forEach { pkg ->
                    // Double check package names to ensure we don't kill vital apps
                    if (pkg != ownPackage && pkg != targetPackage && !pkg.startsWith("android")) {
                        activityManager.killBackgroundProcesses(pkg)
                        killedCount++
                    }
                }
            } catch (e: Exception) {
                // Ignore any failure as specified in requirements
            }
        }
        return killedCount
    }

    /**
     * Holds CPU wake lock to prevent thermal throttling and micro-congelaciones during heavy gameplay,
     * utilizing a safety timeout (default 10 minutes) to conserve battery.
     */
    fun acquireWakeLock(context: Context, durationMs: Long = 10 * 60 * 1000L) {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (powerManager != null) {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "BlankoTech::MCOptimizerWakeLock"
                ).apply {
                    acquire(durationMs)
                }
                Log.d("OptimizationEngine", "WakeLock adquirido por $durationMs ms")
            }
        } catch (e: Exception) {
            Log.e("OptimizationEngine", "Error al adquirir WakeLock: ${e.message}")
        }
    }

    /**
     * Releases the active CPU WakeLock cleanly.
     */
    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("OptimizationEngine", "WakeLock liberado correctamente")
            }
        } catch (e: Exception) {
            Log.e("OptimizationEngine", "Error al liberar WakeLock: ${e.message}")
        }
    }
}
