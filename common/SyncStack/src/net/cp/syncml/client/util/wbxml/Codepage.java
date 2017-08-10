/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.wbxml;

import java.io.*;
import java.util.Stack;

import net.cp.syncml.client.util.Logger;


/**
 * An abstract class defining a WBXML codepage. <br/><br/>
 * 
 * A codepage is responsible for handling tag IDs and their associated content (if any) as 
 * they are parsed.
 *
 * @see Wbxml#parse(InputStream, Codepage[])
 * 
 * @author Denis Evoy
 */
public abstract class Codepage
{
    protected Logger log;                   //the logger used to log activity
    private String[] tagNames;              //the names of the tags

    //state information when parsing WBXML data
    private Stack inTagStack;               //the stack containing nested tag IDs

    //state information when writing WBXML data
    private int nestingLevel;               //the current nesting level when writing tags
    
    
    /**
     * Creates a new codepage with the specified logger.
     * 
     * @param logger the logger to use to log activity.
     */
    public Codepage(Logger logger)
    {
        log = logger;
        tagNames = getTagNames();
    }
    
    
    /**
     * Sets the stack which will contain the IDs of nested tags as they are parsed.
     *  
     * @param stack the stack containing the IDs of nested tags.
     */
    public void setTagStack(Stack stack)
    {
        inTagStack = stack;
    }
    

    /**
     * Returns the current level of nested tags that have been written so far.
     *  
     * @return the current nesting level.
     */
    public int getNestingLevel()
    {
        return nestingLevel;
    }

    /**
     * Sets the level of nested tags as they are being written.
     *  
     * @param level the nesting level.
     */
    public void setNestingLevel(int level)
    {
        nestingLevel = level;
    }

    
    /** 
     * Returns the ID of the tag which is the parent of the current tag.
     * 
     * @return The ID of the parent tag or 0 if the tag has no parent.
     */
    public int getParentId()
    {
        return getAncestorId(0);
    }
    
    /** 
     * Returns the ID of the tag which is the specified ancestor of the current tag.
     * 
     * @param index the index of the ancestor whose ID should be returned.
     * @return The ID of the ancestor tag or 0 if the tag has no such ancestor.
     */
    public int getAncestorId(int index)
    {
        int ancestorIndex = inTagStack.size() - index - 2;
        if ( (ancestorIndex < 0) || (ancestorIndex >= inTagStack.size()) )
            return 0;
        
        Wbxml.Tag tag = (Wbxml.Tag)inTagStack.elementAt(ancestorIndex);
        return tag.tagId;
    }
    
    
    /**
     * Returns the names of the tags defined by the codepage. <br/><br/>
     * 
     * Implementations must return the tag names in the same order as the 
     * tag IDs are defined.
     * 
     * @return The names of the tags defined by the codepage.
     */
    public abstract String[] getTagNames();
    
    
    /**
     * Called when the start of a tag is encountered in the WBXML document.
     * 
     * @param tagId         the ID of the tag that has been encountered. Will be non-zero and positive.
     * @param hasContent    set to <code>true</code> if the tag contains contents (data or child tags). 
     * @throws WbxmlException if the event couldn't be handled.
     */
    public void onTagStart(int tagId, boolean hasContent) 
        throws WbxmlException
    {
        if (log != null)
            log.debug("WBXML: IN: " + getInIndent(0) + "<" + getTagName(tagId) + ">");
    }

