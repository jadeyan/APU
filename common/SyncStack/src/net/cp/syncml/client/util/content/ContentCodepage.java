/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util.content;


import java.util.*;
import java.io.*;

import net.cp.syncml.client.devinfo.*;
import net.cp.syncml.client.util.*;
import net.cp.syncml.client.util.wbxml.*;


/**
 * A class defining a WBXML codepage representing files and folders. <br/><br/>
 * 
 * This codepage is specific to Critical Path as an official WBXML codepage doesn't yet 
 * exist for files or folders. Therefore, the following associated content types have been defined:
 * <ul>
 *      <li> application/vnd.omads-file+cpwbxml
 *      <li> application/vnd.omads-folder+cpwbxml
 * </ul>
 * 
 * To use this codepage, first provide an implementation for the {@link ContentHandler} interface which
 * will handle file and folder objects as they are parsed. The codepage can then be used as follows:
 * <pre>
 *      ContentHandler handler = new MyContentHandler();
 *      Codepage codepage = new ContentCodepage(handler);
 *      Wbxml.parse(inputStream, new Codepage[] { codepage } );
 *      ...
 * </pre>
 * 
 * You can also use this class to write {@link #writeFileBegin(OutputStream, ContentFile, boolean, long) file} 
 * and {@link #writeFolder(OutputStream, ContentFolder) folder} objects in WBXML format to an output stream. <br/><br/>
 * 
 * Note that this codepage is based on the following specifications:
 * <ul>
 *      <li> File data object specification Approved Version 1.2.1 – 10 August 2007
 *      <li> Folder data object specification Approved Version 1.2.1 – 10 August 2007
 * </ul>
 *
 * @author Denis Evoy
 */
public class ContentCodepage extends Codepage
{
    /** Defines a content type indicating a data sync file encoded in WBXML. */
    public static final ContentType CT_CP_FILE_WBXML =      new ContentType("application", "vnd.omads-file+cpwbxml", "1.0");

    /** Defines a content type indicating a data sync folder encoded in WBXML. */
    public static final ContentType CT_CP_FOLDER_WBXML =    new ContentType("application", "vnd.omads-folder+cpwbxml", "1.0");
    
    
    //File/Folder codepage definition
    private static final byte TAG_A =                0x05;
    private static final byte TAG_ACCESSED =         0x06;
    private static final byte TAG_ATTRIBUTES =       0x07;
    private static final byte TAG_BODY =             0x08;
    private static final byte TAG_CREATED =          0x09;
    private static final byte TAG_CTTYPE =           0x0A;
    private static final byte TAG_D =                0x0B;
    private static final byte TAG_EXT =              0x0C;
    private static final byte TAG_FILE =             0x0D;
    private static final byte TAG_FOLDER =           0x0E;
    private static final byte TAG_H =                0x0F;
    private static final byte TAG_MODIFIED =         0x10;
    private static final byte TAG_NAME =             0x11;
    private static final byte TAG_R =                0x12;
    private static final byte TAG_ROLE =             0x13;
    private static final byte TAG_S =                0x14;
    private static final byte TAG_SIZE =             0x15;
    private static final byte TAG_W =                0x16;
    private static final byte TAG_X =                0x17;
    private static final byte TAG_XNAM =             0x18;
    private static final byte TAG_XVAL =             0x19;
    
    private static final String[] TAG_NAMES =        { "A", "Accessed", "Attributes", "Body", "Created", "CTType", "D", "Ext", "File", "Folder", "H", "Modified", "Name", "R", "Role", "S", "Size", "W", "X", "XNam", "XVal" };

    
    private ContentHandler contentHandler;          //the handler to use when content (files or folders) are received
    
    private ContentObject inObject;                 //the file/folder object being parsed
    
    private Vector inExtensions;                    //the collection of Extensions being parsed
    private String inExtName;                       //the name of the extension being parsed
    private Vector inExtValues;                     //the String values of the extension being parsed
    private boolean inFileContentPresent;           //indicates whether or not the file content is present

    
    /**
     * Creates a content codepage with the specified handler.
     * 
     * @param logger  the logger used to log activity.   
     * @param handler the handler to use when content (files or folders) are received. May be null.
     */
    public ContentCodepage(Logger logger, ContentHandler handler)
    {
        super(logger);
        
        contentHandler = handler;
    }

    
    public String[] getTagNames()
    {
        return TAG_NAMES;
    }
    
