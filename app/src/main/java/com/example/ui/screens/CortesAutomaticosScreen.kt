package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CortesAutomaticosScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val configState by viewModel.configCortesState.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    var selectedHour by remember { mutableStateOf("09:00:00") }
    var isActive by remember { mutableStateOf(false) }
    var diasGracia by remember { mutableStateOf("3") }

    val ejecutarCortesState by viewModel.ejecutarCortesState.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var showLoadingDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val ejecutarActivacionesState by viewModel.ejecutarActivacionesState.collectAsState()
    var showConfirmActivacionesDialog by remember { mutableStateOf(false) }
    var showResultActivacionesDialog by remember { mutableStateOf(false) }
    var showLoadingActivacionesDialog by remember { mutableStateOf(false) }
    var showErrorActivacionesDialog by remember { mutableStateOf(false) }
    var errorActivacionesMessage by remember { mutableStateOf("") }

    LaunchedEffect(ejecutarCortesState) {
        when (val state = ejecutarCortesState) {
            is UiState.Loading -> {
                showLoadingDialog = true
                showResultDialog = false
                showErrorDialog = false
            }
            is UiState.Success -> {
                showLoadingDialog = false
                showResultDialog = true
                showErrorDialog = false
            }
            is UiState.Error -> {
                showLoadingDialog = false
                showResultDialog = false
                showErrorDialog = true
                errorMessage = state.message
            }
            null -> {
                showLoadingDialog = false
                showResultDialog = false
                showErrorDialog = false
            }
        }
    }

    LaunchedEffect(ejecutarActivacionesState) {
        when (val state = ejecutarActivacionesState) {
            is UiState.Loading -> {
                showLoadingActivacionesDialog = true
                showResultActivacionesDialog = false
                showErrorActivacionesDialog = false
            }
            is UiState.Success -> {
                showLoadingActivacionesDialog = false
                showResultActivacionesDialog = true
                showErrorActivacionesDialog = false
            }
            is UiState.Error -> {
                showLoadingActivacionesDialog = false
                showResultActivacionesDialog = false
                showErrorActivacionesDialog = true
                errorActivacionesMessage = state.message
            }
            null -> {
                showLoadingActivacionesDialog = false
                showResultActivacionesDialog = false
                showErrorActivacionesDialog = false
            }
        }
    }

    // Initialize local state when data loads successfully
    LaunchedEffect(configState) {
        if (configState is UiState.Success<*> && !isEditing) {
            val successState = configState as UiState.Success<com.example.data.ConfigCortesResponse>
            val data = successState.data.data
            selectedHour = data?.horaEjecucion ?: "00:00:00"
            isActive = (data?.activo == 1)
            diasGracia = (data?.diasGracia ?: 3).toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cortes Automáticos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            if (configState is UiState.Success<*>) {
                FloatingActionButton(
                    onClick = {
                        isEditing = false
                        viewModel.updateConfigCortes(
                            activo = if (isActive) 1 else 0,
                            horaEjecucion = selectedHour,
                            diasGracia = diasGracia.toIntOrNull() ?: 3
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = "Guardar cambios")
                }
            }
        }
    ) { innerPadding ->
        when (val state = configState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Rounded.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadConfigCortes() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is UiState.Success<*> -> {
                val successState = state as UiState.Success<com.example.data.ConfigCortesResponse>
                val data = successState.data.data
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Status Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Estado del Servicio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Rounded.PowerSettingsNew,
                                        contentDescription = null,
                                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isActive) "Activado" else "Desactivado",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "El sistema realizará los cortes de forma automática",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                                Switch(
                                    checked = isActive,
                                    onCheckedChange = { 
                                        isEditing = true
                                        isActive = it 
                                    }
                                )
                            }
                        }
                    }

                    // Configuration Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Configuración de Ejecución",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Hour selector
                            var expanded by remember { mutableStateOf(false) }
                            val hours = (0..23).map { String.format("%02d:00:00", it) }
                            
                            ExposedDropdownMenuBox(
                                expanded = expanded,
                                onExpandedChange = { expanded = it },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedHour.substring(0, 5), // Show HH:MM
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Hora de ejecución (24h)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Rounded.Schedule, contentDescription = null) }
                                )
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    hours.forEach { selectionOption ->
                                        DropdownMenuItem(
                                            text = { Text(selectionOption.substring(0, 5)) },
                                            onClick = {
                                                selectedHour = selectionOption
                                                isEditing = true
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            OutlinedTextField(
                                value = diasGracia,
                                onValueChange = { 
                                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                        diasGracia = it
                                        isEditing = true
                                    }
                                },
                                label = { Text("Días de gracia") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Última ejecución: ${data?.ultimoEjecucion ?: "Desconocida"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Card for Immediate Emergency Cuts
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Corte de Emergencia (Inmediato)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ejecuta de manera manual e inmediata el proceso de corte para todos los clientes que presenten facturas vencidas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ejecutar Corte Inmediato", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Card for Immediate Emergency Activations
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Activación Masiva de Emergencia",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Activa de forma masiva e inmediata a todos los clientes que hayan sido suspendidos por error.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showConfirmActivacionesDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Ejecutar Activación Masiva", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs for Immediate Cuts flow
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Corte Inmediato", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas ejecutar el proceso de cortes de emergencia de inmediato?\n\nEsta acción suspenderá el servicio en el MikroTik a todos los clientes que tengan deudas o facturas vencidas."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.ejecutarCorteInmediato()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Ejecutar Corte")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showLoadingDialog) {
        AlertDialog(
            onDismissRequest = { /* No se puede descartar */ },
            confirmButton = {},
            title = {
                Text("Ejecutando Cortes de Emergencia", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.error,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Por favor espere. El servidor está procesando las suspensiones en los routers MikroTik...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }

    if (showResultDialog) {
        val response = (ejecutarCortesState as? UiState.Success)?.data
        AlertDialog(
            onDismissRequest = {
                showResultDialog = false
                viewModel.resetEjecutarCortesState()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resultado del Proceso", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = response?.mensaje ?: "Proceso de cortes finalizado.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val stats = response?.data
                    if (stats != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clientes Suspendidos:", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${stats.suspendidos ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clientes Activados:", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${stats.activados ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Errores en proceso:", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${stats.errores ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        color = if ((stats.errores ?: 0) > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultDialog = false
                        viewModel.resetEjecutarCortesState()
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.resetEjecutarCortesState()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Error al Ejecutar Cortes", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(errorMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorDialog = false
                        viewModel.resetEjecutarCortesState()
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    // Dialogs for Immediate Activations flow
    if (showConfirmActivacionesDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmActivacionesDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirmar Activación Masiva", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "¿Estás seguro de que deseas ejecutar el proceso de activaciones masivas de inmediato?\n\nEsta acción restablecerá el servicio en el MikroTik a todos los clientes suspendidos para corregir cualquier suspensión realizada por error."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmActivacionesDialog = false
                        viewModel.ejecutarActivacionInmediata()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Ejecutar Activación")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmActivacionesDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showLoadingActivacionesDialog) {
        AlertDialog(
            onDismissRequest = { /* No se puede descartar */ },
            confirmButton = {},
            title = {
                Text("Ejecutando Activación Masiva", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 4.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Por favor espere. El servidor está reactivando las cuentas en los routers MikroTik...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        )
    }

    if (showResultActivacionesDialog) {
        val response = (ejecutarActivacionesState as? UiState.Success)?.data
        AlertDialog(
            onDismissRequest = {
                showResultActivacionesDialog = false
                viewModel.resetEjecutarActivacionesState()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Resultado de Activación", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = response?.mensaje ?: "Proceso de activaciones masivas finalizado.",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    val stats = response?.data
                    if (stats != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Clientes Activados:", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${stats.activados ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Errores en proceso:", fontWeight = FontWeight.Medium)
                                    Text(
                                        "${stats.errores ?: 0}",
                                        fontWeight = FontWeight.Bold,
                                        color = if ((stats.errores ?: 0) > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResultActivacionesDialog = false
                        viewModel.resetEjecutarActivacionesState()
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showErrorActivacionesDialog) {
        AlertDialog(
            onDismissRequest = {
                showErrorActivacionesDialog = false
                viewModel.resetEjecutarActivacionesState()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Error al Activar Clientes", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(errorActivacionesMessage)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showErrorActivacionesDialog = false
                        viewModel.resetEjecutarActivacionesState()
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}
