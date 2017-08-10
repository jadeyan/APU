/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import net.cp.ac.core.AndroidContactAPI5;
import net.cp.engine.EngineSettings;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncCounters;
import net.cp.engine.SyncError;
import net.cp.engine.SyncLog;
import net.cp.engine.SyncProgress;
import net.cp.engine.UIInterface;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.SyncException;
import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.devinfo.ContentType;
import net.cp.syncml.client.devinfo.ContentTypeCapabilities;
import net.cp.syncml.client.store.AlreadyExistsException;
import net.cp.syncml.client.store.NoSuchRecordException;
import net.cp.syncml.client.store.Record;
import net.cp.syncml.client.store.RecordStore;
import net.cp.syncml.client.store.RecordStoreCapabilities;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.ConsumableStack;
import net.cp.syncml.client.util.Logger;

/**
 * A class implementing a simple record store containing PIM contacts. <br/><br/>
 *
 * @author James O'Connor
 */
public class ContactStore implements RecordStore {
    /**
     * The character encoding the use.
     */
    protected static final String ENCODING = "UTF-8";

    private static final int[] SYNC_CAP = { SyncML.SYNC_TYPE_TWO_WAY, SyncML.SYNC_TYPE_TWO_WAY_SLOW, SyncML.SYNC_TYPE_ONE_WAY_CLIENT,
            SyncML.SYNC_TYPE_REFRESH_CLIENT, SyncML.SYNC_TYPE_ONE_WAY_SERVER };
    private static final int[] SYNC_CAP_SIS = { SyncML.SYNC_TYPE_TWO_WAY, SyncML.SYNC_TYPE_TWO_WAY_SLOW, SyncML.SYNC_TYPE_ONE_WAY_CLIENT,
            SyncML.SYNC_TYPE_REFRESH_CLIENT, SyncML.SYNC_TYPE_ONE_WAY_SERVER, SyncML.SYNC_TYPE_SERVER_ALERTED };

    /**
     * the record store settings
     */
    protected EngineSettings settings;

    /**
     * the interface allowing access to the UI
     */
    protected UIInterface uiInterface;

    /**
     * the logger to use to trace activity
     */
    protected Logger logger;

    /**
     * the sync log used to record sync counters and error messages
     */
    protected SyncLog syncLog;

    /**
     * the sync counters to update during the sync
     */
    protected SyncCounters syncCounters;

    /**
     * the vCard encoder/decoder to use
     */
    protected VcardCoder vcardCoder;

    /**
     * the manager responsible for maintaining the sync state for the contacts
     */
    protected ContactStateManager stateManager;

    /**
     * the type of sync being performed
     */
    protected int syncType;

    /**
     * the anchor of the previous successful sync session
     */
    protected String lastAnchor;

    /**
     * the anchor of the current sync session
     */
    protected String nextAnchor;

    /**
     * the capabilities of the store
     */
    protected RecordStoreCapabilities capabilities;

    /**
     * the PIM contact list
     */
    protected ContactList contacts;

    /**
     * the number of contacts in the PIM contact list
     */
    protected int contactsSize;

    /**
     *
     * the data of the contact currently being added/replaced on the client
     */
    protected ByteArrayOutputStream inContactData;

    /**
     * the local ID of the contact currently being replaced on the client
     */
    protected String inLocalId;

    /**
     * the total number of contacts to add/delete/replace that will be sent to the client
     */
    protected int inContactsTotal;

    /**
     * the number of contacts that have been added/deleted/replaced by the client
     */
    protected int inContactsProcessed;

    // variables used when sending changes to the server

    /**
     * the records/changes being sent to the server
     */
    protected DynamicContactStack outRecords;

    /**
     * the total number of contacts to add/delete/replace that will be sent to the server
     */
    protected int outContactsTotal;

    /**
     * the number of contacts that have been added/deleted/replaced by the server
     */
    protected int outContactsProcessed;

    /**
     * the time taken to determine the contacts that have changed since the last sync
     */
    protected long outChangeCalculationTime;

    private final SyncProgress status;

    /**
     * This variable is used to decide whether or not we should time the changelog calc time.
     * The operation should not be timed if it is done during the sync operation, but should
     * be timed if it is done before the sync session is opened with the server.
     * See SP-PBC-171
     */
    private boolean setTime;

    private boolean mHasAlertSlowSync = false;

    /**
     *
     * Creates a new contact store that uses the vCard encoder/decoder
     *
     * @param currentSettings engine settings.
     * @param ui The entity interested in status updates from the sync.
     * @param syncLogger The logger to use.
     */
    public ContactStore(EngineSettings currentSettings, UIInterface ui, Logger syncLogger) {
        settings = currentSettings;
        uiInterface = ui;
        logger = syncLogger;

        syncType = SyncML.SYNC_TYPE_TWO_WAY;

        // set the vCard version to use
        String version = "3.0";

        // create the vCard encoder/decoder to use - selected at build time
        vcardCoder = new CPVcardCoder(this, ENCODING, version, logger);

        // create the sync state manager to use
        stateManager = new UidContactStateManager(this, vcardCoder, uiInterface, logger);
        status = new SyncProgress();
        setTime = false;
    }

    /** Initializes the contact store. */
    /**
     * @param contactList The contact list
     * @throws StoreException if the COntactStore could not be initialized.
     */
    public synchronized void initialize(ContactList contactList) throws StoreException {
        // nothing more to do if the store has already been initialized
        if (contacts != null) return;

        if (logger != null) logger.info("Initializing contact store");

        try {
            // attempt to open the contact list
            contacts = contactList;
            contactsSize = -1;

            // initialize the sync state manager
            stateManager.initialize(contacts);
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to initialize the contact store", e);

            throw new StoreException("Failed to initialize the contact store", e);
        }
    }

    /** Closes the contact store. */
    public synchronized void close() {
        // nothing more to do if the store hasn't been initialized
        if (contacts == null) return;

        if (logger != null) logger.info("Closing contact store");

        // close the state manager
        stateManager.close();

        try {
            // close the contact list
            contacts.close();
            contacts = null;
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to close the contact list - ignoring", e);
        }

    }

