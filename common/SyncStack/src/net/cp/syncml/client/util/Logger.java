/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util;


/**
 * An interface allowing activity to be logged. 
 *
 * Implementations should log the specified events using an appropriate log mechanism. 
 *
 * @author Denis Evoy
 */
public interface Logger
{
    /**
     * Called to write debug information to the log.
     * 
     * @param message the message to be written to the log.
     */
    public void debug(String message);

    /**
     * Called to write informational data to the log.
     * 
     * @param message the message to be written to the log.
     */
    public void info(String message);

    /**
     * Called to write warnings to the log.
     * 
     * @param message the message to be written to the log.
     */
    public void warn(String message);

    /**
     * Called to write errors to the log.
     * 
     * @param message the message to be written to the log.
     */
    public void error(String message);

    /**
     * Called to write errors to the log.
     * 
     * @param message   the message to be written to the log.
     * @param cause     the underlying cause of the error.
     */
    public void error(String message, Throwable cause);
}
