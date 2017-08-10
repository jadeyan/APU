/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.test.logic;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import net.cp.ac.core.AndroidConnectionState;
import net.cp.ac.core.BusinessLogic;
import net.cp.ac.core.SisHandler;
import net.cp.ac.core.SyncEngineService;
import net.cp.ac.logic.PendingSync;
import net.cp.ac.logic.PeriodicSyncAlarmManager;
import net.cp.ac.test.ui.LoginActivity;
import net.cp.ac.test.ui.MainActivity;
import net.cp.ac.test.ui.SyncActivity;
import net.cp.engine.ConnectionState;
import net.cp.engine.EngineSettings;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncError;
import net.cp.engine.SyncLog;
import net.cp.engine.SyncProgress;
import net.cp.engine.UIInterface;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.ServerAlert;
import net.cp.syncml.client.SyncException;
import net.cp.syncml.client.util.*;

import net.cp.ac.R;

/**
 * This class defines the standard business logic for this client. It decides what to do with server alerts and CIS alerts. It makes decisions based on the
 * config and user settings. It takes care of queueing sync requests, and starting the UI if the user needs to be prompted. This class should be extended or
 * replaced to change how the business logic works.
 */
public class DefaultBusinessLogic implements BusinessLogic, UIInterface {
    /**
     * This is used to start sync sessions and send messages to the UI
     */
    protected SyncEngineService service;

    /**
     * The logger to use
     */
    protected Logger logger;

    /**
     * This is used to determine the connection state. Used in combination with the settings to decide if/how to sync.
     */
    protected AndroidConnectionState connectionState;

    /**
     * Used to "debounce" CIS alerts. We will not attempt to sync until this timer has expired.
     */
    protected Timer cisTimer;

    /**
     * This task will be executed when the cisTimer expires
     */
    protected TimerTask cisTask;

    /**
     * Timer used to resume suspended sessions
     */
    protected Timer resumeTimer;

    /**
     * This task will be executed when the resumeTimer expires
     */
    protected TimerTask resumeTask;

    /**
     * Timer used to abort suspended sessions
     */
    protected Timer abortTimer;

    /**
     * This task will be executed when the abortTimer expires
     */
    protected TimerTask abortTask;

    /**
     * Counts how many times we've already tried to resume.
     */
    protected int resumeCount;

    /**
     * This is a queue of sync sessions that need to be run.
     */
    protected Vector<PendingSync> pendingSyncs;

    /**
     * These settings are used to make business logic decisions.
     */
    protected EngineSettings settings;

    /**
     * Used to show an icon on the taskbar when we are syncing in the background
     */
    protected NotificationManager notificationManager;

    /**
     * Used to distinguish between different notifications.
     */
    protected int SYNC_NOTIFICATION_ID = 1;

    protected int SIS_NOTIFICATION_ID = 2;

    protected int CIS_NOTIFICATION_ID = 3;

    /**
     * This String will be displayed in the status bar beside the icon when syncing
     */
    protected String syncNotificationMessage;

    /**
     * This String will be displayed in the status bar beside the icon when a SIS has been received
     */
    protected String sisNotificationMessage;

    /**
     * This String will be displayed in the status bar beside the icon when a CIS has been received
     */
    protected String cisNotificationMessage;

    /**
     * This String will be displayed in the status bar beside the icon when a CIS has been received and no password has been set
     */
    protected String passwordNotificationMessage;

    /**
     * {@inheritDoc} Registers with the SyncEngineService to receive server alerts and CIS alerts. Adds itself as a "local UI" to the service to receive
     * feedback on any sync in progress.
     */
    public void init(SyncEngineService service, Logger logger) {
        this.service = service;
        this.logger = logger;

        pendingSyncs = new Vector<PendingSync>(0);

        settings = EngineSettings.getInstance();

        service.addLocalUI(this);

        service.setServerAlertConsumer(this);

        service.setCISConsumer(this);

        service.setPeriodConsumer(this);

        /**
         * we can safely assume that the connection state was initialized by the sync service
         */
        connectionState = AndroidConnectionState.getInstance();

        Resources resources = service.getApplication().getResources();

        String appName = resources.getString(R.string.app_name);

        syncNotificationMessage = appName + ": " + resources.getString(R.string.syncing);
        sisNotificationMessage = appName + ": " + resources.getString(R.string.sis_received);
        cisNotificationMessage = appName + ": " + resources.getString(R.string.cis_received);
        passwordNotificationMessage = appName + ": " + resources.getString(R.string.cis_no_password);
    }

