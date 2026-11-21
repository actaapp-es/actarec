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
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.view.Gravity;
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
    private ScrollView[] scroll=new ScrollView[1];
    private int seenTheme;
    private ImageView playIcon;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable playbackTick=new Runnable(){@Override public void run(){
        if(player!=null){
            try{
                if(seek!=null&&!seek.isPressed())seek.setProgress(player.getCurrentPosition());
                if(playTime!=null)playTime.setText(Ui.mmss(player.getCurrentPosition())+" / "+Ui.mmss(player.getDuration()));
                if(playIcon!=null)playIcon.setImageResource(player.isPlaying()?R.drawable.ic_pause:R.drawable.ic_play);
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
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Ui.wrap(base));}
    @Override public void onCreate(Bundle state){super.onCreate(state);seenTheme=Ui.themeGen;
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
    
    @Override protected void onResume(){super.onResume();Ui.recreateIfThemeChanged(this,seenTheme);if(store!=null)render();}
    private boolean processing(){return AnalysisService.working&&AnalysisService.activeId==id;}
    private LinearLayout.LayoutParams side(int top){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.MATCH,Ui.WRAP);
        p.setMargins(Ui.dp(this,16),Ui.dp(this,top),Ui.dp(this,16),0);return p;
    }
    private void render(){
        RecordStore.Recording r=store.get(id);if(r==null){finish();return;}
        LinearLayout root=Ui.page(this,Ui.vertical(this),scroll);
        LinearLayout bar=Ui.horizontal(this);bar.setPadding(Ui.dp(this,8),Ui.dp(this,2),Ui.dp(this,8),0);
        ImageView back=Ui.iconButton(this,R.drawable.ic_back,R.color.ink,"Volver");back.setOnClickListener(v->finish());
        bar.addView(back);bar.addView(new View(this),new LinearLayout.LayoutParams(0,1,1));
        ImageView fav=Ui.iconButton(this,r.favorite?R.drawable.ic_star_on:R.drawable.ic_star,R.color.accent,r.favorite?"Quitar de favoritos":"Añadir a favoritos");
        fav.setOnClickListener(v->{store.favorite(id,!r.favorite);render();});bar.addView(fav);
        ImageView more=Ui.iconButton(this,R.drawable.ic_more,R.color.ink,"Más opciones");more.setOnClickListener(v->more(r));bar.addView(more);
        root.addView(bar);
        TextView title=Ui.text(this,R.style.Text_Title,r.title);title.setTextSize(26);title.setMaxLines(3);
        title.setPadding(Ui.dp(this,24),Ui.dp(this,8),Ui.dp(this,24),0);title.setOnClickListener(v->rename(r));root.addView(title);
        TextView meta=Ui.text(this,R.style.Text_Meta,Ui.date(r.createdAt)+" · "+Ui.mmss(r.durationMs));
        meta.setPadding(Ui.dp(this,24),Ui.dp(this,6),Ui.dp(this,24),0);root.addView(meta);
        playerCard(root,r);
        bookmarksCard(root,r);
        tabHeader(root);
        if(processing()){
            LinearLayout progress=Ui.card(this);progress.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));
            status=Ui.text(this,R.style.Text_Body,AnalysisService.progress.isEmpty()?"Procesando audio…":AnalysisService.progress);
            status.setTextSize(14.5f);progress.addView(status);
            android.widget.ProgressBar pb=new android.widget.ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);pb.setIndeterminate(true);
            pb.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.accent)));
            progress.addView(pb,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,10));root.addView(progress,side(14));
        }else status=null;
        if(r.error!=null&&!r.error.isEmpty()&&!processing()){
            LinearLayout warning=Ui.card(this);warning.setBackgroundResource(R.drawable.bg_warn_card);warning.setPadding(Ui.dp(this,18),Ui.dp(this,16),Ui.dp(this,18),Ui.dp(this,16));
            TextView t1=Ui.text(this,R.style.Text_Body,"No se completó el análisis");t1.setTextSize(14.5f);t1.setTextColor(getColor(R.color.danger));warning.addView(t1);
            TextView t2=Ui.text(this,R.style.Text_Meta,r.error);t2.setTextColor(getColor(R.color.danger));warning.addView(t2,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,4));
            root.addView(warning,side(14));
        }
        if(tab==0)summaryTab(root,r);else if(tab==1)transcriptTab(root,r);else chatTab(root,r);
        TextView foot=Ui.text(this,R.style.Text_Meta,"Tus audios permanecen en el teléfono. Solo se envía a Groq lo que autorizas.");
        foot.setTextSize(12);foot.setPadding(Ui.dp(this,26),Ui.dp(this,20),Ui.dp(this,26),0);root.addView(foot);
    }
    private void playerCard(LinearLayout root,RecordStore.Recording r){
        LinearLayout panel=Ui.card(this);panel.setPadding(Ui.dp(this,16),Ui.dp(this,14),Ui.dp(this,20),Ui.dp(this,14));
        LinearLayout row=Ui.horizontal(this);
        FrameLayout play=new FrameLayout(this);play.setBackgroundResource(R.drawable.bg_circle_accent);play.setClickable(true);play.setFocusable(true);
        play.setContentDescription("Reproducir o pausar");
        playIcon=Ui.iconView(this,player!=null&&player.isPlaying()?R.drawable.ic_pause:R.drawable.ic_play,R.color.on_accent,24);
        play.addView(playIcon,new FrameLayout.LayoutParams(Ui.dp(this,24),Ui.dp(this,24),Gravity.CENTER));
        play.setOnClickListener(v->togglePlay(new File(r.path)));
        row.addView(play,Ui.lp(Ui.dp(this,48),Ui.dp(this,48)));
        LinearLayout right=Ui.vertical(this);
        seek=new SeekBar(this);seek.setMax((int)Math.min(Integer.MAX_VALUE,Math.max(1,r.durationMs)));
        seek.setProgressTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.accent)));
        seek.setThumbTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.accent)));
        seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(getColor(R.color.track)));
        if(player!=null){try{seek.setMax(player.getDuration());seek.setProgress(player.getCurrentPosition());}catch(Exception ignored){}}
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override public void onProgressChanged(SeekBar b,int p,boolean fromUser){}
            @Override public void onStartTrackingTouch(SeekBar b){}
            @Override public void onStopTrackingTouch(SeekBar b){try{ensurePlayer(new File(r.path));player.seekTo(b.getProgress());}catch(Exception e){toast("No se pudo avanzar en el audio");}}
        });
        right.addView(seek);
        playTime=Ui.text(this,R.style.Text_Meta,"0:00 / "+Ui.mmss(r.durationMs));playTime.setTextSize(12);
        playTime.setPadding(Ui.dp(this,16),0,0,0);
        if(player!=null){try{playTime.setText(Ui.mmss(player.getCurrentPosition())+" / "+Ui.mmss(player.getDuration()));}catch(Exception ignored){}}
        right.addView(playTime);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,Ui.WRAP,1);rp.setMarginStart(Ui.dp(this,10));row.addView(right,rp);
        panel.addView(row);root.addView(panel,side(18));
    }
    private long currentPosition(RecordStore.Recording r){
        try{if(player!=null)return player.getCurrentPosition();}catch(Exception ignored){}
        return seek!=null?seek.getProgress():0;
    }
    private void bookmarksCard(LinearLayout root,RecordStore.Recording r){
        List<RecordStore.Bookmark> list=store.bookmarks(id);
        LinearLayout head=Ui.horizontal(this);head.setPadding(Ui.dp(this,26),Ui.dp(this,22),Ui.dp(this,14),0);
        head.addView(Ui.text(this,R.style.Text_Section,list.isEmpty()?"Marcadores":"Marcadores · "+list.size()),new LinearLayout.LayoutParams(0,Ui.WRAP,1));
        TextView add=Ui.textButton(this,"Marcar aquí");
        add.setOnClickListener(v->{store.addBookmark(id,currentPosition(r),"");toast("Marcador añadido");render();});
        head.addView(add);root.addView(head);
        if(list.isEmpty()){
            TextView hint=Ui.text(this,R.style.Text_Meta,"Pulsa «Marcar aquí» mientras escuchas para señalar un momento importante.");
            hint.setPadding(Ui.dp(this,26),Ui.dp(this,4),Ui.dp(this,26),0);root.addView(hint);return;
        }
        LinearLayout group=Ui.vertical(this);group.setBackgroundResource(R.drawable.bg_card);group.setClipToOutline(true);
        for(int i=0;i<list.size();i++){
            final RecordStore.Bookmark b=list.get(i);if(i>0)group.addView(Ui.separator(this,56));
            LinearLayout row=Ui.horizontal(this);row.setPadding(Ui.dp(this,18),Ui.dp(this,12),Ui.dp(this,12),Ui.dp(this,12));
            row.setBackgroundResource(R.drawable.bg_card_click);row.setClickable(true);
            row.addView(Ui.iconView(this,R.drawable.ic_bookmark,R.color.accent,20));
            TextView label=Ui.text(this,R.style.Text_Body,b.label==null||b.label.isEmpty()?"Marcador "+(i+1):b.label);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.WRAP,1);lp.setMarginStart(Ui.dp(this,14));row.addView(label,lp);
            row.addView(Ui.text(this,R.style.Text_Meta,Ui.mmss(b.positionMs)));
            ImageView del=Ui.iconButton(this,R.drawable.ic_trash,R.color.ink_2,"Eliminar marcador");
            del.setOnClickListener(v->{store.deleteBookmark(b.id);render();});row.addView(del);
            row.setOnClickListener(v->jumpTo(r,b.positionMs));
            row.setOnLongClickListener(v->{renameBookmark(b);return true;});
            row.setContentDescription("Ir al marcador en "+Ui.mmss(b.positionMs));
            group.addView(row);
        }
        root.addView(group,side(8));
        TextView tip=Ui.text(this,R.style.Text_Meta,"Toca para ir al momento. Mantén pulsado para ponerle nombre.");
        tip.setTextSize(12);tip.setPadding(Ui.dp(this,26),Ui.dp(this,8),Ui.dp(this,26),0);root.addView(tip);
    }
    private void jumpTo(RecordStore.Recording r,long positionMs){
        try{ensurePlayer(new File(r.path));player.seekTo((int)positionMs);if(!player.isPlaying())player.start();
            if(seek!=null)seek.setProgress((int)positionMs);if(playIcon!=null)playIcon.setImageResource(R.drawable.ic_pause);
            scroll[0].post(()->scroll[0].smoothScrollTo(0,0));
        }catch(Exception e){releasePlayer();toast("No se pudo reproducir el archivo");}
    }
    private void renameBookmark(RecordStore.Bookmark b){
        final EditText input=new EditText(this);input.setSingleLine(true);input.setText(b.label);input.setHint("Nombre del marcador");
        new AlertDialog.Builder(this).setTitle("Marcador en "+Ui.mmss(b.positionMs)).setView(input)
            .setPositiveButton("Guardar",(dlg,w)->{store.renameBookmark(b.id,input.getText().toString());render();})
            .setNegativeButton("Cancelar",null).show();
    }
    private void ensurePlayer(File audio) throws Exception {
        if(!audio.isFile())throw new IllegalStateException("El audio ya no está disponible");
        if(player!=null)return;
        player=new MediaPlayer();player.setDataSource(audio.getAbsolutePath());player.prepare();
        player.setOnCompletionListener(v->{if(playIcon!=null)playIcon.setImageResource(R.drawable.ic_play);if(seek!=null)seek.setProgress(0);});
        if(seek!=null)seek.setMax(player.getDuration());
    }
    private void togglePlay(File audio){
        try{ensurePlayer(audio);
            if(player.isPlaying())player.pause();else player.start();
            if(playIcon!=null)playIcon.setImageResource(player.isPlaying()?R.drawable.ic_pause:R.drawable.ic_play);
        }catch(Exception e){releasePlayer();toast("No se pudo reproducir el archivo");}
    }
    private void tabHeader(LinearLayout root){
        LinearLayout tabs=Ui.horizontal(this);tabs.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        String[] titles={"Resumen","Texto","Preguntar"};
        for(int i=0;i<titles.length;i++){
            final int index=i;TextView item=Ui.chip(this,titles[i],tab==i);item.setMinHeight(Ui.dp(this,34));
            item.setContentDescription("Ver "+titles[i]);
            item.setOnClickListener(v->{tab=index;render();scroll[0].post(()->scroll[0].smoothScrollTo(0,0));});
            LinearLayout.LayoutParams p=Ui.lp(Ui.WRAP,Ui.WRAP);if(i>0)p.setMarginStart(Ui.dp(this,8));tabs.addView(item,p);
        }
        root.addView(tabs,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,18));
    }
    private TextView body(String s){TextView t=Ui.text(this,R.style.Text,s);t.setTextSize(15.5f);t.setLineSpacing(Ui.dp(this,3),1);t.setTextIsSelectable(true);return t;}
    private void cardBlock(LinearLayout card,String label,String text){
        if(text==null||text.isEmpty())return;
        TextView l=Ui.label(this,label);card.addView(l,Ui.lpTop(this,Ui.WRAP,Ui.WRAP,card.getChildCount()==0?0:18));
        card.addView(body(text),Ui.lpTop(this,Ui.MATCH,Ui.WRAP,6));
    }
    private LinearLayout padded(){LinearLayout c=Ui.card(this);c.setPadding(Ui.dp(this,20),Ui.dp(this,18),Ui.dp(this,20),Ui.dp(this,18));return c;}
    private void summaryTab(LinearLayout root,RecordStore.Recording r){
        if(r.summary.isEmpty()){
            LinearLayout card=padded();
            card.addView(Ui.text(this,R.style.Text_Body,"Resumen y tareas con IA"));
            TextView m=Ui.text(this,R.style.Text_Meta,"Groq transcribe el audio y deja por escrito el contexto, un resumen breve y las tareas con responsable y fecha. Usa tu propia clave.");
            m.setLineSpacing(Ui.dp(this,2),1);card.addView(m,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,6));
            TextView analyze=Ui.button(this,r.analyzed()?"Crear resumen":"Transcribir y resumir",true);
            analyze.setEnabled(!processing());analyze.setAlpha(processing()?.5f:1f);analyze.setOnClickListener(v->confirmAnalysis(r));
            card.addView(analyze,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,16));root.addView(card,side(14));return;
        }
        Analysis a=Analysis.parse(r.summary);
        if(a==null){ // resumen antiguo en texto
            LinearLayout card=padded();card.addView(body(r.summary));root.addView(card,side(14));
        }else{
            LinearLayout card=padded();
            cardBlock(card,"Contexto",a.context);cardBlock(card,"Resumen",a.summary);
            root.addView(card,side(14));
            if(!a.tasks.isEmpty()){
                root.addView(Ui.section(this,"Tareas · "+a.tasks.size()));
                LinearLayout group=Ui.vertical(this);group.setBackgroundResource(R.drawable.bg_card);group.setClipToOutline(true);
                for(int i=0;i<a.tasks.size();i++){
                    Analysis.Task t=a.tasks.get(i);if(i>0)group.addView(Ui.separator(this,56));
                    LinearLayout row=Ui.horizontal(this);row.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),Ui.dp(this,14));
                    ImageView chk=Ui.iconView(this,R.drawable.ic_check,R.color.accent,20);row.addView(chk);
                    LinearLayout tx=Ui.vertical(this);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,Ui.WRAP,1);tp.setMarginStart(Ui.dp(this,16));
                    tx.addView(Ui.text(this,R.style.Text_Body,t.title));
                    StringBuilder sub=new StringBuilder();
                    if(!t.owner.isEmpty())sub.append(t.owner);
                    if(t.date!=null){if(sub.length()>0)sub.append(" · ");sub.append(t.date);if(t.time!=null)sub.append(" ").append(t.time);}
                    if(!t.detail.isEmpty()){if(sub.length()>0)sub.append("\n");sub.append(t.detail);}
                    if(sub.length()>0){TextView s2=Ui.text(this,R.style.Text_Meta,sub.toString());s2.setLineSpacing(Ui.dp(this,2),1);tx.addView(s2,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,3));}
                    row.addView(tx,tp);group.addView(row);
                }
                root.addView(group,side(0));
            }
            if(!a.note.isEmpty()){
                LinearLayout note=Ui.card(this);note.setBackgroundResource(R.drawable.bg_accent_card);note.setPadding(Ui.dp(this,20),Ui.dp(this,16),Ui.dp(this,20),Ui.dp(this,16));
                TextView l=Ui.label(this,"Para recordar");l.setTextColor(getColor(R.color.accent));note.addView(l);
                TextView n=Ui.text(this,R.style.Text,a.note);n.setTextSize(15);n.setTextColor(getColor(R.color.accent));note.addView(n,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,4));
                root.addView(note,side(14));
            }
        }
        LinearLayout actions=Ui.horizontal(this);actions.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
        TextView copy=Ui.button(this,"Copiar",false);copy.setOnClickListener(v->copy(Analysis.toText(r.summary)));
        actions.addView(copy,new LinearLayout.LayoutParams(0,Ui.WRAP,1));
        TextView again=Ui.button(this,"Volver a resumir",false);again.setEnabled(!processing());again.setOnClickListener(v->confirmAnalysis(r));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.WRAP,1);p.setMarginStart(Ui.dp(this,10));actions.addView(again,p);
        root.addView(actions,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,14));
    }
    private void transcriptTab(LinearLayout root,RecordStore.Recording r){
        LinearLayout panel=padded();
        if(!r.analyzed()){
            panel.addView(Ui.text(this,R.style.Text_Body,"Aún no hay transcripción"));
            TextView m=Ui.text(this,R.style.Text_Meta,"Analiza el audio para convertir las voces en texto.");panel.addView(m,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,6));
            TextView analyze=Ui.button(this,"Transcribir con Groq",true);analyze.setEnabled(!processing());analyze.setOnClickListener(v->confirmAnalysis(r));
            panel.addView(analyze,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,16));root.addView(panel,side(14));
        }else{
            panel.addView(body(r.transcript));root.addView(panel,side(14));
            LinearLayout actions=Ui.horizontal(this);actions.setPadding(Ui.dp(this,16),0,Ui.dp(this,16),0);
            TextView copy=Ui.button(this,"Copiar",false);copy.setOnClickListener(v->copy(r.transcript));
            actions.addView(copy,new LinearLayout.LayoutParams(0,Ui.WRAP,1));
            TextView edit=Ui.button(this,"Editar texto",false);edit.setEnabled(!processing());edit.setOnClickListener(v->editTranscript(r));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.WRAP,1);p.setMarginStart(Ui.dp(this,10));actions.addView(edit,p);
            root.addView(actions,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,14));
        }
    }
    private EditText field(String hint){
        EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15.5f);e.setTextColor(getColor(R.color.ink));e.setHintTextColor(getColor(R.color.ink_2));
        e.setBackgroundResource(R.drawable.bg_field);e.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),Ui.dp(this,14));return e;
    }
    private void chatTab(LinearLayout root,RecordStore.Recording r){
        List<RecordStore.Message> messages=store.messages(id);
        for(RecordStore.Message message:messages){
            boolean mine="user".equals(message.role);
            LinearLayout bubble=padded();if(mine)bubble.setBackgroundResource(R.drawable.bg_accent_card);
            TextView who=Ui.label(this,mine?"Tú":"Actarec");if(mine)who.setTextColor(getColor(R.color.accent));bubble.addView(who);
            TextView content=body(message.body);bubble.addView(content,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,4));
            root.addView(bubble,side(12));
        }
        LinearLayout ask=padded();
        if(!r.analyzed()){
            ask.addView(Ui.text(this,R.style.Text_Meta,"Transcribe primero la grabación para poder hacer preguntas."));
            TextView go=Ui.button(this,"Ir a la transcripción",true);go.setOnClickListener(v->{tab=1;render();});
            ask.addView(go,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,14));
        }else{
            if(messages.isEmpty()){
                ask.addView(Ui.text(this,R.style.Text_Meta,"Pregunta lo que quieras sobre esta grabación. Solo responde con lo que se dijo."));
                LinearLayout chips=Ui.horizontal(this);
                String[] examples={"¿Qué se acordó?","¿Qué quedó pendiente?"};
                for(int i=0;i<examples.length;i++){final String hint=examples[i];TextView c=Ui.chip(this,hint,true);c.setMinHeight(Ui.dp(this,34));
                    c.setOnClickListener(v->{if(question!=null)question.setText(hint);});
                    LinearLayout.LayoutParams p=Ui.lp(Ui.WRAP,Ui.WRAP);if(i>0)p.setMarginStart(Ui.dp(this,8));chips.addView(c,p);}
                ask.addView(chips,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,12));
            }
            question=field("Escribe una pregunta");question.setMinLines(2);question.setMaxLines(4);
            question.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            ask.addView(question,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,14));
            TextView send=Ui.button(this,"Preguntar",true);send.setEnabled(!processing());send.setAlpha(processing()?.5f:1f);
            send.setOnClickListener(v->askQuestion());ask.addView(send,Ui.lpTop(this,Ui.MATCH,Ui.WRAP,12));
        }
        root.addView(ask,side(12));
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
        EditText input=field("Transcripción");input.setSingleLine(false);input.setMinLines(7);input.setMaxLines(12);
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
        EditText input=field("Nombre de la grabación");input.setText(r.title);input.selectAll();
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
                case 3:shareText("Resumen de Actarec",Analysis.toText(r.summary));break;
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
