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
