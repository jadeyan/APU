/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.core;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import net.cp.engine.EngineSettings;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.util.Logger;
import android.os.Environment;
import android.util.Log;

/**
 * This class provides a Logger implementation for file and Android logcat based logging.
 * It can provide two separate Loggers, one for UI, and one for the service.
 * It is expected that all UI Activitys will reside in the same process and same JVM.
 * Therefore they will use the same instance of the Logger, and log to the same file.
 * However, the Engine/Service will run in a different process, and so must log to a different
 * file to prevent file locking issues. It therefore has it's own instance of Logger, with a separate logPath.
 *
 * @author James O'Connor
 *
 */
public class AppLogger implements Logger
{
    private static AppLogger eInstance = null;
    private static AppLogger uiInstance = null;

    private int references = 0;

    private EngineSettings settings;
    private String logPath;
    private FileWriter writer;

    /**
     * Sets up logging for the engine/service.
     *
     * @param settings EngineSettings object, used to determine logging levels etc...
     * @param logPath The path to the log file. MUST NOT BE THE SAME AS THE UI LOGPATH!
     */
    public static void initEngineLogger(EngineSettings settings, String logPath)
    {
        if(eInstance == null && settings != null && settings.logType > 0)
        {
            eInstance = new AppLogger(settings, logPath);
            eInstance.info("Created Engine Logger with path: " + logPath);
        }
    }

    /**
     * Sets up logging for the UI.
     *
     * @param settings EngineSettings object, used to determine logging levels etc...
     * (this is not a typo, do not try to pass in UISettings objects here!)
     *
     * @param logPath The path to the log file. SHOULD NOT BE THE SAME AS THE ENGINE LOGPATH!
     */
    public static void initUILogger(EngineSettings settings, String logPath)
    {
        if(uiInstance == null && settings != null && settings.logType > 0)
        {
            uiInstance = new AppLogger(settings, logPath);
            uiInstance.info("Created UI Logger with path: " + logPath);
        }
    }

    /**
     * @param settings
     * @param logPath
     */
    private AppLogger(EngineSettings settings, String logPath)
    {
        this.settings = settings;
        this.logPath = Environment.getExternalStorageDirectory() + logPath;

        try
        {
            if(logPath != null && UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_FILE))
            {
                //create/open log file, truncate it if necessary
                new File(logPath).createNewFile();
                writer = new FileWriter(logPath, false);
            }
        }

