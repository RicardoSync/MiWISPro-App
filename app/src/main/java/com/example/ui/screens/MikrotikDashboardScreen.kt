package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.MikrotikStatsResponse
import com.example.ui.theme.GreenBadgeBg
import com.example.ui.theme.GreenBadgeText
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikrotikDashboardScreen(
    routerId: String,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var statsState by remember(routerId) { mutableStateOf<UiState<MikrotikStatsResponse>>(UiState.Loading) }

    LaunchedEffect(routerId) {
        viewModel.getMikrotikStatsRemote(routerId) { state ->
            statsState = state
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dashboard Mikrotik", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = statsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.getMikrotikStatsRemote(routerId) { statsState = it } }) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val data = state.data.data
                    if (data == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontró información del router.", color = MaterialTheme.colorScheme.secondary)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Header
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = GreenBadgeBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Dns, contentDescription = null, tint = GreenBadgeText)
                                        Text(data.nombre ?: "Router", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = GreenBadgeText)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("IP: ${data.ipAddress ?: "N/D"}", style = MaterialTheme.typography.bodyLarge, color = GreenBadgeText.copy(alpha = 0.8f))
                                }
                            }

                            // Gauges
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // CPU Gauge
                                Card(
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("CPU Load", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        GaugeChart(
                                            percentage = (data.cpuLoad ?: 0) / 100f,
                                            color = if ((data.cpuLoad ?: 0) > 80) Color.Red else MaterialTheme.colorScheme.primary,
                                            label = "${data.cpuLoad ?: 0}%"
                                        )
                                    }
                                }

                                // Memory Gauge
                                // Attempt to parse "76.9MiB" vs "128MiB"
                                val freeMem = parseSizeToMB(data.freeMemory)
                                val totalMem = parseSizeToMB(data.totalMemory)
                                val usedMem = totalMem - freeMem
                                val memPercent = if (totalMem > 0) (usedMem / totalMem).toFloat() else 0f

                                Card(
                                    shape = RoundedCornerShape(28.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Memoria Usada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        GaugeChart(
                                            percentage = memPercent,
                                            color = MaterialTheme.colorScheme.secondary,
                                            label = "${(memPercent * 100).toInt()}%"
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("${String.format("%.1f", usedMem)} / ${String.format("%.1f", totalMem)} MB", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }

                            // System Info List
                            Card(
                                shape = RoundedCornerShape(28.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text("Información de Sistema", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Divider()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    DetailRow("Uptime", data.uptime ?: "-")
                                    DetailRow("Versión RouterOS", data.version ?: "-")
                                    DetailRow("Última Actualización", data.lastUpdate ?: "-")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GaugeChart(percentage: Float, color: Color, label: String, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 240f
            val startAngle = 150f
            val strokeWidth = 12.dp.toPx()
            
            // Background arc
            drawArc(
                color = color.copy(alpha = 0.2f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )

            // Foreground arc
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle * percentage.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun parseSizeToMB(sizeStr: String?): Double {
    if (sizeStr == null) return 0.0
    val cleaned = sizeStr.uppercase().trim()
    val value = cleaned.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
    return when {
        cleaned.contains("MIB") || cleaned.contains("MB") -> value
        cleaned.contains("KIB") || cleaned.contains("KB") -> value / 1024.0
        cleaned.contains("GIB") || cleaned.contains("GB") -> value * 1024.0
        else -> value / (1024.0 * 1024.0) // Assume bytes if no unit
    }
}
