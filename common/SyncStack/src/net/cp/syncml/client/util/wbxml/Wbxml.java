/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.wbxml;


import java.io.*;
import java.util.Stack;


/**
 * A class providing various WBXML encoding/decoding functionality.
 *
 * @author Denis Evoy
 */
public class Wbxml
{
    /** The definition of WBXML protocol version 1.1. */
    public static final int VERSION_1_1 =           (byte)0x01;

    /** The definition of WBXML protocol version 1.2. */
    public static final int VERSION_1_2 =           (byte)0x02;

    /** The definition of WBXML protocol version 1.3. */
    public static final int VERSION_1_3 =           (byte)0x03;
    
    
    /** The definition of the UTF-8 character set as defined by the IANA. */
    public static final int CHARSET_UTF8 =          106;
    
    
    /** A token indicating that the code page should be changed for all following tokens. The following byte indicates the new code page number. */
    public static final byte TOKEN_SWITCH_PAGE =    (byte)0x00;

    /** A token indicating the end of an element or attribute list. */
    public static final byte TOKEN_END =            (byte)0x01;

    /** A token indicating a character entity ("&#32;" for example). The following integer indicates the character entity number. */
    public static final byte TOKEN_CHAR_ENTITY =    (byte)0x02;

    /** A token indicating an in-line string.  */
    public static final byte TOKEN_INLINE_STRING =  (byte)0x03;

    /** A token indicating an unknown element with no attributes or content. The following integer indicates the offset into the string table. */
    public static final byte TOKEN_LITERAL =        (byte)0x04;

    /** A token indicating a string document specific extension token. This token is followed by string data with the appropriate (character set dependent) termination. */
    public static final byte TOKEN_EXT_STRING_0 =   (byte)0x40;

    /** A token indicating a string document specific extension token. This token is followed by string data with the appropriate (character set dependent) termination. */
    public static final byte TOKEN_EXT_STRING_1 =   (byte)0x41;

    /** A token indicating a string document specific extension token. This token is followed by string data with the appropriate (character set dependent) termination. */
    public static final byte TOKEN_EXT_STRING_2 =   (byte)0x42;

    /** A token indicating a processing instruction. This token is followed by an attribute list. */
    public static final byte TOKEN_PI =             (byte)0x43;

    /** A token indicating an unknown element with content but no attributes. The following integer indicates the offset into the string table. */
    public static final byte TOKEN_LITERAL_C =      (byte)0x44;

    /** A token indicating an integer document specific extension token. This token is followed by a multi-byte integer. */
    public static final byte TOKEN_EXT_INT_0 =      (byte)0x80;

    /** A token indicating an integer document specific extension token. This token is followed by a multi-byte integer. */
    public static final byte TOKEN_EXT_INT_1 =      (byte)0x81;

    /** A token indicating an integer document specific extension token. This token is followed by a multi-byte integer. */
    public static final byte TOKEN_EXT_INT_2 =      (byte)0x82;

    /** A token indicating a string table reference. The following integer indicates the offset into the string table. */
    public static final byte TOKEN_STR_TABLE_REF =  (byte)0x83;

    /** A token indicating an unknown element with attributes but content. The following integer indicates the offset into the string table. */
    public static final byte TOKEN_LITERAL_A =      (byte)0x84;

    /** A token indicating a byte document specific extension token. */
    public static final byte TOKEN_EXT_BYTE_0 =     (byte)0xC0;

    /** A token indicating a byte document specific extension token. */
    public static final byte TOKEN_EXT_BYTE_1 =     (byte)0xC1;

    /** A token indicating a byte document specific extension token. */
    public static final byte TOKEN_EXT_BYTE_2 =     (byte)0xC2;

    /** A token indicating a string table reference. The following integer indicates the length of the data and the actual data follows that. */
    public static final byte TOKEN_OPAQUE_DATA =    (byte)0xC3;

    /** A token indicating an unknown element with attributes and content. The following integer indicates the offset into the string table. */
    public static final byte TOKEN_LITERAL_AC =     (byte)0xC4;
    
    
    //The encoding to use when handling strings
    private static final String ENCODING_UTF8 =     "UTF-8";

    
    //a class containing state information used when parsing tags
    static class Tag
    {
        public byte tagId;
        public Codepage codepage;

        public Tag(byte id, Codepage cp)
        {
            tagId = id;
            codepage = cp;
        }
    }
    
