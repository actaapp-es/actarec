package es.actarec.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.util.List;

/** Detalle de una grabación: reproducción, transcripción, resumen y chat. */
public final class DetailActivity extends Activity {
    private long id;
    private RecordStore store;
    private MediaPlayer player;
    private boolean busy;
    private TextView status;
    private EditText questionField;
    private ScrollView scroll;
    private LinearLayout root;

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        id=getIntent().getLongExtra("id",-1);
        store=new RecordStore(this);
        render();
    }
    @Override protected void onResume(){super.onResume();if(store!=null)render();}

    private void render(){
        RecordStore.Recording r=store.get(id);
        if(r==null){finish();return;}
        root=Ui.vertical(this);
        scroll=Ui.page(this,root);
        setContentView(scroll);
        TextView back=Ui.text(this,"‹  Volver a grabaciones",14,Ui.GREEN,true);
        back.setPadding(0,Ui.dp(this,4),0,Ui.dp(this,12));
        back.setOnClickListener(v->finish());root.addView(back);
        root.addView(Ui.space(this,16));
        root.addView(Ui.tag(this,"TU ARCHIVO DE AUDIO",Ui.GREEN));
        root.addView(Ui.space(this,9));
        TextView title=Ui.text(this,r.title,27,Ui.INK,true);
        title.setOnClickListener(v->rename(r));root.addView(title);
        root.addView(Ui.space(this,9));
        root.addView(Ui.text(this,Ui.date(r.createdAt)+"   ·   "+Ui.mmss(r.durationMs),13,Ui.MUTED,false));
        root.addView(Ui.space(this,22));

        LinearLayout audio=Ui.card(this);
        audio.setBackground(Ui.shape(this,Ui.DARK_GREEN,23));
        audio.addView(Ui.tag(this,"GRABACIÓN ORIGINAL",0xFFB9D5CA));
        audio.addView(Ui.space(this,12));
        LinearLayout actions=Ui.horizontal(this);
        TextView play=Ui.button(this,player!=null&&player.isPlaying()?"Ⅱ  Pausar audio":"▶  Reproducir",Ui.WHITE,Ui.GREEN);
        actions.addView(play,new LinearLayout.LayoutParams(0,-2,1));
        play.setOnClickListener(v->play(new File(r.path),play));
        TextView rename=Ui.mini(this,"Renombrar",0xFF2E6357,Ui.WHITE);
        LinearLayout.LayoutParams margin=Ui.lp(this,-2,-2);margin.leftMargin=Ui.dp(this,9);
        actions.addView(rename,margin);rename.setOnClickListener(v->rename(r));
        audio.addView(actions);root.addView(audio);
        root.addView(Ui.space(this,22));

        LinearLayout summary=Ui.card(this);
        TextView summaryHeader=Ui.text(this,"✦  Resumen inteligente",20,Ui.INK,true);summary.addView(summaryHeader);
        summary.addView(Ui.space(this,11));
        if(r.summary.isEmpty())summary.addView(Ui.text(this,"Aún no has analizado esta grabación. Groq puede extraer decisiones, datos clave y tareas.",14,Ui.MUTED,false));
        else {
            TextView result=Ui.text(this,r.summary,14,Ui.INK,false);
            result.setTextIsSelectable(true);result.setLineSpacing(Ui.dp(this,4),1);
            summary.addView(result);
            summary.addView(Ui.space(this,9));
            TextView copy=Ui.mini(this,"Copiar resumen",Ui.SOFT,Ui.GREEN);
            copy.setOnClickListener(v->copy(r.summary));summary.addView(copy);
        }
        summary.addView(Ui.space(this,17));
        TextView analyze=Ui.button(this,r.transcript.isEmpty()?"✦  Transcribir y resumir con Groq":"↻  Repetir análisis",Ui.GREEN,Ui.WHITE);
        analyze.setOnClickListener(v->confirmAnalysis(r));summary.addView(analyze);
        status=Ui.text(this,busy?"Procesando con Groq…":"",12,Ui.MUTED,false);
        LinearLayout.LayoutParams sp=Ui.lp(this,-1,-2);sp.topMargin=Ui.dp(this,8);
        summary.addView(status,sp);
        root.addView(summary);
        root.addView(Ui.space(this,14));

        LinearLayout transcript=Ui.card(this);
        transcript.addView(Ui.text(this,"≡  Transcripción",20,Ui.INK,true));
        transcript.addView(Ui.space(this,10));
        if(r.transcript.isEmpty())transcript.addView(Ui.text(this,"Aquí aparecerá el texto de la conversación, listo para consultar y copiar.",14,Ui.MUTED,false));
        else {
            TextView txt=Ui.text(this,r.transcript,14,Ui.INK,false);
            txt.setTextIsSelectable(true);txt.setLineSpacing(Ui.dp(this,4),1);
            transcript.addView(txt);
            transcript.addView(Ui.space(this,9));
            TextView copy=Ui.mini(this,"Copiar transcripción",Ui.SOFT,Ui.GREEN);
            copy.setOnClickListener(v->copy(r.transcript));transcript.addView(copy);
        }
        root.addView(transcript);
        root.addView(Ui.space(this,14));

        LinearLayout chat=Ui.card(this);
        chat.addView(Ui.text(this,"◈  Pregunta a Actarec",20,Ui.INK,true));
        chat.addView(Ui.space(this,9));
        chat.addView(Ui.text(this,"Pregunta por acuerdos, cifras, nombres o cualquier detalle mencionado.",13,Ui.MUTED,false));
        chat.addView(Ui.space(this,14));
        List<RecordStore.Message> messages=store.messages(id);
        for(RecordStore.Message m:messages){
            LinearLayout bubble=Ui.vertical(this);
            bubble.setPadding(Ui.dp(this,13),Ui.dp(this,13),Ui.dp(this,13),Ui.dp(this,13));
            boolean user="user".equals(m.role);
            bubble.setBackground(Ui.shape(this,user?Ui.SOFT:Ui.BG,14));
            bubble.addView(Ui.tag(this,user?"TÚ":"ACTAREC",Ui.GREEN));
            bubble.addView(Ui.space(this,5));
            TextView body=Ui.text(this,m.body,14,Ui.INK,false);body.setTextIsSelectable(true);
            bubble.addView(body);
            LinearLayout.LayoutParams bp=Ui.lp(this,-1,-2);bp.bottomMargin=Ui.dp(this,9);
            chat.addView(bubble,bp);
        }
        questionField=Ui.input(this,"¿Qué se acordó?",false);
        questionField.setSingleLine(false);
        questionField.setMinLines(2);questionField.setMaxLines(4);
        questionField.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        chat.addView(questionField,new LinearLayout.LayoutParams(-1,-2));
        chat.addView(Ui.space(this,10));
        TextView send=Ui.button(this,"Enviar pregunta   →",Ui.GREEN,Ui.WHITE);
        send.setOnClickListener(v->sendQuestion());chat.addView(send);
        root.addView(chat);
        root.addView(Ui.space(this,22));
        TextView del=Ui.mini(this,"Eliminar esta grabación",0xFFF7E3E0,0xFFB3261E);
        del.setOnClickListener(v->delete());root.addView(del);
        root.addView(Ui.space(this,13));
        root.addView(Ui.text(this,"El audio permanece en este dispositivo. Al pulsar analizar, autorizas su envío a Groq.",12,Ui.FAINT,false));
    }
    private void play(File file,TextView btn) {
        if(!file.exists()){Toast.makeText(this,"Audio no encontrado",Toast.LENGTH_LONG).show();return;}
        try {
            if(player==null){
                player=new MediaPlayer();player.setDataSource(file.getAbsolutePath());
                player.prepare();player.setOnCompletionListener(mp->btn.setText("▶  Reproducir"));
            }
            if(player.isPlaying()){player.pause();btn.setText("▶  Reproducir");}
            else{player.start();btn.setText("Ⅱ  Pausar audio");}
        }catch(Exception e){Toast.makeText(this,"No se pudo reproducir",Toast.LENGTH_LONG).show();releasePlayer();}
    }
    private void confirmAnalysis(RecordStore.Recording r){
        if(busy)return;
        if(!SecureKey.configured(this)){missingKey();return;}
        new AlertDialog.Builder(this).setTitle("Analizar con Groq")
            .setMessage("Actarec enviará el audio a la API de Groq para transcribirlo y luego enviará el texto para resumirlo. ¿Quieres continuar?")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Enviar a Groq",(d,w)->analyze(r)).show();
    }
    private void analyze(RecordStore.Recording r){
        busy=true;status.setText("Preparando la transcripción…");
        new Thread(()->{
            try {
                GroqClient groq=new GroqClient(SecureKey.get(this));
                progress("Transcribiendo con Whisper de Groq…");
                String transcript=groq.transcribe(new File(r.path),this::progress);
                if(transcript.isEmpty())throw new IllegalStateException("Groq devolvió una transcripción vacía");
                progress("Generando resumen inteligente…");
                String summary=groq.summarize(transcript,this::progress);
                store.setAnalysis(id,transcript,summary);
                runOnUiThread(()->{busy=false;render();Toast.makeText(this,"Análisis guardado",Toast.LENGTH_SHORT).show();});
            }catch(Exception e){
                runOnUiThread(()->{busy=false;if(status!=null)status.setText("Error: "+e.getMessage());});
            }
        }).start();
    }
    private void progress(String s){runOnUiThread(()->{if(status!=null)status.setText(s);});}
    private void sendQuestion(){
        if(busy)return;
        String question=questionField.getText().toString().trim();
        if(question.isEmpty())return;
        RecordStore.Recording r=store.get(id);
        if(r==null||r.transcript.isEmpty()){
            Toast.makeText(this,"Transcribe primero esta grabación",Toast.LENGTH_LONG).show();return;
        }
        if(!SecureKey.configured(this)){missingKey();return;}
        busy=true;
        TextView sending=Ui.text(this,"Actarec está pensando…",13,Ui.MUTED,false);
        root.addView(sending);
        new Thread(()->{
            try{
                List<RecordStore.Message> history=store.messages(id);
                String answer=new GroqClient(SecureKey.get(this)).ask(question,r.transcript,r.summary,history);
                store.message(id,"user",question);
                store.message(id,"assistant",answer);
                runOnUiThread(()->{busy=false;render();scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));});
            }catch(Exception e){runOnUiThread(()->{busy=false;Toast.makeText(this,"Groq: "+e.getMessage(),Toast.LENGTH_LONG).show();render();});}
        }).start();
    }
    private void missingKey(){
        new AlertDialog.Builder(this).setTitle("Configura Groq")
            .setMessage("Para transcribir, resumir y preguntar necesitas tu propia API key de Groq.")
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Ir a ajustes",(d,w)->startActivity(new Intent(this,SettingsActivity.class))).show();
    }
    private void rename(RecordStore.Recording r){
        EditText input=Ui.input(this,"Nombre de la grabación",false);input.setText(r.title);
        new AlertDialog.Builder(this).setTitle("Renombrar grabación").setView(input)
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Guardar",(d,w)->{
                    String title=input.getText().toString().trim();
                    if(!title.isEmpty())store.rename(id,title);
                    render();
                }).show();
    }
    private void delete(){
        if(busy)return;
        new AlertDialog.Builder(this).setTitle("Eliminar grabación")
                .setMessage("Se borrarán el audio, su transcripción, su resumen y las preguntas guardadas en este dispositivo.")
                .setNegativeButton("Cancelar",null)
                .setPositiveButton("Eliminar",(d,w)->{releasePlayer();store.delete(id);finish();}).show();
    }
    private void copy(String text){
        ((ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Actarec",text));
        Toast.makeText(this,"Copiado",Toast.LENGTH_SHORT).show();
    }
    private void releasePlayer(){
        if(player!=null){try{player.release();}catch(Exception ignored){}player=null;}
    }
    @Override protected void onDestroy(){releasePlayer();super.onDestroy();}
}
