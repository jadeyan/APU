/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Hashtable;

import net.cp.ac.core.AndroidPersistentStoreManager;
import net.cp.syncml.client.devinfo.ContentType;
import net.cp.syncml.client.util.Logger;
import net.cp.system.NetworkIdentifier;
import android.content.Context;
import android.os.Build;

/**
 * A class providing access to Service/Engine related settings. <br/><br/>
 *
 * Settings are typically downloaded from the server and stored in a persistent record store when the
 * application first starts.
 *
 * @author James O'Connor
 */
public class EngineSettings extends Settings {
    // Definition of the possible media types that can be synced
    public static final int MEDIA_TYPE_NONE = 0;
    public static final int MEDIA_TYPE_CONTACTS = 1;
    public static final int MEDIA_TYPE_MUSIC = 2;
    public static final int MEDIA_TYPE_VIDEOS = 4;
    public static final int MEDIA_TYPE_IMAGES = 8;
    public static final int MEDIA_TYPE_OTHERS = 16;

    // Each sync mode can have one of the following 3 values
    public static final byte SYNC_MODE_AUTO = 2;
    public static final byte SYNC_MODE_REMIND = 1;
    public static final byte SYNC_MODE_OFF = 0;

    // Definition of the available time units
    public static final byte TIME_UNITS_NONE = 0;
    public static final byte TIME_UNITS_SECONDS = 1;
    public static final byte TIME_UNITS_MINUTES = 2;
    public static final byte TIME_UNITS_HOURS = 4;
    public static final byte TIME_UNITS_DAYS = 8;

    // Definition of the available conflict resolution methods
    public static final byte CONFLICT_RES_NONE = 0;
    public static final byte CONFLICT_RES_RECENT_WINS = 1;
    public static final byte CONFLICT_RES_SERVER_WINS = 2;
    public static final byte CONFLICT_RES_CLIENT_WINS = 4;
    public static final byte CONFLICT_RES_DUPLICATE = 8;
    public static final byte CONFLICT_RES_IGNORE = 16;

    // Definition of the possible sync status
    public static final byte SYNC_STATUS_NONE = 0;
    public static final byte SYNC_STATUS_SUCCESS = 1;
    public static final byte SYNC_STATUS_FAIL = 2;
    public static final byte SYNC_STATUS_PARTIAL = 4;

    // Definition of the possible log types
    public static final int LOG_TYPE_NONE = 0;
    public static final int LOG_TYPE_LOGCAT = 1;
    public static final int LOG_TYPE_FILE = 2;

    // Definition of the possible log levels
    public static final int LOG_LEVEL_NONE = 0;
    public static final int LOG_LEVEL_ERROR = 1;
    public static final int LOG_LEVEL_WARNING = 2;
    public static final int LOG_LEVEL_DEBUG = 4;
    public static final int LOG_LEVEL_INFO = 8;

    // Definition of how the sync was initiated
    public static final byte SYNC_TYPE_MANUAL = 1;
    public static final byte SYNC_TYPE_SIS = 2;
    public static final byte SYNC_TYPE_CIS = 4;

    // Keys used for bundles on starting Sync activity...
    public static final String SYNC_TYPE = "SYNC-TYPE";
    public static final String SYNC_MESSAGE = "SYNC-MESSAGE";

    // Definition of the prefixes used when specifying home networks
    private static final String NETWORK_CC_ISO3666_PREFIX = "CISO=";
    private static final String NETWORK_CC_ITU_PREFIX = "CITU=";
    private static final String NETWORK_ID_PREFIX = "NID=";

    // Definition of the prefix used in the device ID
    protected static final String DEVICE_ID_PREFIX = "PBC-";

    // Bootstrap settings read from the application JAD file
    public String logFilePath;                      // the path where the application log file should be created
    public String resourcePath;                     // the path where the application resources should be created

    // User settings (which may be changed by the user)
    public String userName;                         // the users name
    public String userPassword;                     // the users password
    public String userDomainDefault;                // the default user domain if one isn't explicitly entered by the user

    public String httpSyncServerAddress;            // the address (IP or hostname) of the SyncML server
    public int httpSyncServerPort;                  // the port number of the SyncML server HTTP port
    public boolean httpUseSSL;                      // use SSL to connect to the server?

    public int logType;                             // the type of logging that should be used (file, logcat etc...)
    public int logLevel;                            // the level of logging to be used

    public boolean ignoreSyncModes;                 // if set to true, we won't check which network interface we use to sync. This can increase performance dramatically.
    public int syncModeNoCost;                      // the sync mode (SYNC_MODE_XXX) to use with no cost connections
    public int syncModeNormalCost;                  // the sync mode (SYNC_MODE_XXX) to use with normal cost connections
    public int syncModeHighCost;                    // the sync mode (SYNC_MODE_XXX) to use with high cost connections

    public long changeTimeout;                       // the time to wait after a change has been detected, before initiating a sync

    public int syncSelectedMediaTypes;              // a bit mask of the types of media (MEDIA_TYPE_XXX) that the user has selected to sync

