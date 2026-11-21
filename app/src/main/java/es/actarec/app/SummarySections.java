package es.actarec.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Interpreta los encabezados de Groq sin depender del formato Markdown exacto. Java puro. */
public final class SummarySections {
    public static final class Section {
        public final String title,text;
        Section(String title,String text){this.title=title;this.text=text;}
    }
    private SummarySections(){}
    private static String normalized(String s){
        return Normalizer.normalize(s.toUpperCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").replaceAll("[^A-Z ]","").trim();
    }
    public static List<Section> parse(String text){
        List<Section> list=new ArrayList<>();
        if(text==null||text.trim().isEmpty())return list;
        String title="En pocas palabras";StringBuilder content=new StringBuilder();
        for(String line:text.split("\\r?\\n")){
            String head=normalized(line);
            String newTitle=null;
            if(head.equals("RESUMEN"))newTitle="En pocas palabras";
            else if(head.equals("DECISIONES"))newTitle="Decisiones";
            else if(head.equals("TAREAS Y RESPONSABLES")||head.equals("TAREAS")||head.equals("PROXIMOS PASOS"))newTitle="Próximos pasos";
            else if(head.equals("DATOS IMPORTANTES")||head.equals("DATOS CLAVE"))newTitle="Datos relevantes";
            if(newTitle!=null){
                if(content.length()>0){list.add(new Section(title,content.toString().trim()));content.setLength(0);}
                title=newTitle;
            }else if(!line.trim().isEmpty()){
                if(content.length()>0)content.append('\n');
                content.append(line.replaceAll("^\\s*[-*]\\s+","• ").trim());
            }
        }
        if(content.length()>0)list.add(new Section(title,content.toString().trim()));
        if(list.isEmpty())list.add(new Section("En pocas palabras",text.trim()));
        return list;
    }
}
