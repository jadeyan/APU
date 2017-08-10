/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.core;

import net.cp.ac.logic.PeriodicSyncAlarmManager;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.util.Logger;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

/**
 * This class is used to hook into the Android OS so that our service will be started at bootup. It is referenced in the project manifest file.
 * 
 * @see android.content.BroadcastReceiver
 */
public class SyncBroadcastReceiver extends BroadcastReceiver {
    /**
     * This is field is only necessary because the build system can remove this class from the final package unless it is referenced somewhere else in the
     * project. It serves no other purpose.
     */
    public static final int data = 0;

    private boolean mPendingSyncForConnection = false;
    /*
     * (non-Javadoc)
     * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
     */
    public void onReceive(Context context, Intent intent) {
        Logger logger = AppLogger.getEngineInstance();
            
        // Log.e("broadcastreceiver", "intent:"+intent.getAction());
        // we are booting up, start the service
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            startSyncService(context, intent);
        }

        // a new app has just been installed (maybe our own!), start the service
        if ("android.intent.action.ACTION_PACKAGE_ADDED".equals(intent.getAction())) {
            startSyncService(context, intent);
        }

        // the alarm triggered new periodic sync
        if (PeriodicSyncAlarmManager.ALARM_INTENT.equals(intent.getAction())) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
            wl.acquire();

            if (logger != null) logger.debug("Received broadcast: Periodic alarm, wake lock acquired..");
            
            if (UtilityClass.isNetworkAvailable(context)) {
                if (logger != null) {
                    logger.debug("Network available, about to perform auto sync.");
                }
                if (PeriodicSyncAlarmManager.getInstance() != null) {
                    PeriodicSyncAlarmManager.getInstance().onConsume();
                }
                
                mPendingSyncForConnection = false;
            } else {
                if (logger != null) {
                    logger.debug("Network not available, pending for the network change broadcast.");
                }
                mPendingSyncForConnection = true;
            }
            
            wl.release();
        }
        
        if ("android.net.conn.CONNECTIVITY_CHANGE".equals(intent.getAction())) {
            if (UtilityClass.isNetworkAvailable(context)) {
                if (mPendingSyncForConnection) {
                    if (logger != null) {
                        logger.debug("network available, about to perform pending sync.");
                    }
                    
                    if (PeriodicSyncAlarmManager.getInstance() != null) {
                        PeriodicSyncAlarmManager.getInstance().onConsume();
                    }
                    
                    mPendingSyncForConnection = false;
                }
            }
        }

        // TODO listen for other intents that tell us to start/stop a sync (e.g. SIS, power-down)
    }
    
    /**
     * @param context
     *            The context to use to start the service
     * @param intent
     *            The intent that triggered the service to be started
     */
    private void startSyncService(Context context, Intent intent) {
        ComponentName name = new ComponentName(context.getPackageName(), SyncEngineService.class.getName());
        ComponentName service = context.startService(new Intent().setComponent(name));
        if (service == null) {
            Logger logger = AppLogger.getEngineInstance();
            if (logger != null) logger.error("SyncBroadcastReceiver: Could not start sync service " + name.toString());
        }
    }

}
