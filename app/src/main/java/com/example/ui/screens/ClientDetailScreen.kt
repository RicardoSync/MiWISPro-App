package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    client: Client,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var detailState by remember(client.id) { mutableStateOf<UiState<Client>>(UiState.Loading) }

    // Fetch fresh details with full speed specs from real-time API upon screen load
    LaunchedEffect(client.id) {
        viewModel.getClientDetailRemote(client.id) { state ->
            detailState = state
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Perfil de Cliente",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when (val state = detailState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Descargando información en tiempo real...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                state.message,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    viewModel.getClientDetailRemote(client.id) { s ->
                                        detailState = s
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reintentar Sincronización")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val fetchedClient = state.data
                    val isActive = fetchedClient.activo == 1

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        item {
                            // Dynamic Customer Profile Header Banner with beautiful layout and status
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isActive) GreenBadgeBg else RedBadgeBg
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = (if (isActive) GreenBadgeText else RedBadgeText).copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(99.dp))
                                                .background(if (isActive) GreenBadgeText.copy(alpha = 0.1f) else RedBadgeText.copy(alpha = 0.1f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(if (isActive) GreenBadgeText else RedBadgeText, CircleShape)
                                                )
                                                Text(
                                                    text = if (isActive) "CONEXIÓN ACTIVA" else "CONEXIÓN SUSPENDIDA",
                                                    color = if (isActive) GreenBadgeText else RedBadgeText,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Black
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.surface, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Person,
                                                contentDescription = null,
                                                tint = if (isActive) GreenBadgeText else RedBadgeText,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    Text(
                                        text = fetchedClient.nombreCompleto?.uppercase() ?: "SIN NOMBRE REGISTRADO",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isActive) GreenBadgeText else RedBadgeText,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Block 1: Connection configurations
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.SettingConnection,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Credenciales del Router & MikroTik",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    DetailPropertyRow("Tipo de Conexión", fetchedClient.tipoConexion?.uppercase() ?: "ESTÁTICA")
                                    if (fetchedClient.tipoConexion?.contains("pppoe", ignoreCase = true) == true) {
                                        DetailPropertyRow("Usuario PPPoE", fetchedClient.pppoeUsuario ?: "-")
                                        DetailPropertyRow("Contraseña PPPoE", fetchedClient.pppoePassword ?: "-")
                                    }
                                    DetailPropertyRow("Segmento / Gateway IP", fetchedClient.ipAddress ?: fetchedClient.ipCliente ?: "-")
                                    DetailPropertyRow("Puerto de Comunicación", fetchedClient.puertoApi?.toString() ?: "8729")
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    DetailPropertyRow("Velocidad de Bajada", fetchedClient.velocidadBajada ?: "Sin Configurar")
                                    DetailPropertyRow("Velocidad de Subida", fetchedClient.velocidadSubida ?: "Sin Configurar")
                                    DetailPropertyRow("Límite de Ráfaga", fetchedClient.burstLimit ?: "0/0")
                                }
                            }
                        }

                        // Block 2: Fiscal and Comercial Detail
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Payments,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Paquetes de Internet & Cobros",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val currentDues = fetchedClient.saldoActual?.toDoubleOrNull() ?: 0.0
                                    DetailPropertyRow("Monto por Cobrar", "$${fetchedClient.saldoActual ?: "0.00"} MXN", isHighlighted = currentDues > 0)
                                    DetailPropertyRow("Día de Corte Mensual", "Día ${fetchedClient.diaCorte ?: "30"} de cada mes")
                                    DetailPropertyRow("Siguiente Fecha de Vencimiento", fetchedClient.proximoPago ?: "-")
                                    
                                    if (!fetchedClient.promesaPagoHasta.isNullOrEmpty()) {
                                        DetailPropertyRow("Fecha Límite Pago Promesa", fetchedClient.promesaPagoHasta, isWarning = true)
                                    }

                                    DetailPropertyRow("Paquete Base", fetchedClient.nombrePaquete ?: "-")
                                    DetailPropertyRow("Precio del Plan", "$${fetchedClient.precioPaquete ?: "0"} MXN")
                                    
                                    if (fetchedClient.nombreServicio != null) {
                                        DetailPropertyRow("Servicios Adicionales", "${fetchedClient.nombreServicio} ($${fetchedClient.precioServicio ?: "0"} MXN)")
                                    }
                                }
                            }
                        }

                        // Block 3: Personal identity details
                        item {
                            Card(
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Rounded.Badge,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Información del Titular",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    DetailPropertyRow("ID de Registro", fetchedClient.id)
                                    DetailPropertyRow("DNI / RFC", fetchedClient.dniRfc ?: "SIN_RFC")
                                    DetailPropertyRow("Teléfono", fetchedClient.telefono ?: "Sin teléfono")
                                    DetailPropertyRow("Dirección Física", fetchedClient.direccion ?: "Sin especificar")
                                    DetailPropertyRow("Filtro Contenidos", if (fetchedClient.filtroAdultos == 1) "ACTIVO" else "DESACTIVADO")
                                }
                            }
                        }

                        // GPS Maps Launcher Button
                        if (!fetchedClient.coordenadas.isNullOrEmpty()) {
                            item {
                                Button(
                                    onClick = { openGoogleMapsFromDetail(context, fetchedClient.coordenadas, fetchedClient.nombreCompleto) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = BlueBadgeBg,
                                        contentColor = BlueBadgeText
                                    ),
                                    border = BorderStroke(1.dp, BlueBadgeText.copy(alpha = 0.2f)),
                                    elevation = ButtonDefaults.buttonElevation(0.dp)
                                ) {
                                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Ubicar en Google Maps", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }

                        // Core Toggle Status Button (Keeps suspension or activation toggler live)
                        item {
                            Button(
                                onClick = {
                                    viewModel.toggleClientStatusRemote(fetchedClient) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            // Update details instantly on screen
                                            viewModel.getClientDetailRemote(fetchedClient.id) { stateResult ->
                                                detailState = stateResult
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("detail_action_toggle_${fetchedClient.id}"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive) RedBadgeBg else GreenBadgeBg,
                                    contentColor = if (isActive) RedBadgeText else GreenBadgeText
                                ),
                                border = BorderStroke(1.dp, (if (isActive) RedBadgeText else GreenBadgeText).copy(alpha = 0.3f)),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActive) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isActive) "SUSPENDER CONEXIÓN" else "ACTIVAR CONEXIÓN",
                                    fontWeight = FontWeight.Black,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPropertyRow(label: String, value: String, isHighlighted: Boolean = false, isWarning: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isHighlighted -> RedBadgeText
                isWarning -> OrangeBadgeText
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

private fun openGoogleMapsFromDetail(context: Context, coordenadas: String, nombre: String?) {
    try {
        val geoUri = "geo:$coordenadas?q=$coordenadas(${Uri.encode(nombre ?: "Cliente")})"
        val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            val webUri = "https://www.google.com/maps/search/?api=1&query=$coordenadas"
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Imposible abrir mapas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

// Icon fallbacks specifically for router settings or payment indicators
private val Icons.Rounded.SettingConnection: androidx.compose.ui.graphics.vector.ImageVector
    get() = Icons.Rounded.SettingsInputComponent
