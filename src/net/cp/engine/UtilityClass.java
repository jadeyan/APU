/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import net.cp.syncml.client.util.DateTime;
import net.cp.syncml.client.util.Logger;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * A class defining various useful static methods.
 *
 * @author Denis Evoy
 */
public class UtilityClass
{
    //Definition of the GMT time zones used for calendar operations
    private static final TimeZone TIMEZONE_GMT = TimeZone.getTimeZone("GMT");

    /* Creates a new utility class - private to prevent creation. */
    private UtilityClass()
    {
        super();
    }

    /** Returns a new local ID based on the current time. */
    public static String getLuid()
    {
        return Long.toString( System.currentTimeMillis() );
    }

    /** Parses the specified string into tokens separated by the specified separator character. */
    public static void getTokens(String string, Vector<String> tokens, char separator)
    {
        StringBuffer token = new StringBuffer();
        int stringLen = string.length();
        for (int i = 0; i < stringLen; i++)
        {
            //skip escaped characters
            char c = string.charAt(i);
            if ( (c == '\\') && ((i + 1) < stringLen) )
            {
                token.append( string.charAt(i + 1) );
                i++;
                continue;
            }

            //check for a separator character
            if (c == separator)
            {
                tokens.addElement( token.toString() );
                token.setLength(0);
                continue;
            }

            token.append(c);
        }

        //add the last token
        tokens.addElement( token.toString() );
    }

    /** Returns whether or not the specified value is present in the specified array. */
    public static boolean contains(int[] array, int value)
    {
        if ( (array == null) || (array.length <= 0) )
            return false;

        for (int i = 0; i < array.length; i++)
        {
            if (array[i] == value)
                return true;
        }

        return false;
    }

    /**
     * Reads a byte array from the specified input stream.
     * NOTE the input stream must contain the length of the array as an integer before the bytes
     */
    public static byte[] readBytes(DataInputStream stream)
        throws IOException
    {
        //read the length of the data
        int length = stream.readInt();

        //read the specified number of bytes
        byte[] data = new byte[length];
        for (int i = 0; i < length; i++)
            data[i] = stream.readByte();

        return data;
    }

    /**
     *  Write the length of the array as an integer, followed by the array bytes.
     */
    public static void writeBytes(DataOutputStream stream, byte[] data)
        throws IOException
    {
        //write the length of the data
        stream.writeInt(data.length);

        //write the specified number of bytes
        for (int i = 0; i < data.length; i++)
            stream.writeByte(data[i]);
    }

    /** Closes the specified input stream. */
    public static void streamClose(InputStream stream, Logger logger)
    {
        try
        {
            //close the byte stream
            if (stream != null)
                stream.close();
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to close input stream - ignoring", e);
        }
    }

    /** Closes the specified output stream. */
    public static void streamClose(OutputStream stream, Logger logger)
    {
        try
        {
            //close the byte stream
            if (stream != null)
                stream.close();
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to close output stream - ignoring", e);
        }
    }

    /** Closes the specified reader. */
    public static void streamClose(Reader stream, Logger logger)
    {
        try
        {
            //close the reader stream
            if (stream != null)
                stream.close();
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to close stream reader - ignoring", e);
        }
    }

    /** Read a non-empty, optionally trimmed line, from the specified character reader stream. */
    public static String readNextLine(InputStream is, String charset, boolean trim)
        throws IOException
    {
        ByteArrayOutputStream bios = new ByteArrayOutputStream();
        int charRead = is.read();

        while (charRead != -1)
        {
            // skip garbage
            while (charRead != -1 && charRead <= 0x20)
                charRead = is.read();

            // read everything until CR of LF
            while (charRead != -1 && charRead != 0x0A && charRead != 0x0D)
            {
                bios.write(charRead);
                charRead = is.read();
            }

            // check non empty result
            if (bios.size() > 0)
            {
                // convert the byte array stream to a string
                String s;
                if (charset == null)
                    s = new String(bios.toByteArray());
                else
                    s = new String(bios.toByteArray(), charset);

                if (trim)
                    s = s.trim();
                if (s.length() > 0)
                    return s;

                // empty result, reset byte array stream and continue
                bios.reset();
            }
        }

        return null;
    }

    /** Returns the file name from the specified path (removing trailing slash if required). */
    public static String getBaseName(String name, boolean clean)
    {
        int length = name.length();
        if (length == 0)
            return name;

        //check if there is a trailing separator
        int endIndex = -1;
        int startIndex = -1;
        if (name.charAt(length - 1) == '/')
        {
            //indicate if this trailing separator character should be ignored in the final output
            if (clean)
                endIndex = length - 1;

            //search backwards for the next separator (ignoring the trailing separator)
            startIndex = name.lastIndexOf('/', length - 2);
        }
        else
        {
            //search backwards for the next separator
            startIndex = name.lastIndexOf('/');
        }

        //no separator was found - return everything from the start of the string until the end index
        if (startIndex < 0)
        {
            if (endIndex < 0)
                return name;
            return name.substring(0, endIndex);
        }

        //separator was found - return everything from the after the separator until the end index
        if (endIndex < 0)
            return name.substring(startIndex + 1);
        return name.substring(startIndex + 1, endIndex);
    }

