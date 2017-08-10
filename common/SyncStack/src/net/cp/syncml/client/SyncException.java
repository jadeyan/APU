/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client;


/**
 * An exception indicating a generic sync problem.
 *
 * @author Denis Evoy
 */
public class SyncException extends Exception
{
    private int code;                   //the SyncML status code associated with the exception (if any)
    private String data;                //additional status data associated with the exception (if any)

    
    /** Creates a new exception with no message or cause. */
    public SyncException()
    {
        super();
    }

    /**
     * Creates a new exception with the specified message.
     * 
     * @param msg a textual description of the exception.
     */
    public SyncException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception with the specified message and cause.
     * 
     * @param msg   a textual description of the exception.
     * @param cause the underlying cause of this exception.
     */
    public SyncException(String msg, Throwable cause)
    {
        super(msg + ": " + cause.getMessage());
        
        //copy the status information from the cause (if available)
        if (cause instanceof SyncException)
        {
            code = ((SyncException)cause).code;
            data = ((SyncException)cause).data;
        }
    }
    
    /**
     * Creates a new exception with the specified message and SyncML status code.
     * 
     * @param msg           a textual description of the exception.
     * @param statusCode    the {@link SyncML#STATUS_ACCEPTED status code} associated with the exception.
     */
    public SyncException(String msg, int statusCode)
    {
        super(msg + " - status '" + statusCode + "'");
        
        code = statusCode;
        data = null;
    }
    
    /**
     * Creates a new exception with the specified message and SyncML status information.
     * 
     * @param msg           a textual description of the exception.
     * @param statusCode    the {@link SyncML#STATUS_ACCEPTED status code} associated with the exception.
     * @param statusData    the {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED status data} associated with the exception.
     */
    public SyncException(String msg, int statusCode, String statusData)
    {
        super(msg + " - status '" + statusCode + "' - reason '" + statusData + "'");
        
        code = statusCode;
        data = statusData;
    }
    
    /**
     * Creates a new exception with the specified message, cause and SyncML status information.
     * 
     * @param msg           a textual description of the exception.
     * @param statusCode    the {@link SyncML#STATUS_ACCEPTED status code} associated with the exception.
     * @param statusData    the {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED status data} associated with the exception.
     * @param cause         the underlying cause of this exception.
     */
    public SyncException(String msg, int statusCode, String statusData, Throwable cause)
    {
        super(msg + " - status '" + statusCode + "' - reason '" + statusData + "': " + cause.getMessage());
        
        code = statusCode;
        data = statusData;
    }
    
    
    /**
     * Returns the SyncML status code associated with the exception.
     * 
     * @return the {@link SyncML#STATUS_ACCEPTED status code} associated with the exception or zero if there is none.
     */
    public int getStatusCode()
    {
        return code;
    }

    /**
     * Returns any additional status data associated with the exception.
     * 
     * @return the {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED status data} associated with the exception or null if there is none.
     */
    public String getStatusData()
    {
        return data;
    }
}
