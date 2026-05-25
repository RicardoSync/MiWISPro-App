package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
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

@OptIn(ExperimentalMaterial3Api::class)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // High legibility slim Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Buscar por nombre, teléfono...", style = MaterialTheme.typography.bodyMedium) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Buscar", tint = MaterialTheme.colorScheme.primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("search_input"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Main client list contents
        when (val state = uiState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth()
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
                val filteredClients = clientsList.filter { client ->
                    // Keyword fields match
                    val nameMatch = client.nombreCompleto?.contains(searchQuery, ignoreCase = true) ?: false
                    val phoneMatch = client.telefono?.contains(searchQuery, ignoreCase = true) ?: false
                    val ipMatch = client.ipCliente?.contains(searchQuery, ignoreCase = true) ?: false
                    searchQuery.isEmpty() || nameMatch || phoneMatch || ipMatch
                }

                if (filteredClients.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1.0f)
                            .fillMaxWidth(),
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
                    // Pagination Computations
                    val totalItems = filteredClients.size
                    val totalPages = maxOf(1, kotlin.math.ceil(totalItems.toDouble() / itemsPerPage).toInt())
                    val safeCurrentPage = minOf(currentPage, totalPages)
                    val startIndex = (safeCurrentPage - 1) * itemsPerPage
                    val endIndex = minOf(startIndex + itemsPerPage, totalItems)
                    val paginatedList = filteredClients.subList(startIndex, endIndex)

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
                                onDeuda = { viewModel.selectClientForDeuda(client) },
                                onPago = { viewModel.selectClientForPago(client) },
                                onToggleStatus = {
                                    viewModel.toggleClientStatusRemote(client) { success, message ->
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                    }
                                }
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

@Composable
fun ClientCard(
    client: Client,
    onDetail: () -> Unit,
    onDeuda: () -> Unit,
    onPago: () -> Unit,
    onToggleStatus: () -> Unit
) {
    val isActive = client.activo == 1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with name and responsive status indicator badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1.0f).padding(end = 8.dp)) {
                    Text(
                        text = client.nombreCompleto?.uppercase() ?: "CLIENTE SIN NOMBRE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${client.telefono ?: "Sin Teléfono"} • ${if (client.ipCliente.isNullOrEmpty()) "IP Dinámica" else client.ipCliente}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Active status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (isActive) GreenBadgeBg else RedBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (isActive) GreenBadgeText else RedBadgeText, CircleShape)
                        )
                        Text(
                            text = if (isActive) "ACTIVO" else "SUSPENDIDO",
                            color = if (isActive) GreenBadgeText else RedBadgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Spec connection tags row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Plan
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = client.nombrePaquete ?: "Plan Especial",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                // Connection Mode
                val isPppoe = client.tipoConexion?.contains("pppoe", ignoreCase = true) == true
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPppoe) BlueBadgeBg else OrangeBadgeBg)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isPppoe) "PPPoE" else "Estática",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isPppoe) BlueBadgeText else OrangeBadgeText
                    )
                }

                // Balance
                val balance = client.saldoActual?.toDoubleOrNull() ?: 0.0
                if (balance > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RedBadgeBg)
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$$balance Pendiente",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = RedBadgeText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Four high-importance interactive action buttons matching material guidelines
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Perfil Detail Launcher
                Button(
                    onClick = onDetail,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("action_profile_${client.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Detalles",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "Perfil",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Deuda/Cajero Button
                Button(
                    onClick = onDeuda,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("action_deuda_${client.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = "Deuda",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "Deuda",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Cobro (Registrar Pago) Button
                Button(
                    onClick = onPago,
                    modifier = Modifier
                        .weight(0.9f)
                        .height(38.dp)
                        .testTag("action_pago_${client.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BlueBadgeBg,
                        contentColor = BlueBadgeText
                    ),
                    border = BorderStroke(1.dp, BlueBadgeText.copy(alpha = 0.15f)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PointOfSale,
                        contentDescription = "Cobrar",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        "Cobro",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Active toggler
                Button(
                    onClick = onToggleStatus,
                    modifier = Modifier
                        .weight(1.1f)
                        .height(38.dp)
                        .testTag("action_toggle_${client.id}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) RedBadgeBg else GreenBadgeBg,
                        contentColor = if (isActive) RedBadgeText else GreenBadgeText
                    ),
                    border = BorderStroke(
                        width = 1.dp,
                        color = (if (isActive) RedBadgeText else GreenBadgeText).copy(alpha = 0.2f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 1.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isActive) Icons.Rounded.Block else Icons.Rounded.CheckCircle,
                        contentDescription = if (isActive) "Cortar" else "Activar",
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isActive) "Cortar" else "Activar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
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
