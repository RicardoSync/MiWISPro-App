package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Client
import com.example.data.ConsumoClienteResponse
import com.example.data.DeudaResponse
import com.example.data.FacturaDetalle
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState
import kotlin.math.max

enum class ClientTab { INFO, CONSUMO, DEUDAS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    client: Client,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(ClientTab.INFO) }

    var detailState by remember(client.id) { mutableStateOf<UiState<Client>>(UiState.Loading) }
    var deudaState by remember(client.id) { mutableStateOf<UiState<DeudaResponse>>(UiState.Loading) }
    var consumoState by remember(client.id) { mutableStateOf<UiState<ConsumoClienteResponse>>(UiState.Loading) }

    // Fetch details
    LaunchedEffect(client.id) {
        viewModel.getClientDetailRemote(client.id) { state -> detailState = state }
        viewModel.getConsumoClienteRemote(client.id) { state -> consumoState = state }
        viewModel.getClientDeudaRemote(client.id) { state -> deudaState = state }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perfil del Cliente", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        Toast.makeText(context, "Sincronizando...", Toast.LENGTH_SHORT).show()
                        viewModel.syncMikrotikRemote(client.id) { success, message, detail ->
                            val text = if (detail != null) "$message\n($detail)" else message
                            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(Icons.Rounded.Sync, contentDescription = "Sincronizar MikroTik")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()) // Remove bottom padding for full scroll
        ) {
            // Elegant Tab Row
            PrimaryTabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                listOf(ClientTab.INFO to "General", ClientTab.CONSUMO to "Internet", ClientTab.DEUDAS to "Deudas").forEach { (tab, title) ->
                    Tab(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        text = {
                            Text(
                                title,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            // Crossfade with Spinner for smooth transitions between tabs and loading states
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(durationMillis = 300),
                label = "tab_transition",
                modifier = Modifier.fillMaxSize()
            ) { selectedTab ->
                when (selectedTab) {
                    ClientTab.INFO -> AnimatedTabContent(detailState) { data ->
                        ClientInfoTabContent(data, viewModel, context)
                    }
                    ClientTab.CONSUMO -> AnimatedTabContent(consumoState) { data ->
                        ClientConsumoTabContent(data)
                    }
                    ClientTab.DEUDAS -> AnimatedTabContent(deudaState) { data ->
                        ClientDeudasTabContent(client, data, context)
                    }
                }
            }
        }
    }
}

/**
 * Helper to wrap UiState handling with a smooth Spinner
 */
@Composable
fun <T> AnimatedTabContent(
    state: UiState<T>,
    onSuccess: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
        label = "state_transition",
        modifier = Modifier.fillMaxSize()
    ) { targetState ->
        when (targetState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando información...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(targetState.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
                    }
                }
            }
            is UiState.Success -> {
                onSuccess(targetState.data)
            }
        }
    }
}

// -----------------------------------------------------------------------------------
// TAB 1: INFO GENERAL (PROFILE REDESIGN)
// -----------------------------------------------------------------------------------

