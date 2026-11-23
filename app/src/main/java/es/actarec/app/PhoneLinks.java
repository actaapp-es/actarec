package es.actarec.app;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Layout;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Detecta números de teléfono en cualquier texto de la app: al mantener pulsado, ofrece programar una llamada. */
final class PhoneLinks {
    private PhoneLinks(){}
    private static final Pattern P=Pattern.compile("(?<![\\d+])(?:\\+|00)?\\d[\\d ().\\-]{7,17}\\d(?![\\d])");

    /** Devuelve el número (solo dígitos y +) que hay en la posición dada del texto, o null. */
    static String at(CharSequence text,int offset){
        if(text==null)return null;
        Matcher m=P.matcher(text);
        while(m.find()){
            if(offset<m.start()||offset>m.end())continue;
            String raw=m.group();String n=normalize(raw);
            if(n!=null)return n;
        }
        return null;
    }
    /** Acepta 9 dígitos que empiecen por 6-9 (España) o números con prefijo internacional de 10 a 15 dígitos. */
    static String normalize(String raw){
        boolean plus=raw.startsWith("+")||raw.startsWith("00");
        String d=raw.replaceAll("[^0-9]","");
        if(raw.startsWith("00"))d=d.substring(2);
        if(plus){if(d.length()<10||d.length()>15)return null;return "+"+d;}
        if(d.length()==9&&"6789".indexOf(d.charAt(0))>=0)return d;
        return null;
    }

    /** Activa la pulsación larga sobre números en este texto (también en cajas de texto). */
    static void attach(final TextView t){
        final float[] xy=new float[2];
        t.setOnTouchListener((v,e)->{
            if(e.getActionMasked()==MotionEvent.ACTION_DOWN){xy[0]=e.getX();xy[1]=e.getY();}
            return false;
        });
        t.setOnLongClickListener(v->{
            Layout l=t.getLayout();if(l==null)return false;
            float x=xy[0]-t.getTotalPaddingLeft()+t.getScrollX(),y=xy[1]-t.getTotalPaddingTop()+t.getScrollY();
            int line=l.getLineForVertical((int)y);
            int off=l.getOffsetForHorizontal(line,x);
            String n=at(t.getText(),off);
            if(n==null)n=at(t.getText(),off-1);
            if(n==null)return false;
            menu(t.getContext(),n);return true;
        });
    }

    static void menu(final Context c,final String number){
        final android.app.Activity a=activity(c);
        String[] items={"Programar una llamada","Llamar ahora","Copiar el número"};
        new AlertDialog.Builder(c).setTitle(Calls.pretty(number)).setItems(items,(d,w)->{
            if(w==0){if(a!=null)Calls.showScheduleDialog(a,number,null);}
            else if(w==1){try{c.startActivity(new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",number,null)));}catch(Exception e){Toast.makeText(c,"No se pudo abrir el marcador",Toast.LENGTH_SHORT).show();}}
            else{((ClipboardManager)c.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Teléfono",number));Toast.makeText(c,"Número copiado",Toast.LENGTH_SHORT).show();}
        }).show();
    }
    private static android.app.Activity activity(Context c){
        while(c instanceof android.content.ContextWrapper){if(c instanceof android.app.Activity)return (android.app.Activity)c;c=((android.content.ContextWrapper)c).getBaseContext();}
        return null;
    }
}
