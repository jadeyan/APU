/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.util.Vector;

import net.cp.ac.logic.PeriodicSyncAlarmManager;
import net.cp.ac.test.logic.DefaultBusinessLogic;
import net.cp.ac.ui.UICallbackInterface;
import net.cp.engine.ConnectionState;
import net.cp.engine.EngineSettings;
import net.cp.engine.MobileDevice;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncError;
import net.cp.engine.SyncLog;
import net.cp.engine.SyncProgress;
import net.cp.engine.UIInterface;
import net.cp.engine.contacts.ContactStore;
import net.cp.syncml.client.SyncException;
import net.cp.syncml.client.SyncListener;
import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.SyncManager;
import net.cp.syncml.client.store.RecordStore;
import net.cp.syncml.client.util.ConsumableStack;
import net.cp.syncml.client.util.Logger;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.DeadObjectException;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;

/**
 * This class is the main class that co-ordinates the sync engine, business logic, SIS, CIS, and accepts
 * RPC connections from any UIs.
 *
 * It runs constantly as a service on the Android device.
 *
 */
public class SyncEngineService extends Service implements UIInterface, SyncListener {
    private final SyncEngineBinder binder = new SyncEngineBinder(this);
    private EngineSettings settings;
    private Logger logger;
    private SyncManager syncManager;
    private Vector<UIInterface> localInterfaces = null;
    private Vector<UICallbackInterface> callbackInterfaces = null;
    private boolean abortingSync;
    private boolean suspendingSync;
    private boolean resumingSync;
    private boolean suspended;
    private boolean initialized;
    private AndroidPersistentStoreManager storeManager;
    private SyncProgress lastSyncProgress;
    private SyncProgress currentSyncProgress;
    private BusinessLogic businessLogic;
    private SisHandler sisHandler;
    private CisHandler cisHandler;
    private PeriodicSyncAlarmManager pisHandler;

    // SIS
    private UICallbackInterface remoteSisClient;
    private UIInterface localSisClient;

    // CIS
    private UICallbackInterface remoteCisClient;

    // Periodic sync
    private UICallbackInterface remotePeriodClient;

    /**
     * This field is necessary, because the build system can remove SyncBroadcastReceiver class
     * from the final package unless it is referenced somewhere else in the project
     */
    @SuppressWarnings("unused")
    private final int data = SyncBroadcastReceiver.data;

    /**
     * Used to disable UI feedback where appropriate
     */
    private boolean uiFeedbackDisabled;

    private ContactStore contactStore;

    private int percentBattery;

    private WakeLock wakeLock;

