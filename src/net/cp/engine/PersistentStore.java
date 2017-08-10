/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import net.cp.syncml.client.store.StoreException;

/**
 * An interface encapsulating a record store which can be used to persist data on the device. <br/><br/>
 * 
 * This interface encapsulates the standard MIDP {@link RecordStore} implementation to allow records to be stored on the Android device.
 * 
 * @author James O'Connor
 */
public interface PersistentStore
{

    /** 
     * Opens the record store if it is not already open.
     *  
     * @throws StoreException if the store couldn't be opened or created. 
     */
    public boolean open(boolean create) 
        throws StoreException;

    /**
     *  Closes the record store.
     */
    public void close();
   
    /** 
     * Returns the number of records in the record store.
     *  
     * @return the number of records in the record store.
     * @throws StoreException if the number of records couldn't be determined.
     */
    public int getNumRecords()
        throws StoreException;

    /** 
     * Starts enumerating the records in the record store. <br/><br/>
     * 
     * Note that only one enumeration can be performed at any one time. If an enumeration is already 
     * in progress, an exception will be thrown.
     *  
     * @throws StoreException if the enumeration couldn't be started. 
     */
    public void startEnumeration()
        throws StoreException;

    /** Stops any enumeration of records that may be in progress. */
    public void stopEnumeration();
    
    /** 
     * Returns TRUE if an enumeration of the record store has been started.
     *  
     * @return TRUE if an enumeration of the record store has been started.
     */
    public boolean enumerationStarted();
    
    /** 
     * Returns the ID of the next record in the enumeration.
     *  
     * @return the ID of the next record in the enumeration or 0 if there are no more records.
     * @throws StoreException if the next record couldn't be retrieved. 
     */
    public int nextRecordId()
        throws StoreException;
    
    /** 
     * Reads the record with the specified record ID from the store.
     *  
     * @param recordId the ID of the record to read. Note 1 is the first record.
     * @return the record data or null if the record doesn't exist.
     * @throws StoreException if the record data couldn't be read.
     */
    public byte[] readRecord(int recordId)
        throws StoreException;
    
    /** 
     * Writes the specified data to the record store (adding a new record if necessary).
     *  
     * @param recordId  the ID of the record to update or 0 if a new record should be added. Note 1 is the first record.
     * @param data      the data to write.
     * @return the ID of the record which was written.
     * @throws StoreException if the record couldn't be written.
     */
    public int writeRecord(int recordId, byte[] data)
        throws StoreException;
    
    /** 
     * Deletes the record with the specified ID from the record store.
     *  
     * @param recordId the ID of the record to delete. 
     * @throws StoreException if the record couldn't be deleted.
     */
    public void deleteRecord(int recordId)
        throws StoreException;    

    /** 
     * Returns the number of bytes available in the record store.
     * 
     * @return the number of bytes available in the record store.
     */
    public int getSizeAvailable();
}
