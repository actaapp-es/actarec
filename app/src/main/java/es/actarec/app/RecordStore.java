package es.actarec.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaMetadataRetriever;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Biblioteca local; el proveedor de Groq no recibe nada hasta que el usuario lo autoriza. */
public final class RecordStore extends SQLiteOpenHelper {
    private static final int VERSION=3;
    public static final class Recording {
        public long id,createdAt,durationMs;
        public String title,path,transcript,summary,error;
        public boolean favorite;
        public boolean analyzed(){return transcript!=null&&!transcript.isEmpty();}
    }
    public static final class Bookmark {
        public long id,recordingId,positionMs;
        public String label;
    }
    public static final class Message {
        public long id,recordingId,createdAt;
        public String role,body;
    }
    private final Context context;
    public RecordStore(Context c){super(c.getApplicationContext(),"actarec.db",null,VERSION);context=c.getApplicationContext();}
    @Override public void onConfigure(SQLiteDatabase db){super.onConfigure(db);db.setForeignKeyConstraintsEnabled(true);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE recordings(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT NOT NULL,path TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,duration_ms INTEGER NOT NULL,transcript TEXT NOT NULL DEFAULT '',summary TEXT NOT NULL DEFAULT '',favorite INTEGER NOT NULL DEFAULT 0,analysis_error TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE messages(id INTEGER PRIMARY KEY AUTOINCREMENT,recording_id INTEGER NOT NULL,role TEXT NOT NULL,body TEXT NOT NULL,created_at INTEGER NOT NULL,FOREIGN KEY(recording_id) REFERENCES recordings(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX idx_messages_recording ON messages(recording_id,id)");
        createBookmarks(db);
    }
    private static void createBookmarks(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS bookmarks(id INTEGER PRIMARY KEY AUTOINCREMENT,recording_id INTEGER NOT NULL,position_ms INTEGER NOT NULL,label TEXT NOT NULL DEFAULT '',FOREIGN KEY(recording_id) REFERENCES recordings(id) ON DELETE CASCADE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_bookmarks_recording ON bookmarks(recording_id,position_ms)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int old,int version){
        if(old<2){
            db.execSQL("ALTER TABLE recordings ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE recordings ADD COLUMN analysis_error TEXT NOT NULL DEFAULT ''");
        }
        if(old<3)createBookmarks(db);
    }
    public File recordingsDir(){
        File f=new File(context.getFilesDir(),"recordings");
        if(!f.isDirectory()&&!f.mkdirs())throw new IllegalStateException("No se pudo crear el almacenamiento local");
        return f;
    }
    public boolean validPath(File file){
        try{return file.getCanonicalFile().getParentFile().equals(recordingsDir().getCanonicalFile());}
        catch(Exception e){return false;}
    }
    public synchronized long add(String title,String path,long duration){
        File f=new File(path);
        if(!validPath(f)||!f.isFile()||f.length()==0)throw new IllegalArgumentException("Archivo de audio inválido");
        ContentValues cv=new ContentValues();cv.put("title",title.trim());cv.put("path",path);
        cv.put("created_at",System.currentTimeMillis());cv.put("duration_ms",Math.max(0,duration));
        return getWritableDatabase().insertOrThrow("recordings",null,cv);
    }
    private Recording read(Cursor c){
        Recording r=new Recording();r.id=c.getLong(c.getColumnIndexOrThrow("id"));
        r.title=c.getString(c.getColumnIndexOrThrow("title"));r.path=c.getString(c.getColumnIndexOrThrow("path"));
        r.createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));r.durationMs=c.getLong(c.getColumnIndexOrThrow("duration_ms"));
        r.transcript=c.getString(c.getColumnIndexOrThrow("transcript"));r.summary=c.getString(c.getColumnIndexOrThrow("summary"));
        r.favorite=c.getInt(c.getColumnIndexOrThrow("favorite"))==1;
        r.error=c.getString(c.getColumnIndexOrThrow("analysis_error"));return r;
    }
    public synchronized List<Recording> all(){
        List<Recording> data=new ArrayList<>();
        // La biblioteca no carga transcripciones completas de cientos de reuniones.
        try(Cursor c=getReadableDatabase().rawQuery(
                "SELECT id,title,path,created_at,duration_ms,"+
                "CASE WHEN length(transcript)>0 THEN '1' ELSE '' END AS transcript,"+
                "CASE WHEN length(summary)>0 THEN '1' ELSE '' END AS summary,"+
                "favorite,'' AS analysis_error "+
                "FROM recordings ORDER BY created_at DESC,id DESC",null)){
            while(c.moveToNext())data.add(read(c));
        }
        return data;
    }
    public synchronized Recording get(long id){
        try(Cursor c=getReadableDatabase().query("recordings",null,"id=?",new String[]{Long.toString(id)},null,null,null)){
            return c.moveToFirst()?read(c):null;
        }
    }
    private void update(long id,ContentValues cv){
        getWritableDatabase().update("recordings",cv,"id=?",new String[]{Long.toString(id)});
    }
    public synchronized void setTranscript(long id,String transcript){
        ContentValues cv=new ContentValues();cv.put("transcript",transcript);cv.put("summary","");cv.put("analysis_error","");update(id,cv);
    }
    public synchronized void setSummary(long id,String summary){
        ContentValues cv=new ContentValues();cv.put("summary",summary);cv.put("analysis_error","");update(id,cv);
    }
    public synchronized void setError(long id,String error){
        ContentValues cv=new ContentValues();cv.put("analysis_error",error==null?"":error);update(id,cv);
    }
    public synchronized void rename(long id,String title){
        ContentValues cv=new ContentValues();cv.put("title",title.trim());update(id,cv);
    }
    public synchronized void favorite(long id,boolean favorite){
        ContentValues cv=new ContentValues();cv.put("favorite",favorite?1:0);update(id,cv);
    }
    public synchronized void message(long recordingId,String role,String body){
        ContentValues cv=new ContentValues();cv.put("recording_id",recordingId);
        cv.put("role",role);cv.put("body",body);cv.put("created_at",System.currentTimeMillis());
        getWritableDatabase().insertOrThrow("messages",null,cv);
    }
    public synchronized long addBookmark(long recordingId,long positionMs,String label){
        ContentValues cv=new ContentValues();cv.put("recording_id",recordingId);cv.put("position_ms",Math.max(0,positionMs));cv.put("label",label==null?"":label.trim());
        return getWritableDatabase().insertOrThrow("bookmarks",null,cv);
    }
    public synchronized void addBookmarks(long recordingId,List<Long> positions){
        for(Long p:positions)addBookmark(recordingId,p,"");
    }
    public synchronized List<Bookmark> bookmarks(long recordingId){
        List<Bookmark> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("bookmarks",null,"recording_id=?",new String[]{Long.toString(recordingId)},null,null,"position_ms ASC,id ASC")){
            while(c.moveToNext()){
                Bookmark b=new Bookmark();b.id=c.getLong(c.getColumnIndexOrThrow("id"));b.recordingId=recordingId;
                b.positionMs=c.getLong(c.getColumnIndexOrThrow("position_ms"));b.label=c.getString(c.getColumnIndexOrThrow("label"));list.add(b);
            }
        }
        return list;
    }
    public synchronized void renameBookmark(long id,String label){
        ContentValues cv=new ContentValues();cv.put("label",label==null?"":label.trim());
        getWritableDatabase().update("bookmarks",cv,"id=?",new String[]{Long.toString(id)});
    }
    public synchronized void deleteBookmark(long id){getWritableDatabase().delete("bookmarks","id=?",new String[]{Long.toString(id)});}
    public synchronized void clearMessages(long id){getWritableDatabase().delete("messages","recording_id=?",new String[]{Long.toString(id)});}
    public synchronized List<Message> messages(long id){
        List<Message> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("messages",null,"recording_id=?",new String[]{Long.toString(id)},null,null,"id ASC")){
            while(c.moveToNext()){
                Message m=new Message();m.id=c.getLong(c.getColumnIndexOrThrow("id"));m.recordingId=id;
                m.role=c.getString(c.getColumnIndexOrThrow("role"));m.body=c.getString(c.getColumnIndexOrThrow("body"));
                m.createdAt=c.getLong(c.getColumnIndexOrThrow("created_at"));list.add(m);
            }
        }
        return list;
    }
    public synchronized void delete(long id){
        Recording recording=get(id);if(recording==null)return;
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.delete("messages","recording_id=?",new String[]{Long.toString(id)});
            db.delete("bookmarks","recording_id=?",new String[]{Long.toString(id)});
            db.delete("recordings","id=?",new String[]{Long.toString(id)});db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        File f=new File(recording.path);if(validPath(f))f.delete();
    }
    /** Al reiniciar, recupera MP4 finalizados que quedaron sin indexar, sin borrar datos. */
    public synchronized int recoverOrphans(){
        int recovered=0;
        if(RecordingService.running)return 0;
        File[] files=recordingsDir().listFiles();if(files==null)return 0;
        for(File file:files){
            if(!file.isFile()||!file.getName().endsWith(".m4a")||file.length()<1024)continue;
            try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM recordings WHERE path=? LIMIT 1",new String[]{file.getAbsolutePath()})){
                if(c.moveToFirst())continue;
            }
            MediaMetadataRetriever meta=new MediaMetadataRetriever();
            try{
                meta.setDataSource(file.getAbsolutePath());
                String value=meta.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long duration=value==null?0:Long.parseLong(value);
                if(duration>300){add("Grabación recuperada",file.getAbsolutePath(),duration);recovered++;}
            }catch(Exception ignored){/* Mantener el archivo para recuperación manual */}
            finally{try{meta.release();}catch(Exception ignored){}}
        }
        return recovered;
    }
}