    public byte contactConflictResolution;          // the conflict resolution method to use when syncing contacts

    public boolean sisEnable;                       // indicates if the application should accept/or reject SIS requests

    public int contactMinSyncLimit;                 // the minimum number of contacts that must have changed before an client initiated sync will be performed

    // Static configuration (usually configured by the operator)
    public long upgradeCheckInterval;               // the time interval (in seconds) between upgrade checks

    public String deviceType;                       // the type of device (phone, PDA, etc)
    public String deviceManf;                       // the manufacturer of the device
    public String deviceModel;                      // the model of the device
    public String deviceOem;                        // the OEM of the device
    public String deviceHwVersion;                  // the version number of the device hardware
    public String deviceFwVersion;                  // the version number of the device firmware

    public String appCapabilityId;                  // the capability ID associated with the application (used as permissions check by the SyncML server when syncing)

    public String httpSyncServerPath;               // the path of the SyncML server
    public int httpConnectionTimeout;              // the maximum amount of time (in milliseconds) to wait for a response from the server
    public int httpMaxMessageSize;                  // the maximum HTTP message size in bytes

    public String contactStoreServerUri;            // the URI of the contact store on the server that contacts should be synced to
    public int contactStoreMaxContacts;             // the maximum number of contacts that can be synced

    public int syncAllowedMediaTypes;               // a bitmask of the types of media (MEDIA_TYPE_XXX) that the user is allowed to sync

    public boolean contactCisAllowed;               // indicates if the application should sync on changes
    public boolean contactSisAllowed;               // indicates if the application should listen for SIS requests for contact syncing

    public int sisPort;                             // the port number on which the application should listen for SIS requests

    public String deviceDisplayName;                // the display name of the device (as the manufacturer and model combination may not always be pretty)

    /**
     * A string containing the home networks description
     */
    public String homeNetworks;

    /**
     * Network identifiers holding the home networks description
     */
    public NetworkIdentifier homeNetworkIdentifiers[];

    public String defaultClientNonce;               // the default client nonce to use when validating server alerts - only used if a real nonce is not available

    public boolean suspendResumeAllowed;            // indicates if the application should support suspend/resume

    public int resumeRetryTimeout;                  // number of seconds to wait before trying to resume an unintentionally suspended session
    public int resumeRetryCount;                    // number of times to try resuming an unintentionally suspended session
    public int abortTimeout;                        // number of seconds to wait before aborting a manually suspended session

    /**
     *  The minimum battery level in percent that is required before a sync can be initiated
     *  If the level is below this for a manual sync, the user is prompted, for automatic, the sync never starts.
     */
    public int minBatteryLevel;

    // Runtime state
    public String appVersion;                       // the version of the application
    public boolean initialLaunch;                   // indicates if the application has ever been run yet

    public String lastDownloadedUrl;                // the URL of the last resource file that was successfully downloaded

    public long lastUpgradeCheckTime;               // the time when the last upgrade check was performed

    public String deviceId;                         // the unique ID of the device (automatically generated)

    public long lastSyncDate;                       // the date when the last sync operation took place
    public byte lastSyncStatus;                     // the status (SYNC_STATUS_XXX) of the last sync operation
    public String lastSyncError;                    // the localized error which caused the last sync to fail
    public int syncCount;                           // the total number of sync operations that have been performed

    public long lastPeriodicSyncTime;               // the time of the last registered periodic sync alarm

    public String serverNonce;                      // the last authentication nonce that was received from the server

    public String clientNonce;                      // the last authentication nonce that was sent to the server
    public String oldClientNonce;                   // the previous authentication nonce that was sent to the server

    public String timeStamp;                        // the time stamp for when the APP was downloaded
    public int lastSyncMediaTypes;                  // the media types (MEDIA_TYPE_XXX) which were synced during the last sync

    private boolean showPreSyncPage;                // indicates if the app should display the pre sync page

    private boolean showHostname;                   // is the user allowed configure the hostname setting?
    private boolean showPort;                       // is the user allowed configure the port setting?
    private boolean showSSL;                        // is the user allowed configure the useSSL setting?
    private boolean showSIS;                        // is the user allowed configure the Online Sync Requests setting?

    private String helpFileLocation;                // the location where the help file is found -- not in use in program APU

    protected static EngineSettings instance = null;  // the single instance of the settings

    public boolean syncReadOnlyContacts;

    public boolean periodicAllowed;                 // indicates if the application should sync by period

    public int periodicSyncDaysLimits;              // the number of days interval to sync periodic

    public int periodicSyncHourOfDay;               // the hour user set for periodic sync

    public int periodicSyncMinute;                  // the minute user set for periodic sync

    public int dateOfLastPeriodicSync;              // record the date of last periodic sync

    public int contactCacheSize;                    // size of the contact cache

    /* Creates new settings - protected to enforce singleton behavior. */
    protected EngineSettings(Logger theLogger) {
        super(theLogger);
        RMS_SETTINGS_NAME = "PB-EngineSettings";
    }

