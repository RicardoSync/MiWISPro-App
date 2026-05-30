package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.rounded.Router
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SupervisorAccount
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Assignment
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AjustesScreen
import com.example.ui.screens.ClientesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.RegistrarPagoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PagosScreen
import com.example.ui.screens.MikrotiksScreen
import com.example.ui.screens.MikrotikDashboardScreen
import com.example.ui.screens.CortesAutomaticosScreen
import com.example.ui.screens.TareasScreen
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ClientViewModel
import com.example.ui.viewmodel.HomeTab
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
sealed interface AppScreen {
    object Main : AppScreen
    data class Detail(val client: com.example.data.Client) : AppScreen
    data class Pago(val client: com.example.data.Client) : AppScreen
    data class MikrotikDashboard(val routerId: String) : AppScreen
    data class MikrotikDhcpLeases(val routerId: String) : AppScreen

    object RegistrarCliente : AppScreen
    object Suspendidos : AppScreen
    object HistorialPagos : AppScreen
    object CortesAutomaticos : AppScreen
    object Tareas : AppScreen
    object Premisas : AppScreen
    object Logs : AppScreen
    data class FacturaDetail(val factura: com.example.data.FacturaData, val isNew: Boolean) : AppScreen
}

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: com.example.ui.viewmodel.ClientViewModel = viewModel()
        val appConfig by viewModel.appConfig.collectAsState()
        val isConfigLoaded by viewModel.isConfigLoaded.collectAsState()

        var isAuthenticated by remember { mutableStateOf(false) }
        var authError by remember { mutableStateOf<String?>(null) }

        val triggerBiometricAuth = {
          val executor = ContextCompat.getMainExecutor(this@MainActivity)
          val biometricPrompt = BiometricPrompt(this@MainActivity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
              override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                authError = "Identidad no verificada ($errString)"
              }

              override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthenticated = true
                authError = null
              }

              override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                authError = "Firma biométrica no coincide. Intente de nuevo."
              }
            })

          val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso Seguro MiWISPro")
            .setSubtitle("Utilice su huella dactilar o contraseña de dispositivo")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

          try {
            biometricPrompt.authenticate(promptInfo)
          } catch (e: Exception) {
            authError = "El dispositivo no soporta o tiene desactivada la seguridad."
          }
        }

        val needsSetup = appConfig.subdominio.isBlank() || appConfig.token.isBlank()
        val needsTerms = !needsSetup && !appConfig.termsAccepted

        LaunchedEffect(isConfigLoaded, needsSetup, needsTerms) {
          if (isConfigLoaded && !needsSetup && !needsTerms && !isAuthenticated) {
            triggerBiometricAuth()
          }
        }

        if (!isConfigLoaded) {
          Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
             CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        } else if (needsSetup) {
          com.example.ui.screens.SetupScreen(viewModel = viewModel)
        } else if (needsTerms) {
          com.example.ui.screens.TermsScreen(viewModel = viewModel)
        } else if (isAuthenticated) {
          MainAppContent(viewModel = viewModel)
        } else {
          LockOverlay(
            onAuthenticateClick = { triggerBiometricAuth() },
            errorMessage = authError
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: ClientViewModel = viewModel()) {
  val currentTab by viewModel.currentTab.collectAsState()
  val selectedDetailClient by viewModel.selectedDetailClient.collectAsState()
  val selectedPagoClient by viewModel.selectedPagoClient.collectAsState()
  val appConfig by viewModel.appConfig.collectAsState()

  // We need a state for selected router id in viewmodel, but since we navigate by creating state,
  // we can use a local state or add it to viewmodel. Let's add it to viewmodel.
  val selectedRouterId by viewModel.selectedRouterId.collectAsState()
  val selectedRouterIdForDhcpLeases by viewModel.selectedRouterIdForDhcpLeases.collectAsState()

  val navToRegistrar by viewModel.navigateToRegistrarCliente.collectAsState()
  val navToSuspendidos by viewModel.navigateToSuspendidos.collectAsState()
  val navToHistorial by viewModel.navigateToHistorialPagos.collectAsState()
  val navToCortes by viewModel.navigateToCortesAutomaticos.collectAsState()
  val navToTareas by viewModel.navigateToTareas.collectAsState()
  val navToPremisas by viewModel.navigateToPremisas.collectAsState()
  val navToLogs by viewModel.navigateToLogs.collectAsState()
  val selectedFacturaDetail by viewModel.selectedFacturaDetail.collectAsState()

  val screenState = remember(selectedDetailClient, selectedPagoClient, selectedRouterId, selectedRouterIdForDhcpLeases, navToRegistrar, navToSuspendidos, navToHistorial, navToCortes, navToTareas, navToPremisas, navToLogs, selectedFacturaDetail) {
    when {
      selectedFacturaDetail != null -> AppScreen.FacturaDetail(selectedFacturaDetail!!.first, selectedFacturaDetail!!.second)
      selectedDetailClient != null -> AppScreen.Detail(selectedDetailClient!!)
      selectedPagoClient != null -> AppScreen.Pago(selectedPagoClient!!)
      navToRegistrar -> AppScreen.RegistrarCliente
      selectedRouterIdForDhcpLeases != null -> AppScreen.MikrotikDhcpLeases(selectedRouterIdForDhcpLeases!!)
      selectedRouterId != null -> AppScreen.MikrotikDashboard(selectedRouterId!!)


      navToSuspendidos -> AppScreen.Suspendidos
      navToHistorial -> AppScreen.HistorialPagos
      navToCortes -> AppScreen.CortesAutomaticos
      navToTareas -> AppScreen.Tareas
      navToPremisas -> AppScreen.Premisas
      navToLogs -> AppScreen.Logs
      else -> AppScreen.Main
    }
  }

  var isTransitioning by remember { mutableStateOf(false) }
  LaunchedEffect(screenState, currentTab) {
      isTransitioning = true
      kotlinx.coroutines.delay(400) // Small transition delay to show spinner
      isTransitioning = false
  }

  Box(modifier = Modifier.fillMaxSize()) {
    AnimatedContent(
      targetState = screenState,
      transitionSpec = {
        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
      },
      label = "screen_routing"
    ) { targetScreen ->
      when (targetScreen) {
        is AppScreen.Detail -> {
          ClientDetailScreen(
            client = targetScreen.client,
            viewModel = viewModel,
            onBack = { viewModel.selectClientForDetail(null) }
          )
        }
        is AppScreen.MikrotikDashboard -> {
          MikrotikDashboardScreen(
            routerId = targetScreen.routerId,
            viewModel = viewModel,
            onBack = { viewModel.selectRouterForDashboard(null) }
          )
        }
        is AppScreen.MikrotikDhcpLeases -> {
          com.example.ui.screens.MikrotikDhcpLeasesScreen(
            routerId = targetScreen.routerId,
            viewModel = viewModel,
            onBack = { viewModel.selectRouterForDhcpLeases(null) }
          )
        }

        is AppScreen.Pago -> {
          RegistrarPagoScreen(
            client = targetScreen.client,
            viewModel = viewModel,
            onBack = { viewModel.selectClientForPago(null) }
          )
        }
        is AppScreen.RegistrarCliente -> {
          com.example.ui.screens.RegistrarClienteScreen(
            viewModel = viewModel,
            onBack = { viewModel.openRegistrarCliente(false) }
          )
        }
        is AppScreen.Suspendidos -> {
          com.example.ui.screens.ClientesSuspendidosScreen(
            viewModel = viewModel,
            onBack = { viewModel.openSuspendidos(false) }
          )
        }
        is AppScreen.HistorialPagos -> {
          com.example.ui.screens.HistorialPagosScreen(
            viewModel = viewModel,
            onBack = { viewModel.openHistorialPagos(false) }
          )
        }
        is AppScreen.CortesAutomaticos -> {
          CortesAutomaticosScreen(
            viewModel = viewModel,
            onBack = { viewModel.openCortesAutomaticos(false) }
          )
        }
        is AppScreen.Tareas -> {
          TareasScreen(
            viewModel = viewModel,
            onBack = { viewModel.openTareas(false) }
          )
        }
        is AppScreen.Premisas -> {
          com.example.ui.screens.PremisasScreen(
            viewModel = viewModel,
            onBack = { viewModel.openPremisas(false) }
          )
        }
        is AppScreen.Logs -> {
          com.example.ui.screens.LogsScreen(
            viewModel = viewModel,
            onBack = { viewModel.openLogs(false) }
          )
        }
        is AppScreen.FacturaDetail -> {
          com.example.ui.screens.FacturaDetailScreen(
            factura = targetScreen.factura,
            isNewCreation = targetScreen.isNew,
            viewModel = viewModel,
            onBack = { viewModel.openFacturaDetail(null) }
          )
        }
        is AppScreen.Main -> {
          val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
          val coroutineScope = rememberCoroutineScope()

          ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
              ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
              ) {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Header Area
                Box(
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                      brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                      )
                    )
                    .padding(16.dp),
                  contentAlignment = Alignment.BottomStart
                ) {
                  Column {
                    Box(
                      modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f))
                        .border(2.dp, MaterialTheme.colorScheme.onPrimary, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(36.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                      "Administrador",
                      style = MaterialTheme.typography.titleMedium,
                      color = MaterialTheme.colorScheme.onPrimary,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      appConfig.subdominio.ifEmpty { "miwispro.net" },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                  }
                }
                
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                  text = "Gestión",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.secondary,
                  modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )

                NavigationDrawerItem(
                  label = { Text("Clientes suspendidos") },
                  selected = false,
                  onClick = { 
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openSuspendidos(true)
                  },
                  icon = { Icon(Icons.Rounded.Block, contentDescription = null) },
                  modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                  label = { Text("Historial de pagos") },
                  selected = false,
                  onClick = { 
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openHistorialPagos(true)
                  },
                  icon = { Icon(Icons.Rounded.History, contentDescription = null) },
                  modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                  label = { Text("Cortes automáticos") },
                  selected = false,
                  onClick = { 
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openCortesAutomaticos(true)
                  },
                  icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
                  modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                  label = { Text("Tareas Automatizadas") },
                  selected = false,
                  onClick = { 
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openTareas(true)
                  },
                  icon = { Icon(Icons.Rounded.Assignment, contentDescription = null) },
                  modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                  label = { Text("Premisas") },
                  selected = false,
                  onClick = { 
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openPremisas(true)
                  },
                  icon = { Icon(Icons.Rounded.ReceiptLong, contentDescription = null) },
                  modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                  label = { Text("Logs del Sistema") },
                  selected = false,
                  onClick = { 
                    coroutineScope.launch { drawerState.close() }
                    viewModel.openLogs(true)
                  },
                  icon = { Icon(Icons.Rounded.List, contentDescription = null) },
                  modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )


                }
              }
            }
          ) {
            Scaffold(
              modifier = Modifier.fillMaxSize(),
              topBar = {
                TopAppBar(
                  navigationIcon = {
                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                      Icon(Icons.Rounded.Menu, contentDescription = "Menú")
                    }
                  },
                  title = {
                    Text(
                      text = when (currentTab) {
                        HomeTab.Home -> "Panel MiWISPro"
                        HomeTab.Clientes -> "Gestión de Clientes"
                        HomeTab.Mikrotik -> "MikroTik"
                        HomeTab.Ajustes -> "Ajustes de Sistema"
                        else -> "MiWISPro"
                      },
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.titleLarge
                    )
                  },
                  actions = {
                    IconButton(
                      onClick = { viewModel.loadClientes() },
                      modifier = Modifier
                        .padding(end = 8.dp)
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                        .testTag("action_sync_topbar")
                    ) {
                      Icon(Icons.Rounded.Refresh, contentDescription = "Sincronizar", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                  },
                  colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                  ),
                  modifier = Modifier.statusBarsPadding()
                )
              },
              bottomBar = {
                NavigationBar(
                  containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                ) {
                  val items = listOf(
                    NavigationItemData(tab = HomeTab.Home, selectedIcon = Icons.Rounded.Home, unselectedIcon = Icons.Outlined.Home, testTag = "nav_home", label = "Inicio"),
                    NavigationItemData(tab = HomeTab.Clientes, selectedIcon = Icons.Rounded.People, unselectedIcon = Icons.Outlined.People, testTag = "nav_clientes", label = "Clientes"),
                    NavigationItemData(tab = HomeTab.Global, selectedIcon = Icons.Rounded.Speed, unselectedIcon = Icons.Outlined.Speed, testTag = "nav_global", label = "Monitoreo"),
                    NavigationItemData(tab = HomeTab.Mikrotik, selectedIcon = Icons.Rounded.Router, unselectedIcon = Icons.Outlined.Router, testTag = "nav_mikrotik", label = "MikroTik"),
                    NavigationItemData(tab = HomeTab.Ajustes, selectedIcon = Icons.Rounded.Settings, unselectedIcon = Icons.Outlined.Settings, testTag = "nav_ajustes", label = "Ajustes")
                  )
                  items.forEach { item ->
                    NavigationBarItem(
                      selected = currentTab == item.tab,
                      onClick = { viewModel.selectTab(item.tab) },
                      icon = {
                        if (item.iconDrawableRes != null) {
                          Icon(painterResource(id = item.iconDrawableRes), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
                        } else if (item.selectedIcon != null && item.unselectedIcon != null) {
                          Icon(if (currentTab == item.tab) item.selectedIcon else item.unselectedIcon, contentDescription = null)
                        }
                      },
                      label = { 
                        Text(
                          text = item.label,
                          maxLines = 1,
                          style = MaterialTheme.typography.labelSmall
                        ) 
                      },
                      alwaysShowLabel = true
                    )
                  }
                }
              }
            ) { innerPadding ->
              when (currentTab) {
                HomeTab.Home -> DashboardScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                HomeTab.Clientes -> ClientesScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                HomeTab.Global -> com.example.ui.screens.GlobalConsumptionScreen(
                    viewModel = viewModel,
                    onBack = { viewModel.selectTab(HomeTab.Home) },
                    modifier = Modifier.padding(innerPadding)
                )
                HomeTab.Mikrotik -> MikrotiksScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                HomeTab.Ajustes -> AjustesScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
                else -> DashboardScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
              }
            }
          }
        }
      }
    }

    if (isTransitioning) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f))
          .pointerInput(Unit) {},
        contentAlignment = Alignment.Center
      ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
      }
    }
  }
}

private data class NavigationItemData(
  val tab: HomeTab,
  val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  val iconDrawableRes: Int? = null,
  val testTag: String,
  val label: String
)

@Composable
private fun LockOverlay(
    onAuthenticateClick: () -> Unit,
    errorMessage: String?
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(42.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Acceso Protegido",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "MIWIS Clientes requiere verificar su identidad para continuar de forma segura.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(44.dp))

            Button(
                onClick = onAuthenticateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text("Ingresar de forma Segura", fontWeight = FontWeight.Bold)
            }
        }
            }
        }
    }
}
