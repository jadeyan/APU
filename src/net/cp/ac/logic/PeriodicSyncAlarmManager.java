package net.cp.ac.logic;

import java.util.Calendar;

import net.cp.ac.core.AppLogger;
import net.cp.ac.core.SyncBroadcastReceiver;
import net.cp.ac.core.SyncEngineService;
import net.cp.engine.EngineSettings;
import net.cp.syncml.client.util.Logger;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class PeriodicSyncAlarmManager {

    private static PeriodicSyncAlarmManager instance;

    /**
     * The consumer of the PIS information
     */
    protected SyncEngineService consumer;

    public static final String ALARM_INTENT = "critical_path.intent.action.ACTION_ALARM";

    private PeriodicSyncAlarmManager(SyncEngineService consumer) {
        this.consumer = consumer;
    }

    public static PeriodicSyncAlarmManager init(SyncEngineService consumer) {
        if (instance == null) instance = new PeriodicSyncAlarmManager(consumer);
        return instance;
    }

    public static PeriodicSyncAlarmManager getInstance() {
        return instance;
    }

    /**
     * @param context
     * @return
     */
    public boolean startAlarm(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        EngineSettings settings = EngineSettings.getInstance();
        
        if (settings.periodicAllowed) {
            Logger logger = AppLogger.getEngineInstance();
            if (logger != null) {
                logger.debug("About to start alarm at hour: " + settings.periodicSyncHourOfDay + " and minute: " + settings.periodicSyncMinute);
            }
        
            int period = settings.periodicSyncDaysLimits;
            Intent in = new Intent(context, SyncBroadcastReceiver.class);
            in.setAction(ALARM_INTENT);
            in.addCategory("critical_path");
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);// PendingIntent.FLAG_UPDATE_CURRENT
            
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, settings.periodicSyncHourOfDay);
            calendar.set(Calendar.MINUTE, settings.periodicSyncMinute);
            calendar.set(Calendar.SECOND, 0);   
            
            long timeToAlarm = calendar.getTimeInMillis(); 
            if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
                timeToAlarm += (24 * 60 * 60 * 1000);
            } 

            alarm.setRepeating(AlarmManager.RTC_WAKEUP, timeToAlarm, period * 24 * 60 * 60 * 1000, pendingIntent);
            
            return true;
        }
        
        return false;
    }

    /**
     * record current time as time of last periodic sync, trigger new sync
     */
    public void onConsume() {
//        EngineSettings settings = EngineSettings.getInstance();
//        Calendar calendar = Calendar.getInstance();
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        calendar.set(Calendar.HOUR_OF_DAY, settings.periodicSyncHourOfDay);
//        calendar.set(Calendar.MINUTE, settings.periodicSyncMinute);
//        settings.lastPeriodicSyncTime = calendar.getTimeInMillis();
//        settings.writeAllSettings();
        if (consumer != null) {
            consumer.onPeriodicSync();
        }

    }

    public void onSyncEnd() {
        try {
            // if(wakeLock!=null)
            // wakeLock.release();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * @param context
     */
    public void cancelAlarm(Context context) {
        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent in = new Intent(context, SyncBroadcastReceiver.class);
        in.setAction(ALARM_INTENT);
        in.addCategory("critical_path");
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, in, PendingIntent.FLAG_NO_CREATE);// PendingIntent.FLAG_NO_CREATE
        alarm.cancel(pendingIntent);
    }

    /**
     * @param context
     *            The context to use to start the service
     */
    public static void startSyncService(Context context) {
        ComponentName name = new ComponentName(context.getPackageName(), SyncEngineService.class.getName());
        ComponentName service = context.startService(new Intent().setComponent(name));
        if (service == null) {
            Logger logger = AppLogger.getEngineInstance();
            if (logger != null) logger.error("PeriodicSyncAlarmManager: Could not start sync service " + name.toString());
        }
    }
}