    /* Clears all settings and assigns them their default values. */
    @Override
    protected void clear() {
        if (logger != null) logger.info("Clearing application settings");

        recordIdUser = 0;
        recordIdConfig = 0;
        recordIdState = 0;
        recordVersionUser = 0;

        resourcePath = null;

        // user settings
        userName = "";
        userPassword = "";
        userDomainDefault = "";

        httpSyncServerAddress = "";
        httpSyncServerPort = 0;
        httpUseSSL = false;

        logFilePath = null;
        logType = 0;
        logLevel = 0;

        ignoreSyncModes = false;
        syncModeNoCost = SYNC_MODE_OFF;
        syncModeHighCost = SYNC_MODE_OFF;

        changeTimeout = 0;

        syncSelectedMediaTypes = MEDIA_TYPE_NONE;

        contactConflictResolution = CONFLICT_RES_NONE;

        sisEnable = false;

        contactMinSyncLimit = 0;

        // static configuration
        upgradeCheckInterval = 0;

        deviceType = "";
        deviceManf = "";
        deviceModel = "";
        deviceOem = "";
        deviceHwVersion = "";
        deviceFwVersion = "";

        appCapabilityId = "";

        httpSyncServerPath = "";
        httpConnectionTimeout = 0;
        httpMaxMessageSize = 0;

        contactStoreServerUri = "";
        contactStoreMaxContacts = 0;

        syncAllowedMediaTypes = MEDIA_TYPE_NONE;

        contactCisAllowed = false;
        contactSisAllowed = false;
        sisPort = 0;
        deviceDisplayName = "";

        homeNetworks = "";
        homeNetworkIdentifiers = new NetworkIdentifier[0];

        defaultClientNonce = "";
        suspendResumeAllowed = false;

        resumeRetryTimeout = 0;
        resumeRetryCount = 0;
        abortTimeout = 0;

        minBatteryLevel = 0;

        // runtime state
        appVersion = "";
        initialLaunch = true;

        lastDownloadedUrl = "";

        upgrading = false;
        lastUpgradeCheckTime = 0;

        deviceId = "";

        lastSyncDate = 0;
        lastSyncStatus = SYNC_STATUS_NONE;
        lastSyncError = "";
        syncCount = 0;

        lastPeriodicSyncTime = 0;

        serverNonce = "";

        clientNonce = "";
        oldClientNonce = "";

        timeStamp = "";
        lastSyncMediaTypes = MEDIA_TYPE_NONE;

        showPreSyncPage = true;

        showHostname = true;
        showPort = true;
        showSSL = true;
        showSIS = true;

        helpFileLocation = "";

        syncReadOnlyContacts = false;
        periodicAllowed = false;
        periodicSyncDaysLimits = 1;
        periodicSyncHourOfDay = 0;
        periodicSyncMinute = 0;

        contactCacheSize = 0;
    }

    /** Returns the single instance of the settings. */
    public static EngineSettings getInstance() {
        return instance;
    }

    public static EngineSettings getInstance(Context context, Logger theLogger) {
        if (instance == null) {
            instance = new EngineSettings(theLogger);
        }

        try {
            instance.readSettings(AndroidPersistentStoreManager.getConfigFromStore(context, theLogger));
        } catch (BackupException e) {
            if (theLogger != null) theLogger.error("Error getting EngineSettings instance");
        }

        return instance;
    }

    /** Initializes the settings. */
    public synchronized static EngineSettings init(byte[] configData, PersistentStoreManager manager, Logger theLogger) throws BackupException {
        storeManager = manager;
        // create the single instance of the settings if necessary
        if (instance == null) {
            if (theLogger != null) theLogger.info("Initializing application settings");
            instance = new EngineSettings(theLogger);
            instance.readSettings(configData);
        }

        return instance;
    }

    /** Reinitialises the settings. */
    public synchronized static void reinit(byte[] configData, PersistentStoreManager manager, Logger theLogger) throws BackupException {
        if (instance == null) return;

        storeManager = manager;

        // just clear and read the settings again
        if (theLogger != null) theLogger.info("Reinitializing application settings");

        synchronized (instance) {
            Logger iLogger = instance.logger;
            instance.logger = null;
            instance.clear();
            instance.readSettings(configData);
            instance.logger = iLogger;
        }
    }

    /** Closes the settings. */
    public synchronized void close() {
        if (instance != null && instance.logger != null) instance.logger.info("Closing application settings");

        instance = null;
    }

    /** Returns TRUE if the user has selected the specified media type for syncing. */
    public boolean isSelectedMediaType(int mediaType) {
        return isFlagSet(syncSelectedMediaTypes, mediaType);
    }

    /** Sets or unsets the specified media type for syncing. */
    public void setSelectedMediaType(int mediaType, boolean set) {
        syncSelectedMediaTypes = setFlag(syncSelectedMediaTypes, mediaType, set);
    }

    /** Returns TRUE if the user has selected contacts for syncing. */
    public boolean isSelectedContactSync() {
        return isSelectedMediaType(MEDIA_TYPE_CONTACTS);
    }

    /** Returns TRUE if the user has selected content for syncing. */
    public boolean isSelectedContentSync() {
        return (syncSelectedMediaTypes > 1);
    }

