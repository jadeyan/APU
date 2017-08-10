/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.io.*;
import java.util.Vector;

import net.cp.engine.Settings;
import net.cp.engine.PersistentStore;
import net.cp.engine.contacts.ContactSyncError;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;

/**
 * A class which manages a log of any relevant messages that occur during a sync session. <br/><br/>
 *
 * The sync log messages are store in an RMS record store and are read/written on demand to
 * conserve memory. The log messages for each content type (i.e. contacts or content) are stored
 * in separate RMS record stores.
 */
public class SyncLog
{
    private static final String RMS_SYNCLOG_PREFIX =    "PBSL-";    // PhoneBackup Sync Log

    //Definition of the possible record store versions
    private static final short VERSION_1 =              1;
    private static final short VERSION_CURRENT =        VERSION_1;

    //Definition of the possible record types
    private static final byte RECORD_TYPE_LAST_SYNC_COUNTERS =  1;
    private static final byte RECORD_TYPE_LAST_SYNC_ERROR =     2;
    private static final byte RECORD_TYPE_LAST_SYNC_CONFLICT =  4;
    private static final byte RECORD_TYPE_OVERALL_COUNTERS =    8;

    private static Logger logger;                              //the logger to use to log activity

    private static String RMS_SYNCLOG_LAST_STATUS = "PBLastStatus";

    private String recordStoreName;                     //the name of the RMS record store where the sync log is persisted
    private PersistentStore recordStore;                  //the RMS record store where the sync log is persisted

    //record IDs
    private int recordIdLastSyncCounters;               //the ID of the record containing the last sync counters
    private int recordIdOverallSyncCounters;            //the ID of the record containing the overall sync counters

    /**
     * journal -- The journal is an internal SQLite file that is used
     * when running transactions (to ensure roll back). Cannot disable it. Only found on Android OS 4.0.3
     */
    private final static String DB_DERIVATIVE_File = "-journal";

    /* Creates a new sync log writer - private to prevent instantiation. */
    private SyncLog()
    {

        recordStoreName = null;
        recordStore = null;

        recordIdLastSyncCounters = 0;
        recordIdOverallSyncCounters = 0;
    }

    /**
     * Opens a sync log with the specified name.
     *
     * @param storeName the name of the store that is associated with the sync log.
     * @return an instance allowing access to the sync log for the specified store.
     * @throws Exception if the sync log couldn't be opened.
     */
    public static SyncLog open(String storeName, Logger theLogger)
        throws Exception
    {
        logger = theLogger;

        //all sync log RMS records should have the same prefix to make them easy to identify
        SyncLog syncLog = new SyncLog();
        syncLog.openSyncLog(RMS_SYNCLOG_PREFIX + storeName);

        return syncLog;
    }

    /**
     * Returns the last sync counters for the specified media types (Settings.MEDIA_TYPE_XXX).
     *
     * @param mediaTypes the media types whose counters should be returned.
     * @return the counters of the last sync for the specified media types.
     * @throws Exception if the counters couldn't be retrieved.
     */
    public static SyncCounters getLastSyncCounters(int mediaTypes)
        throws Exception
    {
        return SyncLog.getCounters(RECORD_TYPE_LAST_SYNC_COUNTERS, mediaTypes);
    }

    /**
     * Returns the overall sync counters for the specified media types.
     *
     * @param mediaTypes the media types (Settings.MEDIA_TYPE_XXX) whose counters should be returned.
     * @return the cumulative counters of all syncs for the specified media types.
     * @throws Exception if the counters couldn't be retrieved.
     */
    public static SyncCounters getOverallSyncCounters(int mediaTypes)
        throws Exception
    {
        return SyncLog.getCounters(RECORD_TYPE_OVERALL_COUNTERS, mediaTypes);
    }

