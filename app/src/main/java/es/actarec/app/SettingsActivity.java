package es.actarec.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Preferencias privadas: BYOK + cápsula de grabación en segundo plano. */
public final class SettingsActivity extends Activity {
    private EditText field;
    @Override public void onCreate(Bundle savedInstanceState){super.onCreate(savedInstanceState);render();}
    @Override protected void onResume(){
        super.onResume();
        if(!Settings.canDrawOverlays(this))
            getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("overlay_enabled",false).apply();
        if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
        render();
    }
    private void render(){
        LinearLayout root=Ui.vertical(this);
        setContentView(Ui.page(this,root));
        TextView back=Ui.text(this,"‹  Volver",14,Ui.GREEN,true);
        back.setOnClickListener(v->finish());root.addView(back);
        root.addView(Ui.space(this,24));
        root.addView(Ui.tag(this,"PERSONALIZA TU GRABADORA",Ui.GREEN));
        root.addView(Ui.space(this,9));
        root.addView(Ui.text(this,"Ajustes",30,Ui.INK,true));
        root.addView(Ui.space(this,20));

        LinearLayout groq=Ui.card(this);
        groq.addView(Ui.text(this,"✦  Groq AI",20,Ui.INK,true));
        groq.addView(Ui.space(this,9));
        groq.addView(Ui.text(this,"Introduce tu propia clave de API. Actarec la cifra con Android Keystore y se conecta directamente a Groq, sin servidor intermediario.",14,Ui.MUTED,false));
        groq.addView(Ui.space(this,13));
        boolean configured=SecureKey.configured(this);
        groq.addView(Ui.text(this,configured?"✓ Clave configurada":"Clave no configurada",12,configured?Ui.GREEN:Ui.MUTED,true));
        groq.addView(Ui.space(this,10));
        field=Ui.input(this,"gsk_… (introduce tu clave)",true);
        groq.addView(field);
        groq.addView(Ui.space(this,10));
        TextView save=Ui.button(this,"Guardar clave Groq",Ui.GREEN,Ui.WHITE);
        save.setOnClickListener(v->{
            String key=field.getText().toString().trim();
            if(key.isEmpty()){Toast.makeText(this,"Introduce una clave",Toast.LENGTH_SHORT).show();return;}
            try{SecureKey.set(this,key);field.setText("");Toast.makeText(this,"Clave guardada cifrada",Toast.LENGTH_SHORT).show();render();}
            catch(Exception e){Toast.makeText(this,"No se pudo guardar la clave",Toast.LENGTH_LONG).show();}
        });groq.addView(save);
        if(configured){
            groq.addView(Ui.space(this,9));
            TextView erase=Ui.mini(this,"Eliminar clave guardada",0xFFF7E3E0,0xFFB3261E);
            erase.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Eliminar clave")
                    .setMessage("Las grabaciones locales no se borrarán.")
                    .setNegativeButton("Cancelar",null)
                    .setPositiveButton("Eliminar",(d,w)->{
                        try{SecureKey.set(this,"");render();}catch(Exception ignored){}
                    }).show());
            groq.addView(erase);
        }
        root.addView(groq);
        root.addView(Ui.space(this,14));

        LinearLayout capsule=Ui.card(this);
        capsule.addView(Ui.text(this,"◉  Isla flotante",20,Ui.INK,true));
        capsule.addView(Ui.space(this,8));
        capsule.addView(Ui.text(this,"Si Android detecta una cámara/notch en pantalla, puedes tener una cápsula con contador y botones de pausa y guardar justo debajo de la barra de estado.",14,Ui.MUTED,false));
        capsule.addView(Ui.space(this,12));
        boolean cutout=getSharedPreferences("prefs",Context.MODE_PRIVATE).getBoolean("has_cutout",false);
        boolean allowed=Settings.canDrawOverlays(this);
        capsule.addView(Ui.text(this,cutout?"✓ Notch o recorte detectado":"Sin notch detectado: usa las notificaciones",12,Ui.GREEN,true));
        capsule.addView(Ui.space(this,12));
        Switch enabled=new Switch(this);
        enabled.setText("Mostrar cápsula flotante");
        enabled.setTextSize(14);enabled.setTextColor(Ui.INK);
        enabled.setChecked(getSharedPreferences("prefs",MODE_PRIVATE).getBoolean("overlay_enabled",false));
        enabled.setEnabled(cutout);
        enabled.setOnCheckedChangeListener((button,checked)->{
            getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("overlay_enabled",checked).apply();
            if(checked&&!Settings.canDrawOverlays(this)){
                new AlertDialog.Builder(this).setTitle("Permiso para superponer")
                        .setMessage("Android debe permitir mostrar Actarec encima de otras aplicaciones. La cápsula es opcional; la notificación siempre funciona.")
                        .setNegativeButton("Ahora no",(d,w)->{
                            getSharedPreferences("prefs",MODE_PRIVATE).edit().putBoolean("overlay_enabled",false).apply();render();
                        })
                        .setPositiveButton("Abrir ajustes",(d,w)->{
                            Intent intent=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()));
                            startActivity(intent);
                        }).show();
            }else if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
        });
        capsule.addView(enabled);
        capsule.addView(Ui.space(this,10));
        capsule.addView(Ui.text(this,"La cápsula NO invade físicamente el notch: Android reserva esa zona al sistema. Fabricantes como Xiaomi y Samsung pueden aplicar más restricciones.",12,Ui.FAINT,false));
        root.addView(capsule);
        root.addView(Ui.space(this,14));

        LinearLayout privacy=Ui.card(this);
        privacy.addView(Ui.text(this,"🔒  Privacidad",20,Ui.INK,true));
        privacy.addView(Ui.space(this,9));
        privacy.addView(Ui.text(this,"• Grabaciones, resúmenes y preguntas se almacenan localmente en el espacio privado de Android.\n\n• Nunca enviamos el audio a Groq sin que pulses «Enviar a Groq».\n\n• La clave se guarda con Android Keystore. Los audios no están cifrados por la propia app: dependen del cifrado del dispositivo.\n\n• Avisa y consigue el consentimiento adecuado antes de grabar a otras personas.",13,Ui.MUTED,false));
        root.addView(privacy);
        root.addView(Ui.space(this,17));
        root.addView(Ui.text(this,"Actarec 0.3 · diseño visual inspirado en Acta (actaapp-es/acta)",11,Ui.FAINT,false));
    }
}