    /**
     * Called when the end of a tag is encountered in the WBXML document.
     * 
     * @param tagId the ID of the tag that has been ended. Will be non-zero and positive.
     * @throws WbxmlException if the event couldn't be handled.
     */
    public void onTagEnd(int tagId)
        throws WbxmlException
    {
        if (log != null)
            log.debug("WBXML: IN: " + getInIndent(0) + "</" + getTagName(tagId) + ">");
    }
    
    
    /**
     * Called when in-line string data is encountered in the WBXML document.
     * 
     * @param tagId the ID of the element that the in-line string data belongs to. Will be non-zero and positive.
     * @param data  the string data that has been encountered. May be null or empty.
     * @throws WbxmlException if the event couldn't be handled.
     */
    public void onStringData(int tagId, String data) 
        throws WbxmlException
    {
        if (log != null)
            log.debug("WBXML: IN: " + getInIndent(1) + data);
    }

    
    /**
     * Called when opaque data is first encountered in a tag in the WBXML document. <br/><br/>
     * 
     * This method will be followed by zero or more calls to {@link #onOpaqueData(int, byte[], int)}}
     * which will contain the actual data itself and a call to {@link #onOpaqueDataEnd(int, boolean)}.
     * 
     * @param tagId     the ID of the tag that the opaque data belongs to. Will be non-zero and positive.
     * @param length    the total size (in bytes) of the opaque data to expect. Will be zero or positive.
     * @throws WbxmlException if the event couldn't be handled.
     */
    public void onOpaqueDataBegin(int tagId, long length) 
        throws WbxmlException
    {
        if (log != null)
            log.debug("WBXML: IN: " + getInIndent(1) + "Data Start [Length=" + length + "]");
    }

    /**
     * Called when opaque data is encountered in a tag in the WBXML document. <br/><br/>
     * 
     * Note that this method may be called multiple times (especially when dealing with large 
     * amounts of data). This method will be followed by a call to {@link #onOpaqueDataEnd(int, boolean)}.
     * The specified data should be cached and not acted upon until {@link #onOpaqueDataEnd(int, boolean)}
     * is called.
     * 
     * @param tagId     the ID of the tag that the opaque data belongs to. Will be non-zero and positive.
     * @param data      the data that was read. Will not be null or empty.
     * @param length    the number of bytes that were read. Will be zero or positive.
     * @throws WbxmlException if the event couldn't be handled.
     */
    public void onOpaqueData(int tagId, byte[] data, int length) 
        throws WbxmlException
    {
        if (log != null)
            log.debug("WBXML: IN: " + getInIndent(1) + "data[" + length + "]");
    }

