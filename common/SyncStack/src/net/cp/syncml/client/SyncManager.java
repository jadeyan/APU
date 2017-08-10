/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client;


import android.util.Log;
import net.cp.syncml.client.engine.Session;
import net.cp.syncml.client.devinfo.Device;
import net.cp.syncml.client.devinfo.DeviceCapabilities;
import net.cp.syncml.client.store.RecordStore;
import net.cp.syncml.client.util.Logger;


/**
 * The main class used to manage sync sessions. <br/><br/>
 * 
 * This class is typically used as follows:
 * <pre>
 *      //create the sync manager
 *      Device desktopDevice = new MyDevice(deviceId);
 *      Transport httpTransport = new MyTransport(hostname, port, path);
 *      SyncLogger logger = new DefaultSyncLogger();
 *      SyncListener listener = new DefaultSyncListener(logger);
 *      SyncManager manager = new SyncManager(desktopDevice, httpTransport, username, password, listener, logger);
 *      
 *      //create the store
 *      RecordStore store = new MyStore();
 *      RecordStore[] stores = { store };
 *      
 *      //start the session
 *      manager.startSync(stores, mySessionId);
 *      
 *      //wait for it to complete
 *      while (manager.isSyncRunning())
 *      {
 *          try
 *          {
 *              Thread.sleep(1000);
 *          }
 *          catch (InterruptedException e)
 *          {
 *              break;
 *          }
 *      }
 *      ...
 * </pre>
 * 
 * @see #startSync(RecordStore[], String)
 * @see #stopSync()
 * @see #isSyncRunning()
 *
 * @author Denis Evoy
 */
public class SyncManager
{
    private Device syncDevice;                  //the device on which a sync session runs
    private Transport syncTransport;            //the transport to use during a session 
    private Logger syncLogger;                  //the logger to use when logging session activity
    private SyncListener syncListener;          //the listener who is to receive sync session progress notifications
    
    private String authUsername;                //the username of the user with access to the sync server
    private String authPasword;                 //the password of the user with access to the sync server
    
    private Session syncSession;                //the current sync session (if any)
    private Thread syncThread;                  //the current thread in which the sync session is running (if any)

    
    /**
     * Creates a new sync manager to manage sync sessions for the specified device.
     * 
     * @param device    the device on which a sync session runs. Must not be null.
     * @param transport the transport to use to carry the session. Must not be null.
     * @param username  the username of a user with access to the SyncML server. Must not be null or empty.
     * @param password  the password of a user with access to the SyncML server. Must not be null or empty.
     * @param listener  the listener who is to receive sync session progress notifications. Must not be null.
     * @param logger    the logger to use when logging session activity. May be null.
     */
    public SyncManager(Device device, Transport transport, String username, String password, SyncListener listener, Logger logger)
    {
        if (device == null)
            throw new IllegalArgumentException("no sync device specified");
        if (transport == null)
            throw new IllegalArgumentException("no sync transport specified");
        if ( (username == null) || (username.length() <= 0) )
            throw new IllegalArgumentException("no username specified");
        if ( (password == null) || (password.length() <= 0) )
            throw new IllegalArgumentException("no password specified");
        if (listener == null)
            throw new IllegalArgumentException("no sync listener specified");
        
        //get the static details
        String deviceId = device.getDeviceID();
        String deviceType = device.getDeviceType();
        DeviceCapabilities deviceCaps = device.getCapabilities();
        String transportUri = transport.getTargetURI();

        //validate any mandatory details
        if ( (deviceId == null) || (deviceId.length() <= 0) )
            throw new IllegalArgumentException("no device ID specified");
        if ( (deviceType == null) || (deviceType.length() <= 0) )
            throw new IllegalArgumentException("no device type specified");
        if (deviceCaps == null)
            throw new IllegalArgumentException("no device capabilities specified");
        if ( (transportUri == null) || (transportUri.length() <= 0) )
            throw new IllegalArgumentException("no transport target URI specified");
        
        syncDevice = device;
        syncTransport = transport;
        syncListener = listener;
        syncLogger = logger;
        
        authUsername = username;
        authPasword = password;
        
        syncSession = null;
    }


    /**
     * Returns the device on which the sync session is running.
     * 
     * @return The sync session device. Will not be null.
     */
    public Device getDevice()
    {
        return syncDevice;
    }

    /**
     * Returns the transport over which the sync session is operating.
     * 
     * @return The sync session transport. Will not be null.
     */
    public Transport getTransport()
    {
        return syncTransport;
    }

    /**
     * Returns the listener who is to receive sync session progress notifications.
     * 
     * @return The sync notification listener. Will not be null.
     */
    public SyncListener getSyncListener()
    {
        return syncListener;
    }

