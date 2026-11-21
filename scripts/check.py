#!/usr/bin/env python3
"""Detección de errores triviales antes de descargar Gradle/Android."""
from pathlib import Path
from xml.etree import ElementTree as ET
import re
import sys

root = Path(__file__).resolve().parents[1]
required = [
    'README.md','LICENSE','PRIVACIDAD.md',
    'build.gradle','settings.gradle','app/build.gradle',
    'app/src/main/AndroidManifest.xml',
    '.github/workflows/android.yml',
    'app/src/main/java/es/actarec/app/RecordingService.java',
    'app/src/main/java/es/actarec/app/GroqClient.java',
]
errors=[]
for f in required:
    if not (root/f).is_file():errors.append('Falta '+f)
for p in (root/'app/src/main').rglob('*.xml'):
    try:ET.parse(p)
    except ET.ParseError as e:errors.append(f'{p}: XML incorrecto {e}')
for p in (root/'app/src/main/java').rglob('*.java'):
    s=p.read_text(encoding='utf8')
    if re.search(r'gsk_[A-Za-z0-9_-]{20,}',s):errors.append('Clave Groq incrustada en '+p.name)
if len(list((root/'app/src/main/java').rglob('*.java')))<8:errors.append('Faltan módulos Android')
if errors:
    for e in errors:print('ERROR:',e,file=sys.stderr)
    raise SystemExit(1)
print('Estructura, XML y secretos: comprobaciones superadas; sigue siendo necesaria la compilación Android.')
