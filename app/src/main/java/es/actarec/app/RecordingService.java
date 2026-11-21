package es.actarec.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Grabador real con micrófono en Foreground Service. No captura audio interno ni llamadas. */
public final class RecordingService extends Service {
    public static final String ACTION_START="es.actarec.action.START";
    public static final String ACTION_STOP="es.actarec.action.STOP";
    public static final String ACTION_TOGGLE="es.actarec.action.TOGGLE";
    public static final String ACTION_REFRESH="es.actarec.action.REFRESH";
    public static final String ACTION_STATE="es.actarec.action.STATE";
    public static final String EXTRA_RECORD_ID="record_id";
    private static final String CHANNEL="actarec_recordings";
    private static final int NOTIFICATION_ID=108;
    public static volatile boolean running=false;
    public static volatile boolean paused=false;
    private static volatile long startElapsed=0;
    private static volatile long pausedElapsed=0;
    private static volatile long pauseStart=0;
    private static volatile long lastRecordId=-1;

    private final Handler handler=new Handler(Looper.getMainLooper());
    private MediaRecorder recorder;
    private File file;
    private OverlayController overlay;
    private final Runnable ticker=new Runnable() {
        @Override public void run() {
            if(running) {
                if(overlay!=null)overlay.update(paused,elapsed());
                if((SystemClock.elapsedRealtime()/1000)%5==0) refreshNotification();
                handler.postDelayed(this,1000);
            }
        }
    };
    public static long elapsed() {
        if(!running)return 0;
        long until=paused?pauseStart:SystemClock.elapsedRealtime();
        return Math.max(0,until-startElapsed-pausedElapsed);
    }
    public static long lastId(){return lastRecordId;}

