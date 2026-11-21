# Actarec

**Grabaciones Android con memoria.** Graba conversaciones presenciales en segundo plano, transcribe el audio con Groq, genera resúmenes y haz preguntas sobre cada conversación. Proyecto abierto, sin servidor ni cuenta propia.

> **Estado:** versión de prueba (0.3.0). Publicamos el código y ofrecemos compilación automática. Hasta que pase una compilación real y pruebas en varios Android, no se puede garantizar un funcionamiento perfecto.

## Descargar APK

Cuando GitHub Actions haya terminado correctamente, descarga **Actarec-preview.apk** desde [Releases](https://github.com/actaapp-es/actarec/releases/tag/preview). El nombre del archivo es constante. También se genera un `SHA256SUMS.txt` de verificación.

Si la pestaña *Releases* no muestra la APK, mira [Actions](https://github.com/actaapp-es/actarec/actions) para ver el fallo. **Una publicación correcta de GitHub Pages no equivale a la compilación de la APK.**

## Funciones

- Grabación de **micrófono** con `ForegroundService`: continúa cuando usas otras apps, salvo restricciones de Android/fabricante.
- Notificación persistente para pausar, reanudar y detener.
- Cápsula flotante opcional bajo el notch, con permiso de superposición y en móviles compatibles; nunca ocupa la zona reservada del sistema.
- Biblioteca privada, reproducción, renombrado, borrado e importación de audio.
- Groq `whisper-large-v3-turbo` para transcripción y `openai/gpt-oss-120b` para resumen y preguntas.
- Grabaciones largas AAC/M4A troceadas localmente para subir a Groq por partes.
- Clave de Groq **personal** guardada cifrada con Android Keystore. No contiene claves de otros usuarios ni requiere servidor.

**Limitaciones:** No graba el audio interno de llamadas. La cápsula no es la Dynamic Island real del sistema. Android restringe inicio del micrófono desde el fondo: hay que comenzar desde la pantalla visible de Actarec. No se garantiza supervivencia si el fabricante fuerza el cierre del servicio. Los audios y la base de datos se almacenan localmente pero la app **no cifra los archivos de audio por sí misma**. Para usar IA se envía audio/transcripción a Groq con consentimiento.

## Compilación en GitHub (automática)

Al hacer `push` en `main`, `.github/workflows/android.yml`:
1. Instala Java 17, Gradle 8.11.1 y Android SDK 35, sin `android-actions/setup-android@v3`.
2. Compila una APK instalable (`assembleDebug` si no hay clave de firma, `assembleRelease` si la hay).
3. Sube el artefacto de compilación a Actions.
4. Crea/actualiza la release pública **preview** con `Actarec-preview.apk` y SHA-256.

Una etiqueta `vX.Y.Z` crea una release de esa versión. Si los secretos de firma no están configurados, la release se marca como **experimental** (debug) y **no debe presentarse como estable**.

### Muy importante al actualizar un repositorio existente

**No subas solo estos archivos encima de los anteriores desde la web**, porque GitHub no elimina automáticamente los archivos antiguos. Debes borrar `.github/workflows/release.yml`, `tools/bootstrap/`, `gradle/wrapper/` y los antiguos `gradlew*` del repositorio previo. Esos elementos ya no forman parte del proyecto nuevo.

La forma más segura es clonar el repositorio en un PC y reemplazar por completo su contenido, conservando solo `.git`:

```bash
git clone https://github.com/actaapp-es/actarec.git
cd actarec
# Copia aquí los archivos extraídos de ESTE ZIP; borra los archivos viejos que no figuran en él.
git add -A
git commit -m "Reconstruir compilación Android con flujo único"
git push origin main
```

En **Settings → Actions → General**, asegúrate de permitir el uso de GitHub Actions y de que el token pueda crear Releases (permiso `contents: write` indicado en el YAML, sujeto a los ajustes del repositorio).

## Compilar localmente

Requiere Android SDK 35, Java 17 y **Gradle 8.11.1** instalados. Desde la raíz:

```bash
gradle --no-daemon :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

No se distribuye un Gradle Wrapper no oficial. Si necesitas wrapper para desarrollo local, genéralo mediante Gradle oficial: `gradle wrapper --gradle-version 8.11.1`, y sube el wrapper estándar resultante.

## Firma estable (opcional)

Para que las actualizaciones se instalen encima sin desinstalar se necesita una **misma clave privada** de firma para todas las versiones. Configura en GitHub > Settings > Secrets and variables > Actions estos 4 secretos:

- `ACTAREC_KEYSTORE_BASE64` — el archivo `.jks` codificado como Base64.
- `ACTAREC_STORE_PASSWORD` — contraseña del almacén.
- `ACTAREC_KEY_ALIAS` — alias de firma.
- `ACTAREC_KEY_PASSWORD` — contraseña de esa clave.

Nunca incluyas el keystore ni las contraseñas en el repositorio público. Guarda copias seguras de la clave: perderla impediría actualizar sobre instalaciones previas.

## Privacidad y licencia

Revisa [PRIVACIDAD.md](PRIVACIDAD.md) antes de utilizar la IA. Grabar una conversación puede exigir informar o pedir consentimiento a los participantes; comprueba la legislación aplicable. Código MIT en [LICENSE](LICENSE). Diseño inspirado en la paleta pública de [Acta](https://github.com/actaapp-es/acta), sin incorporar su código.
