package es.actarec.app;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;

/** Elementos visuales nativos inspirados en la paleta pública de Acta (actaapp-es/acta). */
public final class Ui {
    private Ui() {}
    public static final int BG = Color.rgb(245,244,240);
    public static final int WHITE = Color.WHITE;
    public static final int GREEN = Color.rgb(30,77,69);
    public static final int DARK_GREEN = Color.rgb(20,57,51);
    public static final int INK = Color.rgb(21,22,20);
    public static final int MUTED = Color.rgb(92,94,89);
    public static final int FAINT = Color.rgb(139,141,135);
    public static final int LINE = Color.rgb(230,228,222);
    public static final int SOFT = Color.rgb(225,235,231);
    public static final int RED = Color.rgb(218,95,81);

    public static int dp(Context c, float n) { return (int) (n * c.getResources().getDisplayMetrics().density + .5f); }
    public static GradientDrawable shape(Context c, int color, float radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(c, radius));
        return d;
    }
    public static GradientDrawable outline(Context c, int color, float radius, int stroke) {
        GradientDrawable d = shape(c, color, radius);
        d.setStroke(dp(c, 1), stroke);
        return d;
    }
    public static LinearLayout vertical(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    public static LinearLayout horizontal(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    public static LinearLayout card(Context c) {
        LinearLayout l = vertical(c);
        l.setPadding(dp(c,20),dp(c,20),dp(c,20),dp(c,20));
        l.setBackground(shape(c, WHITE, 22));
        return l;
    }
    public static TextView text(Context c, String value, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(sp);
        t.setFontFeatureSettings("kern");
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }
    public static TextView button(Context c, String value, int bg, int fg) {
        TextView t = text(c, value, 15, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(c,52));
        t.setPadding(dp(c,18),dp(c,12),dp(c,18),dp(c,12));
        t.setBackground(shape(c, bg, 16));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }
    public static TextView mini(Context c, String value, int bg, int fg) {
        TextView t = text(c, value, 13, fg, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c,14),dp(c,10),dp(c,14),dp(c,10));
        t.setMinHeight(dp(c,42));
        t.setBackground(shape(c,bg,13));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }
    public static TextView tag(Context c, String value, int color) {
        TextView t = text(c, value, 11, color, true);
        t.setLetterSpacing(.16f);
        return t;
    }
    public static View space(Context c, int height) {
        Space v = new Space(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1,dp(c,height)));
        return v;
    }
    public static LinearLayout.LayoutParams lp(Context c, int width, int height) {
        return new LinearLayout.LayoutParams(width < 0 ? width : dp(c,width), height < 0 ? height : dp(c,height));
    }
    public static void setTop(Context c, View v, int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin=dp(c,top);
        v.setLayoutParams(p);
    }
    public static ScrollView page(Activity a, LinearLayout content) {
        a.getWindow().setStatusBarColor(BG);
        a.getWindow().setNavigationBarColor(BG);
        a.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        ScrollView s = new ScrollView(a);
        s.setFillViewport(true);
        s.setClipToPadding(false);
        s.setBackgroundColor(BG);
        s.setVerticalScrollBarEnabled(false);
        content.setPadding(dp(a,22),dp(a,24),dp(a,22),dp(a,40));
        s.addView(content);
        return s;
    }
    public static EditText input(Context c, String hint, boolean secret) {
        EditText e = new EditText(c);
        e.setTextColor(INK);
        e.setHintTextColor(FAINT);
        e.setTextSize(15);
        e.setSingleLine(true);
        e.setPadding(dp(c,16),dp(c,12),dp(c,16),dp(c,12));
        e.setBackground(outline(c, WHITE, 14, LINE));
        e.setHint(hint);
        if(secret) e.setInputType(0x81); // TYPE_CLASS_TEXT | TYPE_TEXT_VARIATION_PASSWORD
        return e;
    }
    public static String mmss(long millis) {
        long seconds=Math.max(0,millis)/1000;
        long h=seconds/3600, m=(seconds/60)%60, s=seconds%60;
        return h>0 ? String.format(java.util.Locale.ROOT,"%02d:%02d:%02d",h,m,s)
                : String.format(java.util.Locale.ROOT,"%02d:%02d",m,s);
    }
    public static String date(long epochMillis) {
        java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("d MMM yyyy · HH:mm", new java.util.Locale("es","ES"));
        return f.format(new java.util.Date(epochMillis));
    }
}
