/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.ui;

import java.io.InputStream;

import net.cp.ac.core.AndroidEngineSettings;
import net.cp.ac.core.AndroidPersistentStoreManager;
import net.cp.ac.core.AppLogger;
import net.cp.ac.core.ParcelableSyncProgress;
import net.cp.ac.core.SyncEngineInterface;
import net.cp.ac.core.SyncEngineService;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncProgress;
import net.cp.engine.EngineSettings;
import net.cp.engine.UIInterface;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.ServerAlert;
import net.cp.syncml.client.SyncException;
import net.cp.syncml.client.util.Logger;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * This class should be extended to create the UI that displays the sync progress to the user.
 * It handles all the complex RPC calls necessary to interface with the sync service.
 *
 *
 *
 */
public abstract class SyncUIActivity extends Activity implements UIInterface
{
    /**
     * The logger to use.
     * Note this field is static, and is shared by all instances of this class.
     */
    protected static Logger logger;

    /**
     * Settings relating to the sync engine and business logic.
     * Note this field is static, and is shared by all instances of this class.
     */
    protected static EngineSettings engineSettings;

    /**
     * Settings relating to the UI and everything else not covered by teh engine settings.
     * Note this field is static, and is shared by all instances of this class.
     */
    protected static UISettings uiSettings;

    /**
     * Used to access persistent storage.
     * Note this field is static, and is shared by all instances of this class.
     */
    protected static AndroidPersistentStoreManager storeManager;

    /**
     * Used to connect to the SyncEngineService service.
     * Note this field is static, and is shared by all instances of this class.
     */
    protected static UIServiceConnection serviceConnection;

    /**
     * Set to true if we have registered for server alerts, otherwise false.
     *
     */
    protected boolean registeredForServerAlerts;

    /**
     * Set to true if we have registered for CIS alerts, otherwise false.
     */
    protected boolean registeredForCISAlerts;

    /**
     * Called to bind this Activity to the sync service.
     * This allows the Activity to commmunicate with the service,
     * and vice-versa.
     */
    protected void bindToSyncService()
    {
        Context context = getApplication();
        if(serviceConnection == null)
            serviceConnection = new UIServiceConnection(this, logger);

        else //Activity could have been destroyed and re-created since
            serviceConnection.resetUI(this);

        if(!serviceConnection.bindCalled())
        {
            //explicitly start the service, so that it will remain running after the UI quits
            ComponentName name = new ComponentName(getPackageName(), SyncEngineService.class.getName());
            ComponentName service = context.startService(new Intent().setComponent(name));
            if (service == null)
            {
                if(logger != null)
                    logger.error("SyncUIActivity: Could not start sync service " + name.toString());
            }

            //now bind UI to service
            serviceConnection.bind(context);
        }
    }

    protected boolean bindCalled()
    {
        if (serviceConnection != null)
            return serviceConnection.bindCalled();

        return false;
    }

    protected boolean isBound()
    {
        if (serviceConnection != null)
            return serviceConnection.isBound();

        return false;
    }

