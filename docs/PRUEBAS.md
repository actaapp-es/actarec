# Checklist de verificación antes de anunciar una versión estable

**La compilación exitosa NO garantiza que las funciones funcionen en todos los dispositivos.** Ejecuta al menos esta matriz manual sobre una APK de la misma firma que vas a publicar.

## Dispositivos

- [ ] Android 10/11 sin notch: grabar, pausar, guardar desde notificación.
- [ ] Android 13: conceder y denegar `POST_NOTIFICATIONS` y confirmar el resultado.
- [ ] Android 14/15: grabación en segundo plano tras cambiar de app, apagar pantalla y desbloquear.
- [ ] Android 16: comprobar Live Update si existe.
- [ ] Al menos dos marcas/fabricantes Android, con y sin restricciones de batería agresivas.

## Flujo de grabación

- [ ] Inicio con permiso de micrófono concedido / denegado.
- [ ] Audio grabado con pantalla apagada 30 min.
- [ ] Pausa y reanudación: duración sin contar pausas, archivo reproducible.
- [ ] Guardar y marcar desde la notificación; marcadores al grabar y al escuchar.
- [ ] Revocar permiso de superposición durante una grabación.
- [ ] Abrir otras apps que usen micrófono; verificar que el usuario comprende los límites.
- [ ] Simular interrupción/reinicio: comprobar recuperación de archivos finalizados sin indexar.
- [ ] Importar audio en M4A, MP3 y WAV; comprobar otros formatos compatibles.

## Groq y contenido

- [ ] Clave Groq válida y clave inválida.
- [ ] Modo idioma automático, español e inglés.
- [ ] Grabación corta y AAC/M4A de más de 25 MB, fragmentación y transcripción completa.
- [ ] Caída de red durante resumen: la transcripción debe mantenerse.
- [ ] Preguntar por dato presente y por dato ausente; respuesta cautelosa.
- [ ] Cambiar de pantalla mientras analiza; notificación de progreso.
- [ ] Corregir transcripción y comprobar que resumen/chat antiguos se invalidan.
- [ ] Compartir audio mediante proveedor de solo lectura y compartir resumen como texto.

## Seguridad y actualización

- [ ] Inspeccionar que el APK no contiene API keys ni keystore privada.
- [ ] Comprobar que `apksigner verify --verbose` informa firma válida.
- [ ] Actualizar release v1.0.0 → v1.0.1 conservando grabaciones.
- [ ] Validar que preview y release usan `applicationId` distintos.
- [ ] Probar borrado local y ausencia de copia de seguridad automática.

## Criterio de salida

No declarar una release estable hasta que GitHub Actions termine **en verde**, una APK firmada se instale y pase las pruebas de grabación/Groq en un Android real. Registrar cualquier incidencia conocida en Issues antes de distribuir.
