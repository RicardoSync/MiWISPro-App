package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LogItem(
    val id: String?,
    @Json(name = "script_name") val scriptName: String?,
    val subdominio: String?,
    @Json(name = "fecha_ejecucion") val fechaEjecucion: String?,
    val status: String?, // "exito", "error", "parcial"
    @Json(name = "total_procesados") val totalProcesados: String?,
    val detalles: String?,
    @Json(name = "duracion_segundos") val duracionSegundos: String?
)

@JsonClass(generateAdapter = true)
data class LogsResponse(
    val success: Boolean,
    val subdominio: String?,
    @Json(name = "total_categorias") val totalCategorias: Int?,
    val data: Map<String, List<LogItem>>?
)
