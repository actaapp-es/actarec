package es.actarec.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Reparte una transcripción en párrafos legibles. Java puro, sin red.
 * Corta cuando hay una pausa larga entre frases o cuando el párrafo ya es largo y termina una frase.
 */
public final class Paragraphs {
    private Paragraphs(){}
    /** Pausa (segundos) que se considera cambio de tema. */
    static final double PAUSE=1.6,LONG_PAUSE=4.0;
    static final int MIN_CHARS=110,TARGET_CHARS=300,MAX_CHARS=520;

    /** Un fragmento de texto con su inicio y fin en segundos (-1 si no se conocen). */
    public static final class Piece {
        public final double start,end;public final String text;
        public Piece(double start,double end,String text){this.start=start;this.end=end;this.text=text;}
    }

    private static boolean endsSentence(String s){
        String t=s.trim();if(t.isEmpty())return false;
        char c=t.charAt(t.length()-1);
        return c=='.'||c=='!'||c=='?'||c=='…'||c=='"'||c=='»'||c==')';
    }

    /** Con tiempos: pausas entre fragmentos + longitud. */
    public static String fromPieces(List<Piece> pieces){
        StringBuilder out=new StringBuilder(),cur=new StringBuilder();
        double lastEnd=-1;
        for(Piece p:pieces){
            String t=p.text.trim();if(t.isEmpty())continue;
            if(cur.length()>0){
                boolean timed=lastEnd>=0&&p.start>=0;
                double gap=timed?p.start-lastEnd:0;
                boolean closed=endsSentence(cur.toString());
                boolean cut=(timed&&gap>=LONG_PAUSE&&cur.length()>=60)
                        ||(timed&&gap>=PAUSE&&closed&&cur.length()>=MIN_CHARS)
                        ||(closed&&cur.length()>=MIN_CHARS+40&&startsNewIdea(t))
                        ||(closed&&cur.length()>=TARGET_CHARS&&!timed)
                        ||(closed&&cur.length()>=MAX_CHARS);
                if(cut){flush(out,cur);}else cur.append(' ');
            }
            cur.append(t);
            lastEnd=p.end>p.start?p.end:p.start;
        }
        flush(out,cur);
        return out.toString().trim();
    }

    /** Sin tiempos (texto plano o transcripciones antiguas): agrupa frases. */
    public static String fromText(String text){
        if(text==null)return "";
        String t=text.trim();if(t.isEmpty())return "";
        if(t.contains("\n\n"))return t; // ya tiene párrafos
        String[] sentences=t.replaceAll("\\s*\\n\\s*"," ").split("(?<=[.!?…])\\s+(?=[\\p{Lu}¿¡\"«(\\d])");
        List<Piece> pieces=new ArrayList<>();
        for(String s:sentences)pieces.add(new Piece(-1,-1,s));
        return fromPieces(pieces);
    }
    private static void flush(StringBuilder out,StringBuilder cur){
        if(cur.length()==0)return;
        if(out.length()>0)out.append("\n\n");
        out.append(cur.toString().trim());cur.setLength(0);
    }
    /** Conectores que suelen abrir una idea nueva. */
    private static boolean startsNewIdea(String s){
        String l=s.toLowerCase(java.util.Locale.ROOT);
        String[] starts={"bueno","entonces","por otro lado","por otra parte","además","luego","después","vale","otra cosa","y respecto","en cuanto a","por cierto","pues","ahora","para terminar","por último","en resumen","respecto a","sobre "};
        for(String w:starts)if(l.startsWith(w))return true;
        return false;
    }
}
