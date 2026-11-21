# Privacidad · Actarec

Actarec es una grabadora Android de código abierto sin publicidad, cuentas propias, telemetría ni SDK analíticos.

## Datos locales

El audio, las transcripciones, los resúmenes, favoritos y el historial de preguntas permanecen en el almacenamiento privado de la app. La base de datos SQLite y los archivos usan el cifrado del sistema del dispositivo; **no se afirma que cada audio esté cifrado adicionalmente por Actarec**. `android:allowBackup="false"` desactiva la copia de seguridad automática estándar de la aplicación.

La clave personal de Groq se almacena cifrada con Android Keystore (AES-GCM). No aparece en el código fuente ni en la APK publicada.

## Envío a Groq

**La grabación no se sube automáticamente.** Al autorizar la transcripción, Actarec envía el audio seleccionado a la API HTTPS de Groq. Para el resumen se envía la transcripción a un modelo de texto. Al hacer preguntas se envían la pregunta, el resumen y los fragmentos relevantes de la transcripción, junto con un contexto limitado de preguntas anteriores. El procesamiento externo queda sujeto a la política de Groq y a los costes y límites de la cuenta del usuario. Puedes informarte en https://groq.com/privacy-policy/.

La opción «Comprobar conexión» solo consulta el listado de modelos de Groq; no sube tus conversaciones.

## Permisos

- **Micrófono:** grabación ambiental al pulsar el botón de inicio.
- **Servicio en primer plano:** mantener la grabación y, cuando se autorice, el análisis visibles al cambiar de app.
- **Notificaciones:** controles para pausar, reanudar y guardar desde Android.
- **Superposición:** cápsula opcional bajo la barra superior del dispositivo, solo tras conceder permiso.
- **Internet:** comunicación con Groq al analizar o comprobar una API key.
- **Wake lock:** ayuda a evitar la suspensión del procesador mientras se graba.

## Borrado y exportación

Puedes eliminar cualquier grabación y sus análisis o compartir explícitamente el audio o texto. Desinstalar Actarec normalmente elimina los datos privados de la aplicación. Exporta previamente tus grabaciones si quieres conservarlas.

## Consentimiento

Es responsabilidad de quien graba informar a las demás personas y respetar la legislación aplicable. Actarec no captura el audio interno de las llamadas telefónicas.
