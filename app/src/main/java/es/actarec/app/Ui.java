package es.actarec.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Diseño de Actarec: misma estética que Acta (colores, tarjetas, botones, tema claro/oscuro). */
final class Ui {
    private Ui() {}
    static final int THEME_AUTO=0,THEME_LIGHT=1,THEME_DARK=2;
    static final String[] THEME_LABELS={"Automático (como el móvil)","Claro","Oscuro"};
    static final String[] THEME_SHORT={"Automático","Claro","Oscuro"};
    static final int MATCH=ViewGroup.LayoutParams.MATCH_PARENT,WRAP=ViewGroup.LayoutParams.WRAP_CONTENT;
    static volatile int themeGen;

    static SharedPreferences prefs(Context c){return c.getApplicationContext().getSharedPreferences("prefs",Context.MODE_PRIVATE);}
    static int theme(Context c){return prefs(c).getInt("theme",THEME_AUTO);}
    static void setTheme(Context c,int mode){prefs(c).edit().putInt("theme",mode).apply();themeGen++;}
    /** Aplica el tema elegido en Ajustes al contexto de una pantalla. */
    static Context wrap(Context base){
        int mode;try{mode=theme(base);}catch(Throwable t){return base;}
        if(mode==THEME_AUTO)return base;
        Configuration cfg=new Configuration(base.getResources().getConfiguration());
        int night=mode==THEME_DARK?Configuration.UI_MODE_NIGHT_YES:Configuration.UI_MODE_NIGHT_NO;
        cfg.uiMode=(cfg.uiMode&~Configuration.UI_MODE_NIGHT_MASK)|night;
        return base.createConfigurationContext(cfg);
    }
    static void recreateIfThemeChanged(Activity a,int seen){if(seen!=themeGen)a.recreate();}

    static int dp(Context c,float v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    static TextView text(Context c,int style,CharSequence s){TextView t=new TextView(c,null,0,style);t.setText(s);return t;}
    static TextView text(Context c,int style,CharSequence s,float sp,boolean bold){
        TextView t=text(c,style,s);t.setTextSize(sp);if(bold)t.setTypeface(bold());return t;
    }
    static Typeface medium(){return Typeface.create("sans-serif-medium",Typeface.NORMAL);}
    static Typeface bold(){return Typeface.create("sans-serif-medium",Typeface.BOLD);}
    static LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    static LinearLayout.LayoutParams lpTop(Context c,int w,int h,float top){
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(w,h);l.topMargin=dp(c,top);return l;
    }
    static LinearLayout vertical(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    static LinearLayout horizontal(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}

    static LinearLayout card(Context c){
        LinearLayout l=vertical(c);l.setBackgroundResource(R.drawable.bg_card);l.setClipToOutline(true);return l;
    }
    static Drawable groupBg(Context c,boolean top,boolean bottom){
        float r=dp(c,22),t=top?r:0,b=bottom?r:0;
        GradientDrawable g=new GradientDrawable();g.setColor(c.getColor(R.color.surface));
        g.setCornerRadii(new float[]{t,t,t,t,b,b,b,b});
        GradientDrawable mask=new GradientDrawable();mask.setColor(0xFFFFFFFF);mask.setCornerRadii(new float[]{t,t,t,t,b,b,b,b});
        return new RippleDrawable(ColorStateList.valueOf(c.getColor(R.color.ripple)),g,mask);
    }
    static View separator(Context c,float startDp){
        View v=new View(c);v.setBackgroundColor(c.getColor(R.color.line));
        LinearLayout.LayoutParams l=new LinearLayout.LayoutParams(MATCH,Math.max(1,dp(c,1)));l.setMarginStart(dp(c,startDp));
        v.setLayoutParams(l);return v;
    }
    static TextView section(Context c,String s){
        TextView t=text(c,R.style.Text_Section,s);t.setPadding(dp(c,10),dp(c,22),dp(c,10),dp(c,8));return t;
    }
    static TextView label(Context c,String s){return text(c,R.style.Text_Label,s);}
    static TextView button(Context c,String s,boolean primary){
        TextView b=new TextView(c,null,0,R.style.Text);b.setText(s);b.setTextSize(primary?16:15);b.setTypeface(bold());
        b.setGravity(Gravity.CENTER);b.setTextColor(c.getColor(primary?R.color.on_accent:R.color.accent));
        b.setBackgroundResource(primary?R.drawable.bg_btn:R.drawable.bg_btn2);
        b.setMinHeight(dp(c,54));b.setPadding(dp(c,20),0,dp(c,20),0);b.setCompoundDrawablePadding(dp(c,8));
        b.setClickable(true);b.setFocusable(true);return b;
    }
    static void icon(TextView t,int res,int colorRes,float sizeDp){
        Context c=t.getContext();Drawable d=c.getDrawable(res).mutate();int s=dp(c,sizeDp);
        d.setBounds(0,0,s,s);d.setTint(c.getColor(colorRes));t.setCompoundDrawablesRelative(d,null,null,null);
    }
    static TextView textButton(Context c,String s){TextView t=new TextView(c,null,0,R.style.Text_Action);t.setText(s);t.setClickable(true);t.setFocusable(true);return t;}
    static ImageView iconView(Context c,int res,int colorRes,float sizeDp){
        ImageView iv=new ImageView(c);iv.setImageResource(res);iv.setImageTintList(ColorStateList.valueOf(c.getColor(colorRes)));
        iv.setLayoutParams(new LinearLayout.LayoutParams(dp(c,sizeDp),dp(c,sizeDp)));return iv;
    }
    static ImageView iconButton(Context c,int res,int colorRes,String description){
        ImageView b=new ImageView(c);b.setImageResource(res);b.setImageTintList(ColorStateList.valueOf(c.getColor(colorRes)));
        b.setScaleType(ImageView.ScaleType.CENTER);b.setBackgroundResource(R.drawable.bg_icon_btn);
        b.setContentDescription(description);b.setClickable(true);b.setFocusable(true);
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(c,48),dp(c,48)));return b;
    }
    static TextView chip(Context c,String s,boolean accent){
        TextView t=new TextView(c,null,0,R.style.Text_Chip);t.setText(s);
        if(accent){t.setBackgroundResource(R.drawable.bg_chip_a);t.setTextColor(c.getColor(R.color.accent));}
        t.setClickable(true);t.setFocusable(true);return t;
    }
    static void clickableCard(View v,String description,View.OnClickListener action){
        v.setContentDescription(description);v.setOnClickListener(action);v.setClickable(true);v.setFocusable(true);
    }

