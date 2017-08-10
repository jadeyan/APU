/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.common;


import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;


public abstract class StringUtils
{
    /** Indicates that a date should be formatted as "DD/MM/YYYY". */
    public static final int DATE_FORMAT_DMY = 1;
    
    /** Indicates that a date should be formatted as "MM/DD/YYYY". */
    public static final int DATE_FORMAT_MDY = 2;

    /** Indicates that a date should be formatted as "YYYY/MM/DD". */
    public static final int DATE_FORMAT_YMD = 3;


    private StringUtils()
    {
        super();
    }
    

    /**
     * Returns TRUE if both specified strings are the same.
     * 
     * @param string1       the first string to compare. May be null or empty.
     * @param string2       the second string to compare. May be null or empty.
     * @param ignoreCase    TRUE if case should be ignored when comparing the strings, FALSE otherwise.
     * @return TRUE if the specified strings are the same.
     */
    public static boolean isEquals(String string1, String string2, boolean ignoreCase)
    {
        //normalise so that 'null' is treated the same as an empty string
        if (string1 == null)
            string1 = "";
        if (string2 == null)
            string2 = "";
        
        //compare the values
        return (ignoreCase) ? string1.equalsIgnoreCase(string2) : string1.equals(string2);
    }

    /**
     * Returns TRUE if both specified strings are exactly the same.
     * 
     * @param string1       the first string to compare. May be null or empty.
     * @param string2       the second string to compare. May be null or empty.
     * @return TRUE if the specified strings are exactly the same.
     */
    public static boolean isEquals(String string1, String string2)
    {
        return isEquals(string1, string2, false);
    }


