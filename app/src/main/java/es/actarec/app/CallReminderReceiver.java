package es.actarec.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Dispara los avisos de llamadas programadas y los vuelve a armar tras reiniciar el móvil. */
public class CallReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent i){
        String a=i.getAction();if(a==null)return;
        if(Calls.ACTION_FIRE.equals(a))Calls.fire(c,i.getStringExtra("id"),i.getStringExtra("number"));
        else if(Calls.ACTION_SNOOZE.equals(a))Calls.snooze(c,i.getStringExtra("id"),i.getStringExtra("number"));
        else if(Intent.ACTION_BOOT_COMPLETED.equals(a)||Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)){
            Calls.rearmAll(c);
            if(Calls.notifyEnabled(c))MissedCallJob.schedule(c);
        }
    }
}
