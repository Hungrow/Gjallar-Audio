# 🛡️ Gjallarhorn
> **Ecosistema Heimdall** — Suite integral de sonido, descanso inteligente, bloc de notas rúnico, botonera de aullidos y laboratorio de audio para Android.

---

## 🌌 Visión
En la mitología nórdica y la leyenda de los guardianes, el **Gjallarhorn** es el artefacto supremo custodiado por el dios **Heimdall**, cuyo sonido resuena con fuerza a través de los nueve reinos. **Gjallarhorn** traslada ese espíritu al smartphone con una navaja suiza de herramientas acústicas, descanso nocturno y productividad bajo una estética nórdica **Dark Slate**, optimizada a 60/120 fps y compatible desde Android 8.0 hasta Android 14+.

---

## 🏛️ Los 5 Módulos de la Suite

```
                   ┌─────────────────────────────────────────┐
                   │          🛡️ GJALLARHORN SUITE           │
                   └────────────────────┬────────────────────┘
          ┌──────────────┬──────────────┼──────────────┬──────────────┐
          ▼              ▼              ▼              ▼              ▼
      🌙 NIGHT        ⚡ FORGE       🐺 AULLIDOS     📝 NOTAS       🎛️ LAB
    Sleep Timer    Video a Audio    Botonera Viral    Sticky Notes   Recorte Tono
    Ciclos REM     Normalización   Favoritos/Local    Transcriptor    Karaoke IA
    Ambientes      Player MP3/WAV  Solid Explorer      3 Locutores   Denoise Audio
```

---

### 1. 🌙 Gjallar Night (Descanso, Sleep Timer & Ciclos REM)
* **Temporizador de Apagado (Sleep Timer Pro):**
  * **Atenuación suave virtual (1 minuto al llegar a 0):** Durante la cuenta atrás el volumen no se altera. Al llegar a 0, disminuye progresivamente el sonido a lo largo del siguiente minuto hasta el silencio y pausa la reproducción. Al detenerse, **restaura de inmediato el volumen multimedia original** para dejarlo exactamente como lo dispuso el usuario.
  * **Apagado y Bloqueo de Pantalla (Estilo Sleep Timer):** Soporte nativo para bloquear y apagar la pantalla automáticamente al concluir el temporizador mediante Administrador de Dispositivos (`DevicePolicyManager.lockNow()`).
  * **Cuádruple corte universal de audio:** Detiene reproductores de audio en segundo plano (Spotify, YouTube ReVanced, YouTube Music, podcasts, audiolibros) mediante eventos multimedia de hardware, pausa activa en `MediaSession` y adquisición exclusiva de foco de audio.
  * **Servicio en primer plano con WakeLock de CPU:** Conteo fiable con notificación persistente e icono en la barra de estado; no se suspende aunque la pantalla esté apagada o entre en modo Doze.
  * **Retorno rápido a Streaming:** Detección en vivo de la pista activa y selector personalizable con chips (YouTube ReVanced, Spotify, YouTube Music, etc.) para volver a tu app favorita en un toque tras configurar el temporizador.
  * **Agitar para extender (+10 min) & Detección inteligente de quietud:** Un leve movimiento añade tiempo si aún sigues despierto, o anticipa el reposo si detecta inmovilidad continua.
* **Calculadora de Ciclos REM (90 minutos):** Horas óptimas para dormir o despertar evitando la inercia del sueño, con botón de 1 toque para programar la alarma nativa del teléfono.
* **Ambientes Nórdicos en bucle:** Mezclador offline de grabaciones orgánicas (lluvia nórdica, viento de Midgard, hoguera de campamento).

---

### 2. ⚡ Gjallar Forge (Conversor de Video a Audio)
* **Extracción Rápida:** Convierte cualquier pista de vídeo almacenada en el dispositivo a formatos de audio seleccionables (`MP3 Universal`, `WAV sin compresión`, etc.).
* **Procesamiento de Señal:**
  * **Normalización de sonoridad (*Normalize Loudness*):** Nivela el volumen general evitando picos estridentes.
  * **Recorte de silencios (*Trim Silence*):** Elimina tramos mudos al inicio y final del audio.
* **Histórico y Reproductor Integrado:** Escucha inmediata del último archivo forjado y botón para compartir por mensajería o abrir en aplicaciones externas.

---

