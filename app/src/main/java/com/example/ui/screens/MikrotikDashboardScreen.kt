package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
                title = { Text("Dashboard MikroTik", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
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
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Online Status and Basic Info
                            OutlinedCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    ListItem(
                                        headlineContent = { Text(data.nombre ?: "Router Desconocido") },
                                        supportingContent = { Text("IP: ${data.ipAddress ?: "N/D"}") },
                                        leadingContent = { Icon(Icons.Rounded.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        trailingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text("ONLINE", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                            }
                                        },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }

                            // CPU & Memory Gauges
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                // CPU Gauge
                                val cpuLoad = data.cpuLoad ?: 0
                                val cpuColor = if (cpuLoad > 80) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                
                                OutlinedCard(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("CPU", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                            CircularProgressIndicator(
                                                progress = { 1f },
                                                modifier = Modifier.fillMaxSize(),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeWidth = 8.dp,
                                                strokeCap = StrokeCap.Round
                                            )
                                            CircularProgressIndicator(
                                                progress = { cpuLoad / 100f },
                                                modifier = Modifier.fillMaxSize(),
                                                color = cpuColor,
                                                strokeWidth = 8.dp,
                                                strokeCap = StrokeCap.Round
                                            )
                                            Text("${cpuLoad}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cpuColor)
                                        }
                                    }
                                }

                                // Memory Gauge
                                val freeMem = parseSizeToMB(data.freeMemory)
                                val totalMem = parseSizeToMB(data.totalMemory)
                                val usedMem = totalMem - freeMem
                                val memPercent = if (totalMem > 0) (usedMem / totalMem).toFloat() else 0f
                                val memColor = if (memPercent > 0.85f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary

                                OutlinedCard(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("RAM", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                                            CircularProgressIndicator(
                                                progress = { 1f },
                                                modifier = Modifier.fillMaxSize(),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                strokeWidth = 8.dp,
                                                strokeCap = StrokeCap.Round
                                            )
                                            CircularProgressIndicator(
                                                progress = { memPercent },
                                                modifier = Modifier.fillMaxSize(),
                                                color = memColor,
                                                strokeWidth = 8.dp,
                                                strokeCap = StrokeCap.Round
                                            )
                                            Text("${(memPercent * 100).toInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = memColor)
                                        }
                                    }
                                }
                            }

                            // System Info List
                            OutlinedCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    ListItem(
                                        headlineContent = { Text("Información de Sistema", fontWeight = FontWeight.Bold) },
                                        leadingContent = { Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    ListItem(
                                        headlineContent = { Text("Uptime") },
                                        supportingContent = { Text(data.uptime ?: "-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                                        leadingContent = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    ListItem(
                                        headlineContent = { Text("Versión RouterOS") },
                                        supportingContent = { Text(data.version ?: "-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                                        leadingContent = { Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                                    ListItem(
                                        headlineContent = { Text("Última Actualización") },
                                        supportingContent = { Text(data.lastUpdate ?: "-", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                                        leadingContent = { Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
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
