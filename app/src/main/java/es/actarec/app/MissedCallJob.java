package es.actarec.app;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.provider.CallLog;

/** Se despierta cuando cambia el registro de llamadas y avisa de las perdidas nuevas. */
public class MissedCallJob extends JobService {
    private static final int ID=7301;

    static void schedule(Context c){
        JobScheduler js=c.getSystemService(JobScheduler.class);if(js==null)return;
        JobInfo.Builder b=new JobInfo.Builder(ID,new ComponentName(c,MissedCallJob.class));
        b.addTriggerContentUri(new JobInfo.TriggerContentUri(CallLog.Calls.CONTENT_URI,JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS));
        b.setTriggerContentUpdateDelay(1500);b.setTriggerContentMaxDelay(8000);
        js.schedule(b.build());
    }
    static void cancel(Context c){JobScheduler js=c.getSystemService(JobScheduler.class);if(js!=null)js.cancel(ID);}

    @Override public boolean onStartJob(final JobParameters p){
        new Thread(()->{
            try{Calls.checkNewMissed(this);}catch(Exception ignored){}
            if(Calls.notifyEnabled(this))schedule(this);
            jobFinished(p,false);
        }).start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters p){return false;}
}