    /**
     * Not implemented. Returns StatusCodes.NONE .
     */
    public int getSyncState() {
        return StatusCodes.NONE;
    }

    /**
     * Not implemented.
     */
    public void onGetChangesEnd() {

    }

    /**
     * Not implemented.
     */
    public void onSyncError() {

    }

    /**
     * {@inheritDoc} This implementation checks if any syncable items were changed during the sync, and if so, triggers another sync (subject to settings). If
     * there are pending syncs (CIS, SIS etc...) the next one in the queue is started. It spawns a thread to do the checking, so the methods is non-blocking
     */
    public void onSyncEnd() {
        // cancel any abort timer
        if (abortTimer != null) {
            abortTimer.cancel();
            abortTimer = null;
        }

        // cancel any resume timer
        if (resumeTimer != null) {
            resumeTimer.cancel();
            resumeTimer = null;
        }

        resumeCount = 0;

        if (notificationManager != null) {
            notificationManager.cancel(SYNC_NOTIFICATION_ID);
            notificationManager.cancel(SIS_NOTIFICATION_ID);
            notificationManager.cancel(CIS_NOTIFICATION_ID);
            notificationManager = null;
        }

        // now that the sync is over, see if we have to sync anything else
        // Only required if CIS is enabled!

        if (settings.isCisAllowed()) {
            new Thread() {

                public void run() {
                    SyncError lastStatus = SyncLog.getLastStatus();

                    // only do this if last sync was successful
                    if (lastStatus.errorCode != StatusCodes.SYNC_SUCCESS) return;

                    // now, check if the user changed any contacts while we were syncing.
                    // if they did, start the timer to queue a CIS
                    if (settings.isFlagSet(settings.syncAllowedMediaTypes, EngineSettings.MEDIA_TYPE_CONTACTS)) {
                        /*
                         * We don't save the changelogs, as they are likely to be empty, and inaccurate for the next manual sync
                         */
                        int numChanges = service.getNumberChangedContacts(false);

                        if (numChanges > 0) onCISIntent(EngineSettings.MEDIA_TYPE_CONTACTS, numChanges);
                    }

                    // process the next sync (if one exists)
                    processPendingSync();
                }
            }.start();
        }

        if (logger != null) logger.info("Sync end, release wake lock");
        PeriodicSyncAlarmManager.getInstance().onSyncEnd();
    }

    /**
     * This method will check for a pending sync. If there is a pending sync, it will check the connection state. It will then act according to the sync mode
     * for that connection state.<br/>
     * The sync mode may be:<br/>
     * <br/>
     * - Automatic (immediately start a sync)<br/>
     * - Remind Me (tell the user it's time to sync, give them the option to proceed or cancel)<br/>
     * - Off (do nothing)<br/>
     */
    protected void processPendingSync() {
        if (logger != null) logger.info("processPendingSync");

        // checking the vector size is cheaper than checking the connection type,
        // so always do this first!
        if (pendingSyncs.size() > 0) {
            int connectionType = getSyncConnectionType();

            switch (connectionType) {
            case ConnectionState.CONNECTION_TYPE_ANY:
                sync(EngineSettings.SYNC_MODE_AUTO, connectionType);
                break;
            case ConnectionState.CONNECTION_TYPE_NO_COST:
                sync(settings.syncModeNoCost, connectionType);
                break;
            case ConnectionState.CONNECTION_TYPE_NORMAL_COST:
                sync(settings.syncModeNormalCost, connectionType);
                break;
            case ConnectionState.CONNECTION_TYPE_HIGH_COST:
                sync(settings.syncModeHighCost, connectionType);
                break;
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.ac.core.BusinessLogic#getSyncConnectionType()
     */
    public int getSyncConnectionType() {
        // major time saver this!
        if (settings.ignoreSyncModes) return ConnectionState.CONNECTION_TYPE_ANY;

        int connections = connectionState.getConnectionState();

        // try no cost sync first
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NO_COST)) {
            if (settings.syncModeNoCost != EngineSettings.SYNC_MODE_OFF) return ConnectionState.CONNECTION_TYPE_NO_COST;

        }

        // try normal cost sync
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NORMAL_COST)) {
            if (settings.syncModeNormalCost != EngineSettings.SYNC_MODE_OFF) return ConnectionState.CONNECTION_TYPE_NORMAL_COST;
        }

