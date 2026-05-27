package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.data.MikrotikStatsResponse
import com.example.data.MikrotikAccionResponse
import com.example.data.MikrotikTrafficData
import com.example.data.MikrotikTrafficResponse
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
    val context = LocalContext.current
    var showConfirmDialog by remember { mutableStateOf(false) }
    var isRestarting by remember { mutableStateOf(false) }
    var interfacesState by remember(routerId) { mutableStateOf<UiState<MikrotikAccionResponse>?>(null) }
    var selectedInterfaceForTraffic by remember { mutableStateOf<String?>(null) }
    var trafficReadings by remember { mutableStateOf<List<MikrotikTrafficData>>(emptyList()) }
    var currentTrafficProgress by remember { mutableStateOf(0) }
    var trafficError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(routerId) {
        viewModel.getMikrotikStatsRemote(routerId) { state ->
            statsState = state
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            icon = { Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("¿Confirmar Reinicio?") },
            text = { Text("El router quedará fuera de línea unos minutos. ¿Estás seguro de que deseas reiniciar este MikroTik?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        isRestarting = true
                        viewModel.reiniciarMikrotikRemote(routerId) { success, message ->
                            isRestarting = false
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Text("Confirmar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val currentInterfaceName = selectedInterfaceForTraffic
    if (currentInterfaceName != null) {
        AlertDialog(
            onDismissRequest = {
                selectedInterfaceForTraffic = null
                currentTrafficProgress = 0
                trafficReadings = emptyList()
                trafficError = null
            },
            icon = {
                Icon(
                    Icons.Rounded.TrendingUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    "Tráfico en Tiempo Real",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Interfaz: $currentInterfaceName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val progress = currentTrafficProgress
                    val error = trafficError
                    val readings = trafficReadings

                    if (error != null) {
                        // Error State
                        Icon(
                            Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    } else if (progress in 1..5 && readings.size < 5) {
                        // Loading / Monitoring State
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Muestreando... Lectura $progress de 5",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (readings.isNotEmpty()) {
                            val latest = readings.last()
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Lectura Actual",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Rounded.ArrowDownward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Descarga (Rx)", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                latest.rxHuman ?: "0 bps",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Rounded.ArrowUpward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Carga (Tx)", style = MaterialTheme.typography.labelSmall)
                                            Text(
                                                latest.txHuman ?: "0 bps",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (readings.size == 5) {
                        // Finished / Results State
                        val avgRxBits = readings.mapNotNull { it.rxBitsPerSecond }.average()
                        val avgTxBits = readings.mapNotNull { it.txBitsPerSecond }.average()
                        val peakRxBits = readings.mapNotNull { it.rxBitsPerSecond }.maxOrNull()?.toDouble() ?: 0.0
                        val peakTxBits = readings.mapNotNull { it.txBitsPerSecond }.maxOrNull()?.toDouble() ?: 0.0

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Summary Cards
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Consumo Promedio (5 seg)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Descarga (Rx):", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            formatBitsToHuman(avgRxBits),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Carga (Tx):", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            formatBitsToHuman(avgTxBits),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Picos de Velocidad",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Peak Descarga:", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            formatBitsToHuman(peakRxBits),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Peak Carga:", style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            formatBitsToHuman(peakTxBits),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }

                            // Readings Detail
                            Text(
                                "Historial de Muestras",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for ((idx, reading) in readings.withIndex()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Seg ${idx + 1}:",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Rx ${reading.rxHuman} | Tx ${reading.txHuman}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedInterfaceForTraffic = null
                        currentTrafficProgress = 0
                        trafficReadings = emptyList()
                        trafficError = null
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
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

                            // Control Actions Card
                            OutlinedCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            "Acciones de Control",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Reiniciar el MikroTik dejará sin conexión temporalmente a todos los clientes asociados a este router.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { showConfirmDialog = true },
                                        enabled = !isRestarting,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error,
                                            contentColor = MaterialTheme.colorScheme.onError
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isRestarting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onError,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Enviando comando...")
                                        } else {
                                            Icon(
                                                Icons.Rounded.RestartAlt,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Reiniciar MikroTik", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interfaces Card
                            OutlinedCard(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Header
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.SettingsEthernet,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                "Interfaces del Router",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        // Total Badge when success
                                        val currentInterfacesState = interfacesState
                                        if (currentInterfacesState is UiState.Success<MikrotikAccionResponse>) {
                                            val total = currentInterfacesState.data.total ?: 0
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    "$total Interfaces",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(16.dp))

                                    val currentIfaceState = interfacesState
                                    when (currentIfaceState) {
                                        null -> {
                                            Button(
                                                onClick = {
                                                    viewModel.getMikrotikInterfacesRemote(routerId) {
                                                        interfacesState = it
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    Icons.Rounded.List,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Listar Interfaces", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        is UiState.Loading -> {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 24.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                        is UiState.Error -> {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                            ) {
                                                Text(currentIfaceState.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Button(
                                                    onClick = {
                                                        viewModel.getMikrotikInterfacesRemote(routerId) {
                                                            interfacesState = it
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Reintentar")
                                                }
                                            }
                                        }
                                        is UiState.Success<MikrotikAccionResponse> -> {
                                            val interfaces = currentIfaceState.data.data ?: emptyList()
                                            if (interfaces.isEmpty()) {
                                                Text(
                                                    "No se encontraron interfaces en este router.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            } else {
                                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                    for ((index, iface) in interfaces.withIndex()) {
                                                        if (index > 0) {
                                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                                        }

                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable {
                                                                    val ifaceName = iface.name
                                                                    if (ifaceName != null) {
                                                                        selectedInterfaceForTraffic = ifaceName
                                                                        currentTrafficProgress = 1
                                                                        trafficReadings = emptyList()
                                                                        trafficError = null
                                                                        viewModel.monitorMikrotikTrafficRemote(
                                                                            routerId = routerId,
                                                                            interfaceName = ifaceName,
                                                                            onProgress = { idx, data ->
                                                                                currentTrafficProgress = idx
                                                                                trafficReadings = trafficReadings + data
                                                                            },
                                                                            onFinished = { readings ->
                                                                                currentTrafficProgress = 5
                                                                                trafficReadings = readings
                                                                            },
                                                                            onError = { errMsg ->
                                                                                trafficError = errMsg
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            // Left Icon depending on Type
                                                            val icon = when (iface.type?.lowercase()) {
                                                                "ether" -> Icons.Rounded.Cable
                                                                "wg" -> Icons.Rounded.Shield
                                                                "loopback" -> Icons.Rounded.SyncAlt
                                                                else -> Icons.Rounded.NetworkCheck
                                                            }
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(40.dp)
                                                                    .background(
                                                                        if (iface.disabled == true) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                                                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                                        shape = RoundedCornerShape(8.dp)
                                                                    ),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Icon(
                                                                    icon,
                                                                    contentDescription = null,
                                                                    tint = if (iface.disabled == true) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                                                    modifier = Modifier.size(20.dp)
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(12.dp))

                                                            // Details
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    iface.name ?: "-",
                                                                    style = MaterialTheme.typography.bodyLarge,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (iface.disabled == true) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                                                                )
                                                                
                                                                // Row with Type and Comment
                                                                val commentText = iface.comment?.takeIf { it.isNotBlank() }
                                                                val typeLabel = iface.type?.uppercase() ?: "DESCONOCIDO"
                                                                Text(
                                                                    text = if (commentText != null) "$typeLabel • $commentText" else typeLabel,
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(8.dp))

                                                            // Status Badge / Indicators
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                if (iface.disabled == true) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(6.dp))
                                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            "DIS",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = MaterialTheme.colorScheme.onErrorContainer
                                                                        )
                                                                    }
                                                                } else {
                                                                    // Running indicator
                                                                    val isRunning = iface.running == true
                                                                    val statusText = if (isRunning) "RUN" else "IDLE"
                                                                    val statusBg = if (isRunning) GreenBadgeBg else MaterialTheme.colorScheme.surfaceVariant
                                                                    val statusColor = if (isRunning) GreenBadgeText else MaterialTheme.colorScheme.onSurfaceVariant
                                                                    
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .background(statusBg, shape = RoundedCornerShape(6.dp))
                                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Text(
                                                                            statusText,
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = statusColor
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

private fun formatBitsToHuman(bits: Double): String {
    return when {
        bits >= 1_000_000_000 -> String.format(java.util.Locale.US, "%.2f Gbps", bits / 1_000_000_000.0)
        bits >= 1_000_000 -> String.format(java.util.Locale.US, "%.2f Mbps", bits / 1_000_000.0)
        bits >= 1_000 -> String.format(java.util.Locale.US, "%.2f Kbps", bits / 1_000.0)
        else -> String.format(java.util.Locale.US, "%.2f bps", bits)
    }
}

