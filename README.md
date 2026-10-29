# MediLima
Aplicación Android en Kotlin para localizar el centro médico más cercano según especialidad.
Base de datos: **Firebase Realtime Database** (plan Spark — 100% gratuito, sin tarjeta).

---

## Estructura del proyecto

```
MediLima/
├── app/src/main/
│   ├── java/com/example/medilima/
│   │   ├── model/
│   │   │   ├── CentroMedico.kt        ← id, nombre, dirección, coords, imagenBase64, especialidades (Map), horarios
│   │   │   ├── Especialidad.kt        ← id, nombre, descripcion, icono
│   │   │   ├── HorarioAtencion.kt     ← dia, apertura, cierre
│   │   │   └── RouteResponse.kt       ← Modelo Retrofit (OpenRouteService)
│   │   ├── repository/
│   │   │   ├── FirebaseRepository.kt  ← CRUD Realtime Database + compresión Base64
│   │   │   ├── ApiService.kt          ← Interface Retrofit
│   │   │   └── RetrofitClient.kt      ← Singleton Retrofit
│   │   ├── adapter/
│   │   │   ├── CentroMedicoAdapter.kt ← RecyclerView, decodifica Base64 para imagen
│   │   │   └── EspecialidadAdapter.kt ← RecyclerView especialidades
│   │   └── ui/
│   │       ├── main/
│   │       │   ├── MainActivity.kt    ← Mapa + spinner de especialidad → ruta automática
│   │       │   └── MainViewModel.kt   ← Lógica MVVM
│   │       ├── centromedico/
│   │       │   ├── ListCentroMedicoActivity.kt
│   │       │   └── FormCentroMedicoActivity.kt
│   │       └── especialidad/
│   │           ├── ListEspecialidadActivity.kt
│   │           └── FormEspecialidadActivity.kt
│   └── res/layout/  menu/  drawable/  values/
```

---

## Pasos de configuración (OBLIGATORIOS)

### 1. Firebase Realtime Database (GRATIS — plan Spark)

1. Ir a [console.firebase.google.com](https://console.firebase.google.com)
2. Crear proyecto → **"MediLima"**
3. En el menú lateral: **Realtime Database** → Crear base de datos
4. Elegir región: `us-central1` (o la más cercana)
5. Iniciar en **modo de prueba** (permite leer/escribir durante 30 días, suficiente para el proyecto)
6. Agregar app Android con package `com.example.centrosmedicos`
7. Descargar `google-services.json` → colocarlo en `app/`

**Reglas de Realtime Database (modo prueba — ya viene configurado):**
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

**Estructura JSON en RTDB que crea la app automáticamente:**
```
medilima-default-rtdb/
├── especialidades/
│   ├── -ABC123/
│   │   ├── nombre: "Cardiología"
│   │   ├── descripcion: "Enfermedades del corazón"
│   │   └── icono: "❤️"
│   └── ...
└── centros_medicos/
    ├── -XYZ789/
    │   ├── nombre: "Hospital Rebagliati"
    │   ├── latitud: -12.0781
    │   ├── longitud: -77.0367
    │   ├── imagenBase64: "..."   ← imagen comprimida (máx ~50KB)
    │   ├── especialidades/
    │   │   ├── -ABC123: true
    │   │   └── -DEF456: true
    │   └── horarios/
    │       ├── 0: { dia: "Lunes", apertura: "08:00", cierre: "20:00" }
    │       └── ...
    └── ...
```

### 2. Google Maps API Key

1. Ir a [console.cloud.google.com](https://console.cloud.google.com)
2. Habilitar **Maps SDK for Android**
3. Crear API Key
4. Editar `app/secrets.properties`:
   ```
   GOOGLE_MAPS_API_KEY=AIzaSy...TU_CLAVE_AQUI
   ```

### 3. OpenRouteService API Key (rutas con tráfico, gratis)

1. Registrarse en [openrouteservice.org](https://openrouteservice.org/dev/#/login) — completamente gratuito
2. Crear token de API
3. En `MainViewModel.kt` reemplazar:
   ```kotlin
   private val ORS_API_KEY = "TU_ORS_API_KEY_AQUI"
   ```

---

## Cómo funciona la imagen de fachada (sin Firebase Storage)

Al agregar/editar un centro médico:
1. El usuario selecciona la imagen desde la **galería del dispositivo**
2. La app la **redimensiona a máx 400×400px** y la **comprime al 50% JPEG**
3. La convierte a **Base64** y la guarda directamente en el nodo de Realtime Database
4. Tamaño resultante: ~30–60KB por imagen, bien dentro del límite de 1GB del plan gratuito

Para mostrarla, se decodifica el Base64 a Bitmap en el adaptador y en la pantalla principal.

---

## Flujo de la app

### Pantalla Principal
- Muestra mapa de Lima con la ubicación del usuario (marker azul)
- **Spinner** de especialidades → al seleccionar:
  - Coloca **markers rojos** en todos los centros con esa especialidad
  - Calcula la **ruta más corta** (considerando tráfico vía ORS) al más cercano
  - Dibuja la ruta en **azul**
  - Muestra card con nombre, dirección, teléfono, horarios e imagen

### Menú (3 puntos en toolbar)
- **🏥 Centros Médicos** → lista RecyclerView → agregar / editar / eliminar
- **⚕️ Especialidades** → lista RecyclerView → agregar / editar / eliminar

### Datos iniciales
Al primer arranque, `seedIfEmpty()` puebla Realtime Database con:
- **7 especialidades**: Cardiología, Hepatología, Traumatología, Neurología, Emergencia General, Neumología, Gastroenterología
- **8 centros reales de Lima**: Hospital Rebagliati, Loayza, Anglo Americana, Internacional, María Auxiliadora, San Borja, Hipólito Unanue, Ricardo Palma

---

## Tecnologías

| Tecnología | Uso |
|---|---|
| Kotlin | Lenguaje principal |
| Google Maps SDK | Mapa, markers, polylines |
| FusedLocationProvider | GPS del usuario |
| OpenRouteService API | Rutas con tráfico real (driving-car) |
| **Firebase Realtime Database** | **Base de datos en la nube (GRATIS)** |
| **Base64 + compresión Bitmap** | **Imágenes de fachada sin Storage** |
| Retrofit + OkHttp | Llamadas a la API de rutas |
| RecyclerView | Listas de centros y especialidades |
| ViewModel + LiveData | Arquitectura MVVM |
| Coroutines | Operaciones asíncronas |
| ViewBinding | Acceso a vistas sin findViewById |

---

## Límites del plan gratuito Firebase Realtime Database (Spark)

| Recurso | Límite gratuito |
|---|---|
| Almacenamiento | 1 GB |
| Descarga/mes | 10 GB |
| Conexiones simultáneas | 100 |

Para un proyecto académico con <100 centros médicos e imágenes comprimidas, esto es más que suficiente.
