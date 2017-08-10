/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.ui;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Hashtable;

import android.util.Log;

import net.cp.engine.BackupException;
import net.cp.engine.PersistentStore;
import net.cp.engine.PersistentStoreManager;
import net.cp.engine.Settings;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.util.Logger;

/**
 *
 * This class contains settings specific to the UI and other areas not explicitly covered by
 * EngineSettings.
 *
 * Implements the Singleton design pattern.
 *
 */
public class UISettings extends Settings
{
    private static UISettings instance;

    /**
     * The URL the user will be directed to if they select the "Portal" option in the UI
     */
    public String portalUrl;

    /**
     * The title of the main screen
     */
    public String title;

    /**
     * The path where the application log file should be created
     */
    public String logFilePath;

    /**
     *  Creates new settings - protected to enforce singleton behaviour.
     */
    protected UISettings(Logger theLogger)
    {
        super(theLogger);
        RMS_SETTINGS_NAME = "PB-UISettings";
    }


    /**
     * Initializes the settings
     *
     * @param configData the configuration data in "features.properties" format.
     * @param manager Used to access the persistent store.
     * @param theLogger The logger to use.
     * @return the instance of the class
     * @throws BackupException if the settings could not be parsed, or there was a problem
     * accessing persistent storage.
     */
    public synchronized static UISettings init(byte[] configData, PersistentStoreManager manager, Logger theLogger)
        throws BackupException
    {
        storeManager = manager;
        //create the single instance of the settings if necessary
        if (instance == null)
        {
            if(theLogger != null)
                theLogger.info("Initializing application settings");

            instance = new UISettings(theLogger);
            instance.readSettings(configData);
        }

        return instance;
    }

    /**
     * Re-initializes the settings
     *
     * @param configData the configuration data in "features.properties" format.
     * @param manager Used to access the persistent store.
     * @param theLogger The logger to use.
     * @return the instance of the class
     * @throws BackupException if the settings could not be parsed, or there was a problem
     * accessing persistent storage.
     */
    public synchronized static void reinit(byte[] configData, PersistentStoreManager manager, Logger theLogger)
        throws BackupException
    {
        if (instance == null)
            return;

        storeManager = manager;

        //just clear and read the settings again
        if(theLogger != null)
            theLogger.info("Reinitializing UI settings");

        synchronized(instance)
        {
            Logger iLogger = instance.logger;
            instance.logger = null;
            instance.clear();
            instance.readSettings(configData);
            instance.logger = iLogger;
        }

    }

    /**
     * Closes the settings.
     */
    public synchronized static void close()
    {
        if(instance != null && instance.logger != null)
            instance.logger.info("Closing application settings");

        instance = null;
    }

