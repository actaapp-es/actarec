package es.actarec.app;
import java.util.List;
/** Se ejecuta sin Android SDK: java -ea es.actarec.app.TranscriptSearchTest */
public final class TranscriptSearchTest {
    private static void ok(boolean c,String message){if(!c)throw new AssertionError(message);}
    public static void main(String[] args){
        ok(TranscriptSearch.excerpts("Hola mundo","¿Qué pasó?",100).equals("Hola mundo"),"conversación pequeña");
        ok(TranscriptSearch.excerpts("","x",100).isEmpty(),"vacía");
        StringBuilder longText=new StringBuilder();
        for(int i=0;i<45;i++)longText.append("En la sala hablamos de plazos. ");
        longText.append("El cliente confirmó presupuesto 2250 euros. ");
        for(int i=0;i<22;i++)longText.append("Seguimos revisando. ");
        String excerpt=TranscriptSearch.excerpts(longText.toString(),"¿Cuál fue el presupuesto?",850);
        ok(excerpt.contains("2250"),"recuperación por palabra clave");
        ok(excerpt.contains("[Fragmento"),"referencia de fragmento");
        List<String> chunks=TranscriptSearch.split("uno dos tres cuatro cinco seis siete ocho",12);
        String combined=String.join(" ",chunks);
        ok(combined.equals("uno dos tres cuatro cinco seis siete ocho"),"no truncar el contenido");
        ok(TranscriptSearch.split("texto",0).size()==1,"límite mínimo");
        System.out.println("TranscriptSearchTest: OK (5 casos)");
    }
}
