<h1 align="center">Actarec</h1>
<p align="center">Graba. Transcribe. Recuerda.</p>
<p align="center">Grabadora de reuniones y notas de voz para Android, con resumen y tareas mediante Groq.</p>

<p align="center"><a href="https://github.com/actaapp-es/actarec/releases/latest/download/Actarec.apk">Descargar Actarec</a> · <a href="https://github.com/actaapp-es/actarec/releases">Versiones</a> · <a href="PRIVACIDAD.md">Privacidad</a> · <a href="docs/FIRMA.md">Firma</a></p>

## Funciones

- Grabación en segundo plano con pausa, reanudación y guardado desde la notificación.
- Compresión máxima para voz: HE-AAC mono a 16 kbps, unos 7 MB por hora.
- Marcadores durante la grabación y al escuchar; toca uno para saltar a ese momento.
- Transcripción con Whisper (Groq), resumen, contexto y tareas, y preguntas sobre cada grabación.
- Búsqueda, favoritos, importación de audios, compartir y borrado local.
- Grabaciones en el almacenamiento privado del móvil. La clave de Groq es tuya y se guarda cifrada con Android Keystore.
- Sin cuentas, anuncios, analítica ni servidores propios. El audio solo se envía a Groq cuando tú lo autorizas.

No captura el audio interno de las llamadas telefónicas (para eso existe [Acta](https://github.com/actaapp-es/acta)).

## Instalación

Descarga `Actarec.apk` desde [Releases](https://github.com/actaapp-es/actarec/releases/latest), comprueba su SHA-256 con `SHA256SUMS.txt` e instálala (Android 10 o superior). En la app, concede el micrófono y las notificaciones y añade tu clave de Groq en Ajustes (se crea en https://console.groq.com/keys).

## Compilar

Requisitos: JDK 17, Gradle 8.11.1 y Android SDK 35.

```sh
python3 scripts/check.py
gradle --no-daemon :app:assembleDebug
```

Para una release firmada define `ACTAREC_STORE_FILE`, `ACTAREC_STORE_PASSWORD`, `ACTAREC_KEY_ALIAS` y `ACTAREC_KEY_PASSWORD` y ejecuta `gradle --no-daemon :app:assembleRelease`. Los secretos del repositorio y el proceso de publicación están en [docs/FIRMA.md](docs/FIRMA.md).

## Licencia

MIT. Pide los consentimientos que correspondan antes de grabar conversaciones. El uso de Groq puede generar costes según tu plan.