    /* Returns the sync counters of the specified type for the specified media types. */
    private static SyncCounters getCounters(byte recordType, int mediaTypes)
        throws Exception
    {
        SyncLog syncLog = null;
        try
        {
            //examine each record store used by the application
            SyncCounters counters = new SyncCounters(mediaTypes);
            String[] storesNames = Settings.getPersistentStoreManager().listRecordStores();

            if(storesNames == null)
                return null;

            for (int i = 0; i < storesNames.length; i++)
            {
                //ignore stores that are not sync logs
                String storeName = storesNames[i];

                /**
                 * need add a condition -- "storeName.endsWith(DBDerivativeFile)" -- we should not
                 * open or create a file whose name ends with "-journal"
                 * it's a file created by SQLite automatically on Android OS 4.0.3 for each file we created
                 */
                if (! storeName.startsWith(RMS_SYNCLOG_PREFIX)  || storeName.endsWith(DB_DERIVATIVE_File))
                    continue;

                //open the sync log
                syncLog = new SyncLog();
                syncLog.openSyncLog(storeName);

                //get the set of counters
                SyncCounters[] syncCounters = null;
                if (recordType == RECORD_TYPE_LAST_SYNC_COUNTERS)
                    syncCounters = syncLog.getLastSyncCounters();
                else if (recordType == RECORD_TYPE_OVERALL_COUNTERS)
                    syncCounters = syncLog.getOverallCounters();
                else
                    return null;

                //get the counters and add them to what we have already
                for (int j = 0; j < syncCounters.length; j++)
                {
                    //ignore counters for media types we don't want
                    SyncCounters syncCounter = syncCounters[j];
                    if ( (mediaTypes > 0) && (mediaTypes & syncCounter.mediaType) == 0)
                        continue;

                    //add the counters to what we already have
                    counters.addCounters(syncCounter);
                }

                //close the sync log
                syncLog.close();
                syncLog = null;
            }

            return counters;
        }
        finally
        {
            if (syncLog != null)
                syncLog.close();
        }
    }

    /**
     * Resets the overall sync counters for the specified media types.
     *
     * @param mediaTypes the media types (Settings.MEDIA_TYPE_XXX) whose cumulative counters should be reset
     * @throws Exception if the counters couldn't be reset
     */
    public static void resetOverallSyncCounters(int mediaTypes)
        throws Exception
    {
        SyncLog syncLog = null;
        try
        {
            //examine each record store used by the application
            String[] storesNames = Settings.getPersistentStoreManager().listRecordStores();
            for (int i = 0; i < storesNames.length; i++)
            {
                //ignore stores that are not sync logs
                String storeName = storesNames[i];
                if (! storeName.startsWith(RMS_SYNCLOG_PREFIX))
                    continue;

                //open the sync log
                syncLog = new SyncLog();
                syncLog.openSyncLog(storeName);

                //reset the counters
                syncLog.resetOverallCounters(mediaTypes);

                //close the sync log
                syncLog.close();
                syncLog = null;
            }
        }
        finally
        {
            if (syncLog != null)
                syncLog.close();
        }
    }

    /**
     * Returns all sync error messages (String) for the specified media types and sync target.
     *
     * @param mediaTypes    the media types (Settings.MEDIA_TYPE_XXX) whose sync error messages should be returned.
     * @param targetDevice  the sync direction (StatusCodes.SYNC_TO_PHONE etc...) whose sync error messages should be returned.
     * @param maxMessages   the maximum number of messages to return or 0 to return all matching messages.
     * @return a collection of SyncError, ContactSyncError detailing any errors that occurred during the last sync.
     * @throws Exception if the error messages couldn't be retrieved
     */
    public static Vector<SyncError> getErrorsForType(int mediaTypes, int targetDevice, int maxMessages)
        throws Exception
    {
        return SyncLog.getErrors(RECORD_TYPE_LAST_SYNC_ERROR, mediaTypes, targetDevice, maxMessages);
    }

    /**
     * Returns all sync conflict messages (String) for the specified media types and sync direction.
     *
     * @param mediaTypes    the media types (Settings.MEDIA_TYPE_XXX) whose sync conflict messages should be returned.
     * @param targetDevice the sync direction (StatusCodes.SYNC_TO_PHONE etc...) whose sync conflict messages should be returned.
     * @param maxMessages   the maximum number of messages to return or 0 to return all matching messages.
     * @return a collection of SyncError, ContactSyncError detailing any conflicts that occurred during the last sync.
     * @throws Exception if the conflict messages couldn't be retrieved
     */
    public static Vector<SyncError> getConflictErrors(int mediaTypes, int targetDevice, int maxMessages)
        throws Exception
    {
        return SyncLog.getErrors(RECORD_TYPE_LAST_SYNC_CONFLICT, mediaTypes, targetDevice, maxMessages);
    }

