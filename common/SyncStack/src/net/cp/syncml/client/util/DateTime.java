/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util;


/**
 * A class representing a date/time.
 *
 * @author Denis Evoy
 */
public class DateTime
{
    int dtYear;
    int dtMonth;
    int dtDay;                            
    int dtHour;
    int dtMinute;
    int dtSecond;
    boolean dtUtcTime;
    

    /**
     * Creates a date/time from the specified ISO8601 formatted string.
     * 
     * @param dateTime a ISO8601 formatted string. Must not be null or empty.
     * 
     * @see #fromString(String)
     */
    public DateTime(String dateTime)
    {
        fromString(dateTime);
    }
    
    /**
     * Create a date/time representing the specified date and time.
     * 
     * @param year      the year to set. Must be between 0 and 9999.
     * @param month     the month to set. Must be between 1 and 12.
     * @param day       the day to set. Must be between 1 and 31.
     * @param hour      the hour to set. Must be between 0 and 23.
     * @param minute    the minute to set. Must be between 0 and 59.
     * @param second    the second to set. Must be between 0 and 59.
     * @param utcTime   <code>true</code> if the time represents a UTC time or <code>false</code> if it represents local time.
     */
    public DateTime(int year, int month, int day, int hour, int minute, int second, boolean utcTime)
    {
        setYear(year);
        setMonth(month);
        setDay(day);
        setHour(hour);
        setMinute(minute);
        setSecond(second);
        setUtcTime(utcTime);
    }


    /**
     * Returns the year.
     * 
     * @return The year. Will be between 0 and 9999.
     */
    public int getYear()
    {
        return dtYear;
    }

    /**
     * Sets the year.
     * 
     * @param year the year to set. Must be between 0 and 9999.
     */
    public void setYear(int year)
    {
        if ( (year < 0) || (year > 9999) )
            throw new IllegalArgumentException("invalid year specified: " + year);
        
        dtYear = year;
    }


    /**
     * Returns the month of the year.
     * 
     * @return The month. Will be between 1 and 12.
     */
    public int getMonth()
    {
        return dtMonth;
    }

    /**
     * Sets the month of the year.
     * 
     * @param month the month to set. Must be between 1 and 12.
     */
    public void setMonth(int month)
    {
        if ( (month < 1) || (month > 12) )
            throw new IllegalArgumentException("invalid month specified: " + month);
        
        dtMonth = month;
    }


    /**
     * Returns the day of the month.
     * 
     * @return The day. Will be between 1 and 31.
     */
    public int getDay()
    {
        return dtDay;
    }

    /**
     * Sets the day of the month.
     * 
     * @param day the day to set. Must be between 1 and 31.
     */
    public void setDay(int day)
    {
        if ( (day < 1) || (day > 31) )
            throw new IllegalArgumentException("invalid day specified: " + day);
        
        dtDay = day;
    }


    /**
     * Returns the hour of the day.
     * 
     * @return The hour. Will be between 0 and 23.
     */
    public int getHour()
    {
        return dtHour;
    }

    /**
     * Sets the hour of the day.
     * 
     * @param hour the hour to set. Must be between 0 and 23.
     */
    public void setHour(int hour)
    {
        if ( (hour < 0) || (hour > 23) )
            throw new IllegalArgumentException("invalid hour specified: " + hour);
        
        dtHour = hour;
    }


    /**
     * Returns the minute of the hour.
     * 
     * @return The minute. Will be between 0 and 59.
     */
    public int getMinute()
    {
        return dtMinute;
    }

    /**
     * Sets the minute of the hour.
     * 
     * @param minute the minute to set. Must be between 0 and 59.
     */
    public void setMinute(int minute)
    {
        if ( (minute < 0) || (minute > 59) )
            throw new IllegalArgumentException("invalid minute specified: " + minute);
        
        dtMinute = minute;
    }


    /**
     * Returns the second of the minute.
     * 
     * @return The second. Will be between 0 and 59.
     */
    public int getSecond()
    {
        return dtSecond;
    }

    /**
     * Sets the second of the minute.
     * 
     * @param second the second to set. Must be between 0 and 59.
     */
    public void setSecond(int second)
    {
        if ( (second < 0) || (second > 59) )
            throw new IllegalArgumentException("invalid second specified: " + second);
        
        dtSecond = second;
    }


    /**
     * Returns whether or not the time represents a UTC time.
     * 
     * @return <code>true</code> if the time represents a UTC time or <code>false</code> if it represents local time.
     */
    public boolean isUtcTime()
    {
        return dtUtcTime;
    }

    /**
     * Sets whether or not the time represents a UTC time.
     * 
     * @param utcTime <code>true</code> if the time represents a UTC time or <code>false</code> if it represents local time.
     */
    public void setUtcTime(boolean utcTime)
    {
        dtUtcTime = utcTime;
    }


    /**
     * Sets the date/time from the specified ISO8601 formatted string. <br/><br/>
     * 
     * The specified string must be in the following format:
     * <pre>
     * 	       YYYYMMDD[Thhmmss[Z]]
     * </pre>
     *  
     * @param dateTime a ISO8601 formatted string. Must not be null or empty.
     */
    public void fromString(String dateTime)
    {
        if ( (dateTime == null) || ((dateTime.length() != 8) && (dateTime.length() != 15) && (dateTime.length() != 16)) )
            throw new IllegalArgumentException("invalid date/time specified: " + dateTime);
            
        if (dateTime.length() >= 8)
        {
            setYear( Integer.parseInt(dateTime.substring(0, 4)) );
            setMonth( Integer.parseInt(dateTime.substring(4, 6)) );
            setDay( Integer.parseInt(dateTime.substring(6, 8)) );
        }
        
        if (dateTime.length() >= 15)
        {
            setHour( Integer.parseInt(dateTime.substring(9, 11)) );
            setMinute( Integer.parseInt(dateTime.substring(11, 13)) );
            setSecond( Integer.parseInt(dateTime.substring(13, 15)) );
            
            if ((dateTime.length() == 16) && (dateTime.charAt(15) == 'Z'))
                setUtcTime(true);
            else
                setUtcTime(false);
        }
    }
    
    /**
     * Returns the date/time in ISO8601 format. <br/><br/>
     * 
     * The returned string will be in the following format:
     * <pre>
     *         YYYYMMDDThhmmss[Z]
     * </pre>
     * 
     * @return A ISO8601 formatted string. Will not be null or empty.
     */
    public String toString()
    {
        StringBuffer result = new StringBuffer();
        
        result.append(dtYear);
        
        if (dtMonth < 10)
            result.append("0");
        result.append(dtMonth);
            
        if (dtDay < 10)
            result.append("0");
        result.append(dtDay);

        result.append("T");

        if (dtHour < 10)
            result.append("0");
        result.append(dtHour);

        if (dtMinute < 10)
            result.append("0");
        result.append(dtMinute);

        if (dtSecond < 10)
            result.append("0");
        result.append(dtSecond);
        
        if (dtUtcTime)
            result.append("Z");
        
        return result.toString();
    }
}