    // Formato
    static String mmss(long ms){
        long s=Math.max(0,ms)/1000,h=s/3600,m=(s/60)%60;
        if(h>0)return String.format(Locale.ROOT,"%d:%02d:%02d",h,m,s%60);
        return String.format(Locale.ROOT,"%d:%02d",m,s%60);
    }
    static String date(long ms){return new java.text.SimpleDateFormat("d MMM yyyy · HH:mm",new Locale("es","ES")).format(new java.util.Date(ms));}
    static String hour(long ms){return new java.text.SimpleDateFormat("HH:mm",Locale.ROOT).format(new java.util.Date(ms));}

    /** Desde Android 15 las apps se dibujan detrás de las barras: aplica los márgenes al contenido. */
    static void applyInsets(final Activity a){
        if(android.os.Build.VERSION.SDK_INT<35||a.isFinishing())return;
        final View content=a.findViewById(android.R.id.content);if(content==null)return;
        content.setOnApplyWindowInsetsListener((v,in)->{
            android.graphics.Insets bars=in.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
            android.graphics.Insets ime=in.getInsets(android.view.WindowInsets.Type.ime());
            v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
            return android.view.WindowInsets.CONSUMED;
        });
        content.requestApplyInsets();
    }
    /** Pantalla con fondo del tema y scroll vertical; devuelve el contenedor donde añadir las vistas. */
    static LinearLayout page(Activity a,LinearLayout content,android.widget.ScrollView[] scrollOut){
        android.widget.ScrollView s=new android.widget.ScrollView(a);s.setFillViewport(true);
        s.setVerticalScrollBarEnabled(false);s.setBackgroundColor(a.getColor(R.color.bg));
        content.setPadding(0,dp(a,8),0,dp(a,32));s.addView(content,new ViewGroup.LayoutParams(MATCH,WRAP));
        a.setContentView(s);applyInsets(a);if(scrollOut!=null)scrollOut[0]=s;return content;
    }
    static boolean empty(String s){return TextUtils.isEmpty(s);}
}
