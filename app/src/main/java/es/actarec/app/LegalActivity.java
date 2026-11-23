package es.actarec.app;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Aviso legal y política de privacidad de Actarec, con el mismo formato que Acta. */
public final class LegalActivity extends Activity {
    static final String UPDATED="21 de noviembre de 2026";
    static final String[][] CLAUSES={
        {"1. Objeto y aceptación",
         "El presente aviso legal regula el uso de la aplicación Actarec (en adelante, «la Aplicación»), puesta a disposición por su desarrollador o titular (en adelante, «el Desarrollador»). La instalación, el acceso o el uso de la Aplicación implican la aceptación plena y sin reservas de todas las cláusulas de este aviso. Si no está de acuerdo con ellas, no instale ni utilice la Aplicación y desinstálela de su dispositivo."},
        {"2. Licencia de uso y ausencia de garantías",
         "La Aplicación se facilita de forma gratuita, bajo licencia MIT, «tal cual» y «según disponibilidad», sin garantías de ningún tipo, expresas o implícitas, incluidas, a título enunciativo, las de comerciabilidad, idoneidad para un fin determinado, exactitud, disponibilidad, continuidad, ausencia de errores y no infracción de derechos de terceros. El Desarrollador no garantiza que la Aplicación funcione de forma ininterrumpida ni que sea compatible con todos los dispositivos o versiones del sistema operativo."},
        {"3. Responsabilidad exclusiva del usuario",
         "El usuario es el único y exclusivo responsable del uso que haga de la Aplicación y de las grabaciones, transcripciones, resúmenes, tareas, marcadores y cualesquiera otros contenidos que genere, almacene, comparta o difunda con ella, así como de las consecuencias de dicho uso."},
        {"4. Grabación de conversaciones y cumplimiento normativo",
         "La grabación, conservación, tratamiento, uso y difusión de conversaciones, reuniones o voces de terceras personas pueden estar sujetos a requisitos legales que varían según el país y las circunstancias, entre otros los derivados del derecho a la intimidad, al honor y a la propia imagen y de la normativa de protección de datos personales (en particular, el Reglamento (UE) 2016/679 y la Ley Orgánica 3/2018). Corresponde exclusivamente al usuario conocer y cumplir la legislación aplicable, informar a las demás personas y recabar su consentimiento cuando sea exigible, y abstenerse de difundir o utilizar las grabaciones de forma ilícita. El Desarrollador no ofrece asesoramiento jurídico y no será responsable de las grabaciones realizadas por el usuario ni del uso que este haga de ellas."},
        {"5. Usos prohibidos",
         "Queda prohibido utilizar la Aplicación para fines ilícitos o contrarios a la buena fe, entre ellos grabar conversaciones en las que el usuario no tenga derecho a participar o a escuchar, acosar, amenazar, suplantar la identidad de terceros o vulnerar derechos de terceros. El usuario asumirá en exclusiva cualquier responsabilidad derivada de dichos usos."},
        {"6. Fallos de grabación y pérdida de datos",
         "El funcionamiento de la grabación depende del dispositivo, del sistema operativo, del fabricante y de la configuración realizada por el usuario. El Desarrollador no garantiza que todas las grabaciones se completen ni que sean íntegras, audibles o recuperables, y no responde de la pérdida, corrupción o borrado de grabaciones o datos. Es responsabilidad del usuario conservar sus propias copias de seguridad."},
        {"7. Inteligencia artificial y servicios de terceros",
         "Las funciones de transcripción, resumen, tareas y preguntas se basan en servicios de inteligencia artificial de terceros (como Groq) contratados directamente por el usuario mediante su propia clave. Los resultados generados por la inteligencia artificial pueden ser inexactos, incompletos o erróneos y no deben utilizarse como única base para tomar decisiones; el usuario debe verificarlos. Al utilizar estas funciones, el audio y la transcripción que el usuario decida analizar se envían al proveedor elegido, cuyas condiciones y política de privacidad son ajenas al Desarrollador, que no responde de dichos servicios, de su disponibilidad, de sus costes ni del tratamiento que hagan de los datos."},
        {"8. Datos personales",
         "La Aplicación almacena la información en el propio dispositivo del usuario. El Desarrollador no recopila, recibe ni trata las grabaciones, transcripciones ni otros datos personales del usuario. Respecto de los datos de terceros que el usuario grabe o trate, el usuario actúa bajo su exclusiva responsabilidad."},
        {"9. Limitación de responsabilidad",
         "En la máxima medida permitida por la legislación aplicable, el Desarrollador no será responsable de ningún daño o perjuicio, directo o indirecto, incluidos, entre otros, la pérdida de datos, de beneficios, de oportunidades o de reputación, las sanciones administrativas, las reclamaciones de terceros o los daños al dispositivo, que se deriven del uso o de la imposibilidad de uso de la Aplicación, aun cuando se hubiera advertido de su posibilidad."},
        {"10. Derechos irrenunciables",
         "Nada de lo dispuesto en este aviso limita o excluye la responsabilidad que, conforme a la ley aplicable, no pueda ser limitada o excluida, ni los derechos que la normativa de protección de las personas consumidoras reconozca con carácter irrenunciable. Si alguna cláusula fuera declarada nula o inaplicable, se tendrá por no puesta y el resto conservará su plena validez."},
        {"11. Modificaciones",
         "El Desarrollador podrá modificar la Aplicación, suspenderla o modificar este aviso en cualquier momento y sin previo aviso. La versión vigente será la incluida en la Aplicación."},
        {"12. Legislación aplicable",
         "Este aviso se rige por la legislación española. Para la resolución de cualquier controversia, las partes se someten a los juzgados y tribunales que correspondan conforme a la normativa aplicable y, cuando el usuario tenga la condición de consumidor, a los de su domicilio."},
    };
    static final String[][] PRIVACY={
        {"Qué datos usa Actarec",
         "Las grabaciones de audio, sus transcripciones, resúmenes, tareas, marcadores y las preguntas y respuestas con la IA. Todo se guarda únicamente en tu teléfono."},
        {"Dónde se guardan",
         "En el almacenamiento privado de la aplicación, al que no acceden otras apps. La clave de Groq se guarda cifrada con el Android Keystore."},
        {"Cuándo salen del móvil",
         "Solo cuando pulsas transcribir, resumir o preguntar y has configurado tu clave de Groq: entonces se envía a Groq el audio o la transcripción de esa grabación. Actarec no envía tu número de teléfono, ni datos de identificación, ni analítica."},
        {"Llamadas perdidas y llamadas programadas",
         "Si lo activas, Actarec lee el registro de llamadas de tu móvil (permiso de Android que puedes retirar cuando quieras) para avisarte de las llamadas que no has devuelto. Esa información no se guarda ni sale del teléfono. Las llamadas que programas se guardan solo en el móvil, y Actarec no hace llamadas por sí misma: el aviso abre el marcador de Android."},
        {"Qué no hace Actarec",
         "No pide cuentas ni correo, no muestra anuncios, no usa analítica ni cookies y no tiene servidores propios donde subir tus audios."},
        {"Tus derechos",
         "Puedes borrar cualquier grabación desde la app, quitar tu clave de Groq en Ajustes o desinstalar la aplicación para eliminar todos los datos locales. Las condiciones de Groq se rigen por su propia política de privacidad."},
    };
    @Override protected void attachBaseContext(Context base){super.attachBaseContext(Ui.wrap(base));}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        boolean privacy="privacy".equals(getIntent().getStringExtra("doc"));
        LinearLayout c=Ui.page(this,Ui.vertical(this),null);
        LinearLayout bar=Ui.horizontal(this);bar.setPadding(Ui.dp(this,8),Ui.dp(this,2),0,0);
        ImageView back=Ui.iconButton(this,R.drawable.ic_back,R.color.ink,"Volver");back.setOnClickListener(v->finish());bar.addView(back);
        c.addView(bar);
        c.setPadding(Ui.dp(this,24),Ui.dp(this,8),Ui.dp(this,24),Ui.dp(this,32));
        TextView title=Ui.text(this,R.style.Text_Title,privacy?"Política de privacidad":"Aviso legal");title.setTextSize(28);c.addView(title);
        TextView sub=Ui.text(this,R.style.Text_Meta,(privacy?"":"Exención de responsabilidad · ")+"Última actualización: "+UPDATED);
        sub.setPadding(0,Ui.dp(this,6),0,Ui.dp(this,8));c.addView(sub);
        for(String[] cl:privacy?PRIVACY:CLAUSES){
            TextView h=Ui.text(this,R.style.Text,cl[0],15.5f,true);h.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,6));c.addView(h);
            TextView p=Ui.text(this,R.style.Text,cl[1]);p.setTextSize(14.5f);p.setTextColor(getColor(R.color.ink_2));
            p.setLineSpacing(Ui.dp(this,4),1f);p.setTextIsSelectable(true);c.addView(p);
        }
    }
}
