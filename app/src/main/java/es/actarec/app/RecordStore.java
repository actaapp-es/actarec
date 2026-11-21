package es.actarec.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Audio en almacenamiento privado y metadatos/transcripciones en SQLite local. */
public final class RecordStore extends SQLiteOpenHelper {
    public static final class Recording {
        public long id, createdAt, durationMs;
        public String title, path, transcript, summary;
    }
    public static final class Message {
        public long id, recordingId, createdAt;
        public String role, body;
    }
    private final Context context;
    public RecordStore(Context c) {
        super(c.getApplicationContext(),"actarec.db",null,1);
        context=c.getApplicationContext();
    }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE recordings(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,path TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,duration_ms INTEGER NOT NULL,transcript TEXT NOT NULL DEFAULT '',summary TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,recording_id INTEGER NOT NULL,role TEXT NOT NULL,body TEXT NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY(recording_id) REFERENCES recordings(id))");
        db.execSQL("CREATE INDEX idx_messages_recording ON messages(recording_id,id)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { }
    public File recordingsDir() {
        File dir = new File(context.getFilesDir(), "recordings");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("No se puede crear la carpeta de grabaciones");
        return dir;
    }
    public synchronized long add(String title, String path, long durationMs) {
        ContentValues values=new ContentValues();
        values.put("title",title); values.put("path",path);
        values.put("created_at",System.currentTimeMillis());
        values.put("duration_ms",durationMs);
        return getWritableDatabase().insertOrThrow("recordings",null,values);
    }
    private Recording read(Cursor c) {
        Recording r=new Recording();
        r.id=c.getLong(c.getColumnIndexOrThrow("id"));
        r.title=c.getString(c.getColumnIndexOrThrow("title"));
        r.path=c.getString(c.getColumnIndexOrThrow("path"));
        r.createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));
        r.durationMs=c.getLong(c.getColumnIndexOrThrow("duration_ms"));
        r.transcript=c.getString(c.getColumnIndexOrThrow("transcript"));
        r.summary=c.getString(c.getColumnIndexOrThrow("summary"));
        return r;
    }
    public synchronized List<Recording> all() {
        List<Recording> list = new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("recordings",null,null,null,null,null,"created_at DESC,id DESC")) {
            while(c.moveToNext())list.add(read(c));
        }
        return list;
    }
    public synchronized Recording get(long id) {
        try(Cursor c=getReadableDatabase().query("recordings",null,"id=?",new String[]{Long.toString(id)},null,null,null)) {
            return c.moveToFirst()?read(c):null;
        }
    }
    public synchronized void setAnalysis(long id,String transcript,String summary) {
        ContentValues cv=new ContentValues();
        cv.put("transcript",transcript); cv.put("summary",summary);
        getWritableDatabase().update("recordings",cv,"id=?",new String[]{Long.toString(id)});
    }
    public synchronized void rename(long id,String title) {
        ContentValues cv=new ContentValues();cv.put("title",title);
        getWritableDatabase().update("recordings",cv,"id=?",new String[]{Long.toString(id)});
    }
    public synchronized void message(long recordingId,String role,String body) {
        ContentValues cv=new ContentValues();
        cv.put("recording_id",recordingId); cv.put("role",role);
        cv.put("body",body);cv.put("created_at",System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("messages",null,cv);
    }
    public synchronized List<Message> messages(long id) {
        List<Message> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("messages",null,"recording_id=?",new String[]{Long.toString(id)},null,null,"id ASC")) {
            while(c.moveToNext()){
                Message m=new Message(); m.id=c.getLong(c.getColumnIndexOrThrow("id"));
                m.recordingId=id;m.role=c.getString(c.getColumnIndexOrThrow("role"));
                m.body=c.getString(c.getColumnIndexOrThrow("body"));
                m.createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));list.add(m);
            }
        }
        return list;
    }
    public synchronized void delete(long id) {
        Recording r=get(id);
        if (r==null) return;
        File f=new File(r.path);
        try { if (f.getCanonicalFile().getParentFile().equals(recordingsDir().getCanonicalFile())) f.delete(); }
        catch (Exception ignored) { }
        getWritableDatabase().delete("messages","recording_id=?",new String[]{Long.toString(id)});
        getWritableDatabase().delete("recordings","id=?",new String[]{Long.toString(id)});
    }
}
