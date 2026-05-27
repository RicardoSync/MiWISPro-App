package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.example.data.DhcpLease
import com.example.data.MikrotikDhcpLeasesResponse
import com.example.data.MakeLeaseStaticResponse
import com.example.ui.theme.GreenBadgeBg
import com.example.ui.theme.GreenBadgeText
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MikrotikDhcpLeasesScreen(
    routerId: String,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var leasesState by remember(routerId) { mutableStateOf<UiState<MikrotikDhcpLeasesResponse>>(UiState.Loading) }
    var makeStaticState by remember { mutableStateOf<UiState<MakeLeaseStaticResponse>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current

    val refreshLeases = {
        viewModel.getMikrotikDhcpLeasesRemote(routerId) { state ->
            leasesState = state
        }
    }

    LaunchedEffect(routerId) {
        refreshLeases()
    }

    // Loading overlay dialog for make-static action
    if (makeStaticState is UiState.Loading) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
                    Text("Procesando...", style = MaterialTheme.typography.titleMedium)
                }
            },
            text = { Text("Convirtiendo dirección IP a lease estático en el router MikroTik. Por favor espere...") },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Error dialog for make-static action
    if (makeStaticState is UiState.Error) {
        val errorMessage = (makeStaticState as UiState.Error).message
        AlertDialog(
            onDismissRequest = { makeStaticState = null },
            confirmButton = {
                TextButton(onClick = { makeStaticState = null }) {
                    Text("Cerrar")
                }
            },
            icon = {
                Icon(
                    Icons.Rounded.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = { Text("Error al Convertir", fontWeight = FontWeight.Bold) },
            text = { Text(errorMessage) },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Success dialog for make-static action showing details and "Agregar Cliente" action
    if (makeStaticState is UiState.Success) {
        val response = (makeStaticState as UiState.Success<MakeLeaseStaticResponse>).data
        val lease = response.data
        AlertDialog(
            onDismissRequest = {
                makeStaticState = null
                refreshLeases()
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = {
                            makeStaticState = null
                            refreshLeases()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }
                    Button(
                        onClick = {
                            val name = lease?.hostName ?: lease?.address ?: ""
                            val ip = lease?.address ?: ""
                            makeStaticState = null
                            // Navigate directly to registration screen pre-populated!
                            viewModel.openRegistrarClienteWithData(name, ip)
                        },
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Rounded.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Agregar Cliente", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            },
            icon = {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = GreenBadgeText,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    "¡Conversión Exitosa!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = response.message ?: "El lease se convirtió a estático correctamente.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (lease != null) {
                        OutlinedCard(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Hostname:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(lease.hostName ?: "N/D", style = MaterialTheme.typography.bodyMedium)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Dirección IP:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(lease.address ?: "N/D", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Dirección MAC:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(lease.macAddress ?: "N/D", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Servidor DHCP:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text(lease.server ?: "N/D", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    Text(
                        text = "¿Deseas registrar este dispositivo como un cliente activo en el sistema ahora mismo?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Leases DHCP", fontWeight = FontWeight.Bold)
                        val currentLeasesState = leasesState
                        if (currentLeasesState is UiState.Success) {
                            Text(
                                text = currentLeasesState.data.nombre ?: "MikroTik Router",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { refreshLeases() },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Sincronizar Leases",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar por Hostname, IP...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Buscar") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("dhcp_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                AnimatedContent(
                    targetState = leasesState,
                    transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                    label = "dhcp_leases_state_transition",
                    modifier = Modifier.weight(1.0f).fillMaxWidth()
                ) { state ->
                    when (state) {
                        is UiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        is UiState.Error -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Rounded.CloudOff,
                                        contentDescription = "Error",
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { refreshLeases() },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Reintentar")
                                    }
                                }
                            }
                        }
                        is UiState.Success -> {
                            val leasesList = state.data.data ?: emptyList()
                            val filteredLeases = remember(leasesList, searchQuery) {
                                leasesList.filter { lease ->
                                    val nameMatch = lease.hostName?.contains(searchQuery, ignoreCase = true) ?: false
                                    val addressMatch = lease.address?.contains(searchQuery, ignoreCase = true) ?: false
                                    searchQuery.isEmpty() || nameMatch || addressMatch
                                }
                            }

                            if (filteredLeases.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.SearchOff,
                                            contentDescription = "Ningún lease",
                                            modifier = Modifier.size(72.dp),
                                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            "Ningún lease DHCP coincide",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Prueba con otro texto de búsqueda",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            } else {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Total counter badge
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Leases encontrados: ${filteredLeases.size}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    LazyColumn(
                                        modifier = Modifier
                                            .weight(1.0f)
                                            .fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        items(filteredLeases, key = { it.id ?: "" }) { lease ->
                                            DhcpLeaseCard(
                                                lease = lease,
                                                onMakeStatic = { targetLease ->
                                                    viewModel.makeLeaseStaticRemote(routerId, targetLease.address ?: "") { result ->
                                                        makeStaticState = result
                                                    }
                                                }
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

@Composable
fun DhcpLeaseCard(
    lease: DhcpLease,
    onMakeStatic: (DhcpLease) -> Unit
) {
    val isBound = lease.status?.lowercase() == "bound"
    val isWaiting = lease.status?.lowercase() == "waiting"
    
    val statusBg = when {
        isBound -> GreenBadgeBg
        isWaiting -> Color(0xFFFFF3E0) // Light orange
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val statusColor = when {
        isBound -> GreenBadgeText
        isWaiting -> Color(0xFFE65100) // Deep orange
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with Name and Status Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1.0f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getDeviceIcon(lease.hostName),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = lease.hostName?.takeIf { it.isNotBlank() } ?: "Dispositivo Desconocido",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Servidor: ${lease.server ?: "N/D"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Status chip
                Box(
                    modifier = Modifier
                        .background(statusBg, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = lease.status?.uppercase() ?: "UNKNOWN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            // Body info (IP and MAC)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dirección IP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lease.address ?: "N/D",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Dirección MAC",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = lease.macAddress ?: "N/D",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Chips: static vs dynamic, enabled vs disabled, last seen
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dynamic vs Static Chip
                val isDynamic = lease.dynamic == true
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = if (isDynamic) "Dinámico" else "Estático",
                            style = MaterialTheme.typography.labelSmall
                        ) 
                    },
                    modifier = Modifier.height(26.dp),
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (isDynamic) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) 
                                         else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        labelColor = if (isDynamic) MaterialTheme.colorScheme.onSecondaryContainer 
                                     else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    border = null
                )

                // Disabled Chip
                val isDisabled = lease.disabled == true
                if (isDisabled) {
                    AssistChip(
                        onClick = { },
                        label = { Text("Deshabilitado", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(26.dp),
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = null
                    )
                }

                // Last Seen Info
                val lastSeenText = lease.lastSeen?.takeIf { it.isNotBlank() }
                if (lastSeenText != null) {
                    Spacer(modifier = Modifier.weight(1f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = lastSeenText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Convert to static button for Dynamic Leases
            if (lease.dynamic == true) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { onMakeStatic(lease) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.OfflineShare,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hacer Estático", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun getDeviceIcon(hostName: String?): androidx.compose.ui.graphics.vector.ImageVector {
    val name = hostName?.lowercase() ?: ""
    return when {
        name.contains("pixel") || name.contains("phone") || name.contains("oppo") || name.contains("samsung") || name.contains("iphone") || name.contains("android") || name.contains("redmi") || name.contains("xiaomi") || name.contains("huawei") || name.contains("moto") -> Icons.Rounded.Smartphone
        name.contains("pc") || name.contains("desktop") || name.contains("laptop") || name.contains("msi") || name.contains("macbook") || name.contains("computer") -> Icons.Rounded.Laptop
        name.contains("ap") || name.contains("server") || name.contains("mikrotik") || name.contains("router") || name.contains("sw") -> Icons.Rounded.Router
        name.contains("tv") || name.contains("smart") || name.contains("box") -> Icons.Rounded.Tv
        name.contains("camera") || name.contains("cam") -> Icons.Rounded.Videocam
        name.contains("printer") || name.contains("print") -> Icons.Rounded.Print
        else -> Icons.Rounded.Devices
    }
}