    private void initialize() {
        if (initialized) return;

        byte[] configData = null;
        contactStore = null;

        try {
            // load settings from config data
            configData = AndroidPersistentStoreManager.getConfigFromStore(getApplication(), logger);

            AndroidPersistentStoreManager.init(getApplication(), logger);
            storeManager = AndroidPersistentStoreManager.getInstance();
            settings = AndroidEngineSettings.init(configData, storeManager, getApplicationContext(), logger);

            // assume we have some battery unless told otherwise
            percentBattery = 101;
        }

        catch (Throwable e) {
            // no logger yet
            e.printStackTrace();
        }

        // initialise the logger if required
        AppLogger.initEngineLogger(settings, settings.logFilePath);
        logger = AppLogger.getEngineInstance();

        settings.setLogger(logger);

        if (logger != null) logger.info("Initializing Connection State");

        AndroidConnectionState.init(getApplication(), settings, logger);

        if (logger != null) logger.info("Initializing Business Logic");

        // #ifdef service.businessLogic.classname
        // #expand businessLogic = new %service.businessLogic.classname%();
        // #endif

        if (businessLogic == null) businessLogic = new DefaultBusinessLogic();
        businessLogic.init(this, logger);

        if (logger != null) logger.info("Initializing SIS");

        // register for server alerts
        if (SisHandler.getInstance() == null) SisHandler.init(this, this.getApplication(), settings, logger);

        sisHandler = SisHandler.getInstance();

        if (logger != null) logger.info("Initializing CIS");

        // register for contact DB changes
        if (CisHandler.getInstance() == null) CisHandler.init(this, this.getApplication(), logger);

        cisHandler = CisHandler.getInstance();

        // register alarm manger for PIS alerts
        pisHandler = PeriodicSyncAlarmManager.getInstance();
        if (pisHandler == null) pisHandler = PeriodicSyncAlarmManager.init(this);
        pisHandler.startAlarm(getApplication());

        initialized = true;

        if (logger != null) logger.info("initialize() complete. logger: " + logger.toString());
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0) {
        return binder;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onRebind(android.content.Intent)
     */
    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onUnbind(android.content.Intent)
     */
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        super.onCreate();

        this.initialize();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onStart(android.content.Intent, int)
     */
    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    /*
     * (non-Javadoc)
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        if (logger != null) AppLogger.close((AppLogger) logger);

        if (settings != null) {
            settings.close();
        }

        if (sisHandler != null) {
            sisHandler.unregister();
        }

        if (cisHandler != null) {
            cisHandler.unregister();
        }

        super.onDestroy();
    }

    /**
     * This method initiates a sync session for the supplied media types.
     * Only one session can run at a given time.
     *
     * @param syncMediaTypes The types of media to sync
     *
     * @return true if the sync was successfully started, otherwise false.
     */
    public boolean startSync(int syncMediaTypes) {
        try {
            int connectionType = businessLogic.getSyncConnectionType();

            if (connectionType == ConnectionState.CONNECTION_TYPE_NONE) throw new SyncException("Connection error", SyncML.STATUS_SERVICE_UNAVAILABLE);

            if (logger != null) logger.info("starting sync with connection type: " + connectionType);

            // unregister CIS, speed things up
            if (cisHandler != null) cisHandler.unregister();
            return startSync(syncMediaTypes, connectionType);
        }

        catch (SyncException e) {
            // determine the appropriate error message to display
            SyncError error = new SyncError();
            error.errorCode = StatusCodes.SYNC_ERROR_CONNECTION;

            error.syncMLStatusCode = e.getStatusCode();

            if (logger != null) logger.info("writing lastStatus from startSync");

            SyncLog.setLastStatus(error);

            currentSyncProgress.set(StatusCodes.SYNC_COMPLETE, StatusCodes.SYNC_ERROR_CONNECTION, 0, 0, null, -1, -1);

            updateSyncProgress(currentSyncProgress);
        }

        return false;
    }

    /**
     * This method initiates a sync session for the supplied media types.
     * Only one session can run at a given time.
     *
     * @param syncMediaTypes The types of media to sync
     *
     * @param connectionType The type of connection to use for this sync session.<br>
     * E.g. ConnectionState.CONNECTION_TYPE_NO_COST
     *
     * @return true if the sync was successfully started, otherwise false.
     */
    public boolean startSync(int syncMediaTypes, int connectionType) {
        PowerManager pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "net.cp.ac.logic.PeriodicSyncAlarm");
        wakeLock.acquire();
        if (!initialized) initialize();

        if (logger != null) {
            // We want the log to only contain content for 1 sync, so reset the log
            AppLogger.resetLog((AppLogger) logger);

            logger.info("begin startSync()");
        }

        currentSyncProgress = new SyncProgress(StatusCodes.SYNC_STARTING, StatusCodes.SYNC_INITIALIZING, -1, -1, null, -1, -1);

        try {
            if (syncManager == null || !syncManager.isSyncRunning()) {
                abortingSync = false;
                resumingSync = false;
                suspended = false;
                suspendingSync = false;

                lastSyncProgress = null;

                // make sure we display something at the start, as the sync thread can block
                updateSyncProgress(currentSyncProgress);

                // create the device object
                String appVersion = settings.appVersion;
                MobileDevice device = new MobileDevice(settings.deviceId, settings, appVersion);

                // create the HTTP transport
                HTTPTransport transport = new HTTPTransport(settings.httpSyncServerAddress, settings.httpSyncServerPort, settings.httpUseSSL,
                        settings.httpSyncServerPath, settings.httpMaxMessageSize);

                transport.setLogger(logger);
                transport.setConnectionTimeout(settings.httpConnectionTimeout);
                transport.setConnectionType(connectionType);

                // create the record stores to sync
                if (contactStore == null && settings.isFlagSet(syncMediaTypes, EngineSettings.MEDIA_TYPE_CONTACTS)) {
                    contactStore = new ContactStore(settings, this, logger);
                    AndroidContactList contactList = AndroidContactList.getInstance(contactStore, getContentResolver(), this, logger);
                    contactStore.initialize(contactList);
                }

                RecordStore[] stores = null;
                if (contactStore != null) {
                    stores = new RecordStore[] { contactStore };
                }

                else {
                    if (logger != null) logger.info("No stores to sync - aborting sync");

                    throw new SyncException("No stores to sync", SyncML.STATUS_PROCESSING_ERROR);

                }

                // create a new session ID
                String sessionID = Long.toString(System.currentTimeMillis());
                if (logger != null) logger.info("Created new session ID '" + sessionID + "' for user '" + settings.userName + "'");

                // determine the password to use
                String password = settings.userPassword;

                // create the sync manager and start the sync
                syncManager = new SyncManager(device, transport, settings.userName, password, this, logger);
                syncManager.startSync(stores, sessionID);

                if (logger != null) logger.info("end startSync()");

                return true;
            }
        }

        catch (Throwable e) {
            if (logger != null) logger.error("Failed to start the sync session", e);

            // cleanup
            syncManager = null;

            if (contactStore != null) contactStore.close();
            contactStore = null;

            // determine the appropriate error message to display
            SyncError error = new SyncError();
            error.errorCode = StatusCodes.SYNC_START_FAILED;

            if (e instanceof SyncException) error.syncMLStatusCode = ((SyncException) e).getStatusCode();

            if (logger != null) logger.info("writing lastStatus from startSync");

            SyncLog.setLastStatus(error);

            currentSyncProgress.set(StatusCodes.SYNC_COMPLETE, StatusCodes.SYNC_ERROR_START_FAILED, 0, 0, null, -1, -1);

            updateSyncProgress(currentSyncProgress);
        }

        return false;
    }