    /**
     * Returns the logger used by the session.
     * 
     * @return The logger used by the session. Will not be null.
     */
    public Logger getSyncLogger()
    {
        return syncLogger;
    }

    
    /**
     * Returns the username of the user with access to the SyncML server.
     * 
     * @return The username of the sync user. Will not be null or empty.
     */
    public String getAuthUsername()
    {
        return authUsername;
    }

    /**
     * Returns the password of the user with access to the SyncML server.
     * 
     * @return The plain-text password of the sync user. Will not be null or empty.
     */
    public String getAuthPasword()
    {
        return authPasword;
    }


    /**
     * Returns whether or not a sync session is currently running. <br/><br/>
     * 
     * Note that a suspended session is still considered to be running.
     * 
     * @return <code>true</code> is a sync session is running.
     */
    public synchronized boolean isSyncRunning()
    {
        return ( (syncSession != null) && (! syncSession.isStopped()) ); 
    }

    /**
     * Returns whether or not a sync session is suspended.
     * 
     * @return <code>true</code> is a sync session is suspended.
     */
    public synchronized boolean isSyncSuspended()
    {
        return ( (syncSession != null) && (syncSession.isSuspended()) ); 
    }
    
    /**
     * Starts a new sync session with the specified unique ID for the specified record stores.
     * 
     * @param stores    the local record stores to be synced. May not be null or empty.
     * @param sessionId a unique ID for the session. Must be non-zero positive.
     * @return <code>true</code> if the session was started or <code>false</code> if a session already exists.
     */
    public synchronized boolean startSync(RecordStore[] stores, String sessionId)
    {
        //nothing more to do if a sync session is already running
    	Log.e("CP-Sync", "Sync manager starts a sync");
        if (isSyncRunning())
            return false;
        
        //create a new session and start the session thread
        syncSession = new Session(this, stores, sessionId);
        syncThread = new Thread(syncSession);
        syncThread.start();
        return true;
    }

    /** 
     * Stops the current sync session (if one is running) due to a user abort. <br/><br/>
     * 
     * This request will be ignored if a session hasn't been started yet. 
     */
    public synchronized void stopSync()
    {
        stopSync(SyncML.STATUS_OPERATION_CANCELLED, null);
    }

    /** 
     * Stops the current sync session (if one is running) due to the specified reason. <br/><br/>
     * 
     * This request will be ignored if a session hasn't been started yet.
     *  
     * @param statusCode the status code (SyncML.STATUS_XXX) indicating why the session is being stopped.
     * @param statusData any additional status data indicating why the session is being stopped.
     */
    public synchronized void stopSync(int statusCode, String statusData)
    {
        //stop the session if one is running 
        if (isSyncRunning())
            syncSession.stopSession(statusCode, statusData);
        
        //clean up
        syncSession = null;
        syncThread = null;
    }
    
    /** 
     * Requests that the current sync session (if any) be suspended. <br/><br/>
     * 
     * Note that the request to suspend the session may be ignored. For example, if the session is almost 
     * complete, it is better to let it finish than to suspend it. Also, if the session is just starting 
     * and no data has been synced yet, the session will be terminated instead of being suspended. <br/><br/>
     * 
     * If the request to suspend the session is accepted, the {@link SyncListener#onSuspendResult(int, String)} 
     * method will be called to indicate the result of the request. If successful, 
     * {@link SyncListener#onSyncSuspend()} and {@link RecordStore#onSyncSuspend()} will also be called 
     * when the client has actually suspended its activity. If the request was rejected, the session will 
     * continue as normal. <br/><br/>
     * 
     * Note that it is possible for the sync session to become suspended unintentionally (e.g. network 
     * error when connecting to the SyncML server).
     * 
     * @return <code>true</code> if the session will be suspended or <code>false</code> otherwise.
     */
    public synchronized boolean suspendSync()
    {
        //nothing more to do if a sync session is not running or is already suspended
        if ( (! isSyncRunning()) || (isSyncSuspended()) )
            return false;
        
        //request that the session be suspended 
        return syncSession.suspendSession();
    }
    
    /**
     * Resumes the previously suspended sync session with the specified unique ID. <br/><br/>
     * 
     * The {@link SyncListener#onSyncResume(boolean)} method will be called to indicate that the session is being 
     * resumed and {@link RecordStore#onResumeResult(int, String)} will be called to indicate the actual result of 
     * the resume request for each record store. 
     * 
     * @param sessionId a unique ID for the resumed session. Must be non-zero positive.
     * @return <code>true</code> if the session will be resumed or <code>false</code> the session is not suspended.
     */
    public synchronized boolean resumeSync(String sessionId)
    {
        //nothing more to do if a sync session is not suspended or is already being resumed
        if ( (! isSyncSuspended()) || (syncSession.isResuming()) )
            return false;
        
        //resume the existing session and start the session thread
        syncSession.resumeSession(sessionId);
        syncThread = new Thread(syncSession);
        syncThread.start();
        return true;
    }
}
