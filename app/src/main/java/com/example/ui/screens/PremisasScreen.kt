package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.FacturaData
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremisasScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val facturasState by viewModel.facturasState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var facturaToAnular by remember { mutableStateOf<FacturaData?>(null) }

    facturaToAnular?.let { factura ->
        AlertDialog(
            onDismissRequest = { facturaToAnular = null },
            title = { Text("Confirmar anulación") },
            text = { Text("¿Estás seguro de que deseas anular la factura #${factura.id} de ${factura.nombreCliente}?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.anularFactura(factura.id, factura.idCliente) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                    facturaToAnular = null
                }) {
                    Text("Anular", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { facturaToAnular = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premisas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadFacturas() }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refrescar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre o # de factura...") },
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
                    .padding(vertical = 8.dp),
                singleLine = true,
                shape = OutlinedTextFieldDefaults.shape
            )
            
            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = facturasState,
                transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(400)) },
                label = "facturas_state_transition",
                modifier = Modifier.weight(1.0f).fillMaxWidth()
            ) { state ->
                when (state) {
                    is UiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    is UiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.CloudOff, contentDescription = "Error", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(state.message, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadFacturas() }) { Text("Reintentar") }
                            }
                        }
                    }
                    is UiState.Success -> {
                        // Sort by ID descending (most recent first)
                        val sortedFacturas = state.data.data?.sortedByDescending { it.id.toIntOrNull() ?: 0 } ?: emptyList()
                        
                        val filteredFacturas = sortedFacturas.filter { factura ->
                            val nameMatch = factura.nombreCliente?.contains(searchQuery, ignoreCase = true) ?: false
                            val idMatch = factura.id.contains(searchQuery, ignoreCase = true)
                            searchQuery.isEmpty() || nameMatch || idMatch
                        }
                        
                        if (filteredFacturas.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                                    Icon(Icons.Rounded.SearchOff, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Ninguna factura coincide", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Prueba cambiando el texto ingresado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(filteredFacturas, key = { it.id }) { factura ->
                                    FacturaCard(
                                        factura = factura,
                                        onAnular = { facturaToAnular = factura },
                                        onClick = { viewModel.openFacturaDetail(factura, false) }
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

@Composable
fun FacturaCard(
    factura: FacturaData,
    onAnular: () -> Unit,
    onClick: () -> Unit
) {
    val isPagada = factura.estado?.lowercase() == "pagada"
    val isAnulada = factura.estado?.lowercase() == "anulada"
    val isPendiente = factura.estado?.lowercase() == "pendiente"

    val containerColor = when {
        isPagada -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isAnulada -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val iconTint = when {
        isPagada -> MaterialTheme.colorScheme.primary
        isAnulada -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }

    val iconVector = when {
        isPagada -> Icons.Rounded.CheckCircle
        isAnulada -> Icons.Rounded.Cancel
        else -> Icons.Rounded.Schedule
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(iconTint.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = iconTint)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1.0f).padding(end = 8.dp)) {
                    Text(
                        text = factura.nombreCliente?.uppercase() ?: "CLIENTE DESCONOCIDO",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Factura #${factura.id} • ${factura.fechaEmision ?: "Sin fecha"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                AssistChip(
                    onClick = { },
                    label = { 
                        Text(
                            text = factura.estado?.uppercase() ?: "DESCONOCIDO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    leadingIcon = {
                        Icon(imageVector = iconVector, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = iconTint.copy(alpha = 0.2f),
                        labelColor = iconTint,
                        leadingIconContentColor = iconTint
                    ),
                    border = null,
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AssistChip(
                    onClick = { },
                    label = { Text(factura.nombrePlan ?: "Sin Plan", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp),
                    border = AssistChipDefaults.assistChipBorder(enabled = true)
                )
                
                val monto = factura.montoTotal?.toDoubleOrNull() ?: 0.0
                AssistChip(
                    onClick = { },
                    label = { Text("Total: $$monto", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    border = null,
                    modifier = Modifier.height(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onAnular,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anular", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
