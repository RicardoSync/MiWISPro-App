package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarClienteScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val registrarState by viewModel.registrarClienteState.collectAsState()
    val datosState by viewModel.datosRegistroState.collectAsState()

    var nombre by remember { mutableStateOf("") }
    var tel by remember { mutableStateOf("") }
    var selectedPaquete by remember { mutableStateOf<com.example.data.Paquete?>(null) }
    var selectedMikrotik by remember { mutableStateOf<com.example.data.RouterRegistro?>(null) }
    var diaCorte by remember { mutableStateOf("") }
    var proxPago by remember { mutableStateOf("") }
    var tipoConexion by remember { mutableStateOf("estatica") }
    var ipCliente by remember { mutableStateOf("") }
    var pppoeUsuario by remember { mutableStateOf("") }
    var pppoePassword by remember { mutableStateOf("") }
    var coordenadas by remember { mutableStateOf("") }
    var dir by remember { mutableStateOf("") }
    var selectedServicioExtra by remember { mutableStateOf<com.example.data.ServicioExtra?>(null) }
    var dni by remember { mutableStateOf("") }
    var promesaPago by remember { mutableStateOf("") }

    var expandedConexion by remember { mutableStateOf(false) }
    var expandedPaquete by remember { mutableStateOf(false) }
    var expandedMikrotik by remember { mutableStateOf(false) }
    var expandedServicio by remember { mutableStateOf(false) }

    // Handlers for Date Picker
    val showDatePicker = { onDateSelected: (String) -> Unit ->
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val m = (month + 1).toString().padStart(2, '0')
                val d = dayOfMonth.toString().padStart(2, '0')
                onDateSelected("$year-$m-$d")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LaunchedEffect(registrarState) {
        when (val state = registrarState) {
            is UiState.Success -> {
                android.widget.Toast.makeText(context, state.data.mensaje ?: "Cliente registrado exitosamente.", android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetRegistrarClienteState()
                onBack()
            }
            is UiState.Error -> {
                android.widget.Toast.makeText(context, state.message, android.widget.Toast.LENGTH_LONG).show()
                viewModel.resetRegistrarClienteState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Cliente", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (nombre.isNotBlank() && tel.isNotBlank() && selectedPaquete != null && selectedMikrotik != null && diaCorte.isNotBlank() && proxPago.isNotBlank()) {
                        viewModel.registrarClienteRemote(
                            nombre = nombre,
                            tel = tel,
                            idPaquete = selectedPaquete!!.id,
                            idMikrotik = selectedMikrotik!!.id,
                            diaCorte = diaCorte.toIntOrNull() ?: 1,
                            proxPago = proxPago,
                            tipoConexion = tipoConexion,
                            ipCliente = if (tipoConexion == "estatica") ipCliente else null,
                            pppoeUsuario = if (tipoConexion == "pppoe") pppoeUsuario else null,
                            pppoePassword = if (tipoConexion == "pppoe") pppoePassword else null,
                            coordenadas = coordenadas.ifBlank { null },
                            dir = dir.ifBlank { null },
                            idServicioExtra = selectedServicioExtra?.id?.toString(),
                            dni = dni.ifBlank { null },
                            promesaPago = promesaPago.ifBlank { null }
                        )
                    } else {
                        android.widget.Toast.makeText(context, "Faltan campos obligatorios", android.widget.Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                icon = {
                    if (registrarState is UiState.Loading) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.Save, contentDescription = "Guardar", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                text = {
                    Text("Guardar", color = MaterialTheme.colorScheme.onPrimary)
                }
            )
        }
    ) { innerPadding ->
        if (datosState is UiState.Loading) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (datosState is UiState.Error) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text((datosState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadDatosRegistro() }) { Text("Reintentar") }
                }
            }
            return@Scaffold
        }

        val paquetes = (datosState as? UiState.Success)?.data?.data?.paquetes ?: emptyList()
        val routers = (datosState as? UiState.Success)?.data?.data?.routers ?: emptyList()
        val servicios = (datosState as? UiState.Success)?.data?.data?.serviciosExtra ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Información Básica Obligatoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre Completo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) }
            )

            OutlinedTextField(
                value = tel,
                onValueChange = { if (it.length <= 10) tel = it },
                label = { Text("Teléfono") },
                supportingText = { Text("10 dígitos") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expandedPaquete,
                    onExpandedChange = { expandedPaquete = !expandedPaquete },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedPaquete?.nombrePlan ?: "Seleccione...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Paquete") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedPaquete) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedPaquete,
                        onDismissRequest = { expandedPaquete = false }
                    ) {
                        paquetes.forEach { paquete ->
                            DropdownMenuItem(
                                text = { Text("${paquete.nombrePlan} - $${paquete.precio}") },
                                onClick = { selectedPaquete = paquete; expandedPaquete = false }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = expandedMikrotik,
                    onExpandedChange = { expandedMikrotik = !expandedMikrotik },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedMikrotik?.nombre ?: "Seleccione...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("MikroTik") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMikrotik) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedMikrotik,
                        onDismissRequest = { expandedMikrotik = false }
                    ) {
                        routers.forEach { router ->
                            DropdownMenuItem(
                                text = { Text(router.nombre ?: "") },
                                onClick = { selectedMikrotik = router; expandedMikrotik = false }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = diaCorte,
                    onValueChange = { diaCorte = it },
                    label = { Text("Día Corte") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = proxPago,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Próx Pago") },
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker { date -> proxPago = date } }) {
                            Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Configuración de Red", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            ExposedDropdownMenuBox(
                expanded = expandedConexion,
                onExpandedChange = { expandedConexion = !expandedConexion },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = tipoConexion.uppercase(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Conexión") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedConexion) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandedConexion,
                    onDismissRequest = { expandedConexion = false }
                ) {
                    DropdownMenuItem(text = { Text("ESTÁTICA") }, onClick = { tipoConexion = "estatica"; expandedConexion = false })
                    DropdownMenuItem(text = { Text("PPPOE") }, onClick = { tipoConexion = "pppoe"; expandedConexion = false })
                }
            }

            AnimatedVisibility(visible = tipoConexion == "estatica") {
                OutlinedTextField(
                    value = ipCliente,
                    onValueChange = { ipCliente = it },
                    label = { Text("IP Cliente") },
                    supportingText = { Text("ej. 192.168.10.50") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            AnimatedVisibility(visible = tipoConexion == "pppoe") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = pppoeUsuario,
                        onValueChange = { pppoeUsuario = it },
                        label = { Text("PPPoE Usuario") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = pppoePassword,
                        onValueChange = { pppoePassword = it },
                        label = { Text("PPPoE Contraseña") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Información Opcional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            OutlinedTextField(
                value = dir,
                onValueChange = { dir = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = coordenadas,
                onValueChange = { coordenadas = it },
                label = { Text("Coordenadas (Lat, Lng)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = dni,
                    onValueChange = { dni = it },
                    label = { Text("DNI / RFC") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = expandedServicio,
                    onExpandedChange = { expandedServicio = !expandedServicio },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedServicioExtra?.nombre ?: "Ninguno",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Servicio Extra") },
                        trailingIcon = {
                            if (selectedServicioExtra != null) {
                                IconButton(onClick = { selectedServicioExtra = null }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = "Limpiar")
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedServicio)
                            }
                        },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expandedServicio,
                        onDismissRequest = { expandedServicio = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ninguno") },
                            onClick = { selectedServicioExtra = null; expandedServicio = false }
                        )
                        servicios.forEach { srv ->
                            DropdownMenuItem(
                                text = { Text("${srv.nombre} (+$${srv.precio})") },
                                onClick = { selectedServicioExtra = srv; expandedServicio = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = promesaPago,
                onValueChange = {},
                readOnly = true,
                label = { Text("Promesa de Pago") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker { date -> promesaPago = date } }) {
                        Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                    }
                }
            )

            Spacer(modifier = Modifier.height(80.dp)) // padding for FAB
        }
    }
}
