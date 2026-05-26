package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.HomeTab
import com.example.ui.viewmodel.UiState
import com.example.R

@Composable
fun DashboardScreen(
    viewModel: ClientViewModel,
    modifier: Modifier = Modifier
) {
    val dashboardState by viewModel.dashboardState.collectAsState()
    val configState by viewModel.appConfig.collectAsState()
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header has been removed as requested

        when (val state = dashboardState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Cargando métricas de rendimiento...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.08f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Error al consultar panel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.loadDashboard() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reintentar")
                        }
                    }
                }
            }
            is UiState.Success -> {
                val data = state.data.data
                if (data == null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay datos de panel para mostrar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else {
                    DashboardContent(
                        viewModel = viewModel,
                        data = data,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    subdomain: String,
    onRefresh: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "Resumen Operativo",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                )
                Text(
                    text = "Subdominio: $subdomain",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

enum class DashboardSectionTab {
    Pagos, Vencimientos, Deudas, Distribucion
}

@Composable
fun DashboardContent(
    viewModel: ClientViewModel,
    data: DashboardData,
    context: Context
) {
    var selectedSection by remember { mutableStateOf(DashboardSectionTab.Pagos) }
    var showIncome by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_scroll"),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Ingresos (Moved to top)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp)
                    .testTag("kpi_income_card"),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Unspecified)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0F9D58),
                                    Color(0xFF0B8043)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.clickable { showIncome = !showIncome }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "INGRESOS DEL MES",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                                letterSpacing = 1.2.sp
                            )
                            Icon(
                                imageVector = if (showIncome) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        val displayTotal = if (showIncome) "$${data.ingresosMes ?: "0.00"}" else "$****"
                        Text(
                            text = displayTotal,
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimary,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Recaudado este mes de facturas cobradas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        }

        // 1. Sección de Estado Rápido (Badges Compactos)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickStatBadge(
                    title = "Activos",
                    value = data.clientesActivos ?: "0",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                QuickStatBadge(
                    title = "Suspendidos",
                    value = data.clientesSuspendidos ?: "0",
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
                QuickStatBadge(
                    title = "Adeudo",
                    value = data.clientesAdeudo ?: "0",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Cuadrícula de Accesos Directos (Grid Simétrico)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShortcutCard(
                        title = "Clientes",
                        icon = Icons.Rounded.People,
                        onClick = { viewModel.selectTab(HomeTab.Clientes) },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutCard(
                        title = "MikroTiks",
                        icon = Icons.Rounded.Router,
                        onClick = { viewModel.selectTab(HomeTab.Mikrotik) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShortcutCard(
                        title = "Suspendidos",
                        icon = Icons.Rounded.Block,
                        onClick = { viewModel.openSuspendidos(true) },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutCard(
                        title = "Tareas",
                        icon = Icons.Rounded.Assignment,
                        onClick = { viewModel.openTareas(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Horizontal Selection Chips for visual details lists
        item {
            Text(
                text = "Detalles y Monitoreo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    SectionChip(
                        selected = selectedSection == DashboardSectionTab.Pagos,
                        label = "Pagos Recientes",
                        icon = Icons.Rounded.TrendingUp,
                        onClick = { selectedSection = DashboardSectionTab.Pagos }
                    )
                }
                item {
                    SectionChip(
                        selected = selectedSection == DashboardSectionTab.Vencimientos,
                        label = "Próximos a Vencer",
                        icon = Icons.Rounded.NotificationsActive,
                        onClick = { selectedSection = DashboardSectionTab.Vencimientos }
                    )
                }
                item {
                    SectionChip(
                        selected = selectedSection == DashboardSectionTab.Deudas,
                        label = "Clientes Deuda",
                        icon = Icons.Rounded.Dangerous,
                        onClick = { selectedSection = DashboardSectionTab.Deudas }
                    )
                }
                item {
                    SectionChip(
                        selected = selectedSection == DashboardSectionTab.Distribucion,
                        label = "Distribución",
                        icon = Icons.Rounded.DashboardCustomize,
                        onClick = { selectedSection = DashboardSectionTab.Distribucion }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 4. Dynamic List Rendering using filter state
        when (selectedSection) {
            DashboardSectionTab.Pagos -> {
                val list = data.ultimosPagos ?: emptyList()
                if (list.isEmpty()) {
                    item { Box(modifier = Modifier.padding(horizontal = 16.dp)) { LoadingEmptyCard("No se reportan pagos recientes.") } }
                } else {
                    items(list) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PagoRowCard(item)
                        }
                    }
                }
            }
            DashboardSectionTab.Vencimientos -> {
                val list = data.proximosVencer ?: emptyList()
                if (list.isEmpty()) {
                    item { Box(modifier = Modifier.padding(horizontal = 16.dp)) { LoadingEmptyCard("No hay vencimientos programados.") } }
                } else {
                    items(list) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            VencimientoRowCard(item, context)
                        }
                    }
                }
            }
            DashboardSectionTab.Deudas -> {
                val list = data.clientesDeuda ?: emptyList()
                if (list.isEmpty()) {
                    item { Box(modifier = Modifier.padding(horizontal = 16.dp)) { LoadingEmptyCard("Felicidades! Ningún cliente con deuda activa.") } }
                } else {
                    items(list) { item ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            DeudaRowCard(item, context)
                        }
                    }
                }
            }
            DashboardSectionTab.Distribucion -> {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DistributionPanel(
                            paquetes = data.distribucionPaquetes ?: emptyList(),
                            routers = data.distribucionRouters ?: emptyList()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}


@Composable
fun SectionChip(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(FilterChipDefaults.IconSize)
            )
        },
        colors = FilterChipDefaults.filterChipColors(),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected)
    )
}

@Composable
fun PagoRowCard(pago: UltimoPago) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF4CAF50).copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pago.nombreCompleto ?: "Cliente Anónimo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pago.fechaPago ?: "Desconocido",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "+$${pago.montoPagado ?: "0.00"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF4CAF50)
            )
        }
    }
}

@Composable
fun VencimientoRowCard(
    vencer: ProximoVencer,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vencer.nombreCompleto ?: "Cliente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Plan: ${vencer.nombrePlan ?: "N/A"} • Vence: ${vencer.proximoPago ?: "N/D"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            val phone = vencer.telefono ?: ""
            if (phone.isNotEmpty()) {
                IconButton(
                    onClick = {
                        val originalMsg = "Hola ${vencer.nombreCompleto}, te recordamos que tu próximo pago del servicio de Internet con plan ${vencer.nombrePlan} vence el día ${vencer.proximoPago} con un costo de $${vencer.precio}. Gracias por tu preferencia!"
                        try {
                            val uri = Uri.parse("https://api.whatsapp.com/send?phone=52$phone&text=${Uri.encode(originalMsg)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo abrir WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Message,
                        contentDescription = "Contactar",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
fun DeudaRowCard(
    deuda: ClienteDeuda,
    context: Context
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ReportProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = deuda.nombreCompleto ?: "Cliente",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Atraso: ${deuda.mesesDeuda ?: 0} meses",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$${deuda.saldoActual ?: "0.00"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Plan: ${deuda.nombrePlan ?: "Sin plan"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )

                val phone = deuda.telefono ?: ""
                if (phone.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            val originalMsg = "Hola ${deuda.nombreCompleto}, le recordamos que presenta un adeudo de $${deuda.saldoActual} correspondiente a ${deuda.mesesDeuda} meses de servicio. Favor de regularizar su pago. Saludos!"
                            try {
                                val uri = Uri.parse("https://api.whatsapp.com/send?phone=52$phone&text=${Uri.encode(originalMsg)}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo abrir WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Message,
                            contentDescription = "WhatsApp",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cobrar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun DistributionPanel(
    paquetes: List<DistribucionPaquete>,
    routers: List<DistribucionRouter>
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Packages distribution block
            Column {
                Text(
                    text = "Distribución de Planes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                paquetes.forEach { paquete ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = paquete.nombre ?: "Plan Genérico",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "${paquete.total ?: 0} cls",
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))

            // Routers distribution block
            Column {
                Text(
                    text = "Equipos Conectados (Clientes)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                routers.forEach { router ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SettingsEthernet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = router.nombre ?: "Ruteador",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = "${router.total ?: "0"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))
                }
            }
        }
    }
}

@Composable
fun LoadingEmptyCard(hintText: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.04f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
