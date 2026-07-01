package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
  private val viewModel: OptimizerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
        ) { innerPadding ->
          OptimizerScreen(
            viewModel = viewModel,
            onLaunchSuccess = { finish() },
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun OptimizerScreen(
  viewModel: OptimizerViewModel,
  onLaunchSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  // Pulse animation for central glowing optimizer button
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val buttonGlowValue by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 16f,
    animationSpec = infiniteRepeatable(
      animation = tween(1500, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "glow"
  )
  val buttonGlowSize = buttonGlowValue.dp

  val buttonScale by animateFloatAsState(
    targetValue = if (uiState.isOptimizing) 0.94f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "scale"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // Top brand title
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(top = 16.dp)
    ) {
      Text(
        text = "BLANKOTECH",
        color = NeonCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 4.sp,
        fontFamily = FontFamily.SansSerif
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "MC OPTIMIZER",
        color = TextPrimary,
        fontSize = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.sp,
        fontFamily = FontFamily.SansSerif
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = "v1.0 • Minecraft Bedrock Edition",
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium
      )
    }

    // Dynamic Central Action Button with Glowing cyber effects
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(240.dp)
        .padding(16.dp)
    ) {
      // Background gradient pulse glow
      Box(
        modifier = Modifier
          .size(150.dp)
          .shadow(
            elevation = if (uiState.isOptimizing) 0.dp else buttonGlowSize,
            shape = CircleShape,
            clip = false,
            ambientColor = NeonGreen,
            spotColor = NeonGreen
          )
          .background(Color.Transparent)
      )

      // Primary interactive button
      Card(
        shape = CircleShape,
        colors = CardDefaults.cardColors(
          containerColor = if (uiState.isOptimizing) DarkSurface else NeonGreen
        ),
        border = BorderStroke(
          width = 2.dp,
          color = if (uiState.isOptimizing) NeonGreen.copy(alpha = 0.4f) else NeonCyan
        ),
        modifier = Modifier
          .size(150.dp * buttonScale)
          .testTag("submit_button")
          .clickable(
            enabled = !uiState.isOptimizing,
            interactionSource = remember { MutableInteractionSource() },
            indication = LocalIndication.current
          ) {
            viewModel.startOptimization(context, onLaunchSuccess)
          },
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxSize(),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center
        ) {
          if (uiState.isOptimizing) {
            CircularProgressIndicator(
              color = NeonGreen,
              strokeWidth = 4.dp,
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "OPTIMIZANDO",
              color = NeonGreen,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          } else {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Optimize Launcher Button",
              tint = DarkBackground,
              modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "OPTIMIZAR",
              color = DarkBackground,
              fontSize = 14.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Y JUGAR",
              color = DarkBackground,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          }
        }
      }
    }

    // Diagnostic & Optimization Steps Panel
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(DarkSurface)
        .border(BorderStroke(1.dp, Color(0xFF232D34)), RoundedCornerShape(16.dp))
        .padding(20.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "ESTADO DEL SISTEMA",
          color = TextSecondary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.5.sp
        )
        // Cyber Status Indicator
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
              if (uiState.isOptimizing) Color(0xFF1E281F) else Color(0xFF16252C)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text(
            text = if (uiState.isOptimizing) "ACTIVO" else "ESTABLE",
            color = if (uiState.isOptimizing) NeonGreen else NeonCyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Status descriptive text
      Text(
        text = uiState.statusText,
        color = if (uiState.errorMessage != null) Color(0xFFFF5252) else TextPrimary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Start,
        modifier = Modifier
          .fillMaxWidth()
          .height(36.dp)
          .testTag("status_text")
      )

      Spacer(modifier = Modifier.height(16.dp))

      // List of atomic optimization phases
      StepItem(
        title = "CPU WakeLock (Partial)",
        icon = Icons.Default.Speed,
        isActive = uiState.currentStep.ordinal >= OptimizationStep.WAKELOCK.ordinal,
        isCompleted = uiState.currentStep.ordinal > OptimizationStep.WAKELOCK.ordinal
      )
      Spacer(modifier = Modifier.height(10.dp))
      StepItem(
        title = "Limpieza de RAM (Procesos)",
        icon = Icons.Default.Memory,
        isActive = uiState.currentStep.ordinal >= OptimizationStep.RAM_CLEANING.ordinal,
        isCompleted = uiState.currentStep.ordinal > OptimizationStep.RAM_CLEANING.ordinal,
        detailsText = if (uiState.killedProcesses > 0) "${uiState.killedProcesses} cerrados" else null
      )
      Spacer(modifier = Modifier.height(10.dp))
      StepItem(
        title = "GameManager Performance API",
        icon = Icons.Default.Bolt,
        isActive = uiState.currentStep.ordinal >= OptimizationStep.GAMEMODE.ordinal,
        isCompleted = uiState.currentStep.ordinal > OptimizationStep.GAMEMODE.ordinal
      )
      Spacer(modifier = Modifier.height(10.dp))
      StepItem(
        title = "Ejecutar Minecraft Bedrock",
        icon = Icons.Default.PlayArrow,
        isActive = uiState.currentStep.ordinal >= OptimizationStep.LAUNCHING.ordinal,
        isCompleted = uiState.currentStep.ordinal >= OptimizationStep.FINISHED.ordinal,
        isError = uiState.currentStep == OptimizationStep.ERROR
      )
    }

    // Safety and Compliance Native Information Banner
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 16.dp)
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFF0D1E16))
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = "Safety Information Icon",
          tint = NeonGreen,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = stringResource(id = R.string.no_root_banner),
          color = NeonGreen,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold
        )
      }
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "Optimizaciones con APIs nativas de Android. Sin Root ni comandos su.",
        color = TextSecondary,
        fontSize = 10.sp,
        textAlign = TextAlign.Center
      )
    }
  }
}

@Composable
fun StepItem(
  title: String,
  icon: ImageVector,
  isActive: Boolean,
  isCompleted: Boolean,
  detailsText: String? = null,
  isError: Boolean = false
) {
  val iconColor = when {
    isError -> Color(0xFFFF5252)
    isCompleted -> NeonGreen
    isActive -> NeonCyan
    else -> TextSecondary.copy(alpha = 0.3f)
  }

  val textColor = when {
    isError -> Color(0xFFFF5252)
    isActive || isCompleted -> TextPrimary
    else -> TextSecondary.copy(alpha = 0.4f)
  }

  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(24.dp)
          .clip(CircleShape)
          .background(iconColor.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = iconColor,
          modifier = Modifier.size(14.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = title,
        color = textColor,
        fontSize = 12.sp,
        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
      )
    }

    if (detailsText != null && isCompleted) {
      Text(
        text = detailsText,
        color = NeonGreen,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(end = 4.dp)
      )
    } else {
      Icon(
        imageVector = when {
          isError -> Icons.Default.Error
          isCompleted -> Icons.Default.CheckCircle
          isActive -> Icons.Default.PlayArrow
          else -> Icons.Default.CheckCircle
        },
        contentDescription = "Status symbol",
        tint = when {
          isError -> Color(0xFFFF5252)
          isCompleted -> NeonGreen
          isActive -> NeonCyan
          else -> TextSecondary.copy(alpha = 0.15f)
        },
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
