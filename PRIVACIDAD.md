# Privacidad de Actarec

- Los audios (M4A/AAC), transcripciones, resúmenes y preguntas se guardan en el almacenamiento interno privado de la aplicación y una base de datos SQLite.
- Los archivos de audio **no cuentan con cifrado propio**; depende de las protecciones de Android y del dispositivo. En un dispositivo desbloqueado o comprometido puede haber exposición.
- La clave personal Groq se cifra usando Android Keystore. No se incluye ninguna clave en el código.
- Grabar no envía nada a servidores. Al aceptar **Analizar con Groq**, el audio se envía a la API de Groq y la transcripción al modelo de resumen; al hacer preguntas se envían extractos de la conversación y el historial reciente del chat.
- El proyecto no incorpora cuentas de usuario, analítica ni infraestructura propia. No controla los tratamientos, tarifas ni retenciones aplicables por Groq. Consulta su política vigente.
- Puedes eliminar cada grabación y sus mensajes desde la app. Antes de grabar a terceras personas, cumple la normativa aplicable e informa cuando corresponda.