        catch(Throwable e)
        {
            //what else can we do?
            e.printStackTrace();
        }
    }

    /**
     * appends the supplied message and a carriage-return to the end of the log file,
     * if file logging is enabled.
     *
     * @param message the data to be logged
     */
    private void logToFile(String message)
    {

        if(logPath != null && writer != null && UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_FILE))
        {
            try
            {
                Calendar calendar = Calendar.getInstance();

                //make sure timestamp has uniform length, so logs can be merged and sorted by timestamp
                String hours = "" + calendar.get(Calendar.HOUR_OF_DAY);
                while(hours.length() < 2)
                    hours = "0" + hours;

                String minutes = "" + calendar.get(Calendar.MINUTE);

                while(minutes.length() < 2)
                    minutes = "0" + minutes;

                String seconds = "" + calendar.get(Calendar.SECOND);

                while(seconds.length() < 2)
                    seconds = "0" + seconds;

                String milliSeconds = "" + calendar.get(Calendar.MILLISECOND);

                while(milliSeconds.length() < 3)
                    milliSeconds = "0" + milliSeconds;

                String timeStamp = hours + ":" + minutes + ":" + seconds + ":" + milliSeconds;

                String[] lines = UtilityClass.split(message, "\n");
                StringBuffer finalMessage = new StringBuffer();

                String memory = getMemoryString();

                String ticks = null;

                //usually there will only be one line, but for vcards etc, there may be many
                //adding the ".0000", ".0001" etc... ensures that the UI/Engine logs can be merged and sorted properly
                for(int i=0; i<lines.length; i++)
                {
                    ticks = "" + i;
                    while(ticks.length() < 5) //allows for 99999 newline chars per single message, or the traces from a 5.4mb base64 encoded field
                        ticks = "0" + ticks;

                    finalMessage.append(timeStamp + "." + ticks + " " + memory + " " + lines[i] + "\n");
                }

                //in case someone tries to log while the settings are in reinit()
                synchronized(settings)
                {
                    writer.append(finalMessage.toString());
                    writer.flush();
                }
            }

            catch (IOException e)
            {
                //what else can we do?
                e.printStackTrace();
            }

        }
    }

    /**
     * @return a String of the form "[1234KB]" where 1234KB is the amount of memory <b>used</b>
     */
    public String getMemoryString()
    {
       return  "[" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/1024 + "KB]";
    }


    /* (non-Javadoc)
     * @see net.cp.syncml.client.util.Logger#debug(java.lang.String)
     */
    public void debug(String message)
    {
        if(UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_LOGCAT)
           && UtilityClass.isFlagSet(settings.logLevel, EngineSettings.LOG_LEVEL_DEBUG))
        {

            Log.d("CP-Sync", getMemoryString() + " " + message);
        }

        logToFile("DEBUG: " + getMemoryString() + " " + message);
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.util.Logger#error(java.lang.String)
     */
    public void error(String message)
    {
        if(UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_LOGCAT)
                && UtilityClass.isFlagSet(settings.logLevel, EngineSettings.LOG_LEVEL_ERROR))
        {

            Log.e("CP-Sync", getMemoryString() + " " + message);
        }

        logToFile("ERROR: " + getMemoryString() + " " + message);
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.util.Logger#error(java.lang.String, java.lang.Throwable)
     */
    public void error(String message, Throwable cause)
    {
        if(UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_LOGCAT)
                && UtilityClass.isFlagSet(settings.logLevel, EngineSettings.LOG_LEVEL_ERROR))
        {

            Log.e("CP-Sync", getMemoryString() + " " + message, cause);
        }

        String causeString = null;

        if(cause != null)
            causeString = cause.toString();

        logToFile("ERROR: " + getMemoryString() + " " + message + "; Caused by: " + causeString);
    }


    /* (non-Javadoc)
     * @see net.cp.syncml.client.util.Logger#info(java.lang.String)
     */
    public void info(String message)
    {
        if(UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_LOGCAT)
                && UtilityClass.isFlagSet(settings.logLevel, EngineSettings.LOG_LEVEL_INFO))
        {
            Log.i("CP-Sync", getMemoryString() + " " + message);
        }

        logToFile("INFO: " + getMemoryString() + " " + message);
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.util.Logger#warn(java.lang.String)
     */
    public void warn(String message)
    {
        if(UtilityClass.isFlagSet(settings.logType, EngineSettings.LOG_TYPE_LOGCAT)
                && UtilityClass.isFlagSet(settings.logLevel, EngineSettings.LOG_LEVEL_WARNING))
        {
            Log.w("CP-Sync", getMemoryString() + " " + message);
        }

        logToFile("WARNING: " + getMemoryString() + " " + message);
    }

    /**
     * Return an instance of the engine logger if logging is configured.
     * A call to initialize() should be made first.
     *
     * @return The logger instance, or null if logging is not turned on
     */
    public static Logger getEngineInstance()
    {
        if(eInstance != null)
            eInstance.references++;

        return eInstance;
    }

    /**
     * Return an instance of the engine logger if logging is configured.
     * A call to initialize() should be made first.
     *
     * @return The logger instance, or null if logging is not turned on
     */
    public static Logger getUIInstance()
    {
        if(uiInstance != null)
            uiInstance.references++;

        return uiInstance;
    }

    /**
     * Should be called when the Activity or Service that intitialised a logger is about to be destroyed.
     */
    public static void close(AppLogger instance)
    {
        if(instance != null)
        {
            if(instance.references > 0)
                instance.references--;

            if(instance.references <= 0)
            {
                synchronized(instance.settings)
                {
                    UtilityClass.writerClose(instance.writer, null);

                    if(instance == uiInstance)
                        uiInstance = null;
                    else if(instance == eInstance)
                        eInstance = null;
                }
            }
        }
    }

    /**
     * Closes and re-opens the log, truncating it in the process
     *
     * @param instance The logger in question
     */
    public static void resetLog(AppLogger instance)
    {

        try
        {
            synchronized(instance.settings)
            {
                if(instance.logPath != null && UtilityClass.isFlagSet(instance.settings.logType, EngineSettings.LOG_TYPE_FILE))
                {
                    UtilityClass.writerClose(instance.writer, null);
                    instance.writer = new FileWriter(instance.logPath, false);
                }
            }
        }

        catch (IOException e)
        {
            //what else can we do?
            e.printStackTrace();
        }
    }
}
