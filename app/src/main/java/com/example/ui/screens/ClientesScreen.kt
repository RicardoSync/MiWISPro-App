package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Client
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun ClientesScreen(
    viewModel: ClientViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val clientsList by viewModel.clientsList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val itemsPerPage by viewModel.itemsPerPage.collectAsState()
    
    var clientToToggle by remember { mutableStateOf<Client?>(null) }
    
    clientToToggle?.let { client ->
        val isSuspended = client.estado == "cortado" || client.estado == "Cancelado"
        val actionText = if (isSuspended) "Activar" else "Suspender"
        AlertDialog(
            onDismissRequest = { clientToToggle = null },
            title = { Text("Confirmar Acción") },
            text = { Text("¿Estás seguro de que deseas $actionText al cliente ${client.nombreCompleto}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleClientStatusRemote(client) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                    clientToToggle = null
                }) { Text("Confirmar", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = { clientToToggle = null }) { Text("Cancelar") }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Buscar por nombre, teléfono...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Buscar") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .testTag("search_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

        Spacer(modifier = Modifier.height(6.dp))

        // Main client list contents
        AnimatedContent(
            targetState = uiState,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
            label = "clientes_state_transition",
            modifier = Modifier.weight(1.0f).fillMaxWidth()
        ) { state ->
            when (state) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
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
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.loadClientes() },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Reintentar Sincronización")
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    val filteredClients = remember(clientsList, searchQuery) {
                        clientsList.filter { client ->
                            // Keyword fields match
                            val nameMatch = client.nombreCompleto?.contains(searchQuery, ignoreCase = true) ?: false
                            val phoneMatch = client.telefono?.contains(searchQuery, ignoreCase = true) ?: false
                            val ipMatch = client.ipCliente?.contains(searchQuery, ignoreCase = true) ?: false
                            searchQuery.isEmpty() || nameMatch || phoneMatch || ipMatch
                        }
                    }

                    if (filteredClients.isEmpty()) {
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
                                    contentDescription = "Ningún cliente",
                                    modifier = Modifier.size(72.dp),
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    "Ningún cliente coincide",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Prueba cambiando los filtros o el texto ingresado",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Pagination Computations cached to prevent scroll jank
                            val totalItems = filteredClients.size
                            val totalPages = maxOf(1, kotlin.math.ceil(totalItems.toDouble() / itemsPerPage).toInt())
                            val safeCurrentPage = minOf(currentPage, totalPages)
                            val paginatedList = remember(filteredClients, safeCurrentPage, itemsPerPage) {
                                val startIndex = (safeCurrentPage - 1) * itemsPerPage
                                val endIndex = minOf(startIndex + itemsPerPage, totalItems)
                                filteredClients.subList(startIndex, endIndex)
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1.0f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(paginatedList, key = { it.id }) { client ->
                                    ClientCard(
                                        client = client,
                                        onDetail = { viewModel.selectClientForDetail(client) },
                                        onPago = { viewModel.selectClientForPago(client) },
                                        onToggleStatus = { clientToToggle = client }
                                    )
                                }
                            }

                            // Bottom Pagination Capsule Controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "$safeCurrentPage de $totalPages",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${totalItems} clientes)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledIconButton(
                                        onClick = { if (safeCurrentPage > 1) viewModel.setCurrentPage(safeCurrentPage - 1) },
                                        enabled = safeCurrentPage > 1,
                                        modifier = Modifier.size(36.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowBack,
                                            contentDescription = "Prev",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = { if (safeCurrentPage < totalPages) viewModel.setCurrentPage(safeCurrentPage + 1) },
                                        enabled = safeCurrentPage < totalPages,
                                        modifier = Modifier.size(36.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.ArrowForward,
                                            contentDescription = "Next",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
        } // Closes Column

        FloatingActionButton(
            onClick = { viewModel.openRegistrarCliente(true) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = "Registrar Cliente")
        }
    } // Closes Box
} // Closes ClientesScreen

@Composable
fun ClientCard(
    client: Client,
    onDetail: () -> Unit,
    onPago: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val estadoInt = client.estado?.toIntOrNull() ?: if (client.activo == 1) 1 else 3
    val isActive = estadoInt == 1
    val isAdeudo = estadoInt == 2
    val isSuspendido = estadoInt == 3

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with name and status indicator badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val iconColor = when {
                    isAdeudo -> Color(0xFFEF6C00)
                    isSuspendido -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                val iconContainerColor = when {
                    isAdeudo -> Color(0xFFFFF3E0)
                    isSuspendido -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconContainerColor, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = iconColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1.0f).padding(end = 8.dp)) {
                    Text(
                        text = client.nombreCompleto?.uppercase() ?: "CLIENTE SIN NOMBRE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${client.telefono ?: "Sin Teléfono"} • ${if (client.ipCliente.isNullOrEmpty()) "IP Dinámica" else client.ipCliente}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val stateLabel = when {
                    isAdeudo -> "ADEUDO"
                    isSuspendido -> "SUSPENDIDO"
                    else -> "ACTIVO"
                }
                
                val stateColor = when {
                    isAdeudo -> Color(0xFFEF6C00) // Orange
                    isSuspendido -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                
                val stateContainerColor = when {
                    isAdeudo -> Color(0xFFFFF3E0) // Light Orange
                    isSuspendido -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                
                val stateOnContainerColor = when {
                    isAdeudo -> Color(0xFFEF6C00)
                    isSuspendido -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                }

                // Active status chip
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = stateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (isActive) Icons.Rounded.CheckCircle else if (isAdeudo) Icons.Rounded.Warning else Icons.Rounded.Cancel,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = stateContainerColor,
                        labelColor = stateOnContainerColor,
                        leadingIconContentColor = stateOnContainerColor
                    ),
                    border = null,
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Spec connection tags row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Plan
                AssistChip(
                    onClick = { },
                    label = { Text(client.nombrePaquete ?: "Plan Especial", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp),
                    border = AssistChipDefaults.assistChipBorder(enabled = true)
                )
                
                // Connection Mode
                val isPppoe = client.tipoConexion?.contains("pppoe", ignoreCase = true) == true
                AssistChip(
                    onClick = { },
                    label = { Text(if (isPppoe) "PPPoE" else "Estática", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp),
                    border = AssistChipDefaults.assistChipBorder(enabled = true)
                )

                // Balance
                val balance = client.saldoActual?.toDoubleOrNull() ?: 0.0
                if (balance > 0) {
                    AssistChip(
                        onClick = { },
                        label = { Text("$$balance Pendiente", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            labelColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        border = null,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDetail,
                    modifier = Modifier.weight(1f).testTag("action_profile_${client.id}"),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Perfil", style = MaterialTheme.typography.labelSmall)
                }

                Button(
                    onClick = onPago,
                    modifier = Modifier.weight(1f).testTag("action_pago_${client.id}"),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(Icons.Rounded.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Cobro", style = MaterialTheme.typography.labelSmall)
                }

                FilledTonalButton(
                    onClick = onToggleStatus,
                    modifier = Modifier.weight(1.1f).testTag("action_toggle_${client.id}"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (isActive) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (isActive) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(if (isActive) Icons.Rounded.Block else Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isActive) "Cortar" else "Activar", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// Global helper to resolve compilation dependencies in other screens
fun triggerWhatsAppMessage(context: Context, client: Client) {
    if (client.telefono.isNullOrEmpty()) {
        Toast.makeText(context, "Este cliente no tiene teléfono", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val rawMessage = "Hola ${client.nombreCompleto}, le saludamos de Doblenet."
        val encodedMsg = java.net.URLEncoder.encode(rawMessage, "UTF-8")
        val uri = "https://api.whatsapp.com/send?phone=${client.telefono}&text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
    }
}