    /* Returns all sync errors (SyncError, ContactSyncError) of the specified type for the specified media types and sync direction. */
    private static Vector<SyncError> getErrors(byte recordType, int mediaTypes, int targetDevice, int maxErrors)
        throws Exception
    {
        SyncLog syncLog = null;
        try
        {
            //examine each record store used by the application
            Vector<SyncError> errors = new Vector<SyncError>();
            String[] storesNames = Settings.getPersistentStoreManager().listRecordStores();
            for (int i = 0; i < storesNames.length; i++)
            {
                //nothing more to do if we're already retrieved the max number of messages
                if ( (maxErrors > 0) && (errors.size() >= maxErrors) )
                    break;

                //ignore stores that are not sync logs
                String storeName = storesNames[i];
                if (! storeName.startsWith(RMS_SYNCLOG_PREFIX))
                    continue;

                //open the sync log
                syncLog = new SyncLog();
                syncLog.openSyncLog(storeName);

                //determine the maximum number of messages to retrieve from the sync log
                int maxLogMessages = 0;
                if (maxErrors > 0)
                    maxLogMessages = maxErrors - errors.size();

                //get the messages
                syncLog.getLogErrors(errors, recordType, mediaTypes, targetDevice, maxLogMessages);

                //close the sync log
                syncLog.close();
                syncLog = null;
            }

            return errors;
        }
        finally
        {
            if (syncLog != null)
                syncLog.close();
        }
    }

