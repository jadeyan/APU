/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test;


import java.io.*;
import java.util.*;
import java.text.*;

import net.cp.syncml.client.util.Logger;



/**
 * A default SyncLogger class that logs sync session events to the console or a file. <br/><br/>
 * 
 * Log statements are in the form:
 * <pre> &ltTimeStamp&gt [ThreadName] &ltMessage&gt</pre>
 * 
 * Example:
 * <pre>
 *      16:40:10.703 [main] initializing manager
 *      16:40:10.703 [main] initializing session
 *      16:40:10.781 [main] logging in to ...
 * </pre>
 *
 * @see     Logger
 * 
 * @author  Denis Evoy
 */
public class TestSyncLogger implements Logger
{
    private static final SimpleDateFormat DATE_STAMP =  new SimpleDateFormat("HH:mm:ss.SSS");
    
    private PrintStream outputStream = null;                //the output stream to write to
    private boolean closeStream = false;                    //indicates whether or not the stream should be closed
    
    
    /** Creates a new default logger which logs information to <code>STDOUT</code>. */
    public TestSyncLogger() 
    { 
        super(); 
        
        outputStream = System.out;
    }

    /**
     * Creates a new default logger which logs information to the specified stream.
     * 
     * @param stream the output stream to write to 
     */
    public TestSyncLogger(PrintStream stream)
    { 
        super();
        
        if (stream == null)
            throw new IllegalArgumentException("no stream specified");
            
        outputStream = stream;
    }
    
    /** 
     * Creates a new default logger which logs information to the specified file.
     * 
     * If the file already exists, it will be appended to.
     * 
     * @param filename  the name of the log file. 
     */
    public TestSyncLogger(String filename)
    { 
        super();
        
        if (filename == null)
            throw new IllegalArgumentException("no filename specified");
            
        try
        {        
            outputStream = new PrintStream(new FileOutputStream(filename, true), true);
            closeStream = true;
        }
        catch (FileNotFoundException e)
        {
            throw new IllegalArgumentException("invalid filename specified: " + e.getMessage());
        }
    }

    
    /** Closes any resources held by this logger. */
    protected void finalize()
    {
        if (closeStream)
            outputStream.close();            
    }

    
    public void debug(String message)
    {
        log("DEBUG: " + message);
    }

    public void info(String message)
    {
        log("INFO: " + message);
    }

    public void warn(String message)
    {
        log("WARN: " + message);
    }

    public void error(String message)
    {
        log("ERROR: " + message);
    }

    public void error(String message, Throwable cause)
    {
        log("ERROR: " + message);
        if (cause != null)
            cause.printStackTrace(outputStream);
    }
    
    
    /* Logs the specified message. */
    private void log(String message)
    {
        //get the current date
        String timeStamp = DATE_STAMP.format(new Date());
        
        //get the thread info
        String contextID = Thread.currentThread().getName();
        
        //get the amount of free memory
        long freeMemory = Runtime.getRuntime().freeMemory() / 1024;
        
        //build the trace prefix
        String prefix = timeStamp + " [" + contextID + "] [" + freeMemory + "KB] ";

        //print each line
        StringTokenizer stLines = new StringTokenizer(message.trim(), "\r\n", false);
        for (int i = 0; stLines.hasMoreTokens(); i++)
        {
            if (i == 0)
                outputStream.println(prefix + stLines.nextToken());
            else
                outputStream.println(prefix + "    " + stLines.nextToken());
        }
    }
}
