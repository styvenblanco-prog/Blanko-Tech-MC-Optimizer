package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
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

  override fun onResume() {
    super.onResume()
    // Instantly refresh telemetry and list of apps when user switches back
    viewModel.refreshMemoryStats(this)
    viewModel.loadInstalledApps(this)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptimizerScreen(
  viewModel: OptimizerViewModel,
  onLaunchSuccess: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  var showAppSelector by remember { mutableStateOf(false) }
  var searchQuery by remember { mutableStateOf("") }

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

  // Auto trigger stats refresh inside view context
  LaunchedEffect(Unit) {
    viewModel.loadInstalledApps(context)
    while (true) {
      viewModel.refreshMemoryStats(context)
      delay(2000)
    }
  }

  // Error/Permission handler dialogs
  if (uiState.errorMessage == "overlay_permission_required") {
    AlertDialog(
      onDismissRequest = { viewModel.clearErrorMessage() },
      title = { Text(text = stringResource(id = R.string.permission_overlay_title), color = TextPrimary) },
      text = { Text(text = stringResource(id = R.string.permission_overlay_desc), color = TextSecondary) },
      containerColor = DarkSurface,
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearErrorMessage()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
              val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
              ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              context.startActivity(intent)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DarkBackground)
        ) {
          Text(text = stringResource(id = R.string.btn_grant_permission))
        }
      },
      dismissButton = {
        TextButton(onClick = { viewModel.clearErrorMessage() }) {
          Text(text = "CANCELAR", color = TextSecondary)
        }
      }
    )
  }

  // Target App Selector Dialog
  if (showAppSelector) {
    AlertDialog(
      onDismissRequest = { showAppSelector = false },
      title = {
        Text(
          text = stringResource(id = R.string.btn_select_app),
          color = TextPrimary,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text(text = stringResource(id = R.string.search_apps_placeholder), color = TextSecondary.copy(alpha = 0.6f)) },
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = NeonCyan,
              unfocusedBorderColor = Color(0xFF232D34),
              focusedContainerColor = Color(0xFF0F161A),
              unfocusedContainerColor = Color(0xFF0F161A)
            ),
            singleLine = true,
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextSecondary) }
          )

          Box(modifier = Modifier.height(260.dp)) {
            val filteredApps = uiState.installedApps.filter {
              it.label.contains(searchQuery, ignoreCase = true) ||
                      it.packageName.contains(searchQuery, ignoreCase = true)
            }

            if (filteredApps.isEmpty()) {
              Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No se encontraron aplicaciones", color = TextSecondary, fontSize = 13.sp)
              }
            } else {
              LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                items(filteredApps) { app ->
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .clip(RoundedCornerShape(8.dp))
                      .background(if (uiState.selectedApp.packageName == app.packageName) Color(0xFF16252C) else Color.Transparent)
                      .clickable {
                        viewModel.selectApp(app)
                        showAppSelector = false
                        searchQuery = ""
                      }
                      .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Box(
                      modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (app.isGame) Color(0xFF0D1E16) else Color(0xFF101C24)),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(
                        imageVector = if (app.isGame) Icons.Default.Gamepad else Icons.Default.Android,
                        contentDescription = null,
                        tint = if (app.isGame) NeonGreen else NeonCyan,
                        modifier = Modifier.size(18.dp)
                      )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                      Text(
                        text = app.label,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                      Text(
                        text = app.packageName,
                        color = TextSecondary,
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                      )
                    }

                    if (uiState.selectedApp.packageName == app.packageName) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }
      },
      containerColor = DarkSurface,
      confirmButton = {
        TextButton(onClick = {
          showAppSelector = false
          searchQuery = ""
        }) {
          Text(text = "CERRAR", color = NeonCyan)
        }
      }
    )
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .verticalScroll(rememberScrollState())
      .padding(24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(20.dp)
  ) {
    // Top brand title
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(top = 8.dp)
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
        text = "v1.1 • Optimizador Profesional de Juegos",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium
      )
    }

    // Live Telemetry Widget
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // RAM Meter Card
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF232D34))
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Memory,
              contentDescription = null,
              tint = NeonCyan,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(id = R.string.stats_ram_label),
              color = TextSecondary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${uiState.ramUsage}%",
            color = NeonCyan,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
          )
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { uiState.ramUsage.toFloat() / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(CircleShape),
            color = NeonCyan,
            trackColor = Color(0xFF16252C)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = uiState.ramReadable.ifEmpty { "Escaneando..." },
            color = TextSecondary,
            fontSize = 9.sp,
            maxLines = 1
          )
        }
      }

      // CPU Meter Card
      Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF232D34))
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = null,
              tint = NeonGreen,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = stringResource(id = R.string.stats_cpu),
              color = TextSecondary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "${uiState.cpuUsage}%",
            color = NeonGreen,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black
          )
          Spacer(modifier = Modifier.height(4.dp))
          LinearProgressIndicator(
            progress = { uiState.cpuUsage.toFloat() / 100f },
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(CircleShape),
            color = NeonGreen,
            trackColor = Color(0xFF0D1E16)
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Núcleos Activos",
            color = TextSecondary,
            fontSize = 9.sp
          )
        }
      }
    }

    // App Selector Trigger Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clickable { showAppSelector = true },
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF0F161A)),
      border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
    ) {
      Row(
        modifier = Modifier.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF16252C)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (uiState.selectedApp.isGame) Icons.Default.Gamepad else Icons.Default.Android,
            contentDescription = null,
            tint = NeonCyan,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "OPTIMIZAR APLICACIÓN",
            color = TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = uiState.selectedApp.label,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
        Icon(
          imageVector = Icons.Default.SwapHoriz,
          contentDescription = "Change target",
          tint = NeonCyan,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    // Dynamic Central Action Button with Glowing cyber effects
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(200.dp)
        .padding(8.dp)
    ) {
      Box(
        modifier = Modifier
          .size(130.dp)
          .shadow(
            elevation = if (uiState.isOptimizing) 0.dp else buttonGlowSize,
            shape = CircleShape,
            clip = false,
            ambientColor = NeonGreen,
            spotColor = NeonGreen
          )
          .background(Color.Transparent)
      )

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
          .size(130.dp * buttonScale)
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
              strokeWidth = 3.dp,
              modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "OPTIMIZANDO",
              color = NeonGreen,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          } else {
            Icon(
              imageVector = Icons.Default.Bolt,
              contentDescription = "Optimize Launcher Button",
              tint = DarkBackground,
              modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "OPTIMIZAR",
              color = DarkBackground,
              fontSize = 13.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp
            )
            Text(
              text = "Y COMPRESER",
              color = DarkBackground,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp
            )
          }
        }
      }
    }

    // Permission and Overlay Utilities Card Section
    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Usage stats warning alert
      if (!uiState.isUsagePermissionGranted) {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1304)),
          border = BorderStroke(1.dp, Color(0xFF6B4500))
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(id = R.string.permission_usage_title),
                color = Color(0xFFFF9800),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = stringResource(id = R.string.permission_usage_desc),
              color = TextSecondary,
              fontSize = 10.sp,
              lineHeight = 14.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                  addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8C5C00), contentColor = Color.White),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(vertical = 4.dp)
            ) {
              Text(text = stringResource(id = R.string.btn_grant_permission), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Floating window control card
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, Color(0xFF232D34))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF16252C)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(16.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "MONITOR FLOTANTE",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Mostrar RAM y CPU sobre el juego",
                color = TextSecondary,
                fontSize = 10.sp
              )
            }
          }

          Switch(
            checked = uiState.isOverlayActive,
            onCheckedChange = { viewModel.toggleOverlay(context) },
            colors = SwitchDefaults.colors(
              checkedThumbColor = NeonCyan,
              checkedTrackColor = Color(0xFF0F2C33),
              uncheckedThumbColor = TextSecondary,
              uncheckedTrackColor = Color(0xFF16252C)
            )
          )
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
        title = "Ejecutar ${uiState.selectedApp.label}",
        icon = Icons.Default.PlayArrow,
        isActive = uiState.currentStep.ordinal >= OptimizationStep.LAUNCHING.ordinal,
        isCompleted = uiState.currentStep.ordinal >= OptimizationStep.FINISHED.ordinal,
        isError = uiState.currentStep == OptimizationStep.ERROR
      )
    }

    // Technical Safety Footer
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(vertical = 8.dp)
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