    /** Returns the file directory name from the specified path. */
    public static String getDirName(String name)
    {
        int length = name.length();
        if (length == 0)
            return "";

        //check if there is a trailing separator
        int endIndex = -1;
        if (name.charAt(length - 1) == '/')
        {
            //search backwards for the next separator (ignoring the trailing separator)
            endIndex = name.lastIndexOf('/', length - 2);
        }
        else
        {
            //search backwards for the next separator
            endIndex = name.lastIndexOf('/');
        }

        //separator was found - return everything from the start of the string until the separator
        if (endIndex > 0)
            return name.substring(0, endIndex + 1);

        //no separator was found - no directory name to return
        return "";
    }

    /**
     * Returns the current date as a DateTime object.
     *
     * @return A corresponding DateTime object.
     */
    public static DateTime toDateTime()
    {
        return toDateTime( new Date() );
    }

    /** Returns the specified time as a DateTime object. */
    public static DateTime toDateTime(long time)
    {
        return toDateTime( new Date(time) );
    }

    /** Returns the specified date as a DateTime object. */
    public static DateTime toDateTime(Date date)
    {
        //create the calendar object
        Calendar calendar = Calendar.getInstance(TIMEZONE_GMT);
        calendar.setTime(date);

        //retrieve the time components - note that we increment the month so that it is in the expected
        //range (i.e. 1-12)
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        //build the date/time object
        return new DateTime(year, month, day, hour, minutes, seconds, true);
    }

    /** Returns the specified DateTime object as a date. */
    public static Date fromDateTime(DateTime dateTime)
    {
        //create the calendar object
        Calendar calendar = Calendar.getInstance(TIMEZONE_GMT);

        //set the time components - note that we decrement the month so that it is in the
        //expected range (i.e. 0-11)
        calendar.set(Calendar.YEAR, dateTime.getYear());
        calendar.set(Calendar.MONTH, dateTime.getMonth() - 1);
        calendar.set(Calendar.DAY_OF_MONTH, dateTime.getDay());
        calendar.set(Calendar.HOUR_OF_DAY, dateTime.getHour());
        calendar.set(Calendar.MINUTE, dateTime.getMinute());
        calendar.set(Calendar.SECOND, dateTime.getSecond());

        //return the date object
        return calendar.getTime();
    }

    /** Returns the specified date represented as a ISO8601 formatted string. */
    public static String dateToString(Date date)
    {
        return toDateTime(date).toString();
    }

    /** Returns the date represented by the specified ISO8601 format string. */
    public static Date dateFromString(String date)
    {
        try
        {
            //create a DateTime object from the specified string and convert it to a real date
            DateTime dateTime = new DateTime(date);
            return fromDateTime(dateTime);
        }
        catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    /** Returns TRUE if both byte arrays contain the same data. */
    public static boolean isByteArrayEquals(byte[] array1, byte[] array2)
    {
        //neither array is present - consider it a match
        if ( (array1 == null) && (array2 == null) )
            return true;

        //one of the arrays is not present - consider it a mismatch
        if ( (array1 == null) || (array2 == null) )
            return false;

        //array lengths are different - consider it a mismatch
        if (array1.length != array2.length)
            return false;

        //compare each array element
        for (int i = 0; i < array1.length; i++)
        {
            //element values are different - consider it a mismatch
            if (array1[i] != array2[i])
                return false;
        }

        //arrays are a match
        return true;
    }


    /**
     *
     * Splits a String using the defined separator.
     *
     * @param str the String to split.
     * @param separator the separator to split the String by.
     * @return An array of String parts. If the separator was not present in the string,
     * An array of size 1 containing the original string is returned. Note this is the case
     * even with the empty String "".
     */
    public static String[] split(String str, String separator)
    {
        if(str == null)
            return new String[0];

        Vector<String> elements = new Vector<String>(0);

        int index = str.indexOf(separator);

        while(index>=0)
        {
            elements.addElement(str.substring(0, index));
            str = str.substring(index+separator.length());
            index = str.indexOf(separator);
        }

        elements.addElement(str);

        String[] result = new String[elements.size()];

        if(elements.size()>0)
        {
            for(int loop=0; loop<elements.size(); loop++)
            result[loop] = (String)elements.elementAt(loop);
        }

        return result;
    }

    public static String concatenate(String[] values, String separator)
    {
        if(values == null || values.length < 1)
            return "";

        String result = "";
        for(int i=0; i<values.length-1; i++)
            result = result + values[i] + separator;

        result = result + values[values.length-1];

        return result;
    }

    /**
     * @param bitField the bit field to check
     * @param bitValue the specific flag to check for
     *
     * @return true if the specified bit value is set in the specified bit field. Otherwise false.
     */
    public static boolean isFlagSet(int bitField, int bitValue)
    {
        return ((bitField & bitValue) == bitValue);
    }

    /**
     * Closes the supplied FileWriter.
     *
     * @param writer The writer to close.
     * @param logger The logger to use.
     */
    public static void writerClose(FileWriter writer, Logger logger)
    {
        try
        {
            //close the byte stream
            if (writer != null)
                writer.close();
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to close writer - ignoring", e);
        }

    }
    
    /**
     * 
     * @return true if a network (iDEN, GPRS, HSDPA, WIFI etc...) is available, otherwise false.
     */
    public static boolean isNetworkAvailable(Context ctx) {
        boolean networkAvailable = true;

        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();

        if (info != null) {
            return info.isAvailable();
        } else { // active network is null, try all connections manually
            networkAvailable = false;
            NetworkInfo[] infoAll = connectivityManager.getAllNetworkInfo();
            if (infoAll != null) {
                for (NetworkInfo element : infoAll) {
                    if (element.getState() == NetworkInfo.State.CONNECTED) {
                        networkAvailable = true;
                        break;
                    }
                }
            }
        }

        return networkAvailable;
    }
}