    //useful bit-masks
    private static final byte MASK_TAG_ELEMENT_ID =     (byte)0x3F; //00111111 - reveals the identity of a tag (encoded in bits 0-5) 
    private static final byte MASK_TAG_HAS_CONTENT =    (byte)0x40; //01000000 - reveals whether or not a tag contains content (bit 6) 
    private static final byte MASK_TAG_HAS_ATTRS =      (byte)0x80; //10000000 - reveals whether or not a tag contains attributes (bit 7) 
    private static final byte MASK_INT_DATA =           (byte)0x7F; //01111111 - reveals the data part of a multi-byte integer (bits 0-7) 
    private static final byte MASK_INT_CONTINUATION =   (byte)0x80; //10000000 - reveals whether or not there is more data to read for a multi-byte integer (bit 7) 

    
    /**
     * Writes data that encodes the specified tag with no attributes or content to the specified output stream. <br/><br/>
     * 
     * A tag is encoded in a single byte where bits 0-6 identify the element, bit 6 indicates whether 
     * or not the element contains content and bit 7 indicates whether or not the element contains 
     * an attribute list.
     * 
     * @param outputStream  the stream to write the tag to.
     * @param tagId         the identity of the tag (as defined by some codepage).
     * @throws IOException if the tag couldn't be written to the stream.
     */
    public static void writeTag(OutputStream outputStream, byte tagId)
        throws IOException
    {
        writeTag(outputStream, tagId, false, false);
    }
    
    /**
     * Writes data that encodes the specified tag with content but no attributes to the specified output stream. <br/><br/>
     * 
     * A tag is encoded in a single byte where bits 0-6 identify the element, bit 6 indicates whether 
     * or not the element contains content and bit 7 indicates whether or not the element contains 
     * an attribute list.
     * 
     * @param outputStream  the stream to write the tag to.
     * @param tagId         the identity of the tag (as defined by some codepage).
     * @param hasContent    indicates whether or not the element has content.
     * @throws IOException if the tag couldn't be written to the stream.
     */
    public static void writeTag(OutputStream outputStream, byte tagId, boolean hasContent)
        throws IOException
    {
        writeTag(outputStream, tagId, false, hasContent);
    }
    
    /**
     * Writes data that encodes the specified tag and string content to the specified output stream. <br/><br/>
     * 
     * A tag is encoded in a single byte where bits 0-6 identify the element, bit 6 indicates whether 
     * or not the element contains content and bit 7 indicates whether or not the element contains 
     * an attribute list.
     * 
     * @param outputStream  the stream to write the tag to.
     * @param tagId         the identity of the tag (as defined by some codepage).
     * @param content       the content of the tag to write.
     * @throws IOException if the tag couldn't be written to the stream.
     */
    public static void writeTag(OutputStream outputStream, byte tagId, String content)
        throws IOException
    {
        writeTag(outputStream, tagId, false, true);
        writeInlineString(outputStream, content);
        writeTagEnd(outputStream);
    }
    
    /**
     * Writes data that encodes the specified tag and binary content to the specified output stream. <br/><br/>
     * 
     * A tag is encoded in a single byte where bits 0-6 identify the element, bit 6 indicates whether 
     * or not the element contains content and bit 7 indicates whether or not the element contains 
     * an attribute list.
     * 
     * @param outputStream  the stream to write the tag to.
     * @param tagId         the identity of the tag (as defined by some codepage).
     * @param content       the content of the tag to write.
     * @throws IOException if the tag couldn't be written to the stream.
     */
    public static void writeTag(OutputStream outputStream, byte tagId, byte[] content)
        throws IOException
    {
        writeTag(outputStream, tagId, false, true);
        writeOpaqueDataBegin(outputStream, content.length);
        writeOpaqueData(outputStream, content, content.length);
        writeTagEnd(outputStream);
    }
    
    /**
     * Writes data that encodes the specified tag and associated properties to the specified output stream. <br/><br/>
     * 
     * A tag is encoded in a single byte where bits 0-6 identify the element, bit 6 indicates whether 
     * or not the element contains content and bit 7 indicates whether or not the element contains 
     * an attribute list.
     * 
     * @param outputStream  the stream to write the tag to.
     * @param tagId         the identity of the tag (as defined by some codepage).
     * @param hasAttributes indicates whether or not the element has attributes.
     * @param hasContent    indicates whether or not the element has content.
     * @throws IOException if the tag couldn't be written to the stream.
     */
    public static void writeTag(OutputStream outputStream, byte tagId, boolean hasAttributes, boolean hasContent)
        throws IOException
    {
        if (hasAttributes)
            tagId |= MASK_TAG_HAS_ATTRS;

        if (hasContent)
            tagId |= MASK_TAG_HAS_CONTENT;
        
        //write the data
        outputStream.write(tagId);
    }
    
