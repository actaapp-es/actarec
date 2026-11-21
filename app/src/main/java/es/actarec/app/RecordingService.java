package es.actarec.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Grabación del micrófono mediante servicio visible; NO graba audio interno de llamadas. */
public final class RecordingService extends Service {
    public static final String ACTION_START="es.actarec.action.START";
    public static final String ACTION_STOP="es.actarec.action.STOP";
    public static final String ACTION_TOGGLE="es.actarec.action.TOGGLE";
    public static final String ACTION_MARK="es.actarec.action.MARK";
    public static final String ACTION_REFRESH="es.actarec.action.REFRESH";
    public static final String ACTION_STATE="es.actarec.action.STATE";
    public static final String EXTRA_RECORD_ID="record_id";
    private static final int NOTIFICATION_ID=108;
    private static final String CHANNEL="actarec_recording_v1";
    private static final String RECOVERY_FILE="last_recording_file";
    public static volatile boolean running=false,paused=false;
    private static volatile long startAt,pausedTotal,pauseAt,lastRecordId=-1;
    private static volatile float amplitude;
    private static final java.util.ArrayList<Long> marks=new java.util.ArrayList<>();
    /** Añade un marcador en el instante actual de la grabación (también en pausa). */
    public static boolean mark(){
        if(!running)return false;
        synchronized(marks){marks.add(elapsed());}return true;
    }
    public static int markCount(){synchronized(marks){return marks.size();}}
    private static java.util.List<Long> takeMarks(){synchronized(marks){java.util.ArrayList<Long> c=new java.util.ArrayList<>(marks);marks.clear();return c;}}
    private final Handler main=new Handler(Looper.getMainLooper());
    private MediaRecorder recorder;
    private File file;
    private PowerManager.WakeLock wakeLock;
    private long lastNotify=0;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(!running)return;
        try{
            if(!paused && recorder!=null){
                float level=(float)Math.min(1.0,Math.log1p(recorder.getMaxAmplitude())/10.4);
                amplitude=Math.max(level,amplitude*0.62f);
            }else amplitude*=0.63f;
        }catch(Exception ignored){amplitude=0;}
        long now=SystemClock.elapsedRealtime();
        if(now-lastNotify>5000){refreshNotification();lastNotify=now;}
        main.postDelayed(this,300);
    }};
    public static long elapsed(){
        if(!running)return 0;
        long end=paused?pauseAt:SystemClock.elapsedRealtime();
        return Math.max(0,end-startAt-pausedTotal);
    }
    public static float amplitude(){return amplitude;}
    public static long lastId(){return lastRecordId;}
    @Override public void onCreate(){super.onCreate();
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel ch=new NotificationChannel(CHANNEL,"Grabación en curso",NotificationManager.IMPORTANCE_LOW);
        ch.setDescription("Control de pausa, reanudación y parada de las grabaciones");ch.setShowBadge(false);nm.createNotificationChannel(ch);
    }
    @Override public IBinder onBind(Intent i){return null;}
    @Override public int onStartCommand(Intent i,int flags,int startId){
        String action=i==null?"":i.getAction();
        if(ACTION_START.equals(action)){if(!running)startRecording();}
        else if(ACTION_STOP.equals(action)){if(running)stopRecording();else stopSelf();}
        else if(ACTION_TOGGLE.equals(action)){if(running)toggle();}
        else if(ACTION_MARK.equals(action)){if(mark()){refreshNotification();emit(null,-1);}}
        else if(ACTION_REFRESH.equals(action)){if(running)refreshNotification();else stopSelf();}
        else if(!running)stopSelf();
        return START_NOT_STICKY;
    }
    private void lock(){
        try{
            if(wakeLock==null){PowerManager pm=getSystemService(PowerManager.class);
                wakeLock=pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Actarec:MicRecording");
                wakeLock.setReferenceCounted(false);
            }
            if(!wakeLock.isHeld())wakeLock.acquire();
        }catch(Exception ignored){}
    }
    private void unlock(){try{if(wakeLock!=null&&wakeLock.isHeld())wakeLock.release();}catch(Exception ignored){}}
    private void startRecording(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            emit("Actarec necesita permiso de micrófono",-1);stopSelf();return;
        }
        // Solo se inicia desde una actividad visible, cumpliendo Android 14+.
        try{
            startForeground(NOTIFICATION_ID,notification(false,0),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            String name="REC_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(new Date())+".m4a";
            file=new File(new RecordStore(this).recordingsDir(),name);
            getSharedPreferences("prefs",MODE_PRIVATE).edit().putString(RECOVERY_FILE,file.getAbsolutePath()).apply();
            initializeRecorder(file);
            takeMarks();startAt=SystemClock.elapsedRealtime();pausedTotal=0;pauseAt=0;amplitude=0;
            paused=false;running=true;lock();
            refreshNotification();emit(null,-1);
            main.removeCallbacks(tick);main.post(tick);
        }catch(Exception e){
            cleanup();if(file!=null&&file.length()==0)file.delete();file=null;
            getSharedPreferences("prefs",MODE_PRIVATE).edit().remove(RECOVERY_FILE).apply();
            unlock();try{stopForeground(true);}catch(Exception ignored){}
            emit("No se pudo iniciar la grabación: "+safe(e),-1);stopSelf();
        }
    }
    /** Intenta varias configuraciones soportadas para evitar fallos por hardware. */
    private void initializeRecorder(File target) throws Exception {
        Exception last=null;
        // 16 kHz es suficiente para voz y tiene buena compatibilidad en Android.
        // La opción sin frecuencia explícita deja que el fabricante elija.
        for(int rate:new int[]{44100,16000,0}){
            cleanup();
            try{
                recorder=Build.VERSION.SDK_INT>=31?new MediaRecorder(this):new MediaRecorder();
                recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                recorder.setAudioChannels(1);
                if(rate>0)recorder.setAudioSamplingRate(rate);
                recorder.setAudioEncodingBitRate(rate==16000?32000:64000);
                recorder.setOutputFile(target.getAbsolutePath());
                recorder.prepare();recorder.start();
                return;
            }catch(Exception error){
                last=error;
                cleanup();
                // Borra la cabecera MP4 incompleta antes del siguiente intento.
                if(target.isFile())target.delete();
            }
        }
        throw last==null?new IllegalStateException("Grabadora no disponible"):last;
    }
    private void toggle(){
        if(recorder==null)return;
        try{
            if(paused){
                recorder.resume();pausedTotal+=SystemClock.elapsedRealtime()-pauseAt;pauseAt=0;paused=false;lock();
            }else{
                recorder.pause();pauseAt=SystemClock.elapsedRealtime();paused=true;unlock();
            }
            refreshNotification();emit(null,-1);
        }catch(Exception e){emit("No se pudo cambiar el estado: "+safe(e),-1);}
    }
    private void stopRecording(){
        long duration=elapsed();boolean valid=true;
        try{recorder.stop();}catch(Exception e){valid=false;}
        cleanup();unlock();
        long id=-1;
        if(valid&&file!=null&&file.isFile()&&file.length()>1024){
            try{
                String title="Grabación · "+new SimpleDateFormat("d MMM, HH:mm",new Locale("es","ES")).format(new Date());
                RecordStore st=new RecordStore(this);id=st.add(title,file.getAbsolutePath(),duration);
                st.addBookmarks(id,takeMarks());
            }catch(Exception e){emit("Audio guardado, pero no se pudo registrar en la biblioteca",-1);}
        }
        // Si Android interrumpió la salida MP4, conservar el archivo para recuperación manual.
        if(id>=0||file==null||!file.isFile()||file.length()==0){
            getSharedPreferences("prefs",MODE_PRIVATE).edit().remove(RECOVERY_FILE).apply();
            if(file!=null&&file.length()==0)file.delete();
        }
        lastRecordId=id;running=false;paused=false;amplitude=0;
        main.removeCallbacks(tick);
        try{stopForeground(true);}catch(Exception ignored){}
        emit(id<0?"No se completó el archivo de audio. Si es recuperable, aparecerá al abrir Actarec.":null,id);
        stopSelf();
    }
    private static String safe(Exception e){String m=e.getMessage();return m==null?e.getClass().getSimpleName():m;}
    private void cleanup(){
        if(recorder!=null){try{recorder.reset();}catch(Exception ignored){}
            try{recorder.release();}catch(Exception ignored){}recorder=null;}
    }
    private void emit(String error,long id){
        Intent state=new Intent(ACTION_STATE).setPackage(getPackageName());
        if(error!=null)state.putExtra("error",error);
        if(id>=0)state.putExtra(EXTRA_RECORD_ID,id);
        sendBroadcast(state);
    }
    private PendingIntent action(String a,int request){
        return PendingIntent.getService(this,request,new Intent(this,RecordingService.class).setAction(a),
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private Notification notification(boolean paused,long duration){
        PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder n=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_mic)
                .setContentTitle(paused?"Actarec · en pausa":"Actarec · grabando")
                .setContentText(Ui.mmss(duration)+(markCount()>0?"   ·   "+markCount()+" marcadores":"   ·   Pulsa para abrir"))
                .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(R.drawable.ic_stat_mic,paused?"Reanudar":"Pausar",action(ACTION_TOGGLE,3)).build())
                .addAction(new Notification.Action.Builder(R.drawable.ic_stat_mic,"Marcar",action(ACTION_MARK,5)).build())
                .addAction(new Notification.Action.Builder(R.drawable.ic_stat_mic,"Guardar",action(ACTION_STOP,4)).build());
        if(Build.VERSION.SDK_INT>=36){
            // La presentación como isla/Live Update depende de Android y el fabricante.
            try{Notification.Builder.class.getMethod("setShortCriticalText",String.class).invoke(n,paused?"PAUSA":"REC");}catch(Exception ignored){}
            try{Notification.Builder.class.getMethod("setRequestPromotedOngoing",boolean.class).invoke(n,true);}catch(Exception ignored){}
        }
        return n.build();
    }
    private void refreshNotification(){
        if(!running)return;
        // Sin permiso POST_NOTIFICATIONS Android permite la tarea FGS, pero puede
        // restringir la publicación de notificaciones adicionales. No perder audio.
        try{getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,notification(paused,elapsed()));}
        catch(SecurityException ignored){}
    }
    @Override public void onDestroy(){
        main.removeCallbacks(tick);
        if(running&&recorder!=null){
            long d=elapsed();boolean valid=true;
            try{recorder.stop();}catch(Exception e){valid=false;}
            if(valid&&file!=null&&file.length()>1024){
                try{RecordStore st=new RecordStore(this);long rid=st.add("Grabación recuperada",file.getAbsolutePath(),d);st.addBookmarks(rid,takeMarks());
                    getSharedPreferences("prefs",MODE_PRIVATE).edit().remove(RECOVERY_FILE).apply();}catch(Exception ignored){}
            }
        }
        cleanup();unlock();running=false;paused=false;amplitude=0;
        super.onDestroy();
    }
}
