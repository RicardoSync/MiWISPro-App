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
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Hardware
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AjustesScreen
import com.example.ui.screens.ClientesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ClientDetailScreen
import com.example.ui.screens.DeudaDetailScreen
import com.example.ui.screens.RegistrarPagoScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PagosScreen
import com.example.ui.screens.MikrotiksScreen
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

sealed interface AppScreen {
    object Main : AppScreen
    data class Detail(val client: com.example.data.Client) : AppScreen
    data class Deuda(val client: com.example.data.Client) : AppScreen
    data class Pago(val client: com.example.data.Client) : AppScreen
}

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
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

        LaunchedEffect(Unit) {
          triggerBiometricAuth()
        }

        if (isAuthenticated) {
          MainAppContent()
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
  val selectedDeudaClient by viewModel.selectedDeudaClient.collectAsState()
  val selectedPagoClient by viewModel.selectedPagoClient.collectAsState()

  val screenState = remember(selectedDetailClient, selectedDeudaClient, selectedPagoClient) {
    when {
      selectedDetailClient != null -> AppScreen.Detail(selectedDetailClient!!)
      selectedDeudaClient != null -> AppScreen.Deuda(selectedDeudaClient!!)
      selectedPagoClient != null -> AppScreen.Pago(selectedPagoClient!!)
      else -> AppScreen.Main
    }
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
        is AppScreen.Deuda -> {
          DeudaDetailScreen(
            client = targetScreen.client,
            viewModel = viewModel,
            onBack = { viewModel.selectClientForDeuda(null) }
          )
        }
        is AppScreen.Pago -> {
          RegistrarPagoScreen(
            client = targetScreen.client,
            viewModel = viewModel,
            onBack = { viewModel.selectClientForPago(null) }
          )
        }
        is AppScreen.Main -> {
          Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
              TopAppBar(
                title = {
                  Text(
                    text = when (currentTab) {
                      HomeTab.Home -> "Panel Ejecutivo"
                      HomeTab.Clientes -> "Clientes MiWISPro"
                      HomeTab.Mikrotik -> "Ruteadores"
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
                      .padding(end = 12.dp)
                      .size(40.dp)
                      .background(MaterialTheme.colorScheme.primaryContainer, androidx.compose.foundation.shape.CircleShape)
                      .let { 
                        it.border(
                          width = 1.dp,
                          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                          shape = androidx.compose.foundation.shape.CircleShape
                        )
                      }
                      .testTag("action_sync_topbar")
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Refresh,
                      contentDescription = "Sincronizar",
                      tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
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
                modifier = Modifier.testTag("bottom_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 0.dp
              ) {
                val items = listOf(
                  NavigationItemData(tab = HomeTab.Home,  selectedIcon = Icons.Rounded.Home, unselectedIcon = Icons.Outlined.Home, testTag = "nav_home"),
                  NavigationItemData(tab = HomeTab.Clientes, selectedIcon = Icons.Rounded.People, unselectedIcon = Icons.Outlined.People, testTag = "nav_clientes"),
                  NavigationItemData(tab = HomeTab.Mikrotik, iconDrawableRes = R.drawable.ic_tux, testTag = "nav_mikrotik"),
                  NavigationItemData(tab = HomeTab.Ajustes, selectedIcon = Icons.Rounded.Settings, unselectedIcon = Icons.Outlined.Settings, testTag = "nav_ajustes")
                )

                items.forEach { item ->
                  val isSelected = currentTab == item.tab
                  NavigationBarItem(
                    selected = isSelected,
                    onClick = { viewModel.selectTab(item.tab) },
                    colors = NavigationBarItemDefaults.colors(
                      selectedIconColor = MaterialTheme.colorScheme.primary,
                      selectedTextColor = MaterialTheme.colorScheme.primary,
                      unselectedIconColor = MaterialTheme.colorScheme.secondary,
                      unselectedTextColor = MaterialTheme.colorScheme.secondary,
                      indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    icon = {
                      val contentDesc = when(item.tab) {
                          HomeTab.Home -> "Inicio"
                          HomeTab.Clientes -> "Clientes"
                          HomeTab.Mikrotik -> "Ruteador"
                          HomeTab.Ajustes -> "Ajustes"
                          else -> "General"
                      }
                      if (item.iconDrawableRes != null) {
                        Icon(
                          painter = painterResource(id = item.iconDrawableRes),
                          contentDescription = contentDesc,
                          tint = Color.Unspecified,
                          modifier = Modifier.size(24.dp)
                        )
                      } else if (item.selectedIcon != null && item.unselectedIcon != null) {
                        Icon(
                          imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                          contentDescription = contentDesc
                        )
                      }
                    },
                    alwaysShowLabel = false,
                    modifier = Modifier.testTag(item.testTag)
                  )
                }
              }
            }
          ) { innerPadding ->
            when (currentTab) {
              HomeTab.Home -> DashboardScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
              HomeTab.Clientes -> ClientesScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
              HomeTab.Mikrotik -> MikrotiksScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
              HomeTab.Ajustes -> AjustesScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
              else -> DashboardScreen(viewModel = viewModel, modifier = Modifier.padding(innerPadding))
            }
          }
        }
      }
    }
  }
}

private data class NavigationItemData(
  val tab: HomeTab,
  val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
  val iconDrawableRes: Int? = null,
  val testTag: String
)

@Composable
private fun LockOverlay(
    onAuthenticateClick: () -> Unit,
    errorMessage: String?
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
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
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ingresar de forma Segura", fontWeight = FontWeight.Black)
            }
        }
    }
}