    /** Returns TRUE if the user is allowed to sync the specified media type. */
    public boolean isAllowedMediaType(int mediaType) {
        return isFlagSet(syncAllowedMediaTypes, mediaType);
    }

    /** Returns TRUE if the user is allowed to sync the specified content type. */
    public boolean isAllowedMediaType(ContentType contentType) {
        int mediaType = MEDIA_TYPE_OTHERS;
        if (contentType != null) {
            String mainType = contentType.getMainType();
            if (mainType != null) {
                if (mainType.equalsIgnoreCase("audio"))
                    mediaType = MEDIA_TYPE_MUSIC;
                else if (mainType.equalsIgnoreCase("video"))
                    mediaType = MEDIA_TYPE_VIDEOS;
                else if (mainType.equalsIgnoreCase("image")) mediaType = MEDIA_TYPE_IMAGES;
            }
        }

        return isFlagSet(syncAllowedMediaTypes, mediaType);
    }

    /** Returns TRUE if the user is allowed to sync contacts. */
    public boolean isAllowContactSync() {
        return isAllowedMediaType(MEDIA_TYPE_CONTACTS);
    }

    /** Returns TRUE if the user is allowed to sync content. */
    public boolean isAllowContentSync() {
        return (syncAllowedMediaTypes > 1);
    }

    /** Returns the content media types that the user is allowed to sync (if any). */
    public int getAllowedContentMediaTypes() {
        if (!isAllowContentSync()) return MEDIA_TYPE_NONE;

        // return all allowed media types except contacts
        return (syncAllowedMediaTypes & (~MEDIA_TYPE_CONTACTS));
    }

    /** Returns whether or not SIS functionality is allowed by the operator. */
    public boolean isSisAllowed() {
        return contactSisAllowed;
    }

    /** Returned whether or not CIS functionality us allowed by the user. */
    public boolean isCisAllowed() {
        return contactCisAllowed;
    }

