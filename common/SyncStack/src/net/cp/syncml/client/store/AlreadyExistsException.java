/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.store;


/**
 * An exception indicating that a particular record is already present in the local store.
 *
 * @author Denis Evoy
 */
public class AlreadyExistsException extends StoreException
{
    private String recordId;                 //the local ID of the existing record
    
    
    /** 
     * Creates a new exception with no message or cause.
     *  
     * @param localId   the local ID of the existing record. Must not be null or empty.
     */
    public AlreadyExistsException(String localId)
    {
        super();
        
        if ( (localId == null) || (localId.length() <= 0) )
            throw new IllegalArgumentException("no local ID specified");
        
        recordId = localId;
    }

    /**
     * Creates a new exception with the specified message.
     * 
     * @param localId   the local ID of the existing record. Must not be null or empty.
     * @param msg       a textual description of the exception.
     */
    public AlreadyExistsException(String localId, String msg)
    {
        super(msg);

        if ( (localId == null) || (localId.length() <= 0) )
            throw new IllegalArgumentException("no local ID specified");

        recordId = localId;
    }

    /**
     * Creates a new exception with the specified message and cause.
     * 
     * @param localId   the local ID of the existing record. Must not be null or empty.
     * @param msg       a textual description of the exception.
     * @param cause     the underlying cause of this exception.
     */
    public AlreadyExistsException(String localId, String msg, Throwable cause)
    {
        super(msg, cause);

        if ( (localId == null) || (localId.length() <= 0) )
            throw new IllegalArgumentException("no local ID specified");

        recordId = localId;
    }

    
    /**
     * Returns the local ID of the existing record.
     * 
     * @return The local ID of the existing record in the store. Will not be null or empty.
     */
    public String getLocalId()
    {
        return recordId;
    }
}
