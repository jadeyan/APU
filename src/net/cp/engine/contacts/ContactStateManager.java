/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import java.io.ByteArrayOutputStream;

import net.cp.engine.UIInterface;
import net.cp.syncml.client.store.Record;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;

/**
 * An abstract class responsible for managing the sync state for a collection of contacts. <br/><br/>
 * 
 * The sync state is typically used to record which contacts in the store have been synced 
 * and to map SyncML local IDs to contacts in the store. It is also used to determine which
 * contacts have been added/modified/deleted since the last sync. <br/><br/>
 * 
 * The mechanism which performs these tasks (i.e. mapping local IDs to contacts and determining 
 * if a contact has changed) depends on what is supported by the platform. Therefore, it is 
 * necessary to sub-class this class to implement these tasks appropriately.
 * 
 * @author Denis Evoy
 */
public abstract class ContactStateManager {
    /**
     * the record store whose state is being managed
     */
    protected ContactStore contactStore;

    /**
     * the vCard encoder/decoder to use
     */
    protected VcardCoder vcardCoder;

    /**
     * the interface allowing access to the UI
     */
    protected UIInterface ui;

    /**
     * the logger to use to log activity
     * 
     */
    protected Logger logger;

    /**
     * the contacts whose state is being managed
     */
    protected ContactList contacts;

    /**
     * the output stream used to vCard encode contacts
     */
    protected ByteArrayOutputStream vcardOutputStream;

    /** Creates a new sync state manager. */
    public ContactStateManager(ContactStore syncContactStore, VcardCoder syncVcardCoder, UIInterface ui, Logger synclogger) {
        contactStore = syncContactStore;
        vcardCoder = syncVcardCoder;
        this.ui = ui;
        logger = synclogger;

        contacts = null;

        vcardOutputStream = new ByteArrayOutputStream();
    }

    /** Returns whether or not the current sync state is valid. */
    public abstract boolean isStateValid();

    /** Returns the records in the store that are to be synced. */
    public abstract DynamicContactStack getRecords(boolean changesOnly) throws StoreException;

    /** Sets the result of the sync for the contact with the associated local ID. */
    public abstract void setSyncResult(String localId, boolean syncSuccess) throws StoreException;

    /** Returns an identifier that can be used by the user to identify the contact with the associated local ID. */
    public abstract String getContactIdentifier(String localId);

    /** Adds a new sync state record for the specified contact and returns its new local ID. */
    public abstract String addSyncState(Contact contact) throws StoreException;

    /** Updates the sync state record with the associated local ID for the specified contact. */
    public abstract void updateSyncState(String localId, Contact contact) throws StoreException;

    /** Deletes the sync state record with the associated local ID. */
    public abstract void deleteSyncState(String localId) throws StoreException;

    /** Returns the contact associated with the specified local ID.
     * 
     *  @return the contact associated to the specified local ID or null if such a contact does not exist
     */
    public abstract Contact getContact(String localId) throws StoreException;

    public abstract Contact getMinContact(String localId) throws StoreException;

    /** 
     * Initializes the state manager for the specified contact list.
     *  
     * @throws StoreException if the state manager couldn't be initialized. 
     */
    public void initialize(ContactList contactList) throws StoreException {
        // save a reference to the contact list
        contacts = contactList;
    }

    /** Close the sync state manager. */
    public void close() {
        contacts = null;
    }

    /** Returns the next available record to send to the SyncML server. */
    public Record getNextRecord(boolean changesOnly) {
        // this only needs to be implemented when using a "DynamicContactStack" to return records
        return null;
    }

    /** Resets the specified output stream or returns a new stream if it failed to be reset. */
    protected ByteArrayOutputStream resetStream(ByteArrayOutputStream stream) {
        try {
            // try to reset the stream
            stream.reset();
            return stream;
        } catch (Throwable e) {
            // the reset attempt failed so just return a new stream
            return new ByteArrayOutputStream();
        }
    }

    /** Returns the MD5 hash of the specified contact. */
    public abstract byte[] getContactHash(Contact contact) throws StoreException;
}
