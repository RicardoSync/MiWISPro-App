package com.example.ui.screens

import android.widget.Toast
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.Client
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.PaymentRecord
import com.example.ui.viewmodel.UiState
import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagosScreen(
    viewModel: ClientViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val payments by viewModel.payments.collectAsState()
    val clientsList by viewModel.clientsList.collectAsState()

    var showAddPaymentDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Computations
    val totalCollected = payments.sumOf { it.amount }
    val txCount = payments.size

    val filteredPayments = remember(payments, searchQuery) {
        val result = payments.filter { payment ->
            val nameMatch = payment.clientName.contains(searchQuery, ignoreCase = true)
            val dateMatch = payment.date.contains(searchQuery, ignoreCase = true)
            searchQuery.isEmpty() || nameMatch || dateMatch
        }.sortedByDescending { it.date + it.id }
        
        Log.d("PagosScreen", "Filtro aplicado: '$searchQuery' | Pagos encontrados: ${result.size}")
        result
    }

    val metodosState by viewModel.metodosPagoState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadMetodosPago()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPaymentDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_payment_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Registrar Pago")
                    Text("Registrar", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color.Transparent,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Header stats block
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Total Recaudado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$${String.format("%.2f", totalCollected)} MXN",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Transacciones",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$txCount recibos",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Historial de Recaudación",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar por nombre o fecha...") },
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
                    .padding(vertical = 8.dp)
                    .testTag("pago_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (filteredPayments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.ReceiptLong,
                            contentDescription = "Sin pagos",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Aún no hay abonos registrados",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1.0f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // Height for FAB clearance
                ) {
                    items(filteredPayments) { payment ->
                        PaymentItemRow(payment = payment)
                    }
                }
            }
        }
    }

    if (showAddPaymentDialog) {
        AddPaymentDialog(
            clients = clientsList,
            paymentMethods = if (metodosState is UiState.Success) {
                val response = (metodosState as UiState.Success<com.example.data.MetodosPagoResponse>).data
                response.data?.filter { m: com.example.data.MetodoPagoData -> m.activo == 1 }?.map { m: com.example.data.MetodoPagoData -> m.nombre ?: "Método" } ?: listOf("Efectivo")
            } else {
                listOf("Cargando...", "Efectivo")
            },
            onDismiss = { showAddPaymentDialog = false },
            onSave = { selectedClient, amount, method, concept ->
                // Proceed to update client saldo
                val currentSaldo = selectedClient.saldoActual?.toDoubleOrNull() ?: 0.0
                val newSaldo = maxOf(0.0, currentSaldo - amount)
                
                // Construct modified client
                val updatedClient = selectedClient.copy(
                    saldoActual = String.format("%.2f", newSaldo)
                )
                
                // Save mutated state in VM
                viewModel.updateClient(updatedClient)
                
                // Add ledger transaction log
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = sdf.format(Date())
                
                viewModel.addPayment(
                    PaymentRecord(
                        id = "PAG-${202600 + payments.size + 1}",
                        clientName = selectedClient.nombreCompleto ?: "Cliente ${selectedClient.id}",
                        amount = amount,
                        date = todayStr,
                        concept = concept.ifEmpty { "Abono / Pago de Internet" },
                        paymentMethod = method
                    )
                )

                Toast.makeText(context, "¡Pago de $${amount} registrado de forma local!", Toast.LENGTH_SHORT).show()
                showAddPaymentDialog = false
            }
        )
    }
}

@Composable
fun PaymentItemRow(payment: PaymentRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.0f)
            ) {
                // Receipt icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(GreenBadgeBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Paid,
                        contentDescription = null,
                        tint = GreenBadgeText
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = payment.clientName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${payment.concept} • ${payment.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Recibido: ${payment.date}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }

            Text(
                text = "$${String.format("%.2f", payment.amount)}",
                color = GreenBadgeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaymentDialog(
    clients: List<Client>,
    paymentMethods: List<String>,
    onDismiss: () -> Unit,
    onSave: (Client, Double, String, String) -> Unit
) {
    // Collect active clients with balances to make it seamless
    val eligibleClients = remember(clients) {
        clients.filter { (it.saldoActual?.toDoubleOrNull() ?: 0.0) >= 0.0 }
    }

    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var amountText by remember { mutableStateOf("") }
    var conceptText by remember { mutableStateOf("Mensualidad de Internet") }
    var selectedMethod by remember { mutableStateOf("Efectivo") }

    var expandedClientsMenu by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Registrar Recibo de Pago",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Dropdown matching client
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedClient?.nombreCompleto ?: "Seleccione un cliente...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asociar Cliente") },
                        trailingIcon = {
                            IconButton(onClick = { expandedClientsMenu = !expandedClientsMenu }) {
                                Icon(Icons.Rounded.ArrowDropDown, contentDescription = "Ver clientes")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedClientsMenu = true }
                    )

                    DropdownMenu(
                        expanded = expandedClientsMenu,
                        onDismissRequest = { expandedClientsMenu = false },
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        eligibleClients.forEach { client ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(client.nombreCompleto ?: "", fontWeight = FontWeight.Bold)
                                        Text(
                                            "Paquete: ${client.nombrePaquete ?: "Ninguno"} (Deuda: $${client.saldoActual ?: "0"} MXN)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedClient = client
                                    // Default the amount to pay off their remaining ledger balance!
                                    val balance = client.saldoActual?.toDoubleOrNull() ?: 0.0
                                    amountText = if (balance > 0) balance.toString() else "250.00"
                                    conceptText = "Pago mens. ${client.nombrePaquete ?: "Internet"}"
                                    expandedClientsMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Monto cobrado ($ MXN)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Payment Method Choice Row
                Text(
                    "Modalidad de Pago",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    paymentMethods.take(3).forEach { method ->
                        FilterChip(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            label = { Text(method) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 4. Concept
                OutlinedTextField(
                    value = conceptText,
                    onValueChange = { conceptText = it },
                    label = { Text("Concepto o Detalles") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save or Cancel Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val client = selectedClient
                            val amount = amountText.toDoubleOrNull() ?: 0.0
                            if (client != null && amount > 0) {
                                onSave(client, amount, selectedMethod, conceptText)
                            } else {
                                // Raise Toast
                            }
                        },
                        enabled = selectedClient != null && amountText.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Registrar")
                    }
                }
            }
        }
    }
}
