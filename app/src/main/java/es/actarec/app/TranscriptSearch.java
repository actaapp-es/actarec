package es.actarec.app;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Recuperación de fragmentos local, sin subir toda la biblioteca a la nube. Java puro. */
public final class TranscriptSearch {
    private static final Set<String> STOP=new HashSet<>(Arrays.asList(
            "para","como","donde","cuando","cual","cuanto","sobre","porque","quiero","saber","dijo",
            "dice","dijeron","que","con","los","las","del","una","por","hay","fue","son","esta",
            "este","estos","estas","todo","todos","algo","cada","entre","tener","tiene","hemos",
            "han","estamos","cuales","aqui","mucho","desde","hasta","quien","quienes","hablamos"));
    private TranscriptSearch(){}
    public static String excerpts(String transcript,String question,int limit){
        if(transcript==null||transcript.trim().isEmpty())return "";
        if(transcript.length()<=limit)return transcript;
        List<String> blocks=split(transcript,Math.min(900,Math.max(120,limit/5)));
        Set<String> words=new HashSet<>();
        for(String w:normalize(question).split("\\s+"))if(w.length()>3&&!STOP.contains(w))words.add(w);
        final List<Integer> indices=new ArrayList<>();for(int i=0;i<blocks.size();i++)indices.add(i);
        indices.sort((a,b)->Integer.compare(score(blocks.get(b),words),score(blocks.get(a),words)));
        // Reservamos espacio para inicio y cierre: acuerdos suelen estar al final.
        Set<Integer> pick=new HashSet<>();int used=0;
        for(int i:new int[]{0,blocks.size()-1}){
            if(!pick.contains(i) && used+blocks.get(i).length()<=limit){pick.add(i);used+=blocks.get(i).length();}
        }
        for(int i:indices){if(used+blocks.get(i).length()+40>limit)continue;
            if(pick.add(i))used+=blocks.get(i).length()+40;
        }
        List<Integer> order=new ArrayList<>(pick);Collections.sort(order);
        StringBuilder out=new StringBuilder();
        for(int index:order)out.append("[Fragmento ").append(index+1).append("]\n").append(blocks.get(index)).append("\n\n");
        return out.toString().trim();
    }
    public static List<String> split(String text,int maxChars){
        List<String> sections=new ArrayList<>();if(text==null||text.isEmpty())return sections;
        int max=Math.max(20,maxChars),p=0;
        while(p<text.length()){
            int end=Math.min(text.length(),p+max);
            if(end<text.length()){
                int para=text.lastIndexOf("\n\n",end);
                int space=text.lastIndexOf(' ',end);
                if(para>p+max/2)end=para;else if(space>p+max/2)end=space;
            }
            if(end<=p)end=Math.min(text.length(),p+max);
            sections.add(text.substring(p,end));p=end;
            while(p<text.length()&&Character.isWhitespace(text.charAt(p)))p++;
        }
        return sections;
    }
    private static int score(String block,Set<String> words){
        String content=" "+normalize(block)+" ";int points=0;
        for(String word:words){if(content.contains(" "+word+" "))points+=5;else if(content.contains(word))points+=1;}
        return points;
    }
    private static String normalize(String value){
        if(value==null)return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT),Normalizer.Form.NFD)
                .replaceAll("\\p{M}+","").replaceAll("[^a-z0-9 ]"," ");
    }
}
