package es.actarec.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/** Pantalla de inicio: grabar y consultar la biblioteca local. */
public final class MainActivity extends Activity {
    private static final int REQ_AUDIO=201,REQ_NOTIFICATIONS=202,REQ_IMPORT=203;
    private final Handler ui=new Handler(Looper.getMainLooper());
    private LinearLayout root;
    private TextView timer;
    private WaveView wave;
    private boolean receiverRegistered=false;
    private final Runnable tick=new Runnable(){@Override public void run(){
        if(timer!=null&&RecordingService.running)timer.setText(Ui.mmss(RecordingService.elapsed()));
        if(wave!=null)wave.invalidate();
        ui.postDelayed(this,1000);
    }};
    private final BroadcastReceiver recordingUpdates=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){
            String err=intent.getStringExtra("error");
            if(err!=null)Toast.makeText(MainActivity.this,err,Toast.LENGTH_LONG).show();
            else if(intent.hasExtra(RecordingService.EXTRA_RECORD_ID))Toast.makeText(MainActivity.this,"Grabación guardada",Toast.LENGTH_SHORT).show();
            render();
        }
    };
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        render();
        getWindow().getDecorView().post(this::detectCutout);
    }
    private void detectCutout() {
        if(Build.VERSION.SDK_INT<28)return;
        WindowInsets insets=getWindow().getDecorView().getRootWindowInsets();
        if(insets!=null) {
            boolean cutout=false;
            if(insets.getDisplayCutout()!=null) {
                for(Rect r:insets.getDisplayCutout().getBoundingRects()) {
                    if(r.top<Ui.dp(this,80)){cutout=true;break;}
                }
            }
            getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("has_cutout",cutout).apply();
            if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
        }
    }
    @Override protected void onStart(){
        super.onStart();
        IntentFilter filter=new IntentFilter(RecordingService.ACTION_STATE);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(recordingUpdates,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(recordingUpdates,filter);
        receiverRegistered=true;ui.post(tick);
    }
    @Override protected void onStop(){
        if(receiverRegistered){unregisterReceiver(recordingUpdates);receiverRegistered=false;}
        ui.removeCallbacks(tick);
        super.onStop();
    }
    @Override protected void onResume(){super.onResume();render();}

    private void render() {
        root=Ui.vertical(this);
        ScrollView screen=Ui.page(this,root);
        setContentView(screen);
        LinearLayout top=Ui.horizontal(this);
        TextView brand=Ui.text(this,"●  actarec",21,Ui.GREEN,true);
        brand.setLetterSpacing(-.02f);
        top.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView settings=Ui.mini(this,"Ajustes  ⚙",Ui.SOFT,Ui.GREEN);
        settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        top.addView(settings);
        root.addView(top);
        root.addView(Ui.space(this,33));
        root.addView(Ui.tag(this,"TU MEMORIA, CONTIGO",Ui.GREEN));
        root.addView(Ui.space(this,10));
        root.addView(Ui.text(this,"Captura lo\nimportante.",34,Ui.INK,true));
        root.addView(Ui.space(this,11));
        root.addView(Ui.text(this,"Graba una conversación y sigue usando tu móvil. Actarec se encarga de recordarla.",15,Ui.MUTED,false));
        root.addView(Ui.space(this,26));

        LinearLayout record=Ui.vertical(this);
        record.setBackground(Ui.shape(this,Ui.DARK_GREEN,25));
        record.setPadding(Ui.dp(this,22),Ui.dp(this,25),Ui.dp(this,22),Ui.dp(this,23));
        root.addView(record,new LinearLayout.LayoutParams(-1,-2));
        boolean active=RecordingService.running;
        record.addView(Ui.tag(this,active?(RecordingService.paused?"●  GRABACIÓN EN PAUSA":"●  GRABANDO AHORA"):"GRABADORA PERSONAL",0xFFB9D5CA));
        record.addView(Ui.space(this,13));
        record.addView(Ui.text(this,active?(RecordingService.paused?"Grabación pausada":"Escuchando…"):"Una idea merece\nser recordada.",active?27:25,Ui.WHITE,true));
        record.addView(Ui.space(this,22));
        wave=new WaveView(this);
        record.addView(wave,new LinearLayout.LayoutParams(-1,Ui.dp(this,66)));
        record.addView(Ui.space(this,12));
        timer=Ui.text(this,active?Ui.mmss(RecordingService.elapsed()):"00:00",39,Ui.WHITE,true);
        timer.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));
        record.addView(timer);
        record.addView(Ui.space(this,20));
        if(active) {
            LinearLayout actions=Ui.horizontal(this);
            TextView toggle=Ui.button(this,RecordingService.paused?"▶  Reanudar":"Ⅱ  Pausar",Ui.WHITE,Ui.GREEN);
            toggle.setOnClickListener(v->service(RecordingService.ACTION_TOGGLE));
            actions.addView(toggle,new LinearLayout.LayoutParams(0,-2,1));
            TextView stop=Ui.button(this,"■  Guardar",0xFFE3EDE8,Ui.GREEN);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,9);
            actions.addView(stop,p);
            stop.setOnClickListener(v->service(RecordingService.ACTION_STOP));
            record.addView(actions);
        }else {
            TextView start=Ui.button(this,"●  Iniciar grabación",Ui.WHITE,Ui.GREEN);
            start.setOnClickListener(v->startFlow());
            record.addView(start);
        }
        record.addView(Ui.space(this,13));
        record.addView(Ui.text(this,active?"Puedes cambiar de aplicación. Controla la grabación desde la notificación o cápsula flotante.":"Audio guardado solo en tu teléfono. Se envía a Groq cuando tú lo autorizas.",12,0xFFCADDD4,false));
        root.addView(Ui.space(this,25));

        LinearLayout features=Ui.horizontal(this);
        features.setBackground(Ui.shape(this,Ui.SOFT,18));
        features.setPadding(Ui.dp(this,16),Ui.dp(this,15),Ui.dp(this,16),Ui.dp(this,15));
        TextView mic=Ui.text(this,"◉",27,Ui.GREEN,true);features.addView(mic);
        LinearLayout descriptions=Ui.vertical(this);
        LinearLayout.LayoutParams detailsParams=new LinearLayout.LayoutParams(0,-2,1);detailsParams.leftMargin=Ui.dp(this,13);
        descriptions.addView(Ui.text(this,"Segundo plano activado",14,Ui.GREEN,true));
        descriptions.addView(Ui.space(this,3));
        String cutout=getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("has_cutout",false)?"Notch detectado · activa la cápsula en Ajustes":"Control desde notificaciones";
        descriptions.addView(Ui.text(this,cutout,12,Ui.MUTED,false));
        features.addView(descriptions,detailsParams);
        root.addView(features);
        root.addView(Ui.space(this,33));

        LinearLayout libHeader=Ui.horizontal(this);
        TextView lib=Ui.text(this,"Tus grabaciones",22,Ui.INK,true);
        libHeader.addView(lib,new LinearLayout.LayoutParams(0,-2,1));
        TextView importBtn=Ui.mini(this,"＋ Importar",Ui.WHITE,Ui.GREEN);
        importBtn.setOnClickListener(v->importAudio());libHeader.addView(importBtn);
        root.addView(libHeader);
        root.addView(Ui.space(this,13));
        List<RecordStore.Recording> records=new RecordStore(this).all();
        if(records.isEmpty()){
            LinearLayout empty=Ui.card(this);
            empty.addView(Ui.text(this,"Aún no hay grabaciones",17,Ui.INK,true));
            empty.addView(Ui.space(this,6));
            empty.addView(Ui.text(this,"Pulsa «Iniciar grabación» para guardar tu primera conversación.",14,Ui.MUTED,false));
            root.addView(empty);
        }else {
            for(RecordStore.Recording r:records){
                LinearLayout item=Ui.card(this);
                item.setBackground(Ui.outline(this,Ui.WHITE,19,Ui.LINE));
                LinearLayout row=Ui.horizontal(this);
                TextView icon=Ui.mini(this,"♫",Ui.SOFT,Ui.GREEN);
                row.addView(icon);
                LinearLayout info=Ui.vertical(this);
                LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.leftMargin=Ui.dp(this,14);
                TextView title=Ui.text(this,r.title,15,Ui.INK,true);title.setMaxLines(2);
                info.addView(title);
                info.addView(Ui.space(this,5));
                info.addView(Ui.text(this,Ui.date(r.createdAt)+" · "+Ui.mmss(r.durationMs),12,Ui.MUTED,false));
                info.addView(Ui.space(this,7));
                info.addView(Ui.text(this,r.transcript.isEmpty()?"Pendiente de transcribir":"✓ Analizada con Groq",12,r.transcript.isEmpty()?Ui.FAINT:Ui.GREEN,true));
                row.addView(info,ip);
                row.addView(Ui.text(this,"›",27,Ui.GREEN,false));
                item.addView(row);
                item.setOnClickListener(v->{Intent d=new Intent(this,DetailActivity.class);d.putExtra("id",r.id);startActivity(d);});
                LinearLayout.LayoutParams lp=Ui.lp(this,-1,-2);lp.bottomMargin=Ui.dp(this,9);
                root.addView(item,lp);
            }
        }
        root.addView(Ui.space(this,26));
        root.addView(Ui.text(this,"ACTAREC   ·   GRABA   ·   TRANSCRIBE   ·   RECUERDA",10,Ui.FAINT,true));
    }

    private void service(String action){startService(new Intent(this,RecordingService.class).setAction(action));}
    private void startFlow(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;
        }
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATIONS);return;
        }
        try{startForegroundService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_START));}
        catch(Exception e){new AlertDialog.Builder(this).setMessage("Android no permitió iniciar la grabación: "+e.getMessage()).setPositiveButton("Aceptar",null).show();}
    }
    @Override public void onRequestPermissionsResult(int req,String[] permissions,int[] results){
        super.onRequestPermissionsResult(req,permissions,results);
        if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startFlow();
        else new AlertDialog.Builder(this).setTitle("Permiso necesario")
                .setMessage(req==REQ_AUDIO?"Actarec necesita permiso de micrófono para grabar.":"Actarec necesita notificaciones para que puedas detener y pausar la grabación desde fuera de la app.")
                .setPositiveButton("Entendido",null).show();
    }
    private void importAudio(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("audio/*");
        startActivityForResult(i,REQ_IMPORT);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=REQ_IMPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();
        Toast.makeText(this,"Importando audio…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            File dest=null;
            try{
                String name="audio";
                try(Cursor cursor=getContentResolver().query(uri,null,null,null,null)){
                    if(cursor!=null&&cursor.moveToFirst()){
                        int idx=cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if(idx>=0){String found=cursor.getString(idx);if(found!=null&&!found.isEmpty())name=found;}
                    }
                }
                String lower=name.toLowerCase(Locale.ROOT);
                String ext=".m4a";
                for(String e:new String[]{".m4a",".mp3",".mp4",".wav",".ogg",".flac",".webm"})if(lower.endsWith(e)){ext=e;break;}
                dest=new File(new RecordStore(this).recordingsDir(),"import_"+System.currentTimeMillis()+ext);
                try(InputStream in=getContentResolver().openInputStream(uri);FileOutputStream out=new FileOutputStream(dest)){
                    if(in==null)throw new IllegalStateException("Archivo inaccesible");
                    byte[] buf=new byte[65536];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
                }
                long duration=0;
                try{
                    android.media.MediaMetadataRetriever retriever=new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(dest.getAbsolutePath());
                    String ms=retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if(ms!=null)duration=Long.parseLong(ms);
                    retriever.release();
                }catch(Exception ignored){}
                new RecordStore(this).add("Importada: "+name,dest.getAbsolutePath(),duration);
                runOnUiThread(()->{Toast.makeText(this,"Audio importado",Toast.LENGTH_SHORT).show();render();});
            }catch(Exception e){
                if(dest!=null)dest.delete();
                runOnUiThread(()->Toast.makeText(this,"No se pudo importar: "+e.getMessage(),Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private static final class WaveView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        WaveView(Context c){super(c);}
        @Override protected void onDraw(Canvas c){
            super.onDraw(c);
            int count=35;float spacing=getWidth()/(float)count;
            double now=System.currentTimeMillis()/500.0;
            boolean active=RecordingService.running&&!RecordingService.paused;
            paint.setColor(active?0xFFB4D7C7:0xFF66877A);
            for(int i=0;i<count;i++){
                double envelope=.20+.78*Math.pow(Math.sin((i+1)*.56+now*(active?1:0)) * Math.cos(i*.19),2);
                float height=Ui.dp(getContext(),8)+(float)envelope*(getHeight()-Ui.dp(getContext(),10));
                float center=getHeight()/2f;
                float left=i*spacing+spacing*.24f;
                c.drawRoundRect(left,center-height/2,left+spacing*.49f,center+height/2,Ui.dp(getContext(),3),Ui.dp(getContext(),3),paint);
            }
        }
    }
}