    /**
     * This method is used to identify what state the sync is in.
     * @return one of the following in order of decreasing precedance:
     *
     * StatusCodes.SYNC_ABORTING
     * StatusCodes.SYNC_SUSPENDING
     * StatusCodes.SYNC_RESUMING
     * StatusCodes.SYNC_SUSPENDED
     * StatusCodes.SYNC_IN_PROGRESS
     * StatusCodes.NONE
     *
     */
    @Override
    public int getSyncState() {
        if (abortingSync) return StatusCodes.SYNC_ABORTING;

        if (suspendingSync) return StatusCodes.SYNC_SUSPENDING;

        if (resumingSync) return StatusCodes.SYNC_RESUMING;

        if (suspended) return StatusCodes.SYNC_SUSPENDED;

        if (syncInProgress())
            return StatusCodes.SYNC_IN_PROGRESS;

        else
            return StatusCodes.NONE;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#isSyncStopping()
     */
    public boolean isSyncStopping() {
        return abortingSync;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#isSyncSuspending()
     */
    public boolean isSyncSuspending() {
        return suspendingSync;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#onGetChangesEnd()
     */
    @Override
    public void onGetChangesEnd() {
        try {
            // call local interfaces
            if (localInterfaces != null) {
                for (UIInterface uiInterface : localInterfaces)
                    uiInterface.onGetChangesEnd();
            }

            // call RPC interfaces
            if (callbackInterfaces != null && callbackInterfaces.size() > 0) {
                int size = callbackInterfaces.size();
                UICallbackInterface uiInterface = null;

                for (int i = size - 1; i >= 0; i--) {
                    uiInterface = callbackInterfaces.get(i);

                    if (uiInterface.asBinder().isBinderAlive())
                        uiInterface.onGetChangesEnd();

                    else
                        callbackInterfaces.remove(i); // remove the dead interface from the set!!!
                }
            }
        }

        catch (Throwable e) {
            if (logger != null) logger.error("error calling uiInterface method", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#onSyncError()
     */
    @Override
    public void onSyncError() {
        try {
            // call local interfaces
            if (localInterfaces != null) {
                for (UIInterface uiInterface : localInterfaces)
                    uiInterface.onSyncError();
            }

            // call RPC interfaces
            if (callbackInterfaces != null && callbackInterfaces.size() > 0) {
                int size = callbackInterfaces.size();
                UICallbackInterface uiInterface = null;

                for (int i = size - 1; i >= 0; i--) {
                    uiInterface = callbackInterfaces.get(i);

                    if (uiInterface.asBinder().isBinderAlive())
                        uiInterface.onSyncError();

                    else
                        callbackInterfaces.remove(i); // remove the dead interface from the set!!!
                }
            }
        }

        catch (Throwable e) {
            if (logger != null) logger.error("error calling uiInterface method", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#updateSyncProgress(net.cp.engine.SyncProgress)
     */
    @Override
    public void updateSyncProgress(SyncProgress progress) {
        if (uiFeedbackDisabled) return;

        // we need to modify progress to indicate aborting/suspending/resuming states
        if (abortingSync)
            progress.statusHeading = StatusCodes.SYNC_ABORTING;

        else if (suspendingSync)
            progress.statusHeading = StatusCodes.SYNC_SUSPENDING;

        else if (resumingSync) progress.statusHeading = StatusCodes.SYNC_RESUMING;

        lastSyncProgress = progress;

        try {
            // call local interfaces
            if (localInterfaces != null) {
                for (UIInterface uiInterface : localInterfaces)
                    uiInterface.updateSyncProgress(progress);
            }

            // call RPC interfaces
            if (callbackInterfaces != null && callbackInterfaces.size() > 0) {
                // make it ready to be sent across RPC (if necessary)
                ParcelableSyncProgress status = new ParcelableSyncProgress(progress);

                int size = callbackInterfaces.size();
                UICallbackInterface uiInterface = null;

                for (int i = size - 1; i >= 0; i--) {
                    uiInterface = callbackInterfaces.get(i);

                    if (uiInterface.asBinder().isBinderAlive()) {
                        try {
                            uiInterface.updateSyncProgress(status);
                        }

                        catch (DeadObjectException e) {
                            callbackInterfaces.remove(i); // remove the dead interface from the set!!!
                        }
                    }

                    else
                        callbackInterfaces.remove(i); // remove the dead interface from the set!!!
                }
            }
        }

        catch (Throwable e) {
            if (logger != null) logger.error("error calling uiInterface method", e);
        }

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onAddResult(int, java.lang.String)
     */
    @Override
    public void onAddResult(int statusCode, String statusData) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onCopyResult(int, java.lang.String)
     */
    @Override
    public void onCopyResult(int statusCode, String statusData) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onDeleteResult(int, java.lang.String)
     */
    @Override
    public void onDeleteResult(int statusCode, String statusData) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onMessageReceive()
     */
    @Override
    public void onMessageReceive() {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onMessageSend()
     */
    @Override
    public void onMessageSend() {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onMoveResult(int, java.lang.String)
     */
    @Override
    public void onMoveResult(int statusCode, String statusData) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onReplaceResult(int, java.lang.String)
     */
    @Override
    public void onReplaceResult(int statusCode, String statusData) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onSuspendResult(int, java.lang.String)
     */
    @Override
    public void onSuspendResult(int statusCode, String statusData) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onSyncEnd(boolean, int, java.lang.String)
     */
    @Override
    public void onSyncEnd(boolean success, int statusCode, String statusData) {
        // sync complete
        abortingSync = false;
        suspended = false;
        resumingSync = false;

        // TODO make sure this is the correct thing to do
        syncManager = null;

        lastSyncProgress = null; // we are not syncing anymore

        // make sure any existing contactStore is closed so we recalc the changes next time
        if (contactStore != null) {
            contactStore.close();
            contactStore = null;
        }

        // we're finished syncing, no need for temp store any more
        AndroidPersistentStoreManager.deleteTemporaryStore();

        // re-register for CIS, now that sync is over
        cisHandler.register();

        onSyncEnd();

        // try to return the memory ASAP
        // System.gc();

        if (logger != null) logger.info("onSyncEnd complete");
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onSyncResume(boolean)
     */
    @Override
    public void onSyncResume(boolean success) {
        if (logger != null) logger.info("Sync session resuming - success=" + success);

        // update sync state
        resumingSync = false;

        if (success) {
            suspendingSync = false;
            suspended = false;
        } else {
            // update the UI to indicate that the resume failed
            currentSyncProgress.set(StatusCodes.SYNC_SUSPENDED, StatusCodes.SYNC_SUDDENLY_SUSPENDED, 0, 0, null, 0, 0);
            updateSyncProgress(currentSyncProgress);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onSyncStart()
     */
    @Override
    public void onSyncStart() {}

    /*
     * (non-Javadoc)
     * @see net.cp.engine.UIInterface#onSyncEnd()
     */
    @Override
    public void onSyncEnd() {
        try {
            // call local interfaces
            if (localInterfaces != null) {
                for (UIInterface uiInterface : localInterfaces)
                    uiInterface.onSyncEnd();
            }

            // call RPC interfaces
            if (callbackInterfaces != null && callbackInterfaces.size() > 0) {
                int size = callbackInterfaces.size();
                UICallbackInterface uiInterface = null;

                for (int i = size - 1; i >= 0; i--) {
                    uiInterface = callbackInterfaces.get(i);

                    if (uiInterface.asBinder().isBinderAlive()) {
                        try {
                            uiInterface.onSyncEnd();
                        }

                        catch (DeadObjectException e) {
                            callbackInterfaces.remove(i); // remove the dead interface from the set!!!
                        }
                    }

                    else
                        callbackInterfaces.remove(i); // remove the dead interface from the set!!!
                }
            }
        }

        catch (Throwable e) {
            if (logger != null) logger.error("error calling uiInterface method", e);
        } finally {
            if (wakeLock != null) wakeLock.release();
        }
    }

    // TODO Important! provide API to check if we are suspending. Possibly update UI feedback for aborting also
    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onSyncSuspend()
     */
    @Override
    public void onSyncSuspend() {
        if (logger != null) logger.info("Sync session suspended");

        suspended = true;

        // check if the suspension was triggered by the user - if not, this is a unsolicited suspend which
        // is usually caused by a connection error
        if (!suspendingSync) {
            // if "Suspend/Resume" isn't allowed, or if there is no UI/businesslogic, we can only
            // stop the sync session at this point
            if ((!settings.suspendResumeAllowed)) {
                syncManager.stopSync(SyncML.STATUS_SERVICE_UNAVAILABLE, null);
                return;
            }

            // update the UI to indicate that there was a unsolicited suspend
            currentSyncProgress.set(StatusCodes.SYNC_SUSPENDED, StatusCodes.SYNC_SUDDENLY_SUSPENDED, 0, 0, null, 0, 0);
            updateSyncProgress(currentSyncProgress);
        }

        else // manual suspend
        {
            // indicate that we are no longer trying to suspend the sync
            suspendingSync = false;

            // update the UI to indicate that the session has been suspended
            currentSyncProgress.set(StatusCodes.SYNC_SUSPENDED, StatusCodes.SYNC_MANUALLY_SUSPENDED, 0, 0, null, 0, 0);
            updateSyncProgress(currentSyncProgress);
        }

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onDisplayRequest(byte[])
     */
    @Override
    public void onDisplayRequest(byte[] data) throws SyncException {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onCopyRequest(int)
     */
    @Override
    public void onCopyRequest(int statusCode) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onAddRequest(int)
     */
    @Override
    public void onAddRequest(int statusCode) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onMoveRequest(int)
     */
    @Override
    public void onMoveRequest(int statusCode) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onReplaceRequest(int)
     */
    @Override
    public void onReplaceRequest(int statusCode) {

    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.SyncListener#onDeleteRequest(int)
     */
    @Override
    public void onDeleteRequest(int statusCode) {

    }

    /**
     * Add a local observer of sync progress to the list.
     * The "UI" does not have to be a graphical/visible UI.
     *
     * @param uiInterface the observer
     */
    public void addLocalUI(UIInterface uiInterface) {
        if (localInterfaces == null) localInterfaces = new Vector<UIInterface>();

        localInterfaces.add(uiInterface);
    }

    /**
     * Add an RPC callback/remote observer of sync progress to the list.
     *
     * The "UI" does not have to be a graphical/visible UI.
     *
     * @param uiInterface the observer
     */
    public void addCallbackUI(UICallbackInterface uiInterface) {
        if (callbackInterfaces == null) callbackInterfaces = new Vector<UICallbackInterface>();

        callbackInterfaces.add(uiInterface);
    }

    /**
     * @return true if we are currently syncing, otherwise false.
     */
    public boolean syncInProgress() {
        if (logger != null) logger.debug("checking syncInProgress...");

        if (syncManager == null)
            return false;

        else
            return syncManager.isSyncRunning();
    }

    /**
     * Cause the current sync to stop
     */
    public synchronized void abortSync() {
        // nothing more to do if we are not syncing
        if (syncManager == null) return;

        if (logger != null) logger.info("Aborting the sync session");

        // nothing more to do if the session has already been aborted
        if (abortingSync == true) return;

        if (logger != null) logger.info("User has chosen to abort the sync session - starting task to abort the sync");

        // indicate that the session should stop
        abortingSync = true;

        currentSyncProgress.set(StatusCodes.SYNC_ABORTING, StatusCodes.NONE, 0, 0, null, -1, -1);

        // update the UI to indicate that we are aborting
        updateSyncProgress(currentSyncProgress);

        // stop the transport - we do to prevent it retrying dropped connections - this should make the
        // abort more responsive in those cases
        HTTPTransport transport = (HTTPTransport) syncManager.getTransport();
        transport.stopTransport();

        // stop the sync session
        syncManager.stopSync();
    }

    /**
     * Called to suspend the sync session in progress.
     * The session can be resumed for a certain period of time before it is aborted.
     */
    public synchronized void suspendSync() {
        // nothing more to do if the session is already being aborted/suspended/resumed (or is already suspended)
        if ((abortingSync) || (suspendingSync) || (resumingSync) || (syncManager.isSyncSuspended())) return;

        if (logger != null) logger.debug("User has chosen to suspend the sync session - suspending the sync session");

        // suspend the session
        suspendingSync = syncManager.suspendSync();
        if (suspendingSync) {
            // update the UI to indicate that we are suspending
            currentSyncProgress.set(StatusCodes.SYNC_SUSPENDING, StatusCodes.SYNC_MANUALLY_SUSPENDED, 0, 0, null, 0, 0);
            updateSyncProgress(currentSyncProgress);
        }
    }

    /**
     * Called to resume the suspended sync session.
     */
    public synchronized void resumeSync() {
        // nothing more to do if the session is currently being aborted/suspended/resumed (or is not suspended)
        if ((abortingSync) || (suspendingSync) || (resumingSync) || (!syncManager.isSyncSuspended())) return;

        if (logger != null) logger.debug("User has chosen to resume the sync session - resuming the sync session");

        // create a new session ID
        String sessionID = Long.toString(System.currentTimeMillis());
        if (logger != null) logger.info("Created new session ID '" + sessionID + "' for user '" + settings.userName + "'");

        // resume the session
        resumingSync = syncManager.resumeSync(sessionID);

        if (resumingSync) {
            // update the UI to indicate that we are resuming
            currentSyncProgress.set(StatusCodes.SYNC_RESUMING, StatusCodes.NONE, 0, 0, null, 0, 0);
            updateSyncProgress(currentSyncProgress);
        }
    }

    /**
     *
     * @return the last progress/status of the current sync. null if we are not currently syncing.
     */
    public SyncProgress getLastProgress() {
        return lastSyncProgress;
    }

    /**
     * @param uiInterface The RPC/remote server alert consumer. Remove the existing consumer by passing null.
     */
    public void setServerAlertConsumer(UICallbackInterface uiInterface) {
        remoteSisClient = uiInterface;
    }

    /**
     * @param uiInterface The local server alert consumer. Remove the existing consumer by passing null.
     */
    public void setServerAlertConsumer(UIInterface uiInterface) {
        localSisClient = uiInterface;
    }

    /**
     * @param uiInterface The RPC/remote CIS consumer. Remove the existing consumer by passing null.
     */
    public void setCISConsumer(UICallbackInterface uiInterface) {
        if (logger != null) logger.info("Setting remote interface for CIS");

        remoteCisClient = uiInterface;
    }

    /**
     * @param uiInterface The local CIS consumer. Remove the existing consumer by passing null.
     */
    public void setCISConsumer(UIInterface uiInterface) {
        if (logger != null) logger.info("Setting local interface for CIS");
    }

    /**
     * 
     * @param uiInterface The local periodic sync consumer. Remove the existing consumer by passing null.
     */
    public void setPeriodConsumer(UICallbackInterface uiInterface) {
        if (logger != null) logger.info("Setting remote interface for Period");

        remotePeriodClient = uiInterface;
    }

    /**
     * @param uiInterface The local Periodic sync consumer. Remove the existing consumer by passing null.
     */
    public void setPeriodConsumer(UIInterface uiInterface) {
        if (logger != null) logger.info("Setting local interface for Periodic Sync");
    }

    /**
     * @return true if there is an RPC/remote UI registered to receive server alerts. Otherwise false.
     */
    public boolean isUIRegisteredForSIS() {
        // check if the remote client is in good shape
        if (remoteSisClient != null && remoteSisClient.asBinder().isBinderAlive())
            return true;

        else {
            // no point in keeping a dead object around
            if (remoteSisClient != null) remoteSisClient = null;

            return false;
        }
    }

    /**
     * @return true if there is an RPC/remote UI registered to receive CIS alerts. Otherwise false.
     */
    public boolean isUIRegisteredForCIS() {
        // check if the remote client is in good shape
        if (remoteCisClient != null && remoteCisClient.asBinder().isBinderAlive())
            return true;

        else {
            // no point in keeping a dead object around
            if (remoteCisClient != null) remoteCisClient = null;

            return false;
        }
    }

    /**
     * @return true if there is an PRC/remote UI registered to receive periodic sync alerts. Otherwise false.
     */
    public boolean isUIRegisteredForPIS() {
        // check if the remote client is in good shape
        if (this.remotePeriodClient != null && remotePeriodClient.asBinder().isBinderAlive())
            return true;
        else {
            // no point in keeping a dead object around
            if (remotePeriodClient != null) remotePeriodClient = null;
            return false;
        }
    }

    /**
     * Called by the CisHandler when syncable items has changed
     * @param mediaType the type of syncable item that has changed
     */
    protected void onCISIntent(int mediaType) {
        businessLogic.onCISIntent(mediaType);
    }

    /**
     * Called by the AlarmManager when current time is for periodic sync 
     */
    protected void onPISIntent() {
        businessLogic.onPeriodicSync();
    }

    /**
     * This is called by the business logic to pass on a server alert to the UI.
     * Do not call this unless you know what you are doing!
     *
     * @param data the SMS "user data"
     * @return true if the alert was successfully passed to the UI, otherwise false.
     */
    public boolean sendUIServerAlert(byte[] data) {
        int i = 0;

        // wait for UI to load if necessary
        while (!isUIRegisteredForSIS() && i < 50) {
            i++;
            if (logger != null) logger.info("waiting for SIS UI");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        if (!isUIRegisteredForSIS()) return false;

        try {
            remoteSisClient.serverAlertReceived(data);

            return true;
        }

        catch (DeadObjectException e) {
            if (logger != null) logger.error("sendUIServerAlert: remote UI is dead", e);

            return false;
        }

        catch (RemoteException e) {
            if (logger != null) logger.error("sendUIServerAlert: RPC error in processing server alert", e);

            return false;
        }
    }

    /**
     * Called when a server alert has been received.
     * We always pass the alert to the business logic first.
     * It can then decide to send it up to the UI if necessary.
     *
     * @param data the SMS "user data"
     */
    @Override
    public void serverAlertReceived(byte[] data) {
        if (data != null) {
            if (logger != null) logger.info("serverAlertReceived sending to business logic");

            if (logger != null) logger.info("sending alert to localSisClient");

            localSisClient.serverAlertReceived(data);
        }
    }

    /**
     * Not implemented. This should never be called on the service
     */
    @Override
    public void onItemsChanged(int mediaType, int numberOfChanges) {}

    /**
     * Sends a CIS alert to the UI, if one is registered.
     *
     * @param mediaType the type of media to sync
     * @param numberOfChanges the number of items that have changed, or -1 if unknown
     * @return true if the alert was successfully passed to the UI, otherwise false.
     */
    public boolean sendUICIS(int mediaType, int numberOfChanges) {
        int i = 0;

        // wait for UI to load if necessary
        while (!isUIRegisteredForCIS() && i < 50) {
            i++;
            if (logger != null) logger.info("waiting for CIS UI");

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
        }

        if (!isUIRegisteredForCIS()) return false;

        if (logger != null) logger.info("calling onItemsChanged in remoteCisClient");

        try {
            remoteCisClient.onItemsChanged(mediaType, numberOfChanges);
        }

        catch (DeadObjectException e) {
            if (logger != null) logger.error("onItemsChanged: remote UI is dead", e);

            return false;
        }

        catch (RemoteException e) {
            if (logger != null) logger.error("RPC error in onItemsChanged", e);

            return false;
        }

        return true;
    }

    /**
     * This calculates the number of contacts that have changed since the last sync,
     * by running through the contacts and calculating the sync state.
     * <b>This is an expensive operation, so this method should be called sparingly.</b>
     *
     * This method should not be called while a sync is in progress.
     *
     * @param saveChangelogs if true, the changelogs calculated here will be used in the subsequent sync.
     * @return the number of contacts that have changed since the last sync,
     * or -1 if the operation could not be completed.
     */
    public synchronized int getNumberChangedContacts(boolean saveChangelogs) {
        // initialize contact info
        int contactChangeCount = -1;

        if (!syncInProgress()) {
            try {
                // make sure any existing contactStore is closed
                if (contactStore != null) {
                    contactStore.close();
                    contactStore = null;
                }

                uiFeedbackDisabled = true; // stay quiet as we are only checking, not syncing
                ContactStore myContactStore = new ContactStore(settings, this, logger);
                AndroidContactList contactList = AndroidContactList.getInstance(myContactStore, getContentResolver(), this, logger);
                // Log.e("SyncEngineService", "getNumberChangedContacts");
                myContactStore.initialize(contactList);

                // make sure we time the operation
                myContactStore.enableSetChangelogTime();

                // determine the number of contacts that have changed since the last sync
                ConsumableStack changes = myContactStore.getChangedRecords();

                // make sure we turn off the timer once we're done
                myContactStore.disableSetChangelogTime();

                // clean up
                // myContactStore.close();

                contactChangeCount = changes.size();

                if (saveChangelogs)
                    contactStore = myContactStore;
                else
                    myContactStore.close();

            }

            catch (Throwable e) {
                if (logger != null) logger.error("Failed to retrieve contactChangeCount", e);
                Log.e("CP-Sync", "Failed to retrieve contact chages count", e);
            }

            finally {
                uiFeedbackDisabled = false;
            }
        }

        return contactChangeCount;
    }

    /**
     * @return The battery level in percent, or 100 if unknown
     */
    public int getBatteryPercent() {
        if (logger != null) logger.info("Battery Level: " + percentBattery);

        return percentBattery;
    }

    public void onPeriodicSync() {
        businessLogic.onPeriodicSync();
    }

    private class SyncEngineBinder extends SyncEngineInterface.Stub {
        SyncEngineService service;

        public SyncEngineBinder(SyncEngineService service) {
            this.service = service;
        }

        /*
         * (non-Javadoc)
         * @see net.cp.ac.core.SyncEngineInterface#startSync(int)
         */
        @Override
        public boolean startSync(int syncMediaTypes) {
            try {
                return service.startSync(syncMediaTypes);
            } catch (Throwable e) {
                return false;
            }
        }

        @Override
        public int getSyncState() {
            return service.getSyncState();
        }

        @Override
        public void abortSync() {
            service.abortSync();
        }

        @Override
        public void suspendSync() {
            service.suspendSync();
        }

        @Override
        public void resumeSync() {
            service.resumeSync();
        }

        @Override
        public ParcelableSyncProgress getLastProgress() {
            SyncProgress progress = service.getLastProgress();

            if (progress == null) return null;

            return new ParcelableSyncProgress(progress);
        }

        @Override
        public void registerCallback(UICallbackInterface uiInterface) throws RemoteException {
            service.addCallbackUI(uiInterface);
        }

        @Override
        public void setServerAlertConsumer(UICallbackInterface uiInterface) throws RemoteException {
            service.setServerAlertConsumer(uiInterface);
        }

        @Override
        public void setCISConsumer(UICallbackInterface uiInterface) throws RemoteException {
            service.setCISConsumer(uiInterface);
        }

        @Override
        public void setPISConsumer(UICallbackInterface uiInterface) throws RemoteException {
            service.setPeriodConsumer(uiInterface);
        }

        /**
         * This calculates the number of contacts that have changed since the last sync,
         * by running through the contacts and calculating the sync state.
         * This is an expensive operation, so this method should be called sparingly.

         * @param saveChangelogs if true, the changelogs calculated here will be used in the subsequent sync.
         * @return the number of contacts that have changed since the last sync,
         * or -1 if the operation could not be completed.
         */
        @Override
        public int getNumberChangedContacts(boolean saveChangelogs) throws RemoteException {
            return service.getNumberChangedContacts(saveChangelogs);
        }

        /**
         * @return The battery level in percent, or 100 if unknown
         */
        @Override
        public int getBatteryPercent() {
            return service.getBatteryPercent();
        }
    }

    @Override
    public void onAlertSlowSync() {
        // TODO Auto-generated method stub
        try {
            // call RPC interfaces
            if (callbackInterfaces != null && callbackInterfaces.size() > 0) {
                int size = callbackInterfaces.size();
                UICallbackInterface uiInterface = null;

                for (int i = size - 1; i >= 0; i--) {
                    uiInterface = callbackInterfaces.get(i);

                    if (uiInterface.asBinder().isBinderAlive()) {
                        try {
                            uiInterface.onAlertSlowSync();
                        }

                        catch (DeadObjectException e) {
                            callbackInterfaces.remove(i); // remove the dead
                                                          // interface from
                                                          // the set!!!
                        }
                    }

                    else
                        callbackInterfaces.remove(i); // remove the dead
                                                      // interface from the
                                                      // set!!!
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
