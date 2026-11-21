# Firma de producción y versiones públicas

Una APK distribuida públicamente necesita la **misma clave de firma** en todas sus actualizaciones. No uses la firma de depuración de GitHub como clave definitiva. **Guarda la clave de publicación fuera del repositorio y haz una copia de seguridad cifrada.** Si la pierdes, los usuarios no podrán actualizar con la misma identidad de aplicación.

## 1. Genera una clave en tu equipo de confianza

Necesitas Java 17 (`keytool`). No lo ejecutes en un ordenador compartido.

```sh
keytool -genkeypair -v \
  -keystore actarec-release.jks -storetype PKCS12 \
  -alias actarec -keyalg RSA -keysize 3072 -validity 10000 \
  -dname "CN=Actarec, O=Actarec, C=ES"
```

Anota **en tu gestor privado de contraseñas** la contraseña de la keystore y del alias, y realiza una copia segura de `actarec-release.jks`. **No lo metas en ningún ZIP público.**

## 2. Secretos del repositorio GitHub

Ve a `Settings → Secrets and variables → Actions → New repository secret` y crea:

| Nombre | Valor |
| --- | --- |
| `ACTAREC_KEYSTORE_BASE64` | contenido Base64 de `actarec-release.jks` (sin saltos de línea) |
| `ACTAREC_STORE_PASSWORD` | contraseña del almacén |
| `ACTAREC_KEY_ALIAS` | `actarec` |
| `ACTAREC_KEY_PASSWORD` | contraseña de la clave |

En macOS/Linux puedes obtener el texto Base64 con `base64 < actarec-release.jks | tr -d '\n'`; en PowerShell, `[Convert]::ToBase64String([IO.File]::ReadAllBytes("actarec-release.jks"))`.

No pegues estos valores en el chat, README, issues ni código.

## 3. Publica una release estable

Revisa la compilación automática en `main`, completa [PRUEBAS.md](PRUEBAS.md) y crea una etiqueta anotada:

```sh
git tag -a v1.0.0 -m 'Actarec 1.0.0'
git push origin v1.0.0
```

El workflow `Android · Actarec` exige los cuatro secretos y construye `:app:assembleRelease`. Comprueba la APK con `apksigner`, calcula `SHA256SUMS.txt` y adjunta ambos archivos a GitHub Releases.

**Si la compilación falla, no publiques manualmente una APK sin verificar o con firma debug.** Revisa los registros de GitHub Actions y corrige el fallo antes de repetir.

## Identidades y actualizaciones

- **Release estable:** `es.actarec.app` — utiliza la clave de producción y `versionCode` creciente.
- **Preview:** `es.actarec.app.preview` — firma debug separada; no migra datos automáticamente a la estable.
- Si los usuarios instalaron una versión anterior de `es.actarec.app` firmada con otra clave, puede ser necesario desinstalarla. Antes, deben **exportar sus grabaciones**; desinstalar borra los datos privados de la aplicación.