    /**
     * {@inheritDoc}
     *
     * Initializes settings and the logger for this class.
     */
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        registeredForServerAlerts = false;
        registeredForCISAlerts = false;
    }

    /**
     * {@inheritDoc}
     *
     * Initializes settings and the logger for this class.
     */
    protected void onStart()
    {
        super.onStart();

        boolean success = initSettings();

        if(!success)
            return;

        if(logger == null)
        {
            //initialise the logger if required
            AppLogger.initUILogger(engineSettings, uiSettings.logFilePath);
            logger = AppLogger.getUIInstance();

            engineSettings.setLogger(logger);
            uiSettings.setLogger(logger);
        }

        if(logger != null)
            logger.info("initialize() complete. logger: " + logger.toString());

        //bind in case a sync is already in progress
        bindToSyncService();
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    protected void onDestroy()
    {
        super.onDestroy();
    }

    /**
     * @return true if we successfully initialized settings, false if there was a problem.
     */
    private boolean initSettings()
    {
        if(engineSettings == null || uiSettings == null)
        {
            //read in the engine settings for future use
            byte[] configData = null;

            InputStream configInputStream = null;

            try
            {
                configData = AndroidPersistentStoreManager.getConfigFromStore(getApplication(), logger);

                //first run, the config data has not been saved to persistent storage yet
                if(configData == null || configData.length <= 0)
                {
                    configInputStream = getConfigInputStreamFromResource();

                    if(configInputStream != null)
                        AndroidPersistentStoreManager.writeConfigToStore(configInputStream, getApplication(), logger);

                    else
                        return false;

                    //now read it back from the store
                    configData = AndroidPersistentStoreManager.getConfigFromStore(getApplication(), logger);

                    if(configData == null)
                        return false;
                }

                if(storeManager == null)
                {
                    AndroidPersistentStoreManager.init(getApplication(), logger);
                    storeManager = AndroidPersistentStoreManager.getInstance();
                }

                if(engineSettings == null)
                    engineSettings = AndroidEngineSettings.init(configData, storeManager, getApplicationContext(), logger);
                if(uiSettings == null)
                    uiSettings = UISettings.init(configData, storeManager, logger);
            }

            catch (Throwable e)
            {
                //no logger yet
                e.printStackTrace();
                return false;
            }

            finally
            {
                UtilityClass.streamClose(configInputStream, logger);
            }
        }

        return true;
    }

    /** returns an InputStream from which the config data can be read */
    protected abstract InputStream getConfigInputStreamFromResource();

    /** Called when we have successfully connected to the service */
    protected abstract void onBindComplete();

    /** Updates the sync session progress UI with the specified details. */
    public abstract void updateSyncProgress(SyncProgress progress);

    /** Called when the engine has finished calculating the changes. */
    public abstract void onGetChangesEnd();

    /** Called when an error occurs during the sync session. */
    public abstract void onSyncError();

    /** Called when the sync has ended, no matter how. */
    public abstract void onSyncEnd();

    /**
     * Called when the request to start a sync did not succeed.
     * This is called when the sync session has not even been started because of a error.
     * E.g. a connection error
     */
    public void onStartSyncFailure(){}

    /** Called when a Server Alert is received. */
    public void onServerAlert(ServerAlert alert){}


    /** Called when syncable items have changed, and it is time to sync.
     *  Only called if this UI is registered to receive CIS notifications.
     *  @param mediaType the type of items that have changed.
     *  Currently only EngineSettings.MEDIA_TYPE_CONTACTS supported
     *  @param numberOfChanges the number of changes detected,
     *  or -1 if this information could not be determined.
     *  Note that if settings.contactMinSyncLimit is set to 0, numberOfChanges will always be -1.
     */
    public void onItemsChanged(int mediaType, int numberOfChanges){}

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
    public int getSyncState()
    {
        //wait for service to attach
        if(serviceConnection != null && serviceConnection.isBound())
        {
            try
            {
                return serviceConnection.syncService.getSyncState();
            }

            catch (Throwable e)
            {
                if(logger != null)
                    logger.error("Error talking to service", e);
            }
        }

        return StatusCodes.NONE;
    }

    protected SyncProgress getLastProgress()
    {
        //wait for service to attach
        if(serviceConnection != null && serviceConnection.isBound())
        {
            try
            {
                return serviceConnection.syncService.getLastProgress();
            }

            catch (Throwable e)
            {
                if(logger != null)
                    logger.error("Error talking to service", e);
            }
        }

        return null;
    }

    /**
     * @return The battery level in percent, 100 if unknown,
     * or 0 if we failed to connect to service.
     */
    protected int getBatteryPercent()
    {
        //wait for service to attach
        if(serviceConnection != null && serviceConnection.isBound())
        {
            try
            {
                return serviceConnection.syncService.getBatteryPercent();
            }

            catch (Throwable e)
            {
                if(logger != null)
                    logger.error("Error talking to service", e);
            }
        }

        return 0;
    }

    /**
     * Tells the sync service to start a sync session.
     * This method does not block.
     *
     * @param syncMediaTypes the media types to sync
     */
    protected void startSync(int syncMediaTypes)
    {
        if(logger != null)
            logger.info("startSync called");

        //if no sync type is specified, return
        if (syncMediaTypes == EngineSettings.MEDIA_TYPE_NONE)
            return;

        bindToSyncService();

        final int syncTypes = syncMediaTypes;

        new Thread()
        {
            public void run()
            {
                //wait for service to attach
                while(!serviceConnection.isBound())
                    try {Thread.sleep(200);} catch (InterruptedException e1){}

                try
                {
                    if(serviceConnection.syncService.getSyncState() == StatusCodes.NONE)
                    {
                        boolean success = serviceConnection.syncService.startSync(syncTypes);
                        if(!success)
                            onStartSyncFailure();
                    }
                }
                catch (RemoteException e)
                {
                    onStartSyncFailure();

                    if(logger != null)
                        logger.error("Error initiating sync", e);

                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Called by the UI to suspend the sync session in progress.
     * The session can be resumed for a certain period of time before it is aborted.
     */
    protected void suspendSync()
    {
        try
        {
            if(serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.suspendSync();
            }

        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error registering for SIS", e);
        }
    }

    /**
     * Called by the UI to resume the suspended sync session.
     */
    protected void resumeSync()
    {
        try
        {
            if(serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.resumeSync();
            }

        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error registering for SIS", e);
        }
    }

    /**
     * Called to fully stop the sync session in progress.
     * The session cannot be resumed. A new session must be started.
     */
    protected void abortSync()
    {
        try
        {
            if(serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.abortSync();
            }

        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error registering for SIS", e);
        }
    }

    /* (non-Javadoc)
     * @see net.cp.engine.UIInterface#serverAlertReceived(byte[])
     */
    public void serverAlertReceived(byte[] data)
    {
        try
        {
            ServerAlert alert = ServerAlert.parse(data);
            onServerAlert(alert);
        }

        catch (SyncException e)
        {
            if(logger != null)
                logger.error("Error parsing server alert data", e);
        }
    }

    /**
     * Tell the sync service that we want to be notified when server alerts come in.
     *
     */
    protected void registerForServerAlerts()
    {
        try
        {
            if(!registeredForServerAlerts && serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.setServerAlertConsumer(serviceConnection.binder);
                registeredForServerAlerts = true;
            }

        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error registering for SIS", e);
        }
    }

    /**
     * Tell the sync service that we don't want to be notified when server alerts come in.
     *
     */
    protected void unRegisterForServerAlerts()
    {
        try
        {
            if(registeredForServerAlerts && serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.setServerAlertConsumer(null);
                registeredForServerAlerts = false;
            }
        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error unregistering for SIS", e);
        }
    }

    /**
     * Tell the sync service that we want to be notified when CIS alerts come in.
     *
     */
    protected void registerForCISAlerts()
    {
        try
        {
            if(!registeredForCISAlerts && serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.setCISConsumer(serviceConnection.binder);
                registeredForCISAlerts = true;
            }

        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error registering for CIS", e);
        }
    }

    /**
     * Tell the sync service that we don't want to be notified when CIS alerts come in.
     *
     */
    protected void unRegisterForCISAlerts()
    {
        try
        {
            if(registeredForCISAlerts && serviceConnection != null && serviceConnection.isBound())
            {
                serviceConnection.syncService.setCISConsumer(null);
                registeredForCISAlerts = false;
            }
        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error unregistering for CIS", e);
        }
    }
}

/**
 *
 * Utility class to facilitate RPC callbacks from the service to the UI.
 */
class UICallbackBinder extends UICallbackInterface.Stub
{
    SyncUIActivity uiActivity;

    /**
     * @param uiActivity The UI in question
     */
    public UICallbackBinder(SyncUIActivity uiActivity)
    {
        this.uiActivity = uiActivity;
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.UICallbackInterface#updateSyncProgress(net.cp.ac.core.ParcelableSyncProgress)
     */
    public void updateSyncProgress(ParcelableSyncProgress status)
    {
        uiActivity.updateSyncProgress(status);
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.UICallbackInterface#onGetChangesEnd()
     */
    public void onGetChangesEnd()
    {
        uiActivity.onGetChangesEnd();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.UICallbackInterface#onSyncError()
     */
    public void onSyncError()
    {
        uiActivity.onSyncError();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.UICallbackInterface#onSyncEnd()
     */
    public void onSyncEnd()
    {
        uiActivity.onSyncEnd();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.UICallbackInterface#serverAlertReceived(byte[])
     */
    public void serverAlertReceived(byte[] data)
    {
        uiActivity.serverAlertReceived(data);
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.UICallbackInterface#onItemsChanged(int, int)
     */
    public void onItemsChanged(int mediaType, int numberOfChanges)
    {
        uiActivity.onItemsChanged(mediaType, numberOfChanges);
    }

    /**
     * @param uiActivity The UI in question
     */
    public void setUIInterface(SyncUIActivity uiActivity)
    {
        this.uiActivity = uiActivity;
    }

    /**
     * Called then the bind operation has completed.
     */
    public void onBindComplete()
    {
        uiActivity.onBindComplete();
    }

	@Override
	public void onAlertSlowSync() throws RemoteException {
		this.uiActivity.onAlertSlowSync();
	}
}

/**
 *
 * Utility class to facilitate the RPC connection between the UI and the service.
 */
class UIServiceConnection implements ServiceConnection
{
    protected SyncEngineInterface syncService;
    protected final UICallbackBinder binder;
    protected boolean isBound;
    protected boolean bindCalled;
    protected Logger logger;
    protected Thread bindThread;

    /**
     * @param uiActivity The UI in question.
     * @param logger The logger to use.
     */
    public UIServiceConnection(SyncUIActivity uiActivity, Logger logger)
    {
        this.logger = logger;
        isBound = false;
        bindCalled = false;
        binder = new UICallbackBinder(uiActivity);
    }

    /**
     * @return true if bind(Context context) has been called already, otherwise false.
     */
    public synchronized boolean bindCalled()
    {
        return bindCalled;
    }

    /**
     * Creates an RPC connection between the UI and the sync service.
     * Non blocking.
     * @param context The context to use to bind to the sync service.
     */
    public synchronized void bind(Context context)
    {
        final Context theContext = context;
        final ServiceConnection connection = this;

        bindCalled = true;

        if(logger != null)
            logger.info("SyncUIActivity: binding to service");

        bindThread = new Thread( new Runnable(){ public void run(){

            theContext.bindService(new Intent(SyncEngineService.class.getName()),
                connection, Context.BIND_AUTO_CREATE);
        }});

        bindThread.start();

    }

    /**
     * This is called when the connection with the service has been
     * established, giving us the service object we can use to
     * interact with the service.
     */
    public void onServiceConnected(ComponentName className,
            IBinder service)
    {
        /*
         * We are communicating with our
         * service through an IDL interface, so get a client-side
         * representation of that from the raw service object.
         */
        syncService = SyncEngineInterface.Stub.asInterface(service);

        // pass our UI interface back to the service to receive updates
        try
        {
            isBound = true;
            syncService.registerCallback(binder);
            binder.onBindComplete();
        }
        catch (RemoteException e)
        {
            if(logger != null)
                logger.error("Error in callback", e);
            else
                e.printStackTrace();
        }
    }

    /* (non-Javadoc)
     * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
     */
    public void onServiceDisconnected(ComponentName className)
    {
        // This is called when the connection with the service has been
        // unexpectedly disconnected -- that is, its process crashed.
        isBound = false;
        bindCalled = false;
        syncService = null;
    }

    /**
     * @return true if the service is bound, otherwise false.
     */
    public synchronized boolean isBound()
    {
        return isBound;
    }

    /**
     * Called to reset the UI the service will pass information to.
     *
     * @param uiActivity The UI in question
     */
    public void resetUI(SyncUIActivity uiActivity)
    {
        binder.setUIInterface(uiActivity);
    }

}
