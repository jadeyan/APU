/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.store;


import net.cp.syncml.client.devinfo.ContentType;


/**
 * An interface defining a single record in a local record store. <br/><br/>
 * 
 * Implementations should use this interface to encapsulate a record (or a change to a record) 
 * in the local store and to provide information about that record/change to the SyncML client.
 *
 * @see RecordStore#getAllRecords()
 * @see RecordStore#getChangedRecords()
 *
 * @author Denis Evoy
 */
public interface Record
{
    /** Indicates that the record has been added to the local store. */
    public static final int CHANGE_TYPE_ADD =       1;

    /** Indicates that the record has been modified in the local store. */
    public static final int CHANGE_TYPE_REPLACE =   2;

    /** Indicates that the record has been deleted from the local store. */
    public static final int CHANGE_TYPE_DELETE =    3;

    /** Indicates that the record has been moved in the local store. */
    public static final int CHANGE_TYPE_MOVE =      4;
    
    /** Indicates that the record has been copied in the local store. */
    public static final int CHANGE_TYPE_COPY =      5;
    
    
    /** Defines a local ID identifying the root of a record hierarchy */
    public static final String ROOT_ID = "/"; 
    
    
    /**
     * Called to close the record. <br/><br/>
     * 
     * Implementations should perform any cleanup of the record here.
     */
    public void close();

    
    /**
     * Called to retrieve the record store in which the record resides. <br/><br/>
     * 
     * Implementations must return a reference to the record store in which the record resides.
     *   
     * @return The record store associated with the record. Must not be null.
     */
    public RecordStore getRecordStore();
    
    
    /**
     * Called to retrieve the unique local ID of the record. <br/><br/>
     * 
     * Implementations must return a non-null, non-empty string which uniquely identifies the 
     * record in the local store. <br/><br/>
     * 
     * The semantics of the local ID depends on the change type of the record as follows:
     * <ul>
     *      <li> Add - the ID must identify the new record. 
     *      <li> Replace - the ID must identify the record whose content was replaced. 
     *      <li> Delete - the ID must identify the record that was deleted.
     *      <li> Copy - the ID must identify the original record from which the copy was made. 
     *      <li> Move - the ID must identify the record that was moved.
     * </ul>  
     *   
     * @return The unique ID of the record. Must not be null or empty.
     */
    public String getLocalId();

    /**
     * Called to retrieve the unique local ID of the record that is the parent of this record. <br/><br/>
     * 
     * This method is only applicable for record stores that are hierarchical in nature (e.g files/folders, etc).
     * If this is the case, implementations must return a non-null, non-empty string which uniquely identifies 
     * the record that is the parent of this record in the local store. If the parent of this record is the 
     * root of the hierarchy, {@link #ROOT_ID} should be returned. <br/><br/>
     * 
     * The semantics of the parent ID depends on the change type of the record as follows:
     * <ul>
     *      <li> Add - the ID must identify the record that is the parent of the new record. 
     *      <li> Replace - the ID is not required. 
     *      <li> Delete - the ID is not required.
     *      <li> Copy - the ID must identify the record that is the parent of the copy that was created. 
     *      <li> Move - the ID must identify the record that is the new parent of the record that was moved.
     * </ul>  
     *   
     * @return The unique ID of the parent record, or null if not applicable.
     */
    public String getParentId();
    
    /**
     * Called to retrieve the unique local ID of this copied record. <br/><br/>
     * 
     * This method is only applicable when this record encapsulates a change of type {@link #CHANGE_TYPE_COPY copy}. 
     * If so, implementations must return a non-null, non-empty string which uniquely identifies the record 
     * that is the copy of this record in the local store.
     *    
     * @return The unique ID of the copied record, or null if not applicable.
     */
    public String getTargetId();
    
    /**
     * Called to retrieve the unique local ID of the record that is the parent of this copied record. <br/><br/>
     * 
     * This method is only applicable when this record encapsulates a change of type {@link #CHANGE_TYPE_COPY copy}.
     * If so, implementations must return a non-null, non-empty string which uniquely identifies the record 
     * where this record was copied to.  If the parent of this copied record is the root of the hierarchy, 
     * {@link #ROOT_ID} should be returned.
     *   
     * @return The unique ID of the parent of the copied record, or null if not applicable.
     */
    public String getTargetParentId();
    
    /**
     * Called to retrieve the content type of the record. <br/><br/>
     * 
     * Implementations must return the MIME type of the data contained in the record.
     *   
     * @return The content type of the record. Must not be null.
     */
    public ContentType getContentType();

    /**
     * Called to retrieve the {@link #CHANGE_TYPE_ADD change type} that applies to the record. <br/><br/>
     * 
     * Implementations must return the {@link #CHANGE_TYPE_ADD change type} that applies to the 
     * record or 0 if there is no explicit change type. An explicit change type is not necessary when 
     * performing a slow two-way sync or a refresh from client, but will be required in all other cases 
     * as it indicates whether the record has been added, deleted, modified, copied or moved since the 
     * last successful sync session.
     *   
     * @return The {@link #CHANGE_TYPE_ADD change type} that applies to the record or 0 if not set.
     */
    public int getChangeType();

    /**
     * Called to determine the operation holds a field level replacement. <br/><br/>
     * 
     * This method is only applicable when the record encapsulates a change of type {@link #CHANGE_TYPE_REPLACE replace},
	 * {@link #CHANGE_TYPE_COPY copy} or {@link #CHANGE_TYPE_MOVE move}. If so, implementations must return 
	 * <code>true</code> if the data in the record contains only the fields that should be replaced rather 
	 * than the content of the entire record. As a result, only those fields will be updated by the SyncML server. 
     * 
     * @return <code>true</code> if operation holds a field level replace, return <code>false</code> otherwise.
     */
    public boolean isFieldLevelReplace();
    
    /**
     * Called to retrieve the size of the data associated with the record. <br/><br/>
     * 
     * Implementations must return the size (in bytes) of the data associated with the record. In the 
     * case where there is no data associated with the record, 0 must be returned. A valid non-zero
     * data size must be returned in all cases - the only exception is where the record encapsulates a 
     * change type of {@link #CHANGE_TYPE_DELETE delete} or {@link #CHANGE_TYPE_MOVE move}.
     *   
     * @return The size of the data associated with the record or 0 if there is no data. Must be zero or positive.
     * @throws StoreException if the data size couldn't be determined.
     */
    public long getDataSize()
        throws StoreException;

    /**
     * Called to retrieve the data associated with the record. <br/><br/>
     * 
     * Implementations must return the specified amount of data associated with the record after placing 
     * exactly <code>length</code> bytes of data into the specified buffer. Note that this method will only 
     * be called where necessary. Therefore it's best to delay loading the record data into memory until the 
     * call is actually made.
     *   
     * @param buffer the buffer to write the data into. Will not be null.
     * @param length the number of bytes to place in the buffer. Will be non-zero and positive.
     * @return The number of bytes placed in the buffer. Must be the same as <code>length</code>.
     * @throws StoreException if the data couldn't be retrieved.
     */
    public int getData(byte[] buffer, int length) 
        throws StoreException;    
}
