/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.io.*;
import java.util.Hashtable;
import net.cp.engine.BackupException;
import net.cp.engine.PersistentStore;
import net.cp.syncml.client.util.Logger;

/**
 * A class providing access to application settings. <br/><br/>
 *
 * This class provides the methods to parse config file data to create settings, and  read/write settings in persistent storage.
 * Subclasses can be created for specific separate categories of settings. (e.g UI vs Engine).
 *
 * Settings are typically downloaded as a config file from the server and stored in a persistent record store when the
 * application first starts.
 *
 * @author James O'Connor
 */
public abstract class Settings
{
    protected String RMS_SETTINGS_NAME;

    /**
     * Definition of the possible record store version numbers.
     *
     *
     * The version number is stored as a short (15 bits available) where each of the 3 digits
     * of the version number is represented as 5 bits. For example, version "1.1.0" is encoded
     * as "00001 00001 00000" in binary (0x420 hex). Note that version 1.0.0 is encoded as 1 for
     * backward compatibility.
     *
     */
    protected static final short VERSION_1_0_0 =          0x400;  // 00001 00000 00000

    /**
     * The current settings version
     */
    protected static final short VERSION_CURRENT =        VERSION_1_0_0;

    //Definition of the possible record types

    /**
     * Settings that can be changed by the user
     */
    protected static final byte RECORD_TYPE_USER =        1;

    /**
     * Static configuration settings, not usually changed once deployed.
     */
    protected static final byte RECORD_TYPE_CONFIG =      2;

    /**
     * Settings representing the current runtime state.
     */
    protected static final byte RECORD_TYPE_STATE =       3;

    /**
     * Used to access persistent storage.
     */
    protected static PersistentStoreManager storeManager;

    /**
     * Used to log activity
     */
    protected Logger logger;

    /**
     * The record ID of the user settings
     */
    protected int recordIdUser;

    /**
     * The record ID of the static configuration
     */
    protected int recordIdConfig;                   //

    /**
     * The record ID of the runtime state settings
     */
    protected int recordIdState;

    /**
     * The version number of the user settings
     */
    protected short recordVersionUser;

    /**
     * Indicates if the application is being upgraded. Set by some external class that checks for upgrade
     */
    public boolean upgrading;

    /**
     *  Creates new settings - protected to enforce singleton behaviour, but allowing subclass access.
     */
    protected Settings(Logger theLogger)
    {
        logger = theLogger;
        RMS_SETTINGS_NAME = "PB-Settings";
        clear();
    }

    /**
     *  Clears all settings and assigns them their default values.
     */
    protected abstract void clear();

    /**
     *  Writes the user settings to persistent storage.
     */
    public boolean writeUserSettings()
    {
        try
        {
            writeSettingsRms(RMS_SETTINGS_NAME, RECORD_TYPE_USER);
            return true;
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to write user settings to the '" + RMS_SETTINGS_NAME + "' record store", e);
            return false;
        }
    }

    /**
     * Writes the runtime state settings to persistent storage.
     */
    public boolean writeStateSettings()
    {
        try
        {
            writeSettingsRms(RMS_SETTINGS_NAME, RECORD_TYPE_STATE);
            return true;
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to write state settings to the '" + RMS_SETTINGS_NAME + "' record store", e);
            return false;
        }
    }

    /**
     * Writes all settings to persistent storage.
     */
    public boolean writeAllSettings()
    {
        try
        {
            writeSettingsRms(RMS_SETTINGS_NAME, 0);
            return true;
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to all settings to the '" + RMS_SETTINGS_NAME + "' record store", e);
            return false;
        }
    }

    /** Reads the application settings.
     *
     * @param configData config data in "features.properties" format.
     * If null, it will read the settings from the existing RMS entry only.
     * @throws BackupException
     */
    public void readSettings(byte[] configData)
        throws BackupException
    {
        if (logger != null)
            logger.info("Reading application settings");


        //load the settings from the RMS
        readSettingsRms(RMS_SETTINGS_NAME);

        //load the settings from the property file if the user or config settings are missing in the
        //RMS (or if we are doing an upgrade)
        if ( (recordIdUser <= 0) || (recordIdConfig <= 0) || (upgrading) )
        {
            readSettingsResource(configData, upgrading);

            //store these settings in the RMS
            writeSettingsRms(RMS_SETTINGS_NAME, 0);
        }

        //make sure all records were read
        if ( (recordIdUser <= 0) || (recordIdConfig <= 0) || (recordIdState <= 0) )
            throw new BackupException("Application settings are incomplete. recordIdUser: " + recordIdUser + " recordIdConfig: " + recordIdConfig + " recordIdState: " + recordIdState);
    }

    /**
     *  Reads the settings from the record store with the specified name.
     */
    protected synchronized void readSettingsRms(String storeName)
        throws BackupException
    {
        if (logger != null)
            logger.info("Reading settings from the '" + storeName + "' record store");

        PersistentStore recordStore = null;
        try
        {
            //open the store (creating it if necessary)
            recordStore = storeManager.openRecordStore(storeName);
            if (recordStore.getNumRecords() <= 0)
            {
                if (logger != null)
                    logger.debug("Record store is empty - no settings to read");
                return;
            }

            //read and parse each record
            int recordId;
            recordStore.startEnumeration();
            while ((recordId = recordStore.nextRecordId()) > 0)
            {
                byte[] recordData = recordStore.readRecord(recordId);
                readSettingsRms(recordId, recordData);
            }
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to read settings from the '" + storeName + "' record store", e);

            throw new BackupException("Failed to read settings from the '" + storeName + "' record store", e);
        }
        finally
        {
            if (recordStore != null)
                recordStore.stopEnumeration();

            storeManager.closeRecordStore(recordStore);
        }
    }

