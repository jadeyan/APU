/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.TelephonyManager;
import net.cp.engine.BackupException;
import net.cp.engine.EngineSettings;
import net.cp.engine.PersistentStoreManager;
import net.cp.syncml.client.util.Logger;

/**
 * This class implements Android specific behaviour for engine settings.
 * This includes broadcasting an Intent when the settings have been saved.
 *
 * This class implements the Singleton design pattern.
 *
 * @see EngineSettings
 * @see Settings
 *
 *
 *
 */
public class AndroidEngineSettings extends EngineSettings
{
    /**
     * The context used to send the broadcast Intents
     */
    protected static Context context;

    /**
     * Used to receive notice that the settings have been changed in another process
     */
    protected static BroadcastReceiver settingsUpdater;

    /**
     * Used to make sure that we don't act on an Intent that was sent from our own process.
     * This prevents us from unneccessarily refreshing the settings from disk.
     */
    protected static long refreshID;

    /**
     * The name of the extra info put into the broadcast Intent (the value of this info is the refreshID)
     */
    protected static final String refreshName = "net.cp.ac.core.AndroidEngineSettings.id";

    /**
     * The name of the Intent used to tell other process to reload their settings
     */
    public static final String RELOAD_SETTINGS_INTENT = "net.cp.ac.intent.RELOAD_SETTINGS";


    /**
     * @param theLogger The logger to use
     * @param context The context to use
     */
    public AndroidEngineSettings(Logger theLogger, Context context)
    {
        super(theLogger);
        AndroidEngineSettings.context = context;
        initBroadcastReceiver();
    }


    /**
     * @param configData The configuration data in "features.properties" format
     * @param manager Used to access the PersistentStore
     * @param context The context to use
     * @param theLogger The logger to use
     * @return returns the instance of EngineSettings
     * @throws BackupException if there was a problem parsing the data or storing it in the PersistentStore
     */
    public synchronized static EngineSettings init(byte[] configData, PersistentStoreManager manager, Context context, Logger theLogger)
        throws BackupException
    {
        storeManager = manager;

        //create the single instance of the settings if necessary
        if (instance == null)
        {
            if(theLogger != null)
                theLogger.info("Initializing application settings");
            instance = new AndroidEngineSettings(theLogger, context);
            instance.readSettings(configData);
        }

        return instance;
    }

    /**
     *  {@inheritDoc}
     *  This method also broadcasts an Intent
     *  so that other processes can be aware that the settings have changed.
     */
    @Override
    public boolean writeStateSettings()
    {
        //let everyone know we'ew changing the settings
        Intent reloadSettingsIntent = new Intent();
        reloadSettingsIntent.setAction(RELOAD_SETTINGS_INTENT);

        reloadSettingsIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        refreshID = System.currentTimeMillis();

        reloadSettingsIntent.putExtra(refreshName, refreshID);

        boolean ret = super.writeStateSettings();

        //tell anyone that cares to reload their settings
        context.sendOrderedBroadcast(reloadSettingsIntent, null);

        if(logger != null)
            logger.info("AndroidEngineSettings: State settings changed, broadcasting intent");

        return ret;
    }

    /**
     *  {@inheritDoc}
     *  This method also broadcasts an Intent
     *  so that other processes can be aware that the settings have changed.
     */
    @Override
    public boolean writeUserSettings()
    {
        //let everyone know we'ew changing the settings
        Intent reloadSettingsIntent = new Intent();
        reloadSettingsIntent.setAction(RELOAD_SETTINGS_INTENT);

        reloadSettingsIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        refreshID = System.currentTimeMillis();

        reloadSettingsIntent.putExtra(refreshName, refreshID);

        boolean ret = super.writeUserSettings();

        //tell anyone that cares to reload their settings
        context.sendOrderedBroadcast(reloadSettingsIntent, null);

        if(logger != null)
            logger.info("AndroidEngineSettings: Saving user settings, broadcasting intent");

        return ret;
    }

    /**
     * @return the refreshID
     */
    protected long getRefreshID()
    {
        return refreshID;
    }

    /**
     * Sets up the broadcast receiver to be notified when settings have been changed.
     */
    private void initBroadcastReceiver()
    {
        if(settingsUpdater == null)
        {
            settingsUpdater = new BroadcastReceiver()
            {
                public void onReceive(Context context, Intent intent)
                {
                    //make sure we received the right intent
                    if(RELOAD_SETTINGS_INTENT.equals(intent.getAction()))
                    {
                        try
                        {
                            long intentID = intent.getLongExtra(refreshName, getRefreshID());
                            //reload the data from the RMS, unless it was us that sent the intent!
                            if(intentID != getRefreshID())
                            {
                                if(logger != null)
                                    logger.info("AndroidEngineSettings: refresh intent receieved. id: " + intentID + ", reloading settings");

                                readSettings(null);
                            }
                        }

                        catch (BackupException e)
                        {
                            if(logger != null)
                                logger.error("AndroidEngineSettings: failed to reload settings", e);
                        }
                    }
                }
            };
        }
        IntentFilter filter = new IntentFilter(RELOAD_SETTINGS_INTENT);

        //register for broadcasts, using the filter defined above
        context.registerReceiver(settingsUpdater, filter);
    }

    /* (non-Javadoc)
     * @see net.cp.engine.EngineSettings#close()
     */
    public void close()
    {
        if(settingsUpdater != null)
        {
            context.unregisterReceiver(settingsUpdater);
            settingsUpdater = null;
        }

        super.close();
    }

    @Override
    public String getDeviceId()
    {
        TelephonyManager teleManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        String imei = teleManager.getDeviceId();

        if ( (imei != null) && (imei.length() > 0) )
        {
            if (logger != null)
                logger.info("Found IMEI: '" + imei + "'");
            return DEVICE_ID_PREFIX + imei;
        }

        //IMEI not accessible, so make up a device ID
        if (logger != null)
            logger.info("No IMEI available - generating a unique device ID");

        return DEVICE_ID_PREFIX + Long.toString(System.currentTimeMillis());
    }

}
