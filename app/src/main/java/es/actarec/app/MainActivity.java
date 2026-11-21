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
        // Encabezado editorial
        LinearLayout toolbar=Ui.horizontal(this);
        FrameLayout logo=Ui.iconSquare(this,"audio",Ui.WHITE,Ui.GREEN,48);
        toolbar.addView(logo,Ui.lp(this,48,48));
        LinearLayout identity=Ui.vertical(this);
        identity.addView(Ui.text(this,"actarec",20,Ui.INK,true));identity.addView(Ui.space(this,2));
        identity.addView(Ui.text(this,"TU MEMORIA, EN ORDEN",9.5f,Ui.FAINT,true));
        LinearLayout.LayoutParams il=new LinearLayout.LayoutParams(0,-2,1);il.leftMargin=Ui.dp(this,12);
        toolbar.addView(identity,il);
        FrameLayout settings=Ui.iconSquare(this,"settings",Ui.GREEN,Ui.SOFT,46);
        Ui.clickable(settings,"Abrir ajustes",v->startActivity(new Intent(this,SettingsActivity.class)));
        toolbar.addView(settings,Ui.lp(this,46,46));page.addView(toolbar);
        page.addView(Ui.space(this,30));
        page.addView(Ui.tag(this,"CAPTURA CADA IDEA",Ui.GREEN));page.addView(Ui.space(this,9));
        TextView hero=Ui.text(this,"Las buenas ideas\nmerecen quedarse.",31,Ui.INK,true);
        hero.setLetterSpacing(-0.03f);page.addView(hero);
        page.addView(Ui.space(this,9));
        page.addView(Ui.text(this,"Graba, escucha y entiende tus conversaciones sin dejar de usar tu móvil.",14.5f,Ui.MUTED,false));
        page.addView(Ui.space(this,25));
        drawRecorder();page.addView(Ui.space(this,17));
        drawInfoBar();page.addView(Ui.space(this,30));
        drawLibrary();page.addView(Ui.space(this,35));
        page.addView(Ui.tag(this,"ACTAREC  ·  PRIVACIDAD EN PRIMER LUGAR",Ui.FAINT));
    }
    private void drawRecorder(){
        LinearLayout card=Ui.vertical(this);card.setBackground(Ui.shape(this,Ui.DARK_GREEN,27));
        card.setPadding(Ui.dp(this,22),Ui.dp(this,24),Ui.dp(this,22),Ui.dp(this,22));
        boolean active=RecordingService.running;
        LinearLayout line=Ui.horizontal(this);
        FrameLayout symbol=Ui.iconSquare(this,"mic",Ui.WHITE,0xFF2C6659,42);
        line.addView(symbol,Ui.lp(this,42,42));
        TextView badge=Ui.tag(this,active?(RecordingService.paused?"EN PAUSA":"GRABANDO AHORA"):"ESTUDIO DE GRABACIÓN",Ui.MINT);
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(0,-2,1);bp.leftMargin=Ui.dp(this,12);
        line.addView(badge,bp);
        if(active){TextView dot=Ui.text(this,"●",16,RecordingService.paused?Ui.CREAM:Ui.RED,true);line.addView(dot);}
        card.addView(line);card.addView(Ui.space(this,20));
        TextView cardTitle=Ui.text(this,active?(RecordingService.paused?"Grabación pausada":"Estamos grabando"):
                "Tu próxima gran\nconversación.",active?24:25,Ui.WHITE,true);
        card.addView(cardTitle);card.addView(Ui.space(this,14));
        wave=new WaveView(this);card.addView(wave,Ui.lp(this,-1,66));
        card.addView(Ui.space(this,13));
        timer=Ui.text(this,active?Ui.mmss(RecordingService.elapsed()):"00:00",43,Ui.WHITE,false);
        timer.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));
        timer.setLetterSpacing(.035f);card.addView(timer);card.addView(Ui.space(this,22));
        if(active){
            LinearLayout controls=Ui.horizontal(this);
            TextView pause=Ui.button(this,RecordingService.paused?"▶  Reanudar":"Ⅱ  Pausar",Ui.WHITE,Ui.GREEN);
            pause.setOnClickListener(v->control(RecordingService.ACTION_TOGGLE));
            controls.addView(pause,new LinearLayout.LayoutParams(0,-2,1));
            TextView stop=Ui.button(this,"■  Guardar",0xFFBFE7D6,Ui.DARK_GREEN);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,9);
            controls.addView(stop,p);stop.setOnClickListener(v->control(RecordingService.ACTION_STOP));
            card.addView(controls);
        }else{
            TextView start=Ui.button(this,"●   Comenzar grabación",Ui.WHITE,Ui.GREEN);
            start.setOnClickListener(v->startFlow());card.addView(start);
        }
        card.addView(Ui.space(this,12));
        card.addView(Ui.text(this,active?"Sigue usando otras aplicaciones; tienes controles en la notificación.":
            "El audio se queda aquí. Tú decides si enviarlo a Groq.",12,0xFFC6DDD1,false));
        page.addView(card,Ui.lp(this,-1,-2));
    }
    private void drawInfoBar(){
        LinearLayout info=Ui.horizontal(this);
        info.setPadding(Ui.dp(this,15),Ui.dp(this,15),Ui.dp(this,15),Ui.dp(this,15));
        info.setBackground(Ui.shape(this,Ui.SOFT,17));
        info.addView(Ui.iconSquare(this,"shield",Ui.GREEN,0xFFD3E6DC,39),Ui.lp(this,39,39));
        LinearLayout texts=Ui.vertical(this);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.leftMargin=Ui.dp(this,12);
        texts.addView(Ui.text(this,"Grabación protegida",13.5f,Ui.GREEN,true));texts.addView(Ui.space(this,4));
        boolean cutout=getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("has_cutout",false);
        texts.addView(Ui.text(this,cutout?"Notch detectado · cápsula opcional":"Controles siempre en notificaciones",11.5f,Ui.MUTED,false));
        info.addView(texts,lp);page.addView(info);
    }
    private void drawLibrary(){
        LinearLayout top=Ui.horizontal(this);
        LinearLayout left=Ui.vertical(this);left.addView(Ui.section(this,"Biblioteca"));
        int total=new RecordStore(this).all().size();
        left.addView(Ui.space(this,4));left.addView(Ui.text(this,total+(total==1?" grabación guardada":" grabaciones guardadas"),12.5f,Ui.MUTED,false));
        top.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        TextView importBtn=Ui.mini(this,"＋ Importar",Ui.SOFT,Ui.GREEN);
        importBtn.setOnClickListener(v->importAudio());top.addView(importBtn);
        page.addView(top);page.addView(Ui.space(this,15));
        EditText finder=Ui.input(this,"Buscar grabaciones…",false);
        finder.setSingleLine(true);finder.setText(search);finder.setCompoundDrawablePadding(Ui.dp(this,6));
        finder.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        finder.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){search=s.toString();refreshResults();}
            @Override public void afterTextChanged(Editable e){}
        });
        page.addView(finder);page.addView(Ui.space(this,13));
        filterRow=Ui.horizontal(this);page.addView(filterRow);buildFilters();page.addView(Ui.space(this,14));
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
    private void refreshResults(){
        if(results==null)return;results.removeAllViews();
        List<RecordStore.Recording> recordings=new RecordStore(this).all();int shown=0;
        for(RecordStore.Recording r:recordings){
            if(selectedFilter==1&&!r.analyzed())continue;
            if(selectedFilter==2&&!r.favorite)continue;
            if(!search.trim().isEmpty()&&!r.title.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT)))continue;
            addRecording(r);shown++;
        }
        if(shown==0){
            LinearLayout empty=Ui.card(this);empty.setGravity(Gravity.CENTER_HORIZONTAL);
            empty.addView(Ui.iconSquare(this,"audio",Ui.GREEN,Ui.SOFT,60),Ui.lp(this,60,60));
            empty.addView(Ui.space(this,15));empty.addView(Ui.text(this,recordings.isEmpty()?"Tu historia comienza aquí":"No hay resultados",18,Ui.INK,true));
            empty.addView(Ui.space(this,8));
            TextView message=Ui.text(this,recordings.isEmpty()?"Inicia una grabación para guardar lo que de verdad importa.":
                "Prueba otro término de búsqueda o cambia el filtro.",13,Ui.MUTED,false);
            message.setGravity(Gravity.CENTER);empty.addView(message);
            results.addView(empty);
        }
    }
    private void addRecording(RecordStore.Recording r){
        LinearLayout box=Ui.horizontal(this);
        box.setBackground(Ui.touchBackground(this,Ui.WHITE,18,Ui.LINE));
        box.setPadding(Ui.dp(this,14),Ui.dp(this,14),Ui.dp(this,13),Ui.dp(this,14));
        box.addView(Ui.iconSquare(this,r.analyzed()?"document":"audio",Ui.GREEN,Ui.SOFT,48),Ui.lp(this,48,48));
        LinearLayout text=Ui.vertical(this);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.leftMargin=Ui.dp(this,12);
        TextView title=Ui.text(this,r.title,14.5f,Ui.INK,true);title.setMaxLines(2);text.addView(title);
        text.addView(Ui.space(this,5));text.addView(Ui.text(this,Ui.date(r.createdAt),11.5f,Ui.MUTED,false));
        text.addView(Ui.space(this,7));
        String stage=!r.summary.isEmpty()?"✓ Resumida":(r.analyzed()?"✓ Transcrita":"○ Pendiente");
        text.addView(Ui.text(this,stage+"  ·  "+Ui.mmss(r.durationMs),11.5f,r.analyzed()?Ui.GREEN:Ui.FAINT,true));
        box.addView(text,tp);
        if(r.favorite){TextView star=Ui.text(this,"★",17,Ui.GREEN,true);box.addView(star);}
        Ui.Icon arrow=new Ui.Icon(this,"chevron",Ui.GREEN);
        LinearLayout.LayoutParams ap=Ui.lp(this,18,18);ap.leftMargin=Ui.dp(this,8);box.addView(arrow,ap);
        Ui.clickable(box,"Abrir "+r.title,v->{
            startActivity(new Intent(this,DetailActivity.class).putExtra("id",r.id));
        });
        results.addView(box,Ui.margin(this,-1,-2,0,9));
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
            paint.setColor(recording?0xFFBCE7D3:0xFF668E7D);paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<levels.length;i++){
                float level=RecordingService.running?levels[i]:.2f+.3f*(float)Math.abs(Math.sin(i*.43f));
                float height=Ui.dp(getContext(),5)+level*(getHeight()-Ui.dp(getContext(),9));
                float left=i*gap+gap*.29f,center=getHeight()/2f;
                canvas.drawRoundRect(left,center-height/2,left+gap*.42f,center+height/2,Ui.dp(getContext(),3),Ui.dp(getContext(),3),paint);
            }
        }
    }
}