    /**
     * Called when no more opaque data is to be read from a tag in the WBXML document. <br/><br/>
     * 
     * Implementations should only act on the data that has been read so far if
     * <code>commit</code> is <code>true</code>. If <code>commit</code> is <code>false</code>,
     * any data that has been read so far should be discarded.
     * 
     * @param tagId     the ID of the tag that the opaque data belongs to. Will be non-zero and positive.
     * @param commit    set to <code>true</code> if all data has been read from the tag.
     * @throws WbxmlException if the event couldn't be handled.
     */
    public void onOpaqueDataEnd(int tagId, boolean commit) 
        throws WbxmlException
    {
        if (log != null)
            log.debug("WBXML: IN: " + getInIndent(1) + "Data End [Commit=" + commit + "]");
    }
    
    
    /**
     * Writes a tag with the specified ID to the specified output stream.
     * 
     * @param outputStream  the output stream to write the tag to. 
     * @param tagId         the ID of the tag to write. Must be zero or positive.
     * @param hasContent    indicates whether or not the tag contains child tags.
     * @throws WbxmlException   if the tag is invalid in some way.
     * @throws IOException      if the tag couldn't be written to the output stream.
     */
    protected void writeTag(OutputStream outputStream, byte tagId, boolean hasContent)
        throws WbxmlException, IOException
    {
        if (log != null)
        {
            if (hasContent)
                log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "<" + getTagName(tagId) + ">");
            else
                log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "<" + getTagName(tagId) + "/>");
        }
        
        Wbxml.writeTag(outputStream, tagId, hasContent);
        
        if (hasContent)
            nestingLevel++;
    }
    
    /**
     * Writes a tag with the specified ID and string content to the specified output stream.
     * 
     * @param outputStream  the output stream to write the tag to. 
     * @param tagId         the ID of the tag to write. Must be zero or positive.
     * @param content       the text content of the tag (which will be written as a string literal).
     * @throws WbxmlException   if the tag or content is invalid in some way.
     * @throws IOException      if the tag or content couldn't be written to the output stream.
     */
    protected void writeTag(OutputStream outputStream, byte tagId, String content)
        throws WbxmlException, IOException
    {
        if (log != null)
        {
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "<" + getTagName(tagId) + ">");
            log.debug("WBXML: OUT: " + getIndent(nestingLevel + 1) + content);
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "</" + getTagName(tagId) + ">");
        }
        
        Wbxml.writeTag(outputStream, tagId, content);
    }
    
    
    /**
     * Writes a tag with the specified ID and binary content to the specified output stream.
     * 
     * @param outputStream  the output stream to write the tag to. 
     * @param tagId         the ID of the tag to write. Must be zero or positive.
     * @param content       the binary content of the tag (which will be written as opaque data).
     * @throws WbxmlException   if the tag or content is invalid in some way.
     * @throws IOException      if the tag or content couldn't be written to the output stream.
     */
    protected void writeTag(OutputStream outputStream, byte tagId, byte[] content)
        throws WbxmlException, IOException
    {
        if (log != null)
        {
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "<" + getTagName(tagId) + ">");
            log.debug("WBXML: OUT: " + getIndent(nestingLevel + 1) + "data[" + content.length + "]");
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "</" + getTagName(tagId) + ">");
        }
        
        Wbxml.writeTag(outputStream, tagId, content);
    }
    
    
    /**
     * Writes the end of the specified tag to the specified output stream.
     * 
     * @param outputStream  the output stream to write the end tag to. 
     * @param tagId         the ID of the end tag to write. Must be zero or positive.
     * @throws WbxmlException   if the tag is invalid in some way.
     * @throws IOException      if the end tag couldn't be written to the output stream.
     */
    protected void writeTagEnd(OutputStream outputStream, byte tagId)
        throws WbxmlException, IOException
    {
        nestingLevel--;

        if (log != null)
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "</" + getTagName(tagId) + ">");
        
        Wbxml.writeTagEnd(outputStream);
    }
    
    
    /**
     * Begins writing opaque data of the specified length to the specified output stream. <br/><br/>
     * 
     * This is usually followed by one or more calls to {@link #writeOpaqueData(OutputStream, byte[], int)} to write the opaque
     * data in multiple chunks to the output stream. 
     * 
     * @param outputStream  the output stream to begin writing the opaque data to. 
     * @param length        the total size (in bytes) of the opaque data that will be written. Must be non-zero and positive.
     * @throws WbxmlException   if writing opaque data is invalid in some way.
     * @throws IOException      if the beginning of the opaque data couldn't be written to the output stream.
     */
    protected void writeOpaqueDataBegin(OutputStream outputStream, long length)
        throws WbxmlException, IOException
    {
        if (log != null)
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "Data Start [Length=" + length + "]");

        Wbxml.writeOpaqueDataBegin(outputStream, length);
    }
    
    /**
     * Writes the specified opaque data of the specified length to the specified output stream. <br/><br/>
     * 
     * This method can be called one or more times. This allows a large amount of opaque data to be written in multiple chunks. 
     * 
     * @param outputStream  the output stream to write the opaque data to. 
     * @param data          the buffer containing the data to write.
     * @param length        the number of bytes to write to the output stream. Must be non-zero and positive.
     * @throws WbxmlException   if writing opaque data is invalid in some way.
     * @throws IOException      if the opaque data couldn't be written to the output stream.
     */
    protected void writeOpaqueData(OutputStream outputStream, byte[] data, int length)
        throws WbxmlException, IOException
    {
        if (log != null)
            log.debug("WBXML: OUT: " + getIndent(nestingLevel) + "data[" + length + "]");

        Wbxml.writeOpaqueData(outputStream, data, length);
    }
    
    
    private String getInIndent(int additional)
    {
        if (inTagStack != null)
            return getIndent(inTagStack.size() + additional - 1);
        
        return "";
    }
    
    private String getIndent(int indent)
    {
        StringBuffer indentBuffer = new StringBuffer();
        for (int i = 0; i < indent; i++)
            indentBuffer.append("    ");
        
        return indentBuffer.toString();
    }

    private String getTagName(int tagId)
        throws WbxmlException
    {
        //all tag IDs start at 5 - adjust the ID so we can index into the array of tag names
        tagId = tagId - 5;
        if ( (tagId < 0) || (tagId >= tagNames.length) )
            throw new WbxmlException("unknown tag found: " + tagId);
        
        return tagNames[tagId];
    }
}
