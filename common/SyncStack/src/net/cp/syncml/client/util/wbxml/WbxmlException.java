/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.wbxml;
import java.lang.Exception;


/**
 * An exception indicating a generic WBXML encoding/decoding problem.
 *
 * @author Denis Evoy
 */
public class WbxmlException extends Exception
{
    /** Creates a new exception with no message or cause. */
    public WbxmlException()
    {
        super();
    }

    /**
     * Creates a new exception with the specified message.
     * 
     * @param msg a textual description of the exception.
     */
    public WbxmlException(String msg)
    {
        super(msg);
    }

    /**
     * Creates a new exception with the specified message and cause.
     * 
     * @param msg   a textual description of the exception.
     * @param cause the underlying cause of this exception.
     */
    public WbxmlException(String msg, Throwable cause)
    {
        super(msg + ": " + cause.getMessage());
    }
}
