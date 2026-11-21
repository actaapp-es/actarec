package es.actarec.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.List;

/** Detalle: reproductor, resumen, transcripción editable y preguntas con Groq. */
public final class DetailActivity extends Activity {
    private long id;
    private RecordStore store;
    private int tab=0;
    private boolean listening=false;
    private MediaPlayer player;
    private SeekBar seek;
    private TextView playLabel,playTime,status;
    private EditText question;
    private ScrollView scroll;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable playbackTick=new Runnable(){@Override public void run(){
        if(player!=null){
            try{
                if(seek!=null&&!seek.isPressed())seek.setProgress(player.getCurrentPosition());
                if(playTime!=null)playTime.setText(Ui.mmss(player.getCurrentPosition())+" / "+Ui.mmss(player.getDuration()));
                if(playLabel!=null)playLabel.setText(player.isPlaying()?"Pausar":"Reproducir");
            }catch(Exception ignored){}
        }
        handler.postDelayed(this,350);
    }};
    private final BroadcastReceiver progressReceiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent intent){
        if(intent.getLongExtra(AnalysisService.EXTRA_ID,-1)!=id)return;
        if(intent.getBooleanExtra("done",false)){
            String error=intent.getStringExtra(AnalysisService.EXTRA_ERROR);
            if(error!=null)Toast.makeText(DetailActivity.this,error,Toast.LENGTH_LONG).show();
            render();
        }else if(status!=null)status.setText(AnalysisService.progress);
    }};
    @Override public void onCreate(Bundle state){super.onCreate(state);
        id=getIntent().getLongExtra("id",-1);store=new RecordStore(this);if(id<0){finish();return;}
        if(state!=null)tab=state.getInt("detail_tab",0);render();
    }
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putInt("detail_tab",tab);}
    @Override protected void onStart(){super.onStart();
        IntentFilter f=new IntentFilter(AnalysisService.ACTION_PROGRESS);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(progressReceiver,f,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(progressReceiver,f);
        listening=true;handler.removeCallbacks(playbackTick);handler.post(playbackTick);
    }
    @Override protected void onStop(){
        if(listening){unregisterReceiver(progressReceiver);listening=false;}
        handler.removeCallbacks(playbackTick);
        if(player!=null){try{if(player.isPlaying())player.pause();}catch(Exception ignored){}}
        super.onStop();
    }
    @Override protected void onResume(){super.onResume();if(store!=null)render();}
    private boolean processing(){return AnalysisService.working&&AnalysisService.activeId==id;}
    private void render(){
        RecordStore.Recording r=store.get(id);if(r==null){finish();return;}
        LinearLayout root=Ui.vertical(this);scroll=Ui.page(this,root);setContentView(scroll);
        LinearLayout bar=Ui.horizontal(this);
        TextView back=Ui.mini(this,"Biblioteca",Ui.SOFT,Ui.GREEN);
        back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(0,-2,1));
        TextView more=Ui.mini(this,"Más",Ui.WHITE,Ui.GREEN);
        more.setOnClickListener(v->more(r));
        LinearLayout.LayoutParams mp=Ui.lp(this,-2,45);mp.leftMargin=Ui.dp(this,8);bar.addView(more,mp);
        root.addView(bar);root.addView(Ui.space(this,24));
                TextView title=Ui.text(this,r.title,29,Ui.INK,true);title.setLetterSpacing(-.02f);title.setMaxLines(3);
        title.setOnClickListener(v->rename(r));root.addView(title);root.addView(Ui.space(this,8));
        root.addView(Ui.text(this,Ui.date(r.createdAt)+"   ·   "+Ui.mmss(r.durationMs),12.5f,Ui.MUTED,false));
        root.addView(Ui.space(this,21));
        playerCard(root,r);root.addView(Ui.space(this,20));
        tabHeader(root,r);root.addView(Ui.space(this,15));
        if(processing()){
            LinearLayout progress=Ui.horizontal(this);
            progress.setPadding(Ui.dp(this,15),Ui.dp(this,15),Ui.dp(this,15),Ui.dp(this,15));
            progress.setBackground(Ui.shape(this,Ui.SOFT,16));
            progress.addView(Ui.iconSquare(this,"spark",Ui.GREEN,0xFFD4E8DD,42),Ui.lp(this,42,42));
            status=Ui.text(this,AnalysisService.progress.isEmpty()?"Procesando audio…":AnalysisService.progress,12.5f,Ui.GREEN,true);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,12);progress.addView(status,p);
            root.addView(progress);root.addView(Ui.space(this,13));
        }else status=null;
        if(r.error!=null&&!r.error.isEmpty()&&!processing()){
            LinearLayout warning=Ui.card(this);warning.setBackground(Ui.shape(this,0xFFFFECE7,15));
            warning.addView(Ui.text(this,"No se completó el análisis",13.5f,0xFF9C4239,true));warning.addView(Ui.space(this,6));
            warning.addView(Ui.text(this,r.error,12.5f,0xFF904439,false));
            root.addView(warning);root.addView(Ui.space(this,12));
        }
        if(tab==0)summaryTab(root,r);
        else if(tab==1)transcriptTab(root,r);
        else chatTab(root,r);
        root.addView(Ui.space(this,19));
        root.addView(Ui.text(this,"Tus audios permanecen en el teléfono. Solo se envía a Groq el contenido que autorizas a procesar.",11.5f,Ui.FAINT,false));
        root.addView(Ui.space(this,12));
    }
    private void playerCard(LinearLayout root,RecordStore.Recording r){
        LinearLayout panel=Ui.card(this);
        LinearLayout top=Ui.horizontal(this);
        top.addView(Ui.iconSquare(this,"audio",Ui.GREEN,Ui.SOFT,44),Ui.lp(this,44,44));
        LinearLayout title=Ui.vertical(this);
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,-2,1);ip.leftMargin=Ui.dp(this,12);
        title.addView(Ui.tag(this,"Audio original",Ui.MUTED));title.addView(Ui.space(this,6));
        title.addView(Ui.text(this,"Tu grabación",17,Ui.INK,true));top.addView(title,ip);
        panel.addView(top);panel.addView(Ui.space(this,17));
        seek=new SeekBar(this);seek.setMax((int)Math.min(Integer.MAX_VALUE,Math.max(1,r.durationMs)));
        if(Build.VERSION.SDK_INT>=21){seek.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
            seek.setThumbTintList(android.content.res.ColorStateList.valueOf(Ui.GREEN));
            seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFD8D6CF));}
        if(player!=null){try{seek.setMax(player.getDuration());seek.setProgress(player.getCurrentPosition());}catch(Exception ignored){}}
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int p,boolean fromUser){}
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){try{ensurePlayer(new File(r.path));player.seekTo(b.getProgress());}catch(Exception e){toast("No se pudo avanzar en el audio");}}
        });
        panel.addView(seek);panel.addView(Ui.space(this,4));
        playTime=Ui.text(this,"00:00 / "+Ui.mmss(r.durationMs),12,Ui.MUTED,false);
        if(player!=null){try{playTime.setText(Ui.mmss(player.getCurrentPosition())+" / "+Ui.mmss(player.getDuration()));}catch(Exception ignored){}}
        panel.addView(playTime);panel.addView(Ui.space(this,15));
        playLabel=Ui.button(this,player!=null&&player.isPlaying()?"Pausar":"Reproducir",Ui.GREEN,Ui.WHITE);
        playLabel.setOnClickListener(v->togglePlay(new File(r.path)));panel.addView(playLabel);
        root.addView(panel);
    }
    private void ensurePlayer(File audio) throws Exception {
        if(!audio.isFile())throw new IllegalStateException("El audio ya no está disponible");
        if(player!=null)return;
        player=new MediaPlayer();player.setDataSource(audio.getAbsolutePath());player.prepare();
        player.setOnCompletionListener(v->{if(playLabel!=null)playLabel.setText("Reproducir");if(seek!=null)seek.setProgress(0);});
        if(seek!=null)seek.setMax(player.getDuration());
    }
    private void togglePlay(File audio){
        try{ensurePlayer(audio);
            if(player.isPlaying())player.pause();else player.start();
            playLabel.setText(player.isPlaying()?"Pausar":"Reproducir");
        }catch(Exception e){releasePlayer();toast("No se pudo reproducir el archivo");}
    }
    private void tabHeader(LinearLayout root,RecordStore.Recording r){
        LinearLayout tabs=Ui.horizontal(this);
        String[] titles={"Resumen","Texto","Preguntar"};
        for(int i=0;i<titles.length;i++){
            final int index=i;TextView item=Ui.chip(this,titles[i],tab==i);
            item.setContentDescription("Ver "+titles[i]);
            item.setOnClickListener(v->{tab=index;render();scroll.post(()->scroll.smoothScrollTo(0,0));});
            LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);
            if(i>0)params.leftMargin=Ui.dp(this,7);tabs.addView(item,params);
        }
        root.addView(tabs);
    }
    private void summaryTab(LinearLayout root,RecordStore.Recording r){
        if(r.summary.isEmpty()){
            LinearLayout card=Ui.card(this);
            card.addView(Ui.iconSquare(this,"spark",Ui.GREEN,Ui.SOFT,55),Ui.lp(this,55,55));
            card.addView(Ui.space(this,14));card.addView(Ui.section(this,"De audio a claridad."));
            card.addView(Ui.space(this,8));
            card.addView(Ui.text(this,"Groq puede extraer las ideas principales, acuerdos, tareas y datos importantes de esta conversación.",13.5f,Ui.MUTED,false));
            card.addView(Ui.space(this,18));
            TextView analyze=Ui.button(this,r.analyzed()?"✦  Crear resumen":"✦  Transcribir y resumir",Ui.GREEN,Ui.WHITE);
            analyze.setEnabled(!processing());analyze.setAlpha(processing()?.5f:1f);
            analyze.setOnClickListener(v->confirmAnalysis(r));card.addView(analyze);root.addView(card);
        }else{
            for(SummarySections.Section section:SummarySections.parse(r.summary)){
                LinearLayout panel=Ui.card(this);
                LinearLayout heading=Ui.horizontal(this);
                heading.addView(Ui.iconSquare(this,"check",Ui.GREEN,Ui.SOFT,40),Ui.lp(this,40,40));
                TextView name=Ui.text(this,section.title,17,Ui.INK,true);
                LinearLayout.LayoutParams param=new LinearLayout.LayoutParams(0,-2,1);param.leftMargin=Ui.dp(this,11);
                heading.addView(name,param);panel.addView(heading);panel.addView(Ui.space(this,12));
                TextView body=Ui.text(this,section.text,14,Ui.INK,false);body.setTextIsSelectable(true);body.setLineSpacing(Ui.dp(this,4),1);
                panel.addView(body);root.addView(panel,Ui.margin(this,-1,-2,0,11));
            }
            LinearLayout actions=Ui.horizontal(this);
            TextView copy=Ui.mini(this,"Copiar",Ui.SOFT,Ui.GREEN);
            copy.setOnClickListener(v->copy(r.summary));actions.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            TextView again=Ui.mini(this,"↻ Regenerar resumen",Ui.WHITE,Ui.GREEN);
            again.setEnabled(!processing());again.setOnClickListener(v->confirmAnalysis(r));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,8);
            actions.addView(again,p);root.addView(actions);
        }
    }
    private void transcriptTab(LinearLayout root,RecordStore.Recording r){
        LinearLayout panel=Ui.card(this);
        panel.addView(Ui.section(this,"Transcripción"));panel.addView(Ui.space(this,6));
        panel.addView(Ui.text(this,"Texto editable para corregir nombres y cifras antes de resumir.",12.5f,Ui.MUTED,false));
        panel.addView(Ui.space(this,16));
        if(!r.analyzed()){
            panel.addView(Ui.text(this,"Aún no hay transcripción. Analiza el archivo para convertir las voces en texto.",14,Ui.MUTED,false));
            panel.addView(Ui.space(this,16));
            TextView analyze=Ui.button(this,"✦  Analizar audio con Groq",Ui.GREEN,Ui.WHITE);
            analyze.setEnabled(!processing());analyze.setOnClickListener(v->confirmAnalysis(r));panel.addView(analyze);
        }else{
            TextView text=Ui.text(this,r.transcript,14,Ui.INK,false);text.setLineSpacing(Ui.dp(this,4),1.05f);
            text.setTextIsSelectable(true);panel.addView(text);panel.addView(Ui.space(this,17));
            LinearLayout actions=Ui.horizontal(this);
            TextView copy=Ui.mini(this,"Copiar",Ui.SOFT,Ui.GREEN);copy.setOnClickListener(v->copy(r.transcript));
            actions.addView(copy,new LinearLayout.LayoutParams(0,-2,1));
            TextView edit=Ui.mini(this,"Editar texto",Ui.WHITE,Ui.GREEN);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,8);
            edit.setEnabled(!processing());edit.setOnClickListener(v->editTranscript(r));actions.addView(edit,p);
            panel.addView(actions);
        }
        root.addView(panel);
    }
    private void chatTab(LinearLayout root,RecordStore.Recording r){
        LinearLayout introduction=Ui.card(this);
        introduction.addView(Ui.section(this,"Pregunta a Actarec"));introduction.addView(Ui.space(this,8));
        introduction.addView(Ui.text(this,"Encuentra acuerdos, decisiones o detalles sin volver a escuchar toda la reunión.",13.5f,Ui.MUTED,false));
        root.addView(introduction,Ui.margin(this,-1,-2,0,12));
        List<RecordStore.Message> messages=store.messages(id);
        for(RecordStore.Message message:messages){
            boolean mine="user".equals(message.role);
            LinearLayout bubble=Ui.card(this);
            bubble.setBackground(Ui.shape(this,mine?Ui.SOFT:Ui.WHITE,18));
            bubble.setPadding(Ui.dp(this,16),Ui.dp(this,15),Ui.dp(this,16),Ui.dp(this,15));
            bubble.addView(Ui.tag(this,mine?"Tú":"Actarec (Groq)",Ui.GREEN));bubble.addView(Ui.space(this,8));
            TextView content=Ui.text(this,message.body,14,Ui.INK,false);content.setTextIsSelectable(true);
            content.setLineSpacing(Ui.dp(this,3),1);bubble.addView(content);
            root.addView(bubble,Ui.margin(this,-1,-2,0,10));
        }
        LinearLayout ask=Ui.card(this);
        if(!r.analyzed()){
            ask.addView(Ui.text(this,"Transcribe primero la grabación para poder hacer preguntas.",13.5f,Ui.MUTED,false));
            ask.addView(Ui.space(this,12));TextView go=Ui.button(this,"Ir a transcripción",Ui.GREEN,Ui.WHITE);
            go.setOnClickListener(v->{tab=1;render();});ask.addView(go);
        }else{
            if(messages.isEmpty()){
                String[] examples={"¿Qué se acordó?","¿Qué tareas quedan?","¿Qué cantidades se mencionan?"};
                for(String hint:examples){TextView suggestion=Ui.mini(this,hint,Ui.SOFT,Ui.GREEN);
                    suggestion.setOnClickListener(v->{if(question!=null)question.setText(hint);});
                    ask.addView(suggestion,Ui.margin(this,-1,-2,0,8));
                }
                ask.addView(Ui.space(this,5));
            }
            question=Ui.input(this,"Escribe una pregunta…",false);question.setSingleLine(false);
            question.setMinLines(2);question.setMaxLines(4);
            question.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            ask.addView(question);ask.addView(Ui.space(this,10));
            TextView send=Ui.button(this,"Enviar pregunta   →",Ui.GREEN,Ui.WHITE);
            send.setEnabled(!processing());send.setAlpha(processing()?.5f:1f);
            send.setOnClickListener(v->askQuestion());ask.addView(send);
        }
        root.addView(ask);
    }
    private void confirmAnalysis(RecordStore.Recording r){
        if(processing())return;
        if(!SecureKey.configured(this)){missingKey();return;}
        String msg=r.analyzed()?"Se enviará tu transcripción a Groq para generar el resumen. El audio no se volverá a subir.":
                "Se enviará el audio seleccionado a Groq para transcribirlo. Después se enviará el texto para generar un resumen. ¿Autorizas este procesamiento?";
        new AlertDialog.Builder(this).setTitle("Analizar con Groq").setMessage(msg)
                .setNegativeButton("Cancelar",null).setPositiveButton("Autorizar y continuar",(d,w)->startAnalysis(AnalysisService.ACTION_ANALYZE,null)).show();
    }
    private void askQuestion(){
        if(processing()||question==null)return;
        String value=question.getText().toString().trim();if(value.isEmpty())return;
        if(value.length()>1200){toast("Máximo 1.200 caracteres por pregunta");return;}
        if(!SecureKey.configured(this)){missingKey();return;}
        startAnalysis(AnalysisService.ACTION_ASK,value);
    }
    private void startAnalysis(String action,String value){
        if(AnalysisService.working){toast("Espera a que termine el análisis en curso");return;}
        Intent intent=new Intent(this,AnalysisService.class).setAction(action).putExtra(AnalysisService.EXTRA_ID,id);
        if(value!=null)intent.putExtra(AnalysisService.EXTRA_QUESTION,value);
        try{startForegroundService(intent);handler.postDelayed(this::render,400);}
        catch(Exception e){toast("No se pudo iniciar Groq. Comprueba la conexión y permisos.");}
    }
    private void missingKey(){new AlertDialog.Builder(this).setTitle("Conecta Groq")
            .setMessage("Introduce tu clave personal de Groq en Ajustes. Nunca se publica en el repositorio.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Abrir ajustes",(d,w)->startActivity(new Intent(this,SettingsActivity.class))).show();}
    private void editTranscript(RecordStore.Recording r){
        if(processing())return;
        EditText input=Ui.input(this,"Transcripción",false);input.setSingleLine(false);input.setMinLines(7);input.setMaxLines(12);
        input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(r.transcript);
        new AlertDialog.Builder(this).setTitle("Corregir transcripción")
                .setMessage("Al modificar el texto habrá que regenerar el resumen y las respuestas anteriores se eliminarán para evitar contradicciones.")
                .setView(input).setNegativeButton("Cancelar",null)
                .setPositiveButton("Guardar texto",(d,w)->{
                    String updated=input.getText().toString().trim();
                    if(!updated.isEmpty()){store.setTranscript(id,updated);store.clearMessages(id);render();}
                }).show();
    }
    private void rename(RecordStore.Recording r){
        EditText input=Ui.input(this,"Nombre de la grabación",false);input.setText(r.title);input.selectAll();
        new AlertDialog.Builder(this).setTitle("Renombrar grabación").setView(input)
                .setNegativeButton("Cancelar",null).setPositiveButton("Guardar",(d,w)->{
                    String name=input.getText().toString().trim();
                    if(!name.isEmpty()){store.rename(id,name.substring(0,Math.min(120,name.length())));render();}
                }).show();
    }
    private void more(RecordStore.Recording r){
        String[] choices={r.favorite?"Quitar de favoritos":"Añadir a favoritos","Renombrar","Compartir audio","Compartir resumen","Compartir transcripción","Eliminar grabación"};
        new AlertDialog.Builder(this).setTitle("Opciones de grabación").setItems(choices,(d,which)->{
            switch(which){
                case 0:store.favorite(id,!r.favorite);render();break;
                case 1:rename(r);break;
                case 2:shareAudio();break;
                case 3:shareText("Resumen de Actarec",r.summary);break;
                case 4:shareText("Transcripción de Actarec",r.transcript);break;
                case 5:delete();break;
            }
        }).show();
    }
    private void shareAudio(){
        try{
            android.net.Uri uri=AudioProvider.uri(this,id);
            Intent share=new Intent(Intent.ACTION_SEND);share.setType(getContentResolver().getType(uri));
            share.putExtra(Intent.EXTRA_STREAM,uri);
            share.setClipData(ClipData.newRawUri("Actarec",uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share,"Compartir audio"));
        }catch(Exception e){toast("No se pudo compartir el audio");}
    }
    private void shareText(String title,String text){
        if(text==null||text.trim().isEmpty()){toast("Primero genera ese contenido");return;}
        Intent i=new Intent(Intent.ACTION_SEND).setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT,title);i.putExtra(Intent.EXTRA_TEXT,text);
        startActivity(Intent.createChooser(i,"Compartir con…"));
    }
    private void copy(String value){
        ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Actarec",value));
        toast("Copiado al portapapeles");
    }
    private void delete(){
        if(processing()){toast("Espera a que termine el análisis");return;}
        new AlertDialog.Builder(this).setTitle("Eliminar definitivamente")
                .setMessage("Se eliminarán el audio, el resumen, la transcripción y las preguntas guardadas. No se puede deshacer.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Eliminar",(d,w)->{releasePlayer();store.delete(id);finish();}).show();
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
    private void releasePlayer(){if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}}
    @Override protected void onDestroy(){handler.removeCallbacks(playbackTick);releasePlayer();super.onDestroy();}
}
