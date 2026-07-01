package com.example

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

data class OptimizerUiState(
    val currentStep: OptimizationStep = OptimizationStep.IDLE,
    val statusText: String = "Dispositivo listo para la optimización",
    val killedProcesses: Int = 0,
    val gameModeStatus: String = "",
    val isOptimizing: Boolean = false,
    val errorMessage: String? = null
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
    private val minecraftPackage = "com.mojang.minecraftpe"

    fun startOptimization(context: Context, onLaunchSuccess: () -> Unit) {
        if (_uiState.value.isOptimizing) return

        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    isOptimizing = true, 
                    currentStep = OptimizationStep.WAKELOCK,
                    statusText = context.getString(R.string.status_wakelock),
                    errorMessage = null
                ) 
            }
            delay(600) // Smooth visual delay

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
                optimizerEngine.clearBackgroundProcesses(context)
            }
            
            _uiState.update { 
                it.copy(
                    killedProcesses = killed,
                    statusText = context.getString(R.string.ram_cleared_format, killed)
                ) 
            }
            delay(1000)

            // 3. GameMode API Phase
            _uiState.update { 
                it.copy(
                    currentStep = OptimizationStep.GAMEMODE,
                    statusText = context.getString(R.string.status_gamemode)
                ) 
            }
            delay(800)
            
            val gameModeResult = optimizerEngine.requestPerformanceMode(context, minecraftPackage)
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
            val launchIntent = packageManager.getLaunchIntentForPackage(minecraftPackage)
            
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
    }
}
