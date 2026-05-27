package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface MiwisApiService {
    @GET("api/get_clientes.php")
    suspend fun getClientes(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): ClientApiResponse

    @GET("api/toggle_status.php")
    suspend fun toggleStatus(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String,
        @Query("accion") accion: Int
    ): ToggleStatusResponse

    @GET("api/get_cliente.php")
    suspend fun getCliente(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String
    ): SingleClientApiResponse

    @GET("api/consultar_deuda.php")
    suspend fun consultarDeuda(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String
    ): DeudaResponse

    @GET("api/registrar_pago.php")
    suspend fun registrarPago(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String,
        @Query("monto") monto: String,
        @Query("metodo") metodo: String,
        @Query("referencia") referencia: String
    ): PagoResponse

    @GET("api/get_mikrotiks.php")
    suspend fun getMikrotiks(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): MikrotiksResponse

    @GET("api/get_dashboard.php")
    suspend fun getDashboard(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): DashboardResponse

    @GET("api/get_consumo_cliente.php")
    suspend fun getConsumoCliente(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String,
        @Query("periodo") periodo: String? = null,
        @Query("minutos") minutos: Int? = null
    ): ConsumoClienteResponse

    @GET("api/get_mikrotik_stats.php")
    suspend fun getMikrotikStats(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String
    ): MikrotikStatsResponse

    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("api/registrar_cliente.php")
    suspend fun registrarCliente(
        @retrofit2.http.Field("token") token: String,
        @retrofit2.http.Field("subdominio") subdominio: String,
        @retrofit2.http.Field("nombre") nombre: String,
        @retrofit2.http.Field("tel") tel: String,
        @retrofit2.http.Field("id_paquete") idPaquete: Int,
        @retrofit2.http.Field("id_mikrotik") idMikrotik: Int,
        @retrofit2.http.Field("dia_corte") diaCorte: Int,
        @retrofit2.http.Field("prox_pago") proxPago: String,
        @retrofit2.http.Field("tipo_conexion") tipoConexion: String? = null,
        @retrofit2.http.Field("ip_cliente") ipCliente: String? = null,
        @retrofit2.http.Field("pppoe_usuario") pppoeUsuario: String? = null,
        @retrofit2.http.Field("pppoe_password") pppoePassword: String? = null,
        @retrofit2.http.Field("coordenadas") coordenadas: String? = null,
        @retrofit2.http.Field("dir") dir: String? = null,
        @retrofit2.http.Field("id_servicio_extra") idServicioExtra: String? = null,
        @retrofit2.http.Field("dni") dni: String? = null,
        @retrofit2.http.Field("promesa_pago") promesaPago: String? = null
    ): RegistrarClienteResponse

    @GET("api/get_datos_registro.php")
    suspend fun getDatosRegistro(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): DatosRegistroResponse

    @GET("api/get_clientes_suspendidos.php")
    suspend fun getClientesSuspendidos(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): ClientesSuspendidosResponse

    @GET("api/get_historial_pagos.php")
    suspend fun getHistorialPagos(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("cliente") cliente: String? = null,
        @Query("fecha") fecha: String? = null
    ): HistorialPagosResponse

    @GET("api/sync_mikrotik.php")
    suspend fun syncMikrotik(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String
    ): SyncMikrotikResponse

    @GET("api/config_cortes.php")
    suspend fun getConfigCortes(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): ConfigCortesResponse

    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("api/config_cortes.php")
    suspend fun updateConfigCortes(
        @retrofit2.http.Field("token") token: String,
        @retrofit2.http.Field("subdominio") subdominio: String,
        @retrofit2.http.Field("activo") activo: Int?,
        @retrofit2.http.Field("hora_ejecucion") horaEjecucion: String?,
        @retrofit2.http.Field("dias_gracia") diasGracia: Int?
    ): ConfigCortesResponse

    @GET("api/config_automatizacion.php")
    suspend fun getConfigAutomatizacion(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): ConfigAutomatizacionResponse

    @retrofit2.http.FormUrlEncoded
    @retrofit2.http.POST("api/config_automatizacion.php")
    suspend fun updateConfigAutomatizacion(
        @retrofit2.http.Field("token") token: String,
        @retrofit2.http.Field("subdominio") subdominio: String,
        @retrofit2.http.Field("stats_mikrotik_activo") statsMikrotikActivo: Int?,
        @retrofit2.http.Field("monitor_trafico_activo") monitorTraficoActivo: Int?,
        @retrofit2.http.Field("recordatorio_pago_activo") recordatorioPagoActivo: Int?,
        @retrofit2.http.Field("recordatorio_corte_activo") recordatorioCorteActivo: Int?,
        @retrofit2.http.Field("reporte_diario_activo") reporteDiarioActivo: Int?
    ): ConfigAutomatizacionResponse

    @GET("api/get_facturas.php")
    suspend fun getFacturas(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id_cliente") idCliente: Int? = null
    ): FacturasResponse

    @GET("api/anular_factura.php")
    suspend fun anularFactura(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id_factura") idFactura: String,
        @Query("id_cliente") idCliente: String
    ): AnularFacturaResponse

    @GET("api/crear_factura.php")
    suspend fun crearFactura(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id_cliente") idCliente: String
    ): CrearFacturaResponse

    @GET("api/get_metodos_pago.php")
    suspend fun getMetodosPago(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String
    ): MetodosPagoResponse

    @GET("api/ejecutar_cortes.php")
    suspend fun ejecutarCortes(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("accion") accion: Int = 1
    ): EjecutarCortesResponse

    @GET("api/ejecutar_activaciones.php")
    suspend fun ejecutarActivaciones(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("accion") accion: Int = 1
    ): EjecutarActivacionesResponse

    @GET("api/reiniciar_mikrotik.php")
    suspend fun reiniciarMikrotik(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String
    ): ReiniciarMikrotikResponse

    @GET("api/mikrotik_accion.php")
    suspend fun getMikrotikAccion(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String,
        @Query("accion") accion: Int = 5
    ): MikrotikAccionResponse

    @GET("api/mikrotik_accion.php")
    suspend fun getMikrotikTraffic(
        @Query("token") token: String,
        @Query("subdominio") subdominio: String,
        @Query("id") id: String,
        @Query("interfaz") interfaz: String,
        @Query("accion") accion: Int = 2
    ): MikrotikTrafficResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://miwispro.net/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val apiService: MiwisApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MiwisApiService::class.java)
    }
}
