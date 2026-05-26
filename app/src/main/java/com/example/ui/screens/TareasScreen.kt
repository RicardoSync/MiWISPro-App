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
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TareasScreen(
    viewModel: ClientViewModel,
    onBack: () -> Unit
) {
    val configState by viewModel.configAutomatizacionState.collectAsState()
    
    var statsMikrotikActivo by remember { mutableStateOf(false) }
    var monitorTraficoActivo by remember { mutableStateOf(false) }
    var recordatorioPagoActivo by remember { mutableStateOf(false) }
    var recordatorioCorteActivo by remember { mutableStateOf(false) }
    var reporteDiarioActivo by remember { mutableStateOf(false) }
    
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(configState) {
        if (configState is UiState.Success<*> && !isEditing) {
            val successState = configState as UiState.Success<com.example.data.ConfigAutomatizacionResponse>
            val data = successState.data.data
            statsMikrotikActivo = data?.statsMikrotikActivo == 1
            monitorTraficoActivo = data?.monitorTraficoActivo == 1
            recordatorioPagoActivo = data?.recordatorioPagoActivo == 1
            recordatorioCorteActivo = data?.recordatorioCorteActivo == 1
            reporteDiarioActivo = data?.reporteDiarioActivo == 1
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tareas de Automatización", fontWeight = FontWeight.Bold) },
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
                        viewModel.updateConfigAutomatizacion(
                            statsMikrotikActivo = if (statsMikrotikActivo) 1 else 0,
                            monitorTraficoActivo = if (monitorTraficoActivo) 1 else 0,
                            recordatorioPagoActivo = if (recordatorioPagoActivo) 1 else 0,
                            recordatorioCorteActivo = if (recordatorioCorteActivo) 1 else 0,
                            reporteDiarioActivo = if (reporteDiarioActivo) 1 else 0
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
                        Button(onClick = { viewModel.loadConfigAutomatizacion() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            is UiState.Success<*> -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Configura las tareas que se ejecutan automáticamente en segundo plano.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    AutomationItemCard(
                        title = "Estadísticas Mikrotik",
                        description = "Recopila y actualiza estadísticas de uso de los routers Mikrotik.",
                        icon = Icons.Rounded.Router,
                        isChecked = statsMikrotikActivo,
                        onCheckedChange = { 
                            statsMikrotikActivo = it
                            isEditing = true
                        }
                    )

                    AutomationItemCard(
                        title = "Monitor de Tráfico",
                        description = "Monitorea constantemente el tráfico en tiempo real para estadísticas.",
                        icon = Icons.Rounded.NetworkCheck,
                        isChecked = monitorTraficoActivo,
                        onCheckedChange = { 
                            monitorTraficoActivo = it
                            isEditing = true
                        }
                    )

                    AutomationItemCard(
                        title = "Recordatorios de Pago",
                        description = "Envía recordatorios automáticos de pago a clientes por vencer.",
                        icon = Icons.Rounded.Payment,
                        isChecked = recordatorioPagoActivo,
                        onCheckedChange = { 
                            recordatorioPagoActivo = it
                            isEditing = true
                        }
                    )

                    AutomationItemCard(
                        title = "Avisos de Corte",
                        description = "Envía notificaciones previas o en el momento del corte de servicio.",
                        icon = Icons.Rounded.WarningAmber,
                        isChecked = recordatorioCorteActivo,
                        onCheckedChange = { 
                            recordatorioCorteActivo = it
                            isEditing = true
                        }
                    )

                    AutomationItemCard(
                        title = "Reporte Diario",
                        description = "Genera y envía un reporte administrativo diario con resúmenes.",
                        icon = Icons.Rounded.Assessment,
                        isChecked = reporteDiarioActivo,
                        onCheckedChange = { 
                            reporteDiarioActivo = it
                            isEditing = true
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(72.dp)) // space for FAB
                }
            }
        }
    }
}

@Composable
fun AutomationItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isChecked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (isChecked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
