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
            capsule=new LinearLayout(service);capsule.setOrientation(LinearLayout.HORIZONTAL);capsule.setGravity(Gravity.CENTER_VERTICAL);
            capsule.setBackground(pill(0xFF151614,28));
            capsule.setPadding(Ui.dp(service,14),Ui.dp(service,7),Ui.dp(service,7),Ui.dp(service,7));
            capsule.setElevation(Ui.dp(service,12));
            View live=new View(service);live.setBackground(pill(0xFFE0574A,5));
            LinearLayout.LayoutParams dp=new LinearLayout.LayoutParams(Ui.dp(service,10),Ui.dp(service,10));dp.setMarginEnd(Ui.dp(service,8));capsule.addView(live,dp);
            clock=label("Rec 0:00",0xFFFFFFFF);clock.setMinWidth(Ui.dp(service,70));clock.setContentDescription("Abrir Actarec");
            clock.setOnClickListener(v->{Intent screen=new Intent(service,MainActivity.class);
                screen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);service.startActivity(screen);});
            capsule.addView(clock);
            pause=button("Pausa",0xFF37403C,0xFFFFFFFF);
            LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(Ui.dp(service,64),Ui.dp(service,38));pp.setMarginStart(Ui.dp(service,6));
            capsule.addView(pause,pp);pause.setOnClickListener(v->control(RecordingService.ACTION_TOGGLE));
            TextView stop=button("Guardar",0xFF1E4D45,0xFFFFFFFF);
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(Ui.dp(service,72),Ui.dp(service,38));sp.setMarginStart(Ui.dp(service,5));
            capsule.addView(stop,sp);stop.setOnClickListener(v->control(RecordingService.ACTION_STOP));
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
    private static android.graphics.drawable.GradientDrawable pill(int color,float dp){
        android.graphics.drawable.GradientDrawable g=new android.graphics.drawable.GradientDrawable();g.setColor(color);g.setCornerRadius(dp*3);return g;
    }
    private TextView label(String s,int color){TextView t=new TextView(service);t.setText(s);t.setTextColor(color);t.setTextSize(13);
        t.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.BOLD));t.setClickable(true);return t;}
    private TextView button(String s,int bg,int fg){TextView t=label(s,fg);t.setGravity(Gravity.CENTER);t.setTextSize(12.5f);
        t.setBackground(pill(bg,13));return t;}
    void update(boolean paused,long duration){
        if(!allowed()){remove();return;}
        if(!added)return;
        clock.setText((paused?"Pausa ":"Rec ")+Ui.mmss(duration));
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