    public void onTagStart(int tagId, boolean hasContent) 
        throws WbxmlException
    {
        super.onTagStart(tagId, hasContent);
        
        if (tagId == TAG_A)
        {
            //set the archived attribute
            inObject.getAttributes().setArchived(true);
        }
        else if (tagId == TAG_ATTRIBUTES)
        {
            //create a new set of attributes
            inObject.setAttributes( new Attributes() );
            inObject.setField(ContentObject.FIELD_ATTRIBUTES);
        }
        else if (tagId == TAG_BODY)
        {
            //about to read the file body - so handle the file header information now
            inFileContentPresent = true;
            inObject.setField(ContentObject.FIELD_FILE_CONTENTS);
            if (contentHandler != null)
                contentHandler.onFileBegin((ContentFile)inObject, true);
        }
        else if (tagId == TAG_D)
        {
            //set the deletable attribute
            inObject.getAttributes().setDeletable(true);
        }
        else if (tagId == TAG_EXT)
        {
            //clear temporary variables used to store extension details
            inExtName = null;
            if (inExtValues != null)
                inExtValues.removeAllElements();
            inExtValues = null;
        }
        else if (tagId == TAG_FILE)
        {
            //clear variables used to store file details
            inObject = new ContentFile();
            inFileContentPresent = false;
        }
        else if (tagId == TAG_FOLDER)
        {
            //clear variables used to store folder details
            inObject = new ContentFolder();
        }
        else if (tagId == TAG_H)
        {
            //set the hidden attribute
            inObject.getAttributes().setHidden(true);
        }
        else if (tagId == TAG_R)
        {
            //set the readable attribute
            inObject.getAttributes().setReadable(true);
        }
        else if (tagId == TAG_S)
        {
            //set the system attribute
            inObject.getAttributes().setSystem(true);
        }
        else if (tagId == TAG_W)
        {
            //set the writable attribute
            inObject.getAttributes().setWritable(true);
        }
        else if (tagId == TAG_X)
        {
            //set the executable attribute
            inObject.getAttributes().setExecutable(true);
        }
    }

    public void onTagEnd(int tagId)
        throws WbxmlException
    {
        super.onTagEnd(tagId);
        
        if (tagId == TAG_EXT)
        {
            //create a new extension and add it to the list of extensions parsed so far
            if ( (inExtName == null) || (inExtName.length() <= 0) || (inExtValues.size() <= 0) )
                return;
            
            String[] values = new String[inExtValues.size()];
            for (int i = 0; i < inExtValues.size(); i++)
                values[i] = (String)inExtValues.elementAt(i);
                
            if (inExtensions == null)
                inExtensions = new Vector();
            inExtensions.addElement( new Extension(inExtName, values) );
        }
        else if (tagId == TAG_FILE)
        {
            //handle the file details - if the body is present, we've already done so
            if (inFileContentPresent)
                return;
            
            setObjectExtensions();
            if (contentHandler != null)
            {
                contentHandler.onFileBegin((ContentFile)inObject, false);
                contentHandler.onFileEnd((ContentFile)inObject, true);
            }
        }
        else if (tagId == TAG_FOLDER)
        {
            //handle the folder details
            setObjectExtensions();
            if (contentHandler != null)
                contentHandler.onFolder((ContentFolder)inObject);
        }
    }
    
    public void onStringData(int tagId, String data) 
        throws WbxmlException
    {
        super.onStringData(tagId, data);
        
        if (tagId == TAG_ACCESSED)
        {
            //set the access time 
            inObject.setAccessedTime( new DateTime(data) );
            inObject.setField(ContentObject.FIELD_ACCESS_TIME);
        }
        else if (tagId == TAG_CREATED)
        {
            //set the create time 
            inObject.setCreatedTime( new DateTime(data) );
            inObject.setField(ContentObject.FIELD_CREATED_TIME);
        }
        else if (tagId == TAG_CTTYPE)
        {
            //set the content type 
            ((ContentFile)inObject).setContentType( new ContentType(data) );
        }
        else if (tagId == TAG_MODIFIED)
        {
            //set the modified time 
            inObject.setModifiedTime( new DateTime(data) );
            inObject.setField(ContentObject.FIELD_MODIFIED_TIME);
        }
        else if (tagId == TAG_NAME)
        {
            //set the object name 
            inObject.setName(data);
            inObject.setField(ContentObject.FIELD_NAME);
        }
        else if (tagId == TAG_ROLE)
        {
            //set the folder role 
            ((ContentFolder)inObject).setRole(data);
            inObject.setField(ContentObject.FIELD_FOLDER_ROLE);
        }
        else if (tagId == TAG_SIZE)
        {
            //set the file size 
            ((ContentFile)inObject).setSize( Long.parseLong(data) );
        }
        else if (tagId == TAG_XNAM)
        {
            //temporarily record the extension name
            inExtName = data;
        }
        else if (tagId == TAG_XVAL)
        {
            //temporarily record the extension value
            if (inExtValues == null)
                inExtValues = new Vector();
            inExtValues.addElement(data);
        }
    }
    
