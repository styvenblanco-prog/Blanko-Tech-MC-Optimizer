package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

data class TargetApp(
    val packageName: String,
    val label: String,
    val isGame: Boolean
)

data class FrozenAppState(
    val packageName: String,
    val label: String,
    val isInstalled: Boolean,
    val isFrozen: Boolean
)

data class OptimizerUiState(
    val currentStep: OptimizationStep = OptimizationStep.IDLE,
    val statusText: String = "Dispositivo listo para la optimización",
    val killedProcesses: Int = 0,
    val gameModeStatus: String = "",
    val isOptimizing: Boolean = false,
    val errorMessage: String? = null,
    val installedApps: List<TargetApp> = emptyList(),
    val selectedApp: TargetApp = TargetApp("com.mojang.minecraftpe", "Minecraft Bedrock Edition", true),
    val cpuUsage: Int = 0,
    val ramUsage: Int = 0,
    val ramReadable: String = "",
    val isUsagePermissionGranted: Boolean = false,
    val isOverlayPermissionGranted: Boolean = false,
    val isOverlayActive: Boolean = false,
    val isShizukuRunning: Boolean = false,
    val isShizukuPermissionGranted: Boolean = false,
    val frozenApps: List<FrozenAppState> = emptyList()
)

enum class OptimizationStep {
    IDLE,
    WAKELOCK,
    RAM_CLEANING,
    GAMEMODE,
    LAUNCHING,
    FINISHED,
    ERROR
}

class OptimizerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(OptimizerUiState())
    val uiState: StateFlow<OptimizerUiState> = _uiState.asStateFlow()

    private val optimizerEngine = OptimizationEngine()

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        _uiState.update { it.copy(isShizukuPermissionGranted = granted) }
    }

    init {
        // Start live system CPU stats monitoring loop
        startStatsMonitor()
        ShizukuManager.registerListener(shizukuPermissionListener)
        updateShizukuState()
    }

    private fun startStatsMonitor() {
        viewModelScope.launch {
            while (true) {
                val cpuVal = optimizerEngine.getCpuUsage()
                _uiState.update { state ->
                    state.copy(cpuUsage = cpuVal)
                }
                delay(1500)
            }
        }
    }

    private fun updateShizukuState() {
        val running = ShizukuManager.isShizukuRunning()
        val granted = ShizukuManager.isPermissionGranted()
        _uiState.update { 
            it.copy(
                isShizukuRunning = running,
                isShizukuPermissionGranted = granted
            )
        }
    }

    fun requestShizukuPermission(requestCode: Int = 1001) {
        ShizukuManager.requestPermission(requestCode)
    }

    fun restoreHeavyApps(context: Context) {
        if (!ShizukuManager.isShizukuRunning() || !ShizukuManager.isPermissionGranted()) return
        viewModelScope.launch {
            _uiState.update { it.copy(statusText = "Restaurando aplicaciones en segundo plano...") }
            optimizerEngine.restoreHeavyApps(context) { progress ->
                _uiState.update { it.copy(statusText = progress) }
            }
            refreshFrozenApps(context)
        }
    }

    /**
     * Updates the UI state with exact, fresh system memory reading.
     */
    fun refreshMemoryStats(context: Context) {
        val (ramPercent, ramDesc) = optimizerEngine.getRamStats(context)
        val cpuVal = optimizerEngine.getCpuUsage()
        val usageGranted = optimizerEngine.isUsageAccessGranted(context)
        val overlayGranted = optimizerEngine.isOverlayPermissionGranted(context)
        val shizukuRunning = ShizukuManager.isShizukuRunning()
        val shizukuGranted = ShizukuManager.isPermissionGranted()
        
        _uiState.update { 
            it.copy(
                ramUsage = ramPercent,
                ramReadable = ramDesc,
                cpuUsage = cpuVal,
                isUsagePermissionGranted = usageGranted,
                isOverlayPermissionGranted = overlayGranted,
                isShizukuRunning = shizukuRunning,
                isShizukuPermissionGranted = shizukuGranted
            )
        }
        refreshFrozenApps(context)
    }

    fun refreshFrozenApps(context: Context) {
        val list = listOf(
            Triple("com.whatsapp", "WhatsApp", "whatsapp"),
            Triple("com.whatsapp.w4b", "WhatsApp Business", "whatsapp_business"),
            Triple("com.facebook.katana", "Facebook", "facebook"),
            Triple("com.instagram.android", "Instagram", "instagram"),
            Triple("com.android.vending", "Google Play Store", "playstore")
        )
        
        val updated = list.map { (pkg, name, _) ->
            val isInstalled = isAppInstalled(context, pkg)
            val isFrozen = if (isInstalled) isAppFrozen(context, pkg) else false
            FrozenAppState(pkg, name, isInstalled, isFrozen)
        }
        _uiState.update { it.copy(frozenApps = updated) }
    }

    private fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
            true
        } catch (e: Exception) {
            try {
                context.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
                true
            } catch (ex: Exception) {
                false
            }
        }
    }

    private fun isAppFrozen(context: Context, packageName: String): Boolean {
        return try {
            val state = context.packageManager.getApplicationEnabledSetting(packageName)
            state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED ||
                    state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER ||
                    state == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        } catch (e: Exception) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(packageName, android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
                !appInfo.enabled
            } catch (ex: Exception) {
                false
            }
        }
    }

    fun toggleAppFreeze(context: Context, packageName: String) {
        if (!ShizukuManager.isShizukuRunning()) {
            _uiState.update { it.copy(statusText = "Error: Shizuku no está activo en segundo plano.") }
            return
        }
        if (!ShizukuManager.isPermissionGranted()) {
            _uiState.update { it.copy(statusText = "Error: Permiso de Shizuku no concedido.") }
            requestShizukuPermission()
            return
        }
        viewModelScope.launch {
            val isCurrentlyFrozen = isAppFrozen(context, packageName)
            val nextState = !isCurrentlyFrozen
            _uiState.update { it.copy(statusText = if (nextState) "Congelando $packageName..." else "Descongelando $packageName...") }
            
            withContext(Dispatchers.IO) {
                optimizerEngine.setAppFrozenState(packageName, nextState)
            }
            
            delay(500)
            refreshFrozenApps(context)
            
            val doubleCheckState = isAppFrozen(context, packageName)
            if (doubleCheckState == nextState) {
                _uiState.update { it.copy(statusText = if (nextState) "$packageName congelado con éxito." else "$packageName reactivado con éxito.") }
            } else {
                _uiState.update { it.copy(statusText = "Error al congelar/activar $packageName. Verifica el estado de Shizuku.") }
            }
        }
    }

    /**
     * Scans and loads launcher activities of installed apps/games.
     */
    fun loadInstalledApps(context: Context) {
        viewModelScope.launch {
            val appsList = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val rawList = pm.queryIntentActivities(intent, 0)
                val apps = mutableListOf<TargetApp>()
                val ownPackage = context.packageName

                for (resolveInfo in rawList) {
                    val packageName = resolveInfo.activityInfo.packageName
                    if (packageName == ownPackage) continue

                    val label = resolveInfo.loadLabel(pm).toString()
                    val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        try {
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            (appInfo.category == android.content.pm.ApplicationInfo.CATEGORY_GAME) ||
                                    (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_IS_GAME) != 0
                        } catch (e: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                    apps.add(TargetApp(packageName, label, isGame || packageName == "com.mojang.minecraftpe"))
                }

                apps.sortedWith(
                    compareByDescending<TargetApp> { it.packageName == "com.mojang.minecraftpe" }
                        .thenByDescending { it.isGame }
                        .thenBy { it.label }
                )
            }

            _uiState.update { it.copy(installedApps = appsList) }
        }
    }

    fun selectApp(app: TargetApp) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    /**
     * Starts or stops the floating overlay widget service based on current status.
     */
    fun toggleOverlay(context: Context) {
        val currentActive = _uiState.value.isOverlayActive
        val intent = Intent(context, OverlayService::class.java)
        
        if (currentActive) {
            context.stopService(intent)
            _uiState.update { it.copy(isOverlayActive = false) }
        } else {
            if (optimizerEngine.isOverlayPermissionGranted(context)) {
                context.startService(intent)
                _uiState.update { it.copy(isOverlayActive = true) }
            } else {
                // Request permission flow from UI
                _uiState.update { it.copy(errorMessage = "overlay_permission_required") }
            }
        }
    }

    fun setOverlayActiveState(active: Boolean) {
        _uiState.update { it.copy(isOverlayActive = active) }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun startOptimization(context: Context, onLaunchSuccess: () -> Unit) {
        if (_uiState.value.isOptimizing) return
        val targetApp = _uiState.value.selectedApp

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isOptimizing = true, 
                    currentStep = OptimizationStep.WAKELOCK,
                    statusText = context.getString(R.string.status_wakelock),
                    errorMessage = null
                ) 
            }
            delay(600)

            // 1. WakeLock Phase
            optimizerEngine.acquireWakeLock(context)
            
            // 2. RAM Cleaning Phase
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.RAM_CLEANING,
                    statusText = context.getString(R.string.status_ram)
                ) 
            }
            delay(800)
            
            val killed = withContext(Dispatchers.IO) {
                optimizerEngine.clearBackgroundProcesses(context, targetApp.packageName)
            }
            
            _uiState.update { 
                it.copy(
                    killedProcesses = killed,
                    statusText = context.getString(R.string.ram_cleared_format, killed)
                ) 
            }
            delay(1000)

            // Integrate Shizuku App Freezing if Shizuku is running and granted
            if (_uiState.value.isShizukuRunning && _uiState.value.isShizukuPermissionGranted) {
                _uiState.update { it.copy(statusText = "Localizando apps pesadas en segundo plano...") }
                delay(600)
                optimizerEngine.freezeHeavyApps(context) { progress ->
                    _uiState.update { it.copy(statusText = progress) }
                }
                refreshFrozenApps(context)
                delay(1200)
            }

            // 3. GameMode API Phase
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.GAMEMODE,
                    statusText = context.getString(R.string.status_gamemode)
                ) 
            }
            delay(800)
            
            val gameModeResult = optimizerEngine.requestPerformanceMode(context, targetApp.packageName)
            _uiState.update { 
                it.copy(
                    gameModeStatus = gameModeResult,
                    statusText = gameModeResult
                ) 
            }
            delay(1000)

            // 4. Launching Phase
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.LAUNCHING,
                    statusText = context.getString(R.string.status_launching)
                ) 
            }
            delay(800)

            val packageManager = context.packageManager
            val launchIntent = packageManager.getLaunchIntentForPackage(targetApp.packageName)
            
            if (launchIntent != null) {
                _uiState.update { 
                    it.copy(
                        currentStep = OptimizationStep.FINISHED,
                        statusText = context.getString(R.string.status_finished)
                    ) 
                }
                delay(400)
                
                // Launch Game
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                
                // Reset state & execute completion callback
                _uiState.update { it.copy(isOptimizing = false) }
                onLaunchSuccess()
            } else {
                _uiState.update { 
                    it.copy(
                        currentStep = OptimizationStep.ERROR,
                        isOptimizing = false,
                        statusText = context.getString(R.string.error_not_installed),
                        errorMessage = context.getString(R.string.error_not_installed)
                    ) 
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        optimizerEngine.releaseWakeLock()
        ShizukuManager.unregisterListener(shizukuPermissionListener)
    }
}
