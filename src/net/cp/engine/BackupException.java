/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

/**
 * A class representing a general exception that occurred during the application.
 *
 * @author Denis Evoy
 */
public class BackupException extends Exception
{
    private static final long serialVersionUID = 7584834968593796567L;
    /**
     * The localized error message for display to the user
     */
    String localizedMessage;

    /**
     * @param message The error message
     */
    public BackupException(String message)
    {
        super(message);

        localizedMessage = null;
    }

    /**
     * @param message The error message
     * @param cause The exception that caused the problem, if any
     */
    public BackupException(String message, Throwable cause)
    {
        super(message + " - Caused by: " + cause.getClass().getName() + " : " + cause.getMessage());

        localizedMessage = null;
        if (cause instanceof BackupException)
            localizedMessage = ((BackupException)cause).getUserMessage();
    }

    /**
     * @param message error message
     * @param userMessage The localized error message to be displayed to the user
     */
    public BackupException(String message, String userMessage)
    {
        super(message);

        localizedMessage = userMessage;
    }

    /**
     * @param message The error message
     * @param cause The exception that caused the problem, if any
     * @param userMessage The localized error message to be displayed to the user
     */
    public BackupException(String message, Throwable cause, String userMessage)
    {
        super(message + " - Caused by: " + cause.getClass().getName() + " : " + cause.getMessage());

        localizedMessage = userMessage;
    }


    /**
     * @return the localized error message to be displayed to the user
     */
    public String getUserMessage()
    {
        return localizedMessage;
    }
}
