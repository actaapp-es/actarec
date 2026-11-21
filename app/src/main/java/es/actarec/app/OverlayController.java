package es.actarec.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Cápsula experimental bajo el recorte. Una app normal NO puede ocupar la barra de estado. */
final class OverlayController {
    private final Service service;
    private WindowManager window;
    private LinearLayout pill;
    private TextView clock, pause;
    private boolean attached;

    OverlayController(Service service) {this.service=service;}

    boolean shouldShow() {
        return Settings.canDrawOverlays(service)
                && service.getSharedPreferences("prefs",Context.MODE_PRIVATE).getBoolean("overlay_enabled",false)
                && service.getSharedPreferences("prefs",Context.MODE_PRIVATE).getBoolean("has_cutout",false);
    }

    void showIfAvailable() {
        if(!shouldShow()) {remove();return;}
        if(attached) return;
        try {
            window=(WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
            pill=Ui.horizontal(service);
            pill.setBackground(Ui.shape(service,Ui.INK,27));
            pill.setPadding(Ui.dp(service,15),Ui.dp(service,7),Ui.dp(service,10),Ui.dp(service,7));
            pill.setElevation(Ui.dp(service,9));

            TextView status=Ui.text(service,"●",13,Ui.RED,true);
            pill.addView(status,new LinearLayout.LayoutParams(Ui.dp(service,18),-2));

            clock=Ui.text(service,"REC 00:00",13,Ui.WHITE,true);
            clock.setContentDescription("Abrir Actarec");
            clock.setOnClickListener(v -> {
                Intent i=new Intent(service,MainActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                service.startActivity(i);
            });
            pill.addView(clock);

            pause=Ui.mini(service,"Ⅱ",0xFF343936,Ui.WHITE);
            LinearLayout.LayoutParams pauseParams=Ui.lp(service,44,38);pauseParams.leftMargin=Ui.dp(service,9);
            pill.addView(pause,pauseParams);
            pause.setContentDescription("Pausar o reanudar grabación");
            pause.setOnClickListener(v -> {
                Intent i=new Intent(service,RecordingService.class).setAction(RecordingService.ACTION_TOGGLE);
                service.startService(i);
            });

            TextView stop=Ui.mini(service,"■",0xFF503734,0xFFFFC7BE);
            LinearLayout.LayoutParams stopParams=Ui.lp(service,42,38);stopParams.leftMargin=Ui.dp(service,5);
            pill.addView(stop,stopParams);
            stop.setContentDescription("Detener y guardar grabación");
            stop.setOnClickListener(v -> service.startService(new Intent(service,RecordingService.class).setAction(RecordingService.ACTION_STOP)));

            int bar=0;
            int id=service.getResources().getIdentifier("status_bar_height","dimen","android");
            if(id>0)bar=service.getResources().getDimensionPixelSize(id);
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
            params.y=bar+Ui.dp(service,8);
            params.setTitle("Actarec • grabación");
            window.addView(pill,params);
            attached=true;
        }catch(Exception e){remove();}
    }
    void update(boolean paused,long durationMs) {
        if(!attached)return;
        clock.setText((paused ? "PAUSA " : "REC ")+Ui.mmss(durationMs));
        pause.setText(paused?"▶":"Ⅱ");
    }
    void remove() {
        if(attached && window!=null && pill!=null) {
            try { window.removeViewImmediate(pill); }catch(Exception ignored) { }
        }
        attached=false;pill=null;clock=null;pause=null;
    }
}
