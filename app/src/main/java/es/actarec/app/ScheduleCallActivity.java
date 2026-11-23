package es.actarec.app;

import android.app.Activity;
import android.os.Bundle;

/** Pantalla transparente que abre el diálogo de programar llamada (desde una notificación). */
public class ScheduleCallActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b);
        String number=getIntent().getStringExtra("number");
        if(number==null||number.isEmpty()){finish();return;}
        Calls.showScheduleDialog(this,number,null,this::finish);
    }
}
