package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Client(
    val id: String,
    @Json(name = "nombre_completo") val nombreCompleto: String?,
    @Json(name = "dni_rfc") val dniRfc: String?,
    val telefono: String?,
    val direccion: String?,
    @Json(name = "ip_cliente") val ipCliente: String?,
    val coordenadas: String?,
    @Json(name = "id_paquete") val idPaquete: String?,
    @Json(name = "id_servicio_extra") val idServicioExtra: String?,
    @Json(name = "id_mikrotik") val idMikrotik: String?,
    @Json(name = "tipo_conexion") val tipoConexion: String?, // "pppoe" / "estatica"
    @Json(name = "pppoe_usuario") val pppoeUsuario: String?,
    @Json(name = "pppoe_password") val pppoePassword: String?,
    @Json(name = "dia_corte") val diaCorte: String?,
    @Json(name = "proximo_pago") val proximoPago: String?,
    @Json(name = "promesa_pago_hasta") val promesaPagoHasta: String?,
    val estado: String?, // "1" = Activo, "2" = Cortado/Suspendido, etc.
    val activo: Int?, // 1 or 0
    @Json(name = "nombre_paquete") val nombrePaquete: String?,
    @Json(name = "nombre_mikrotik") val nombreMikrotik: String?,
    @Json(name = "precio_paquete") val precioPaquete: String?,
    @Json(name = "precio_servicio") val precioServicio: String?,
    @Json(name = "nombre_servicio") val nombreServicio: String?,
    @Json(name = "filtro_adultos") val filtroAdultos: Int?,
    @Json(name = "saldo_actual") val saldoActual: String?,
    @Json(name = "ip_address") val ipAddress: String?,
    @Json(name = "puerto_api") val puertoApi: Int?,
    @Json(name = "velocidad_subida") val velocidadSubida: String?,
    @Json(name = "velocidad_bajada") val velocidadBajada: String?,
    @Json(name = "burst_limit") val burstLimit: String?,
    @Json(name = "burst_threshold") val burstThreshold: String?,
    @Json(name = "burst_time") val burstTime: String?,
    @Json(name = "limit_at") val limitAt: String?
)

@JsonClass(generateAdapter = true)
data class ClientApiResponse(
    val success: Boolean,
    val subdominio: String?,
    @Json(name = "total_clientes") val totalClientes: Int?,
    val data: List<Client>?
)

@JsonClass(generateAdapter = true)
data class SingleClientApiResponse(
    val success: Boolean,
    val subdominio: String?,
    val data: Client?
)

@JsonClass(generateAdapter = true)
data class ToggleStatusResponse(
    val success: Boolean,
    val message: String?
)

@JsonClass(generateAdapter = true)
data class FacturaDetalle(
    @Json(name = "id_factura") val idFactura: Int?,
    val descripcion: String?,
    @Json(name = "fecha_vencimiento") val fechaVencimiento: String?,
    @Json(name = "monto_pendiente") val montoPendiente: String?,
    val estado: String?
)

@JsonClass(generateAdapter = true)
data class DeudaData(
    @Json(name = "id_cliente") val idCliente: Int?,
    val nombre: String?,
    @Json(name = "estado_servicio") val estadoServicio: String?,
    @Json(name = "costo_mensual") val costoMensual: String?,
    @Json(name = "total_a_pagar") val totalAPagar: String?,
    @Json(name = "fecha_vencimiento") val fechaVencimiento: String?,
    @Json(name = "facturas_pendientes") val facturasPendientes: Int?,
    @Json(name = "detalle_facturas") val detalleFacturas: List<FacturaDetalle>?
)

@JsonClass(generateAdapter = true)
data class DeudaResponse(
    val success: Boolean,
    val subdominio: String?,
    val data: DeudaData?
)

@JsonClass(generateAdapter = true)
data class PagoData(
    @Json(name = "id_pago") val idPago: Int?,
    @Json(name = "url_recibo_pdf") val urlReciboPdf: String?,
    @Json(name = "monto_pagado") val montoPagado: String?,
    @Json(name = "nuevo_saldo") val nuevoSaldo: String?,
    @Json(name = "proximo_pago") val proximoPago: String?,
    @Json(name = "internet_reactivado") val internetReactivado: Boolean?,
    @Json(name = "whatsapp_enviado") val whatsappEnviado: Boolean?
)

@JsonClass(generateAdapter = true)
data class PagoResponse(
    val success: Boolean,
    val mensaje: String?,
    val data: PagoData?
)

