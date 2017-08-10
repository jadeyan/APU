/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery;


import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import net.cp.mtk.common.io.StreamUtils;
import net.cp.mtk.j2me.tools.discovery.files.FileSystem;


public class Logger
{
    public static final int SEVERITY_CRITICAL = 0;
    public static final int SEVERITY_HIGH =     1;
    public static final int SEVERITY_MEDIUM =   2;
    public static final int SEVERITY_LOW =      3;

    
    private static Logger instance = null;

    private FileConnection logFile;
    private OutputStream logStream;
    private StringBuffer logBuffer;
    
    //store issues based on severity
    private Vector[] issues = new Vector[4];


    public static synchronized void initialize(String logFileUrl)
    {
        if (instance == null)
            instance = new Logger(logFileUrl);
    }

    public static synchronized void shutdown()
    {
        if (instance != null)
            instance.close();
        instance = null;
    }

    public static synchronized String getOutputUrl()
    {
        if (instance != null)
            return instance.getLogFileUrl();
        return null;
    }

    public static synchronized void setOutputUrl(String url)
    {
        if (instance != null)
            instance.setLogFileUrl(url);
    }

    public static synchronized String getOutputBuffer()
    {
        if ( (instance != null) && (instance.logBuffer != null) )
            return instance.logBuffer.toString();
        return null;
    }
    
    public static synchronized Vector getIssues(int severity)
    {
        if (instance != null)
            return instance.issues[severity];
        return null;
    }

    public static synchronized void log(String logString)
    {
        if (instance != null)
            instance.printLog(logString, null);
    }

    public static synchronized void log(String logString, Throwable cause)
    {
        if (instance != null)
            instance.printLog(logString, cause);
    }

    public static synchronized void logIssue(int severity, String logString)
    {
        if (instance != null)
            instance.recordIssue(severity, logString, null);
    }

    public static synchronized void logIssue(int severity, String logString, Throwable cause)
    {
        if (instance != null)
            instance.recordIssue(severity, logString, cause);
    }

    public static synchronized void logIssues()
    {
        if (instance != null)
            instance.printIssues();
    }


    private Logger(String logFileUrl)
    {
        //create an in-memory buffer where we store the log in the case where we have no open log file
        logBuffer = new StringBuffer(4096);

        //create the collection of issues
        for (int i = 0; i < issues.length; i++)
            issues[i] = new Vector();

        //setup the log file (if one was specified)
        logFile = null;
        logStream = null;
        if ( (logFileUrl != null) && (logFileUrl.length() > 0) )
            setLogFileUrl(logFileUrl);            
    }
    
    private synchronized void close()
    {
        StreamUtils.closeStream(logStream);
        logStream = null;

        FileSystem.closeFile(logFile);
        logFile = null;
        
        logBuffer = null;

        for (int i = 0; i < issues.length; i++)
        {
            if (issues[i] != null)
                issues[i].removeAllElements();
        }
    }
    
    private synchronized String getLogFileUrl()
    {
        if (logFile != null)
            return logFile.getURL();
        
        return null;
    }
    
    private synchronized void setLogFileUrl(String url)
    {
        if ( (url == null) || (url.length() <= 0) )
            return;
        
        //make sure we're not already logging to a file
        if (logFile != null)
            return;
        
        //open the log file
        try
        {
            logFile = (FileConnection)Connector.open(url);
            if (logFile.exists())
                logFile.delete();
            logFile.create();
            logStream = logFile.openOutputStream();
        }
        catch (Throwable e)
        {
            //cleanup
            StreamUtils.closeStream(logStream);
            logStream = null;

            FileSystem.closeFile(logFile);
            logFile = null;
        }

        //write whatever we have already buffered to the file and clear the buffer
        if (logBuffer != null)
        {
            try
            {
                if (logBuffer.length() > 0)
                    logStream.write( logBuffer.toString().getBytes("UTF-8") );
                logBuffer = null;
            }
            catch (UnsupportedEncodingException e)
            {
                logIssue(SEVERITY_CRITICAL, "UTF-8 encoding is not supported", e);
            }
            catch (Throwable e)
            {
                logIssue(SEVERITY_CRITICAL, "Failed to write log buffer to file '" + url + "'", e);
            }
        }
    }

    private synchronized void recordIssue(int severity, String logString, Throwable cause)
    {
        if ( (severity < 0) || (severity >= issues.length) )
            return;

        if (cause != null)
            logString = logString + ": " + cause;
        
        issues[severity].addElement(logString);
    }

    private synchronized void printIssues()
    {
        printIssues(SEVERITY_CRITICAL, "CRITICAL: ");
        printIssues(SEVERITY_HIGH,     "HIGH:     ");
        printIssues(SEVERITY_MEDIUM,   "MEDIUM:   ");
        printIssues(SEVERITY_LOW,      "LOW:      ");
    }

    private synchronized void printIssues(int severity, String label)
    {
        Vector issueStrings = issues[severity];
        for (int i = 0; i < issueStrings.size(); i++)
            printLog(label + issueStrings.elementAt(i), null);            
    }
    
    private synchronized void printLog(String logString, Throwable cause)
    {
        try
        {
            if (cause != null)
                logString = logString + ": " + cause;

            System.out.println(logString);
            
            if (logBuffer != null)
            {
                logBuffer.append(logString);
                logBuffer.append("\r\n");
            }
            
            if (logStream != null)
            {
                logStream.write( logString.getBytes("UTF-8") );
                logStream.write( "\r\n".getBytes("UTF-8") );
            }
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
}
