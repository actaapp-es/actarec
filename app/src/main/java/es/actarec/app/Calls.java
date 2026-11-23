package es.actarec.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Llamadas perdidas (del registro del teléfono) y llamadas programadas (avisos con botón «Llamar»). */
final class Calls {
    private Calls(){}
    static final String CH_REMIND="remind",CH_MISSED="missed";
    static final String ACTION_FIRE="es.actarec.app.CALL_REMINDER",ACTION_SNOOZE="es.actarec.app.CALL_SNOOZE";
    private static final String P_SCHED="sched_calls",P_DISMISS="missed_dismissed",P_LAST="missed_last_seen",P_NOTIFY="missed_notify";
    static final int REQ_LOG=41,REQ_NOTI=42;
    private static final Locale ES=new Locale("es","ES");

    // ------------------------------------------------------------ Permisos y ajustes
    static boolean canReadLog(Context c){return c.checkSelfPermission(Manifest.permission.READ_CALL_LOG)==PackageManager.PERMISSION_GRANTED;}
    static boolean notifyEnabled(Context c){return canReadLog(c)&&Ui.prefs(c).getBoolean(P_NOTIFY,true);}
    static void setNotify(Context c,boolean on){Ui.prefs(c).edit().putBoolean(P_NOTIFY,on).apply();if(on)MissedCallJob.schedule(c);else MissedCallJob.cancel(c);}

    // ------------------------------------------------------------ Llamadas perdidas
    static final class Missed{String number,name;int count;long last;}

    /** Perdidas de los últimos 14 días que aún no has devuelto ni descartado, la más reciente primero. */
    static List<Missed> missed(Context c){
        List<Missed> out=new ArrayList<>();
        if(!canReadLog(c))return out;
        Map<String,Long> dismissed=dismissed(c);
        Map<String,Missed> by=new HashMap<>();
        List<String> order=new ArrayList<>();
        java.util.Set<String> returned=new java.util.HashSet<>();
        long since=System.currentTimeMillis()-14L*24*3600*1000;
        try(Cursor q=c.getContentResolver().query(CallLog.Calls.CONTENT_URI,
                new String[]{CallLog.Calls.NUMBER,CallLog.Calls.CACHED_NAME,CallLog.Calls.TYPE,CallLog.Calls.DATE,CallLog.Calls.DURATION},
                CallLog.Calls.DATE+">?",new String[]{String.valueOf(since)},CallLog.Calls.DATE+" DESC")){
            if(q==null)return out;
            while(q.moveToNext()){
                String num=q.getString(0);if(num==null||num.trim().isEmpty())continue;
                String key=key(num);int type=q.getInt(2);long date=q.getLong(3);
                boolean answered=type==CallLog.Calls.OUTGOING_TYPE||(type==CallLog.Calls.INCOMING_TYPE&&q.getLong(4)>0);
                if(answered){returned.add(key);continue;}
                if(type!=CallLog.Calls.MISSED_TYPE)continue;
                if(returned.contains(key))continue;           // ya la has devuelto o la contestaste después
                Long d=dismissed.get(key);if(d!=null&&date<=d)continue;
                Missed m=by.get(key);
                if(m==null){m=new Missed();m.number=num;m.name=q.getString(1);m.last=date;by.put(key,m);order.add(key);}
                m.count++;
            }
        }catch(Exception ignored){}
        for(String k:order)out.add(by.get(k));
        return out;
    }
    static void dismiss(Context c,String number){
        Map<String,Long> m=dismissed(c);m.put(key(number),System.currentTimeMillis());
        JSONObject o=new JSONObject();try{for(Map.Entry<String,Long> e:m.entrySet())o.put(e.getKey(),e.getValue());}catch(Exception ignored){}
        Ui.prefs(c).edit().putString(P_DISMISS,o.toString()).apply();
    }
    private static Map<String,Long> dismissed(Context c){
        Map<String,Long> m=new HashMap<>();
        try{JSONObject o=new JSONObject(Ui.prefs(c).getString(P_DISMISS,"{}"));java.util.Iterator<String> it=o.keys();while(it.hasNext()){String k=it.next();m.put(k,o.getLong(k));}}catch(Exception ignored){}
        return m;
    }
    static String key(String number){
        String d=PhoneNumberUtils.normalizeNumber(number==null?"":number).replaceAll("[^0-9]","");
        return d.length()>9?d.substring(d.length()-9):d;
    }
    static String pretty(String number){
        String f=PhoneNumberUtils.formatNumber(number,"ES");
        return f==null?number:f;
    }
    static String ago(long t){
        long m=Math.max(0,(System.currentTimeMillis()-t)/60000);
        if(m<1)return "ahora";if(m<60)return "hace "+m+" min";
        long h=m/60;if(h<24)return "hace "+h+(h==1?" hora":" horas");
        long d=h/24;return "hace "+d+(d==1?" día":" días");
    }

    // ------------------------------------------------------------ Llamadas programadas
    static final class Sched{String id,number;long at;}

