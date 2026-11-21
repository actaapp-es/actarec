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
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/** Inicio nativo de Actarec: grabadora persistente y biblioteca privada. */
public final class MainActivity extends Activity {
    private static final int REQ_AUDIO=201,REQ_NOTIFICATION=202,REQ_IMPORT=203;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout page,results,filterRow;
    private ScrollView scroll;
    private WaveView wave;
    private TextView timer;
    private String search="";
    private int selectedFilter=0;
    private boolean subscribed;
    private final Runnable clock=new Runnable(){@Override public void run(){
        if(timer!=null){timer.setText(RecordingService.running?Ui.mmss(RecordingService.elapsed()):"00:00");}
        if(wave!=null)wave.invalidate();handler.postDelayed(this,180);
    }};
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
        String error=i.getStringExtra("error");
        if(error!=null)Toast.makeText(MainActivity.this,error,Toast.LENGTH_LONG).show();
        else if(i.hasExtra(RecordingService.EXTRA_RECORD_ID))Toast.makeText(MainActivity.this,"Grabación guardada",Toast.LENGTH_SHORT).show();
        render();
    }};
    @Override public void onCreate(Bundle state){super.onCreate(state);render();
        getWindow().getDecorView().post(this::detectNotch);
        new Thread(()->{int n=new RecordStore(this).recoverOrphans();
            if(n>0)runOnUiThread(()->{if(!isFinishing()){Toast.makeText(this,"Se recuperaron "+n+" grabaciones",Toast.LENGTH_LONG).show();render();}});
        }).start();
    }
    @Override protected void onStart(){super.onStart();
        IntentFilter filter=new IntentFilter(RecordingService.ACTION_STATE);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(updates,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(updates,filter);
        subscribed=true;handler.removeCallbacks(clock);handler.post(clock);
    }
    @Override protected void onStop(){if(subscribed){unregisterReceiver(updates);subscribed=false;}
        handler.removeCallbacks(clock);super.onStop();
    }
    @Override protected void onResume(){super.onResume();render();}
    private void detectNotch(){
        if(Build.VERSION.SDK_INT<28)return;
        WindowInsets inset=getWindow().getDecorView().getRootWindowInsets();
        if(inset==null||inset.getDisplayCutout()==null)return;
        boolean notch=false;
        for(Rect r:inset.getDisplayCutout().getBoundingRects())if(r.top<=Ui.dp(this,72)){notch=true;break;}
        getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("has_cutout",notch).apply();
        if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
    }
    private void render(){
        page=Ui.vertical(this);scroll=Ui.page(this,page);setContentView(scroll);
        LinearLayout toolbar=Ui.horizontal(this);
        toolbar.addView(Ui.text(this,"Actarec",30,Ui.INK,true),new LinearLayout.LayoutParams(0,-2,1));
        FrameLayout settings=Ui.iconSquare(this,"settings",Ui.INK,Ui.SOFT,44);
        settings.setBackground(Ui.shape(this,Ui.SOFT,22));
        Ui.clickable(settings,"Abrir ajustes",v->startActivity(new Intent(this,SettingsActivity.class)));
        toolbar.addView(settings,Ui.lp(this,44,44));page.addView(toolbar);
        page.addView(Ui.space(this,20));
        drawRecorder();page.addView(Ui.space(this,26));
        drawLibrary();page.addView(Ui.space(this,12));
    }
    private void drawRecorder(){
        LinearLayout card=Ui.card(this);
        boolean active=RecordingService.running;
        LinearLayout line=Ui.horizontal(this);
        if(active){
            TextView dot=Ui.text(this,"",1,Ui.RED,false);
            dot.setBackground(Ui.shape(this,RecordingService.paused?Ui.FAINT:Ui.RED,5));
            line.addView(dot,Ui.lp(this,10,10));line.addView(Ui.space(this,1),Ui.lp(this,8,1));
        }
        line.addView(Ui.text(this,active?(RecordingService.paused?"En pausa":"Grabando"):"Listo para grabar",14,active?Ui.INK:Ui.MUTED,true));
        card.addView(line);card.addView(Ui.space(this,14));
        timer=Ui.text(this,active?Ui.mmss(RecordingService.elapsed()):"00:00",46,Ui.INK,false);
        timer.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));
        card.addView(timer);card.addView(Ui.space(this,12));
        wave=new WaveView(this);card.addView(wave,Ui.lp(this,-1,56));
        card.addView(Ui.space(this,18));
        if(active){
            LinearLayout controls=Ui.horizontal(this);
            TextView pause=Ui.button(this,RecordingService.paused?"Reanudar":"Pausar",Ui.SOFT,Ui.GREEN);
            pause.setOnClickListener(v->control(RecordingService.ACTION_TOGGLE));
            controls.addView(pause,new LinearLayout.LayoutParams(0,-2,1));
            TextView stop=Ui.button(this,"Guardar",Ui.GREEN,Ui.WHITE);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,10);
            controls.addView(stop,p);stop.setOnClickListener(v->control(RecordingService.ACTION_STOP));
            card.addView(controls);
        }else{
            TextView start=Ui.button(this,"Grabar",Ui.GREEN,Ui.WHITE);
            start.setOnClickListener(v->startFlow());card.addView(start);
        }
        card.addView(Ui.space(this,12));
        card.addView(Ui.text(this,active?"Puedes usar otras aplicaciones; los controles están en la notificación.":
            "El audio se queda en tu móvil. Tú decides si enviarlo a Groq.",12.5f,Ui.FAINT,false));
        page.addView(card,Ui.lp(this,-1,-2));
    }
    private void drawLibrary(){
        LinearLayout top=Ui.horizontal(this);
        int total=new RecordStore(this).all().size();
        LinearLayout left=Ui.vertical(this);left.addView(Ui.section(this,"Biblioteca"));
        left.addView(Ui.space(this,3));left.addView(Ui.text(this,total+(total==1?" grabación":" grabaciones"),12.5f,Ui.MUTED,false));
        top.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        TextView importBtn=Ui.mini(this,"Importar audio",Ui.SOFT,Ui.GREEN);
        importBtn.setOnClickListener(v->importAudio());top.addView(importBtn);
        page.addView(top);page.addView(Ui.space(this,14));
        EditText finder=Ui.input(this,"Buscar grabaciones",false);
        finder.setText(search);finder.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        finder.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){search=s.toString();refreshResults();}
            @Override public void afterTextChanged(Editable e){}
        });
        page.addView(finder);page.addView(Ui.space(this,12));
        filterRow=Ui.horizontal(this);page.addView(filterRow);buildFilters();page.addView(Ui.space(this,6));
        results=Ui.vertical(this);page.addView(results);refreshResults();
    }
    private void buildFilters(){
        if(filterRow==null)return;filterRow.removeAllViews();
        String[] labels={"Todas","Analizadas","Favoritas"};
        for(int i=0;i<labels.length;i++){
            final int index=i;TextView chip=Ui.chip(this,labels[i],selectedFilter==i);
            chip.setOnClickListener(v->{selectedFilter=index;buildFilters();refreshResults();});
            LinearLayout.LayoutParams p=Ui.lp(this,-2,-2);if(i>0)p.leftMargin=Ui.dp(this,7);
            filterRow.addView(chip,p);
        }
    }
    private String dayLabel(long ms){
        java.util.Calendar now=java.util.Calendar.getInstance(),then=java.util.Calendar.getInstance();then.setTimeInMillis(ms);
        int d=(int)((startOfDay(now)-startOfDay(then))/86400000L);
        if(d<=0)return "Hoy";if(d==1)return "Ayer";
        return new java.text.SimpleDateFormat("d MMMM yyyy",new Locale("es","ES")).format(new java.util.Date(ms));
    }
    private static long startOfDay(java.util.Calendar c){
        java.util.Calendar x=(java.util.Calendar)c.clone();x.set(java.util.Calendar.HOUR_OF_DAY,12);x.set(java.util.Calendar.MINUTE,0);
        x.set(java.util.Calendar.SECOND,0);x.set(java.util.Calendar.MILLISECOND,0);
        return x.getTimeInMillis()+x.get(java.util.Calendar.ZONE_OFFSET)+x.get(java.util.Calendar.DST_OFFSET);
    }
    private void refreshResults(){
        if(results==null)return;results.removeAllViews();
        List<RecordStore.Recording> recordings=new RecordStore(this).all();int shown=0;
        String lastDay=null;LinearLayout group=null;
        for(RecordStore.Recording r:recordings){
            if(selectedFilter==1&&!r.analyzed())continue;
            if(selectedFilter==2&&!r.favorite)continue;
            if(!search.trim().isEmpty()&&!r.title.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT)))continue;
            String day=dayLabel(r.createdAt);
            if(!day.equals(lastDay)){
                TextView label=Ui.text(this,day,13,Ui.MUTED,true);label.setPadding(Ui.dp(this,4),0,0,Ui.dp(this,8));
                results.addView(label,Ui.margin(this,-1,-2,shown==0?8:18,0));
                group=Ui.vertical(this);group.setBackground(Ui.outlined(this,Ui.WHITE,22,Ui.LINE));
                results.addView(group,Ui.lp(this,-1,-2));lastDay=day;
            }else{
                View line=new View(this);line.setBackgroundColor(Ui.LINE);
                LinearLayout.LayoutParams lp=Ui.lp(this,-1,1);lp.leftMargin=Ui.dp(this,74);group.addView(line,lp);
            }
            addRecording(group,r);shown++;
        }
        if(shown==0){
            LinearLayout empty=Ui.card(this);empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.addView(Ui.text(this,recordings.isEmpty()?"Aún no hay grabaciones":"No hay resultados",16,Ui.INK,true));
            empty.addView(Ui.space(this,6));
            TextView message=Ui.text(this,recordings.isEmpty()?"Pulsa Grabar para empezar o importa un audio.":
                "Prueba otro término o cambia el filtro.",13,Ui.MUTED,false);
            message.setGravity(Gravity.CENTER);empty.addView(message);
            results.addView(empty,Ui.margin(this,-1,-2,8,0));
        }
    }
    private void addRecording(LinearLayout group,RecordStore.Recording r){
        LinearLayout box=Ui.horizontal(this);
        box.setBackground(Ui.touchBackground(this,Ui.WHITE,22,0));
        box.setPadding(Ui.dp(this,16),Ui.dp(this,14),Ui.dp(this,16),Ui.dp(this,14));
        FrameLayout av=Ui.iconSquare(this,r.analyzed()?"document":"audio",Ui.GREEN,Ui.SOFT,48);
        av.setBackground(Ui.shape(this,Ui.SOFT,24));
        box.addView(av,Ui.lp(this,44,44));
        LinearLayout text=Ui.vertical(this);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=Ui.dp(this,14);
        TextView title=Ui.text(this,r.title,15.5f,Ui.INK,false);title.setMaxLines(2);text.addView(title);
        text.addView(Ui.space(this,4));
        String hour=new java.text.SimpleDateFormat("HH:mm",Locale.ROOT).format(new java.util.Date(r.createdAt));
        String stage=!r.summary.isEmpty()?"Resumida":(r.analyzed()?"Transcrita":"Sin analizar");
        text.addView(Ui.text(this,hour+" · "+stage,12.5f,Ui.MUTED,false));
        box.addView(text,tp);
        TextView time=Ui.text(this,Ui.mmss(r.durationMs),13,Ui.GREEN,true);
        LinearLayout.LayoutParams dp=Ui.lp(this,-2,-2);dp.leftMargin=Ui.dp(this,8);box.addView(time,dp);
        if(r.favorite){Ui.Icon star=new Ui.Icon(this,"star",Ui.GREEN);LinearLayout.LayoutParams sp=Ui.lp(this,16,16);sp.leftMargin=Ui.dp(this,8);box.addView(star,sp);}
        Ui.clickable(box,"Abrir "+r.title,v->startActivity(new Intent(this,DetailActivity.class).putExtra("id",r.id)));
        group.addView(box,Ui.lp(this,-1,-2));
    }
    private void control(String action){startService(new Intent(this,RecordingService.class).setAction(action));}
    private void startFlow(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_AUDIO);return;
        }
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED
                &&!getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("asked_notifications",false)){
            getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("asked_notifications",true).apply();
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTIFICATION);return;
        }
        try{startForegroundService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_START));}
        catch(Exception e){new AlertDialog.Builder(this).setTitle("No se pudo grabar")
                .setMessage("Android no permitió iniciar el micrófono. Comprueba los permisos de Actarec y vuelve a intentarlo.")
                .setPositiveButton("Entendido",null).show();}
    }
    @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] results){
        super.onRequestPermissionsResult(code,permissions,results);
        if(code==REQ_AUDIO){
            if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startFlow();
            else Toast.makeText(this,"Concede acceso al micrófono para grabar",Toast.LENGTH_LONG).show();
        }else if(code==REQ_NOTIFICATION){
            if(results.length==0||results[0]!=PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this,"Activa las notificaciones para ver los controles fuera de la app",Toast.LENGTH_LONG).show();
            startFlow();
        }
    }
    private void importAudio(){
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");startActivityForResult(intent,REQ_IMPORT);
    }
    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);
        if(request!=REQ_IMPORT||result!=RESULT_OK||data==null||data.getData()==null)return;
        Uri source=data.getData();Toast.makeText(this,"Importando archivo…",Toast.LENGTH_SHORT).show();
        new Thread(()->{
            File destination=null;
            try{
                String name="audio";
                try(Cursor c=getContentResolver().query(source,null,null,null,null)){
                    if(c!=null&&c.moveToFirst()){
                        int index=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if(index>=0){String n=c.getString(index);if(n!=null&&!n.trim().isEmpty())name=n.trim();}
                    }
                }
                String lower=name.toLowerCase(Locale.ROOT),extension=null;
                for(String ext:new String[]{".m4a",".mp3",".mp4",".wav",".ogg",".flac",".webm"})
                    if(lower.endsWith(ext)){extension=ext;break;}
                if(extension==null)throw new IllegalArgumentException("Usa M4A, MP3, WAV, OGG, FLAC, MP4 o WebM");
                destination=File.createTempFile("import_",extension,new RecordStore(this).recordingsDir());
                long amount=0;
                try(InputStream in=getContentResolver().openInputStream(source);FileOutputStream out=new FileOutputStream(destination)){
                    if(in==null)throw new IllegalArgumentException("No se puede abrir el archivo");
                    byte[] buffer=new byte[65536];int n;
                    while((n=in.read(buffer))!=-1){amount+=n;
                        if(amount>750L*1024*1024)throw new IllegalArgumentException("Archivo superior a 750 MB");
                        out.write(buffer,0,n);
                    }
                }
                if(amount<1024)throw new IllegalArgumentException("El audio está vacío o dañado");
                long duration=0;
                android.media.MediaMetadataRetriever media=new android.media.MediaMetadataRetriever();
                try{media.setDataSource(destination.getAbsolutePath());
                    String meta=media.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    if(meta!=null)duration=Long.parseLong(meta);
                }finally{media.release();}
                if(duration<100)throw new IllegalArgumentException("Formato de audio no compatible");
                String visible=name.length()>75?name.substring(0,75)+"…":name;
                new RecordStore(this).add("Importada · "+visible,destination.getAbsolutePath(),duration);
                runOnUiThread(()->{if(!isFinishing()){Toast.makeText(this,"Audio importado",Toast.LENGTH_SHORT).show();render();}});
            }catch(Exception e){
                if(destination!=null)destination.delete();
                String message=e.getMessage()==null?"Archivo incompatible":e.getMessage();
                runOnUiThread(()->Toast.makeText(this,"Error de importación: "+message,Toast.LENGTH_LONG).show());
            }
        }).start();
    }
    /** Vista reactiva con intensidad real del micrófono, no una onda inventada. */
    private static final class WaveView extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] levels=new float[42];
        private long lastSample=0;
        WaveView(Context c){super(c);for(int i=0;i<levels.length;i++)levels[i]=.13f;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
            boolean recording=RecordingService.running&&!RecordingService.paused;
            if(recording&&System.currentTimeMillis()-lastSample>130){
                System.arraycopy(levels,1,levels,0,levels.length-1);
                levels[levels.length-1]=Math.max(.10f,RecordingService.amplitude());lastSample=System.currentTimeMillis();
            }
            float gap=getWidth()/(float)levels.length;
            paint.setColor(recording?Ui.GREEN:0xFFD8D6CF);paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<levels.length;i++){
                float level=RecordingService.running?levels[i]:.2f+.3f*(float)Math.abs(Math.sin(i*.43f));
                float height=Ui.dp(getContext(),5)+level*(getHeight()-Ui.dp(getContext(),9));
                float left=i*gap+gap*.29f,center=getHeight()/2f;
                canvas.drawRoundRect(left,center-height/2,left+gap*.42f,center+height/2,Ui.dp(getContext(),3),Ui.dp(getContext(),3),paint);
            }
        }
    }
}
