package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalConsumptionScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mikrotiksState by viewModel.mikrotiksState.collectAsState()

    // Real data state
    var isLoaded by remember { mutableStateOf(false) }
    var realDownMbps by remember { mutableFloatStateOf(0f) }
    var realUpMbps by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadMikrotiks()
        viewModel.getConsumoClienteRemote("1") { state ->
            when (state) {
                is UiState.Success -> {
                    val dataList = state.data.data
                    if (!dataList.isNullOrEmpty() && dataList.size > 1) {
                        try {
                            var maxDown = 0f
                            var maxUp = 0f
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            
                            val sortedData = dataList.sortedBy { it.fecha }
                            for (i in 1 until sortedData.size) {
                                val prev = sortedData[i - 1]
                                val curr = sortedData[i]
                                
                                val downDiff = (curr.downloadBytes ?: 0) - (prev.downloadBytes ?: 0)
                                val upDiff = (curr.uploadBytes ?: 0) - (prev.uploadBytes ?: 0)
                                
                                val safeDown = if (downDiff < 0) 0L else downDiff
                                val safeUp = if (upDiff < 0) 0L else upDiff
                                
                                val prevTime = prev.fecha?.let { sdf.parse(it)?.time } ?: 0L
                                val currTime = curr.fecha?.let { sdf.parse(it)?.time } ?: 0L
                                
                                var seconds = (currTime - prevTime) / 1000L
                                if (seconds <= 0) seconds = 300L
                                
                                val downMbps = (safeDown * 8f) / (seconds * 1_000_000f)
                                val upMbps = (safeUp * 8f) / (seconds * 1_000_000f)
                                
                                if (downMbps > maxDown) maxDown = downMbps
                                if (upMbps > maxUp) maxUp = upMbps
                            }
                            
                            realDownMbps = maxDown
                            realUpMbps = maxUp
                        } catch (e: Exception) {
                            errorMessage = "Error al calcular picos: ${e.message}"
                        }
                    }
                    isLoaded = true
                }
                is UiState.Error -> {
                    errorMessage = state.message
                    isLoaded = true
                }
                is UiState.Loading -> {
                    isLoaded = false
                }
            }
        }
    }

    val globalDownMbps by animateFloatAsState(
        targetValue = if (isLoaded) realDownMbps else 0f, 
        animationSpec = tween(1500)
    )
    val globalUpMbps by animateFloatAsState(
        targetValue = if (isLoaded) realUpMbps else 0f, 
        animationSpec = tween(1500)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Main Gauge Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tráfico Total de Red", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Pico de consumo 24hrs de clientes", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        // Download Gauge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SpeedometerGauge(percentage = (globalDownMbps / 1000f).coerceIn(0f, 1f), color = MaterialTheme.colorScheme.primary, value = globalDownMbps)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bajada (Mbps)", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        // Upload Gauge
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SpeedometerGauge(percentage = (globalUpMbps / 500f).coerceIn(0f, 1f), color = MaterialTheme.colorScheme.secondary, value = globalUpMbps)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Upload, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Subida (Mbps)", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // Mikrotik Nodes Status
            Text("Nodos Principales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            when (val state = mikrotiksState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Success -> {
                    val routers = state.data.data ?: emptyList()
                    if (routers.isEmpty()) {
                        Text("No hay Mikrotiks configurados en el sistema.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    } else {
                        routers.forEach { router ->
                            val status = router.isOnline == true || router.activo == "1"
                            NodeItemCard(
                                name = router.nombre ?: "Router Desconocido",
                                ip = router.ipAddress ?: "0.0.0.0",
                                clientes = router.numeroClientes ?: "0",
                                status = status
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SpeedometerGauge(percentage: Float, color: Color, value: Float, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val sweepAngle = 240f
            val startAngle = 150f
            val strokeWidth = 14.dp.toPx()
            
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
                sweepAngle = sweepAngle * percentage,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                size = Size(size.width, size.height)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = String.format("%.1f", value), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun NodeItemCard(name: String, ip: String, clientes: String, status: Boolean) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(if (status) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Router,
                    contentDescription = null,
                    tint = if (status) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(ip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
            if (status) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("$clientes Clientes", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("ONLINE", style = MaterialTheme.typography.labelSmall, color = GreenBadgeText)
                }
            } else {
                Text("OFFLINE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = RedBadgeText)
            }
        }
    }
}