    /**
     * Writes data that encodes the end of a tag to the specified output stream.
     * 
     * @param outputStream  the stream to write the tag to.
     * @throws IOException if the end tag couldn't be written to the stream.
     */
    public static void writeTagEnd(OutputStream outputStream)
        throws IOException
    {
        //write the data
        outputStream.write(TOKEN_END);
    }
    
    
    /**
     * Writes a WBXML header containing the specified information.
     * 
     * @param outputStream  the stream to write the header to.
     * @param version       the {@link #VERSION_1_1 version number} to write.
     * @param charset       the {@link #CHARSET_UTF8 character set} to write. 
     * @param docId         the public identifier of the XML document being encoded. May be null or empty.
     * @throws IOException if the header couldn't be written to the stream.
     */
    public static void writeHeader(OutputStream outputStream, int version, int charset, String docId)
        throws IOException
    {
        //write the WBXML version
        outputStream.write((byte)version);
        
        //write the document ID as an index into the string table - always the first string table entry
        outputStream.write((byte)0x00);
        outputStream.write((byte)0x00);
        
        //write the character set
        writeInt(outputStream, charset);
        
        //write the string table (if any)
        if ( (docId != null) && (docId.length() > 0) )
        {
            //string table contains only the document ID
            byte[] docIdBytes = docId.getBytes(ENCODING_UTF8);
            writeInt(outputStream, docIdBytes.length);
            outputStream.write(docIdBytes);
        }
        else
        {
            //string table is empty
            outputStream.write((byte)0x00);
        }
    }
    
    
    /**
     * Writes a codepage switch instruction to the specified output stream.
     * 
     * @param outputStream  the stream to write the instruction to.
     * @param pageIndex     the index identifying the new page.
     * @throws IOException if the instruction couldn't be written to the stream.
     */
    public static void writePageSwitch(OutputStream outputStream, int pageIndex)
        throws IOException
    {
        //write the data
        outputStream.write(TOKEN_SWITCH_PAGE);
        outputStream.write(pageIndex);
    }

    
    /**
     * Reads a byte from the specified input stream.
     * 
     * @param inputStream the stream to read the data from.
     * @return The byte that was read.
     * @throws IOException if the byte couldn't be read from the stream.
     */
    private static int readByte(InputStream inputStream) 
        throws IOException
    {
        //read the data
        int data = inputStream.read();
        if (data == -1)
            throw new IOException("unexpected end of stream while reading byte data");
        
        return data;
    }

    
    /**
     * Reads a multi-byte integer from the specified input stream. <br/><br/>
     * 
     * A single integer value is encoded into a sequence of N bytes. The first N-1 bytes 
     * have the continuation flag set to 1 while the final byte in the series has a 
     * continuation flag value of 0.
     * 
     * @param inputStream the stream to read the data from.
     * @return The integer that was read.
     * @throws IOException if the integer couldn't be read from the stream.
     */
    private static long readInt(InputStream inputStream) 
        throws IOException
    {
        long result = 0;
        while (true)
        {
            //read the data
            int data = inputStream.read();
            if (data == -1)
                throw new IOException("unexpected end of stream while reading multi-byte integer");

            //get the scalar value (i.e. bits 0-7) and append it to our result 
            result = result | (data & MASK_INT_DATA);
            
            //nothing more to do if the continuation bit (i.e. bit 8) is not set
            if ((data & MASK_INT_CONTINUATION) == 0)
                break;

            //need to read more data so shift it up for the next value
            result = (result << 7);
        }

        return result;
    }

    /**
     * Writes the specified data to the specified output stream as a multi-byte integer. <br/><br/>
     * 
     * A single data value is encoded into a sequence of N bytes. The first N-1 bytes 
     * have the continuation flag set to 1 while the final byte in the series has a 
     * continuation flag value of 0.
     * 
     * @param outputStream  the stream to write the data to.
     * @param data          the data to write.
     * @throws IOException if the integer couldn't be written to the stream.
     */
    private static void writeInt(OutputStream outputStream, long data)
        throws IOException
    {
        //convert the data to a multi-byte array (which will be in the reverse order)
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (data != 0)
        {
            //write the data part of the integer (bits 0-6)
            output.write((int)(data & MASK_INT_DATA));

            //get the next set of bits
            data = (data >> 7);
        }
        byte[] reversedBits = output.toByteArray();
        int reversedBitsLen = reversedBits.length;

        //reorder the bits and set the continuation bit as necessary
        byte[] result = new byte[reversedBitsLen];
        for (int i = 0; i < reversedBitsLen; i++)
        {
            //only set the continuation bit if this isn't the last byte.
            if (i != (reversedBitsLen - 1)) 
                result[i] = (byte)(reversedBits[reversedBitsLen - i - 1] | MASK_INT_CONTINUATION);
            else
                result[i] = reversedBits[0];
        }
        
        //write the data
        outputStream.write(result);
    }

    
    /**
     * Reads an in-line UTF-8 string from the specified input stream.
     * 
     * @param inputStream   the stream to read the data from.
     * @param buffer        the temporary buffer to use when reading the data.
     * @return The string that was read.
     * @throws IOException if the string couldn't be read from the stream.
     */
    private static String readInlineString(InputStream inputStream, ByteArrayOutputStream buffer) 
        throws IOException
    {
        //reset the buffer
        buffer.reset();

        int data;
        while ((data = inputStream.read()) != 0x00)
        {
            if (data == -1)
                throw new IOException("unexpected end of stream while reading string literal");
            
            buffer.write(data);
        }
        
        //convert to a string (assumes UTF-8 encoding)
        return new String(buffer.toByteArray(), ENCODING_UTF8);
    }
    