    /**
     *  Reads the settings from the specified record store data.
     */
    protected abstract void readSettingsRms(int recordId, byte[] recordData)
        throws Exception;

    /**
     * Writes the application settings of the specified type to the record store with the specified name.
     * A record type of 0 indicates that all records should be written.
     * NOTE StoreName must not exceed 32 characters .
     */
    protected abstract void writeSettingsRms(String storeName, int recordType) throws BackupException;

    /**
     * Upgrades the application settings of the specified type from the specified version.
     */
    protected abstract void upgradeSettingsRms(byte recordType, short fromVersion);

    /**
     * Loads the settings from the specified property file.
     */
    protected abstract void readSettingsResource(byte[] configData, boolean upgrade) throws BackupException;

    /**
     * Returns the value of the property with the specified name
     * from the specified properties or from the application JAD.
     */
    protected String getStringProperty(Hashtable<String, String> properties, String name)
        throws BackupException
    {
        String value = null;
        if (properties != null)
            value = (String)properties.get(name);

        if (value == null)
            throw new BackupException("Property '" + name + "' not found ");

        if (logger != null)
            logger.debug("Read property '" + name + "' with value '" + value + "'");

        return value;
    }

    /**
     *  Returns the long value of the property with the specified name
     *  from the specified properties.
     */
    protected long getLongProperty(Hashtable<String, String> properties, String name)
        throws BackupException
    {
        String value = getStringProperty(properties, name);
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException e)
        {
            throw new BackupException("Invalid long value '" + value + "' for property '" + name + "'");
        }
    }

    /**
     * Returns the integer value of the property with the specified name from the specified properties.
     */
    protected int getIntProperty(Hashtable<String, String> properties, String name)
        throws BackupException
    {
        String value = getStringProperty(properties, name);
        try
        {
            if (value.startsWith("0x"))
                return Integer.parseInt(value.substring(2), 16);

            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            throw new BackupException("Invalid integer value '" + value + "' for property '" + name + "'");
        }
    }

    /**
     * Returns the boolean value of the property with the specified name
     * from the specified properties
     */
    protected boolean getBooleanProperty(Hashtable<String, String> properties, String name)
        throws BackupException
    {
        String value = getStringProperty(properties, name);
        if (value.equalsIgnoreCase("true"))
            return true;
        else if (value.equalsIgnoreCase("false"))
            return false;
        else
            throw new BackupException("Invalid boolean value '" + value + "' for property '" + name + "'");
    }


    /**
     * Returns TRUE if the specified bit value is set in the specified bit field.
     * @param bitField the bitField to check
     * @param bitValue the bit value to check for
     */
    public boolean isFlagSet(int bitField, int bitValue)
    {
        return ((bitField & bitValue) == bitValue);
    }

    /**
     * Sets or unsets the specified bit value in the specified bit field.
     */
    public int setFlag(int bitField, int bitValue, boolean set)
    {
        //set the specified bit in the bit mask
        if (set)
            return (bitField | bitValue);

        //unset the specified bit in the bit mask
        return (bitField & (~bitValue));
    }

    /**
     * Parses the supplied config data and returns a Hastable of the results.
     *
     * @param propertyData The config data in "features.properties" format.
     */
    protected Hashtable<String, String> readProperties(byte[] propertyData)
        throws BackupException
    {
        if (logger != null)
            logger.info("Reading properties from inputstream");

        ByteArrayInputStream propertyStream = null;
        try
        {
            //get the property data
            if (propertyData == null)
                return null;

            //open the input stream and read the style properties from the file
            propertyStream = new ByteArrayInputStream(propertyData);
            return readProperties(propertyStream, "UTF-8", logger);
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to read properties from inputstream. propertyData.length: " + propertyData.length, e);

            throw new BackupException("Failed to read properties from inputstream", e);
        }
        finally
        {
            UtilityClass.streamClose(propertyStream, logger);
        }
    }

    /**
     * @param inputStream Inputstream containing data in the "features.properties" format.
     * @param charset The character set to use to read teh strings.
     * @param logger The logger to use.
     * @return A hash table of properties.
     * @throws IOException thrown if there is a problem reading from the stream.
     */
    public static Hashtable<String, String> readProperties(InputStream inputStream, String charset, Logger logger)
    throws IOException
    {
        String propertyLine = null;
        Hashtable<String, String> properties = new Hashtable<String, String>();

        while ((propertyLine = UtilityClass.readNextLine(inputStream, charset, false)) != null)
        {
            //ignore empty or commented lines
            String trimmedLine = propertyLine.trim();
            if ( (trimmedLine.length() <= 0) || (trimmedLine.startsWith("#")) )
                continue;

            int index = propertyLine.indexOf('=');
            if (index <= 0)
                throw new IOException("Invalid property '" + propertyLine + "' found in property RMS");

            String value = "";
            String name = propertyLine.substring(0, index);
            if (index < (propertyLine.length() - 1))
                value = propertyLine.substring(index + 1);

            properties.put(name, value);
        }

        return properties;
    }

    /**
     * @return The persistent store manager used by this class
     */
    public static PersistentStoreManager getPersistentStoreManager()
    {
        return storeManager;
    }

    /**
     * Often the loggger can't be supplied to the constructor, as the logging settings themselves need to be read by this class!
     * This method allows us to set the logger as soon as we have it, so that we can seee what's happening in the class.
     * @param logger
     */
    public void setLogger(Logger logger)
    {
        this.logger = logger;
    }
}
