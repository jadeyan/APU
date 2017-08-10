/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;
import net.cp.syncml.client.store.StoreException;

/**
 * @author James O'Connor
 */
public interface PersistentStoreManager
{
    /**
     * Opens the record store with the specified name, creating it if necessary.
     *
     * @param storeName     the name of the store to delete. Must not be null or empty.
     * @return the opened record store.
     * @throws StoreException if the record store could not be opened or created.
     */
    public PersistentStore openRecordStore(String storeName)
        throws StoreException;

    /**
     * Opens the record store with the specified name.
     *
     * @param storeName     the name of the store to delete. Must not be null or empty.
     * @param create        if TRUE, the record store will be created if it doesn't exist.
     * @return the opened record store or null if it doesn't exist.
     * @throws StoreException if the record store could not be opened or created.
     */
    public PersistentStore openRecordStore(String storeName, boolean create)
        throws StoreException;


    /**
     * Closes the specified record store.
     *
     * @param store the record store to close.
     */
    public void closeRecordStore(PersistentStore store);

    /**
     * Deletes the record store with the specified name.
     *
     * @param storeName     the name of the store to delete. Must not be null or empty.
     * @throws StoreException if the record store could not be deleted.
     */
    public void deleteRecordStore(String storeName)
        throws StoreException;

    /** Returns the names of the record stores owned by the application. */
    public String[] listRecordStores();
}
