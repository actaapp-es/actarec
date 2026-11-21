#!/usr/bin/env python3
"""Comprobaciones estáticas de integridad; no sustituye la compilación Android."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re

root=Path(__file__).resolve().parents[1]
app=root/'app/src/main'
java=app/'java/es/actarec/app'
files=[p for p in root.rglob('*') if p.is_file()]
errors=[]
def require(condition,message):
    if not condition: errors.append(message)

for expected in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties',
                 'README.md','LICENSE','PRIVACIDAD.md','.github/workflows/android.yml',
                 'app/src/main/AndroidManifest.xml','docs/FIRMA.md']:
    require((root/expected).is_file(),f'Falta {expected}')

xmls=[p for p in app.rglob('*.xml')]
for p in xmls:
    try: ET.parse(p)
    except ET.ParseError as exc: errors.append(f'XML inválido {p.relative_to(root)}: {exc}')

manifest=ET.parse(app/'AndroidManifest.xml').getroot()
android='{http://schemas.android.com/apk/res/android}'
permission={x.get(android+'name') for x in manifest.findall('uses-permission')}
for p in ['android.permission.RECORD_AUDIO','android.permission.INTERNET',
          'android.permission.FOREGROUND_SERVICE_MICROPHONE','android.permission.FOREGROUND_SERVICE_DATA_SYNC',
          'android.permission.SYSTEM_ALERT_WINDOW','android.permission.WAKE_LOCK']:
    require(p in permission,f'Permiso necesario ausente: {p}')
application=manifest.find('application')
for kind in ['activity','service','provider']:
    for el in application.findall(kind):
        name=el.get(android+'name','')
        if name.startswith('.'):
            require((java/(name[1:]+'.java')).is_file(),f'Componente sin clase: {name}')
        require(el.get(android+'exported') in ('true','false'),f'Componente sin exported: {name}')
services={s.get(android+'name'):s.get(android+'foregroundServiceType','') for s in application.findall('service')}
require(services.get('.RecordingService')=='microphone','RecordingService sin tipo microphone')
require(services.get('.AnalysisService')=='dataSync','AnalysisService sin tipo dataSync')
provider=application.find('provider')
require(provider is not None and provider.get(android+'authorities')=='${applicationId}.audio','Autoridad de audio incorrecta')

sources='\n'.join(p.read_text(encoding='utf-8') for p in java.glob('*.java'))
require('whisper-large-v3-turbo' in sources,'Modelo de transcripción no definido')
require('openai/gpt-oss-120b' in sources,'Modelo de texto no definido')
require('AnalysisService' in sources and 'RecordingService' in sources,'Servicios no conectados')
for name in re.findall(r'R\.drawable\.([A-Za-z0-9_]+)',sources):
    require((app/'res/drawable'/f'{name}.xml').exists(),f'Drawable inexistente: {name}')

for p in files:
    rel=p.relative_to(root)
    require(p.suffix.lower() not in ('.apk','.aab','.jks','.keystore','.p12'),f'Archivo privado/binario: {rel}')
    if p.suffix in ('.java','.md','.gradle','.yml','.py','.xml'):
        text=p.read_text(encoding='utf-8')
        require(not re.search(r'gsk_[A-Za-z0-9]{25,}',text),f'Parece haber una clave Groq en: {rel}')
        require(not re.search(r'(?i)-----BEGIN (RSA )?PRIVATE KEY-----',text),f'Clave privada en: {rel}')

workflows=list((root/'.github/workflows').glob('*.yml'))
require(len(workflows)==1,'Debe existir un único workflow de Android')
workflow=(root/'.github/workflows/android.yml').read_text(encoding='utf-8')
require('setup-android@' not in workflow,'El setup-android antiguo no es compatible')
require(':app:assembleRelease' in workflow,'Falta compilación release')
require('apksigner' in workflow,'Falta comprobación de firma APK')
require('ACTAREC_KEYSTORE_BASE64' in workflow,'Faltan secretos de firma')
require('gh release create' in workflow,'Falta publicación de descargas')
require('transcriptions' in sources,'Falta API de Groq para transcribir')

if errors:
    for error in errors: print('ERROR:',error)
    raise SystemExit(1)
print(f'OK: {len(files)} archivos, {len(xmls)} recursos XML, {len(list(java.glob("*.java")))} fuentes Java.')
print('Nota: esta prueba no compila ni instala la APK.')
