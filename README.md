<p align="center"><strong>ACTAREC</strong></p>
<h1 align="center">Actarec</h1>
<p align="center">Graba. Transcribe. Recuerda.</p>
<p align="center">Grabadora de reuniones Android con Groq AI, biblioteca privada y controles mientras usas otras apps.</p>

<p align="center"><a href="https://github.com/actaapp-es/actarec/releases">📥 APK de Actarec</a> · <a href="PRIVACIDAD.md">Privacidad</a> · <a href="docs/FIRMA.md">Firma y publicación</a></p>

## Funciones

- **Grabación en segundo plano** con `ForegroundService` de micrófono y controles de **pausa, reanudación y guardar** desde una notificación persistente.
- **Cápsula flotante opcional** situada **debajo de la barra de estado** en móviles con notch/recorte. Android no permite insertar controles propios dentro del notch de forma universal. Requiere permiso de superposición; en el resto de móviles se usan notificaciones.
- Grabaciones privadas en almacenamiento interno de la app, **búsqueda, favoritos**, importación de M4A/MP3/WAV/OGG/FLAC/WebM/MP4 y reproducción con desplazamiento.
- **Transcripción con Groq** (`whisper-large-v3-turbo`), detección automática de idioma o español/inglés, división de archivos largos AAC/M4A.
- **Resumen y tareas con Groq** (`openai/gpt-oss-120b`) y **preguntas contextualizadas** sobre una conversación. Permite editar la transcripción.
- **Compartir** texto o audio usando permisos temporales de Android; eliminar audios y análisis locales.
- Clave Groq del usuario cifrada con Android Keystore. El audio solo se envía a Groq cuando el usuario lo autoriza.
- Interfaz nativa Android con la paleta forestal/marfil de Acta, sin SDK analítico, publicidad ni bibliotecas de diseño externas.

> **Qué no hace:** no captura audio interno de llamadas telefónicas; no modifica la barra del sistema ni garantiza soporte de Live Updates/Islas de todos los fabricantes. La grabación requiere permisos y depende de las políticas de batería de cada móvil.

## Instalación pública

En [Releases](https://github.com/actaapp-es/actarec/releases), descarga una APK de etiqueta estable (`v1.0.0`, etc.) **únicamente cuando aparezca como firmada de producción**. En la app, concede acceso al micrófono, habilita notificaciones y configura tu clave de Groq en Ajustes. Puedes crear una clave personal en https://console.groq.com/keys.

La release `preview` se genera desde `main`, usa un **identificador independiente** (`es.actarec.app.preview`) y una firma de depuración; es para pruebas, no sustituye a una release estable.

## Publicar la versión definitiva (mantenedores)

El código fuente permite producir una APK release. **No se puede publicar una versión definitiva firmada sin que el propietario aporte una clave de firma privada persistente**. No guardes esa clave en Git.

1. Añade los cuatro secretos de GitHub Actions siguiendo [docs/FIRMA.md](docs/FIRMA.md): `ACTAREC_KEYSTORE_BASE64`, `ACTAREC_STORE_PASSWORD`, `ACTAREC_KEY_ALIAS`, `ACTAREC_KEY_PASSWORD`.
2. Sube el contenido de este proyecto a `main`, manteniendo `.github/workflows/android.yml` (el único workflow del repositorio).
3. Espera a que `Android · Actarec` termine correctamente. En `main` se genera automáticamente `Actarec-preview.apk`.
4. **Tras probar el comportamiento real en Android**, crea la etiqueta anotada `v1.0.0` y súbela. GitHub Actions compilará `:app:assembleRelease`, verificará la APK con `apksigner` y publicará `Actarec-v1.0.0.apk` con su SHA-256 en Releases.

Si faltan secretos al publicar una etiqueta, el workflow **fallará expresamente**; nunca presentará una APK debug como si fuera una versión definitiva.

## Compilar localmente

Requisitos: **JDK 17**, **Gradle 8.11.1** y **Android SDK 35** con build-tools 35.0.0. Este repositorio no incluye un Gradle Wrapper artesanal ni claves privadas.

```sh
python3 scripts/verify.py
gradle --no-daemon :app:lintDebug :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk
```

Para compilar una release firmada, define las cuatro variables `ACTAREC_STORE_FILE`, `ACTAREC_STORE_PASSWORD`, `ACTAREC_KEY_ALIAS`, `ACTAREC_KEY_PASSWORD` en tu entorno y ejecuta `gradle --no-daemon :app:assembleRelease`. Nunca compartas la clave de firma ni la subas al repositorio.

## Pruebas

- `python3 scripts/verify.py`: integridad XML, manifiesto, recursos, secretos y workflow.
- Tests de lógica Java incluidos en `tests/`; GitHub Actions los compila y ejecuta automáticamente.
- GitHub Actions ejecuta Android Lint, compila la APK y verifica la firma con `apksigner`.
- La [matriz de pruebas manuales](docs/PRUEBAS.md) debe completarse en dispositivos reales antes de anunciar una versión estable.

**Estado de esta entrega:** el código y las pruebas estáticas se han revisado, pero desde el entorno donde se generó este ZIP no se dispone de Android SDK, Groq API key ni dispositivo físico. No se afirma que se haya compilado o instalado una APK de producción ni que se haya probado en todos los Android. La primera compilación real debe confirmarse en GitHub Actions.

## Estructura

```text
.github/workflows/android.yml   CI / releases firmadas
app/src/main/java/...           Código Java Android
app/src/main/res/               Tema, iconos, colores
scripts/verify.py              Verificación del repositorio
tests/                         Pruebas unitarias Java puras
docs/FIRMA.md                   Configuración de la firma estable
docs/PRUEBAS.md                 Matriz de validación en dispositivos
PRIVACIDAD.md                  Política de privacidad
LICENSE                        MIT
```

## Aviso legal

Pide los consentimientos que correspondan antes de grabar conversaciones. El uso de la API de Groq puede generar costes según tu plan. El proyecto se distribuye bajo la **licencia MIT**, sin garantía comercial, como software de código abierto.