@JsonClass(generateAdapter = true)
data class RouterData(
    val id: String?,
    val nombre: String?,
    @Json(name = "ip_address") val ipAddress: String?,
    @Json(name = "usuario_api") val usuarioApi: String?,
    @Json(name = "puerto_api") val puertoApi: String?,
    val activo: String?,
    @Json(name = "numero_clientes") val numeroClientes: String?,
    @Json(name = "is_online") val isOnline: Boolean?,
    @Json(name = "estado_red") val estadoRed: String?
)

@JsonClass(generateAdapter = true)
data class MikrotiksResponse(
    val success: Boolean,
    val subdominio: String?,
    @Json(name = "total_routers") val totalRouters: Int?,
    val data: List<RouterData>?
)

@JsonClass(generateAdapter = true)
data class HistorialIngreso(
    val mes: String?,
    val total: String?
)

@JsonClass(generateAdapter = true)
data class UltimoPago(
    @Json(name = "nombre_completo") val nombreCompleto: String?,
    @Json(name = "monto_pagado") val montoPagado: String?,
    @Json(name = "fecha_pago") val fechaPago: String?
)

@JsonClass(generateAdapter = true)
data class ProximoVencer(
    @Json(name = "nombre_completo") val nombreCompleto: String?,
    val telefono: String?,
    @Json(name = "proximo_pago") val proximoPago: String?,
    @Json(name = "nombre_plan") val nombrePlan: String?,
    val precio: String?
)

@JsonClass(generateAdapter = true)
data class DistribucionPaquete(
    val nombre: String?,
    val total: String?
)

@JsonClass(generateAdapter = true)
data class DistribucionRouter(
    val nombre: String?,
    val total: String?
)

@JsonClass(generateAdapter = true)
data class ClienteMapa(
    @Json(name = "nombre_completo") val nombreCompleto: String?,
    val coordenadas: String?,
    val estado: String?,
    @Json(name = "ip_cliente") val ipCliente: String?
)

@JsonClass(generateAdapter = true)
data class ClienteDeuda(
    @Json(name = "nombre_completo") val nombreCompleto: String?,
    val telefono: String?,
    @Json(name = "proximo_pago") val proximoPago: String?,
    @Json(name = "precio_paquete") val precioPaquete: String?,
    @Json(name = "nombre_plan") val nombrePlan: String?,
    @Json(name = "saldo_actual") val saldoActual: String?,
    @Json(name = "meses_deuda") val mesesDeuda: Int?,
    @Json(name = "monto_deuda") val montoDeuda: Double?
)

@JsonClass(generateAdapter = true)
data class ClienteSuspendidoLista(
    @Json(name = "nombre_completo") val nombreCompleto: String?,
    val telefono: String?,
    @Json(name = "proximo_pago") val proximoPago: String?,
    @Json(name = "precio_paquete") val precioPaquete: String?,
    @Json(name = "nombre_plan") val nombrePlan: String?
)

@JsonClass(generateAdapter = true)
data class DashboardData(
    @Json(name = "ingresos_mes") val ingresosMes: String?,
    @Json(name = "clientes_activos") val clientesActivos: String?,
    @Json(name = "clientes_suspendidos") val clientesSuspendidos: String?,
    @Json(name = "clientes_adeudo") val clientesAdeudo: String?,
    @Json(name = "clientes_cancelados") val clientesCancelados: String?,
    @Json(name = "historial_ingresos") val historialIngresos: List<HistorialIngreso>?,
    @Json(name = "ultimos_pagos") val ultimosPagos: List<UltimoPago>?,
    @Json(name = "proximos_vencer") val proximosVencer: List<ProximoVencer>?,
    @Json(name = "distribucion_paquetes") val distribucionPaquetes: List<DistribucionPaquete>?,
    @Json(name = "distribucion_routers") val distribucionRouters: List<DistribucionRouter>?,
    @Json(name = "clientes_mapa") val clientesMapa: List<ClienteMapa>?,
    @Json(name = "clientes_deuda") val clientesDeuda: List<ClienteDeuda>?,
    @Json(name = "clientes_suspendidos_lista") val clientesSuspendidosLista: List<ClienteSuspendidoLista>?,
    @Json(name = "clientes_proximos_corte") val clientesProximosCorte: List<ProximoVencer>?
)

@JsonClass(generateAdapter = true)
data class DashboardResponse(
    val success: Boolean,
    val subdominio: String?,
    val data: DashboardData?
)

