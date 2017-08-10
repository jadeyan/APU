/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.cp.ac.core.AndroidContactAPI5;
import net.cp.engine.EngineSettings;
import net.cp.engine.PersistentStore;
import net.cp.engine.Settings;
import net.cp.engine.StatusCodes;
import net.cp.engine.UIInterface;
import net.cp.engine.UtilityClass;
import net.cp.mtk.common.security.MD5;
import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.store.Record;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;

/**
 * A class responsible for managing the sync state for a collection of contacts where the 'UID'
 * field is supported but the 'REV' field is not. <br/><br/>
 *
 * The 'UID' field allows us to easily find a PIM entry from it's local ID. The mapping between local
 * IDs UID and UIDs is maintained in the RMS so the application can match a contact with its associated
 * sync state information. This state information also contains a hash of the contact vCard data and is
 * used to determine if the contact has changed since the last sync. <br/><br/>
 *
 * Change detection is implemented as follow:
 * <ol>
 *      <li> Examine each contact in the PIM and use its 'UID' field to find its associated sync state
 *           information in the RMS.
 *      <li> If the associated sync state was not found, it indicates that the contact is a new contact
 *           and is marked as an Add.
 *      <li> If the associated sync state was found, the contact is encoded as a vCard and its MD5 hash
 *           computed.
 *      <li> If the hash matches the one in the sync state record, it indicates that the contact has not
 *           changed since the last sync and can be eliminated from further processing.
 *      <li> If the hash does not match the one in the sync state record, it indicates that the contact
 *           has changed since the last sync is marked as a Replace.
 *
 *      <li> Examine each remaining unmatched record in RMS. All such records identify contacts that have
 *           been deleted since the last sync and should be marked as a Delete.
 * </ol>
 *
 * When dealing with Replace or Delete requests from the server, we need to be able to identify the contact
 * that the operation applies to. We do this in one of the following ways:
 * <ol>
 *      <li> If the "contact.statemanager.fastcontactlookup" J2MEPolish variable is defined as "true",
 *           a mapping will be maintained between each contact and its UID. This allows Replace and
 *           Delete operations to be processed quickly but will require more memory.
 *      <li> If the "contact.statemanager.fastcontactlookup" J2MEPolish variable is defined as "false",
 *           each contact must be found manually by scanning the PIM list to find a contact with matching
 *           UID. This will be slower but will not require much memory.
 * </ol>
 *
 * @author Herve Fayolle
 */
public class UidContactStateManager extends ContactStateManager {
    /**
     * Stands for: Phone Backup Sync State
     */
    protected static final String RMS_STATE_PREFIX = "PBSS-";

    // Definition of the possible record store versions
    protected static final short VERSION_1 = 1;
    protected static final short VERSION_CURRENT = VERSION_1;

    /**
     * the suffix to add to RMS id to convert it into a local id
     */
    protected String localIdSuffix;

    /**
     * the RMS store where the sync state is persisted
     */
    protected PersistentStore syncStateRecordStore;

    /**
     * the total number of contacts present in the PIM
     */
    protected int totalContactCount;

    /**
     * the total number of changes that were found
     */
    protected int totalChangesCount;

    /**
     * output stream used when writing records to the RMS
     */
    protected ByteArrayOutputStream outRecordStream;

    /**
     * the ID of the record containing the sync state info
     */
    protected int recordIdSyncStateInfo;

    protected HashMap<String, UidContactRecord> deletedContactStates;

    /**
     * Record the saved state rmsID and corresponded contact UID
     * uid,  rmsID
     */
    protected HashMap<String, Integer> recordIdAndUidMap;

    private HashMap<String, String> mContactsMap;

