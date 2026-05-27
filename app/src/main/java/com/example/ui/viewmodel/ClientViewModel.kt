package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Client
import com.example.data.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.AppConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HomeTab {
    Home, Clientes, Pagos, Ajustes, Mikrotik, Global
}

data class PaymentRecord(
    val id: String,
    val clientName: String,
    val amount: Double,
    val date: String,
    val concept: String,
    val paymentMethod: String // "Efectivo", "Transferencia", "Tarjeta"
)

class ClientViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val configDao = db.appConfigDao()

    private val _currentTab = MutableStateFlow(HomeTab.Home)
    val currentTab: StateFlow<HomeTab> = _currentTab.asStateFlow()

    private val _selectedDetailClient = MutableStateFlow<Client?>(null)
    val selectedDetailClient: StateFlow<Client?> = _selectedDetailClient.asStateFlow()

    fun selectClientForDetail(client: Client?) {
        _selectedDetailClient.value = client
    }

    private val _selectedRouterId = MutableStateFlow<String?>(null)
    val selectedRouterId: StateFlow<String?> = _selectedRouterId.asStateFlow()

    fun selectRouterForDashboard(routerId: String?) {
        _selectedRouterId.value = routerId
    }

    private val _selectedPagoClient = MutableStateFlow<Client?>(null)
    val selectedPagoClient: StateFlow<Client?> = _selectedPagoClient.asStateFlow()

    fun selectClientForPago(client: Client?) {
        _selectedPagoClient.value = client
    }

    private val _navigateToRegistrarCliente = MutableStateFlow(false)
    val navigateToRegistrarCliente: StateFlow<Boolean> = _navigateToRegistrarCliente.asStateFlow()

    fun openRegistrarCliente(open: Boolean) {
        _navigateToRegistrarCliente.value = open
        if (open) {
            loadDatosRegistro()
        }
    }

    private val _navigateToSuspendidos = MutableStateFlow(false)
    val navigateToSuspendidos: StateFlow<Boolean> = _navigateToSuspendidos.asStateFlow()

    fun openSuspendidos(open: Boolean) {
        _navigateToSuspendidos.value = open
        if (open) {
            loadClientesSuspendidos()
        }
    }

    private val _navigateToHistorialPagos = MutableStateFlow(false)
    val navigateToHistorialPagos: StateFlow<Boolean> = _navigateToHistorialPagos.asStateFlow()

    fun openHistorialPagos(open: Boolean) {
        _navigateToHistorialPagos.value = open
        if (open) {
            loadHistorialPagos()
        }
    }

    private val _uiState = MutableStateFlow<UiState<List<Client>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Client>>> = _uiState.asStateFlow()

    // Base client data
    private val _clientsList = MutableStateFlow<List<Client>>(emptyList())
    val clientsList: StateFlow<List<Client>> = _clientsList.asStateFlow()

    // Search queries
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Pagination Settings
    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _itemsPerPage = MutableStateFlow(5)
    val itemsPerPage: StateFlow<Int> = _itemsPerPage.asStateFlow()

    // Payment stats & ledger
    private val _payments = MutableStateFlow<List<PaymentRecord>>(emptyList())
    val payments: StateFlow<List<PaymentRecord>> = _payments.asStateFlow()

    private val _metodosPagoState = MutableStateFlow<UiState<com.example.data.MetodosPagoResponse>>(UiState.Loading)
    val metodosPagoState: StateFlow<UiState<com.example.data.MetodosPagoResponse>> = _metodosPagoState.asStateFlow()

    fun loadMetodosPago() {
        viewModelScope.launch {
            _metodosPagoState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getMetodosPago(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _metodosPagoState.value = UiState.Success(response)
                } else {
                    _metodosPagoState.value = UiState.Error("Error al cargar métodos de pago.")
                }
            } catch (e: Exception) {
                _metodosPagoState.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    // MikroTiks routers status flow
    private val _mikrotiksState = MutableStateFlow<UiState<com.example.data.MikrotiksResponse>>(UiState.Loading)
    val mikrotiksState: StateFlow<UiState<com.example.data.MikrotiksResponse>> = _mikrotiksState.asStateFlow()

    fun loadMikrotiks() {
        viewModelScope.launch {
            _mikrotiksState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getMikrotiks(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _mikrotiksState.value = UiState.Success(response)
                } else {
                    _mikrotiksState.value = UiState.Error("La API retornó success=false al consultar MikroTiks.")
                }
            } catch (e: Exception) {
                _mikrotiksState.value = UiState.Error("Error al conectar con la API: ${e.localizedMessage ?: "Verifique conexión"}")
            }
        }
    }

    // Dynamic Dashboard State System
    private val _dashboardState = MutableStateFlow<UiState<com.example.data.DashboardResponse>>(UiState.Loading)
    val dashboardState: StateFlow<UiState<com.example.data.DashboardResponse>> = _dashboardState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getDashboard(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _dashboardState.value = UiState.Success(response)
                } else {
                    _dashboardState.value = UiState.Error("La API retornó success=false al consultar el Dashboard.")
                }
            } catch (e: Exception) {
                _dashboardState.value = UiState.Error("Error de Red al conectar con la API: ${e.localizedMessage ?: "Verifique conexión"}")
            }
        }
    }

    // Local DB configurations
    private val _appConfig = MutableStateFlow(AppConfig())
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _isConfigLoaded = MutableStateFlow(false)
    val isConfigLoaded: StateFlow<Boolean> = _isConfigLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            // Fetch configuration or initialize with default
            var existingConfig = configDao.getConfig()
            if (existingConfig == null) {
                existingConfig = AppConfig()
                configDao.insertConfig(existingConfig)
            }
            _appConfig.value = existingConfig
            _isConfigLoaded.value = true
            
            // Initial payload loading with actual db credentials
            loadClientes()
            loadDashboard()
        }
    }

    fun selectTab(tab: HomeTab) {
        _currentTab.value = tab
        if (tab == HomeTab.Mikrotik) {
            loadMikrotiks()
        } else if (tab == HomeTab.Home) {
            loadDashboard()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        _currentPage.value = 1 // Reset pagination upon search
    }

    fun setCurrentPage(page: Int) {
        _currentPage.value = page
    }

    fun setItemsPerPage(count: Int) {
        _itemsPerPage.value = count
        _currentPage.value = 1
    }

    fun updateConfig(subdominio: String, token: String) {
        viewModelScope.launch {
            val updated = _appConfig.value.copy(subdominio = subdominio, token = token)
            configDao.insertConfig(updated)
            _appConfig.value = updated
            // Automatically refresh client payload with new settings
            loadClientes()
        }
    }

    fun acceptTerms() {
        viewModelScope.launch {
            val updated = _appConfig.value.copy(termsAccepted = true)
            configDao.insertConfig(updated)
            _appConfig.value = updated
        }
    }

    fun downloadTerms(context: Context) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse("https://demo.miwispro.net/controllers/TerminosController.php")
            val request = DownloadManager.Request(uri)
                .setTitle("Términos y Condiciones MiWISPro")
                .setDescription("Descargando archivo PDF")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Terminos_MiWISPro.pdf")
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadClientes() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getClientes(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success && response.data != null) {
                    _clientsList.value = response.data
                    _uiState.value = UiState.Success(response.data)
                    generateMockPayments(response.data)
                } else {
                    _uiState.value = UiState.Error("Respuesta de API inválida (" + (response.subdominio ?: "error") + ")")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Error al cargar clientes: ${e.localizedMessage ?: "Verifique conexión"}")
            }
        }
    }

    private fun generateMockPayments(clients: List<Client>) {
        val records = mutableListOf<PaymentRecord>()
        // Generate realistic payment history of a few active clients
        clients.take(12).forEachIndexed { index, client ->
            val price = (client.precioPaquete?.toDoubleOrNull() ?: 250.0) + 
                         (client.precioServicio?.toDoubleOrNull() ?: 0.0)
            if (price > 0 && client.activo == 1) {
                records.add(
                    PaymentRecord(
                        id = "PAG-${202600 + index}",
                        clientName = client.nombreCompleto ?: "Cliente ${client.id}",
                        amount = price,
                        date = "2026-05-${25 - (index % 5)}",
                        concept = "Mensualidad de ${client.nombrePaquete ?: "Plan de Internet"}",
                        paymentMethod = when (index % 3) {
                            0 -> "Efectivo"
                            1 -> "Transferencia"
                            else -> "Tarjeta"
                        }
                    )
                )
            }
        }
        _payments.value = records
    }

    private val _registrarClienteState = MutableStateFlow<UiState<com.example.data.RegistrarClienteResponse>?>(null)
    val registrarClienteState: StateFlow<UiState<com.example.data.RegistrarClienteResponse>?> = _registrarClienteState.asStateFlow()

    private val _datosRegistroState = MutableStateFlow<UiState<com.example.data.DatosRegistroResponse>>(UiState.Loading)
    val datosRegistroState: StateFlow<UiState<com.example.data.DatosRegistroResponse>> = _datosRegistroState.asStateFlow()

    fun loadDatosRegistro() {
        viewModelScope.launch {
            _datosRegistroState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getDatosRegistro(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _datosRegistroState.value = UiState.Success(response)
                } else {
                    _datosRegistroState.value = UiState.Error("No se pudieron cargar los datos de registro")
                }
            } catch (e: Exception) {
                _datosRegistroState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    fun resetRegistrarClienteState() {
        _registrarClienteState.value = null
    }

    private val _suspendidosState = MutableStateFlow<UiState<com.example.data.ClientesSuspendidosResponse>>(UiState.Loading)
    val suspendidosState: StateFlow<UiState<com.example.data.ClientesSuspendidosResponse>> = _suspendidosState.asStateFlow()

    fun loadClientesSuspendidos() {
        viewModelScope.launch {
            _suspendidosState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getClientesSuspendidos(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _suspendidosState.value = UiState.Success(response)
                } else {
                    _suspendidosState.value = UiState.Error("No se pudieron cargar los clientes suspendidos")
                }
            } catch (e: Exception) {
                _suspendidosState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    private val _historialPagosState = MutableStateFlow<UiState<com.example.data.HistorialPagosResponse>>(UiState.Loading)
    val historialPagosState: StateFlow<UiState<com.example.data.HistorialPagosResponse>> = _historialPagosState.asStateFlow()

    fun loadHistorialPagos(cliente: String? = null, fecha: String? = null) {
        viewModelScope.launch {
            _historialPagosState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getHistorialPagos(
                    token = config.token,
                    subdominio = config.subdominio,
                    cliente = cliente?.takeIf { it.isNotBlank() },
                    fecha = fecha?.takeIf { it.isNotBlank() }
                )
                if (response.success) {
                    _historialPagosState.value = UiState.Success(response)
                } else {
                    _historialPagosState.value = UiState.Error("No se pudo cargar el historial de pagos")
                }
            } catch (e: Exception) {
                _historialPagosState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    fun registrarClienteRemote(
        nombre: String,
        tel: String,
        idPaquete: Int,
        idMikrotik: Int,
        diaCorte: Int,
        proxPago: String,
        tipoConexion: String?,
        ipCliente: String?,
        pppoeUsuario: String?,
        pppoePassword: String?,
        coordenadas: String?,
        dir: String?,
        idServicioExtra: String?,
        dni: String?,
        promesaPago: String?
    ) {
        viewModelScope.launch {
            _registrarClienteState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.registrarCliente(
                    token = config.token,
                    subdominio = config.subdominio,
                    nombre = nombre,
                    tel = tel,
                    idPaquete = idPaquete,
                    idMikrotik = idMikrotik,
                    diaCorte = diaCorte,
                    proxPago = proxPago,
                    tipoConexion = tipoConexion,
                    ipCliente = ipCliente,
                    pppoeUsuario = pppoeUsuario,
                    pppoePassword = pppoePassword,
                    coordenadas = coordenadas,
                    dir = dir,
                    idServicioExtra = idServicioExtra,
                    dni = dni,
                    promesaPago = promesaPago
                )
                if (response.success) {
                    _registrarClienteState.value = UiState.Success(response)
                    loadClientes() // Reload to reflect the new client
                } else {
                    _registrarClienteState.value = UiState.Error(response.error ?: "La API retornó error.")
                }
            } catch (e: Exception) {
                _registrarClienteState.value = UiState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    // Toggle client status remotely calling MiwisPro Server APIs
    fun toggleClientStatusRemote(
        client: Client,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            val currentActive = client.activo ?: 1
            // 1 for suspending, 2 for activating
            val action = if (currentActive == 1) 1 else 2
            val config = _appConfig.value
            
            try {
                val response = RetrofitClient.apiService.toggleStatus(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = client.id,
                    accion = action
                )
                
                if (response.success) {
                    // Update in-memory state dynamically to sync without heavy database refresh
                    val updatedList = _clientsList.value.map { c ->
                        if (c.id == client.id) {
                            val newActive = if (action == 1) 0 else 1
                            val newEstado = if (newActive == 1) "1" else "2"
                            c.copy(activo = newActive, estado = newEstado)
                        } else {
                            c
                        }
                    }
                    _clientsList.value = updatedList
                    _uiState.value = UiState.Success(updatedList)
                    
                    onResult(true, response.message ?: "Operación exitosa")
                } else {
                    onResult(false, response.message ?: "La API retornó error")
                }
            } catch (e: Exception) {
                onResult(false, "Error: ${e.localizedMessage ?: "Consulte su conexión"}")
            }
        }
    }

    // Fetches individual client data from remote server
    fun getClientDetailRemote(
        clientId: String,
        onResult: (state: UiState<Client>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(UiState.Loading)
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getCliente(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = clientId
                )
                if (response.success && response.data != null) {
                    onResult(UiState.Success(response.data))
                } else {
                    onResult(UiState.Error("La API de Miwis no encontró el cliente."))
                }
            } catch (e: Exception) {
                onResult(UiState.Error("Error de Red: ${e.localizedMessage ?: "Intente de nuevo"}"))
            }
        }
    }

    // Fetches individual debt data from remote server
    fun getClientDeudaRemote(
        clientId: String,
        onResult: (state: UiState<com.example.data.DeudaResponse>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(UiState.Loading)
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.consultarDeuda(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = clientId
                )
                if (response.success) {
                    onResult(UiState.Success(response))
                } else {
                    onResult(UiState.Error("La API de MiWISPro no encontró información de deuda o retornó error."))
                }
            } catch (e: java.lang.Exception) {
                onResult(UiState.Error("Error de Red: ${e.localizedMessage ?: "Intente de nuevo"}"))
            }
        }
    }

    // Registers a payment on the remote server
    fun registrarPagoRemote(
        clientId: String,
        monto: String,
        metodo: String,
        referencia: String,
        onResult: (state: UiState<com.example.data.PagoResponse>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(UiState.Loading)
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.registrarPago(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = clientId,
                    monto = monto,
                    metodo = metodo,
                    referencia = referencia
                )
                if (response.success) {
                    onResult(UiState.Success(response))
                } else {
                    onResult(UiState.Error(response.mensaje ?: "La API de MiWISPro retornó error al registrar el pago."))
                }
            } catch (e: java.lang.Exception) {
                onResult(UiState.Error("Error de Red / API: ${e.localizedMessage ?: "Intente de nuevo"}"))
            }
        }
    }

    fun deleteClient(clientId: String): Boolean {
        val updated = _clientsList.value.filter { it.id != clientId }
        _clientsList.value = updated
        _uiState.value = UiState.Success(updated)
        return true
    }

    fun updateClient(updatedClient: Client): Boolean {
        val updated = _clientsList.value.map { client ->
            if (client.id == updatedClient.id) updatedClient else client
        }
        _clientsList.value = updated
        _uiState.value = UiState.Success(updated)
        return true
    }

    fun addPayment(payment: PaymentRecord) {
        _payments.value = listOf(payment) + _payments.value
    }

    // Fetches internet consumption for a specific client
    fun getConsumoClienteRemote(
        clientId: String,
        periodo: String = "hoy",
        minutos: Int = 5,
        onResult: (state: UiState<com.example.data.ConsumoClienteResponse>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(UiState.Loading)
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getConsumoCliente(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = clientId,
                    periodo = periodo,
                    minutos = minutos
                )
                if (response.success) {
                    onResult(UiState.Success(response))
                } else {
                    onResult(UiState.Error("La API de MiWISPro no encontró información de consumo."))
                }
            } catch (e: Exception) {
                onResult(UiState.Error("Error de Red al consultar consumo: ${e.localizedMessage ?: "Intente de nuevo"}"))
            }
        }
    }

    // Fetches stats for a specific Mikrotik router
    fun getMikrotikStatsRemote(
        routerId: String,
        onResult: (state: UiState<com.example.data.MikrotikStatsResponse>) -> Unit
    ) {
        viewModelScope.launch {
            onResult(UiState.Loading)
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getMikrotikStats(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = routerId
                )
                if (response.success) {
                    onResult(UiState.Success(response))
                } else {
                    onResult(UiState.Error("La API no pudo obtener las estadísticas del Mikrotik."))
                }
            } catch (e: Exception) {
                onResult(UiState.Error("Error de Red: ${e.localizedMessage ?: "Intente de nuevo"}"))
            }
        }
    }

    // Sync client to Mikrotik
    fun syncMikrotikRemote(
        clientId: String,
        onResult: (success: Boolean, message: String, detail: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.syncMikrotik(
                    token = config.token,
                    subdominio = config.subdominio,
                    id = clientId
                )
                if (response.success) {
                    onResult(true, response.mensaje ?: "Sincronizado exitosamente", response.detalleRouter)
                } else {
                    onResult(false, response.error ?: "Error al sincronizar", response.detalleRouter)
                }
            } catch (e: Exception) {
                onResult(false, "Error de red: ${e.localizedMessage ?: "Intente de nuevo"}", null)
            }
        }
    }

    private val _navigateToCortesAutomaticos = MutableStateFlow(false)
    val navigateToCortesAutomaticos: StateFlow<Boolean> = _navigateToCortesAutomaticos.asStateFlow()

    fun openCortesAutomaticos(open: Boolean) {
        _navigateToCortesAutomaticos.value = open
        if (open) {
            loadConfigCortes()
        }
    }

    private val _navigateToTareas = MutableStateFlow(false)
    val navigateToTareas: StateFlow<Boolean> = _navigateToTareas.asStateFlow()

    fun openTareas(open: Boolean) {
        _navigateToTareas.value = open
        if (open) {
            loadConfigAutomatizacion()
        }
    }

    private val _navigateToPremisas = MutableStateFlow(false)
    val navigateToPremisas: StateFlow<Boolean> = _navigateToPremisas.asStateFlow()

    private val _selectedFacturaDetail = MutableStateFlow<Pair<com.example.data.FacturaData, Boolean>?>(null)
    val selectedFacturaDetail: StateFlow<Pair<com.example.data.FacturaData, Boolean>?> = _selectedFacturaDetail.asStateFlow()

    fun openPremisas(open: Boolean) {
        _navigateToPremisas.value = open
        if (open) {
            loadFacturas()
        }
    }

    private val _facturasState = MutableStateFlow<UiState<com.example.data.FacturasResponse>>(UiState.Loading)
    val facturasState: StateFlow<UiState<com.example.data.FacturasResponse>> = _facturasState.asStateFlow()

    fun loadFacturas() {
        viewModelScope.launch {
            _facturasState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getFacturas(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _facturasState.value = UiState.Success(response)
                } else {
                    _facturasState.value = UiState.Error("Error al obtener las premisas/facturas")
                }
            } catch (e: Exception) {
                _facturasState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    fun anularFactura(
        idFactura: String,
        idCliente: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.anularFactura(
                    token = config.token,
                    subdominio = config.subdominio,
                    idFactura = idFactura,
                    idCliente = idCliente
                )
                if (response.success) {
                    onResult(true, response.message ?: "Factura anulada correctamente")
                    loadFacturas() // Reload facturas to reflect changes
                } else {
                    onResult(false, response.error ?: "Error al anular la factura")
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error de red")
            }
        }
    }

    fun crearFacturaManual(
        idCliente: String,
        onResult: (success: Boolean, message: String, data: com.example.data.CrearFacturaData?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.crearFactura(
                    token = config.token,
                    subdominio = config.subdominio,
                    idCliente = idCliente
                )
                if (response.success) {
                    onResult(true, response.message ?: "Factura generada con éxito", response.data)
                } else {
                    onResult(false, response.error ?: response.message ?: "Error al crear la factura", null)
                }
            } catch (e: Exception) {
                onResult(false, e.message ?: "Error de red", null)
            }
        }
    }

    fun openFacturaDetail(factura: com.example.data.FacturaData?, isNew: Boolean = false) {
        if (factura != null) {
            _selectedFacturaDetail.value = Pair(factura, isNew)
        } else {
            _selectedFacturaDetail.value = null
        }
    }

    private val _configCortesState = MutableStateFlow<UiState<com.example.data.ConfigCortesResponse>>(UiState.Loading)
    val configCortesState: StateFlow<UiState<com.example.data.ConfigCortesResponse>> = _configCortesState.asStateFlow()

    fun loadConfigCortes() {
        viewModelScope.launch {
            _configCortesState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getConfigCortes(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _configCortesState.value = UiState.Success(response)
                } else {
                    _configCortesState.value = UiState.Error(response.error ?: "Error al obtener configuración de cortes")
                }
            } catch (e: Exception) {
                _configCortesState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    fun updateConfigCortes(activo: Int?, horaEjecucion: String?, diasGracia: Int?) {
        viewModelScope.launch {
            _configCortesState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.updateConfigCortes(
                    token = config.token,
                    subdominio = config.subdominio,
                    activo = activo,
                    horaEjecucion = horaEjecucion,
                    diasGracia = diasGracia
                )
                if (response.success) {
                    _configCortesState.value = UiState.Success(response)
                } else {
                    _configCortesState.value = UiState.Error(response.error ?: "Error al actualizar configuración de cortes")
                }
            } catch (e: Exception) {
                _configCortesState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    private val _ejecutarCortesState = MutableStateFlow<UiState<com.example.data.EjecutarCortesResponse>?>(null)
    val ejecutarCortesState: StateFlow<UiState<com.example.data.EjecutarCortesResponse>?> = _ejecutarCortesState.asStateFlow()

    fun resetEjecutarCortesState() {
        _ejecutarCortesState.value = null
    }

    fun ejecutarCorteInmediato() {
        viewModelScope.launch {
            _ejecutarCortesState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.ejecutarCortes(
                    token = config.token,
                    subdominio = config.subdominio,
                    accion = 1
                )
                if (response.success) {
                    _ejecutarCortesState.value = UiState.Success(response)
                    loadClientes()
                } else {
                    _ejecutarCortesState.value = UiState.Error(response.mensaje ?: "Error al ejecutar cortes de emergencia")
                }
            } catch (e: Exception) {
                _ejecutarCortesState.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    private val _ejecutarActivacionesState = MutableStateFlow<UiState<com.example.data.EjecutarActivacionesResponse>?>(null)
    val ejecutarActivacionesState: StateFlow<UiState<com.example.data.EjecutarActivacionesResponse>?> = _ejecutarActivacionesState.asStateFlow()

    fun resetEjecutarActivacionesState() {
        _ejecutarActivacionesState.value = null
    }

    fun ejecutarActivacionInmediata() {
        viewModelScope.launch {
            _ejecutarActivacionesState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.ejecutarActivaciones(
                    token = config.token,
                    subdominio = config.subdominio,
                    accion = 1
                )
                if (response.success) {
                    _ejecutarActivacionesState.value = UiState.Success(response)
                    loadClientes()
                } else {
                    _ejecutarActivacionesState.value = UiState.Error(response.mensaje ?: "Error al ejecutar activaciones de emergencia")
                }
            } catch (e: Exception) {
                _ejecutarActivacionesState.value = UiState.Error(e.localizedMessage ?: "Error de red")
            }
        }
    }

    private val _configAutomatizacionState = MutableStateFlow<UiState<com.example.data.ConfigAutomatizacionResponse>>(UiState.Loading)
    val configAutomatizacionState: StateFlow<UiState<com.example.data.ConfigAutomatizacionResponse>> = _configAutomatizacionState.asStateFlow()

    fun loadConfigAutomatizacion() {
        viewModelScope.launch {
            _configAutomatizacionState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.getConfigAutomatizacion(
                    token = config.token,
                    subdominio = config.subdominio
                )
                if (response.success) {
                    _configAutomatizacionState.value = UiState.Success(response)
                } else {
                    _configAutomatizacionState.value = UiState.Error(response.error ?: "Error al obtener configuración de automatización")
                }
            } catch (e: Exception) {
                _configAutomatizacionState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

    fun updateConfigAutomatizacion(
        statsMikrotikActivo: Int?,
        monitorTraficoActivo: Int?,
        recordatorioPagoActivo: Int?,
        recordatorioCorteActivo: Int?,
        reporteDiarioActivo: Int?
    ) {
        viewModelScope.launch {
            _configAutomatizacionState.value = UiState.Loading
            try {
                val config = _appConfig.value
                val response = RetrofitClient.apiService.updateConfigAutomatizacion(
                    token = config.token,
                    subdominio = config.subdominio,
                    statsMikrotikActivo = statsMikrotikActivo,
                    monitorTraficoActivo = monitorTraficoActivo,
                    recordatorioPagoActivo = recordatorioPagoActivo,
                    recordatorioCorteActivo = recordatorioCorteActivo,
                    reporteDiarioActivo = reporteDiarioActivo
                )
                if (response.success) {
                    _configAutomatizacionState.value = UiState.Success(response)
                } else {
                    _configAutomatizacionState.value = UiState.Error(response.error ?: "Error al actualizar configuración de automatización")
                }
            } catch (e: Exception) {
                _configAutomatizacionState.value = UiState.Error(e.message ?: "Error de red")
            }
        }
    }

}
