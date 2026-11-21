package es.actarec.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

/** Ajustes con la misma estética que Acta: grupos de filas, valores a la derecha e interruptores. */
public final class SettingsActivity extends Activity {
    private int seenTheme;
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Ui.wrap(base));}
    @Override public void onCreate(Bundle state){super.onCreate(state);seenTheme=Ui.themeGen;render();}
    @Override protected void onResume(){super.onResume();
        Ui.recreateIfThemeChanged(this,seenTheme);
        if(RecordingService.running)startService(new Intent(this,RecordingService.class).setAction(RecordingService.ACTION_REFRESH));
        render();
    }
    private LinearLayout.LayoutParams side(){
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(Ui.MATCH,Ui.WRAP);p.setMargins(Ui.dp(this,16),0,Ui.dp(this,16),0);return p;
    }
    private void render(){
        LinearLayout root=Ui.page(this,Ui.vertical(this),null);
        LinearLayout bar=Ui.horizontal(this);bar.setPadding(Ui.dp(this,8),Ui.dp(this,2),0,0);
        ImageView back=Ui.iconButton(this,R.drawable.ic_back,R.color.ink,"Volver");back.setOnClickListener(v->finish());bar.addView(back);
        root.addView(bar);
        TextView title=Ui.text(this,R.style.Text_Title,"Ajustes");title.setPadding(Ui.dp(this,24),Ui.dp(this,8),Ui.dp(this,24),0);root.addView(title);

        root.addView(Ui.section(this,"Inteligencia artificial"));
        LinearLayout ai=group();
        boolean has=SecureKey.configured(this);
        addRow(ai,"Clave de Groq","Se guarda cifrada en el móvil. Tú pones tu propia clave y pagas solo lo que uses (hay nivel gratuito).",has?"Guardada":"Falta",v->keyDialog());
        ai.addView(Ui.separator(this,18));
        String lang=Ui.prefs(this).getString("language","");
        addRow(ai,"Idioma de transcripción","Mejora el reconocimiento de nombres y cifras.","en".equals(lang)?"Inglés":"Español",v->languageDialog());
        root.addView(ai,side());

        root.addView(Ui.section(this,"Grabación"));
        LinearLayout rec=group();
        addRow(rec,"Notificaciones","Controles de pausa y guardar mientras usas otras apps.","",v->{
            Intent i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName());
            try{startActivity(i);}catch(Exception e){toast("Abre las notificaciones de Actarec desde los ajustes de Android");}
        });
        root.addView(rec,side());

        root.addView(Ui.section(this,"Apariencia"));
        LinearLayout look=group();
        addRow(look,"Tema","",Ui.THEME_SHORT[Ui.theme(this)],v->themeDialog());
        root.addView(look,side());

        root.addView(Ui.section(this,"Información legal"));
        LinearLayout legal=group();
        addRow(legal,"Política de privacidad","Qué datos usa Actarec y cuándo salen del móvil.","",v->startActivity(new Intent(this,LegalActivity.class).putExtra("doc","privacy")));
        legal.addView(Ui.separator(this,18));
        addRow(legal,"Aviso legal y exención de responsabilidad","Condiciones de uso de Actarec.","",v->startActivity(new Intent(this,LegalActivity.class)));
        legal.addView(Ui.separator(this,18));
        addRow(legal,"Código abierto","Actarec es software libre (MIT). Código fuente y descargas en GitHub.","",v->open("https://github.com/actaapp-es/actarec"));
        root.addView(legal,side());

        root.addView(Ui.section(this,"Síguenos"));
        LinearLayout social=group();
        addRow(social,"Instagram","@actaapp.es","",v->open("https://www.instagram.com/actaapp.es"));
        social.addView(Ui.separator(this,18));
        addRow(social,"TikTok","@actaapp","",v->open("https://www.tiktok.com/@actaapp"));
        root.addView(social,side());

        root.addView(Ui.section(this,"Apoya a Actarec"));
        LinearLayout donate=group();
        addRow(donate,"Donar con Bitcoin","Actarec es gratis y sin anuncios. Si te resulta útil, puedes colaborar. Es voluntario.","",v->bitcoinDialog());
        root.addView(donate,side());

        TextView help=Ui.button(this,"Ayuda por Telegram",true);
        Ui.icon(help,R.drawable.ic_send,R.color.on_accent,20);
        help.setOnClickListener(v->open("https://t.me/actaapp"));
        root.addView(help,Ui.lpMargins(this,16,28,16,0));
        String version="";
        try{version=getPackageManager().getPackageInfo(getPackageName(),0).versionName;}catch(Exception ignored){}
        TextView foot=Ui.text(this,R.style.Text_Meta,"Actarec "+version+"\nLas grabaciones se guardan únicamente en este teléfono.");
        foot.setLineSpacing(0,1.3f);foot.setTextSize(12);foot.setGravity(android.view.Gravity.CENTER);
        foot.setPadding(Ui.dp(this,24),Ui.dp(this,26),Ui.dp(this,24),0);root.addView(foot);
    }
    private static final String BTC="bc1qqpyyxg64ac9j4zksmslna9d8sxp9ht7r25k7t9";
    private void open(String url){
        try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
        catch(Exception e){toast("No hay ninguna aplicación para abrir este enlace");}
    }
    private void bitcoinDialog(){
        TextView t=new TextView(this);t.setText(BTC);t.setTextIsSelectable(true);t.setTextSize(14);t.setTextColor(getColor(R.color.ink));
        t.setTypeface(android.graphics.Typeface.MONOSPACE);t.setPadding(Ui.dp(this,24),Ui.dp(this,12),Ui.dp(this,24),Ui.dp(this,4));
        new AlertDialog.Builder(this).setTitle("Donar con Bitcoin").setView(t)
            .setMessage("Gracias por apoyar a Actarec. Las donaciones son voluntarias y no desbloquean nada: la app es igual para todos. Comprueba bien la dirección antes de enviar; las transacciones de Bitcoin no se pueden deshacer.")
            .setNegativeButton("Cerrar",null)
            .setPositiveButton("Copiar dirección",(d,w)->{
                android.content.ClipboardManager cm=getSystemService(android.content.ClipboardManager.class);
                cm.setPrimaryClip(android.content.ClipData.newPlainText("Dirección Bitcoin",BTC));toast("Dirección copiada");
            }).show();
    }
    private LinearLayout group(){LinearLayout g=Ui.vertical(this);g.setBackgroundResource(R.drawable.bg_card);g.setClipToOutline(true);return g;}
    private View addRow(LinearLayout group,String title,String sub,String value,View.OnClickListener click){
        View row=LayoutInflater.from(this).inflate(R.layout.row_setting,group,false);
        ((TextView)row.findViewById(R.id.title)).setText(title);
        TextView s=row.findViewById(R.id.sub);if(sub.isEmpty())s.setVisibility(View.GONE);else s.setText(sub);
        TextView v=row.findViewById(R.id.value);if(value.isEmpty())v.setVisibility(View.GONE);else v.setText(value);
        if(click!=null)row.setOnClickListener(click);else{row.setClickable(false);row.setBackground(null);}
        group.addView(row);return row;
    }
    private void keyDialog(){
        boolean has=SecureKey.configured(this);
        EditText field=new EditText(this);field.setHint(has?"Nueva clave (para reemplazarla)":"gsk_…");field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);field.setTextColor(getColor(R.color.ink));
        field.setHintTextColor(getColor(R.color.ink_2));field.setBackgroundResource(R.drawable.bg_field);
        field.setPadding(Ui.dp(this,18),Ui.dp(this,14),Ui.dp(this,18),Ui.dp(this,14));
        LinearLayout box=Ui.vertical(this);box.setPadding(Ui.dp(this,22),Ui.dp(this,6),Ui.dp(this,22),0);box.addView(field);
        AlertDialog.Builder b=new AlertDialog.Builder(this).setTitle("Clave de Groq")
            .setMessage("Crea una gratis en console.groq.com/keys. Nunca se publica ni sale del móvil salvo hacia Groq.").setView(box)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Guardar",(d,w)->{
                String key=field.getText().toString().trim();
                if(!key.startsWith("gsk_")||key.length()<15){toast("Introduce una clave de Groq válida (gsk_…)");return;}
                try{SecureKey.set(this,key);toast("Clave guardada y cifrada");render();}catch(Exception e){toast("No se pudo guardar en Android Keystore");}
            });
        if(has)b.setNeutralButton("Quitar",(d,w)->{try{SecureKey.set(this,"");render();}catch(Exception e){toast("No se pudo quitar la clave");}});
        b.show();
        if(has){
            // Comprobación rápida de la clave ya guardada
            new Thread(()->{
                String msg;
                try{new GroqClient(this,SecureKey.get(this)).checkConnection();msg="Conexión con Groq correcta";}
                catch(Exception e){msg=e.getMessage()==null?"No se pudo conectar":e.getMessage();}
                final String result=msg;runOnUiThread(()->toast(result));
            }).start();
        }
    }
    private void languageDialog(){
        String[] names={"Español","Inglés"},values={"es","en"};
        int cur="en".equals(Ui.prefs(this).getString("language",""))?1:0;
        new AlertDialog.Builder(this).setTitle("Idioma de transcripción").setSingleChoiceItems(names,cur,(d,which)->{
            Ui.prefs(this).edit().putString("language",values[which]).apply();d.dismiss();render();
        }).show();
    }
    private void themeDialog(){
        new AlertDialog.Builder(this).setTitle("Tema").setSingleChoiceItems(Ui.THEME_LABELS,Ui.theme(this),(d,which)->{
            Ui.setTheme(this,which);d.dismiss();recreate();
        }).show();
    }
    private void toast(String message){Toast.makeText(this,message,Toast.LENGTH_LONG).show();}
}