    /** Closes the contact store.
     * @throws StoreException */
    public synchronized void open() throws StoreException {
        if (logger != null) logger.info("Opening contact store");

        // initialize the sync state manager
        stateManager.initialize(contacts);

    }

    /** Returns the sync state manager used by the contact store.
     *
     * @return The sync state manager
     */
    public ContactStateManager getStateManager() {
        return stateManager;
    }

    /** Returns the content type of the data (vCard 3.0 by default) returned by this record store.
     *
     * @return The content type for the data that will be sent to the sync server.
     * E.g. text/vcard for 3.0, text/x-vcard for 2.1
     */
    public ContentType getContentType() {
        return vcardCoder.getContentType();
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getServerURI()
     */
    @Override
    public String getServerURI() {
        return settings.contactStoreServerUri;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getClientURI()
     */
    @Override
    public String getClientURI() {
        return contacts.getName();
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return contacts.getName();
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getCapabilities()
     */
    @Override
    public RecordStoreCapabilities getCapabilities() {
        if (capabilities == null) {
            ContentType ctType = getContentType();
            ContentTypeCapabilities ctCap = new ContentTypeCapabilities(ctType, null, true);
            ContentTypeCapabilities[] ctCaps = { ctCap };

            int[] syncTypes = (settings.contactSisAllowed) ? SYNC_CAP_SIS : SYNC_CAP;

            capabilities = new RecordStoreCapabilities(ctCaps, ctType, ctType, syncTypes, 255, false);
        }

        return capabilities;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getLastAnchor()
     */
    @Override
    public String getLastAnchor() {
        return lastAnchor;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getNextAnchor()
     */
    @Override
    public String getNextAnchor() {
        return nextAnchor;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getSyncType()
     */
    @Override
    public int getSyncType() {
        return syncType;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#setSyncType(int)
     */
    @Override
    public void setSyncType(int type) {
        syncType = type;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getMetaInfoExtensions()
     */
    @Override
    public String[] getMetaInfoExtensions() {
        // add extensions
        String[] extensions = new String[3];
        extensions[0] = RecordStore.EMI_PARAM_CLIENT_TIME + "=" + UtilityClass.dateToString(new Date());
        extensions[1] = RecordStore.EMI_PARAM_CONFLICT_RES + "=" + getConflictRes(settings.contactConflictResolution);
        extensions[2] = RecordStore.EMI_PARAM_CHANGELOG_TIME + "=" + outChangeCalculationTime;

        return extensions;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#setMetaInfoExtensions(java.lang.String[])
     */
    @Override
    public void setMetaInfoExtensions(String[] extensions) {
        // not implemented
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onNumberOfChanges(int)
     */
    @Override
    public void onNumberOfChanges(int changeCount) {
        if (logger != null) logger.info("Expecting " + changeCount + " changes from the server");

        inContactsTotal = changeCount;
    }

    /**
     * Returns the contact list.
     *
     * @return The contact list.
     */
    public ContactList getContacts() {
        return contacts;
    }

    /** Updates the displayed sync progress as specified.
     *
     * @param statusHeading The general heading for the current state of the sync.
     * @param statusCode The specific status code for the current state of the sync.
     * @param totalCount The total number of items to sync in this phase.
     * @param currentCount The current number of items synced in this phase.
     */
    public void updateProgress(int statusHeading, int statusCode, int totalCount, int currentCount) {
        status.set(statusHeading, statusCode, totalCount, currentCount, null, -1, -1);
        uiInterface.updateSyncProgress(status);
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onSyncStart()
     */
    @Override
    public void onSyncStart() throws StoreException {
        // update the sync progress in the UI
        updateProgress(StatusCodes.SYNC_STARTING, StatusCodes.SYNC_INITIALIZING, -1, -1);

        mHasAlertSlowSync = false;

        // reset the sync counters
        syncCounters = new SyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);
        syncCounters.lastSyncDate = System.currentTimeMillis();
        syncCounters.syncCount = 1;
        inContactsTotal = 0;
        inContactsProcessed = 0;
        outContactsTotal = (outRecords != null) ? outRecords.size() : 0;
        outContactsProcessed = 0;

        try {
            if (logger != null) logger.debug("Opening contact list");

            // open the sync log and retrieve the last anchor (if any) from the sync log
            lastAnchor = null;
            syncLog = SyncLog.open(contacts.getName(), logger);
            SyncCounters[] lastSyncCounters = syncLog.getLastSyncCounters();

            if ((lastSyncCounters != null) && (lastSyncCounters.length > 0)) {
                // read last anchor
                lastAnchor = lastSyncCounters[0].lastSyncAnchor;

                // then wipe out the last anchor in RMS. This will automatically trigger a slow (next) sync
                // if the application is killed. We want this to avoid the server to re-send contact adds
                // that were not acknowledged by the client because of the interruption.
                lastSyncCounters[0].lastSyncAnchor = "";
                syncLog.setLastSyncCounters(lastSyncCounters);
            }

            if (logger != null) logger.info("lastAnchor: " + lastAnchor);

            // remove all log messages from the last sync
            syncLog.removeLogError(EngineSettings.MEDIA_TYPE_CONTACTS, 0);

            // determine the number of contacts in the list and check if the max number of contacts has been reached
            contactsSize = getContactSize();
            if (contactsSize > 0) checkContactLimit(contactsSize);

            // check if the user has chosen to abort the sync
            if (uiInterface.getSyncState() == StatusCodes.SYNC_ABORTING)
                throw new StoreException("Session aborted by the user", SyncML.STATUS_OPERATION_CANCELLED);

            // generate the new anchor for the current sync
            nextAnchor = Long.toString(syncCounters.lastSyncDate);

            // if any of our state information is missing, we have to perform a slow sync
            if ((lastAnchor == null) || (lastAnchor.length() <= 0) || (!stateManager.isStateValid())) {
                // we only switch to a slow sync if not already performing a refresh
                if ((syncType != SyncML.SYNC_TYPE_REFRESH_CLIENT) && (syncType != SyncML.SYNC_TYPE_REFRESH_SERVER)) syncType = SyncML.SYNC_TYPE_TWO_WAY_SLOW;

                lastAnchor = null;
            }

            // open any necessary streams
            inContactData = new ByteArrayOutputStream();
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to start sync session for contacts", e);

            throw new StoreException("Failed to start sync session for contacts", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onSyncSuspend()
     */
    @Override
    public void onSyncSuspend() {
        if (logger != null) logger.debug("Sync session has been suspended");
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onSyncResume()
     */
    @Override
    public void onSyncResume() throws StoreException {
        if (logger != null) logger.debug("Sync session is being resumed");
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onSyncEnd(boolean, int, java.lang.String)
     */
    @Override
    public void onSyncEnd(boolean success, int statusCode, String statusData) {
        if (logger != null) logger.debug("Sync is finishing with success=" + success + ", statusCode=" + statusCode + ", statusData=" + statusData);

        try {
            // determine the overall status of the sync for the store
            if (success) {
                // indicates that the sync session was a success - if there were any store-specific errors, we
                // will treat the sync as being incomplete
                syncCounters.lastSyncStatus = StatusCodes.SYNC_SUCCESS;
                if ((syncCounters.inItemsFailed > 0) || (syncCounters.outItemsFailed > 0)) {
                    syncCounters.lastSyncStatus = StatusCodes.SYNC_INCOMPLETE;

                    if (logger != null)
                        logger.info("syncCounters.inItemsFailed:" + syncCounters.inItemsFailed + ", syncCounters.outItemsFailed: "
                                + syncCounters.outItemsFailed);
                }

                // update the sync anchor anyway
                syncCounters.lastSyncAnchor = nextAnchor;
            } else {
                // indicates that the sync session failed - but change status to partial when sync session is aborted
                if (statusCode == SyncML.STATUS_OPERATION_CANCELLED)
                    syncCounters.lastSyncStatus = StatusCodes.SYNC_INCOMPLETE;
                else
                    syncCounters.lastSyncStatus = convertStatusCode(statusCode);

                // depending on when the session ended, keep the same sync anchor as before
                // or reset the anchor to force a slow sync (required to avoid the server sending
                // duplicates for contact adds that were not acknowledged by the client
                if (inContactsTotal > 0)
                    syncCounters.lastSyncAnchor = null;
                else
                    syncCounters.lastSyncAnchor = lastAnchor;

                if (syncCounters.lastSyncAnchor == null) syncCounters.lastSyncAnchor = "";
            }

            // store the details of the sync in the sync log
            if (syncLog != null) {
                // save the last sync counters
                SyncError lastError = new ContactSyncError();
                lastError.syncMLStatusCode = statusCode;
                lastError.errorCode = syncCounters.lastSyncStatus;
                syncCounters.lastSyncError = lastError;
                SyncCounters[] contactCounters = new SyncCounters[] { syncCounters };
                syncLog.setLastSyncCounters(contactCounters);

                // update the overall counters
                syncLog.updateOverallCounters(contactCounters);

                if (logger != null) logger.info("writing lastStatus");

                // set the generic last status
                SyncLog.setLastStatus(lastError);
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to save the details of the sync session - ignoring", e);
        }

        // close the sync log
        if (syncLog != null) syncLog.close();
        syncLog = null;

        // close any open streams
        if (inContactData != null) {
            try {
                inContactData.close();
            } catch (Throwable e) {
                if (logger != null) logger.error("Failed to close incoming contact data stream - ignoring", e);
            }

            inContactData = null;
        }

        // remove any outgoing changes
        if (outRecords != null) outRecords.removeAllElements();
        outRecords = null;

        // close the store
        close();

        // update the sync progress in the UI
        updateProgress(StatusCodes.SYNC_COMPLETE, (success) ? StatusCodes.SYNC_SUCCESS : StatusCodes.SYNC_FAILED, 0, 0);
    }

    public short convertStatusCode(int statusCode) {
        // map some status codes to an explicit error message
        switch (statusCode) {
        case SyncML.STATUS_OPERATION_CANCELLED:
            return StatusCodes.SYNC_ERROR_USER_ABORT;
        case SyncML.STATUS_SERVICE_UNAVAILABLE:
            return StatusCodes.SYNC_ERROR_CONNECTION;
        case SyncML.STATUS_INVALID_CREDENTIALS:
            return StatusCodes.SYNC_ERROR_CREDENTIALS;
        case SyncML.STATUS_DEVICE_FULL:
            return StatusCodes.SYNC_ERROR_DEVICE_FULL;
        }

        // otherwise, just use a catch-all error message

        return StatusCodes.SYNC_FAILED;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getAllRecords()
     */
    @Override
    public ConsumableStack getAllRecords() throws StoreException {
        // The only place we know there will be a slow sync
        // sometime the getAllRecords() is called twice(not know why), but we only need to alert once during each sync
        if (!mHasAlertSlowSync) {
            mHasAlertSlowSync = true;
            uiInterface.onAlertSlowSync();
        }

        // retrieve all records
        return getRecords(false);
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#getChangedRecords()
     */
    @Override
    public ConsumableStack getChangedRecords() throws StoreException {
        // retrieve only those records that have changed
        return getRecords(true);
    }

    /** Returns the set of records or changes to send to the SyncML server.
     *
     * @param changesOnly if true only changed records will be returned, otherwise all records will be returned.
     * @throws StoreException
     */
    private synchronized ConsumableStack getRecords(boolean changesOnly) throws StoreException {
        // nothing more to do if we have already build the list of records
        if (outRecords != null) {
            // check if the type of records to return (changed contacts or all contacts) has changed - this
            // can happen if we've already used this store to retrieve the changed contacts but the SyncML
            // server later requested that all contacts be returned
            if (outRecords.getChangesOnly() != changesOnly) {
                outRecords.setChangesOnly(changesOnly);
                outContactsTotal = outRecords.size();
            }

            // notify UI the change calculation is done
            uiInterface.onGetChangesEnd();

            return outRecords;
        }

        if (logger != null) logger.info("Retrieving records - changesOnly=" + changesOnly);

        try {
            // update the sync progress in the UI
            updateProgress(StatusCodes.SYNC_CHECKING_CONTACTS, StatusCodes.NONE, -1, -1);

            // we need to time how long the changlog calculation takes
            outChangeCalculationTime = 0;
            long startTime = System.currentTimeMillis();

            // get the list of changed contacts
            outRecords = stateManager.getRecords(changesOnly);

            if (setTime) outChangeCalculationTime = System.currentTimeMillis() - startTime;

            // update total number of records that will be sent to the server
            outContactsTotal = outRecords.size();
            if (outContactsTotal > 0) {
                // update the sync progress in the UI
                updateProgress(StatusCodes.SYNC_CLIENT_UPDATES, StatusCodes.NONE, -1, -1);
            } else {
                // update the sync progress in the UI
                updateProgress(StatusCodes.SYNC_CLIENT_UPDATES, StatusCodes.SYNC_NO_UPDATES, -1, -1);
            }

            // notify UI the change calculation is done
            uiInterface.onGetChangesEnd();

            return outRecords;
        } catch (Throwable e) {
            // failed to retrieve changed contacts
            if (logger != null) logger.error("Failed to retrieve records to send", e);

            throw new StoreException("Failed to retrieve records to send", e);
        }
    }

    /** Returns the next available record or change to send to the SyncML server.
     *
     * @param changesOnly
     * @return The next record.
     *
     */
    public Record getNextRecord(boolean changesOnly) {
        // use the state manager to retrieve the next change
        return stateManager.getNextRecord(changesOnly);
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onResumeResult(int, java.lang.String)
     */
    @Override
    public void onResumeResult(int statusCode, String statusData) {
        if (logger != null) logger.info("Received resume status of '" + statusCode + "' (" + statusData + ")");
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onAddResult(java.lang.String, int, java.lang.String)
     */
    @Override
    public void onAddResult(String localId, int statusCode, String statusData) {
        if (logger != null)
            logger.info("Received Add status of '" + statusCode + (statusData != null ? "' (" + statusData + ")" : "") + "for contact with local ID '"
                    + localId + "'" + " Current out contacts count:" + (outContactsProcessed + 1));

        // update the sync progress in the UI
        outContactsProcessed++;
        updateProgress(StatusCodes.SYNC_CLIENT_UPDATES, StatusCodes.SYNC_SENDING_UPDATE, outContactsTotal, outContactsProcessed);

        try {
            // check the result of the operation
            String contactIdentifier = null;
            if ((SyncML.isSuccessStatus(statusCode)) || (statusCode == SyncML.STATUS_ITEM_ALREADY_EXISTS)) {

                // contact was added successfully (or was already present on the server)
                if (logger != null) {
                    logger.debug("Server successfully added contact with local ID '" + localId + "'");
                }

                // update number of successful adds
                syncCounters.outItemsAdded++;

                // update the sync state to indicate that we successfully synced the change
                stateManager.setSyncResult(localId, true);
            } else {
                // the server failed to add the contact
                contactIdentifier = stateManager.getContactIdentifier(localId);
                if (logger != null) logger.error("Server failed to add contact '" + contactIdentifier + "' with local ID '" + localId + "'");

                // handle the error
                onStoreError(false, StatusCodes.SYNC_ADD, contactIdentifier);

                // update the sync state to indicate that we failed to sync the change
                stateManager.setSyncResult(localId, false);
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to process Add result - ignoring", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onReplaceResult(java.lang.String, int, java.lang.String)
     */
    @Override
    public void onReplaceResult(String localId, int statusCode, String statusData) {
        if (logger != null) logger.info("Received Replace status of '" + statusCode + "' (" + statusData + ") for contact with local ID '" + localId + "'");

        // update the sync progress in the UI
        outContactsProcessed++;
        updateProgress(StatusCodes.SYNC_CLIENT_UPDATES, StatusCodes.SYNC_SENDING_UPDATE, outContactsTotal, outContactsProcessed);

        try {
            // check the result of the operation
            String contactIdentifier = null;
            if (statusCode == SyncML.STATUS_CONFLICT_SERVER_WON) {
                // contact was in conflict and the server won
                contactIdentifier = stateManager.getContactIdentifier(localId);
                if (logger != null)
                    logger.debug("Server failed to replace contact '" + contactIdentifier + "' with local ID '" + localId + "' - conflict found and server won");

                // handle the conflict
                onStoreConflict(StatusCodes.SYNC_UPDATE, contactIdentifier);

                // treat this as a success so that the change won't be retried - we expect that the server
                // will send a Replace request to the client
                stateManager.setSyncResult(localId, true);
            } else if (SyncML.isSuccessStatus(statusCode)) {
                if (logger != null) {
                    logger.debug("Server successfully replaced contact with local ID '" + localId + "'");
                }

                // update number of successful replaces
                syncCounters.outItemsReplaced++;

                // update the sync state to indicate that we successfully synced the change
                stateManager.setSyncResult(localId, true);
            } else {
                // the server failed to replace the contact
                contactIdentifier = stateManager.getContactIdentifier(localId);
                if (logger != null) logger.debug("Server failed to replace the contact '" + contactIdentifier + "' with local ID '" + localId + "'");

                // handle the error
                onStoreError(false, StatusCodes.SYNC_UPDATE, contactIdentifier);

                // update the sync state to indicate that we failed to sync the change
                stateManager.setSyncResult(localId, false);
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to process Replace result - ignoring", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onDeleteResult(java.lang.String, int, java.lang.String)
     */
    @Override
    public void onDeleteResult(String localId, int statusCode, String statusData) {
        if (logger != null) logger.info("Received Delete status of '" + statusCode + "' (" + statusData + ") for contact with local ID '" + localId + "'");

        // update the sync progress in the UI
        outContactsProcessed++;
        updateProgress(StatusCodes.SYNC_CLIENT_UPDATES, StatusCodes.SYNC_SENDING_UPDATE, outContactsTotal, outContactsProcessed);

        try {
            // check the result of the operation
            String contactIdentifier = null;
            if (statusCode == SyncML.STATUS_CONFLICT_SERVER_WON) {
                // contact was in conflict and the server won
                contactIdentifier = stateManager.getContactIdentifier(localId);
                if (logger != null)
                    logger.debug("Server failed to delete contact '" + contactIdentifier + "' with local ID '" + localId + "' - conflict found and server won");

                // handle the conflict
                onStoreConflict(StatusCodes.SYNC_DELETE, contactIdentifier);

                // treat this as a success so that the change won't be retried - we expect that the server
                // will send an Add request to the client
                stateManager.setSyncResult(localId, true);
            } else if ((SyncML.isSuccessStatus(statusCode)) || (statusCode == SyncML.STATUS_ITEM_GONE)) {
                if (logger != null) {
                    logger.debug("Server successfully deleted contact with local ID '" + localId + "'");
                }

                // update number of successful deletes
                syncCounters.outItemsDeleted++;

                // update the sync state to indicate that we successfully synced the change
                stateManager.setSyncResult(localId, true);
            } else {
                // the server failed to delete the contact
                contactIdentifier = stateManager.getContactIdentifier(localId);
                if (logger != null) logger.debug("Server failed to delete contact '" + contactIdentifier + "' with local ID '" + localId + "'");

                // handle the error
                onStoreError(false, StatusCodes.SYNC_DELETE, contactIdentifier);

                // update the sync state to indicate that we failed to sync the change
                stateManager.setSyncResult(localId, false);
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to process Delete result - ignoring", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onCopyResult(java.lang.String, java.lang.String, int, java.lang.String)
     */
    @Override
    public void onCopyResult(String localId, String targetLocalId, int statusCode, String statusData) {
        // not implemented
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#onMoveResult(java.lang.String, int, java.lang.String)
     */
    @Override
    public void onMoveResult(String localId, int statusCode, String statusData) {
        // not implemented
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#addRecordBegin(java.lang.String, java.lang.String, java.lang.String,
     * net.cp.syncml.client.devinfo.ContentType)
     */
    @Override
    public void addRecordBegin(String parentId, String parentGlobalId, String globalId, ContentType contentType) throws StoreException {
        if (logger != null) logger.info("Starting to add a new contact with global ID '" + globalId + "' and content type '" + contentType.toString() + "'");

        // reset the input stream
        inContactData.reset();
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#addRecordData(byte[])
     */
    @Override
    public void addRecordData(byte[] data) throws StoreException, AlreadyExistsException {
        if (logger != null) logger.info("Received " + data.length + " bytes for new contact being added");

        try {
            // add the data to what we have already
            inContactData.write(data);
        } catch (IOException e) {
            if (logger != null) logger.error("Failed to write incoming data for new contact being added", e);

            // handle the error
            onStoreError(true, StatusCodes.SYNC_ADD, "", e);

            throw new StoreException("Failed to write incoming data for new contact being added", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#addRecordEnd(boolean)
     */
    @Override
    public String addRecordEnd(boolean commit) throws StoreException, AlreadyExistsException {
        if (logger != null) logger.info("Finished adding new contact - committing=" + commit);

        // update the sync progress in the UI
        inContactsProcessed++;
        updateProgress(StatusCodes.SYNC_SERVER_UPDATES, StatusCodes.SYNC_RECEIVING_UPDATE, inContactsTotal, inContactsProcessed);

        // nothing more to do if the contact should not be committed
        if (!commit) {
            inContactData.reset();
            return null;
        }

        Contact newContact = null;
        String vcardString = null;
        String contactIdentifier = null;
        boolean contactComitted = false;
        try {
            // convert data to a vCard string
            vcardString = new String(inContactData.toByteArray(), ContactStore.ENCODING);

            // decode the vCard data
            newContact = vcardCoder.decode(contacts, vcardString);

            vcardString = null; // save memory
            // System.gc();

            // check if we've reached the maximum number of contacts allowed
            checkContactLimit(contactsSize + 1);

            if (logger != null) {
                contactIdentifier = getContactIdentifier(newContact);
                logger.debug("Adding contact '" + contactIdentifier + "'");
            }

            // add the contact
            addContact(newContact);
            contactComitted = true;

            if (logger != null) logger.debug("Adding new sync state record for contact '" + contactIdentifier + "'");

            // add a sync state record for this new contact and get a new local ID
            String localId = stateManager.addSyncState(newContact);
            if (logger != null) logger.debug("Successfully added contact '" + contactIdentifier + "' with new local ID '" + localId + "'");

            // update number of adds performed
            syncCounters.inItemsAdded++;
            contactsSize++;

            // return the new local ID
            return localId;
        } catch (Throwable e) {
            if (contactIdentifier == null) {
                if (newContact != null)
                    contactIdentifier = getContactIdentifier(newContact);
                else
                    contactIdentifier = getContactIdentifier(vcardString);
            }

            if (logger != null) logger.error("Failed to add new contact '" + contactIdentifier + "'", e);

            // remove the new contact if it had been added
            if ((contactComitted) && (newContact != null)) {
                try {
                    contacts.removeContact(newContact);
                } catch (Throwable f) {
                    if (logger != null) logger.error("Failed to remove contact '" + contactIdentifier + "'", f);

                    // Allow the exception to fall through and let the exception be thrown below
                }
            }

            // handle the error
            onStoreError(true, StatusCodes.SYNC_ADD, contactIdentifier, e);

            throw new StoreException("Failed to add new contact '" + contactIdentifier + "'", e);
        }
    }

    /** Called to commit the specified contact to the PIM.
     *
     * @param contact
     * @throws Exception
     */
    protected void addContact(Contact contact) throws Exception {
        if (logger != null) logger.debug("Committing contact to PIM");

        // add the contact
        contacts.commit(contact);
    }

    /** Throws an exception if the specified size exceeds the maximum number of allowed contacts. */
    protected void checkContactLimit(int size) throws StoreException {
        // check if we've reached the maximum number of contacts allowed
        int maxContacts = settings.contactStoreMaxContacts;
        if ((maxContacts > 0) && (size > maxContacts)) {
            if (logger != null) logger.error("Maximum number of contacts reached");

            throw new StoreException("Maximum number of contacts reached", SyncML.STATUS_DEVICE_FULL);
        }
    }

    @Override
    public void replaceRecordBegin(String localId, ContentType contentType, boolean fieldLevelReplace) throws StoreException, NoSuchRecordException {
        if (logger != null) logger.info("Starting to replace contact with local ID '" + localId + "' and content type '" + contentType.toString() + "'");

        // reset the input stream
        inContactData.reset();

        // store the local ID so we can refer to it later
        inLocalId = localId;
    }

    @Override
    public void replaceRecordData(byte[] data) throws StoreException, AlreadyExistsException {
        if (logger != null) logger.info("Received " + data.length + " bytes of contact being replaced");

        try {
            // add the data to what we have already
            inContactData.write(data);
        } catch (IOException e) {
            if (logger != null) logger.error("Failed to write incoming data for contact being replaced", e);

            // handle the error
            onStoreError(true, StatusCodes.SYNC_UPDATE, "", e);

            // cleanup
            inContactData.reset();
            inLocalId = null;

            throw new StoreException("Failed to write incoming data for contact being replaced", e);
        }
    }

    @Override
    public String replaceRecordEnd(boolean commit) throws StoreException, AlreadyExistsException {
        if (logger != null) logger.info("Finished replacing contact with local ID '" + inLocalId + "' - committing=" + commit);

        // update the sync progress in the UI
        inContactsProcessed++;
        updateProgress(StatusCodes.SYNC_SERVER_UPDATES, StatusCodes.SYNC_RECEIVING_UPDATE, inContactsTotal, inContactsProcessed);

        // nothing more to do if the contact should not be committed
        if (!commit) {
            inContactData.reset();
            return null;
        }

        Contact newContact = null;
        String vcardString = null;
        String contactIdentifier = null;
        boolean contactComitted = false;
        try {
            // convert data to a vCard string
            vcardString = new String(inContactData.toByteArray(), ContactStore.ENCODING);

            // decode the vCard data
            newContact = vcardCoder.decode(contacts, vcardString);

            // retrieve the contact with the specified local ID
            Contact oldContact = stateManager.getContact(inLocalId);
            if (oldContact != null) {
                if (logger != null) {
                    contactIdentifier = getContactIdentifier(oldContact);
                    logger.debug("Updating existing contact  with local ID '" + inLocalId + "'");
                }

                // update the existing contact
                updateContact(oldContact, newContact);

                try {
                    if (logger != null) logger.debug("Updating sync state record for contact '" + contactIdentifier + "'");

                    // update the sync state to indicate that the contact has been synced
                    stateManager.updateSyncState(inLocalId, newContact);
                    if (logger != null) logger.debug("Successfully updated existing contact '" + contactIdentifier + "' with local ID '" + inLocalId + "'");
                } catch (StoreException e) {
                    // ignore the exception as we can't roll back the update of the contact - the contact
                    // will be sent as an update during the next sync so the affect is benign
                    if (logger != null) logger.error("Failed to update the sync state record with local ID '" + inLocalId + "' - ignoring", e);
                }

                // update number of updates performed
                syncCounters.inItemsReplaced++;
            } else {
                if (logger != null) {
                    contactIdentifier = getContactIdentifier(newContact);
                    logger.debug("Could not find contact '" + contactIdentifier + "' with local ID '" + inLocalId + "' - adding new contact");
                }

                // check if we've reached the maximum number of contacts allowed
                checkContactLimit(contactsSize + 1);

                // contact doesn't exist - add it
                addContact(newContact);
                contactComitted = true;

                if (logger != null) logger.debug("Adding new sync state record for contact '" + contactIdentifier + "'");

                // add a sync state record for this new contact and get a new local ID
                String localId = stateManager.addSyncState(newContact);
                if (logger != null) logger.debug("Successfully added contact '" + contactIdentifier + "' with new local ID '" + localId + "'");

                // update number of adds performed
                syncCounters.inItemsAdded++;
                contactsSize++;

                // return the new local ID to the server
                return localId;
            }

            return null;
        } catch (Throwable e) {
            if (contactIdentifier == null) {
                if (newContact != null)
                    contactIdentifier = getContactIdentifier(newContact);
                else
                    contactIdentifier = getContactIdentifier(vcardString);
            }

            if (logger != null) logger.error("Failed to replace contact '" + contactIdentifier + "' with local ID '" + inLocalId + "'", e);

            // remove the contact if it had been added
            if ((contactComitted) && (newContact != null)) {
                try {
                    contacts.removeContact(newContact);
                } catch (Throwable f) {
                    if (logger != null) logger.error("Failed to remove contact '" + contactIdentifier + "'", f);

                    // Allow the exception to fall through and let the exception be thrown below
                }
            }

            // handle the error
            onStoreError(true, StatusCodes.SYNC_UPDATE, contactIdentifier, e);

            throw new StoreException("Failed to replace contact '" + contactIdentifier + "' with local ID '" + inLocalId + "'", e);
        } finally {
            // cleanup
            inContactData.reset();
            inLocalId = null;
        }
    }

    /** Called to update the specified contact with the information from the specified new contact.
     *
     * @param oldContact
     * @param newContact
     * @throws StoreException
     */
    protected void updateContact(Contact oldContact, Contact newContact) throws StoreException {
        try {
            // copy the new field values to the old contact
            int[] newFieldIds = newContact.getFields();
            for (int newFieldId : newFieldIds) {
                // ignore standard read-only fields
                if ((newFieldId == Contact.UID) || (newFieldId == Contact.REVISION)) continue;

                // get field details
                int newFieldDataType = contacts.getFieldDataType(newFieldId);
                String newFieldLabel = contacts.getFieldLabel(newFieldId);

                if (logger != null) logger.debug("Removing existing field '" + newFieldLabel + "' from contact");

                // remove the entire field from the old contact
                removeContactField(oldContact, newFieldId);

                // add the new field values
                int newValueCount = newContact.countValues(newFieldId);
                for (int j = 0; j < newValueCount; j++) {
                    if (logger != null) logger.debug("Adding new field '" + newFieldLabel + "' value [" + j + "] to contact");

                    int newAttributes = newContact.getAttributes(newFieldId, j);
                    if (newFieldDataType == Contact.STRING) {
                        String newValue = newContact.getString(newFieldId, j);
                        oldContact.addString(newFieldId, newAttributes, newValue);
                    }

                    else if (newFieldDataType == Contact.STRING_ARRAY) {
                        String[] newValue = newContact.getStringArray(newFieldId, j);
                        oldContact.addStringArray(newFieldId, newAttributes, newValue);
                    }

                    else if (newFieldDataType == Contact.BINARY) {
                        byte[] newValue = newContact.getBinary(newFieldId, j);
                        oldContact.addBinary(newFieldId, newAttributes, newValue, 0, newValue.length);
                    }

                    else {
                        if (logger != null) logger.error("Error copying field to old contact. Unknown data type:" + newFieldDataType);
                    }
                }
            }

            // remove any supported fields that were not present in the new contact - this is cater for the
            // case where all values of a field were removed on the server and so that field was not present
            // in the vCard data from the server
            int[] supportedFieldIds = contacts.getSupportedFields();

            for (int supportedFieldId2 : supportedFieldIds) {
                int supportedFieldId = supportedFieldId2;

                // ignore standard read-only fields
                if ((supportedFieldId == Contact.UID) || (supportedFieldId == Contact.REVISION)) continue;

                // don't remove the field if it was present in the new contact
                if (UtilityClass.contains(newFieldIds, supportedFieldId)) continue;

                // don't remove the field if is not supported by the vCard encoder/decoder - these fields
                // would never have been sent to the server
                if (!vcardCoder.isFieldSupported(oldContact, supportedFieldId)) continue;

                String supportedFieldLabel = contacts.getFieldLabel(supportedFieldId);
                if (logger != null) logger.debug("Removing field '" + supportedFieldLabel + "' with ID " + supportedFieldId + " from contact");

                // remove the entire field from the old contact
                removeContactField(oldContact, supportedFieldId);
            }

            if (logger != null) logger.debug("Committing contact to PIM");

            if (logger != null) {
                logger.debug("about to update the old contact with version: " + oldContact.getString(Contact.VERSION, 0));
            }

            // update the contact
            contacts.commit(oldContact);

            String version = oldContact.getString(Contact.VERSION, 0);
            if (logger != null) {
                logger.debug("Old contact updated, new version: " + version);
            }

            ((AndroidContactAPI5) newContact).addString(Contact.VERSION, Contact.ATTR_NONE, version);
        } catch (Exception e) {
            if (logger != null) logger.error("Failed to commit contact", e);

            throw new StoreException("Failed to commit contact", e);
        }
    }

    /** Removes the all values of the specified field from the specified contact.
     *
     * @param contact
     * @param fieldId
     */
    protected void removeContactField(Contact contact, int fieldId) {
        int valueCount = contact.countValues(fieldId);
        for (int i = (valueCount - 1); i >= 0; i--)
            contact.removeValue(fieldId, i);
    }

    @Override
    public void deleteRecord(String localId) throws StoreException, NoSuchRecordException {
        if (logger != null) logger.info("Deleting contact with local ID '" + localId + "'");

        // update the sync progress in the UI
        inContactsProcessed++;
        updateProgress(StatusCodes.SYNC_SERVER_UPDATES, StatusCodes.SYNC_RECEIVING_UPDATE, inContactsTotal, inContactsProcessed);

        try {
            // retrieve the contact with the specified local ID
            Contact oldContact = stateManager.getMinContact(localId);
            if (oldContact != null) {

                if (logger != null) {
                    logger.debug("Deleting existing contact with local ID '" + localId + "'");
                }

                // delete the existing contact
                deleteContact(oldContact);
                contactsSize--;

                try {
                    if (logger != null) logger.debug("Deleting sync state record for contact with local ID '" + localId + "'");

                    // remove the sync state
                    stateManager.deleteSyncState(localId);
                    if (logger != null) logger.debug("Successfully deleted existing contact with local ID '" + localId + "'");
                } catch (StoreException e) {
                    // ignore the exception as we can't roll back the delete of the contact - the contact
                    // will be sent as a delete during the next sync so the affect is benign
                    if (logger != null) logger.error("Failed to delete the sync state record with local ID '" + localId + "' - ignoring", e);
                }

                // update number of deletes performed
                syncCounters.inItemsDeleted++;
            } else {
                // contact doesn't exist - ignore
                if (logger != null) logger.debug("Could not find contact with local ID '" + localId + "' - ignoring delete");
            }
        } catch (Throwable e) {
            Contact oldContact = stateManager.getContact(localId);
            String contactIdentifier = getContactIdentifier(oldContact);
            if (logger != null) logger.error("Failed to delete contact '" + contactIdentifier + "' with local ID '" + localId + "'", e);

            // handle the error
            onStoreError(true, StatusCodes.SYNC_DELETE, contactIdentifier, e);

            throw new StoreException("Failed to delete contact '" + contactIdentifier + "' with local ID '" + localId + "'", e);
        }
    }

    /** Called to delete the specified contact from the device.
     *
     * @param contact
     * @throws StoreException
     */
    protected void deleteContact(Contact contact) throws StoreException {
        try {
            // remove the contact
            contacts.removeContact(contact);
        } catch (Exception e) {
            if (logger != null) logger.error("Failed to remove contact", e);

            throw new StoreException("Failed to remove contact", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#copyRecord(java.lang.String, java.lang.String, java.lang.String, byte[])
     */
    @Override
    public String copyRecord(String localId, String toParentLocalId, String toParentGlobalId, byte[] data) throws StoreException, NoSuchRecordException,
            AlreadyExistsException {
        throw new StoreException("Copying contacts is not implemented");
    }

    /*
     * (non-Javadoc)
     * @see net.cp.syncml.client.store.RecordStore#moveRecord(java.lang.String, java.lang.String, java.lang.String, byte[])
     */
    @Override
    public void moveRecord(String localId, String toParentLocalId, String toParentGlobalId, byte[] data) throws StoreException, NoSuchRecordException {
        throw new StoreException("Moving contacts is not implemented");
    }

    /** Returns an identifier that can be used by the user to identify the specified contact.
     *
     * @param contact
     * @return An identifier, e.g. the formatted name
     */
    public String getContactIdentifier(Contact contact) {
        return getContactIdentifierFromContact(contact, logger);
    }

    /** Returns an identifier that can be used by the user to identify the specified contact. */
    /**
     * @param contact
     * @param logger
     * @return An identifier, e.g. the formatted name
     */
    public String getContactIdentifierFromContact(Contact contact, Logger logger) {
        String name = null;

        if (contact != null) {
            try {
                // first look for the formatted name
                if ((contacts.isSupportedField(Contact.FORMATTED_NAME) && (contact.countValues(Contact.FORMATTED_NAME) > 0))) {
                    name = contact.getString(Contact.FORMATTED_NAME, 0);
                }

                // if no name was found, look for the structured name
                if ((name == null) || (name.length() <= 0)) {
                    if ((contacts.isSupportedField(Contact.NAME)) && (contact.countValues(Contact.NAME) > 0)) {
                        String[] names = contact.getStringArray(Contact.NAME, 0);
                        if (names.length >= 2)
                            name = names[1] + " " + names[0];
                        else if (names.length >= 1) name = names[0];
                    }
                }
            } catch (Throwable e) {
                if (logger != null) logger.error("Failed to get the contact identifier from the contact", e);
            }
        }

        if (name == null) name = "";

        return name;
    }

    /**
     * Returns an identifier that can be used by the user to identify
     * the contact represented by the specified vCard.
     *
     * @param vcardString
     * @return An identifier, e.g. the formatted name
     */
    public String getContactIdentifier(String vcardString) {
        // get the identifier by parsing the specified vCard data
        String name = vcardCoder.getContactIdentifier(vcardString);
        if (name != null) return name;

        return "";
    }

    /** Returns the number of contacts currently in the PIM list or -1 if the number is unknown. */
    public int getContactSize() {
        // just return the size if we already know it
        if (contactsSize >= 0) {
            return contactsSize;
        } else {
            contactsSize = contacts.size();
        }

        return contactsSize;
    }

    /** Sets the number of contacts currently in the PIM list (only if the number is currently unknown). */
    public void setContactSize(int size) {
        // don't overwrite the size if we already know it
        if (contactsSize >= 0) return;

        contactsSize = size;
    }

    /** Handles the specified store-specific operation error. */
    protected void onStoreError(boolean incomming, int operationResId, String contactId) {
        onStoreError(incomming, operationResId, contactId, null);
    }

    /** Handles the specified store-specific operation error and cause. */
    protected void onStoreError(boolean incoming, int operationResId, String contactId, Throwable cause) {
        ContactSyncError error = new ContactSyncError();

        error.contactId = contactId;

        // obtain the localized operation details
        error.operationId = operationResId;

        // obtain the operation direction

        if (incoming)
            error.targetDevice = StatusCodes.SYNC_TO_PHONE;
        else
            error.targetDevice = StatusCodes.SYNC_TO_SERVER;

        error.errorCode = StatusCodes.SYNC_OP_FAILED;

        error.maxContacts = settings.contactStoreMaxContacts;

        if ((cause != null) && (cause instanceof SyncException)) {
            // if we know the actual cause, add it to the error
            int statusCode = ((SyncException) cause).getStatusCode();
            error.syncMLStatusCode = statusCode;
        }

        onStoreError(incoming, error);
    }

    /** Handles the specified store-specific error. */
    protected void onStoreError(boolean incoming, ContactSyncError error) {
        try {
            // update the error counters and notify the listener that a store-specific error has occurred
            if (incoming)
                syncCounters.inItemsFailed++;
            else
                syncCounters.outItemsFailed++;

            uiInterface.onSyncError();

            // add the error message to the sync log
            if (error != null) syncLog.addError(error);

        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to record sync error - ignoring", e);
        }
    }

    /** Handles the specified store-specific operation conflict. */
    protected void onStoreConflict(int operationResId, String contactId) {
        try {
            // update the conflict counters - conflicts only apply in the client-to-server direction
            syncCounters.outItemsConflict++;

            ContactSyncError error = new ContactSyncError();

            error.contactId = contactId;

            error.operationId = operationResId;

            error.targetDevice = StatusCodes.SYNC_TO_SERVER;

            error.errorCode = StatusCodes.SYNC_OP_CONFLICT;

            syncLog.addConflictError(error);
        } catch (Exception e) {
            if (logger != null) logger.error("Failed to record sync conflict - ignoring", e);
        }
    }

    /* Converts the specified conflict-resolution setting to the associated SyncML EMI extension. */
    private String getConflictRes(byte conflictResMethod) {
        if (conflictResMethod == EngineSettings.CONFLICT_RES_RECENT_WINS)
            return EMI_CONFLICT_RES_RECENT_WINS;
        else if (conflictResMethod == EngineSettings.CONFLICT_RES_DUPLICATE)
            return EMI_CONFLICT_RES_DUPLICATE;
        else if (conflictResMethod == EngineSettings.CONFLICT_RES_CLIENT_WINS)
            return EMI_CONFLICT_RES_CLIENT_WINS;
        else if (conflictResMethod == EngineSettings.CONFLICT_RES_SERVER_WINS)
            return EMI_CONFLICT_RES_SERVER_WINS;
        else if (conflictResMethod == EngineSettings.CONFLICT_RES_IGNORE)
            return EMI_CONFLICT_RES_IGNORE;
        else
            return EMI_CONFLICT_RES_RECENT_WINS;
    }

    /**
     * Used to indicate that we want to time the next changelog elaboration.
     *
     * If the changelog calculation is done outside the sync session
     * (e.g. if we were counting the number of changes to decide if we want to sync)
     * then it needs to be timed so that this info can be sent to the server.
     * If we calculate the changes during the session, this is not necessary.
     */
    public void enableSetChangelogTime() {
        setTime = true;
    }

    /**
     * Used to indicate that we do not want to time the next changelog elaboration.
     *
     * If the changelog calculation is done outside the sync session
     * (e.g. if we were counting the number of changes to decide if we want to sync)
     * then it needs to be timed so that this info can be sent to the server.
     * If we calculate the changes during the session, this is not necessary.
     */
    public void disableSetChangelogTime() {
        setTime = false;
    }
}