    /* (non-Javadoc)
     * @see net.cp.engine.Settings#readSettingsResource(byte[], boolean)
     */
    protected void readSettingsResource(byte[] configData, boolean upgrade)
        throws BackupException
    {
        if (logger != null)
            logger.info("Reading UI settings from the config data - upgrade=" + upgrade);

        try
        {
            //load the properties
            Hashtable<String, String> properties = readProperties(configData);

            portalUrl = getStringProperty(properties, "config.ui.portalUrl");
            title = getStringProperty(properties, "config.ui.title");
            logFilePath = getStringProperty(properties, "config.ui.logFilePath");
        }

        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to read UI settings from the supplied inputstream", e);

            throw new BackupException("Failed to read UI settings from the supplied inputstream", e);
        }
    }

    /* (non-Javadoc)
     * @see net.cp.engine.Settings#clear()
     */
    protected void clear()
    {
        if (logger != null)
            logger.info("Clearing UI settings");

        recordIdUser = 0;
        recordIdConfig = 0;
        recordIdState = 0;
        recordVersionUser = 0;

        upgrading = false;

        portalUrl = "";
        title = "";
        logFilePath = "";

    }

    /* (non-Javadoc)
     * @see net.cp.engine.Settings#readSettingsRms(int, byte[])
     */
    protected void readSettingsRms(int recordId, byte[] recordData)
            throws Exception
    {

        ByteArrayInputStream byteStream = null;
        DataInputStream dataStream = null;
        try
        {
            byteStream = new ByteArrayInputStream(recordData);
            dataStream = new DataInputStream(byteStream);

            //make sure the version is correct
            short version = dataStream.readShort();
            if ( (version <= 0) || (version > VERSION_CURRENT) )
                throw new BackupException("Invalid version '" + version + "' found");

            //read the data based on the record type
            byte recordType = dataStream.readByte();
            if (recordType == RECORD_TYPE_USER)
            {
                //read all user settings - nothing more to do if they have already been read
                if (recordIdUser > 0)
                    throw new BackupException("Multiple UI user settings records found in the record store");

                if (logger != null)
                    logger.debug("Reading UI user settings (version '" + version + "') from the record store");

                //TODO insert user UI settings here (if any)

                //perform any other upgrade steps if necessary
                if (version < VERSION_CURRENT)
                    upgradeSettingsRms(recordType, version);

                //remember the record ID so we can update the record later
                recordIdUser = recordId;
                recordVersionUser = version;
            }
            else if (recordType == RECORD_TYPE_CONFIG)
            {
                //read all static config - nothing more to do if they have already been read
                if (recordIdConfig > 0)
                    throw new BackupException("Multiple UI config settings records found in the record store");

                if (logger != null)
                    logger.debug("Reading config settings (version '" + version + "') from the record store");

                portalUrl = dataStream.readUTF();
                title = dataStream.readUTF();
                logFilePath = dataStream.readUTF();

                //perform any other upgrade steps if necessary
                if (version < VERSION_CURRENT)
                    upgradeSettingsRms(recordType, version);

                //remember the record ID so we can update the record later
                recordIdConfig = recordId;
            }
            else if (recordType == RECORD_TYPE_STATE)
            {
                //read all runtime state settings - nothing more to do if they have already been read
                if (recordIdState > 0)
                    throw new BackupException("Multiple UI state settings records found in the record store");

                if (logger != null)
                    logger.debug("Reading UI state settings (version '" + version + "') from the record store");

                upgrading = dataStream.readBoolean();

                //perform any other upgrade steps if necessary
                if (version < VERSION_CURRENT)
                    upgradeSettingsRms(recordType, version);

                //remember the record ID so we can update the record later
                recordIdState = recordId;
            }
            else
            {
                throw new BackupException("Invalid record type '" + recordType + "' found");
            }
        }
        finally
        {
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }

    }

    /**
     * Not implemented
     */
    protected void upgradeSettingsRms(byte recordType, short fromVersion)
    {
        //TODO implement UISettings upgrade
    }

    /* (non-Javadoc)
     * @see net.cp.engine.Settings#writeSettingsRms(java.lang.String, int)
     */
    protected void writeSettingsRms(String storeName, int recordType)
            throws BackupException
    {

        if (logger != null)
            logger.info("Writing UI settings to the '" + storeName + "' record store");

        PersistentStore recordStore = null;
        ByteArrayOutputStream byteStream = null;
        DataOutputStream dataStream = null;
        try
        {
            //open the store (creating it if necessary)
            recordStore = storeManager.openRecordStore(storeName);

            //create the necessary streams
            byteStream = new ByteArrayOutputStream();
            dataStream = new DataOutputStream(byteStream);

            //write the user settings if required
            if ( (recordType == 0) || (recordType == RECORD_TYPE_USER) )
            {
                if (logger != null)
                    logger.debug("Writing UI user settings (version '" + VERSION_CURRENT + "') to the record store");

                //write the current version number and record type
                byteStream.reset();
                dataStream.writeShort(VERSION_CURRENT);
                dataStream.writeByte(RECORD_TYPE_USER);

                //TODO add user settings here (if any)

                //write the record
                byte[] recordData = byteStream.toByteArray();
                recordIdUser = recordStore.writeRecord(recordIdUser, recordData);
                recordVersionUser = VERSION_CURRENT;
            }

            //write the static config settings if required
            if ( (recordType == 0) || (recordType == RECORD_TYPE_CONFIG) )
            {
                if (logger != null)
                    logger.debug("Writing UI config settings (version '" + VERSION_CURRENT + "') to the record store");

                //write the current version number and record type
                byteStream.reset();
                dataStream.writeShort(VERSION_CURRENT);
                dataStream.writeByte(RECORD_TYPE_CONFIG);

                dataStream.writeUTF(portalUrl);
                dataStream.writeUTF(title);
                dataStream.writeUTF(logFilePath);

                //write the record
                byte[] recordData = byteStream.toByteArray();
                recordIdConfig = recordStore.writeRecord(recordIdConfig, recordData);
            }

            //write the runtime state settings if required
            if ( (recordType == 0) || (recordType == RECORD_TYPE_STATE) )
            {
                if (logger != null)
                    logger.debug("Writing UI state settings (version '" + VERSION_CURRENT + "') to the record store");

                //write the current version number and record type
                byteStream.reset();
                dataStream.writeShort(VERSION_CURRENT);
                dataStream.writeByte(RECORD_TYPE_STATE);

                dataStream.writeBoolean(upgrading);

                //write the record
                byte[] recordData = byteStream.toByteArray();
                recordIdState = recordStore.writeRecord(recordIdState, recordData);
            }
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to write UI settings to the '" + storeName + "' record store", e);

            throw new BackupException("Failed to write UI settings to the '" + storeName + "' record store", e);
        }
        finally
        {
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
            storeManager.closeRecordStore(recordStore);
        }

    }
}
