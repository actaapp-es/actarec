package es.actarec.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * Diagrama de la grabación: de qué va (raíz) → temas en el orden en que se hablaron → decisiones → tareas.
 * Se dibuja con Canvas, sin librerías, y sigue el tema claro/oscuro.
 */
final class DiagramView extends View {
    private static final class Node {
        StaticLayout head;final List<StaticLayout> lines=new ArrayList<>();
        int fill,stroke,ink,sub;String marker;boolean root;
        float x,y,w,h;
    }
    private final List<Node> nodes=new ArrayList<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint headPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),rootPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),bodyPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG),
            markPaint=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Analysis analysis;private final String meta;
    private final float d;
    private int builtWidth=-1;float totalHeight;

    DiagramView(Context c,Analysis a,String meta){
        super(c);analysis=a;this.meta=meta;d=c.getResources().getDisplayMetrics().density;
        headPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));headPaint.setTextSize(15*d);
        rootPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));rootPaint.setTextSize(17*d);
        bodyPaint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));bodyPaint.setTextSize(13*d);
        markPaint.setTypeface(Typeface.create("sans-serif-medium",Typeface.BOLD));markPaint.setTextSize(13*d);markPaint.setTextAlign(Paint.Align.CENTER);
        setContentDescription(describe(a));
    }
    private static String describe(Analysis a){
        StringBuilder sb=new StringBuilder("Diagrama de la grabación. ");
        if(!a.title.isEmpty())sb.append(a.title).append(". ");
        for(int i=0;i<a.topics.size();i++)sb.append("Tema ").append(i+1).append(": ").append(a.topics.get(i).title).append(". ");
        return sb.toString();
    }
    private StaticLayout layout(String s,TextPaint p,int width){
        return StaticLayout.Builder.obtain(s,0,s.length(),p,Math.max(20,width)).setLineSpacing(2*d,1f).setAlignment(Layout.Alignment.ALIGN_NORMAL).build();
    }
    private void build(int width){
        nodes.clear();Context c=getContext();
        float spine=26*d,gap=22*d,pad=14*d;
        // Raíz
        Node root=new Node();root.root=true;root.fill=c.getColor(R.color.accent);root.stroke=root.fill;root.ink=c.getColor(R.color.on_accent);root.sub=0xCCFFFFFF;
        float rootW=width;
        root.head=layout(analysis.title.isEmpty()?"Grabación":analysis.title,rootPaint,(int)(rootW-2*pad));
        if(!meta.isEmpty())root.lines.add(layout(meta,bodyPaint,(int)(rootW-2*pad)));
        nodes.add(root);
        float cardX=spine*2+4*d,cardW=width-cardX;
        for(int i=0;i<analysis.topics.size();i++){
            Analysis.Topic t=analysis.topics.get(i);Node n=new Node();
            n.fill=c.getColor(R.color.surface);n.stroke=c.getColor(R.color.line);n.ink=c.getColor(R.color.ink);n.sub=c.getColor(R.color.ink_2);n.marker=String.valueOf(i+1);
            n.head=layout(t.title,headPaint,(int)(cardW-2*pad));
            for(String p:t.points)n.lines.add(layout("• "+p,bodyPaint,(int)(cardW-2*pad)));
            nodes.add(n);
        }
        if(!analysis.decisions.isEmpty()){
            Node n=new Node();n.fill=c.getColor(R.color.ok_soft);n.stroke=c.getColor(R.color.ok);n.ink=c.getColor(R.color.ok);n.sub=c.getColor(R.color.ink);n.marker="✓";
            n.head=layout("Decisiones y acuerdos",headPaint,(int)(cardW-2*pad));
            for(String p:analysis.decisions)n.lines.add(layout("• "+p,bodyPaint,(int)(cardW-2*pad)));
            nodes.add(n);
        }
        if(!analysis.tasks.isEmpty()){
            Node n=new Node();n.fill=c.getColor(R.color.warn_soft);n.stroke=c.getColor(R.color.warn);n.ink=c.getColor(R.color.warn);n.sub=c.getColor(R.color.ink);n.marker="!";
            n.head=layout("Tareas pendientes · "+analysis.tasks.size(),headPaint,(int)(cardW-2*pad));
            for(Analysis.Task t:analysis.tasks){
                String s="• "+t.title+(t.owner.isEmpty()?"":" ("+t.owner+")")+(t.date!=null?" · "+t.date:"");
                n.lines.add(layout(s,bodyPaint,(int)(cardW-2*pad)));
            }
            nodes.add(n);
        }
        float y=0;
        for(int i=0;i<nodes.size();i++){
            Node n=nodes.get(i);n.w=n.root?rootW:cardW;n.x=n.root?0:cardX;
            float h=pad+n.head.getHeight();
            for(StaticLayout l:n.lines)h+=6*d+l.getHeight();
            n.h=h+pad;n.y=y;y+=n.h+gap;
        }
        totalHeight=y-gap+2*d;builtWidth=width;
    }
    @Override protected void onMeasure(int wSpec,int hSpec){
        int w=MeasureSpec.getSize(wSpec);if(w<=0)w=(int)(320*d);
        if(w!=builtWidth)build(w);
        setMeasuredDimension(w,(int)Math.ceil(totalHeight));
    }
    @Override protected void onDraw(Canvas cv){
        if(nodes.isEmpty())return;
        float pad=14*d,r=14*d,spine=26*d;
        // Línea guía que une la raíz con cada bloque
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*d);paint.setColor(getContext().getColor(R.color.track));
        Node last=nodes.get(nodes.size()-1);
        if(nodes.size()>1)cv.drawLine(spine,nodes.get(0).y+nodes.get(0).h,spine,last.y+markY(last),paint);
        for(int i=0;i<nodes.size();i++){
            Node n=nodes.get(i);
            RectF box=new RectF(n.x,n.y,n.x+n.w,n.y+n.h);
            paint.setStyle(Paint.Style.FILL);paint.setColor(n.fill);cv.drawRoundRect(box,r,r,paint);
            if(!n.root){
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.5f*d);paint.setColor(n.stroke);cv.drawRoundRect(box,r,r,paint);
                // Nudo en la línea guía + conector horizontal
                float cy=n.y+markY(n);
                paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2*d);paint.setColor(getContext().getColor(R.color.track));
                cv.drawLine(spine+11*d,cy,n.x,cy,paint);
                paint.setStyle(Paint.Style.FILL);paint.setColor(n.stroke);cv.drawCircle(spine,cy,11*d,paint);
                markPaint.setColor(getContext().getColor(R.color.on_accent));
                cv.drawText(n.marker,spine,cy+4.5f*d,markPaint);
            }
            float ty=n.y+pad;
            cv.save();cv.translate(n.x+pad,ty);headPaint.setColor(n.ink);
            (n.root?rootPaint:headPaint).setColor(n.ink);bodyPaint.setColor(n.sub);
            n.head.draw(cv);cv.restore();
            ty+=n.head.getHeight();
            for(StaticLayout l:n.lines){
                ty+=6*d;cv.save();cv.translate(n.x+pad,ty);l.draw(cv);cv.restore();ty+=l.getHeight();
            }
        }
    }
    private float markY(Node n){return 14*d+n.head.getHeight()/2f;}
}
