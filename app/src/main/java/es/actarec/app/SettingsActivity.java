package es.actarec.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Preferencias: clave Groq propia, controles en segundo plano y privacidad. */
public final class SettingsActivity extends Activity {
    @Override public void onCreate(Bundle state){super.onCreate(state);render();}
    @Override protected void onResume(){super.onResume();
        if(!Settings.canDrawOverlays(this))getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("overlay_enabled",false).apply();
        if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
        render();
    }
    private void render(){
        LinearLayout root=Ui.vertical(this);setContentView(Ui.page(this,root));
        TextView back=Ui.mini(this,"‹  Volver",Ui.SOFT,Ui.GREEN);back.setOnClickListener(v->finish());
        root.addView(back,Ui.lp(this,-2,-2));root.addView(Ui.space(this,25));
        root.addView(Ui.tag(this,"CONFIGURA TU EXPERIENCIA",Ui.GREEN));root.addView(Ui.space(this,8));
        root.addView(Ui.text(this,"Ajustes de Actarec",30,Ui.INK,true));root.addView(Ui.space(this,9));
        root.addView(Ui.text(this,"Una aplicación personal. Tus audios son tuyos.",14,Ui.MUTED,false));
        root.addView(Ui.space(this,25));
        drawGroq(root);root.addView(Ui.space(this,12));
        drawOverlay(root);root.addView(Ui.space(this,12));
        drawLanguage(root);root.addView(Ui.space(this,12));
        drawPrivacy(root);root.addView(Ui.space(this,25));
        String version="1.0.0";
        try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
        root.addView(Ui.tag(this,"ACTAREC "+version+" · DISEÑADO PARA ANDROID",Ui.FAINT));
    }
    private void drawGroq(LinearLayout root){
        LinearLayout box=Ui.card(this);LinearLayout title=Ui.horizontal(this);
        title.addView(Ui.iconSquare(this,"spark",Ui.GREEN,Ui.SOFT,43),Ui.lp(this,43,43));
        LinearLayout text=Ui.vertical(this);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,12);
        text.addView(Ui.text(this,"Inteligencia con Groq",18,Ui.INK,true));text.addView(Ui.space(this,4));
        text.addView(Ui.text(this,"Transcribir · Resumir · Preguntar",12,Ui.MUTED,false));title.addView(text,p);box.addView(title);
        box.addView(Ui.space(this,15));
        boolean configured=SecureKey.configured(this);
        TextView status=Ui.mini(this,configured?"✓  Clave conectada":"○  Falta configurar",configured?Ui.SOFT:Ui.CREAM,configured?Ui.GREEN:Ui.MUTED);
        box.addView(status,Ui.lp(this,-2,-2));box.addView(Ui.space(this,12));
        box.addView(Ui.text(this,"Utiliza tu propia API key. Se cifra con Android Keystore y nunca se incluye en las APK públicas.",13,Ui.MUTED,false));
        box.addView(Ui.space(this,13));
        EditText field=Ui.input(this,configured?"Nueva clave (para reemplazarla)":"Introduce tu clave gsk_…",true);
        box.addView(field);box.addView(Ui.space(this,10));
        TextView save=Ui.button(this,configured?"Actualizar clave Groq":"Guardar clave Groq",Ui.GREEN,Ui.WHITE);
        save.setOnClickListener(v->{
            String key=field.getText().toString().trim();
            if(!key.startsWith("gsk_")||key.length()<15){toast("Introduce una clave de Groq válida (gsk_…)");return;}
            try{SecureKey.set(this,key);field.setText("");toast("Clave cifrada y guardada");render();}
            catch(Exception e){toast("No se pudo guardar en Android Keystore");}
        });box.addView(save);
        if(configured){
            box.addView(Ui.space(this,8));
            TextView verify=Ui.mini(this,"Comprobar conexión",Ui.SOFT,Ui.GREEN);
            verify.setOnClickListener(v->{verify.setEnabled(false);verify.setText("Comprobando…");
                new Thread(()->{
                    String msg;
                    try{new GroqClient(SecureKey.get(this)).checkConnection();msg="Conexión con Groq correcta";}
                    catch(Exception e){msg="No se pudo conectar: "+(e.getMessage()==null?"error de red":e.getMessage());}
                    final String result=msg;runOnUiThread(()->{verify.setEnabled(true);verify.setText("Comprobar conexión");toast(result);});
                }).start();
            });box.addView(verify);
            box.addView(Ui.space(this,8));
            TextView remove=Ui.mini(this,"Eliminar clave",0xFFFFE9E6,0xFFA73C33);
            remove.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar clave Groq")
                    .setMessage("Los audios y los análisis que ya están guardados seguirán en el teléfono.")
                    .setNegativeButton("Cancelar",null).setPositiveButton("Eliminar",(d,w)->{
                        try{SecureKey.set(this,"");render();}catch(Exception e){toast("No se pudo eliminar la clave");}
                    }).show());box.addView(remove);
        }
        root.addView(box);
    }
    private void drawOverlay(LinearLayout root){
        LinearLayout box=Ui.card(this);LinearLayout line=Ui.horizontal(this);
        line.addView(Ui.iconSquare(this,"audio",Ui.GREEN,Ui.SOFT,43),Ui.lp(this,43,43));
        LinearLayout title=Ui.vertical(this);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=Ui.dp(this,12);
        title.addView(Ui.text(this,"Cápsula flotante",18,Ui.INK,true));title.addView(Ui.space(this,4));
        title.addView(Ui.text(this,"Para pantallas con notch o cámara",12,Ui.MUTED,false));line.addView(title,p);box.addView(line);
        box.addView(Ui.space(this,14));
        box.addView(Ui.text(this,"Muestra el tiempo y permite pausar y guardar junto al recorte superior. No modifica la isla del sistema ni puede funcionar en todos los fabricantes.",13,Ui.MUTED,false));
        box.addView(Ui.space(this,11));
        boolean notch=getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("has_cutout",false);
        box.addView(Ui.text(this,notch?"✓  Recorte de pantalla detectado":"Sin recorte detectado: usa notificaciones",12.5f,notch?Ui.GREEN:Ui.MUTED,true));
        box.addView(Ui.space(this,9));
        Switch enabled=new Switch(this);enabled.setText("Mostrar cápsula al grabar");enabled.setTextColor(Ui.INK);enabled.setTextSize(13.5f);
        enabled.setPadding(0,Ui.dp(this,5),0,Ui.dp(this,5));
        enabled.setThumbTintList(new ColorStateList(new int[][]{new int[]{}},new int[]{Ui.GREEN}));
        enabled.setTrackTintList(new ColorStateList(new int[][]{new int[]{}},new int[]{Ui.SOFT}));
        enabled.setEnabled(notch);
        enabled.setChecked(getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("overlay_enabled",false));
        enabled.setOnCheckedChangeListener((v,on)->{
            if(on&&!Settings.canDrawOverlays(this)){
                new AlertDialog.Builder(this).setTitle("Permitir superposición")
                    .setMessage("Android debe autorizar que Actarec muestre controles sobre otras aplicaciones. Es opcional: la notificación seguirá disponible.")
                    .setNegativeButton("Cancelar",(d,w)->{enabled.setChecked(false);})
                    .setPositiveButton("Abrir permiso",(d,w)->{
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
                    }).show();
            }else{
                getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("overlay_enabled",on).apply();
                if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
            }
        });box.addView(enabled);box.addView(Ui.space(this,8));
        TextView notify=Ui.mini(this,"Ajustes de notificaciones de Android",Ui.SOFT,Ui.GREEN);
        notify.setOnClickListener(v->{
            Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
            try{startActivity(i);}catch(Exception e){toast("Abre las notificaciones de Actarec desde Ajustes de Android");}
        });box.addView(notify);
        root.addView(box);
    }
    private void drawLanguage(LinearLayout root){
        LinearLayout box=Ui.card(this);
        box.addView(Ui.text(this,"Idioma de transcripción",18,Ui.INK,true));box.addView(Ui.space(this,8));
        box.addView(Ui.text(this,"Automático detecta otros idiomas. Puedes fijar español o inglés para mejorar el reconocimiento en grabaciones específicas.",13,Ui.MUTED,false));
        box.addView(Ui.space(this,12));
        String selected=getSharedPreferences("prefs",MODE_PRIVATE).getString("language","");
        LinearLayout choices=Ui.horizontal(this);
        String[] values={"","es","en"},names={"Auto","Español","Inglés"};
        for(int i=0;i<values.length;i++){
            final String value=values[i];TextView chip=Ui.chip(this,names[i],selected.equals(value));
            chip.setOnClickListener(v->{getSharedPreferences("prefs",MODE_PRIVATE).edit().putString("language",value).apply();render();});
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(i>0)p.leftMargin=Ui.dp(this,7);
            choices.addView(chip,p);
        }
        box.addView(choices);root.addView(box);
    }
    private void drawPrivacy(LinearLayout root){
        LinearLayout box=Ui.card(this);
        LinearLayout heading=Ui.horizontal(this);
        heading.addView(Ui.iconSquare(this,"shield",Ui.GREEN,Ui.SOFT,43),Ui.lp(this,43,43));
        TextView title=Ui.text(this,"Privacidad y seguridad",18,Ui.INK,true);
        LinearLayout.LayoutParams p=Ui.lp(this,-2,-2);p.leftMargin=Ui.dp(this,12);heading.addView(title,p);box.addView(heading);
        box.addView(Ui.space(this,14));
        box.addView(Ui.text(this,"• Audio, transcripciones y respuestas almacenados localmente.\n\n• El audio se envía a Groq únicamente cuando autorizas un análisis. Las preguntas envían los extractos relevantes.\n\n• Tu clave API se cifra con el Keystore del dispositivo. Los audios usan el cifrado del sistema Android.\n\n• Avisa a las personas que vayas a grabar y cumple las normas de consentimiento aplicables.",13,Ui.MUTED,false));
        root.addView(box);
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
