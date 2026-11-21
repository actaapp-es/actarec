"""Valida el esquema SQLite y la actualización de datos de la versión 0.3."""
from pathlib import Path
import sqlite3, re

root=Path(__file__).resolve().parents[1]
src=(root/'app/src/main/java/es/actarec/app/RecordStore.java').read_text()
create_block=src.split('@Override public void onCreate(SQLiteDatabase db){',1)[1].split('    @Override public void onUpgrade',1)[0]
migrate_block=src.split('@Override public void onUpgrade(SQLiteDatabase db,int old,int version){',1)[1].split('    public File recordingsDir',1)[0]
regex=r'db\.execSQL\("([^"\n]+)"\)'
new_ddl=re.findall(regex,create_block)
migration=re.findall(regex,migrate_block)
assert len(new_ddl)==3,len(new_ddl)
assert len(migration)==2,len(migration)

conn=sqlite3.connect(':memory:')
conn.execute('PRAGMA foreign_keys=ON')
for sql in new_ddl:conn.execute(sql)
conn.execute('INSERT INTO recordings(title,path,created_at,duration_ms) VALUES (?,?,?,?)',('Prueba','/local/rec.m4a',123,25000))
conn.execute('INSERT INTO messages(recording_id,role,body,created_at) VALUES (1,?,?,?)',('user','¿Qué pasó?',124))
assert conn.execute('SELECT favorite,analysis_error FROM recordings').fetchone()==(0,'')
conn.execute('DELETE FROM recordings WHERE id=1')
assert conn.execute('SELECT count(*) FROM messages').fetchone()[0]==0,'FK cascade'
conn.close()

conn=sqlite3.connect(':memory:')
conn.executescript('''CREATE TABLE recordings(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,path TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,duration_ms INTEGER NOT NULL,transcript TEXT NOT NULL DEFAULT '',summary TEXT NOT NULL DEFAULT '');''')
conn.execute('INSERT INTO recordings(title,path,created_at,duration_ms,transcript) VALUES (?,?,?,?,?)',('Histórica','/local/old.m4a',111,5000,'Hola'))
for sql in migration:conn.execute(sql)
assert conn.execute('SELECT title,transcript,favorite,analysis_error FROM recordings').fetchone()==('Histórica','Hola',0,'')
print('SQLite: nuevo esquema, cascada de mensajes y migración v1→v2 OK')