### 3. 🐺 Gjallar Aullidos (Botonera, Memes & Audios Legendarios)
* **Botonera Dinámica de Frases y Sonidos:** Reproducción con baja latencia de citas célebres, audios virales y clásicos de la cultura popular con botones discretos e integrados en la interfaz.
* **Selector Superior "Todos" y "Favoritos":** Filtrado ágil de audios y marcado de favoritos en 1 toque con icono rúnico (corazón / estrella nórdica).
* **Catálogo Online & Descarga Sutil:** Navegación por el catálogo de sonidos externos con opción para descargar cualquier audio a la biblioteca local (`Music/GjallarAudio`).
* **Acceso Directo a Archivos:** Acceso directo optimizado por ruta física `resource/folder` para abrir la carpeta directamente en **Solid Explorer** o exploradores de archivos compatibles sin desviarse a la raíz.
* **Acciones de Sistema:** Establecer cualquier audio como tono de llamada, sonido de notificación o alarma con un solo toque.

---

### 4. 📝 Gjallar Notes (Muro de Notas & Transcriptor de Voz)
* **Muro de Notas Rúnicas (Sticky Notes):** Bloc de notas adhesivas con paletas nórdicas de alto contraste visual y lectura garantizada:
  * ᛟ **Ámbar Solar:** Fondo pergamino cálido con tipografía marrón carbón ultra legible.
  * ᛉ **Hielo Glaciar:** Fondo azul abisal con acentos cian glaciar.
  * ᚱ **Musgo del Bosque:** Fondo verde nórdico con tonos esmeralda suaves.
  * ᚦ **Pizarra Nórdica:** Fondo carbón pizarra oscuro con tipografía plata.
  * ᛋ **Cobre Rúnico:** Tono cobrizo otoñal con matices dorados.
* **Persistencia Completa en Disco:** Todas las notas quedan almacenadas localmente mediante `NotesRepository` (no se pierden al cerrar la aplicación o cambiar de módulo).
* **Transcriptor de Voz por Interlocutores (STT en Vivo):**
  * Dictado por voz en tiempo real con motor de reconocimiento de voz.
  * Diferenciación de diálogo de hasta **3 interlocutores simultáneos** identificados por código de color (Cian, Ámbar, Esmeralda).
  * Conversión inmediata de cualquier transcripción o fragmento dictado a una nueva nota adhesiva en el muro.
* **Gestión Rápida:** Copiar contenido íntegro al portapapeles, compartir por apps externas y eliminar notas obsoletas.

---

### 5. 🎛️ Gjallar Lab (Estudio & Laboratorio de Audio)
* **Recortador de Tono (Trimmer):** Recorte visual de audio con selección de tramos de inicio y fin, preescucha en bucle y detección de estribillo (*Chorus Detection*) para crear tonos y alarmas personalizados.
* **Separación de Pistas (IA Vocal / Karaoke):** Algoritmo de separación espectral para aislar la voz de la pista musical o generar instrumentales limpios para Karaoke.
* **Reductor de Ruido (Denoiser):** Limpieza de ruido de fondo, viento y estática con perfiles específicos (*Voz de estudio*, *Ambiente exterior*, etc.).

---

## 🔄 Actualizaciones OTA Integradas
* **Detección Automática:** Notificación en la interfaz cuando existe una versión más reciente publicada en GitHub Releases.
* **Instalación Segura y Limpieza:** Descarga directa e instalación de APK nativa, eliminando automáticamente los paquetes temporales para no saturar el almacenamiento del teléfono.

---

## 🎨 Sistema de Diseño (Nordic Sentinel UI)
Hereda la identidad visual del ecosistema **Heimdall**:
* **Fondo Principal:** `#0F172A` (Slate 900)
* **Superficies & Tarjetas:** `#1E293B` (Slate 800) y `#172033` (Slate 850)
* **Bordes & Separadores:** `#334155` (Slate 700)
* **Acento Primario (Bifrost Glow):** `#38BDF8` (Sky Blue / Cyan)
* **Acento Secundario (Oro Rúnico):** `#F59E0B` (Amber Gold)
* **Tipografía:** Material Design 3 con contraste equilibrado en `#F8FAFC` (Slate 50) y `#94A3B8` (Slate 400).

---

## 🛠️ Tecnologías & Arquitectura
* **Lenguaje:** Kotlin 2.0
* **Interfaz:** Jetpack Compose (Material You 3)
* **Arquitectura:** MVVM + Clean Architecture + StateFlow reactivo
* **Rendimiento:** Indexación y comprobaciones en memoria mediante `HashSet` (`O(1)`) para desplazamiento fluido a 60/120 fps en listas extensas.
* **Servicios de fondo:** `ForegroundService` con tipo `mediaPlayback` y `ServiceCompat` compatible con Android 14 (API 34/35).
* **Control de Dispositivo:** Administrador de Dispositivos (`DevicePolicyManager`) para apagado de pantalla y `NotificationListenerService` para detección multimedia.

---

## 📥 Descarga & Instalación
El archivo compilado y firmado más reciente se encuentra disponible en [GitHub Releases](https://github.com/Hungrow/Gjallar-Audio/releases).