    /**
     * Returns TRUE if both specified string arrays are the same.
     * 
     * @param array1        the first array to compare. May be null or empty.
     * @param array2        the second array2 to compare. May be null or empty.
     * @param ignoreCase    TRUE if case should be ignored when comparing the strings, FALSE otherwise.
     * @return TRUE if the specified arrays are the same.
     */
    public static boolean isEquals(String[] array1, String[] array2, boolean ignoreCase)
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
            if (! isEquals(array1[i], array2[i], ignoreCase))
                return false;
        }
        
        //arrays are a match
        return true;
    }
    
    /**
     * Returns TRUE if both specified string arrays are exactly the same.
     * 
     * @param array1   the first array to compare. May be null or empty.
     * @param array2   the second array2 to compare. May be null or empty.
     * @return TRUE if the specified arrays are exactly the same.
     */
    public static boolean isEquals(String[] array1, String[] array2)
    {
        return isEquals(array1, array2, false);
    }
    
    
    /**
     * Returns the index of the specified string in the specified array.
     * 
     * @param array         the array to search. May be null or empty.
     * @param string        the string to look for. May be null or empty.
     * @param ignoreCase    TRUE if case should be ignored when comparing the strings, FALSE otherwise.
     * @return the index of the specified string in the specified array, or -1 if the string isn't present in the array.
     */
    public static int indexOf(String[] array, String string, boolean ignoreCase)
    {
        if (array == null)
            return -1;

        for (int i = 0; i < array.length; i++)
        {
            if (isEquals(array[i], string))
                return i;
        }
        
        return -1;
    }
    
    /**
     * Splits the specified string into multiple sub-strings at the specified separator characters.
     * 
     * @param string    the string to split. May be null or empty.
     * @param separator the separator character where the string should be split.
     * @return an array of strings. May be empty.
     */
    public static String[] splitString(String string, char separator)
    {
        //split the string at the specified separator character
        int index = -1;
        int start = 0;
        Vector strings = new Vector();
        if ( (string != null) && (string.length() > 0) )
        {
            int stringLength = string.length();
            while (start < stringLength)
            {
                index = string.indexOf(separator, start);
                if (index >= 0)
                {
                    strings.addElement(string.substring(start, index));
                    start = index + 1;
                }
                else
                {
                    strings.addElement(string.substring(start));
                    break;
                }
            }
        }
             
        //convert to an array of strings
        int stringCount = strings.size();
        String[] result = new String[stringCount];
        if (stringCount > 0)
        {
            for (int i = 0; i < stringCount; i++)
                result[i] = (String)strings.elementAt(i);
        }
    
        return result;
    }
    

    /**
     * Concatenates the specified array of strings into a single string.
     * 
     * @param strings   an array of strings to concatenate. May be null or empty.
     * @param offset    the index of the first string to concatenate.
     * @param length    the number of strings to concatenate.
     * @param separator the separator string to place between each concatenated string. May be null or empty.
     * @return a single string made of of the specified strings.
     */
    public static String concatStrings(String[] strings, int offset, int length, String separator)
    {
        if ( (strings == null) || (strings.length <= 0) || (offset < 0) || (length < 0) || (offset >= strings.length) || ((offset + length) > strings.length) )
            return "";
        
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++)
        {
            if ( (separator != null) && (i > 0) )
                buffer.append(separator);
            buffer.append( strings[offset+i] );
        }

        return buffer.toString();
    }

    /**
     * Concatenates the specified array of strings into a single string.
     * 
     * @param strings   an array of strings to concatenate. May be null or empty.
     * @param separator the separator string to place between each concatenated string. May be null or empty.
     * @return a single string made of of the specified strings.
     */
    public static String concatStrings(String[] strings, String separator)
    {
        return concatStrings(strings, 0, (strings != null) ? strings.length : 0, separator);
    }

    
    /**
     * Replaces occurrences of the specified old string in the specified string with the specified new string. 
     * 
     * @param string        the string to do the replace on. May be null or empty.
     * @param oldString     the string to be replaced. May be null or empty.
     * @param newString     the replacement string. May be null or empty.
     * @param replaceAll    TRUE to replace all occurrences of the specified old string, or FALSE to only replace the first occurrence.
     * @return the string with the replace operation completed on it.
     */
    public static String replaceString(String string, String oldString, String newString, boolean replaceAll)
    {
        if ( (string == null) || (string.length() <= 0) || (oldString == null) || (oldString.length() <= 0) )
            return string;

        int index = -1; 
        StringBuffer buffer = new StringBuffer( string.length() );
        while ((index = string.indexOf(oldString)) > -1)
        {
            buffer.append( string.substring(0, index) );
            if (newString != null)
                buffer.append(newString);

            string = string.substring(index + oldString.length());
            if (! replaceAll)
                break;
        }
        buffer.append(string);
        
        return buffer.toString();
    }
    
    /**
     * Replaces the specified old strings with the specified new strings. 
     * 
     * @param data       the string to do the replace on.
     * @param oldStrings the strings to be replaced.
     * @param newStrings the replacement strings.
     * @return the string with the replace operation completed on it.
     */
    /**
     * Replaces all occurrences of the specified old strings in the specified string with the specified new strings. 
     * 
     * This method will replace all occurrences of <code>oldStrings[N]</code> with <code>newStrings[N]</code> for each
     * old string N.
     * 
     * @param string        the string to do the replace on. May be null or empty.
     * @param oldStrings    the strings to be replaced. May be null or empty.
     * @param newStrings    the replacement strings. May be null or empty.
     * @param replaceAll    TRUE to replace all occurrences of each old string, or FALSE to only replace the first occurrence.
     * @return the string with the replace operation completed on it.
     * @throws IllegalArgumentException if the number of old and new strings are different.
     */
    public static String replaceStrings(String string, String[] oldStrings, String[] newStrings, boolean replaceAll)
    {
        if ( (string == null) || (string.length() <= 0) || (oldStrings == null) || (newStrings == null) )
            return string;
        
        if (oldStrings.length != newStrings.length)
            throw new IllegalArgumentException("Incorrect number of replacement strings");
        
        for (int i = 0; i < oldStrings.length; i++)
            string = replaceString(string, oldStrings[i], newStrings[i], replaceAll);
        
        return string;
    }

    /**
     * Formats the specified string by substituting any positional tokens by their associated values.
     * 
     * Positional tokens in the specified string are denoted by the specified token string (e.g. "%s"). The first token 
     * will be replaced by the value at <code>values[0]</code>, the second token will be replaced by the value at 
     * <code>values[1]</code>, etc. 
     *  
     * @param string    the string containing the positional tokens to be replaced. May be null or empty.
     * @param values    the replacement values. May be null or empty.
     * @param token     the token in the string to be replaced.
     * @return the formatted string.
     */
    public static String formatString(String string, Object[] values, String token)
    {
        if ( (string == null) || (string.length() <= 0) || (values == null) || (values.length <= 0) )
            return string;

        for (int i = 0; i < values.length; i++)
        {
            Object value = values[i];
            string = replaceString(string, token, (value != null) ? value.toString() : "", false);
        }

        return string;
    }

    /**
     * Formats the specified string by substituting any named tokens by their associated values.
     * 
     * The string representation of each key in the <code>tokens</code> hashtable is considered to be a token. The 
     * associated value of the key will be used to replace occurrences of the key in the specified string. 
     *  
     * @param string    the string containing the named tokens to be replaced. May be null or empty.
     * @param tokens    the replacement values. May be null or empty.
     * @return the formatted string.
     */
    public static String formatString(String string, Hashtable tokens)
    {
        if ( (string == null) || (string.length() <= 0) || (tokens == null) || (tokens.size() <= 0) )
            return string;

        for (Enumeration keysEnum = tokens.keys(); keysEnum.hasMoreElements(); )
        {
            String key = keysEnum.nextElement().toString();
            Object value = tokens.get(key);
            string = replaceString(string, key, (value != null) ? value.toString() : "", true);
        }

        return string;
    }
    
    
    /**
     * Surround each line in the specified string with the specified prefix and/or postfix.
     * 
     * Each line, delimited by the '\n' character, will have the specified prefix added to the start of the line and 
     * the specified postfix added to the end of the line.
     * 
     * @param lines     a multi-line string. May be null or empty.
     * @param prefix    the prefix to prepend to each line. May be null or empty.
     * @param postfix   the postfix to append to each line. May be null or empty.
     * @return a multi-line string with the specified prefix and postfix added to each line.
     */
    public static String surroundLines(String lines, String prefix, String postfix)
    {
        //no lines specified
        if ( (lines == null) || (lines.length() <= 0) )
            return lines;

        //no prefix or postfix specified
        if ( ((prefix == null) || (prefix.length() <= 0)) && ((postfix == null) || (postfix.length() <= 0)) )
            return lines;
        
        //split into individual lines and add the prefix and postfix to each one
        StringBuffer buffer = new StringBuffer( lines.length() );
        for (int from = 0; ;)
        {
            int index = lines.indexOf('\n', from);
            if (index < 0)
                break;
            
            String line = lines.substring(from, index);
            if (prefix != null)
                buffer.append(prefix);
            buffer.append(line);
            if (postfix != null)
                buffer.append(postfix);
            buffer.append('\n');
            
            from = index + 1;
        }
        
        return buffer.toString();
    }

    /**
     * Prefix each line in the specified string with the specified prefix.
     * 
     * Each line, delimited by the '\n' character, will have the specified prefix added to the start of the line.
     * 
     * @param lines     a multi-line string. May be null or empty.
     * @param prefix    the prefix to prepend to each line. May be null or empty.
     * @return a multi-line string with the specified prefix and postfix added to each line.
     */
    public static String prefixLines(String lines, String prefix)
    {
        return surroundLines(lines, prefix, null);
    }
    
    /**
     * Postfix each line in the specified string with the specified postfix.
     * 
     * Each line, delimited by the '\n' character, will have the specified postfix added to the end of the line.
     * 
     * @param lines     a multi-line string. May be null or empty.
     * @param postfix   the postfix to append to each line. May be null or empty.
     * @return a multi-line string with the specified prefix and postfix added to each line.
     */
    public static String postfixLines(String lines, String postfix)
    {
        return surroundLines(lines, null, postfix);
    }
    
    
    /**
     * Aligns the specified string in a column of the specified width, truncating or padding the string as necessary.
     * 
     * This method is usually used to format a string so that it can be displayed in a column (making the output easier 
     * to read).
     * 
     * If the specified string is longer than the specified width, it is truncated so that it doesn't exceed the 
     * specified width. An optional indicator will be added (e.g. "...") to indicate that it was truncated.
     * 
     * If the specified string is shorter than the specified width, the specified padding characters will be added to 
     * the string so that it is the specified exact width. If the 'alignLeft' parameter is TRUE, the padding characters 
     * are added after the string. If the 'alignLeft' parameter is FALSE, the padding characters are added before the 
     * string. 
     * 
     * @param string                the string which should be aligned. May be null or empty.
     * @param width                 the maximum width allowed for the string. Must be > 0.
     * @param alignLeft             TRUE if the string should be aligned to the left, or FALSE if it should be aligned to the right.
     * @param padCharacter          the padding character to use when aligning the string.
     * @param truncatedIndicator    the indicator to add to the end of the string to indicate that it was truncated. May be null or empty.
     * @return the string which has been reformatted with the specified alignment. 
     */
    public static String alignString(String string, int width, boolean alignLeft, char padCharacter, String truncatedIndicator)
    {
        if ( (string == null) || (string.length() <= 0) )
            return string;
            
        StringBuffer buffer = new StringBuffer(width);
        if (string.length() > width)
        {
            //the string is too large to fit - truncate it so that it doesn't exceed the specified width
            if (truncatedIndicator != null)
            {
                buffer.append( string.substring(0, width-truncatedIndicator.length()) );
                buffer.append(truncatedIndicator);
            }
            else
            {
                return string.substring(0, width);
            }
        }
        else
        {
            //the string is too small - use the specified padding characters to make it the specified width
            int padSize = width - string.length();
            if (! alignLeft)
                buffer.append(string);
            for (int i = 0; i < padSize; i++)
                buffer.append(padCharacter);
            if (alignLeft)
                buffer.append(string);
        }

        return buffer.toString();
    }
    
    /**
     * Aligns the specified string in a column of the specified width, truncating or padding the string as necessary.
     * 
     * This method is usually used to format a string so that it can be displayed in a column (making the output easier 
     * to read).
     * 
     * If the specified string is longer than the specified width, it is truncated and the string "..." added to indicate 
     * that it was truncated. 
     * 
     * If the specified string is shorter than the specified width, empty space will be added to the string so that it 
     * is the specified exact width. If the 'alignLeft' parameter is TRUE, the empty space is added after the string. 
     * If the 'alignLeft' parameter is FALSE, the empty space is added before the string. 
     * 
     * @param string    the string which should be aligned. May be null or empty.
     * @param width     the maximum width allowed for the string. Must be > 0.
     * @param alignLeft TRUE if the string should be aligned to the left, or FALSE if it should be aligned to the right.
     * @return the string which has been reformatted with the specified alignment. 
     */
    public static String alignString(String string, int width, boolean alignLeft)
    {
        return alignString(string, width, alignLeft, ' ', "...");
    }


    /**
     * Encodes the specified string so that it can be safely used to construct a URL.
     * 
     * Any unsafe characters in the specified string will be encoded as one or more <code>%HH</code> encoded sequences,
     * where <code>HH</code> are two hex characters. If any character in the specified string is not a US-ASCII character, 
     * it will be converted to a two or three byte UTF-8 sequence and encoded as described (e.g. as either <code>%HH%HH</code>
     * or <code>%HH%HH%HH</code>). 
     * 
     * @param string the string to be encoded. May be null or empty.
     * @returns the URL encoded string. Will not be null.
     */
    public static String encodeURL(String string)
    {
        if (string == null)
            return "";
        
        int length = string.length();
        if (string.length() <= 0)
            return "";
        
        StringBuffer buffer = new StringBuffer(length);
        for (int i = 0; i < length; i++)
        {
            char c = string.charAt(i);
            if ( ((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || ((c >= '0') && (c <= '9')) || 
                 (c == '-') || (c == '_') || (c == '.') || (c == '*') )
            {
                //unreserved characters - don't have to be encoded
                buffer.append(c);
            } 
            else if (c <= 0x007F) 
            {
                //reserved US-ASCII characters - encode as %HH
                buffer.append('%');
                CommonUtils.hexEncode((byte)c, buffer);
            } 
            else if (c <= 0x07FF) 
            {
                //non US-ASCII characters - encode as %HH%HH (i.e two UTF-8 bytes)
                buffer.append('%');
                CommonUtils.hexEncode((byte)(0xC0 | (c >> 6)), buffer);
                buffer.append('%');
                CommonUtils.hexEncode((byte)(0x80 | (c & 0x3F)), buffer);
            } 
            else 
            {
                //non US-ASCII characters - encode as %HH%HH%HH (i.e three UTF-8 bytes)
                buffer.append('%');
                CommonUtils.hexEncode((byte)(0xE0 | (c >> 12)), buffer);
                buffer.append('%');
                CommonUtils.hexEncode((byte)(0x80 | ((c >> 6) & 0x3F)), buffer);
                buffer.append('%');
                CommonUtils.hexEncode((byte)(0x80 | (c & 0x3F)), buffer);
            }
        }
        
        return buffer.toString();
    }


    /**
     * Returns the string representation of the specified data size.
     * 
     * Some example return values:
     * <pre>
     *      100B
     *      50KB
     *      5.6MB
     *      3.2GB
     * </pre>
     * 
     * @param dataSize          the data size in bytes. Must be >= 0.
     * @param includeFraction   TRUE if fractional units should be included in the returned string (e.g. "5.6MB"), or false if fractional units should be ignored (e.g. "5Mb").
     * @return the string representation of the data size.
     */
    public static String dataSizeToString(long dataSize, boolean includeFraction)
    {
        if (dataSize < 0)
            return null;

        long sizeMultiplied = 0;
        String units = null;
        if (dataSize < 1024)
        {
            return Long.toString(dataSize) + "B";
        }
        else if (dataSize < (1024 * 1024))
        {
            sizeMultiplied = (dataSize * 10) / 1024;
            units = "KB";
        }
        else if (dataSize < (1024 * 1024 * 1024))
        {
            sizeMultiplied = (dataSize * 10) / (1024 * 1024);
            units = "MB";
        }      
        else
        {
            sizeMultiplied = (dataSize * 10) / (1024 * 1024 * 1024);
            units = "GB";
        }
        
        String sizeString = "" + sizeMultiplied / 10;
        if (includeFraction)
        {
            //add decimal place if necessary
            short decimal = (short)(sizeMultiplied % 10);
            if (decimal != 0)
                sizeString = sizeString + "." + decimal;
        }
        
        return sizeString + units;
    }
    
    /**
     * Returns the string representation of the time defined by the specified calendar.
     * 
     * @param calendar       the time to be converted to a string. Must not be null.
     * @param use24hour      TRUE if the returned time should be in the 24h clock. FALSE implies that the returned string will contain either an "AM" or "PM" indicator.
     * @param includeSeconds TRUE if the seconds component of the time should be included in the returned string.
     * @return the string representation of the time.
     */
    public static String timeToString(Calendar calendar, boolean use24hour, boolean includeSeconds)
    {
        StringBuffer buffer = new StringBuffer();
        int hour = calendar.get( (use24hour) ? Calendar.HOUR_OF_DAY : Calendar.HOUR);
        if (hour < 10)
            buffer.append("0");
        buffer.append(hour);
        buffer.append(":");
        int min = calendar.get(Calendar.MINUTE);
        if (min < 10)
            buffer.append("0");
        buffer.append(min);
        if (includeSeconds)
        {
            buffer.append(":");
            int second = calendar.get(Calendar.SECOND);
            if (second < 10)
                buffer.append("0");
            buffer.append(second);
        }
        
        if (! use24hour)
        {
            int am_pm = calendar.get(Calendar.AM_PM);
            buffer.append( (am_pm == Calendar.AM) ? " AM" : " PM");
        }
        
        return buffer.toString();
    }
    
    /**
     * Returns the string representation of the date defined by the specified calendar.
     * 
     * @param calendar  the date to be converted to a string. Must not be null.
     * @param format    one of the common defined date formats (DATE_FORMAT_XXX).
     * @param separator the separator string used to separate components of the date (e.g. "/" or "-"). Must not be null.
     * @return the string representation of the date.
     */
    public static String dateToString(Calendar calendar, int format, String separator)
    {
        StringBuffer buffer = new StringBuffer();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (format == DATE_FORMAT_DMY)
        {
            if (day < 10)
                buffer.append("0");
            buffer.append(day);
            buffer.append(separator);
            if (month < 10)
                buffer.append("0");
            buffer.append(month);
            buffer.append(separator);
            buffer.append(year);
        }
        else if (format == DATE_FORMAT_MDY)
        {
            if (month < 10)
                buffer.append("0");
            buffer.append(month);
            buffer.append(separator);
            if (day < 10)
                buffer.append("0");
            buffer.append(day);
            buffer.append(separator);
            buffer.append(year);
        }
        else if (format == DATE_FORMAT_YMD)
        {
            buffer.append(year);
            buffer.append(separator);
            if (month < 10)
                buffer.append("0");
            buffer.append(month);
            buffer.append(separator);
            if (day < 10)
                buffer.append("0");
            buffer.append(day);
        }
        
        return buffer.toString();
    }
}
