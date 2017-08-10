/**
 * Copyright © 2004-2010 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test.store;


import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import net.cp.mtk.common.security.MD5;
import net.cp.syncml.client.util.*;


/**
 * A class providing common utilities for the test application. 
 *
 * @author Denis Evoy
 */
public class Utils
{
    //Setup date/time formatter (making sure to use the UTC timezone rather than relying on the local one)
    private static final TimeZone TIMEZONE_GMT =                TimeZone.getTimeZone("GMT");
    private static final SimpleDateFormat FORMAT_DATE_ISO8601 = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
    static
    { 
        FORMAT_DATE_ISO8601.setTimeZone(TIMEZONE_GMT); 
    }
    
    //Definition of the buffer size used when hashing a file - note that this buffer size MUST be a 
	//factor of HALF_MB - otherwise "MD5HashFile()" will break
    private static int ONE_MB = 1024*1024;
    private static int HALF_MB = 512*1024;
    private static final int HASHBUF_SIZE =  512;
    private static byte CONTENT_HASH_MD5 = 0x0;
    
    /** Private constructor to prevent instantiation. */
    private Utils() { super(); }


    /** 
     * Computes the MD5 hash of the specified file.
     * 
     * @param file the file to be hashed.
     * @return The MD5 hash of the specified file.
     * @throws IOException if the file couldn't be read.
     */
    
    public static byte[] md5HashFile(File file)
    	throws IOException
    {
    	FileInputStream in = null;
    	DataInputStream din = null;
	
		byte[] hashBuf = new byte[HASHBUF_SIZE];
		byte[] md5Result;
	
		long fileSize = file.length();
		
		try
		{
			in = new FileInputStream(file); 
	        
	        MD5 md5State = new MD5();
	        
	        //file is big, only hash first and last half-MB
			if (fileSize > ONE_MB)
			{
	            din = new DataInputStream(in);
	            int totalRead = 0;
	
	            //we rely on hashBuf.length being a factor of 1mb
	            while (totalRead < HALF_MB)
	            {
	            	din.readFully(hashBuf);
	            	md5State.update(hashBuf, 0, hashBuf.length);
	                totalRead += hashBuf.length;
	            }
	            
	            //skip to last half-MB
	            din.skip(fileSize - ONE_MB);
	            
	            totalRead = 0;
	            while (totalRead < HALF_MB)
	            {
	            	din.readFully(hashBuf);
	            	md5State.update(hashBuf, 0, hashBuf.length);
	                totalRead += hashBuf.length;
	            }
			}

			else //file is small, just hash the whole thing
			{
		        int rd;
		        
		        rd = in.read(hashBuf);
		        while (rd > 0)
		        {
		        	md5State.update(hashBuf, 0, rd);
		        	rd = in.read(hashBuf);
		        }
			}
		
			md5Result = md5State.doFinal();
			byte result[] = new byte[md5Result.length + 1];
	        result[0] = CONTENT_HASH_MD5;
	        System.arraycopy(md5Result, 0, result, 1, md5Result.length);
	        
	        md5Result = null;
	        
	        return result;
		}
		finally
		{
			if (din != null)
				din.close();
			if (in != null)
				in.close();
		}
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

    /**
     * Returns the specified date as a DateTime object.
     * 
     * @param date the date to convert.
     * @return A corresponding DateTime object.
     */
    public static DateTime toDateTime(long date)
    {
        return toDateTime( new Date(date) );
    }

    /**
     * Returns the specified date as a DateTime object.
     * 
     * @param date the date to convert.
     * @return A corresponding DateTime object.
     */
    public static DateTime toDateTime(Date date)
    {
        return new DateTime( FORMAT_DATE_ISO8601.format(date) );
    }

    /** Closes the specified input stream. */
    public static void streamClose(InputStream stream)
    {
        try
        {
            //close the stream
            if (stream != null)
                stream.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    
    /** Closes the specified output stream. */
    public static void streamClose(OutputStream stream)
    {
        try
        {
            //close the stream
            if (stream != null)
                stream.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    
    /** Closes the specified reader. */
    public static void readerClose(Reader reader)
    {
        try
        {
            //close the reader
            if (reader != null)
                reader.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }
    
    /** Closes the specified writer. */
    public static void writerClose(Writer writer)
    {
        try
        {
            //close the writer
            if (writer != null)
                writer.close();
        }
        catch (Throwable e)
        {
            //ignore
        }
    }

    /** Copies the specified file to the specified destination file. */
    public static void copyFile(File fromFile, File toFile)
        throws IOException
    {
        boolean success = false;
        InputStream fromStream = null;
        OutputStream toStream = null;
        try
        {
            int readCount;
            fromStream = new FileInputStream(fromFile);
            toStream = new FileOutputStream(toFile);
            byte[] buffer = new byte[10240];
            while ((readCount = fromStream.read(buffer)) != -1)
            {
                toStream.write(buffer, 0, readCount);
                toStream.flush();
            }
            
            success = true;
        }
        finally
        {
            Utils.streamClose(fromStream);
            Utils.streamClose(toStream);
            if ( (! success) && (toFile.exists()) )
                toFile.delete();
        }
    }
}