    /* Opens the sync log with the specified name. */
    private void openSyncLog(String syncLogName)
        throws Exception
    {
        try
        {
            recordStoreName = syncLogName;
            if (logger != null)
                logger.info("Opening sync log store with name '" + recordStoreName + "'");

            //open the RMS containing the sync log
            recordStore = Settings.getPersistentStoreManager().openRecordStore(recordStoreName);

            //setup the standard sync log records (last sync state, counters, etc)
            if (recordStore.getNumRecords() <= 0)
            {
                //the store has just been created - create some empty records so they can always be found
                //with the same IDs
                recordIdLastSyncCounters = writeCounterRecord(recordIdLastSyncCounters, RECORD_TYPE_LAST_SYNC_COUNTERS, new SyncCounters[0]);
                recordIdOverallSyncCounters = writeCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS, new SyncCounters[0]);
            }
            else
            {
                //the standard sync records are always found at the same record IDs
                recordIdLastSyncCounters = 1;
                recordIdOverallSyncCounters = 2;
            }
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to open the sync log with name '" + recordStoreName + "'", e);

            close();

            throw new Exception("Failed to open the sync log with name '" + recordStoreName + "'");
        }
    }

    /** Closes the sync log. */
    public void close()
    {
        if (logger != null)
            logger.info("Closing sync log store with name '" + recordStoreName + "'");

        //close the record store
        Settings.getPersistentStoreManager().closeRecordStore(recordStore);
        recordStore = null;

        recordIdLastSyncCounters = 0;
        recordIdOverallSyncCounters = 0;
    }

    /**
     * Opens the sync log and reads the status of the last sync session.
     * This is returned as a SyncError object.
     *
     * @return The last sync status.
     */
    public static SyncError getLastStatus()
    {
        ByteArrayInputStream byteStream = null;
        DataInputStream dataStream = null;
        SyncError lastStatus = null;
        PersistentStore store = null;

        try
        {
            store = Settings.getPersistentStoreManager().openRecordStore(RMS_SYNCLOG_LAST_STATUS, true);

            //stored as first record, there should only ever be 1 "last status"
            byte[] data = store.readRecord(1);

            if(data != null && data.length > 0)
            {
                byteStream = new ByteArrayInputStream(data);
                dataStream = new DataInputStream(byteStream);

                lastStatus = SyncError.readFromStream(dataStream);
            }
        }

        catch(Exception e)
        {
            if(logger != null)
                logger.error("error reading lastStatus", e);
        }

        finally
        {
            //close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);

            if(store != null)
                store.close();
        }

        return lastStatus;
    }

    /**
     * Opens the sync log and writes the status of the last sync session.
     *
     * @param error The last status of the last sync
     */
    public static void setLastStatus(SyncError error)
    {
        ByteArrayOutputStream byteStream = null;
        DataOutputStream dataStream = null;
        try
        {
            //create the streams
            byteStream = new ByteArrayOutputStream();
            dataStream = new DataOutputStream(byteStream);

            //write the sync error
            error.writeToStream(dataStream);

            dataStream.flush();
            byteStream.flush();

            //store it in the RMS
            byte[] recordData = byteStream.toByteArray();

            PersistentStore store = Settings.getPersistentStoreManager().openRecordStore(RMS_SYNCLOG_LAST_STATUS, true);

            //store as first record, there should only ever be 1 "last status"
            if(store.getNumRecords() > 0)
                store.writeRecord(1, recordData);

            else
                store.writeRecord(0, recordData);

            store.close();
        }

        catch (Exception e)
        {
            if(logger != null)
                logger.error("error writing lastStatus", e);
        }

        finally
        {
            //close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /**
     * Returns the counters from the last sync that was performed.
     *
     * @return one or more counters (one for each media type) for the last sync that was performed.
     * @throws Exception if the counters couldn't be retrieved.
     */
    public SyncCounters[] getLastSyncCounters()
        throws Exception
    {
        if (logger != null)
            logger.debug("Retrieving last sync counters");

        if (recordIdLastSyncCounters <= 0)
            throw new Exception("No last sync counter record present");

        return readCounterRecord(recordIdLastSyncCounters, RECORD_TYPE_LAST_SYNC_COUNTERS);
    }

    /**
     * Sets the specified counters for the last sync that was performed.
     *
     * @param syncCounters one or more sync counters (one for each media type) to save.
     * @throws Exception if the counters couldn't be saved.
     */
    public void setLastSyncCounters(SyncCounters[] syncCounters)
        throws Exception
    {
        if (logger != null)
            logger.debug("Setting last sync counters");

        //write the sync counters
        writeCounterRecord(recordIdLastSyncCounters, RECORD_TYPE_LAST_SYNC_COUNTERS, syncCounters);
    }


    /**
     * Returns the cumulative counters for all syncs that have been performed.
     *
     * @return one or more counters (one for each media type) for all syncs that were performed.
     * @throws Exception if the counters couldn't be retrieved.
     */
    public SyncCounters[] getOverallCounters()
        throws Exception
    {
        if (logger != null)
            logger.debug("Retrieving overall sync counters");

        if (recordIdOverallSyncCounters <= 0)
            throw new Exception("No overall sync counter record present");

        return readCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS);
    }

    /**
     * Updates the cumulative counters for all performed syncs with the specified data.
     *
     * @param syncCounters one or more sync counters (one for each media type) to add to the existing cumulative counters.
     * @throws Exception if the counters couldn't be updated.
     */
    public void updateOverallCounters(SyncCounters[] syncCounters)
        throws Exception
    {
        if ( (syncCounters == null) || (syncCounters.length <= 0) )
            return;

        if (logger != null)
            logger.debug("Updating overall sync counters");

        //if the overall counters don't exist, just store the supplied ones as-is
        SyncCounters[] overallSyncCounters = readCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS);
        if ( (overallSyncCounters == null) || (overallSyncCounters.length <= 0) )
        {
            writeCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS, syncCounters);
            return;
        }

        //add the specified counters to the existing ones based on their media type
        Vector<SyncCounters> newCounters = new Vector<SyncCounters>();
        for (int i = 0; i < syncCounters.length; i++)
        {
            SyncCounters syncCounter = syncCounters[i];

            //look for an existing set of counters with the same media type
            boolean counterUpdated = false;
            for (int j = 0; j < overallSyncCounters.length; j++)
            {
                SyncCounters overallSyncCounter = overallSyncCounters[j];
                if (overallSyncCounter.mediaType == syncCounter.mediaType)
                {
                    int syncCount = overallSyncCounter.syncCount;
                    overallSyncCounter.addCounters(syncCounter);
                    overallSyncCounter.syncCount = syncCount + 1;
                    counterUpdated = true;
                    break;
                }
            }

            //no existing counters for the media type - record the fact that it is a new set of counters
            if (! counterUpdated)
                newCounters.addElement(syncCounter);
        }

        //build the full list of counters (adding any new counters that were found)
        SyncCounters[] updatedCounters = overallSyncCounters;
        if (newCounters.size() > 0)
        {
            int newLength = overallSyncCounters.length + newCounters.size();
            updatedCounters = new SyncCounters[newLength];

            int i = 0;
            for (i = 0; i < overallSyncCounters.length; i++)
                updatedCounters[i] = overallSyncCounters[i];

            int newCounterIndex = i;
            for (i = 0; i < newCounters.size(); i++)
                updatedCounters[newCounterIndex + i] = (SyncCounters)newCounters.elementAt(i);
        }

        //write the updated counters to the RMS
        writeCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS, updatedCounters);
    }

    /**
     * Resets the overall sync details for the specified media types in the sync log.
     *
     * @param mediaTypes the media types (Settings.MEDIA_TYPE_XXX) whose cumulative counters should be reset.
     * @throws Exception if the counters couldn't be reset.
     */
    public void resetOverallCounters(int mediaTypes)
        throws Exception
    {
        if (logger != null)
            logger.debug("Resetting overall sync counters for media types '" + mediaTypes + "'");

        //nothing more to do if the overall counters don't exist
        SyncCounters[] overallSyncCounters = readCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS);
        if ( (overallSyncCounters == null) || (overallSyncCounters.length <= 0) )
            return;

        //reset the counters for the specified media types
        boolean countersReset = false;
        for (int i = 0; i < overallSyncCounters.length; i++)
        {
            SyncCounters overallSyncCounter = overallSyncCounters[i];
            if ( (mediaTypes == 0) || ((overallSyncCounter.mediaType & mediaTypes) > 0 ) )
            {
                overallSyncCounter.reset();
                countersReset = true;
            }
        }

        //write the updated counters to the RMS
        if (countersReset)
            writeCounterRecord(recordIdOverallSyncCounters, RECORD_TYPE_OVERALL_COUNTERS, overallSyncCounters);
    }


    /* Retrieves all sync log errors (SyncError, ContactSyncError) of the specified type for the specified media types and sync direction. */
    private void getLogErrors(Vector<SyncError> errors, byte recordType, int mediaTypes, int targetDevice, int maxMessages)
        throws Exception
    {
        //read all sync log records from the RMS
        DataInputStream dataStream = null;
        ByteArrayInputStream byteStream = null;
        try
        {
            if (logger != null)
                logger.debug("Getting all sync log messages for media types '" + mediaTypes + "' and sync direction '" + targetDevice + "'");

            //process each record in the sync log RMS
            int recordId;
            int errorCount = 0;
            byte[] recordData;
            recordStore.startEnumeration();
            while ((recordId = recordStore.nextRecordId()) > 0)
            {
                //read the RMS record
                recordData = recordStore.readRecord(recordId);
                if (recordData == null)
                    throw new StoreException("Failed to find sync log message with record ID '" + recordId + "'");

                //parse the binary data using a data stream
                byteStream = new ByteArrayInputStream(recordData);
                dataStream = new DataInputStream(byteStream);

                //read the version
                short version = dataStream.readShort();
                if ( (version <= 0) || (version > VERSION_CURRENT) )
                    throw new Exception("Invalid version '" + version + "' found");

                //ignore the record if it is not a last error message record
                byte currentRecordType = dataStream.readByte();
                if (currentRecordType != recordType)
                    continue;

                //read the media type
                int logMediaType = dataStream.readInt();

                //ignore the record if the media type doesn't match
                if ( (mediaTypes > 0) && ((logMediaType & mediaTypes) == 0) )
                    continue;

                int target;
                if(logMediaType == EngineSettings.MEDIA_TYPE_CONTACTS)
                {
                    ContactSyncError error = ContactSyncError.readFromStream(dataStream);
                    target = error.targetDevice;

                    errors.addElement(error);
                    errorCount++;
                }
                else
                {
                    SyncError error = SyncError.readFromStream(dataStream);
                    target = error.targetDevice;

                    errors.addElement(error);
                    errorCount++;
                }

                //ignore the record if the sync direction doesn't match
                if((targetDevice > 0) && (target != targetDevice))
                    continue;

                //check if we've exceeded the max number of messages
                if ( (maxMessages > 0) && (errorCount >= maxMessages) )
                    break;

            }
        }
        finally
        {
            recordStore.stopEnumeration();

            //close the streams
            UtilityClass.streamClose(byteStream, logger);
            UtilityClass.streamClose(dataStream, logger);
        }
    }

    /**
     * Adds the specified error message to the sync log.
     *
     * @param error the SyncError object containing the details of the error
     * @throws Exception if the error message couldn't be added.
     */
    public void addError(SyncError error)
        throws Exception
    {
        addLogError(RECORD_TYPE_LAST_SYNC_ERROR, error);
    }

    /**
     * Adds the specified conflict message to the sync log.
     *
     * @param error the SyncError object containing the details of the error
     * @throws Exception if the conflict message couldn't be added.
     */
    public void addConflictError(SyncError error)
        throws Exception
    {
        addLogError(RECORD_TYPE_LAST_SYNC_CONFLICT, error);
    }

    /* Adds a message of the specified type to the sync log for the specified media type and sync direction. */
    private void addLogError(byte recordType, SyncError error)
        throws Exception
    {
        ByteArrayOutputStream byteStream = null;
        DataOutputStream dataStream = null;
        try
        {
            if (logger != null)
                logger.debug("Adding sync log message for media type '" + error.mediaType + "' and sync direction '" + error.targetDevice);

            //create the streams
            byteStream = new ByteArrayOutputStream();
            dataStream = new DataOutputStream(byteStream);

            //write the data
            dataStream.writeShort(VERSION_CURRENT);
            dataStream.writeByte(recordType);

            //write the media type, so that when we read the record, we know if it's a SyncError or a ContactSyncError that follows
            dataStream.writeInt(error.mediaType);

            if(error.mediaType == EngineSettings.MEDIA_TYPE_CONTACTS)
            {
                ContactSyncError cError = (ContactSyncError)error;
                cError.writeToStream(dataStream);
            }

            else
                error.writeToStream(dataStream);

            //add a new record
            recordStore.writeRecord(0, byteStream.toByteArray());
        }
        finally
        {
            //close the streams
            UtilityClass.streamClose(byteStream, logger);
            UtilityClass.streamClose(dataStream, logger);
        }
    }

    /**
     * Removes all sync log messages for the specified media types and sync direction.
     *
     * @param mediaTypes the media type (Settings.MEDIA_TYPE_XXX) whose error and conflict records should be removed.
     * @param targetDevice the sync direction (StatusCodes.SYNC_TO_PHONE StatusCodes.SYNC_TO_SERVER) whose error and conflict records should be removed.
     * @throws Exception if the messages couldn't be removed.
     *
     */
    public void removeLogError(int mediaTypes, int targetDevice)
        throws Exception
    {
        //read all sync log records from the RMS
        DataInputStream dataStream = null;
        ByteArrayInputStream byteStream = null;
        try
        {
            if (logger != null)
                logger.debug("Removing all sync log messages for media types '" + mediaTypes + "' and sync direction '" + targetDevice + "'");

            //process each record in the sync log RMS
            int recordId;
            byte[] recordData;

            recordStore.startEnumeration();
            while ((recordId = recordStore.nextRecordId()) > 0)
            {
                //read the RMS record
                recordData = recordStore.readRecord(recordId);
                if (recordData == null)
                    throw new StoreException("Failed to find sync log message with record ID '" + recordId + "'");

                //parse the binary data using a data stream
                byteStream = new ByteArrayInputStream(recordData);
                dataStream = new DataInputStream(byteStream);

                //read the version
                short version = dataStream.readShort();
                if ( (version <= 0) || (version > VERSION_CURRENT) )
                    throw new Exception("Invalid version '" + version + "' found");

                //ignore the record if it is not a last error message record
                byte recordType = dataStream.readByte();
                if ( (recordType != RECORD_TYPE_LAST_SYNC_ERROR) && (recordType != RECORD_TYPE_LAST_SYNC_CONFLICT) )
                    continue;

                //ignore the record if the media type doesn't match
                int logMediaType = dataStream.readInt();
                if ( (mediaTypes > 0) && ((logMediaType & mediaTypes) == 0) )
                    continue;

                int target;
                if(logMediaType == EngineSettings.MEDIA_TYPE_CONTACTS)
                {
                    ContactSyncError error = ContactSyncError.readFromStream(dataStream);
                    target = error.targetDevice;
                }
                else
                {
                    SyncError error = SyncError.readFromStream(dataStream);
                    target = error.targetDevice;
                }

                //ignore the record if the sync direction doesn't match
                if ( (targetDevice > 0) && (target != targetDevice) )
                    continue;

                //remove the record
                recordStore.deleteRecord(recordId);
            }
        }
        finally
        {
            recordStore.stopEnumeration();

            //close the streams
            UtilityClass.streamClose(byteStream, logger);
            UtilityClass.streamClose(dataStream, logger);
        }
    }

    /* Reads the sync counters from the record with the specified ID and type. */
    private SyncCounters[] readCounterRecord(int recordId, byte recordType)
        throws Exception
    {
        ByteArrayInputStream byteStream = null;
        DataInputStream dataStream = null;
        try
        {
            //read the record from the RMS
            byte[] recordData = recordStore.readRecord(recordId);
            if (recordData == null)
                throw new StoreException("Failed to find sync log counters with record ID '" + recordId + "'");

            //create the streams
            byteStream = new ByteArrayInputStream(recordData);
            dataStream = new DataInputStream(byteStream);

            //read the version
            short version = dataStream.readShort();
            if ( (version <= 0) || (version > VERSION_CURRENT) )
                throw new Exception("Invalid version '" + version + "' found");

            //check the record type
            byte counterRecordType = dataStream.readByte();
            if (counterRecordType != recordType)
                throw new Exception("Invalid counter record found");

            //read the counters
            return readCounters(dataStream);
        }
        finally
        {
            //close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /* Writes the specified sync counters to the record with the specified ID and type. */
    private int writeCounterRecord(int recordId, byte recordType, SyncCounters[] syncCounters)
        throws Exception
    {
        ByteArrayOutputStream byteStream = null;
        DataOutputStream dataStream = null;
        try
        {
            //create the streams
            byteStream = new ByteArrayOutputStream();
            dataStream = new DataOutputStream(byteStream);

            //write the sync details
            dataStream.writeShort(VERSION_CURRENT);
            dataStream.writeByte(recordType);
            writeCounters(dataStream, syncCounters);

            dataStream.flush();
            byteStream.flush();

            //store it in the RMS
            byte[] recordData = byteStream.toByteArray();
            return recordStore.writeRecord(recordId, recordData);
        }
        finally
        {
            //close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /* Reads the counters from the specified data stream. */
    private SyncCounters[] readCounters(DataInputStream stream)
        throws Exception
    {
        int counterCount = stream.readInt();
        SyncCounters[] counters = new SyncCounters[counterCount];
        for (int i = 0; i < counterCount; i++)
        {
            counters[i] = new SyncCounters(EngineSettings.MEDIA_TYPE_NONE);
            counters[i].readCounters(stream);
        }

        return counters;
    }

    /* Writes the specified counters to the specified data stream. */
    private void writeCounters(DataOutputStream stream, SyncCounters[] counters)
        throws Exception
    {
        stream.writeInt(counters.length);
        for (int i = 0; i < counters.length; i++)
            counters[i].writeCounters(stream);
    }
}
