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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.Client
import com.example.data.PagoResponse
import com.example.ui.theme.*
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarPagoScreen(
    client: Client,
    viewModel: ClientViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Retrieve registered subdomain from configuration
    val configState by viewModel.appConfig.collectAsState()
    val activeSubdomain = configState.subdominio
    
    // Inputs state
    var montoInput by remember { mutableStateOf(client.saldoActual ?: "") }
    var metodoInput by remember { mutableStateOf("Cobro desde móvil") }
    var referenciaInput by remember { mutableStateOf("") }
    
    // Payment process state
    var registrarState by remember { mutableStateOf<UiState<PagoResponse>?>(null) }
    
    // Remote payment methods state
    val metodosState by viewModel.metodosPagoState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.loadMetodosPago()
    }
    
    // Dropdown state for payment methods
    var dropdownExpanded by remember { mutableStateOf(false) }
    val paymentMethods = if (metodosState is UiState.Success) {
        val response = (metodosState as UiState.Success<com.example.data.MetodosPagoResponse>).data
        response.data?.filter { m: com.example.data.MetodoPagoData -> m.activo == 1 }?.map { m: com.example.data.MetodoPagoData -> m.nombre ?: "Método" } ?: listOf("Efectivo")
    } else {
        listOf("Cargando...", "Efectivo") // Fallback
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Registrar Cobro",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Cliente: ${client.nombreCompleto ?: "Sin Nombre"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("pago_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Regresar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.statusBarsPadding()
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp)
        ) {
            
            // Only show form if we are not in successful registration state
            val currentState = registrarState
            if (currentState is UiState.Success) {
                item {
                    SuccessPaymentCard(
                        response = currentState.data,
                        clientName = client.nombreCompleto ?: "Cliente",
                        subdomain = activeSubdomain,
                        onBack = onBack
                    )
                }
            } else {
                // Client Info Card (Web design aesthetic)
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(99.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "CUENTA #${client.id}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(BlueBadgeBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PriceCheck,
                                        contentDescription = null,
                                        tint = BlueBadgeText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            Text(
                                text = client.nombreCompleto?.uppercase() ?: "SIN NOMBRE REGISTRADO",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Saldo Pendiente",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "$${client.saldoActual ?: "0.00"} MXN",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if ((client.saldoActual?.toDoubleOrNull() ?: 0.0) > 0.0) RedBadgeText else GreenBadgeText
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Paquete Base",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = client.nombrePaquete ?: "Sin Paquete",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Payment Fields Form Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Detalles de la Transacción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            // 1. Amount Field
                            Column {
                                Text(
                                    "Monto a Cobrar (MXN) *",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = montoInput,
                                    onValueChange = { montoInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pago_monto_input"),
                                    placeholder = { Text("Ej. 350.00") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.AttachMoney,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingIcon = {
                                        if (montoInput.isNotEmpty()) {
                                            IconButton(onClick = { montoInput = "" }) {
                                                Icon(Icons.Rounded.Clear, contentDescription = "Limpiar")
                                            }
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            // 2. Payment Method Dropdown (Exposed Dropdown Box style)
                            Column {
                                Text(
                                    "Método de Pago *",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { dropdownExpanded = true }
                                ) {
                                    OutlinedTextField(
                                        value = metodoInput,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false, // We make the actual click occur on the box
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            disabledLeadingIconColor = MaterialTheme.colorScheme.primary,
                                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        leadingIcon = {
                                            Icon(
                                                Icons.Rounded.AccountBalance,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = if (dropdownExpanded) Icons.Rounded.ArrowDropUp else Icons.Rounded.ArrowDropDown,
                                                contentDescription = "Desplegar"
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    
                                    DropdownMenu(
                                        expanded = dropdownExpanded,
                                        onDismissRequest = { dropdownExpanded = false },
                                        modifier = Modifier
                                            .fillMaxWidth(0.85f)
                                            .background(MaterialTheme.colorScheme.surface)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                                    ) {
                                        paymentMethods.forEach { method ->
                                            DropdownMenuItem(
                                                text = { Text(method, fontWeight = FontWeight.Medium) },
                                                onClick = {
                                                    metodoInput = method
                                                    dropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // 3. Reference Field
                            Column {
                                Text(
                                    "Referencia / No. de Autorización",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = referenciaInput,
                                    onValueChange = { referenciaInput = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pago_referencia_input"),
                                    placeholder = { Text("Ej. 1234567890 (Opcional)") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Rounded.Numbers,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Action Response States (Error / Loading)
                if (currentState is UiState.Loading) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Procesando cobro en MiWISPro WEB...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else if (currentState is UiState.Error) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = RedBadgeBg),
                            border = BorderStroke(1.dp, RedBadgeText.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.Error,
                                        contentDescription = null,
                                        tint = RedBadgeText,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Error al Procesar Pago",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = RedBadgeText
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RedBadgeText.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Register payment button helper
                item {
                    Button(
                        onClick = {
                            if (montoInput.trim().isEmpty()) {
                                Toast.makeText(context, "Por favor ingrese el monto", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            
                            viewModel.registrarPagoRemote(
                                clientId = client.id,
                                monto = montoInput.trim(),
                                metodo = metodoInput,
                                referencia = referenciaInput.trim(),
                                onResult = { state ->
                                    registrarState = state
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .testTag("pago_submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        enabled = currentState !is UiState.Loading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PointOfSale,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "REGISTRAR Y PROCESAR COBRO",
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

private fun getDynamicPdfUrl(rawUrl: String, activeSubdomain: String): String {
    if (rawUrl.isEmpty() || activeSubdomain.isEmpty()) return rawUrl
    try {
        val uri = Uri.parse(rawUrl)
        val host = uri.host ?: ""
        if (host.isNotEmpty()) {
            val hostParts = host.split(".")
            if (hostParts.size >= 3) {
                // Reconstruct the host by substituting the first subdomain label
                val newHost = (listOf(activeSubdomain) + hostParts.drop(1)).joinToString(".")
                return rawUrl.replace(host, newHost)
            }
        }
        // Fallback: simple text replacement in case parse failed
        if (rawUrl.contains("doblenet")) {
            return rawUrl.replace("doblenet", activeSubdomain)
        }
    } catch (e: Exception) {
        // Safe fallback
    }
    return rawUrl
}

@Composable
fun SuccessPaymentCard(
    response: PagoResponse?,
    clientName: String,
    subdomain: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val data = response?.data
    val rawPdfUrl = data?.urlReciboPdf ?: ""
    val pdfUrl = remember(rawPdfUrl, subdomain) {
        getDynamicPdfUrl(rawPdfUrl, subdomain)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Beautiful modern success circle badge
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(GreenBadgeBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = GreenBadgeText,
                    modifier = Modifier.size(44.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                "¡Cobro Registrado!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = GreenBadgeText,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = response?.mensaje ?: "El pago fue registrado exitosamente en el ruteador y la base de datos.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Transaction Summary list
            PagoDetailPropertyRow("Pago ID", "#${data?.idPago ?: "N/D"}")
            PagoDetailPropertyRow("Monto Registrado", "$${data?.montoPagado ?: "0.00"} MXN")
            PagoDetailPropertyRow("Nuevo Saldo", "$${data?.nuevoSaldo ?: "0.00"} MXN", isHighlighted = (data?.nuevoSaldo?.toDoubleOrNull() ?: 0.0) > 0.0)
            data?.proximoPago?.let {
                PagoDetailPropertyRow("Próximo Pago", it)
            }
            PagoDetailPropertyRow(
                "Internet Reactivado", 
                if (data?.internetReactivado == true) "SÍ (Sincronizado Mikrotik)" else "Mantiene Status"
            )
            PagoDetailPropertyRow(
                "Notificación WhatsApp", 
                if (data?.whatsappEnviado == true) "Enviado con Éxito" else "No requerido"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Downloader helper button
            Button(
                onClick = {
                    if (pdfUrl.isNotEmpty()) {
                        Toast.makeText(context, "Abriendo comprobante en el navegador...", Toast.LENGTH_SHORT).show()
                        openComprobantePdf(context, pdfUrl)
                    } else {
                        Toast.makeText(context, "URL de Comprobante no disponible", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueBadgeBg,
                    contentColor = BlueBadgeText
                ),
                border = BorderStroke(1.dp, BlueBadgeText.copy(alpha = 0.2f)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Descargar Comprobante PDF",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Return button
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    "Volver Clientes",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun openComprobantePdf(context: Context, url: String) {
    try {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(browserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "No se pudo descargar el ticket: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun PagoDetailPropertyRow(label: String, value: String, isHighlighted: Boolean = false, isWarning: Boolean = false) {
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