        // try high cost sync
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_HIGH_COST)) {
            if (settings.syncModeHighCost != EngineSettings.SYNC_MODE_OFF) return ConnectionState.CONNECTION_TYPE_HIGH_COST;

        }

        // not connected
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NOT_CONNECTED)) {
            if (logger != null) logger.info("not connected!");

            return ConnectionState.CONNECTION_TYPE_NONE;
        }

        if (logger != null) logger.info("connection state unknown!");

        return ConnectionState.CONNECTION_TYPE_NONE;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#updateSyncProgress(net.cp.engine.SyncProgress)
     */
    public void updateSyncProgress(SyncProgress progress) {
        try {
            // Get the notification manager service.
            if (notificationManager == null && service.syncInProgress()) {
                notificationManager = (NotificationManager) service.getSystemService(SyncEngineService.NOTIFICATION_SERVICE);
                showNotification();
            }
        }

        catch (Exception e) {
            if (logger != null) logger.error("Error showing notification icon", e);
        }

        if (progress.statusHeading == StatusCodes.SYNC_SUSPENDED) {
            if (logger != null) logger.info("DefaultBusinessLogic: sync suspended");

            // now, deal with a unintended suspend
            if (progress.statusDetail == StatusCodes.SYNC_SUDDENLY_SUSPENDED) {
                // reset the timer if it exists
                if (resumeTimer != null) resumeTimer.cancel();

                if (resumeCount < settings.resumeRetryCount) {
                    if (logger != null) logger.info("DefaultBusinessLogic: setting resume timer");

                    resumeTimer = new Timer();
                    resumeTask = new TimerTask() {
                        public void run() {

                            if (logger != null) logger.info("DefaultBusinessLogic: resuming due to timeout");

                            service.resumeSync();
                        }
                    };
                    resumeTimer.schedule(resumeTask, settings.resumeRetryTimeout * 1000);

                    resumeCount++;
                }
            }

            // no matter how we suspended, abort after a certain amount of time
            // reset the timer if it exists
            if (abortTimer == null) {
                if (logger != null) logger.info("DefaultBusinessLogic: setting abort timer");

                abortTimer = new Timer();
                abortTask = new TimerTask() {
                    public void run() {

                        if (logger != null) logger.info("DefaultBusinessLogic: aborting due to timeout");
                        service.abortSync();

                    }
                };
                abortTimer.schedule(abortTask, settings.abortTimeout * 1000);
            }
        }

        else if (service.getSyncState() == StatusCodes.SYNC_IN_PROGRESS) {
            if (abortTimer != null) {
                abortTimer.cancel();
                abortTimer = null;
            }

            if (resumeTimer != null) {
                resumeTimer.cancel();
                resumeTimer = null;
            }

            resumeCount = 0;
        }
    }

    /**
     * Start a sync session, subject to the specified sync mode.
     * 
     * @param syncMode
     *            The sync mode may be:<br/>
     * <br/>
     *            - Automatic (immediately start a sync)<br/>
     *            - Remind Me (tell the user it's time to sync, give them the option to proceed or cancel)<br/>
     *            - Off (do nothing)<br/>
     * @param connectionType
     *            The type of connection to use for this sync session.<br>
     *            E.g. ConnectionState.CONNECTION_TYPE_NO_COST
     */
    protected void sync(int syncMode, int connectionType) {
        synchronized (pendingSyncs) {
            if (pendingSyncs.size() > 0 && !service.syncInProgress()) {
                PendingSync sync = pendingSyncs.remove(0);

                // check if user has disabled background data usage
                boolean backgroundAllowed = connectionState.backgroundDataAllowed();

                // sync automatically
                if (syncMode == EngineSettings.SYNC_MODE_AUTO && backgroundAllowed) {
                    int batteryLevel = service.getBatteryPercent();
                    if (batteryLevel < settings.minBatteryLevel) {
                        if (logger != null) logger.info("BusinessLogic: Cannot sync due to low battery: " + batteryLevel + "%");

                        return;
                    }
                    service.startSync(sync.mediaTypes, connectionType);
                }

                // inform the UI, let it decide what to do
                else if ((syncMode == EngineSettings.SYNC_MODE_AUTO && !backgroundAllowed) || syncMode == EngineSettings.SYNC_MODE_REMIND) {
                    if (sync.origin == PendingSync.ORIGIN_CIS) {
                        if (!service.isUIRegisteredForCIS()) startUI();

                        service.sendUICIS(sync.mediaTypes, sync.cisNumChanges);
                    }

                    if (sync.origin == PendingSync.ORIGIN_SIS) {
                        if (!service.isUIRegisteredForSIS()) startUI();

                        service.sendUIServerAlert(sync.sisData);
                    }

                    if (sync.origin == PendingSync.ORIGIN_PERIOD) {

                    }
                }

                // turned off, do nothing
                else {
                    if (logger != null) logger.info("Ignoring call to sync, sync mode is: " + syncMode);
                }
            }
        }
    }

    /**
     * Starts the main sync UI activity . Generally, UI activities should only be started in response to a user action. If feedback is required for an automated
     * process, a notification should be used instead.
     */
    protected void startUI() {
        if (logger != null) logger.info("Starting UI " + MainActivity.class.getName());

        Context context = service.getApplication();

        Intent uiIntent = new Intent(context, MainActivity.class);
        uiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (uiIntent != null) {
            if (logger != null) logger.info("Starting UI activity");

            context.startActivity(uiIntent);
        }
    }

    /**
     * {@inheritDoc} Queue's any syncs contained in the server alert, then starts the next sync on the queue.
     */
    public void serverAlertReceived(byte[] data) {
        try {
            ServerAlert alert = ServerAlert.parse(data);

            if (logger != null) logger.info("DefaultBusinessLogic: Server alert received");

            // check if server alerted sync is allowed
            if (!settings.contactSisAllowed || !settings.sisEnable) {
                if (logger != null) logger.info("Server alerted sync is disabled - ignoring alert");

                return;
            }

            // a null indicates that the alert is invalid
            if (alert == null) {
                if (logger != null) logger.error("Server alert is not valid - ignoring alert");

                return;
            }

            // check if we are already syncing, suspending etc...
            if (getSyncState() != StatusCodes.NONE) {
                if (logger != null) logger.info("Server alert received, but sync in progress - ignoring alert");

                return;
            }

            // check if server alerted sync is allowed during the current sync mode
            int syncMode = getSyncModeFromConnection();

            if (syncMode == EngineSettings.SYNC_MODE_OFF) {
                if (logger != null) logger.error("Server alert received, but sync mode is set to off - ignoring alert");

                return;
            }

            // examine the list of stores specified in the alert to determine which media types should be synced,
            // then add them to the list
            ServerAlert.Store[] syncStores = alert.getSyncStores();
            if ((syncStores != null) && (syncStores.length > 0)) {
                for (int i = 0; i < syncStores.length; i++) {
                    String storeUri = syncStores[i].getServerUri();
                    if ((storeUri == null) || (storeUri.length() <= 0)) continue;

                    if ((storeUri.equalsIgnoreCase(settings.contactStoreServerUri)) && (settings.isAllowContactSync())) {
                        // If the syncMode is AUTO, just create a pending sync
                        if (syncMode == EngineSettings.SYNC_MODE_AUTO) {
                            PendingSync sync = new PendingSync();
                            sync.origin = PendingSync.ORIGIN_SIS;
                            sync.sisData = data;

                            // queue up a sync of the appropriate media type
                            synchronized (pendingSyncs) {
                                sync.mediaTypes = EngineSettings.MEDIA_TYPE_CONTACTS;
                                pendingSyncs.add(sync);
                            }

                            processPendingSync();
                        } else {
                            // The syncMode is REMIND so send the user a notification
                            if (logger != null) logger.error("Server alert received and sync mode set to remind.  Sending notification to user");

                            showRemindSyncNotification(EngineSettings.SYNC_TYPE_SIS, sisNotificationMessage, SIS_NOTIFICATION_ID);
                        }
                    } else {
                        if (logger != null) logger.info("Unknown data store URI '" + storeUri + "' in server alert - ignoring data store");
                    }
                }
            }

            String vendorText = SisHandler.getVendorText(alert);

            if (logger != null) logger.debug("SIS Vendor Text: " + vendorText);

        } catch (SyncException e) {
            if (logger != null) logger.error("Error parsing server alert data", e);
        }
    }

    /**
     * Called when changes have happened and the timer has expired. May calculate the number of items that changed, then calls: public void onItemsChanged(int
     * mediaType, int numberOfChanges)
     * 
     * @param mediaType
     *            The type of media that has changed
     */
    protected void onItemsChanged(int mediaType) {
        // check if client initiated alerted sync is allowed
        if (!settings.contactCisAllowed) {
            if (logger != null) logger.info("CIS alert received, but not allowed - ignoring.");

            return;
        }

        int numChanges = -1;

        try {
            int batteryLevel = service.getBatteryPercent();
            if (batteryLevel < settings.minBatteryLevel) {
                if (logger != null) logger.info("BusinessLogic: Cannot check contacts due to low battery: " + batteryLevel + "%");

                return;
            }

            // Check how many changes there were.
            if (mediaType == EngineSettings.MEDIA_TYPE_CONTACTS) numChanges = service.getNumberChangedContacts(false);// do not use this number, it would be
                                                                                                                      // incorrect
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to get number of item changes", e);
        }

        // Have enough contacts have changed?
        if (settings.contactMinSyncLimit > 0 && numChanges < settings.contactMinSyncLimit) {
            if (logger != null) {
                logger.info("Not enough contacts have changed -  minimum changes before sync is " + settings.contactMinSyncLimit);
            }

            return;
        }

        // check if client alerted sync is allowed during the current sync mode
        int syncMode = getSyncModeFromConnection();

        if (syncMode != EngineSettings.SYNC_MODE_OFF && (settings.userName.length() == 0 || settings.userPassword.length() == 0)) {
            if (logger != null) logger.debug("Client initiates sync alert received, but no password set.  Sending notification to user");

            showNoPasswordNotification();

            return;
        }

        if (syncMode == EngineSettings.SYNC_MODE_OFF) {
            if (logger != null) logger.info("Client initiated sync alert received, but sync mode is set to off - ignoring alert");

            return;
        } else if (syncMode == EngineSettings.SYNC_MODE_REMIND) {
            // The syncMode is REMIND so send the user a notification
            if (logger != null) logger.error("Client initiated sync alert received and sync mode set to remind.  Sending notification to user");

            showRemindSyncNotification(EngineSettings.SYNC_TYPE_CIS, cisNotificationMessage, CIS_NOTIFICATION_ID);
        } else {
            // SyncMode is AUTO, so just process it
            try {
                onItemsChanged(mediaType, numChanges);
            } catch (Throwable e) {
                if (logger != null) logger.error("onItemsChanged(mediaType, numChanges) failed", e);
            }
        }
    }

    /**
     * {@inheritDoc} Starts a timer, to allow for more changes. If the timer already exists, it is reset. When the timer expires, onItemsChanged(int mediaType)
     * is called.
     */
    public void onCISIntent(int mediaType) {
        // this change is likely caused by the current sync. Ignore it.
        if (service.syncInProgress()) return;

        // reset the timer, waiting for more changes
        if (cisTimer != null) cisTimer.cancel();

        cisTimer = new Timer();

        final int mType = mediaType;
        cisTask = new TimerTask() {
            public void run() {
                onItemsChanged(mType);
            }
        };
        cisTimer.schedule(cisTask, settings.changeTimeout);
    }

    /**
     * Starts a timer, to allow for more changes. If the timer already exists, it is reset. When the timer expires, onItemsChanged(int mediaType) is called.
     * 
     * @param mediaType
     *            The type of items that have changed
     * @param numChanges
     *            the number of changes detected
     */
    protected void onCISIntent(int mediaType, int numChanges) {
        // this change is likely caused by the current sync. Ignore it.
        if (service.syncInProgress()) return;

        // reset the timer, waiting for more changes
        if (cisTimer != null) cisTimer.cancel();

        cisTimer = new Timer();

        final int mType = mediaType;
        final int nChanges = numChanges;
        cisTask = new TimerTask() {
            public void run() {
                onItemsChanged(mType, nChanges);
            }
        };
        cisTimer.schedule(cisTask, settings.changeTimeout);
    }

    /**
     * {@inheritDoc} Acts according to various settings. Queue's a sync, then starts the next sync on the queue.
     */
    public void onItemsChanged(int mediaType, int numberOfChanges) {
        if (logger != null) logger.info("DefaultBusinessLogic: onItemsChanged() called. mediaType: " + mediaType + " numberOfChanges: " + numberOfChanges);

        // check if we are already syncing
        if (service.syncInProgress()) {
            if (logger != null) logger.info("onItemsChanged: sync in progress, returning");

            return;
        }

        // we are ready to sync
        synchronized (pendingSyncs) {
            PendingSync sync = new PendingSync();

            sync.mediaTypes = mediaType;
            sync.cisNumChanges = numberOfChanges;
            sync.origin = PendingSync.ORIGIN_CIS;

            pendingSyncs.add(sync);
        }

        processPendingSync();
    }

    public int getSyncModeFromConnection() {
        int connections = connectionState.getConnectionState();

        // try no cost sync first
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NO_COST)) return settings.syncModeNoCost;

        // try normal cost sync
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NORMAL_COST)) return settings.syncModeNormalCost;

        // try high cost sync
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_HIGH_COST)) return settings.syncModeHighCost;

        // not connected
        if (UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NOT_CONNECTED)) {
            if (logger != null) logger.info("not connected!");

            return EngineSettings.SYNC_MODE_OFF;
        }

        if (logger != null) logger.info("connection state unknown!");

        return EngineSettings.SYNC_MODE_OFF;
    }

    /**
     * Shows a notification in the taskbar
     */
    protected void showNotification() {
        Intent intent = new Intent(service.getApplicationContext(), SyncActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, 0);

        Notification notification = new Notification(R.drawable.ic_stat_sync, syncNotificationMessage, System.currentTimeMillis());

        notification.setLatestEventInfo(service, syncNotificationMessage, null, pendingIntent);
        notification.flags = Notification.FLAG_NO_CLEAR;
        notificationManager.notify(SYNC_NOTIFICATION_ID, notification);
    }

    protected void showRemindSyncNotification(byte syncType, String message, int notificationId) {
        Intent intent = new Intent(service.getApplication(), SyncActivity.class);
        Bundle bundle = new Bundle();

        bundle.putInt(EngineSettings.SYNC_TYPE, syncType);

        intent.putExtras(bundle);

        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification notification = new Notification(R.drawable.ic_stat_sync, message, System.currentTimeMillis());

        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;

        notification.setLatestEventInfo(service, message, null, pendingIntent);

        if (notificationManager == null) notificationManager = (NotificationManager) service.getSystemService(SyncEngineService.NOTIFICATION_SERVICE);

        notificationManager.notify(notificationId, notification);
    }

    protected void showNoPasswordNotification() {
        Intent intent = new Intent(service.getApplication(), LoginActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(service, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);

        Notification notification = new Notification(R.drawable.ic_stat_sync, passwordNotificationMessage, System.currentTimeMillis());

        notification.defaults |= Notification.DEFAULT_SOUND;
        notification.defaults |= Notification.DEFAULT_VIBRATE;
        notification.defaults |= Notification.DEFAULT_LIGHTS;

        notification.setLatestEventInfo(service, passwordNotificationMessage, null, pendingIntent);

        if (notificationManager == null) notificationManager = (NotificationManager) service.getSystemService(SyncEngineService.NOTIFICATION_SERVICE);

        notificationManager.notify(CIS_NOTIFICATION_ID, notification);
    }

    @Override
    public void onPeriodicSync() {
        // Log.e("DefaultBusinessLogic", "periodic sync onConsume");
        // this change is likely caused by the current sync. Ignore it.
        if (service.syncInProgress()) {
            PeriodicSyncAlarmManager.getInstance().onSyncEnd();
            return;
        }
        doSyncForPeriodic();
    }

    private void doSyncForPeriodic() {
        // Log.e("businessLogic", "periodic sync started");
        // check if client initiated alerted sync is allowed
        if (!settings.periodicAllowed) {
            if (logger != null) logger.info("It's time for periodic sync, but it's not allowed - ignoring.");
            PeriodicSyncAlarmManager.getInstance().onSyncEnd();
            return;
        }

        int batteryLevel = service.getBatteryPercent();
        if (batteryLevel < settings.minBatteryLevel) {
            if (logger != null) logger.info("BusinessLogic: Cannot check contacts due to low battery: " + batteryLevel + "%");
            PeriodicSyncAlarmManager.getInstance().onSyncEnd();
            return;
        }

        // check if client alerted sync is allowed during the current sync mode
        int syncMode = getSyncModeFromConnection();

        if (syncMode != EngineSettings.SYNC_MODE_OFF && (settings.userName.length() == 0 || settings.userPassword.length() == 0)) {
            if (logger != null) logger.debug("Client initiates sync alert received, but no password set.  Sending notification to user");

            showNoPasswordNotification();

            PeriodicSyncAlarmManager.getInstance().onSyncEnd();

            return;
        }

        if (syncMode == EngineSettings.SYNC_MODE_OFF) {
            if (logger != null) logger.info("Client initiated sync alert received, but sync mode is set to off - ignoring alert");

            PeriodicSyncAlarmManager.getInstance().onSyncEnd();

            return;
        } else if (syncMode == EngineSettings.SYNC_MODE_REMIND) {
            // The syncMode is REMIND so send the user a notification
            if (logger != null) logger.error("Client initiated sync alert received and sync mode set to remind.  Sending notification to user");

            showRemindSyncNotification(EngineSettings.SYNC_TYPE_CIS, cisNotificationMessage, CIS_NOTIFICATION_ID);

            PeriodicSyncAlarmManager.getInstance().onSyncEnd();
        } else {
            // SyncMode is AUTO, so just process it
            if (logger != null) logger.info("Time for perioidic sync");

            // check the state again if we are already for sync
            if (service.syncInProgress()) {
                if (logger != null) logger.info("Periodic Sync: sync in progress, returning");

                PeriodicSyncAlarmManager.getInstance().onSyncEnd();

                return;
            }

            // we are ready to sync
            synchronized (pendingSyncs) {
                PendingSync sync = new PendingSync();

                // media type default to contact
                sync.mediaTypes = EngineSettings.MEDIA_TYPE_CONTACTS;
                sync.origin = PendingSync.ORIGIN_PERIOD;

                pendingSyncs.add(sync);
            }

            processPendingSync();
        }
    }

    @Override
    public void onAlertSlowSync() {
        // TODO Auto-generated method stub
    }
}