    public void onOpaqueData(int tagId, byte[] data, int length) 
        throws WbxmlException
    {
        super.onOpaqueData(tagId, data, length);
        
        if (tagId == TAG_BODY)
        {
            //handle the file contents as opaque data
            if (contentHandler != null)
                contentHandler.onFileData((ContentFile)inObject, data, length);
        }
    }

    public void onOpaqueDataEnd(int tagId, boolean commit) 
        throws WbxmlException
    {
        super.onOpaqueDataEnd(tagId, commit);
        
        if (tagId == TAG_BODY)
        {
            //finish handling the file contents as opaque data
            if (contentHandler != null)
                contentHandler.onFileEnd((ContentFile)inObject, commit);
        }
    }
    
    
    /**
     * Writes the specified folder in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the folder information to.
     * @param folder        the folder information to write.
     * @throws IOException      if the file information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeFolder(OutputStream outputStream, ContentFolder folder)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_FOLDER, true);
        
        writeObject(outputStream, folder);
        
        if (folder.isFieldSet(ContentObject.FIELD_FOLDER_ROLE))
        {
	        String role = folder.getRole();
	        if ( (role != null) && (role.length() > 0) )
	            writeTag(outputStream, TAG_ROLE, role);
        }
        
        writeTagEnd(outputStream, TAG_FOLDER);
    }

    /**
     * Begins writing the specified file in WBXML format to the specified output stream. <br/><br/>
     * 
     * This method must be followed by one or more calls to {@link #writeFileData(OutputStream, ContentFile, byte[], int)}
     * (if <code>contentPresent</code> is <code>true</code>) and a final call to {@link #writeFileEnd(OutputStream, ContentFile, boolean)}.
     * 
     * @param outputStream      the output stream to write the file information to.
     * @param file              the file information to write.
     * @param contentPresent    set to <code>true</code> if the file content will be written.
     * @param contentSize       the size of the file content in bytes. Only necessary if <code>contentPresent</code> is <code>true</code>.
     * @throws IOException      if the file information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     * 
     * @see #writeFileData(OutputStream, ContentFile, byte[], int)
     * @see #writeFileEnd(OutputStream, ContentFile, boolean)
     */
    public void writeFileBegin(OutputStream outputStream, ContentFile file, boolean contentPresent, long contentSize)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_FILE, true);
        
        writeObject(outputStream, file);
        
        if (file.isFieldSet(ContentObject.FIELD_FILE_CONTENTS))
        {
	        ContentType contentType = file.getContentType();
	        if (contentType != null)
	            writeTag(outputStream, TAG_CTTYPE, contentType.toString());
        
	        long size = file.getSize();
	        if (size >= 0)
	            writeTag(outputStream, TAG_SIZE, Long.toString(size));
        
	        if (contentPresent)
	        {
	            writeTag(outputStream, TAG_BODY, true);
	            writeOpaqueDataBegin(outputStream, contentSize);
	        }
        }
    }
    
    /**
     * Writes the specified file data in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the file data to.
     * @param file          the file whose data is being written. 
     * @param data          the data to write.
     * @param length        the number of bytes that should be written.
     * @throws IOException      if the file data couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     * 
     * @see #writeFileBegin(OutputStream, ContentFile, boolean, long)
     * @see #writeFileEnd(OutputStream, ContentFile, boolean)
     */
    public void writeFileData(OutputStream outputStream, ContentFile file, byte[] data, int length)
        throws WbxmlException, IOException
    {
        writeOpaqueData(outputStream, data, length);        
    }

    /**
     * @param outputStream      the output stream to finish writing the file information to.
     * @param file              the file information that has been written.
     * @param contentPresent    set to <code>true</code> if the file content was written.
     * @throws IOException      if the file information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     * 
     * @see #writeFileBegin(OutputStream, ContentFile, boolean, long)
     * @see #writeFileData(OutputStream, ContentFile, byte[], int)
     */
    public void writeFileEnd(OutputStream outputStream, ContentFile file, boolean contentPresent)
        throws WbxmlException, IOException
    {
    	if (file.isFieldSet(ContentObject.FIELD_FILE_CONTENTS))
    	{
	        if (contentPresent)
	            writeTagEnd(outputStream, TAG_BODY);
    	}

        writeTagEnd(outputStream, TAG_FILE);
    }
    
    
    /* Writes the specified content objects in WBXML format to the specified output stream. */
    private void writeObject(OutputStream outputStream, ContentObject object)
        throws WbxmlException, IOException
    {
    	if (object.isFieldSet(ContentObject.FIELD_NAME))
    		writeTag(outputStream, TAG_NAME, object.getName());
        
    	if (object.isFieldSet(ContentObject.FIELD_ACCESS_TIME))
    	{
	        DateTime accessedTime = object.getAccessedTime();
	        if (accessedTime != null)
	            writeTag(outputStream, TAG_ACCESSED, accessedTime.toString());
    	}
	
        if (object.isFieldSet(ContentObject.FIELD_CREATED_TIME))
        {
	        DateTime createdTime = object.getCreatedTime();
	        if (createdTime != null)
	            writeTag(outputStream, TAG_CREATED, createdTime.toString());
        }
	
        if (object.isFieldSet(ContentObject.FIELD_MODIFIED_TIME))
        {
	        DateTime modifiedTime = object.getModifiedTime();
	        if (modifiedTime != null)
	            writeTag(outputStream, TAG_MODIFIED, modifiedTime.toString());

    	}
    	
        if (object.isFieldSet(ContentObject.FIELD_ATTRIBUTES))
        {
	        Attributes attrs = object.getAttributes();
	        if (attrs != null)
	        {
	            writeTag(outputStream, TAG_ATTRIBUTES, true);
	            
	            if (attrs.isArchived())
	                writeTag(outputStream, TAG_A, false);
	            if (attrs.isDeletable())
	                writeTag(outputStream, TAG_D, false);
	            if (attrs.isHidden())
	                writeTag(outputStream, TAG_H, false);
	            if (attrs.isReadable())
	                writeTag(outputStream, TAG_R, false);
	            if (attrs.isSystem())
	                writeTag(outputStream, TAG_S, false);
	            if (attrs.isWritable())
	                writeTag(outputStream, TAG_W, false);
	            if (attrs.isExecutable())
	                writeTag(outputStream, TAG_X, false);
	            
	            writeTagEnd(outputStream, TAG_ATTRIBUTES);
	        }
    	}
        
        if (object.isFieldSet(ContentObject.FIELD_EXTENSIONS))
        {
	        Extension[] exts = object.getExtensions();
	        if ( (exts != null) && (exts.length > 0) )
	        {
	            for (int i = 0; i < exts.length; i++)
	            {
	                Extension ext = exts[i];
	                String[] extValues = ext.getValues();
	                
	                writeTag(outputStream, TAG_EXT, true);
	                writeTag(outputStream, TAG_XNAM, ext.getName());
	                if ( (extValues != null) && (extValues.length > 0) )
	                {
	                    for (int j = 0; j < extValues.length; j++)
	                        writeTag(outputStream, TAG_XVAL, extValues[j]);
	                }
	                writeTagEnd(outputStream, TAG_EXT);
	            }
	        }
        }
    }

    /* initializes the object with the extensions we've parsed so far. */
    private void setObjectExtensions()
    {
        if (inExtensions == null)
            return;
        
        //initialize the object with the extensions we've parsed so far
        if (inExtensions.size() > 0)
        {
            Extension[] exts = new Extension[inExtensions.size()];
            for (int i = 0; i < inExtensions.size(); i++)
                exts[i] = (Extension)inExtensions.elementAt(i);
            inObject.setExtensions(exts);
        }

        //cleanup
        inExtensions.removeAllElements();
        inExtensions = null;
    }
}