@Composable
fun ClientInfoTabContent(
    client: Client,
    viewModel: ClientViewModel,
    context: Context
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showCrearFacturaDialog by remember { mutableStateOf(false) }
    val isActive = client.estado != "cortado" && client.estado != "Cancelado"

    if (showConfirmDialog) {
        val actionText = if (isActive) "Suspender" else "Activar"
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Acción") },
            text = { Text("¿Estás seguro de que deseas $actionText al cliente ${client.nombreCompleto}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleClientStatusRemote(client) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                    showConfirmDialog = false
                }) { Text("Confirmar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showCrearFacturaDialog) {
        AlertDialog(
            onDismissRequest = { showCrearFacturaDialog = false },
            title = { Text("Generar Factura Manual") },
            text = { Text("¿Deseas generar una factura manual para este cliente?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.crearFacturaManual(client.id) { success, message, data ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        if (success && data != null) {
                            // Map CrearFacturaData to FacturaData for the detail screen
                            val facturaData = com.example.data.FacturaData(
                                id = data.idFactura.toString(),
                                idCliente = client.id,
                                idPaquete = null,
                                idServicioExtra = null,
                                montoTotal = data.monto.toString(),
                                montoPagado = "0.00",
                                saldoPendiente = data.monto.toString(),
                                fechaEmision = data.fechaEmision,
                                fechaVencimiento = data.fechaVencimiento,
                                estado = "pendiente",
                                descripcion = data.descripcion,
                                fechaCreacion = data.fechaEmision, // approximation
                                nombreCliente = client.nombreCompleto,
                                nombrePlan = client.nombrePaquete
                            )
                            viewModel.openFacturaDetail(facturaData, isNew = true)
                        }
                    }
                    showCrearFacturaDialog = false
                }) { Text("Generar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { showCrearFacturaDialog = false }) { Text("Cancelar") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // --- PERSONAL INFO GRID ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Información Personal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Nombre Completo") },
                            supportingContent = { Text(client.nombreCompleto?.uppercase() ?: "SIN NOMBRE", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface) },
                            leadingContent = { Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Estado") },
                            supportingContent = { Text(if (isActive) "Activo" else "Suspendido") },
                            leadingContent = { Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Teléfono") },
                            supportingContent = { Text(client.telefono ?: "N/D") },
                            leadingContent = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("RFC / DNI") },
                            supportingContent = { Text(client.dniRfc ?: "N/D") },
                            leadingContent = { Icon(Icons.Rounded.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }

        // --- DYNAMIC SECTIONS (SETTINGS MENU) ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Configuraciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))

                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("IP del Cliente") },
                            supportingContent = { Text(client.ipCliente ?: "IP Dinámica") },
                            leadingContent = { Icon(Icons.Rounded.Language, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("MikroTik Enlazado") },
                            supportingContent = { Text("${client.nombreMikrotik ?: "MikroTik"} • ${client.ipAddress ?: "Sin IP"}") },
                            leadingContent = { Icon(Icons.Rounded.Router, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text("Ubicación en Mapa") },
                            supportingContent = { Text(client.direccion ?: "Sin especificar") },
                            leadingContent = { Icon(Icons.Rounded.Place, contentDescription = null) },
                            trailingContent = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) },
                            modifier = Modifier.clickable {
                                if (!client.coordenadas.isNullOrEmpty()) openGoogleMaps(context, client.coordenadas, client.nombreCompleto)
                                else Toast.makeText(context, "Sin coordenadas guardadas", Toast.LENGTH_SHORT).show()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        if (client.tipoConexion?.contains("pppoe", ignoreCase = true) == true) {
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            ListItem(
                                headlineContent = { Text("Credenciales PPPoE") },
                                supportingContent = { Text("Usuario: ${client.pppoeUsuario ?: "-"}") },
                                leadingContent = { Icon(Icons.Rounded.VpnKey, contentDescription = null) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
            }
        }

        // --- SETTINGS ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Acciones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = if (isActive) MaterialTheme.colorScheme.errorContainer.copy(alpha=0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.5f)),
                    border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text(if (isActive) "Suspender Servicio" else "Reactivar Servicio", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Cambiar estado de conexión") },
                        leadingContent = { Icon(if (isActive) Icons.Rounded.Block else Icons.Rounded.CheckCircle, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) },
                        modifier = Modifier.clickable { showConfirmDialog = true },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            supportingColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            leadingIconColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                            trailingIconColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha=0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Generar Factura Manual", fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("Crear un recibo o premisa nueva") },
                        leadingContent = { Icon(Icons.Rounded.Receipt, contentDescription = null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null) },
                        modifier = Modifier.clickable { showCrearFacturaDialog = true },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent,
                            headlineColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            supportingColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                            leadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Text(value.uppercase(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
    }
}



private fun openGoogleMaps(context: Context, coordenadas: String, nombre: String?) {
    try {
        val geoUri = "geo:$coordenadas?q=$coordenadas(${Uri.encode(nombre ?: "Cliente")})"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) context.startActivity(mapIntent)
        else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$coordenadas")))
    } catch (e: Exception) {
        Toast.makeText(context, "Imposible abrir mapas", Toast.LENGTH_SHORT).show()
    }
}

// -----------------------------------------------------------------------------------
// TAB 2: CONSUMO (BAR CHART & RING)
// -----------------------------------------------------------------------------------

data class ConsumoIntervalo(
    val fecha: String,
    val downMbps: Float,
    val upMbps: Float,
    val downBytes: Long,
    val upBytes: Long
)

@Composable
fun ClientConsumoTabContent(
    response: ConsumoClienteResponse
) {
    val history = response.data ?: emptyList()
    
    Column(modifier = Modifier.fillMaxSize()) {
        if (history.size < 2) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("Datos insuficientes para calcular velocidad.", color = MaterialTheme.colorScheme.secondary)
            }
            return
        }

        // --- CÁLCULO DE MBPS ---
        val dataPoints = history.sortedBy { it.fecha }
        val intervalData = mutableListOf<ConsumoIntervalo>()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

        for (i in 1 until dataPoints.size) {
            val prev = dataPoints[i - 1]
            val curr = dataPoints[i]

            val downDiff = (curr.downloadBytes ?: 0) - (prev.downloadBytes ?: 0)
            val upDiff = (curr.uploadBytes ?: 0) - (prev.uploadBytes ?: 0)

            val safeDownDiff = if (downDiff < 0) 0L else downDiff
            val safeUpDiff = if (upDiff < 0) 0L else upDiff

            val prevTime = sdf.parse(prev.fecha ?: "")?.time ?: 0L
            val currTime = sdf.parse(curr.fecha ?: "")?.time ?: 0L
            
            var seconds = (currTime - prevTime) / 1000
            if (seconds <= 0) seconds = 300 // default 5 min
            
            val downMbps = (safeDownDiff * 8f) / (seconds * 1_000_000f)
            val upMbps = (safeUpDiff * 8f) / (seconds * 1_000_000f)

            intervalData.add(ConsumoIntervalo(curr.fecha ?: "", downMbps, upMbps, safeDownDiff, safeUpDiff))
        }

        val maxSpeed = max(1f, intervalData.maxOfOrNull { it.downMbps } ?: 1f)
        val currentSpeed = intervalData.lastOrNull()?.downMbps ?: 0f
        // Calculamos el pico para mostrarlo en lugar del promedio aritmético bajo
        val displaySpeed = maxSpeed
        val scorePercent = (displaySpeed / maxSpeed).coerceIn(0f, 1f)

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // --- RING PROGRESS INDICATOR (DIAL) ---
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Pico Registrado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                            CircularProgressIndicator(
                                progress = { 1f },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                strokeWidth = 16.dp,
                                strokeCap = StrokeCap.Round
                            )
                            CircularProgressIndicator(
                                progress = { scorePercent },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 16.dp,
                                strokeCap = StrokeCap.Round
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(String.format("%.1f", displaySpeed), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                                Text("Mbps", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // --- HISTORIAL DE CONSUMO (PART-TO-WHOLE BAR) ---
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Historial de Consumo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Saturación de Ancho de Banda", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { scorePercent },
                            modifier = Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(12.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Consumo Actual", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                Text(String.format("%.1f Mbps", displaySpeed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Límite del Periodo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                Text(String.format("%.1f Mbps", maxSpeed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // --- LISTADO DE DETALLE (RECYCLERVIEW) ---
            item {
                Text("Detalle de Tráfico", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top=16.dp))
            }
            
            items(intervalData.reversed()) { item ->
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha=0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDate(item.fecha), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            val totalBytes = item.downBytes + item.upBytes
                            Text(formatBytes(totalBytes), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            // Download
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.ArrowDownward, contentDescription = null, tint = GreenBadgeText, modifier = Modifier.size(16.dp))
                                Text(formatBytes(item.downBytes), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GreenBadgeText)
                            }
                            // Upload
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, tint = BlueBadgeText, modifier = Modifier.size(16.dp))
                                Text(formatBytes(item.upBytes), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = BlueBadgeText)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helpers
private fun formatBytes(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun formatDate(dateStr: String): String {
    try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val date = sdf.parse(dateStr) ?: return dateStr
        val outSdf = java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
        return outSdf.format(date)
    } catch (e: Exception) {
        return dateStr
    }
}

// -----------------------------------------------------------------------------------
// TAB 3: ESTADO DE CUENTA
// -----------------------------------------------------------------------------------

@Composable
fun ClientDeudasTabContent(client: Client, data: DeudaResponse, context: Context) {
    val deudaData = data.data
    if (deudaData == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("¡Sin Deudas Pendientes!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        }
        return
    }

    val totalAPagar = deudaData.totalAPagar ?: "0.00"
    val isMoroso = deudaData.estadoServicio?.contains("moroso", ignoreCase = true) == true || (totalAPagar.toDoubleOrNull() ?: 0.0) > 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = if (isMoroso) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "TOTAL A PAGAR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isMoroso) MaterialTheme.colorScheme.onErrorContainer.copy(alpha=0.8f) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha=0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$$totalAPagar",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Black,
                        color = if (isMoroso) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        val bills = deudaData.detalleFacturas ?: emptyList()
        val sortedBills = bills.sortedByDescending { it.idFactura ?: 0 }
        if (sortedBills.isNotEmpty()) {
            item {
                Text("Historial de Facturas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top=16.dp))
            }
            items(sortedBills) { bill ->
                val estado = bill.estado?.lowercase() ?: "pendiente"
                
                val cardColor = when {
                    estado.contains("pagado") -> Color(0xFFE8F5E9)
                    estado.contains("pendiente") -> Color(0xFFFFF3E0)
                    estado.contains("vencida") -> MaterialTheme.colorScheme.errorContainer
                    estado.contains("anulada") -> Color(0xFFF5F5F5)
                    else -> MaterialTheme.colorScheme.surface
                }
                
                val textColor = when {
                    estado.contains("pagado") -> Color(0xFF2E7D32)
                    estado.contains("pendiente") -> Color(0xFFEF6C00)
                    estado.contains("vencida") -> MaterialTheme.colorScheme.error
                    estado.contains("anulada") -> Color.Gray
                    else -> MaterialTheme.colorScheme.primary
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    border = BorderStroke(1.dp, textColor.copy(alpha=0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(bill.descripcion ?: "Internet", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(textColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    (bill.estado ?: "Pendiente").uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Event, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                Text("Vence: ${bill.fechaVencimiento ?: "-"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            Text("$${bill.montoPendiente ?: "0.00"}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Black, color = textColor)
                        }
                    }
                }
            }
        }
    }
}
