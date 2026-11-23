package es.actarec.app;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Resultado estructurado del análisis (mismo formato que Acta): contexto, resumen, nota y tareas. */
final class Analysis {
    static final class Task {
        String title="",detail="",owner="",date,time;
    }
    static final class Topic {
        String title="";final List<String> points=new ArrayList<>();
    }
    String title="",context="",summary="",note="";
    final List<Topic> topics=new ArrayList<>();
    final List<String> decisions=new ArrayList<>();
    final List<Task> tasks=new ArrayList<>();

    private static final Pattern DATE=Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final Pattern TIME=Pattern.compile("\\d{1,2}:\\d{2}");

    String toJson(){
        try{
            JSONObject o=new JSONObject();o.put("v",3);o.put("titulo",title);o.put("contexto",context);o.put("resumen",summary);o.put("nota",note);
            JSONArray arr=new JSONArray();
            for(Task t:tasks){
                JSONObject j=new JSONObject();j.put("titulo",t.title);j.put("detalle",t.detail);j.put("responsable",t.owner);
                j.put("fecha",t.date==null?JSONObject.NULL:t.date);j.put("hora",t.time==null?JSONObject.NULL:t.time);arr.put(j);
            }
            o.put("tareas",arr);
            JSONArray tp=new JSONArray();
            for(Topic t:topics){JSONObject j=new JSONObject();j.put("titulo",t.title);j.put("puntos",new JSONArray(t.points));tp.put(j);}
            o.put("temas",tp);o.put("decisiones",new JSONArray(decisions));
            return o.toString();
        }catch(Exception e){return "";}
    }
    /** null si es un resumen antiguo en texto plano. */
    static Analysis parse(String stored){
        if(stored==null)return null;String s=stored.trim();if(!s.startsWith("{"))return null;
        try{return fromJson(new JSONObject(s));}catch(Exception e){return null;}
    }
    static Analysis fromJson(JSONObject o){
        Analysis r=new Analysis();
        r.title=clean(o.optString("titulo",""));
        JSONArray tp=o.optJSONArray("temas");
        if(tp!=null)for(int i=0;i<tp.length()&&r.topics.size()<7;i++){
            JSONObject j=tp.optJSONObject(i);if(j==null)continue;
            Topic t=new Topic();t.title=clean(j.optString("titulo",""));if(t.title.isEmpty())continue;
            JSONArray pts=j.optJSONArray("puntos");
            if(pts!=null)for(int k=0;k<pts.length()&&t.points.size()<4;k++){String x=clean(pts.optString(k,""));if(!x.isEmpty())t.points.add(x);}
            r.topics.add(t);
        }
        JSONArray dc=o.optJSONArray("decisiones");
        if(dc!=null)for(int i=0;i<dc.length()&&r.decisions.size()<6;i++){String x=clean(dc.optString(i,""));if(!x.isEmpty())r.decisions.add(x);}
        r.context=clean(o.optString("contexto",""));r.summary=clean(o.optString("resumen",""));r.note=clean(o.optString("nota",""));
        JSONArray arr=o.optJSONArray("tareas");
        if(arr!=null)for(int i=0;i<arr.length();i++){
            JSONObject j=arr.optJSONObject(i);if(j==null)continue;
            Task t=new Task();t.title=clean(j.optString("titulo",""));if(t.title.isEmpty())continue;
            t.detail=clean(j.optString("detalle",""));t.owner=clean(j.optString("responsable",""));
            String d=j.isNull("fecha")?null:j.optString("fecha",null),h=j.isNull("hora")?null:j.optString("hora",null);
            t.date=d!=null&&DATE.matcher(d.trim()).matches()?d.trim():null;
            if(h!=null&&TIME.matcher(h.trim()).matches()){String hh=h.trim();t.time=hh.length()==4?"0"+hh:hh;}
            r.tasks.add(t);
        }
        return r;
    }
    private static String clean(String s){return s==null||"null".equalsIgnoreCase(s.trim())?"":s.trim();}
    boolean isEmpty(){return context.isEmpty()&&summary.isEmpty()&&tasks.isEmpty()&&topics.isEmpty();}
    boolean hasDiagram(){return !topics.isEmpty();}

    /** Texto para copiar o compartir. */
    static String toText(String stored){
        Analysis a=parse(stored);if(a==null)return stored==null?"":stored;
        StringBuilder sb=new StringBuilder();
        if(!a.title.isEmpty())sb.append(a.title).append("\n\n");
        if(!a.context.isEmpty())sb.append("Contexto\n").append(a.context).append("\n\n");
        if(!a.summary.isEmpty())sb.append("Resumen\n").append(a.summary).append("\n\n");
        if(!a.topics.isEmpty()){
            sb.append("Temas, en orden\n");
            for(int i=0;i<a.topics.size();i++){
                Topic t=a.topics.get(i);sb.append(i+1).append(". ").append(t.title).append("\n");
                for(String x:t.points)sb.append("   - ").append(x).append("\n");
            }
            sb.append("\n");
        }
        if(!a.decisions.isEmpty()){
            sb.append("Decisiones y acuerdos\n");
            for(String x:a.decisions)sb.append("• ").append(x).append("\n");
            sb.append("\n");
        }
        if(!a.tasks.isEmpty()){
            sb.append("Tareas\n");
            for(Task t:a.tasks){
                sb.append("• ").append(t.title);
                if(!t.owner.isEmpty())sb.append(" (").append(t.owner).append(")");
                if(t.date!=null)sb.append(" · ").append(t.date).append(t.time!=null?" "+t.time:"");
                if(!t.detail.isEmpty())sb.append("\n  ").append(t.detail);
                sb.append("\n");
            }
            sb.append("\n");
        }
        if(!a.note.isEmpty())sb.append("Para recordar\n").append(a.note).append("\n");
        return sb.toString().trim();
    }
}
