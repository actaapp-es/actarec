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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

/** Inicio de Actarec: grabadora y biblioteca, con la estética de Acta. */
public final class MainActivity extends Activity {
    private static final int REQ_AUDIO=201,REQ_NOTIFICATION=202,REQ_IMPORT=203;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private LinearLayout page,results,filterRow;
    private ScrollView[] scroll=new ScrollView[1];
    private WaveView wave;
    private TextView timer;
    private String search="";
    private int selectedFilter=0,seenTheme;
    private boolean subscribed;
    private final Runnable clock=new Runnable(){@Override public void run(){
        if(timer!=null)timer.setText(RecordingService.running?Ui.mmss(RecordingService.elapsed()):"0:00");
        if(wave!=null)wave.invalidate();handler.postDelayed(this,180);
    }};
    private final BroadcastReceiver updates=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
        String error=i.getStringExtra("error");
        if(error!=null)Toast.makeText(MainActivity.this,error,Toast.LENGTH_LONG).show();
        else if(i.hasExtra(RecordingService.EXTRA_RECORD_ID))Toast.makeText(MainActivity.this,"Grabación guardada",Toast.LENGTH_SHORT).show();
        render();
    }};
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Ui.wrap(base));}
    @Override public void onCreate(Bundle state){super.onCreate(state);seenTheme=Ui.themeGen;render();
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
    @Override protected void onResume(){super.onResume();Ui.recreateIfThemeChanged(this,seenTheme);render();}
    private void detectNotch(){
        if(Build.VERSION.SDK_INT<28)return;
        WindowInsets inset=getWindow().getDecorView().getRootWindowInsets();
        if(inset==null||inset.getDisplayCutout()==null)return;
        boolean notch=false;
        for(Rect r:inset.getDisplayCutout().getBoundingRects())if(r.top<=Ui.dp(this,72)){notch=true;break;}
        Ui.prefs(this).edit().putBoolean("has_cutout",notch).apply();
        if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
    }
    private void render(){
        page=Ui.page(this,Ui.vertical(this),scroll);
        LinearLayout toolbar=Ui.horizontal(this);toolbar.setPadding(Ui.dp(this,24),Ui.dp(this,10),Ui.dp(this,12),0);
        toolbar.addView(Ui.text(this,R.style.Text_Title,"Actarec"),new LinearLayout.LayoutParams(0,Ui.WRAP,1));
        ImageView settings=Ui.iconButton(this,R.drawable.ic_settings,R.color.ink_2,"Abrir ajustes");
        settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        toolbar.addView(settings);page.addView(toolbar);
        drawRecorder();drawLibrary();
    }
    private LinearLayout.LayoutParams side(int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.MATCH,Ui.WRAP);
        p.setMargins(Ui.dp(this,16),Ui.dp(this,top),Ui.dp(this,16),0);return p;
    }
    private void drawRecorder(){
        boolean active=RecordingService.running;
        LinearLayout card=Ui.card(this);card.setPadding(Ui.dp(this,22),Ui.dp(this,20),Ui.dp(this,22),Ui.dp(this,20));
        LinearLayout line=Ui.horizontal(this);
        if(active){
            View dot=new View(this);dot.setBackgroundResource(RecordingService.paused?R.drawable.bg_circle_soft:R.drawable.bg_circle_danger);
            line.addView(dot,Ui.lp(Ui.dp(this,10),Ui.dp(this,10)));line.addView(new View(this),Ui.lp(Ui.dp(this,8),1));
        }
        line.addView(Ui.text(this,R.style.Text_Meta,active?(RecordingService.paused?"En pausa":"Grabando"):"Listo para grabar",14,true));
        card.addView(line);
        timer=Ui.text(this,R.style.Text,active?Ui.mmss(RecordingService.elapsed()):"0:00",48,false);
        timer.setTypeface(android.graphics.Typeface.create("sans-serif-light",0));timer.setFontFeatureSettings("tnum");
        card.addView(timer,Ui.lpTop(this,Ui.WRAP,Ui.WRAP,10));
        wave=new WaveView(this);card.addView(wave,Ui.lpTop(this,Ui.MATCH,Ui.dp(this,52),12));
        if(active){
            LinearLayout controls=Ui.horizontal(this);
            TextView pause=Ui.button(this,RecordingService.paused?"Reanudar":"Pausar",false);
            pause.setOnClickListener(v->control(RecordingService.ACTION_TOGGLE));
            controls.addView(pause,new LinearLayout.LayoutParams(0,Ui.WRAP,1));
            TextView stop=Ui.button(this,"Guardar",true);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.WRAP,1);p.setMarginStart(Ui.dp(this,10));
            controls.addView(stop,p);stop.setOnClickListener(v->control(RecordingService.ACTION_STOP));
            card.addView(controls,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,18));
        }else{
            TextView start=Ui.button(this,"Grabar",true);Ui.icon(start,R.drawable.ic_wave,R.color.on_accent,20);
            start.setOnClickListener(v->startFlow());card.addView(start,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,18));
        }
        TextView note=Ui.text(this,R.style.Text_Meta,active?"Puedes usar otras aplicaciones; los controles están en la notificación."
                :"El audio se queda en tu móvil. Tú decides si enviarlo a Groq.");
        note.setTextSize(12.5f);card.addView(note,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,12));
        page.addView(card,side(18));
    }
    private void drawLibrary(){
        LinearLayout top=Ui.horizontal(this);top.setPadding(Ui.dp(this,26),Ui.dp(this,22),Ui.dp(this,14),0);
        int total=new RecordStore(this).all().size();
        top.addView(Ui.text(this,R.style.Text_Section,"Biblioteca · "+total),new LinearLayout.LayoutParams(0,Ui.WRAP,1));
        TextView importBtn=Ui.textButton(this,"Importar audio");importBtn.setOnClickListener(v->importAudio());top.addView(importBtn);
        page.addView(top);
        EditText finder=new EditText(this);finder.setHint("Buscar grabaciones");finder.setSingleLine(true);finder.setTextSize(15.5f);
        finder.setTextColor(getColor(R.color.ink));finder.setHintTextColor(getColor(R.color.ink_2));finder.setBackgroundResource(R.drawable.bg_field);
        finder.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),Ui.dp(this,14));finder.setText(search);
        finder.setCompoundDrawablePadding(Ui.dp(this,10));
        finder.setCompoundDrawablesRelativeWithIntrinsicBounds(getDrawable(R.drawable.ic_search),null,null,null);
        if(finder.getCompoundDrawablesRelative()[0]!=null)finder.getCompoundDrawablesRelative()[0].setTint(getColor(R.color.ink_2));
        finder.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){search=s.toString();refreshResults();}
            @Override public void afterTextChanged(Editable e){}
        });
        page.addView(finder,side(4));
        filterRow=Ui.horizontal(this);filterRow.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        page.addView(filterRow,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,12));buildFilters();
        results=Ui.vertical(this);page.addView(results);refreshResults();
    }
    private void buildFilters(){
        if(filterRow==null)return;filterRow.removeAllViews();
        String[] labels={"Todas","Analizadas","Favoritas"};
        for(int i=0;i<labels.length;i++){
            final int index=i;TextView chip=Ui.chip(this,labels[i],selectedFilter==i);
            chip.setMinHeight(Ui.dp(this,34));
            chip.setOnClickListener(v->{selectedFilter=index;buildFilters();refreshResults();});
            LinearLayout.LayoutParams p=Ui.lp(Ui.WRAP,Ui.WRAP);if(i>0)p.setMarginStart(Ui.dp(this,8));
            filterRow.addView(chip,p);
        }
    }
    private static long dayStart(java.util.Calendar c){
        java.util.Calendar x=(java.util.Calendar)c.clone();
        x.set(java.util.Calendar.HOUR_OF_DAY,0);x.set(java.util.Calendar.MINUTE,0);x.set(java.util.Calendar.SECOND,0);x.set(java.util.Calendar.MILLISECOND,0);
        return x.getTimeInMillis();
    }
    private String dayLabel(long ms){
        java.util.Calendar now=java.util.Calendar.getInstance(),then=java.util.Calendar.getInstance();then.setTimeInMillis(ms);
        long d=Math.round((dayStart(now)-dayStart(then))/86400000.0);
        if(d<=0)return "Hoy";if(d==1)return "Ayer";
        return new java.text.SimpleDateFormat("d MMMM yyyy",new Locale("es","ES")).format(new java.util.Date(ms));
    }
    private void refreshResults(){
        if(results==null)return;results.removeAllViews();
        List<RecordStore.Recording> recordings=new RecordStore(this).all();
        java.util.List<RecordStore.Recording> shown=new java.util.ArrayList<>();
        for(RecordStore.Recording r:recordings){
            if(selectedFilter==1&&!r.analyzed())continue;
            if(selectedFilter==2&&!r.favorite)continue;
            if(!search.trim().isEmpty()&&!r.title.toLowerCase(Locale.ROOT).contains(search.trim().toLowerCase(Locale.ROOT)))continue;
            shown.add(r);
        }
        if(shown.isEmpty()){
            LinearLayout empty=Ui.card(this);empty.setGravity(Gravity.CENTER_HORIZONTAL);empty.setPadding(Ui.dp(this,20),Ui.dp(this,28),Ui.dp(this,20),Ui.dp(this,28));
            empty.addView(Ui.text(this,R.style.Text_Body,recordings.isEmpty()?"Aún no hay grabaciones":"No hay resultados"));
            TextView m=Ui.text(this,R.style.Text_Meta,recordings.isEmpty()?"Pulsa Grabar para empezar o importa un audio.":"Prueba otro término o cambia el filtro.");
            m.setGravity(Gravity.CENTER);empty.addView(m,Ui.lpTop(this,Ui.WRAP,Ui.WRAP,6));
            results.addView(empty,side(16));return;
        }
        String lastDay=null;LinearLayout group=null;
        for(int i=0;i<shown.size();i++){
            RecordStore.Recording r=shown.get(i);String day=dayLabel(r.createdAt);
            if(!day.equals(lastDay)){
                TextView label=Ui.section(this,day);results.addView(label);
                group=Ui.vertical(this);group.setBackgroundResource(R.drawable.bg_card);group.setClipToOutline(true);
                results.addView(group,side(0));lastDay=day;
            }
            boolean first=group.getChildCount()==0;
            if(!first)group.addView(Ui.separator(this,70));
            addRecording(group,r);
        }
    }
    private void addRecording(LinearLayout group,RecordStore.Recording r){
        LinearLayout box=Ui.horizontal(this);box.setBackgroundResource(R.drawable.bg_card_click);box.setMinimumHeight(Ui.dp(this,68));
        box.setPadding(Ui.dp(this,14),Ui.dp(this,10),Ui.dp(this,16),Ui.dp(this,10));
        FrameLayout av=new FrameLayout(this);
        TextView circle=new TextView(this);circle.setBackgroundResource(R.drawable.bg_avatar);av.addView(circle,new FrameLayout.LayoutParams(Ui.MATCH,Ui.MATCH));
        ImageView icon=Ui.iconView(this,r.analyzed()?R.drawable.ic_doc:R.drawable.ic_wave,R.color.accent,20);
        av.addView(icon,new FrameLayout.LayoutParams(Ui.dp(this,20),Ui.dp(this,20),Gravity.CENTER));
        box.addView(av,Ui.lp(Ui.dp(this,42),Ui.dp(this,42)));
        LinearLayout text=Ui.vertical(this);
        LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,Ui.WRAP,1);tp.setMarginStart(Ui.dp(this,14));
        TextView title=Ui.text(this,R.style.Text_Body,r.title);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text.addView(title);
        String stage=!r.summary.isEmpty()?"Resumida":(r.analyzed()?"Transcrita":"Sin analizar");
        text.addView(Ui.text(this,R.style.Text_Meta,Ui.hour(r.createdAt)+" · "+stage),Ui.lpTop(this,Ui.MATCH,Ui.WRAP,3));
        box.addView(text,tp);
        if(r.favorite){ImageView star=Ui.iconView(this,R.drawable.ic_star_on,R.color.accent,16);LinearLayout.LayoutParams sp=Ui.lp(Ui.dp(this,16),Ui.dp(this,16));sp.setMarginStart(Ui.dp(this,8));box.addView(star,sp);}
        TextView time=Ui.text(this,R.style.Text_Meta,Ui.mmss(r.durationMs),13.5f,true);time.setTextColor(getColor(R.color.accent));
        LinearLayout.LayoutParams dp=Ui.lp(Ui.WRAP,Ui.WRAP);dp.setMarginStart(Ui.dp(this,10));box.addView(time,dp);
        Ui.clickableCard(box,"Abrir "+r.title,v->startActivity(new Intent(this,DetailActivity.class).putExtra("id",r.id)));
        group.addView(box,Ui.lp(Ui.MATCH,Ui.WRAP));
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
        private final float[] levels=new float[44];
        private long lastSample=0;
        WaveView(Context c){super(c);for(int i=0;i<levels.length;i++)levels[i]=.13f;}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
            boolean recording=RecordingService.running&&!RecordingService.paused;
            if(recording&&System.currentTimeMillis()-lastSample>130){
                System.arraycopy(levels,1,levels,0,levels.length-1);
                levels[levels.length-1]=Math.max(.10f,RecordingService.amplitude());lastSample=System.currentTimeMillis();
            }
            float gap=getWidth()/(float)levels.length;
            paint.setColor(getContext().getColor(recording?R.color.accent:R.color.track));paint.setStyle(Paint.Style.FILL);
            for(int i=0;i<levels.length;i++){
                float level=RecordingService.running?levels[i]:.2f+.3f*(float)Math.abs(Math.sin(i*.43f));
                float height=Ui.dp(getContext(),4)+level*(getHeight()-Ui.dp(getContext(),8));
                float left=i*gap+gap*.28f,center=getHeight()/2f;
                canvas.drawRoundRect(left,center-height/2,left+gap*.44f,center+height/2,Ui.dp(getContext(),2),Ui.dp(getContext(),2),paint);
            }
        }
    }
}