    /** Creates a new sync state manager. */
    public UidContactStateManager(ContactStore syncContactStore, VcardCoder syncVcardCoder, UIInterface ui, Logger synclogger) {
        super(syncContactStore, syncVcardCoder, ui, synclogger);

        localIdSuffix = null;

        syncStateRecordStore = null;

        outRecordStream = new ByteArrayOutputStream();

        recordIdSyncStateInfo = 0;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#initialize(net.cp.engine.contacts.ContactList)
     */
    @Override
    public void initialize(ContactList contactList) throws StoreException {
        super.initialize(contactList);

        // nothing more to do if the RMS stores have already been opened
        if (syncStateRecordStore != null) return;

        if (logger != null) logger.info("Initializing sync state manager for contacts with UID field");

        // open the RMS containing the persisted state of the last sync
        String syncStateRmsName = RMS_STATE_PREFIX + contacts.getName();
        syncStateRecordStore = Settings.getPersistentStoreManager().openRecordStore(syncStateRmsName);

        try {
            // setup the sync state info record (local ID suffix, etc)
            if (syncStateRecordStore.getNumRecords() <= 0) {
                // the store has just been created - create the sync state info record so it can always be found
                // with the same ID (i.e. 1)
                recordIdSyncStateInfo = writeSyncStateInfo(recordIdSyncStateInfo, "-" + Long.toString(System.currentTimeMillis()));
            } else {
                // read the sync state info record - always found at the same record ID (i.e. 1)
                recordIdSyncStateInfo = 1;
                readSyncStateInfo(recordIdSyncStateInfo);
            }
        } catch (Exception e) {
            if (logger != null) logger.error("Failed to access the sync state info record", e);

            throw new StoreException("Failed to access the sync state info record", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#close()
     */
    @Override
    public void close() {
        if (logger != null) logger.info("Closing sync state manager for contacts with UID field");

        // close the RMS containing the persisted state of the last sync
        Settings.getPersistentStoreManager().closeRecordStore(syncStateRecordStore);
        syncStateRecordStore = null;

        super.close();
    }

    /** Returns whether or not the current sync state is valid. */
    @Override
    public boolean isStateValid() {
        try {
            // The absence of records indicate that the store has been newly created and is not considered
            // to be valid. There should be at least 1 record, the first one storing version number and other information
            return ((syncStateRecordStore != null) && (syncStateRecordStore.getNumRecords() > 1));
        } catch (StoreException e) {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#getRecords(boolean, boolean)
     */
    @Override
    public DynamicContactStack getRecords(boolean changesOnly) throws StoreException {
        // update the sync state records in the RMS
        updateState(changesOnly);

        if (logger != null)
            logger.info("Got records - changesOnly=" + changesOnly + ", totalChangesCount=" + totalChangesCount + ", totalContactCount=" + totalContactCount);

        syncStateRecordStore.startEnumeration();

        // we will use a dynamic stack so we will only load the sync state information when requested
        return new DynamicContactStack(contactStore, changesOnly, totalContactCount, totalChangesCount);
    }

    private Record getNextRecordInternal(boolean changesOnly) {
        try {
            if (logger != null) logger.info("Retrieving the next record to send to the server");

            // nothing more to do if the enumeration has been closed
            if (!syncStateRecordStore.enumerationStarted()) return null;

            int recordId;
            while ((recordId = syncStateRecordStore.nextRecordId()) > 0) {
                // ignore first record, it doesn't contain a contact state but the version number
                if (recordId == 1) continue;

                UidContactRecord syncState = new UidContactRecord(contactStore);
                readSyncState(syncState, recordId, true);

                // Ignore records that have not changed if that's all we're interested in
                if ((changesOnly) && ((syncState.changeType == 0))) continue;

                // Ignore deleted records if we are interested in all records
                if ((!changesOnly) && ((syncState.changeType == Record.CHANGE_TYPE_DELETE))) continue;

                // Found a valid record to return
                if (logger != null) logger.info("Found record: " + syncState.rmsId + ", " + syncState.uid + ", " + syncState.getLocalId());
                return syncState;
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to retrieve the next record - returning null", e);
        }

        // no more records to return - stop the enumeration
        syncStateRecordStore.stopEnumeration();

        return null;
    }

    private static final int CACHE_SIZE = EngineSettings.getInstance().getContactCacheSize();
    private final Map<String, Contact> mCachedContacts = new HashMap<String, Contact>();
    private final List<Record> mCachedRecords = new LinkedList<Record>();

    @Override
    public Record getNextRecord(boolean changesOnly) {
        if (mCachedRecords.size() == 0) {
            for (int i = 0; i < CACHE_SIZE; i++) {
                Record nextRecord = getNextRecordInternal(changesOnly);

                if (nextRecord == null) {
                    break;
                } else {
                    mCachedRecords.add(nextRecord);
                }
            }

            if (mCachedRecords.size() != 0) {
                long start = System.currentTimeMillis();
                preloadContacts(mCachedRecords);
                long interval = System.currentTimeMillis() - start;
                if (logger != null) logger.info("Preloaded " + mCachedContacts.size() + " contacts on " + interval + " ms.");
            } else {
                if (logger != null) logger.debug("No contacts to preload");
            }

        }

        if (mCachedRecords.size() != 0) {
            return mCachedRecords.remove(0);
        } else {
            return null;
        }
    }

    private void preloadContacts(List<Record> mCachedRecords) {
        List<String> contactsToRead = new ArrayList<String>();
        for (Record record : mCachedRecords) {
            int changeType = record.getChangeType();

            if (changeType <= 0) {
                changeType = Record.CHANGE_TYPE_ADD;
            }

            boolean willReadContact = (changeType == Record.CHANGE_TYPE_ADD || changeType == Record.CHANGE_TYPE_REPLACE || record.isFieldLevelReplace());

            if (willReadContact) {
                UidContactRecord uidRecord = (UidContactRecord) record;
                contactsToRead.add(uidRecord.uid);
            }
        }

        if (contactsToRead.size() > 0) {
            List<Contact> preloadedContacts = contacts.readListOfContacts(contactsToRead);
            for (Contact c : preloadedContacts) {
                AndroidContactAPI5 contact = (AndroidContactAPI5) c;
                mCachedContacts.put(contact.getUID(), contact);
            }
        }
    }

    /**
     * Updates the sync state for all contacts.
     * 
     * @param changesOnly
     * @throws StoreException
     */
    protected void updateState(boolean changesOnly) throws StoreException {
        Hashtable<String, UidContactRecord> syncStateRecords;

        try {
            // initialize the total number of contacts/changes present
            totalChangesCount = 0;
            totalContactCount = contactStore.getContactSize();

            // read the current sync state records from the RMS (if there are any present)
            syncStateRecords = loadSyncStates();
            if (syncStateRecords == null) {
                if (logger != null) logger.info("Setting up initial sync state for all contacts");
            } else {
                if (logger != null) logger.info("Checking for contacts that have not changed since the last sync");
            }

            // initialize counting variables used to update the progress bar
            int processed = 0;

            // examine all contact in the PIM and match them to their sync state based on their UID field
            mContactsMap = contacts.readAllContactIdAndVersion();

            for (Entry<String, String> entry : mContactsMap.entrySet()) {
                // check if the user has chosen to abort the sync
                if (ui.getSyncState() == StatusCodes.SYNC_ABORTING) throw new StoreException("Session aborted by the user", SyncML.STATUS_OPERATION_CANCELLED);

                // if the total number of contacts is not already known, make sure we don't exceed the configured limit
                if (totalContactCount < 0) contactStore.checkContactLimit(processed + 1);

                String uid = entry.getKey();
                String version = entry.getValue();

                try {
                    // try to find the sync state associated with the contact based on the UID
                    UidContactRecord syncState = null;
                    if (syncStateRecords != null) syncState = syncStateRecords.get(uid);

                    if (syncState == null) {
                        // no matching state was found - this indicates that the contact is a new contact
                        syncState = new UidContactRecord(contactStore);
                        syncState.uid = uid;
                        syncState.changeType = Record.CHANGE_TYPE_ADD;
                        syncState.version = version;
                        // syncState.vcardHash = getContactHash(contact);
                        writeSyncState(syncState);
                        totalChangesCount++;

                        if (logger != null) logger.info("Contact '" + uid + "' with new local ID '" + syncState.getLocalId() + "' - added");
                    } else {
                        // a matching contact was found - remove the associated sync state so we can identify deleted contacts later
                        if (syncStateRecords != null) syncStateRecords.remove(uid);

                        // check if the contact has been modified since the last sync by comparing the hash of the vCard data
                        if (syncState.version != null && !syncState.version.equals(version)) {
                            // if (!UtilityClass.isByteArrayEquals(getContactVersion(contact), syncState.version)) {
                            // the contact has been modified since the last sync
                            if (logger != null) logger.info("Contact '" + uid + "' with local ID '" + syncState.getLocalId() + "' - modified");

                            syncState.changeType = Record.CHANGE_TYPE_REPLACE;
                            writeSyncState(syncState);
                            totalChangesCount++;
                        } else {
                            // the contact is unchanged
                            if (logger != null) logger.info("Contact '" + uid + "' with local ID '" + syncState.getLocalId() + "' - unchanged");

                            // check if there is a pending change from the last sync
                            if (syncState.changeType > 0) totalChangesCount++;
                        }
                    }
                } catch (Throwable e) {
                    if (logger != null) logger.error("Failed to check contact '" + uid + "' - ignoring", e);
                }

                // update the sync progress in the UI
                processed++;
                // contactStore.updateProgress(StatusCodes.SYNC_CHECKING_CONTACTS, StatusCodes.NONE, total, processed);
            }

            // if the total number of contacts was not initially known, save that information now
            if (totalContactCount < 0) {
                totalContactCount = processed;
                contactStore.setContactSize(totalContactCount);
            }

            // nothing more to do if there were no sync state records - all contacts in the PIM have already
            // been handled as adds above - no need to search for deletes
            if (syncStateRecords == null) return;

            if (logger != null) logger.info("Checking for contacts that have been deleted since the last sync");

            // the remaining unmatched sync states identify those contacts that have been deleted since the last sync
            processed = 0;

            for (Enumeration<UidContactRecord> contactsEnum = syncStateRecords.elements(); contactsEnum.hasMoreElements();) {
                UidContactRecord syncState = contactsEnum.nextElement();

                // check if the user has chosen to abort the sync
                if (ui.getSyncState() == StatusCodes.SYNC_ABORTING) throw new StoreException("Session aborted by the user", SyncML.STATUS_OPERATION_CANCELLED);

                if (logger != null) logger.info("Contact with local ID '" + syncState.getLocalId() + "' - deleted");

                if (changesOnly) {
                    syncState.changeType = Record.CHANGE_TYPE_DELETE;
                    writeSyncState(syncState);
                    totalChangesCount++;
                } else {
                    // delete the RMS entry, it's no use any more
                    syncStateRecordStore.deleteRecord(syncState.rmsId);
                }

                // update the sync progress in the UI
                processed++;
                // contactStore.updateProgress(StatusCodes.SYNC_CLIENT_UPDATES, StatusCodes.SYNC_DELETED_CONTACT, total, processed);
            }
        } catch (Exception e) {
            if (logger != null) logger.error("Failed to update the sync state", e);

            throw new StoreException("Failed to update the sync state", e);
        }
    }

    /**
     * Reads the sync states from persistent storage
     *
     * @return
     * @throws StoreException
     */
    protected Hashtable<String, UidContactRecord> loadSyncStates() throws StoreException {
        // Read the sync state from the RMS and index them by UID in a hash table

        try {
            // Nothing more to do if the RMS is empty
            int recordCount = syncStateRecordStore.getNumRecords();

            // There's always 1 record, the first one containing version number and other info
            recordCount--;
            if (recordCount <= 0) return null;

            if (logger != null) logger.info("Reading " + recordCount + " sync state records from the RMS");

            // Process each record in the sync state RMS
            // int readRecords = 0;
            int recordId;

            Hashtable<String, UidContactRecord> result = new Hashtable<String, UidContactRecord>();

            syncStateRecordStore.startEnumeration();

            while ((recordId = syncStateRecordStore.nextRecordId()) > 0) {
                // skip the first record as it only contains sync state info (local ID suffix, etc)
                if (recordId == 1) continue;

                // check if the user has chosen to abort the sync
                if (ui.getSyncState() == StatusCodes.SYNC_ABORTING) throw new StoreException("Session aborted by the user", SyncML.STATUS_OPERATION_CANCELLED);

                UidContactRecord syncState = new UidContactRecord(contactStore);
                readSyncState(syncState, recordId, true);

                // update the sync progress in the UI
                // readRecords++;
                // contactStore.updateProgress(StatusCodes.SYNC_CHECKING_CONTACTS, StatusCodes.SYNC_LOADING_STATE, recordCount, readRecords);

                result.put(syncState.uid, syncState);
            }

            return result;
        } finally {
            // stop the enumeration in all cases as we're done with it
            syncStateRecordStore.stopEnumeration();
        }
    }

    /** Reads the sync state record with the specified record ID from the RMS store.
     *
     * @param syncState
     * @param recordId
     * @param mustExist
     * @return
     * @throws StoreException
     */
    protected UidContactRecord readSyncState(UidContactRecord syncState, int recordId, boolean mustExist) throws StoreException {
        ByteArrayInputStream byteStream = null;
        DataInputStream dataStream = null;
        try {
            // read the record data
            byte[] recordData = syncStateRecordStore.readRecord(recordId);

            if (recordData == null) {
                if (mustExist) throw new StoreException("Failed to find sync state with record ID '" + recordId + "'");

                return null;
            }

            // create the streams
            byteStream = new ByteArrayInputStream(recordData);
            dataStream = new DataInputStream(byteStream);

            // read the version
            short version = dataStream.readShort();
            if ((version <= 0) || (version > VERSION_CURRENT)) throw new StoreException("Invalid version '" + version + "' found");

            // parse the binary data using a data stream
            syncState.uid = dataStream.readUTF();
            syncState.changeType = dataStream.readByte();
            syncState.rmsId = recordId;
            syncState.version = dataStream.readUTF();

            return syncState;
        } catch (IOException e) {
            if (logger != null) logger.error("Failed to read the sync state record with record ID '" + recordId + "' from the RMS", e);

            throw new StoreException("Failed to read the sync state record with record ID '" + recordId + "' from the RMS", e);
        } finally {
            // close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /** Writes the specified sync state record to the RMS store.
     *
     * @param syncState
     * @throws StoreException
     */
    public void writeSyncState(UidContactRecord syncState) throws StoreException {
        DataOutputStream dataStream = null;
        try {
            // create the streams
            outRecordStream = resetStream(outRecordStream);
            dataStream = new DataOutputStream(outRecordStream);

            // write the sync state record
            dataStream.writeShort(VERSION_CURRENT);
            dataStream.writeUTF(syncState.uid);
            dataStream.writeByte(syncState.changeType);
            dataStream.writeUTF(syncState.version);

            // write the record data
            syncState.rmsId = syncStateRecordStore.writeRecord(syncState.rmsId, outRecordStream.toByteArray());
        } catch (IOException e) {
            if (logger != null) logger.error("Failed to write the sync state record with record ID '" + syncState.rmsId + "' to the RMS", e);

            throw new StoreException("Failed to read the sync state record with record ID '" + syncState.rmsId + "' to the RMS", e);
        } catch (NullPointerException ne) {
            ne.printStackTrace();
        } finally {
            // close the streams
            UtilityClass.streamClose(dataStream, logger);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#setSyncResult(java.lang.String, boolean)
     */
    @Override
    public void setSyncResult(String localId, boolean syncSuccess) throws StoreException {
        // nothing more to do if the sync failed - we leave the state unchanged so that the record will
        // be re-sent during the next sync
        if (!syncSuccess) return;

        // read the sync state record associated with the specified record ID
        int recordId = getRmsId(localId);
        UidContactRecord syncState = new UidContactRecord(contactStore);
        readSyncState(syncState, recordId, true);

        if (syncState.changeType == Record.CHANGE_TYPE_DELETE) {
            // the contact has been deleted - no need to keep the sync state
            // record
            syncStateRecordStore.deleteRecord(syncState.rmsId);
        } else {
            // update hash value
            if (syncState.changeType == Record.CHANGE_TYPE_REPLACE) {
                if (mContactsMap != null) {
                    syncState.version = mContactsMap.get(syncState.uid);
                }
            }
            // Update the sync state record to indicate that that the record has
            // been processed
            syncState.changeType = 0;
            writeSyncState(syncState);
        }

    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#getContactIdentifier(java.lang.String)
     */
    @Override
    public String getContactIdentifier(String localId) {
        try {
            return contactStore.getContactIdentifier(getContact(localId));
        } catch (Throwable e) {
            if (logger != null) logger.error("getContactIdentifier() exception", e);
        }

        return "";
    }

    /** Reads the sync state info from the record with the specified ID.
     *
     * @param recordId
     * @throws Exception
     */
    protected void readSyncStateInfo(int recordId) throws Exception {
        ByteArrayInputStream byteStream = null;
        DataInputStream dataStream = null;
        try {
            // read the record from the RMS
            byte[] recordData = syncStateRecordStore.readRecord(recordId);

            if (recordData == null) throw new StoreException("Failed to find sync state with record ID '" + recordId + "'");

            // create the streams
            byteStream = new ByteArrayInputStream(recordData);
            dataStream = new DataInputStream(byteStream);

            // read the version
            short version = dataStream.readShort();
            if ((version <= 0) || (version > VERSION_CURRENT)) throw new Exception("Invalid version '" + version + "' found");

            // read the local ID suffix
            localIdSuffix = dataStream.readUTF();
            if ((localIdSuffix == null) || (localIdSuffix.length() <= 0)) throw new Exception("No local ID suffix found");
        } finally {
            // close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /** Writes the specified sync state info to the record with the specified ID.
     *
     * @param recordId
     * @param suffix
     * @return
     * @throws Exception
     */
    protected int writeSyncStateInfo(int recordId, String suffix) throws Exception {
        ByteArrayOutputStream byteStream = null;
        DataOutputStream dataStream = null;
        try {
            // create the streams
            byteStream = new ByteArrayOutputStream();
            dataStream = new DataOutputStream(byteStream);

            // write the sync details
            dataStream.writeShort(VERSION_CURRENT);
            dataStream.writeUTF(suffix);

            // store it in the RMS
            byte[] recordData = byteStream.toByteArray();
            recordId = syncStateRecordStore.writeRecord(recordId, recordData);

            // update the cache
            localIdSuffix = suffix;

            return recordId;
        } finally {
            // close the streams
            UtilityClass.streamClose(dataStream, logger);
            UtilityClass.streamClose(byteStream, logger);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#addSyncState(net.cp.engine.contacts.Contact)
     */
    @Override
    public String addSyncState(Contact contact) throws StoreException {
        // create a new sync state record for the specified contact and save it
        UidContactRecord syncState = new UidContactRecord(contactStore);

        syncState.uid = contact.getString(Contact.UID, 0);
        syncState.version = contact.getString(Contact.VERSION, 0);
        writeSyncState(syncState);

        return syncState.getLocalId();
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactStateManager#updateSyncState(java.lang.String, net.cp.engine.contacts.Contact)
     */
    @Override
    public void updateSyncState(String localId, Contact contact) throws StoreException {
        // read the sync state record associated with the specified record ID
        int recordId = getRmsId(localId);

        UidContactRecord syncState = new UidContactRecord(contactStore);
        readSyncState(syncState, recordId, true);

        // reset the change type to indicate that the contact has been synced
        syncState.changeType = 0;
        syncState.version = contact.getString(Contact.VERSION, 0);

        // update the sync state for the specified contact and save the record
        writeSyncState(syncState);
    }

    @Override
    public void deleteSyncState(String localId) throws StoreException {
        // read the sync state record associated with the specified record ID
        int recordId = getRmsId(localId);

        // delete the sync state record
        syncStateRecordStore.deleteRecord(recordId);
    }

    @Override
    public Contact getContact(String localId) throws StoreException {
        // read the sync state record associated with the specified record ID
        int recordId = getRmsId(localId);
        UidContactRecord syncState = new UidContactRecord(contactStore);
        if (readSyncState(syncState, recordId, false) == null) return null;

        return getContactByUid(syncState.uid);
    }

    @Override
    public Contact getMinContact(String localId) throws StoreException {
        // read the sync state record associated with the specified record ID
        int recordId = getRmsId(localId);
        UidContactRecord syncState = new UidContactRecord(contactStore);
        if (readSyncState(syncState, recordId, false) == null) return null;

        return getMinContactByUid(syncState.uid);
    }

    /**
     * Returns the contact associated with the specified UID.
     *
     * @param uid
     * @return the Contact associated with the specified UID or null if no such Contact exists.
     * @throws StoreException
     */
    protected Contact getContactByUid(String uid) throws StoreException {
        if (mCachedContacts.containsKey(uid)) {
            return mCachedContacts.remove(uid);
        }

        return contacts.readContact(uid);
    }

    /**
     * Returns the contact associated with the specified UID.
     *
     * @param uid
     * @return the Contact associated with the specified UID or null if no such Contact exists.
     * @throws StoreException
     */
    protected Contact getMinContactByUid(String uid) throws StoreException {
        if (mCachedContacts.containsKey(uid)) {
            return mCachedContacts.remove(uid);
        }

        return contacts.readMinContact(uid);
    }

    /**
     * Returns a vcard as a byte array, for the contact specified by UID
     * @param uid The contact in question's UID
     * @return The vcard as bytes
     * @throws StoreException
     */
    protected byte[] getVCardByUID(String uid) throws StoreException {

        Contact contact = getContactByUid(uid);
        if (contact != null) {
            // long start = System.currentTimeMillis();
            // encode the specified contact as a vCard
            vcardOutputStream = resetStream(vcardOutputStream);
            vcardCoder.encode(contact, vcardOutputStream);
            byte[] bytes = vcardOutputStream.toByteArray();
            // logger.info("PERFORMANCE building vcard for uid="+uid+" - time=" + (System.currentTimeMillis() - start));
            return bytes;
        }

        return null;
    }

    /**
     * Returns the local ID associated with the sync state record with the specified record ID.
     *
     * @param rmsId
     * @return
     */
    public String getLocalId(int rmsId) {
        // directly use the RMS ID as the local ID
        return rmsId + localIdSuffix;
    }

    /** Returns the record ID of the sync state record associated with the specified local ID.
     *
     * @param localId
     * @return
     * @throws StoreException
     */
    public int getRmsId(String localId) throws StoreException {
        int idx = localId.indexOf(localIdSuffix);
        if (idx > 0) return Integer.parseInt(localId.substring(0, idx));

        throw new StoreException("Invalid local ID format");
    }

    /** Returns the MD5 hash of the specified contact. */
    @Override
    public byte[] getContactHash(Contact contact) throws StoreException {
        try {
            // encode the contact as a vCard
            vcardOutputStream = resetStream(vcardOutputStream);

            if (contact.countValues(Contact.FORMATTED_NAME) > 0) {
                // fix for FN vs NAME issue.
                // make sure FN is generated from NAME so hashes match next time
                int fnAttributes = contact.getAttributes(Contact.FORMATTED_NAME, 0);
                contact.removeValue(Contact.FORMATTED_NAME, 0);
                contact.addString(Contact.FORMATTED_NAME, fnAttributes, ContactList.getFormattedNameFromArray(contact.getStringArray(Contact.NAME, 0)));
            }

            vcardCoder.encode(contact, vcardOutputStream);

            // MD5 encode the vCard
            return MD5.encode(vcardOutputStream.toByteArray());
        } catch (IllegalStateException e) {
            if (logger != null) logger.error("Failed to compute MD5 hash of contact vCard", e);

            throw new StoreException("Failed to compute MD5 hash of contact vCard", e);
        }
    }
}
