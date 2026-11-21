package es.actarec.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/** Enlace temporal de solo lectura a un audio privado; no expone rutas del dispositivo. */
public final class AudioProvider extends ContentProvider {
    public static Uri uri(android.content.Context context,long id){return Uri.parse("content://"+context.getPackageName()+".audio/recordings/"+id);}
    @Override public boolean onCreate(){return true;}
    private RecordStore.Recording record(Uri uri) throws FileNotFoundException {
        List<String> parts=uri.getPathSegments();
        if(parts.size()!=2||!"recordings".equals(parts.get(0)))throw new FileNotFoundException("Enlace desconocido");
        long id;try{id=Long.parseLong(parts.get(1));}catch(NumberFormatException e){throw new FileNotFoundException("Identificador inválido");}
        RecordStore db=new RecordStore(getContext());
        RecordStore.Recording r=db.get(id);
        if(r==null||!db.validPath(new File(r.path)))throw new FileNotFoundException("Audio no disponible");
        return r;
    }
    @Override public String getType(Uri uri){
        try{String path=record(uri).path.toLowerCase(java.util.Locale.ROOT);
            if(path.endsWith(".mp3"))return "audio/mpeg";
            if(path.endsWith(".wav"))return "audio/wav";
            if(path.endsWith(".ogg"))return "audio/ogg";
            if(path.endsWith(".flac"))return "audio/flac";
            if(path.endsWith(".webm"))return "audio/webm";
            return "audio/mp4";
        }catch(FileNotFoundException e){return "application/octet-stream";}
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        try{
            RecordStore.Recording r=record(uri);File file=new File(r.path);
            String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
            MatrixCursor c=new MatrixCursor(columns);
            Object[] row=new Object[columns.length];
            for(int i=0;i<columns.length;i++){
                if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=file.getName();
                else if(OpenableColumns.SIZE.equals(columns[i]))row[i]=file.length();
            }
            c.addRow(row);return c;
        }catch(FileNotFoundException e){return null;}
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Solo lectura");
        File f=new File(record(uri).path);
        if(!f.isFile())throw new FileNotFoundException("Audio eliminado");
        return ParcelFileDescriptor.open(f,ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Uri insert(Uri uri,ContentValues value){throw new UnsupportedOperationException("Solo lectura");}
    @Override public int update(Uri uri,ContentValues value,String selection,String[] args){throw new UnsupportedOperationException("Solo lectura");}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException("Solo lectura");}
}
