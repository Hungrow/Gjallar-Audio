# 🛡️ Gjallarhorn — Gjallar Audio Suite & Sleep
> **Ecosistema Heimdall** — Navaja suiza de sonido, descanso inteligente, audios míticos y temporizador de apagado para Android.

---

## 🌌 Visión
En la mitología nórdica, el **Gjallarhorn** es el cuerno custodiado por **Heimdall**, cuyo sonido resuena por los nueve reinos. **Gjallar Audio** traslada ese espíritu al dispositivo móvil: una suite integral con diseño Dark Slate nórdico, optimizada para rendimiento a 60/120 fps y compatible desde Android 8.0 hasta Android 14+.

---

## 🚀 Módulos Principales

### 1. 🌙 Gjallar Night (Descanso, Sleep Timer & Ciclos REM)
* **Temporizador de Apagado (Sleep Timer Pro):**
  * **Atenuación suave virtual:** Al llegar a 0, reduce progresivamente el volumen durante el siguiente minuto hasta el silencio y pausa la reproducción. Al finalizar, restaura virtualmente el volumen del sistema para dejarlo **exactamente como lo tenía el usuario**.
  * **Apagado de Pantalla (Modo Sleep Timer):** Integración con Administrador de Dispositivos (`DevicePolicyManager.lockNow()`) para bloquear y apagar la pantalla automáticamente al concluir el temporizador.
  * **Cuádruple corte universal de audio:** Detiene reproductores externos (Spotify, YouTube ReVanced, YouTube Music, podcasts, audiolibros) mediante eventos multimedia por hardware, pausa directa de `MediaSession` y adquisición de foco de audio exclusivo.
  * **Servicio en primer plano con WakeLock:** Mantiene el conteo exacto en la barra de estado incluso con la pantalla apagada o en modo Doze.
  * **Retorno rápido a Streaming:** Detección en vivo de la pista que suena y selector de accesos directos personalizables (YouTube ReVanced, Spotify, YouTube Music, etc.) para volver a la app de música en 1 toque tras activar el temporizador.
  * **Agitar para extender (+10 min):** Un leve movimiento del teléfono añade tiempo si aún sigues despierto.
  * **Detección inteligente de sueño:** Identifica la quietud continua del usuario para iniciar el reposo sin esperar al fin del contador.
* **Calculadora de Ciclos REM (90 minutos):** Calcula las horas ideales para ir a dormir o levantarse evitando la inercia del sueño, con botón de 1 toque para programar la alarma nativa del reloj.
* **Ambientes Nórdicos:** Motor de sonidos orgánicos en bucle offline (lluvia nórdica, viento de Midgard, hogueras de campamento).

---

### 2. 🐺 Aullidos (Catálogo de Sonidos & Descubrimiento)
* **Exploración Online & Catálogo:** Navegación por sonidos virales, memes y efectos de sonido con integración fluida y paginada.
* **Descarga a Local:** Añade cualquier clip de la nube directamente a la biblioteca del dispositivo (`Music/GjallarAudio`).
* **Compartir & Personalizar:** Envío directo por WhatsApp / Telegram y opción de establecer cualquier audio como tono de llamada, alarma o notificación del sistema.

---

### 3. 🍻 Canteros & Audios Míticos
* **Biblioteca Organizada:** Colección de frases legendarias y sonidos cómicos con diseño discreto y elegante.
* **Filtro Rápido:** Selector superior entre **"Todos"** y **"Favoritos"** con marcado instantáneo (icono de corazón / estrella nórdica).
* **Acceso Directo a Archivos:** Botón optimizado para abrir la ruta física `Music/GjallarAudio` directamente en **Solid Explorer**, MiXplorer o el explorador de archivos nativo sin perderse en la raíz del almacenamiento.

---

### 4. 🔄 Actualizaciones OTA Integradas
* **Actualizador Inteligente:** Notificación y descarga automática de nuevas versiones directamente desde GitHub Releases.
* **Gestión de espacio:** Limpieza de APKs antiguos descargados para no saturar la memoria del teléfono.

---

## 🎨 Sistema de Diseño (Nordic Sentinel UI)
* **Fondo Principal:** `#0F172A` (Slate 900)
* **Superficies & Tarjetas:** `#1E293B` (Slate 800) y `#172033` (Slate 850)
* **Bordes & Separadores:** `#334155` (Slate 700)
* **Acentos:** `#38BDF8` (Sky Blue / Cyan Bifrost) y `#F59E0B` (Amber Gold Rúnico)
* **Tipografía:** Material 3 Typography con alto contraste en `#F8FAFC` (Slate 50) y `#94A3B8` (Slate 400)

---

## 🛠️ Tecnologías & Arquitectura
* **Lenguaje:** Kotlin 2.0
* **UI:** Jetpack Compose (Material Design 3)
* **Arquitectura:** MVVM, Clean Architecture, Kotlin Coroutines & StateFlow
* **Indexación:** Comprobaciones y filtrados en memoria `O(1)` con `HashSet` para scroll suave sin pérdida de fotogramas.
* **Servicios de fondo:** `ForegroundService` con tipo `mediaPlayback` y `ServiceCompat` compatible con Android 14 (API 34/35).
* **Permisos dinámicos:** Notificaciones en tiempo de ejecución (Android 13+), administración de pantalla y lectura de almacenamiento segura.

---

## 📥 Instalación
Descarga el APK más reciente compilado y firmado desde [GitHub Releases](https://github.com/Hungrow/Gjallar-Audio/releases).
