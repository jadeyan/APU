/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util;


import java.io.IOException;
import java.io.OutputStream;


/**
 * A class representing an output stream that tracks the amount of data written to it.  
 *
 * @author  Denis Evoy
 */
public class CountingOutputStream extends OutputStream
{
    private OutputStream stream;                //the stream to track
    private int byteCount;                      //the number of bytes written to the stream

    
    /**
     * Creates a new stream on top of the specified stream.
     * 
     * @param outputStream the output stream to track. Must not be null.
     */
    public CountingOutputStream(OutputStream outputStream)
    {
        if (outputStream == null)
            throw new IllegalArgumentException("no output stream specified");
        
        stream = outputStream;
    }

    
    /**
     * Returns the number of bytes that have been written to the stream.
     * 
     * @return The number of bytes that have been written to the stream.
     */
    public int getByteCount()
    {
        return byteCount;
    }

    
    public void write(int data) 
        throws IOException
    {
        stream.write(data);
        byteCount++;
    }

    public void write(byte[] data) 
        throws IOException
    {
        write(data, 0, data.length);
    }


    public void write(byte[] data, int offset, int length) 
        throws IOException
    {
        stream.write(data, offset, length);
        byteCount += (length - offset);
    }

    public void close() 
        throws IOException
    {
        stream.close();
        byteCount = 0;
    }

    public void flush() 
        throws IOException
    {
        stream.flush();
    }
}
