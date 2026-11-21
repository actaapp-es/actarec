# Seguridad de Actarec

Comunica posibles fallos de seguridad a los mantenedores mediante GitHub Security Advisories (preferible a un issue público que exponga información sensible).

No incluyas audios de conversaciones, claves Groq, contraseñas ni certificados de firma en los informes.

- Los secretos de firma deben existir únicamente como secretos de GitHub Actions y en una copia privada del mantenedor.
- La APK pública debe verificarse con `apksigner` y el SHA-256 de la release.
- El código envía audio y texto exclusivamente a Groq después de la autorización del usuario.
- No utilices versiones debug como releases definitivas ni claves privadas compartidas entre mantenedores.