    /** Reads the application settings from the specified record store data. */
    @Override
    protected void readSettingsRms(int recordId, byte[] recordData) throws Exception {
        ByteArrayInputStream byteStream = null;
        DataInputStream dataStream = null;
        try {
            byteStream = new ByteArrayInputStream(recordData);
            dataStream = new DataInputStream(byteStream);

            // make sure the version is correct
            short version = dataStream.readShort();
            if ((version <= 0) || (version > VERSION_CURRENT)) throw new BackupException("Invalid version '" + version + "' found");

            // read the data based on the record type
            byte recordType = dataStream.readByte();
            if (recordType == RECORD_TYPE_USER) {
                if (logger != null) logger.debug("Reading user settings (version '" + version + "') from the record store");

                userName = dataStream.readUTF();
                userPassword = dataStream.readUTF();
                userDomainDefault = dataStream.readUTF();

                httpSyncServerAddress = dataStream.readUTF();
                httpSyncServerPort = dataStream.readInt();
                httpUseSSL = dataStream.readBoolean();

                ignoreSyncModes = dataStream.readBoolean();
                syncModeNoCost = dataStream.readInt();
                syncModeNormalCost = dataStream.readInt();
                syncModeHighCost = dataStream.readInt();

                changeTimeout = dataStream.readLong();

                syncSelectedMediaTypes = dataStream.readByte();

                contactConflictResolution = dataStream.readByte();

                sisEnable = dataStream.readBoolean();

                contactMinSyncLimit = dataStream.readInt();

                logFilePath = dataStream.readUTF();
                logType = dataStream.readInt();
                logLevel = dataStream.readInt();

                syncReadOnlyContacts = dataStream.readBoolean();

                periodicSyncDaysLimits = dataStream.readInt();
                periodicSyncHourOfDay = dataStream.readInt();
                periodicSyncMinute = dataStream.readInt();

                // perform any other upgrade steps if necessary
                if (version < VERSION_CURRENT) upgradeSettingsRms(recordType, version);

                // remember the record ID so we can update the record later
                recordIdUser = recordId;
                recordVersionUser = version;
            } else if (recordType == RECORD_TYPE_CONFIG) {
                if (logger != null) logger.debug("Reading config settings (version '" + version + "') from the record store");

                upgradeCheckInterval = dataStream.readLong();

                deviceType = dataStream.readUTF();
                deviceManf = dataStream.readUTF();
                deviceModel = dataStream.readUTF();
                deviceOem = dataStream.readUTF();
                deviceHwVersion = dataStream.readUTF();

                deviceFwVersion = dataStream.readUTF();

                appCapabilityId = dataStream.readUTF();

                httpSyncServerPath = dataStream.readUTF();
                httpConnectionTimeout = dataStream.readInt();
                httpMaxMessageSize = dataStream.readInt();

                contactStoreServerUri = dataStream.readUTF();
                contactStoreMaxContacts = dataStream.readInt();

                syncAllowedMediaTypes = dataStream.readInt();

                contactCisAllowed = dataStream.readBoolean();
                contactSisAllowed = dataStream.readBoolean();
                sisPort = dataStream.readInt();

                deviceDisplayName = dataStream.readUTF();

                // read home networks
                homeNetworks = dataStream.readUTF();

                homeNetworkIdentifiers = parseNetworks(homeNetworks);

                defaultClientNonce = dataStream.readUTF();
                suspendResumeAllowed = dataStream.readBoolean();

                resumeRetryTimeout = dataStream.readInt();
                resumeRetryCount = dataStream.readInt();
                abortTimeout = dataStream.readInt();

                minBatteryLevel = dataStream.readInt();

                showPreSyncPage = dataStream.readBoolean();

                showHostname = dataStream.readBoolean();
                showPort = dataStream.readBoolean();
                showSSL = dataStream.readBoolean();
                showSIS = dataStream.readBoolean();

                contactCacheSize = dataStream.readInt();

                helpFileLocation = dataStream.readUTF();

                periodicAllowed = dataStream.readBoolean();

                // perform any other upgrade steps if necessary
                if (version < VERSION_CURRENT) upgradeSettingsRms(recordType, version);

                // remember the record ID so we can update the record later
                recordIdConfig = recordId;
            } else if (recordType == RECORD_TYPE_STATE) {
                if (logger != null) logger.debug("Reading state settings (version '" + version + "') from the record store");

                appVersion = dataStream.readUTF();
                initialLaunch = dataStream.readBoolean();

                lastDownloadedUrl = dataStream.readUTF();

                upgrading = dataStream.readBoolean();
                lastUpgradeCheckTime = dataStream.readLong();

                deviceId = dataStream.readUTF();

                lastSyncDate = dataStream.readLong();
                lastSyncStatus = dataStream.readByte();
                lastSyncError = dataStream.readUTF();
                syncCount = dataStream.readInt();
                lastPeriodicSyncTime = dataStream.readLong();
                serverNonce = dataStream.readUTF();
                clientNonce = dataStream.readUTF();
                oldClientNonce = dataStream.readUTF();

                timeStamp = dataStream.readUTF();
                lastSyncMediaTypes = dataStream.readInt();

                // perform any other upgrade steps if necessary
                if (version < VERSION_CURRENT) upgradeSettingsRms(recordType, version);

                // remember the record ID so we can update the record later
                recordIdState = recordId;
            } else {
                throw new BackupException("Invalid record type '" + recordType + "' found");
            }
        } finally {
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /**
     * Writes the application settings of the specified type to the record store with the specified name. A record type of 0 indicates tht all records should be written.
     * NOTE StoreName must not exceed 32 characters
     */
    @Override
    protected synchronized void writeSettingsRms(String storeName, int recordType) throws BackupException {
        if (logger != null) logger.info("Writing settings to the '" + storeName + "' record store");

        PersistentStore recordStore = null;
        ByteArrayOutputStream byteStream = null;
        DataOutputStream dataStream = null;
        try {
            // open the store (creating it if necessary)
            recordStore = storeManager.openRecordStore(storeName);

            // create the necessary streams
            byteStream = new ByteArrayOutputStream();
            dataStream = new DataOutputStream(byteStream);

            // write the user settings if required
            if ((recordType == 0) || (recordType == RECORD_TYPE_USER)) {
                if (logger != null) logger.debug("Writing user settings (version '" + VERSION_CURRENT + "') to the record store");

                // write the current version number and record type
                byteStream.reset();
                dataStream.writeShort(VERSION_CURRENT);
                dataStream.writeByte(RECORD_TYPE_USER);

                dataStream.writeUTF(userName);

                dataStream.writeUTF(userPassword);

                dataStream.writeUTF(userDomainDefault);

                dataStream.writeUTF(httpSyncServerAddress);
                dataStream.writeInt(httpSyncServerPort);
                dataStream.writeBoolean(httpUseSSL);

                dataStream.writeBoolean(ignoreSyncModes);
                dataStream.writeInt(syncModeNoCost);
                dataStream.writeInt(syncModeNormalCost);
                dataStream.writeInt(syncModeHighCost);

                dataStream.writeLong(changeTimeout);

                dataStream.writeByte(syncSelectedMediaTypes);

                dataStream.writeByte(contactConflictResolution);

                // write user settings added in 1.2.0
                dataStream.writeBoolean(sisEnable);

                dataStream.writeInt(contactMinSyncLimit);

                dataStream.writeUTF(logFilePath);
                dataStream.writeInt(logType);
                dataStream.writeInt(logLevel);

                dataStream.writeBoolean(syncReadOnlyContacts);
                dataStream.writeInt(periodicSyncDaysLimits);
                dataStream.writeInt(periodicSyncHourOfDay);
                dataStream.writeInt(periodicSyncMinute);

                // write the record
                byte[] recordData = byteStream.toByteArray();
                recordIdUser = recordStore.writeRecord(recordIdUser, recordData);
                recordVersionUser = VERSION_CURRENT;
            }

            // write the static config settings if required
            if ((recordType == 0) || (recordType == RECORD_TYPE_CONFIG)) {
                if (logger != null) logger.debug("Writing config settings (version '" + VERSION_CURRENT + "') to the record store");

                // write the current version number and record type
                byteStream.reset();
                dataStream.writeShort(VERSION_CURRENT);
                dataStream.writeByte(RECORD_TYPE_CONFIG);

                dataStream.writeLong(upgradeCheckInterval);

                dataStream.writeUTF(deviceType);
                dataStream.writeUTF(deviceManf);
                dataStream.writeUTF(deviceModel);
                dataStream.writeUTF(deviceOem);
                dataStream.writeUTF(deviceHwVersion);

                dataStream.writeUTF(deviceFwVersion);

                dataStream.writeUTF(appCapabilityId);

                dataStream.writeUTF(httpSyncServerPath);
                dataStream.writeInt(httpConnectionTimeout);
                dataStream.writeInt(httpMaxMessageSize);

                dataStream.writeUTF(contactStoreServerUri);
                dataStream.writeInt(contactStoreMaxContacts);

                dataStream.writeInt(syncAllowedMediaTypes);

                dataStream.writeBoolean(contactCisAllowed);
                dataStream.writeBoolean(contactSisAllowed);
                dataStream.writeInt(sisPort);

                dataStream.writeUTF(deviceDisplayName);

                dataStream.writeUTF(homeNetworks);

                dataStream.writeUTF(defaultClientNonce);
                dataStream.writeBoolean(suspendResumeAllowed);

                dataStream.writeInt(resumeRetryTimeout);
                dataStream.writeInt(resumeRetryCount);
                dataStream.writeInt(abortTimeout);

                dataStream.writeInt(minBatteryLevel);

                dataStream.writeBoolean(showPreSyncPage);

                dataStream.writeBoolean(showHostname);
                dataStream.writeBoolean(showPort);
                dataStream.writeBoolean(showSSL);
                dataStream.writeBoolean(showSIS);

                dataStream.writeInt(contactCacheSize);

                dataStream.writeUTF(helpFileLocation);

                dataStream.writeBoolean(periodicAllowed);
                // write the record
                byte[] recordData = byteStream.toByteArray();
                recordIdConfig = recordStore.writeRecord(recordIdConfig, recordData);
            }

            // write the runtime state settings if required
            if ((recordType == 0) || (recordType == RECORD_TYPE_STATE)) {
                if (logger != null) logger.debug("Writing state settings (version '" + VERSION_CURRENT + "') to the record store");

                // write the current version number and record type
                byteStream.reset();
                dataStream.writeShort(VERSION_CURRENT);
                dataStream.writeByte(RECORD_TYPE_STATE);

                dataStream.writeUTF(appVersion);
                dataStream.writeBoolean(initialLaunch);

                dataStream.writeUTF(lastDownloadedUrl);

                dataStream.writeBoolean(upgrading);
                dataStream.writeLong(lastUpgradeCheckTime);

                dataStream.writeUTF(deviceId);

                dataStream.writeLong(lastSyncDate);
                dataStream.writeByte(lastSyncStatus);
                dataStream.writeUTF(lastSyncError);
                dataStream.writeInt(syncCount);

                dataStream.writeLong(lastPeriodicSyncTime);

                dataStream.writeUTF(serverNonce);

                dataStream.writeUTF(clientNonce);
                dataStream.writeUTF(oldClientNonce);

                dataStream.writeUTF(timeStamp);
                dataStream.writeInt(lastSyncMediaTypes);

                // write the record
                byte[] recordData = byteStream.toByteArray();
                recordIdState = recordStore.writeRecord(recordIdState, recordData);
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to write settings to the '" + storeName + "' record store", e);

            throw new BackupException("Failed to write settings to the '" + storeName + "' record store", e);
        } finally {
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
            storeManager.closeRecordStore(recordStore);
        }
    }

    /**
     * TODO complete settings upgrade
     * Upgrades the application settings of the specified type from the specified version.
     */
    @Override
    protected void upgradeSettingsRms(byte recordType, short fromVersion) {

    }

    /* Loads the settings from the specified property file. */
    @Override
    protected void readSettingsResource(byte[] configData, boolean upgrade) throws BackupException {
        if (logger != null) logger.info("Reading settings from the config data - upgrade=" + upgrade);

        try {
            // load the properties
            Hashtable<String, String> properties = readProperties(configData);

            // read default user settings - only do this if we are not upgrading
            if (!upgrade) {
                userDomainDefault = getStringProperty(properties, "user.default.domain");
                httpSyncServerAddress = getStringProperty(properties, "user.default.serverAddress");
                httpUseSSL = getBooleanProperty(properties, "user.default.useSSL");
                userName = buildUsername(getStringProperty(properties, "user.default.userName"), userDomainDefault);
                userPassword = getStringProperty(properties, "user.default.password");
                httpSyncServerPort = getIntProperty(properties, "user.default.serverPort");
                logFilePath = getStringProperty(properties, "config.app.logFilePath");
                logType = getIntProperty(properties, "config.app.logType");
                logLevel = getIntProperty(properties, "config.app.logLevel");
                timeStamp = getStringProperty(properties, "config.app.timeStamp");
                ignoreSyncModes = getBooleanProperty(properties, "user.default.ignoreSyncModes");
                syncModeNoCost = getIntProperty(properties, "user.default.noCostSyncMode");
                syncModeNormalCost = getIntProperty(properties, "user.default.normalCostSyncMode");
                syncModeHighCost = getIntProperty(properties, "user.default.highCostSyncMode");
                syncReadOnlyContacts = getBooleanProperty(properties, "user.default.contact.syncReadOnlyContacts");
                deviceId = getDeviceId();

                // specified in seconds in config file, stored as milliseconds
                changeTimeout = 1000 * getIntProperty(properties, "user.default.contact.changeTimeout");

                contactConflictResolution = (byte) getIntProperty(properties, "user.default.contact.conflictRes");
            }

            sisEnable = getBooleanProperty(properties, "user.default.sisEnable");

            contactMinSyncLimit = getIntProperty(properties, "user.default.contact.minSyncLimit");

            // read static configuration settings
            upgradeCheckInterval = getLongProperty(properties, "config.upgrade.checkInterval");

            deviceType = getStringProperty(properties, "config.device.type");
            deviceManf = getStringProperty(properties, "config.device.manufacturer");

            if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ECLAIR)
                deviceModel = getStringProperty(properties, "config.device.model.cupcake");
            else
                deviceModel = getStringProperty(properties, "config.device.model.eclair");

            deviceOem = getStringProperty(properties, "config.device.oem");
            deviceHwVersion = getStringProperty(properties, "config.device.hwVersion");
            deviceFwVersion = getStringProperty(properties, "config.device.fwVersion");

            appCapabilityId = getStringProperty(properties, "config.app.capabilityId");

            httpSyncServerPath = getStringProperty(properties, "config.server.path");
            httpConnectionTimeout = getIntProperty(properties, "config.server.receiveTimeout");
            httpMaxMessageSize = getIntProperty(properties, "config.server.maxMessageSize");

            contactStoreServerUri = getStringProperty(properties, "config.contact.serverUri");
            contactStoreMaxContacts = getIntProperty(properties, "config.contact.maxContacts");

            syncAllowedMediaTypes = getIntProperty(properties, "config.app.allowedMediaTypes");

            contactCisAllowed = getBooleanProperty(properties, "config.contact.cisAllow");
            contactSisAllowed = getBooleanProperty(properties, "config.contact.sisAllow");
            sisPort = getIntProperty(properties, "config.app.sisPort");

            deviceDisplayName = getStringProperty(properties, "config.device.name");

            homeNetworks = getStringProperty(properties, "config.device.homeNetworks");

            defaultClientNonce = getStringProperty(properties, "config.app.defaultClientNonce");
            suspendResumeAllowed = getBooleanProperty(properties, "config.app.suspendResumeAllow");

            resumeRetryTimeout = getIntProperty(properties, "config.app.resumeRetryTimeout");
            resumeRetryCount = getIntProperty(properties, "config.app.resumeRetryCount");
            abortTimeout = getIntProperty(properties, "config.app.abortTimeout");

            minBatteryLevel = getIntProperty(properties, "config.app.minimumBatteryLevel");

            showPreSyncPage = getBooleanProperty(properties, "config.app.showPreSync");

            showHostname = getBooleanProperty(properties, "config.app.showHostname");
            showPort = getBooleanProperty(properties, "config.app.showPort");
            showSSL = getBooleanProperty(properties, "config.app.showSSL");
            showSIS = getBooleanProperty(properties, "config.app.showSIS");

            contactCacheSize = getIntProperty(properties, "config.contact.cache.size");

            helpFileLocation = getStringProperty(properties, "config.app.helpfile");

            periodicAllowed = getBooleanProperty(properties, "config.contact.periodicAllowed");

            periodicSyncDaysLimits = getIntProperty(properties, "user.default.contact.periodicSyncDayLimits");

            periodicSyncHourOfDay = getIntProperty(properties, "user.default.contact.periodicSyncHourOfDay");

            periodicSyncMinute = getIntProperty(properties, "user.default.contact.periodicSyncMinute");
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to read settings from the supplied inputstream", e);

            throw new BackupException("Failed to read settings from the supplied inputstream", e);
        }
    }

    /** Returns the full username including the default domain name (if it doesn't already contain a domain). */
    public static String buildUsername(String username) {
        return buildUsername(username, instance.userDomainDefault);
    }

    /** Returns the full username including the specified domain name (if it doesn't already contain a domain). */
    public static String buildUsername(String username, String domain) {
        if ((username == null) || (username.length() <= 0)) return "";

        // nothing more to do if the username already contains a domain
        if (username.indexOf('@') >= 0) return username;

        // add the specified domain to the username
        if ((domain != null) && (domain.length() > 0)) username = username + domain;

        return username;
    }

    /* Parses the specified string into separate codes. */
    private String[] parseNetworkCodes(String str) throws BackupException {
        if (str == null || str.length() <= 0) throw new BackupException("Invalid home network codes - no codes found");

        // each code is separated by the '|' character
        String[] codes = UtilityClass.split(str, "|");

        if ((codes == null) || (codes.length <= 0)) throw new BackupException("Invalid home network codes - no codes found");

        // make sure each code is valid
        for (String code : codes) {
            if ((code == null) || (code.length() <= 0)) throw new BackupException("Invalid home network codes - missing code");
        }

        return codes;
    }

    /* Parse the specified home network description. */
    private NetworkIdentifier[] parseNetworks(String networksIdStr) throws BackupException {
        if (networksIdStr == null || networksIdStr.length() <= 0) return null;

        // each network identifier is separated by the ',' character
        String[] networkIdStrs = UtilityClass.split(networksIdStr, ",");

        NetworkIdentifier networkIds[] = new NetworkIdentifier[networkIdStrs.length];
        for (int i = 0; i < networkIdStrs.length; i++) {
            // each network identifier value is separated by the ':' character
            String[] networkIdValues = UtilityClass.split(networkIdStrs[i], ":");
            if (networkIdValues.length < 2) throw new BackupException("Invalid home network '" + networkIdStrs[i] + "' - missing network ID or country code");

            // parse each network identifier value
            String networkId = null;
            String[] countryCodeIso3166Ids = null;
            String[] countryCodeItuIds = null;
            for (String networkIdValue : networkIdValues) {
                if (networkIdValue.startsWith(NETWORK_CC_ISO3666_PREFIX)) {
                    countryCodeIso3166Ids = parseNetworkCodes(networkIdValue.substring(NETWORK_CC_ISO3666_PREFIX.length()));
                } else if (networkIdValue.startsWith(NETWORK_CC_ITU_PREFIX)) {
                    countryCodeItuIds = parseNetworkCodes(networkIdValue.substring(NETWORK_CC_ITU_PREFIX.length()));
                } else if (networkIdValue.startsWith(NETWORK_ID_PREFIX)) {
                    networkId = networkIdValue.substring(NETWORK_ID_PREFIX.length());
                    if ((networkId == null) || (networkId.length() <= 0))
                        throw new BackupException("Invalid home network '" + networkIdStrs[i] + "' - invalid network ID '" + networkIdValue + "'");
                } else {
                    throw new BackupException("Invalid home network '" + networkIdStrs[i] + "' - unknown code '" + networkIdValue + "'");
                }
            }

            // create a new identifier
            networkIds[i] = new NetworkIdentifier(countryCodeIso3166Ids, countryCodeItuIds, networkId);
            if (logger != null) logger.debug("Found network ID [" + networkIds[i] + "]");
        }

        return networkIds;
    }

    /* Returns TRUE if any of the specified codes are a home code. */
    private static boolean isHomeCode(String[] codes, String[] homes) {
        if ((codes == null) || (codes.length <= 0) || (homes == null) || (homes.length <= 0)) return false;

        for (String code : codes) {
            if ((code != null) && (code.length() > 0)) {
                for (String home : homes) {
                    if (code.equals(home)) return true;
                }
            }
        }

        return false;
    }

    /** Returns TRUE if the specified network is a users home network. */
    public boolean isHomeNetwork(NetworkIdentifier network) {
        if (!hasHomeNetwork()) return false;

        for (int i = 0; i < homeNetworkIdentifiers.length; i++) {
            // check network code
            if (!network.networkCode.equals(homeNetworkIdentifiers[i].networkCode)) continue;

            // check country codes
            if ((isHomeCode(network.ituCountryCodes, homeNetworkIdentifiers[i].ituCountryCodes))
                    || (isHomeCode(network.isoCountryCodes, homeNetworkIdentifiers[i].isoCountryCodes))) {
                if (logger != null) logger.debug("Network [" + network + "] is a home network");
                return true;
            }
        }

        if (logger != null) logger.debug("Network [" + network + "] is not a home network");
        return false;
    }

    /**
     * @return true if home networks have been configured.
     */
    public boolean hasHomeNetwork() {
        return ((homeNetworkIdentifiers != null) && (homeNetworkIdentifiers.length > 0));
    }

    /**
     * @return the unique ID of the device.
     */
    public String getDeviceId() {
        return Long.toString(System.currentTimeMillis());
    }

    /**
     * @return true if the PreSyncActivity is to be launched
     */
    public boolean showPreSyncActivity() {
        return showPreSyncPage;
    }

    /**
     * @return true if the Hostname is to be shown in settings
     */
    public boolean showHostname() {
        return showHostname;
    }

    /**
     * @return true if the Port is to be shown in settings
     */
    public boolean showPort() {
        return showPort;
    }

    /**
     * @return true if useSSL checkbox is to be shown in settings
     */
    public boolean showSSL() {
        return showSSL;
    }

    /**
     * @return true if Online Sync Request is to be shown in settings
     */
    public boolean showSIS() {
        return showSIS;
    }

    /**
     * @return the contact cache size
     */
    public int getContactCacheSize() {
        return contactCacheSize;
    }

    /**
     * @return the location of the main help file
     */
    public String getHelpFileLocation() {
        return helpFileLocation;
    }
}
