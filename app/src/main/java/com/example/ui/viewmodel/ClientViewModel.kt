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

sealed interface UiState<out T> {
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class HomeTab {
    Home, Clientes, Pagos, Ajustes, Mikrotik
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

    private val _selectedDeudaClient = MutableStateFlow<Client?>(null)
    val selectedDeudaClient: StateFlow<Client?> = _selectedDeudaClient.asStateFlow()

    fun selectClientForDeuda(client: Client?) {
        _selectedDeudaClient.value = client
    }

    private val _selectedPagoClient = MutableStateFlow<Client?>(null)
    val selectedPagoClient: StateFlow<Client?> = _selectedPagoClient.asStateFlow()

    fun selectClientForPago(client: Client?) {
        _selectedPagoClient.value = client
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

    init {
        viewModelScope.launch {
            // Fetch configuration or initialize with default
            var existingConfig = configDao.getConfig()
            if (existingConfig == null) {
                existingConfig = AppConfig()
                configDao.insertConfig(existingConfig)
            }
            _appConfig.value = existingConfig
            
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
}
