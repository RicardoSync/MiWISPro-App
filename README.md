# MiWISPro App 📱🚀

¡Bienvenido al repositorio de **MiWISPro App**! Esta es la aplicación móvil nativa para Android diseñada específicamente para la gestión, monitoreo y administración en tiempo real de la infraestructura WISP (Wireless Internet Service Provider). 

La aplicación se conecta de forma segura mediante HTTPS a la API de tu servidor **MiWISPro**, permitiendo a los administradores y técnicos gestionar clientes, registrar pagos y supervisar el estado de los routers MikroTik directamente desde el bolsillo.

---

## 🎯 Características Principales

* **Panel de Control Centralizado (Dashboard):** Acceso rápido y limpio a las métricas globales del sistema mediante un diseño simétrico optimizado.
* **Gestión de Clientes Nativa:** Listado y filtrado de clientes en tiempo real utilizando componentes de alto rendimiento (`LazyColumn`).
* **Perfil del Cliente Avanzado:** * Visualización de información detallada, parámetros de red y estados del servicio.
    * Indicador de consumo actual/promedio mediante gráficos circulares nativos (*Dial/Gauge*).
    * Historial de tráfico detallado implementando barras horizontales segmentadas (*Part-to-Whole / Stacked Bars*).
* **Monitoreo de MikroTik:** Dashboard dedicado para supervisar el uso de CPU, memoria RAM, Uptime y consumo de tráfico de tus routers en tiempo real.
* **Módulo de Finanzas:** Formulario compacto y directo para el registro rápido de pagos en campo, con menús desplegables nativos y validaciones integradas.
* **Diseño Ultra-Compacto:** Interfaz purificada bajo los estándares de **Material Design 3 (M3)** de Google, maximizando el espacio vertical y eliminando títulos duplicados para una experiencia fluida y profesional.

---

## 🛠️ Stack Tecnológico

* **Lenguaje:** [Kotlin](https://kotlinlang.org/) (100% Nativo)
* **Framework de UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Declarativo y moderno)
* **Sistema de Diseño:** Material Design 3 (M3)
* **Arquitectura:** MVVM (Model-View-ViewModel) con flujos de datos reactivos (`StateFlow`).
* **Redes y API:** [Retrofit 2](https://square.github.io/retrofit/) & OkHttp para la comunicación asíncrona segura vía HTTPS.
* **Inyección de Dependencias / Concurrencia:** Kotlin Coroutines & Asynchronous Tonal Styling.

---

## 📂 Estructura del Proyecto

La arquitectura del proyecto sigue una estructura limpia y desacoplada:

```text
app/src/main/java/com/example/
│
├── data/                  # Modelos de datos y Clientes de Red
│   ├── Client.kt          # Modelo de datos de los clientes
│   └── RetrofitClient.kt  # Configuración central de la API HTTP/HTTPS
│
├── ui/
│   ├── screens/           # Pantallas nativas de la aplicación (Jetpack Compose)
│   │   ├── DashboardScreen.kt
│   │   ├── ClientesScreen.kt
│   │   ├── ClientDetailScreen.kt
│   │   ├── RegistrarPagoScreen.kt
│   │   └── MikrotikDashboardScreen.kt
│   │
│   ├── theme/             # Configuración del Tema de Material 3 (Colores, Tipografías)
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   └── viewmodel/         # Lógica de negocio y retención de estado de la UI
│       └── ClientViewModel.kt
