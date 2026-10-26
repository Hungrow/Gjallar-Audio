# 🛡️ Gjallar Audio (Sentinel Audio Suite & Sleep)
> **Ecosistema Heimdall** — Herramienta integral de sonido, descanso inteligente y edición de audio.

---

## 🌌 Visión y Conexión con Heimdall
En la mitología nórdica, el **Gjallarhorn** es el cuerno custodiado por **Heimdall**, cuyo sonido viaja y resuena a través de los nueve reinos. Mientras que **Heimdall** es el guardián de escritorio, **Gjallar Audio** es la navaja suiza de sonido y descanso en tu dispositivo móvil.

---

## 🎨 Sistema de Diseño (Nordic Sentinel UI)
Hereda la estética nativa de **Heimdall**:
* **Fondo Principal:** `#0F172A` (Slate 900)
* **Superficies & Tarjetas:** `#1E293B` (Slate 800) y `#172033` (Slate 850)
* **Bordes & Separadores:** `#334155` (Slate 700)
* **Acento Primario (Gjallar / Bifrost Glow):** `#38BDF8` (Sky Blue)
* **Acento Secundario (Oro Rúnico / Estado):** `#F59E0B` (Amber Gold)
* **Textos:** `#F8FAFC` (Slate 50) y `#94A3B8` (Slate 400)

---

## 🏛️ Los 4 Módulos de la Suite

### 1. 🌙 Gjallar Night (Descanso & Ciclos)
* **Calculadora de Ciclos REM (90 minutos):** Calcula las horas exactas de acostarse o levantarse para evitar la inercia del sueño y despertar fresco.
* **Sleep Timer Pro:** Temporizador con desvanecimiento de volumen progresivo (*Fade-Out*), corte global de reproducción multimedia (Spotify, YouTube, etc.) y detector de movimiento (*Shake to extend* +10 min).
* **Generador de Ambientes Nórdicos:** Mezclador offline de lluvia, viento de Midgard, hoguera y frecuencias delta.

### 2. ⚡ Gjallar Forge (Conversor & Lotes)
* **Extracción de Video a Audio:** Conversión ultra-rápida a MP3, AAC/M4A, FLAC sin pérdida y OPUS.
* **Procesamiento por Lotes:** Múltiples conversiones simultáneas en segundo plano.
* **Editor de Metadatos:** Carátula ID3, artista, álbum y título.

### 3. 🎙️ Gjallar Voice (Lector TTS & Perfiles)
* **Lectura Inteligente de Textos:** Lectura de notas, PDFs y documentos.
* **Perfiles Predefinidos:**
  * 🌙 *Voz Nocturna / Relajante:* Cadencia lenta, timbre suave.
  * 📖 *Narrador de Audiolibro:* Tono cálido y envolvente.
  * ⚡ *Podcast / Dinámico:* Ritmo acelerado (1.3x) para estudio rápido.
  * 🧸 *Cuentacuentos Infantil:* Expresivo y melódico.
* **Exportar a MP3:** Guarda cualquier escrito como archivo de audio para escuchar cuando quieras.

### 4. 🎛️ Gjallar Lab (Estudio & IA)
* **Separación de Pistas (IA):** Aísla voces de la música o genera pistas Karaoke.
* **Waveform Cutter:** Recorte con onda sonora para tonos de llamada, alarmas y notificaciones.
* **Reductor de Ruido:** Limpieza cristalina de viento y ruido estático en notas de voz.

---

## 🛠️ Tecnologías
* **Lenguaje:** Kotlin 2.0
* **Interfaz:** Jetpack Compose (Material You 3)
* **Arquitectura:** MVVM + Clean Architecture + StateFlow
* **Motor de Sonido:** Android AudioManager / Media3 ExoPlayer
* **Compatibilidad:** Android 8.0+ (API 26 a API 35)
