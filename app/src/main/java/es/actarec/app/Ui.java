package es.actarec.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

/** Componentes de diseño nativos. Sin fuentes ni bibliotecas externas. */
public final class Ui {
    private Ui() {}
    public static final int BG=0xFFF5F4F0, WHITE=Color.WHITE, GREEN=0xFF1E4D45;
    public static final int DARK_GREEN=0xFF143A33, INK=0xFF151614, MUTED=0xFF62665F;
    public static final int FAINT=0xFF8A8E87, LINE=0xFFE5E6E0, SOFT=0xFFE5EFEA;
    public static final int RED=0xFFD75B53, CREAM=0xFFFFF7E9, MINT=0xFFBCE7D3;

    public static int dp(Context c,float v){return (int)(v*c.getResources().getDisplayMetrics().density+0.5f);}
    public static GradientDrawable shape(Context c,int color,float radius){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
    }
    public static GradientDrawable outlined(Context c,int color,float radius,int stroke){
        GradientDrawable d=shape(c,color,radius);d.setStroke(dp(c,1),stroke);return d;
    }
    public static android.graphics.drawable.Drawable touchBackground(Context c,int color,int radius,int stroke){
        GradientDrawable base=stroke==0?shape(c,color,radius):outlined(c,color,radius,stroke);
        return new RippleDrawable(ColorStateList.valueOf(0x241E4D45),base,shape(c,Color.WHITE,radius));
    }
    public static LinearLayout vertical(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.VERTICAL);return l;}
    public static LinearLayout horizontal(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    public static LinearLayout card(Context c){
        LinearLayout l=vertical(c);l.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));
        l.setBackground(outlined(c,WHITE,22,LINE));return l;
    }
    public static TextView text(Context c,String value,float size,int color,boolean medium){
        TextView t=new TextView(c);t.setText(value);t.setTextColor(color);t.setTextSize(size);t.setIncludeFontPadding(false);
        t.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif",Typeface.NORMAL));
        t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);return t;
    }
    public static TextView button(Context c,String title,int bg,int fg){
        TextView t=text(c,title,14,fg,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,54));
        t.setPadding(dp(c,16),dp(c,13),dp(c,16),dp(c,13));
        t.setBackground(touchBackground(c,bg,17,0));t.setClickable(true);t.setFocusable(true);return t;
    }
    public static TextView mini(Context c,String title,int bg,int fg){
        TextView t=text(c,title,12.5f,fg,true);t.setGravity(Gravity.CENTER);t.setMinHeight(dp(c,42));
        t.setPadding(dp(c,13),dp(c,8),dp(c,13),dp(c,8));
        t.setBackground(touchBackground(c,bg,13,0));t.setClickable(true);t.setFocusable(true);return t;
    }
    public static TextView tag(Context c,String label,int color){
        TextView v=text(c,label.toUpperCase(java.util.Locale.ROOT),10.5f,color,true);
        v.setLetterSpacing(.14f);return v;
    }
    public static TextView chip(Context c,String label,boolean active){
        TextView v=text(c,label,12,active?WHITE:GREEN,true);v.setMinHeight(dp(c,37));v.setGravity(Gravity.CENTER);
        v.setPadding(dp(c,15),dp(c,8),dp(c,15),dp(c,8));
        v.setBackground(touchBackground(c,active?GREEN:WHITE,24,active?0:LINE));v.setClickable(true);v.setFocusable(true);return v;
    }
    public static View space(Context c,int h){Space s=new Space(c);s.setLayoutParams(new LinearLayout.LayoutParams(1,dp(c,h)));return s;}
    public static LinearLayout.LayoutParams lp(Context c,int w,int h){return new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));}
    public static LinearLayout.LayoutParams margin(Context c,int w,int h,int top,int bottom){
        LinearLayout.LayoutParams p=lp(c,w,h);p.topMargin=dp(c,top);p.bottomMargin=dp(c,bottom);return p;
    }
    public static ScrollView page(Activity a,LinearLayout content){
        a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        // En Android 11+ usamos edge-to-edge explícito: los insets se aplican una sola vez.
        // Evita un encabezado duplicado o contenido tapado por las barras en Android 15.
        if(Build.VERSION.SDK_INT>=30)a.getWindow().setDecorFitsSystemWindows(false);
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        scroll.setVerticalScrollBarEnabled(false);scroll.setBackgroundColor(BG);
        content.setPadding(dp(a,20),dp(a,24),dp(a,20),dp(a,40));
        if(Build.VERSION.SDK_INT>=30) {
            scroll.setOnApplyWindowInsetsListener((v,insets)->{
                android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.statusBars()|WindowInsets.Type.navigationBars());
                content.setPadding(dp(a,20),dp(a,18)+bars.top,dp(a,20),dp(a,35)+bars.bottom);
                return insets;
            });
        }
        scroll.addView(content);return scroll;
    }
    public static EditText input(Context c,String hint,boolean secret){
        EditText e=new EditText(c);e.setTextColor(INK);e.setHintTextColor(FAINT);e.setTextSize(14.5f);
        e.setSingleLine(true);e.setPadding(dp(c,16),dp(c,13),dp(c,16),dp(c,13));
        e.setBackground(outlined(c,WHITE,15,LINE));e.setHint(hint);
        if(secret)e.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }
    public static void clickable(View v,String description,View.OnClickListener action){
        v.setContentDescription(description);v.setOnClickListener(action);v.setClickable(true);v.setFocusable(true);
    }
    public static FrameLayout iconSquare(Context c,String name,int color,int bg,int side){
        FrameLayout frame=new FrameLayout(c);frame.setBackground(shape(c,bg,14));
        Icon icon=new Icon(c,name,color);FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(dp(c,side/2f),dp(c,side/2f),Gravity.CENTER);
        frame.addView(icon,p);return frame;
    }
    public static TextView section(Context c,String title){return text(c,title,21,INK,true);}
    public static String mmss(long ms){
        long s=Math.max(0,ms)/1000,h=s/3600,m=(s/60)%60;
        if(h>0)return String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",h,m,s%60);
        return String.format(java.util.Locale.ROOT,"%02d:%02d",m,s%60);
    }
    public static String date(long ms){return new java.text.SimpleDateFormat("d MMM yyyy · HH:mm",new java.util.Locale("es","ES")).format(new java.util.Date(ms));}

    /** Iconos vectoriales de 24x24, usados a escala para que no dependan de emojis. */
    public static final class Icon extends View {
        private final String type;private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int color;
        public Icon(Context c,String type,int color){super(c);this.type=type;this.color=color;setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);}
        @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);
            float scale=Math.min(getWidth(),getHeight())/24f;
            canvas.save();canvas.translate((getWidth()-24*scale)/2,(getHeight()-24*scale)/2);canvas.scale(scale,scale);
            p.setColor(color);p.setStrokeWidth(1.85f);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
            switch(type){
                case "mic": rounded(canvas,9,2,15,15,3);line(canvas,5,11,5,12);arc(canvas,5,8,19,19,0,180);line(canvas,12,19,12,22);line(canvas,8,22,16,22);break;
                case "play":p.setStyle(Paint.Style.FILL);path(canvas,8,5,8,19,19,12);break;
                case "pause":line(canvas,8,5,8,19);line(canvas,16,5,16,19);break;
                case "stop":rounded(canvas,5,5,19,19,2);break;
                case "settings":circle(canvas,12,12,3);circle(canvas,12,12,9);line(canvas,12,1,12,4);line(canvas,12,20,12,23);line(canvas,1,12,4,12);line(canvas,20,12,23,12);break;
                case "back":line(canvas,20,12,4,12);line(canvas,4,12,11,5);line(canvas,4,12,11,19);break;
                case "forward":line(canvas,4,12,20,12);line(canvas,20,12,13,5);line(canvas,20,12,13,19);break;
                case "chevron":line(canvas,9,5,16,12);line(canvas,16,12,9,19);break;
                case "clock":circle(canvas,12,12,9);line(canvas,12,7,12,12);line(canvas,12,12,17,14);break;
                case "audio":line(canvas,4,9,4,15);line(canvas,8,5,8,19);line(canvas,12,2,12,22);line(canvas,16,6,16,18);line(canvas,20,9,20,15);break;
                case "check":line(canvas,4,12,10,18);line(canvas,10,18,20,6);break;
                case "plus":line(canvas,12,4,12,20);line(canvas,4,12,20,12);break;
                case "search":circle(canvas,10,10,7);line(canvas,15,15,21,21);break;
                case "shield":path(canvas,12,2,21,6,20,15,12,22,4,15,3,6);break;
                case "trash":line(canvas,5,7,19,7);line(canvas,9,4,15,4);path(canvas,6,7,7,21,17,21,18,7);line(canvas,10,11,10,17);line(canvas,14,11,14,17);break;
                case "spark":line(canvas,12,2,12,22);line(canvas,2,12,22,12);line(canvas,5,5,19,19);line(canvas,19,5,5,19);break;
                case "chat":rounded(canvas,3,4,21,18,3);line(canvas,7,18,7,22);line(canvas,7,22,12,18);line(canvas,7,9,17,9);line(canvas,7,13,14,13);break;
                case "document":rounded(canvas,5,2,19,22,2);line(canvas,8,8,16,8);line(canvas,8,12,16,12);line(canvas,8,16,13,16);break;
                case "share":line(canvas,12,15,12,3);line(canvas,7,8,12,3);line(canvas,17,8,12,3);line(canvas,5,14,5,21);line(canvas,5,21,19,21);line(canvas,19,21,19,14);break;
                case "star":path(canvas,12,2,15,9,22,9,17,14,19,22,12,18,5,22,7,14,2,9,9,9);break;
                default:circle(canvas,12,12,5);
            }
            canvas.restore();
        }
        private void line(Canvas c,float a,float b,float x,float y){c.drawLine(a,b,x,y,p);}
        private void circle(Canvas c,float x,float y,float r){c.drawCircle(x,y,r,p);}
        private void rounded(Canvas c,float a,float b,float x,float y,float radius){c.drawRoundRect(a,b,x,y,radius,radius,p);}
        private void arc(Canvas c,float a,float b,float x,float y,float start,float sweep){c.drawArc(a,b,x,y,start,sweep,false,p);}
        private void path(Canvas c,float...coords){Path path=new Path();path.moveTo(coords[0],coords[1]);for(int i=2;i<coords.length;i+=2)path.lineTo(coords[i],coords[i+1]);path.close();c.drawPath(path,p);}
    }
}
