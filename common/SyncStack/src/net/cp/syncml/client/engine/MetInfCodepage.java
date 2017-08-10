/**
 * Copyright © 2004-2010 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


import java.io.*;

import net.cp.mtk.common.CommonUtils;
import net.cp.syncml.client.util.*;
import net.cp.syncml.client.util.wbxml.*;


/**
 * A class defining a WBXML codepage for SyncML MetaInf.
 * 
 * This codepage conforms with SyncML MetaInf version 1.2.
 *
 * @author Denis Evoy
 */
public class MetInfCodepage extends Codepage
{
    //The index of the codepage
    public static final int PAGE_INDEX =                1;
    

    //MetaInv v1.2 DTD public ID (-//SYNCML//DTD SyncML 1.2//EN)
    //private static final int PUBLIC_ID =               0x1202;

    //MetInf v1.2 codepage definition
    private static final byte TAG_ANCHOR =             0x05;
    private static final byte TAG_EMI =                0x06;
    private static final byte TAG_FORMAT =             0x07;
    private static final byte TAG_FREE_ID =            0x08;
    private static final byte TAG_FREE_MEM =           0x09;
    private static final byte TAG_LAST =               0x0A;
    private static final byte TAG_MARK =               0x0B;
    private static final byte TAG_MAX_MSG_SIZE =       0x0C;
    private static final byte TAG_MEM =                0x0D;
    //private static final byte TAG_METINF =             0x0E;
    private static final byte TAG_NEXT =               0x0F;
    private static final byte TAG_NEXT_NONCE =         0x10;
    private static final byte TAG_SHARED_MEM =         0x11;
    private static final byte TAG_SIZE =               0x12;
    private static final byte TAG_TYPE =               0x13;
    private static final byte TAG_VERSION =            0x14;
    private static final byte TAG_MAX_OBJECT_SIZE =    0x15;
    private static final byte TAG_FIELD_LEVEL =        0x16;

    private static final String[] TAG_NAMES =          { "Anchor", "EMI", "Format", "FreeID", "FreeMem", "Last", "Mark", "MaxMsgSize", "Mem", "MetInf", "Next", "NextNonce", "SharedMem", "Size", "Type", "Version", "MaxObjSize", "FieldLevel" };
    
    //The encoding to use when handling strings
    private static final String ENCODING_UTF8 =        "UTF-8";

    
    private Metinf inMetinf;                        //the meta info object being parsed
    private ByteArrayOutputStream inDataStream;     //the opaque data being parsed
   
    
    /**
     * Creates a new SyncML codepage for use in the specified session.
     * 
     * @param logger  the logger used to log activity. 
     * @param session the session in which the codepage will be used.
     */
    public MetInfCodepage(Logger logger, Session session)
    {
        super(logger);
        
        inMetinf = new Metinf();
        inDataStream = new ByteArrayOutputStream(256);            
    }
    
    /** 
     * Returns the Meta info that has been parsed.
     * 
     * @return the Meta info that has been parsed.
     */
    public Metinf getMetinf()
    {
        return inMetinf;
    }
    
    /** Clears the Meta info that has been parsed so far. */
    public void clear()
    {
        inMetinf = new Metinf();
    }

    
    public String[] getTagNames()
    {
        return TAG_NAMES;
    }
    
    public void onTagStart(int tagId, boolean hasContent) 
        throws WbxmlException
    {
        super.onTagStart(tagId, hasContent);
        
        if (tagId == TAG_SHARED_MEM)
        {
            //set shared memory
            inMetinf.sharedMem = true;
        }
        else if (tagId == TAG_FIELD_LEVEL)
        {
            //set field level replace
            inMetinf.fieldLevelReplace = true;
        }
    }    
    
    public void onStringData(int tagId, String data) 
        throws WbxmlException
    {
        super.onStringData(tagId, data);
        
        if (tagId == TAG_EMI)
        {
            //add EMI extension
            inMetinf.emiExtensions.addElement(data);
        }
        else if (tagId == TAG_FORMAT)
        {
            //set encoding
            inMetinf.encoding = data;
        }
        else if (tagId == TAG_FREE_ID)
        {
            //set number of free IDs
            inMetinf.freeId = parseLong(data, "FreeId");
        }
        else if (tagId == TAG_FREE_MEM)
        {
            //set amount of free memory
            inMetinf.freeMem = parseLong(data, "FreeMem");
        }
        else if (tagId == TAG_LAST)
        {
            //set last anchor
            inMetinf.lastAnchor = data;
        }
        else if (tagId == TAG_MARK)
        {
            //set mark as
            inMetinf.markAs = data;
        }
        else if (tagId == TAG_MAX_MSG_SIZE)
        {
            //set max message size
            inMetinf.maxMsgSize = parseInt(data, "MaxMsgSize");
        }
        else if (tagId == TAG_NEXT)
        {
            //set next anchor
            inMetinf.nextAnchor = data;
        }
        else if (tagId == TAG_NEXT_NONCE)
        {
            try
            {
                //set the next nonce that should be used during our next MD5 authentication request 
                inMetinf.nextNonce = new String(CommonUtils.base64Decode(data), ENCODING_UTF8);
            }
            catch (IOException e)
            {
                throw new WbxmlException("failed to UTB-8 decode the next nonce received from server: " + data, e);
            }
        }
        else if (tagId == TAG_SIZE)
        {
            //indicates the size of an object
            inMetinf.size = parseLong(data, "Size");
        }
        else if (tagId == TAG_TYPE)
        {
            //set content type
            inMetinf.contentType = data;
        }
        else if (tagId == TAG_VERSION)
        {
            //set version
            inMetinf.version = data;
        }
        else if (tagId == TAG_MAX_OBJECT_SIZE)
        {
            //set max object size
            inMetinf.maxObjSize = parseInt(data, "MaxObjSize");
        }
    }    
    
