package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.UiState

@Composable
fun AjustesScreen(
    viewModel: ClientViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val config by viewModel.appConfig.collectAsState()
    val isSyncing = uiState is UiState.Loading

    // Local input states bound to the database state flow
    var subdominioInput by remember(config) { mutableStateOf(config.subdominio) }
    var tokenInput by remember(config) { mutableStateOf(config.token) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // Card 1: Configuración del Servidor
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Configuración del Servidor",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
                        )

                        ListItem(
                            headlineContent = { Text("Credenciales de Acceso") },
                            supportingContent = { Text("Configura el subdominio y el token para la API de MiWISPro.") },
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        // Form for Subdominio and Token
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            OutlinedTextField(
                                value = subdominioInput,
                                onValueChange = { subdominioInput = it },
                                label = { Text("Subdominio") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = tokenInput,
                                onValueChange = { tokenInput = it },
                                label = { Text("Token ID") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            val hasChanges = subdominioInput != config.subdominio || tokenInput != config.token
                            Button(
                                onClick = {
                                    if (subdominioInput.isNotBlank() && tokenInput.isNotBlank()) {
                                        viewModel.updateConfig(subdominioInput.trim(), tokenInput.trim())
                                        Toast.makeText(context, "¡Configuración guardada en Base de Datos local!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Los campos no pueden estar vacíos", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = hasChanges,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                )
                            ) {
                                Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardar Cambios")
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Forzar Sincronización") },
                            supportingContent = { Text("Actualizar base de datos local.") },
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            },
                            trailingContent = {
                                if (isSyncing) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            modifier = Modifier.clickable(enabled = !isSyncing) {
                                viewModel.loadClientes()
                                Toast.makeText(context, "Sincronizando con Miwis Pro...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Card 2: Preferencias de la App
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Preferencias de la App",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 0.dp)
                        )

                        ListItem(
                            headlineContent = { Text("Términos y Condiciones") },
                            supportingContent = { Text("Revisar y descargar documento legal.") },
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.Assignment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            },
                            trailingContent = {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            },
                            modifier = Modifier.clickable {
                                viewModel.downloadTerms(context)
                                Toast.makeText(context, "Descarga iniciada...", Toast.LENGTH_SHORT).show()
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                        ListItem(
                            headlineContent = { Text("Software de Gestión") },
                            supportingContent = { Text("Miwis Pro WEB by Software Escobedo.") },
                            leadingContent = {
                                Icon(
                                    Icons.Rounded.DeveloperMode,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        }
    }
}
