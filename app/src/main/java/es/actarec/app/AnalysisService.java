package es.actarec.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Transcripción, resumen y chat fuera de la Activity para no cancelar al cambiar de pantalla. */
public final class AnalysisService extends Service {
    public static final String ACTION_ANALYZE="es.actarec.action.ANALYZE";
    public static final String ACTION_ASK="es.actarec.action.ASK";
    public static final String ACTION_PROGRESS="es.actarec.action.ANALYSIS_PROGRESS";
    public static final String EXTRA_ID="record_id",EXTRA_QUESTION="question",EXTRA_ERROR="error";
    private static final String CHANNEL="actarec_ai";
    private static final int NOTIFY_ID=119;
    public static volatile boolean working=false;
    public static volatile long activeId=-1;
    public static volatile String progress="";
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private int latestStartId;

    @Override public void onCreate(){super.onCreate();
        NotificationChannel channel=new NotificationChannel(CHANNEL,"Análisis de audio",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("Progreso de las transcripciones y los resúmenes de Actarec");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        latestStartId=startId;
        if(intent==null){stopSelfResult(startId);return START_NOT_STICKY;}
        String action=intent.getAction();long id=intent.getLongExtra(EXTRA_ID,-1);
        if(id<0||(!ACTION_ANALYZE.equals(action)&&!ACTION_ASK.equals(action))){stopSelfResult(startId);return START_NOT_STICKY;}
        if(working){broadcast(id,"Ya hay un análisis en curso",true);return START_NOT_STICKY;}
        working=true;activeId=id;progress="Preparando análisis seguro…";
        try{startForeground(NOTIFY_ID,notification(id,progress),ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);}
        catch(Exception e){working=false;activeId=-1;stopSelfResult(startId);broadcast(id,"Android no permitió iniciar el análisis",true);return START_NOT_STICKY;}
        String question=intent.getStringExtra(EXTRA_QUESTION);
        executor.execute(()->{
            String error=null;
            try{
                RecordStore db=new RecordStore(this);
                RecordStore.Recording record=db.get(id);
                if(record==null)throw new IllegalStateException("La grabación ya no existe");
                String key=SecureKey.get(this);
                if(key.isEmpty())throw new IllegalStateException("Configura una clave de Groq en Ajustes");
                GroqClient groq=new GroqClient(key,getSharedPreferences("prefs",MODE_PRIVATE).getString("language",""));
                if(ACTION_ANALYZE.equals(action)){
                    String transcript=record.transcript;
                    if(transcript==null||transcript.trim().isEmpty()){
                        update(id,"Transcribiendo audio con Groq…");
                        transcript=groq.transcribe(new File(record.path),message->update(id,message));
                        if(transcript.isEmpty())throw new IllegalStateException("Groq no ha reconocido voz en el archivo");
                        db.setTranscript(id,transcript); // No perder la transcripción si el resumen falla
                    }
                    update(id,"Preparando resumen y tareas…");
                    String summary=groq.summarize(transcript,message->update(id,message));
                    if(summary.isEmpty())throw new IllegalStateException("Groq no devolvió un resumen");
                    db.setSummary(id,summary);
                }else{
                    String q=question==null?"":question.trim();
                    if(q.isEmpty()||q.length()>1200)throw new IllegalArgumentException("Pregunta vacía o demasiado larga");
                    if(!record.analyzed())throw new IllegalStateException("Primero transcribe la conversación");
                    update(id,"Buscando la respuesta…");
                    List<RecordStore.Message> previous=db.messages(id);
                    String answer=groq.ask(q,record.transcript,record.summary,previous);
                    if(answer.isEmpty())throw new IllegalStateException("Groq no devolvió una respuesta");
                    db.message(id,"user",q);db.message(id,"assistant",answer);
                }
                db.setError(id,"");
            }catch(Exception e){
                error=e.getMessage()==null?"Error de conexión o procesamiento":e.getMessage();
                try{new RecordStore(this).setError(id,error);}catch(Exception ignored){}
            }finally{
                final String failure=error;
                main.post(()->{
                    working=false;activeId=-1;progress="";
                    try{stopForeground(true);}catch(Exception ignored){}
                    broadcast(id,failure,failure!=null);
                    stopSelfResult(latestStartId);
                });
            }
        });
        return START_NOT_STICKY;
    }
    private Notification notification(long id,String state){
        Intent screen=new Intent(this,DetailActivity.class).putExtra("id",id);
        screen.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open=PendingIntent.getActivity(this,(int)(id%100000),screen,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_stat_mic)
                .setContentTitle("Actarec · IA en curso").setContentText(state)
                .setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE).setCategory(Notification.CATEGORY_SERVICE)
                .setProgress(0,0,true).build();
    }
    private void update(long id,String s){
        progress=s;
        // No detener una transcripción si el usuario ha denegado notificaciones.
        try{getSystemService(NotificationManager.class).notify(NOTIFY_ID,notification(id,s));}
        catch(SecurityException ignored){}
        broadcast(id,null,false);
    }
    private void broadcast(long id,String error,boolean failed){
        Intent data=new Intent(ACTION_PROGRESS).setPackage(getPackageName()).putExtra(EXTRA_ID,id).putExtra("done",!working);
        if(failed&&error!=null)data.putExtra(EXTRA_ERROR,error);
        sendBroadcast(data);
    }
    @Override public void onDestroy(){executor.shutdownNow();super.onDestroy();}
}
