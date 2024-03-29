/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.store;


/**
 * An exception indicating that a particular record is not present in the local record store.
 *
 * @author Denis Evoy
 */
public class NoSuchRecordException extends StoreException
{
    /** Creates a new exception with no message or cause. */
    public NoSuchRecordException()
    {
        super();
    }

    /**
     * Creates a new exception with the specified message.
     * 
     * @param msg a textual description of the exception.
     */
    public NoSuchRecordException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception with the specified message and cause.
     * 
     * @param msg a textual description of the exception.
     * @param cause the underlying cause of this exception.
     */
    public NoSuchRecordException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
