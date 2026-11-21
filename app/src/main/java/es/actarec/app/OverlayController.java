package es.actarec.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Cápsula interactiva bajo la barra de estado; nunca intenta ocupar el notch del sistema. */
final class OverlayController {
    private final Service service;
    private WindowManager window;
    private LinearLayout capsule;
    private TextView clock,pause;
    private boolean added;
    OverlayController(Service service){this.service=service;}
    private boolean allowed(){
        return Settings.canDrawOverlays(service)
                &&service.getSharedPreferences("prefs",Context.MODE_PRIVATE).getBoolean("overlay_enabled",false)
                &&service.getSharedPreferences("prefs",Context.MODE_PRIVATE).getBoolean("has_cutout",false);
    }
    void showIfAvailable(){
        if(!allowed()){remove();return;}
        if(added)return;
        try{
            window=service.getSystemService(WindowManager.class);
            capsule=Ui.horizontal(service);
            capsule.setBackground(Ui.shape(service,Ui.INK,28));
            capsule.setPadding(Ui.dp(service,14),Ui.dp(service,7),Ui.dp(service,7),Ui.dp(service,7));
            capsule.setElevation(Ui.dp(service,12));
            TextView live=Ui.text(service,"●",13,Ui.RED,true);
            capsule.addView(live,Ui.lp(service,16,-2));
            clock=Ui.text(service,"REC 00:00",12.5f,Ui.WHITE,true);
            clock.setContentDescription("Abrir Actarec");clock.setMinWidth(Ui.dp(service,84));
            Ui.clickable(clock,"Abrir grabadora",v->{
                Intent screen=new Intent(service,MainActivity.class);
                screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                service.startActivity(screen);
            });capsule.addView(clock);
            pause=Ui.mini(service,"Pausa",0xFF37403C,Ui.WHITE);
            LinearLayout.LayoutParams pp=Ui.lp(service,44,39);pp.leftMargin=Ui.dp(service,6);
            capsule.addView(pause,pp);
            Ui.clickable(pause,"Pausar o reanudar",v->control(RecordingService.ACTION_TOGGLE));
            TextView stop=Ui.mini(service,"Guardar",0xFF603D3B,0xFFFFCFC9);
            LinearLayout.LayoutParams sp=Ui.lp(service,44,39);sp.leftMargin=Ui.dp(service,5);
            capsule.addView(stop,sp);Ui.clickable(stop,"Detener y guardar",v->control(RecordingService.ACTION_STOP));
            int statusBar=0;
            int statusId=service.getResources().getIdentifier("status_bar_height","dimen","android");
            if(statusId!=0)statusBar=service.getResources().getDimensionPixelSize(statusId);
            WindowManager.LayoutParams position=new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                    PixelFormat.TRANSLUCENT);
            position.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;
            position.y=statusBar+Ui.dp(service,6);
            position.setTitle("Actarec · grabación");
            window.addView(capsule,position);added=true;
        }catch(Exception e){remove();}
    }
    void update(boolean paused,long duration){
        if(!allowed()){remove();return;}
        if(!added)return;
        clock.setText((paused?"PAUSA ":"REC ")+Ui.mmss(duration));
        pause.setText(paused?"Seguir":"Pausa");
    }
    private void control(String action){
        service.startService(new Intent(service,RecordingService.class).setAction(action));
    }
    void remove(){
        if(added&&window!=null&&capsule!=null){try{window.removeViewImmediate(capsule);}catch(Exception ignored){}}
        added=false;capsule=null;clock=null;pause=null;
    }
}