    @Override public void onCreate() {
        super.onCreate();
        NotificationManager manager=getSystemService(NotificationManager.class);
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Grabaciones de Actarec",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Permite pausar y detener grabaciones en segundo plano");
        manager.createNotificationChannel(channel);
        overlay=new OverlayController(this);
    }
    @Override public IBinder onBind(Intent intent) {return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        String action=intent==null?"":intent.getAction();
        if(ACTION_START.equals(action)) {
            if(!running)startRecording();
        }else if(ACTION_STOP.equals(action)) {
            if(running)stopRecording();else stopSelf();
        }else if(ACTION_TOGGLE.equals(action)) {
            if(running)toggle();
        }else if(ACTION_REFRESH.equals(action)) {
            if(running) {overlay.showIfAvailable();refreshNotification();}
            else stopSelf();
        } else if(!running) stopSelf();
        return START_NOT_STICKY;
    }
    private void startRecording() {
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            sendState("Permiso de micrófono no concedido",-1);
            stopSelf();return;
        }
        // Este START se envía exclusivamente desde una Activity visible, por las restricciones de Android 14+.
        try {
            startForeground(NOTIFICATION_ID,notification(false,0),ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            File dir=new RecordStore(this).recordingsDir();
            String name="REC_"+new SimpleDateFormat("yyyyMMdd_HHmmss_SSS",Locale.ROOT).format(new Date())+".m4a";
            file=new File(dir,name);
            recorder=new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioChannels(1);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(48000);
            recorder.setOutputFile(file.getAbsolutePath());
            recorder.prepare();recorder.start();
            startElapsed=SystemClock.elapsedRealtime();pausedElapsed=0;pauseStart=0;
            paused=false;running=true;
            overlay.showIfAvailable();
            sendState(null,-1);
            handler.removeCallbacks(ticker);handler.post(ticker);
        }catch(Exception e) {
            cleanupRecorder();
            if(file!=null)file.delete();
            stopForeground(true);
            sendState("No se pudo iniciar la grabación: "+e.getMessage(),-1);
            stopSelf();
        }
    }
    private void toggle() {
        try {
            if(paused) {
                recorder.resume();
                pausedElapsed+=SystemClock.elapsedRealtime()-pauseStart;
                pauseStart=0;paused=false;
            }else {
                recorder.pause();
                pauseStart=SystemClock.elapsedRealtime();paused=true;
            }
            refreshNotification();
            if(overlay!=null)overlay.update(paused,elapsed());
            sendState(null,-1);
        } catch(Exception e) {sendState("No se pudo pausar o reanudar",-1);}
    }
    private void stopRecording() {
        long duration=elapsed();
        boolean valid=true;
        try {recorder.stop();}catch(Exception e){valid=false;}
        cleanupRecorder();
        long id=-1;
        if(valid && file!=null && file.length()>1024) {
            try {
                String title="Grabación del "+new SimpleDateFormat("d MMM · HH:mm",new Locale("es","ES")).format(new Date());
                id=new RecordStore(this).add(title,file.getAbsolutePath(),duration);
            }catch(Exception e){sendState("No se pudo guardar la grabación",-1);}
        } else if(file!=null) file.delete();
        lastRecordId=id;
        running=false;paused=false;
        handler.removeCallbacks(ticker);
        if(overlay!=null)overlay.remove();
        stopForeground(true);
        sendState(id<0?"La grabación fue demasiado corta o no se guardó":null,id);
        stopSelf();
    }
    private void cleanupRecorder() {
        if(recorder!=null) {
            try{recorder.reset();}catch(Exception ignored){}
            try{recorder.release();}catch(Exception ignored){}
            recorder=null;
        }
    }
    private void sendState(String error,long id) {
        Intent i=new Intent(ACTION_STATE).setPackage(getPackageName());
        if(error!=null)i.putExtra("error",error);
        if(id>=0)i.putExtra(EXTRA_RECORD_ID,id);
        sendBroadcast(i);
    }
    private PendingIntent action(String a,int requestCode) {
        return PendingIntent.getService(this,requestCode,
                new Intent(this,RecordingService.class).setAction(a),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private Notification notification(boolean isPaused,long duration) {
        PendingIntent open=PendingIntent.getActivity(this,2,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b=new Notification.Builder(this,CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_mic)
                .setContentTitle(isPaused?"Actarec · en pausa":"Actarec · grabando")
                .setContentText(Ui.mmss(duration)+" · toca para abrir")
                .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(R.drawable.ic_stat_mic,isPaused?"Reanudar":"Pausar",action(ACTION_TOGGLE,3)).build())
                .addAction(new Notification.Action.Builder(R.drawable.ic_stat_mic,"Detener",action(ACTION_STOP,4)).build());
        // Android 16 puede mostrar una Live Update; se invoca por reflexión para compilar con API 35.
        // Se solicita al sistema: no se fuerza su aparición ni su posición.
        if(Build.VERSION.SDK_INT>=36) {
            try {Notification.Builder.class.getMethod("setShortCriticalText",String.class).invoke(b,isPaused?"PAUSA":"REC");}catch(Exception ignored){}
            try {Notification.Builder.class.getMethod("setRequestPromotedOngoing",boolean.class).invoke(b,true);}catch(Exception ignored){}
        }
        return b.build();
    }
    private void refreshNotification() {
        if(!running)return;
        getSystemService(NotificationManager.class).notify(NOTIFICATION_ID,notification(paused,elapsed()));
    }
    @Override public void onDestroy() {
        handler.removeCallbacks(ticker);
        if(overlay!=null)overlay.remove();
        if(running && recorder!=null) {
            // Si el sistema destruye el servicio, intentamos finalizar y conservar el audio.
            long d=elapsed();
            boolean ok=true;
            try{recorder.stop();}catch(Exception e){ok=false;}
            if(ok && file!=null && file.length()>1024) {
                try{new RecordStore(this).add("Grabación recuperada",file.getAbsolutePath(),d);}catch(Exception ignored){}
            }
        }
        cleanupRecorder();running=false;paused=false;
        super.onDestroy();
    }
}