    static List<Sched> scheduled(Context c){
        List<Sched> l=new ArrayList<>();
        try{JSONArray a=new JSONArray(Ui.prefs(c).getString(P_SCHED,"[]"));
            for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);Sched s=new Sched();s.id=o.getString("id");s.number=o.getString("n");s.at=o.getLong("t");l.add(s);}
        }catch(Exception ignored){}
        java.util.Collections.sort(l,(x,y)->Long.compare(x.at,y.at));
        return l;
    }
    private static void save(Context c,List<Sched> l){
        JSONArray a=new JSONArray();
        try{for(Sched s:l){JSONObject o=new JSONObject();o.put("id",s.id);o.put("n",s.number);o.put("t",s.at);a.put(o);}}catch(Exception ignored){}
        Ui.prefs(c).edit().putString(P_SCHED,a.toString()).apply();
    }
    static void schedule(Context c,String number,long at){
        List<Sched> l=scheduled(c);
        Sched s=new Sched();s.id=Long.toHexString(System.nanoTime());s.number=number;s.at=at;l.add(s);save(c,l);
        arm(c,s);
    }
    static void cancel(Context c,String id){
        List<Sched> l=scheduled(c);
        for(int i=0;i<l.size();i++)if(l.get(i).id.equals(id)){l.remove(i);break;}
        save(c,l);
        AlarmManager am=c.getSystemService(AlarmManager.class);
        if(am!=null)am.cancel(firePending(c,id,null));
        NotificationManager nm=c.getSystemService(NotificationManager.class);if(nm!=null)nm.cancel(id.hashCode());
    }
    static void rearmAll(Context c){for(Sched s:scheduled(c))arm(c,s);}
    private static PendingIntent firePending(Context c,String id,String number){
        Intent i=new Intent(c,CallReminderReceiver.class).setAction(ACTION_FIRE).putExtra("id",id).putExtra("number",number);
        i.setData(Uri.parse("actarec://call/"+id));
        return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private static void arm(Context c,Sched s){
        AlarmManager am=c.getSystemService(AlarmManager.class);if(am==null)return;
        long at=Math.max(s.at,System.currentTimeMillis()+1000);
        PendingIntent pi=firePending(c,s.id,s.number);
        try{
            if(android.os.Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
            else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
        }catch(SecurityException e){am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);}
    }

    // ------------------------------------------------------------ Notificaciones
    private static void channels(Context c){
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel a=new NotificationChannel(CH_REMIND,"Llamadas programadas",NotificationManager.IMPORTANCE_HIGH);
        a.setDescription("Avisos para llamar a un número a la hora que programaste");nm.createNotificationChannel(a);
        NotificationChannel b=new NotificationChannel(CH_MISSED,"Llamadas perdidas",NotificationManager.IMPORTANCE_DEFAULT);
        b.setDescription("Aviso de llamadas perdidas, con botones para devolverlas o programarlas");nm.createNotificationChannel(b);
    }
    private static PendingIntent dial(Context c,String number,int code){
        return PendingIntent.getActivity(c,code,new Intent(Intent.ACTION_DIAL,Uri.fromParts("tel",number,null)),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    private static PendingIntent scheduleScreen(Context c,String number,int code){
        return PendingIntent.getActivity(c,code,new Intent(c,ScheduleCallActivity.class).putExtra("number",number).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
    }
    static void fire(Context c,String id,String number){
        if(number==null)return;
        channels(c);
        for(Sched s:scheduled(c))if(s.id.equals(id)){
            List<Sched> l=scheduled(c);for(int i=0;i<l.size();i++)if(l.get(i).id.equals(id)){l.remove(i);break;}save(c,l);break;
        }
        int nid=id.hashCode();
        PendingIntent dial=dial(c,number,nid);
        Intent sn=new Intent(c,CallReminderReceiver.class).setAction(ACTION_SNOOZE).putExtra("id",id).putExtra("number",number);
        sn.setData(Uri.parse("actarec://snooze/"+id));
        PendingIntent snooze=PendingIntent.getBroadcast(c,nid,sn,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n=new Notification.Builder(c,CH_REMIND).setSmallIcon(R.drawable.ic_stat_mic)
                .setContentTitle("Llamar a "+pretty(number)).setContentText("Es la hora de la llamada que programaste.")
                .setContentIntent(dial).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
                .addAction(new Notification.Action.Builder(null,"Llamar",dial).build())
                .addAction(new Notification.Action.Builder(null,"Posponer 10 min",snooze).build()).build();
        c.getSystemService(NotificationManager.class).notify(nid,n);
    }
    static void snooze(Context c,String id,String number){
        c.getSystemService(NotificationManager.class).cancel(id.hashCode());
        schedule(c,number,System.currentTimeMillis()+10*60000L);
    }
    static void notifyMissed(Context c,Missed m){
        channels(c);int nid=("m"+key(m.number)).hashCode();
        String who=m.name!=null&&!m.name.isEmpty()?m.name:pretty(m.number);
        Notification n=new Notification.Builder(c,CH_MISSED).setSmallIcon(R.drawable.ic_stat_mic)
                .setContentTitle(m.count>1?m.count+" llamadas perdidas de "+who:"Llamada perdida de "+who)
                .setContentText(ago(m.last)+" · pulsa para devolverla").setWhen(m.last).setShowWhen(true)
                .setContentIntent(dial(c,m.number,nid)).setAutoCancel(true).setCategory(Notification.CATEGORY_MISSED_CALL)
                .addAction(new Notification.Action.Builder(null,"Llamar",dial(c,m.number,nid)).build())
                .addAction(new Notification.Action.Builder(null,"Programar",scheduleScreen(c,m.number,nid+1)).build()).build();
        c.getSystemService(NotificationManager.class).notify(nid,n);
    }
    /** Avisa de las perdidas nuevas desde la última vez que se miró. */
    static void checkNewMissed(Context c){
        SharedPreferences p=Ui.prefs(c);
        long last=p.getLong(P_LAST,0);long now=System.currentTimeMillis();
        p.edit().putLong(P_LAST,now).apply();
        if(!notifyEnabled(c)||last==0)return;
        for(Missed m:missed(c))if(m.last>last)notifyMissed(c,m);
    }
    static void markSeenNow(Context c){Ui.prefs(c).edit().putLong(P_LAST,System.currentTimeMillis()).apply();}

    // ------------------------------------------------------------ Diálogo de programación
    static String label(long t){return new SimpleDateFormat("EEE d MMM, HH:mm",ES).format(new Date(t));}

    /** Pregunta cuándo y programa un aviso para llamar a ese número. */
    static void showScheduleDialog(Activity a,String number,Runnable done){showScheduleDialog(a,number,done,null);}
    /** done: se programó. end: el flujo terminó (programada o cancelada). */
    static void showScheduleDialog(final Activity a,final String number,final Runnable done,final Runnable end){
        final long now=System.currentTimeMillis();
        final List<String> names=new ArrayList<>();final List<Long> times=new ArrayList<>();
        names.add("En 15 minutos");times.add(now+15*60000L);
        names.add("En 1 hora");times.add(now+60*60000L);
        Calendar eve=Calendar.getInstance();eve.set(Calendar.HOUR_OF_DAY,18);eve.set(Calendar.MINUTE,0);eve.set(Calendar.SECOND,0);
        if(eve.getTimeInMillis()>now+30*60000L){names.add("Esta tarde, a las 18:00");times.add(eve.getTimeInMillis());}
        Calendar tm=Calendar.getInstance();tm.add(Calendar.DAY_OF_YEAR,1);tm.set(Calendar.HOUR_OF_DAY,9);tm.set(Calendar.MINUTE,0);tm.set(Calendar.SECOND,0);
        names.add("Mañana, a las 9:00");times.add(tm.getTimeInMillis());
        names.add("Elegir día y hora…");times.add(-1L);
        new AlertDialog.Builder(a).setTitle("Programar llamada a "+pretty(number))
            .setItems(names.toArray(new String[0]),(d,which)->{
                long t=times.get(which);
                if(t>0)finish(a,number,t,done,end);else pick(a,number,done,end);
            }).setNegativeButton("Cancelar",(d,w)->{if(end!=null)end.run();})
            .setOnCancelListener(d->{if(end!=null)end.run();}).show();
    }
    private static void pick(final Activity a,final String number,final Runnable done,final Runnable end){
        final Calendar c=Calendar.getInstance();c.add(Calendar.HOUR_OF_DAY,1);
        android.app.DatePickerDialog dp=new android.app.DatePickerDialog(a,(v,y,m,d)->{
            android.app.TimePickerDialog tp=new android.app.TimePickerDialog(a,(tv,h,min)->{
                Calendar r=Calendar.getInstance();r.set(y,m,d,h,min,0);r.set(Calendar.MILLISECOND,0);
                if(r.getTimeInMillis()<=System.currentTimeMillis()){Toast.makeText(a,"Elige una hora que aún no haya pasado.",Toast.LENGTH_LONG).show();if(end!=null)end.run();return;}
                finish(a,number,r.getTimeInMillis(),done,end);
            },c.get(Calendar.HOUR_OF_DAY),0,true);
            tp.setOnCancelListener(x->{if(end!=null)end.run();});tp.show();
        },c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH));
        dp.setOnCancelListener(x->{if(end!=null)end.run();});
        dp.getDatePicker().setMinDate(System.currentTimeMillis()-1000);dp.show();
    }
    private static void finish(Activity a,String number,long at,Runnable done,Runnable end){
        schedule(a,number,at);
        Toast.makeText(a,"Te avisaré para llamar a "+pretty(number)+" · "+label(at),Toast.LENGTH_LONG).show();
        if(android.os.Build.VERSION.SDK_INT>=33&&a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},REQ_NOTI);
        if(done!=null)done.run();
        if(end!=null)end.run();
    }
}
