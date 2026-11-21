package es.actarec.app;
import java.util.List;
public final class SummarySectionsTest {
  public static void main(String[] args){
    List<SummarySections.Section> a=SummarySections.parse("**RESUMEN**\nEl proyecto avanza.\n\n**DECISIONES**\n- Entregar en junio.\n\n**TAREAS Y RESPONSABLES**\nAna contacta al cliente.");
    if(a.size()!=3)throw new AssertionError("3 secciones, obtuve "+a.size());
    if(!a.get(1).text.contains("Entregar"))throw new AssertionError("decisión perdida");
    if(!a.get(2).title.equals("Próximos pasos"))throw new AssertionError("encabezado");
    if(!SummarySections.parse("Texto libre").get(0).text.equals("Texto libre"))throw new AssertionError("resumen libre");
    System.out.println("SummarySectionsTest: OK (4 casos)");
  }
}