    public void onOpaqueDataBegin(int tagId, long length) 
        throws WbxmlException
    {
        super.onOpaqueDataBegin(tagId, length);

        //reset the stream used to store the data of the item
        inDataStream.reset();            
    }
    
    public void onOpaqueData(int tagId, byte[] data, int length) 
        throws WbxmlException
    {
        super.onOpaqueData(tagId, data, length);

        //store the data of the item
        inDataStream.write(data, 0, length);
    }

    public void onOpaqueDataEnd(int tagId, boolean commit) 
        throws WbxmlException
    {
        super.onOpaqueDataEnd(tagId, commit);

        if (commit)
        {
            //this is a fix for the case where the CP SyncML server incorrectly sends "Type" and "Format" 
            //as opaque data when sending a "basic" authentication challenge 
            if (tagId == TAG_TYPE)
            {
                //set content type
                inMetinf.contentType = parseString(inDataStream.toByteArray(), "Type");
            }
            else if (tagId == TAG_FORMAT)
            {
                //set encoding
                inMetinf.encoding = parseString(inDataStream.toByteArray(), "Format");
            }
        }
    }
    
    
    /**
     * Writes the specified Meta information in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the folder information to.
     * @param metainfo      the meta information to write.
     * @throws IOException      if the meta information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeMetinf(OutputStream outputStream, Metinf metainfo)
        throws WbxmlException, IOException
    {
        if ( ((metainfo.lastAnchor != null) && (metainfo.lastAnchor.length() > 0)) || 
             ((metainfo.nextAnchor != null) && (metainfo.nextAnchor.length() > 0)) )
        {
            writeTag(outputStream, TAG_ANCHOR, true);
            
            if ( (metainfo.lastAnchor != null) && (metainfo.lastAnchor.length() > 0) )
                writeTag(outputStream, TAG_LAST, metainfo.lastAnchor);
        
            if ( (metainfo.nextAnchor != null) && (metainfo.nextAnchor.length() > 0) )
                writeTag(outputStream, TAG_NEXT, metainfo.nextAnchor);

            writeTagEnd(outputStream, TAG_ANCHOR);
        }

        if ( (metainfo.freeMem >= 0) || (metainfo.freeId >= 0) )
        {
            writeTag(outputStream, TAG_MEM, true);
            
            if (metainfo.sharedMem)
                writeTag(outputStream, TAG_SHARED_MEM, false);
            
            if (metainfo.freeMem >= 0)
                writeTag(outputStream, TAG_FREE_MEM, Long.toString(metainfo.freeMem));
            
            if (metainfo.freeId >= 0)
                writeTag(outputStream, TAG_FREE_ID, Long.toString(metainfo.freeId));

            writeTagEnd(outputStream, TAG_MEM);
        }

        if (metainfo.fieldLevelReplace)
            writeTag(outputStream, TAG_FIELD_LEVEL, false);

        if ( (metainfo.encoding != null) && (metainfo.encoding.length() > 0) )
            writeTag(outputStream, TAG_FORMAT, metainfo.encoding);
        
        if ( (metainfo.contentType != null) && (metainfo.contentType.length() > 0) )
            writeTag(outputStream, TAG_TYPE, metainfo.contentType);
        
        if ( (metainfo.markAs != null) && (metainfo.markAs.length() > 0) )
            writeTag(outputStream, TAG_MARK, metainfo.markAs);
        
        if ( (metainfo.nextNonce != null) && (metainfo.nextNonce.length() > 0) )
            writeTag(outputStream, TAG_NEXT_NONCE, CommonUtils.base64Encode(metainfo.nextNonce.getBytes(ENCODING_UTF8)));
        
        if ( (metainfo.version != null) && (metainfo.version.length() > 0) )
            writeTag(outputStream, TAG_VERSION, metainfo.version);

        if (metainfo.size >= 0)
            writeTag(outputStream, TAG_SIZE, Long.toString(metainfo.size));

        if (metainfo.maxMsgSize >= 0)
            writeTag(outputStream, TAG_MAX_MSG_SIZE, Long.toString(metainfo.maxMsgSize));

        if (metainfo.maxObjSize >= 0)
            writeTag(outputStream, TAG_MAX_OBJECT_SIZE, Long.toString(metainfo.maxObjSize));
        
        for (int i = 0; i < metainfo.emiExtensions.size(); i++)
            writeTag(outputStream, TAG_EMI, (String)metainfo.emiExtensions.elementAt(i));
    }

    
    /* Parses the specified data as a Long. */
    private long parseLong(String data, String name)
        throws WbxmlException
    {
        try
        {
            return Long.parseLong(data);
        }
        catch (NumberFormatException e)
        {
            throw new WbxmlException("invalid '" + name + "' value received from server: " + data, e);
        }
    }
    
    /* Parses the specified data as an Integer. */
    private int parseInt(String data, String name)
        throws WbxmlException
    {
        try
        {
            return Integer.parseInt(data);
        }
        catch (NumberFormatException e)
        {
            throw new WbxmlException("invalid '" + name + "' value received from server: " + data, e);
        }
    }
    
    /* Parses the specified binary data as a String. */
    private String parseString(byte[] data, String name)
        throws WbxmlException
    {
        try
        {
            return new String(data, ENCODING_UTF8);
        }
        catch (UnsupportedEncodingException e)
        {
            throw new WbxmlException("invalid '" + name + "' value received from server - couldn't convert to a " + ENCODING_UTF8 + " string", e);
        }
    }
}