    /** 
     * Writes the specified string to the specified output stream as an in-line UTF-8 string.
     *  
     * @param outputStream  the output stream that the string should be written to.
     * @param string        the string to be written.
     * @throws IOException if the string couldn't be written.  
     */
    private static void writeInlineString(OutputStream outputStream, String string)
        throws IOException
    {
        //write the UTF-8, null-terminated string
        outputStream.write(TOKEN_INLINE_STRING);
        outputStream.write(string.getBytes(ENCODING_UTF8));
        outputStream.write(0x00);
    }
    

    /** 
     * Begin writing the specified opaque data to the specified output stream. <br/><br/>
     * 
     * This method should be followed by one or more calls to {@link #writeOpaqueData(OutputStream, byte[], int)}
     * to write the actual data to the stream.
     *  
     * @param outputStream  the output stream that the data should be written to.
     * @param length        the total number of bytes of opaque data that will be written.
     * @throws IOException if the data couldn't be written.  
     * 
     * @see #writeOpaqueData(OutputStream, byte[], int)
     */
    public static void writeOpaqueDataBegin(OutputStream outputStream, long length)
        throws IOException
    {
        outputStream.write(TOKEN_OPAQUE_DATA);

        //write the data length
        writeInt(outputStream, length);
    }
    
    /** 
     * Writes the specified opaque data to the specified output stream. <br/><br/>
     * 
     * This method may be called multiple times (which is useful when dealing with large 
     * amounts of data). A call to this method must be preceded by a call to {@link #writeOpaqueDataBegin(OutputStream, long)}. 
     *  
     * @param outputStream  the output stream that the data should be written to.
     * @param data          the data to be written.
     * @param length        the number of bytes that should be written.
     * @throws IOException if the data couldn't be written.
     * 
     * @see #writeOpaqueDataBegin(OutputStream, long)
     */
    public static void writeOpaqueData(OutputStream outputStream, byte[] data, int length)
        throws IOException
    {
        //write the data itself
        outputStream.write(data, 0, length);
    }
    
    
    /**
     * Parses the data from the specified input stream using the specified codepages. <br/><br/>
     * 
     * While the data is being parsed, the relevant codepage (from the ones supplied) will be 
     * notified when tags or data are encountered. <br/><br/>
     * 
     * This parser has the following limitations:
     * <ul>
     *      <li> Only WBXML v1.2 is supported.
     *      <li> Only UTF-8 encoded strings are supported.
     *      <li> String table references are not supported.
     *      <li> Elements with attributes are not supported.
     * </ul>
     * 
     * @param inputStream   the input stream to read the data from. Must not be null. 
     * @param codepages     the codepages used to parse the input stream. Must not be null or empty.
     * @throws WbxmlException if an error is found while parsing.
     * @throws IOException  if the input stream could not be read.
     */
    public static void parse(InputStream inputStream, Codepage[] codepages) 
        throws WbxmlException, IOException
    {
        if ( (codepages == null) || (codepages.length <= 0) )
            throw new IllegalArgumentException("no codepages specified");
        
        //initialize the codepages
        Stack tagStack = new Stack();
        for (int i = 0; i < codepages.length; i++)
            codepages[i].setTagStack(tagStack);
        Codepage currentCodepage = codepages[0];
        
        //read the WBXML version and ensure it's v1.2
        int wbxmlVersion = readByte(inputStream);
        if (wbxmlVersion != VERSION_1_2)
            throw new WbxmlException("unsupported WBXML version: " + Integer.toHexString(wbxmlVersion));

        //read the document public identifier
        int docId = (int)readInt(inputStream);
        if (docId == 0)
        {
            //no document ID specified - instead, ID is given as an index into the string table - not used in SyncML so ignore
            readInt(inputStream);
        }

        //read the character set and ensure it's UTF8 (i.e. MIBenum code 106 as defined by IANA)
        long charset = readInt(inputStream);
        if (charset != CHARSET_UTF8)
            throw new WbxmlException("unsupported character encoding: " + charset);

        //read the string table length
        int stringTableLen = (int)readInt(inputStream);
        if (stringTableLen > 0)
        {
            //read the string table
            byte[] stringTableData = new byte[stringTableLen];
            if (inputStream.read(stringTableData, 0, stringTableLen) < stringTableLen)
                throw new WbxmlException("unexpected end of stream while reading string table");
        }

        //create buffers so that they can be reused while parsing
        byte[] opaqueDataBuffer = new byte[256];
        ByteArrayOutputStream stringDataBuffer = new ByteArrayOutputStream(256);
        
        //all static data has now been read - now read the rest of the document
        int data;
        while ((data = inputStream.read()) != -1)
        {
            switch((byte)data)
            {
                case TOKEN_SWITCH_PAGE:
                {
                    //codepage switch with page index following - read the index of the new codepage
                    int codepageIndex = readByte(inputStream);
                    if ( (codepageIndex < 0) || (codepageIndex > codepages.length) )
                        throw new WbxmlException("invalid codepage switch found: " + codepageIndex);

                    //switch to the new codepage
                    currentCodepage = codepages[codepageIndex];
                    break;
                }
                case TOKEN_END:
                {
                    //end of an element tag - notify the current codepage
                    Tag tag = (Tag)tagStack.peek();
                    tag.codepage.onTagEnd(tag.tagId);
                    tagStack.pop();
                    break;
                }
                case TOKEN_INLINE_STRING:
                {
                    //in-line string with null-terminated string following - read the string and notify the current codepage
                    Tag tag = (Tag)tagStack.peek();
                    tag.codepage.onStringData(tag.tagId, readInlineString(inputStream, stringDataBuffer));
                    break;
                }
                case TOKEN_OPAQUE_DATA:
                {
                    //opaque data with data length and actual data following - read the data and add it to the current tag
                    long opaqueDataLen = readInt(inputStream);
                    Tag tag = (Tag)tagStack.peek();
                    tag.codepage.onOpaqueDataBegin(tag.tagId, opaqueDataLen);
                    
                    //read the opaque data in chunks
                    for (int dataRead = 0; dataRead < opaqueDataLen; )
                    {
                        //determine how much data to read
                        int requestDataSize;
                        long remainingLen = opaqueDataLen - dataRead;
                        if (remainingLen < opaqueDataBuffer.length)
                            requestDataSize = (int)remainingLen;
                        else
                            requestDataSize = opaqueDataBuffer.length;
                        
                        int readSize = inputStream.read(opaqueDataBuffer, 0, requestDataSize);
                        if (readSize == -1)
                        {
                            tag.codepage.onOpaqueDataEnd(tag.tagId, false);
                            throw new IOException("unexpected end of stream while reading opaque data");
                        }
                        
                        tag.codepage.onOpaqueData(tag.tagId, opaqueDataBuffer, readSize);
                        dataRead += readSize;
                    }
                    tag.codepage.onOpaqueDataEnd(tag.tagId, true);
                    break;
                }
                case TOKEN_CHAR_ENTITY:
                {
                    //character entity with entity value following - ignore
                    readInt(inputStream);
                    break;
                }
                case TOKEN_LITERAL:
                {
                    //unknown tag or attribute name with string table index following - ignore 
                    readInt(inputStream);
                    break;
                }
                case TOKEN_STR_TABLE_REF:
                {
                    //string table reference with string table index following - ignore 
                    readInt(inputStream);
                    break;
                }
                default:
                {
                    //a token referring to a tag in the current code-page - parse the token to determine the tag details
                    byte tagId = (byte)(data & MASK_TAG_ELEMENT_ID);
                    boolean tagHasContent = ((data & MASK_TAG_HAS_CONTENT) > 0);

                    //add it to the list of tags
                    tagStack.push( new Tag(tagId, currentCodepage) );
                    
                    //notify the codepage that the tag is starting
                    currentCodepage.onTagStart(tagId, tagHasContent);

                    //end the tag now if it has no content 
                    if (! tagHasContent)
                    {
                        currentCodepage.onTagEnd(tagId);
                        tagStack.pop();
                    }
                }
            }
        }
    }
}
