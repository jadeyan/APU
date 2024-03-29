/**
 * Copyright � 2004-2010 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test.store;

import java.io.*;
import java.util.Vector;

import net.cp.mtk.common.CommonUtils;
import net.cp.syncml.client.store.*;
import net.cp.syncml.client.util.content.*;
import net.cp.syncml.client.util.wbxml.*;
import net.cp.syncml.client.devinfo.*;


/**
 * A class implementing a record containing file or folder.
 *
 * @author Herve Fayolle
 */
public class DesktopFileRecord implements Record
{
	private DesktopFileStore itsStore;		    //the store containing the record
    private File itsFile;                       //the file represented by the record
	private String itsLocalId;				    //the ID of the record
	private String itsParentId;				    //the ID of the parent of the record
    private String itsMemCardId;                //the memory card id of the file 
    private int itsChangeType;                  //the type of change
    private boolean itsSendContent;             //indicate whether or not the content or the ash on the content should be used

	private String itsTargetId;				    //the ID of the record target
	private String itsParentTargetId;		    //the ID of the parent of record target
	private ContentType itsContentType;		    //the content type of the record
	private File itsWbxmlFile;                  //the file containing the WBXML encoding of the file/folder
    private int itsWbxmlBytesRead;              //the number of bytes that we have read from the WBXML encoded file/folder
	private InputStream itsWbxmlInputStream;    //the input stream used to read the the WBXML encoded file
    private int itsReplacedFields;		        //indicate the fields that should be updated during a field level replace, copy or move
  

    public DesktopFileRecord(DesktopFileStore recordStore, File file, String localId, String parentLocalId, String memoryCardId, int changeType, boolean isDirectory)
    {
    	this(recordStore, file, localId, parentLocalId, memoryCardId, changeType, (! recordStore.useFileRepresentation()), isDirectory);
    }
    
    public DesktopFileRecord(DesktopFileStore recordStore, File file, String localId, String parentLocalId, String memoryCardId, int changeType, boolean sendContent, boolean isDirectory)
	{
		itsStore = recordStore;
		itsChangeType = changeType;
		setFile(file, isDirectory);
		itsLocalId = localId;
		itsParentId = parentLocalId;
		itsMemCardId = memoryCardId;
		itsSendContent = sendContent;
	}
	
	public void close()
	{
	    Utils.streamClose(itsWbxmlInputStream);
	    itsWbxmlInputStream = null;
        itsWbxmlBytesRead = 0;
        
        //cleanup temporary files
        if ( (itsWbxmlFile != null) && (itsWbxmlFile.exists()) )
            itsWbxmlFile.delete();
        itsWbxmlFile = null;
	}

	
	public RecordStore getRecordStore()
	{
		return itsStore;
	}
	
	public String getLocalId()
	{
		return itsLocalId;
	}
	
	public String getParentId()
	{
		return itsParentId;
	}
	
	public void setParentId(String parentId)
	{
		itsParentId = parentId;
	}
	
	public String getTargetId()
	{
		return itsTargetId;
	}

    public void setTargetId(String parentId)
    {
        itsTargetId = parentId;
    }
	
	public String getTargetParentId()
	{
		return itsParentTargetId;
	}
	
	public void setParentTargetId(String parentId)
	{
		itsParentTargetId = parentId;
	}
	
	public ContentType getContentType()
	{
		return itsContentType;
	}
	
	public int getChangeType()
	{
		return itsChangeType;
	}
	
	public void setChangeType(int type)
	{
		itsChangeType = type;
	}
    
    public void setFields(int fields)
    {
        itsReplacedFields = fields;
    }
    
    public boolean isFieldLevelReplace()
    {
        return (itsReplacedFields != 0);
    }

    public File getFile()
    {
        return itsFile;
    }
    
    public void setFile(File file, boolean isDirectory)
    {
        itsFile = file;
        
        if (itsFile.exists())
        {
            if (itsFile.isDirectory())
                itsContentType = ContentCodepage.CT_CP_FOLDER_WBXML; 
            else
                itsContentType = ContentCodepage.CT_CP_FILE_WBXML; 
        }
        else
        {
            if (isDirectory)
                itsContentType = ContentCodepage.CT_CP_FOLDER_WBXML; 
            else
                itsContentType = ContentCodepage.CT_CP_FILE_WBXML;
        }
    }
	
    public long getDataSize()
        throws StoreException
    {
        if (itsWbxmlFile == null)
            getRecordData();
        
        return itsWbxmlFile.length();
    }
    
    public int getData(byte[] buffer, int length) 
        throws StoreException
    {
        if (itsWbxmlFile == null)
            getRecordData();
        
        try
        {
            //read the WBXML encoded data
            int bytesRead = itsWbxmlInputStream.read(buffer, 0, length);
            itsWbxmlBytesRead += bytesRead;
            
            return bytesRead;
        }
        catch (Throwable e)
        {
            throw new StoreException("Failed to read record data", e);
        }
    }
		
    private void getRecordData()
        throws StoreException
    {
        //create the WBXML encoding of the file/folder
        if (itsFile.isDirectory())
            getFolderData();
        else        
            getFileData();
        
        //close any already opened stream
        Utils.streamClose(itsWbxmlInputStream);
        itsWbxmlBytesRead = 0;
        itsWbxmlInputStream = null;

        try
        {
            //open an input stream to access the WBXML encoding
            itsWbxmlInputStream = new FileInputStream(itsWbxmlFile);
        }
        catch (Throwable e)
        {
            throw new StoreException("Failed to access WBXML encoding of record", e);
        }
	}
	
	private void getFolderData()
		throws StoreException
	{
        if (! itsContentType.equals(ContentCodepage.CT_CP_FOLDER_WBXML))
            throw new StoreException("Unsupported folder content type '" + itsContentType + "'");

        FileOutputStream wbxmlStream = null;
		try
		{
            //create the folder object
            ContentFolder folder = new ContentFolder(itsFile.getName());
            folder.setFields(itsReplacedFields);
            
            if (folder.isFieldSet(ContentObject.FIELD_ATTRIBUTES))
            	folder.setAttributes( new Attributes(itsFile.isHidden(), false, false, itsFile.canWrite(), itsFile.canWrite(), itsFile.canRead(), false) );
            
            if (folder.isFieldSet(ContentObject.FIELD_MODIFIED_TIME))
            	folder.setModifiedTime( Utils.toDateTime(itsFile.lastModified()) );
            
            //set extensions
            if (itsMemCardId != null && itsMemCardId.length() > 0)
            {
            	Extension extMemCardId = new Extension(ContentFile.EXT_CP_MEM_CARD_ID, itsMemCardId);
           		folder.setExtensions( new Extension[] { extMemCardId } );
            }

            if (itsStore.getLogger() != null)
                itsStore.getLogger().info("DesktopFileRecord: Building folder information as WBXML opaque data");
            
            //create a temporary file to hold the WBXML encoding of the folder object
            if (itsWbxmlFile != null)
                itsWbxmlFile.delete();
            itsWbxmlFile = File.createTempFile("syncml_out_file_", null);
            itsWbxmlFile.deleteOnExit();
            wbxmlStream = new FileOutputStream(itsWbxmlFile);
		
            //write the folder information
            ContentCodepage cpContent = new ContentCodepage(itsStore.getLogger(), null);
            Wbxml.writeHeader(wbxmlStream, Wbxml.VERSION_1_2, Wbxml.CHARSET_UTF8, null);
            cpContent.writeFolder(wbxmlStream, folder);
		}
        catch(Throwable e)
        {
            throw new StoreException("Failed to write folder information for '" + itsFile.getPath() + "'", e);
        }
        finally
        {
            Utils.streamClose(wbxmlStream);
		}
	}
	
	private void getFileData()
		throws StoreException
	{
        if (! itsContentType.equals(ContentCodepage.CT_CP_FILE_WBXML))
            throw new StoreException("Unsupported folder content type '" + itsContentType + "'");
		
        FileInputStream fileStream = null;
        FileOutputStream wbxmlStream = null;
		try
		{
			Vector extensions = new Vector(); 
			
	        //create the file object
		    ContentFile file = new ContentFile(itsFile.getName());
		    file.setFields(itsReplacedFields);
		    
            if (file.isFieldSet(ContentObject.FIELD_ATTRIBUTES))
		    	file.setAttributes( new Attributes(itsFile.isHidden(), false, false, itsFile.canWrite(), itsFile.canWrite(), itsFile.canRead(), false) );
		    
            if (file.isFieldSet(ContentObject.FIELD_MODIFIED_TIME))
		    	file.setModifiedTime( Utils.toDateTime(itsFile.lastModified()) );
		    
		    //check if we should include the file content 
            if (file.isFieldSet(ContentObject.FIELD_FILE_CONTENTS))
		    {
		    	file.setSize( itsFile.length() );

	            //if not including content, we include the file hash instead
	            if (! itsSendContent)
	            {
					String hash = CommonUtils.base64Encode( Utils.md5HashFile(itsFile) );
					Extension extHash = new Extension(ContentFile.EXT_CP_HASH_CONTENT, hash);
					extensions.add(extHash);
				}
		    }
            
            //set memory card ID (if any)
            if ( (itsMemCardId != null) && (itsMemCardId.length() > 0) )
            {
            	Extension extMemCardId = new Extension(ContentFile.EXT_CP_MEM_CARD_ID, itsMemCardId);
				extensions.add(extMemCardId);
            }
            
            //set all extensions
            if (extensions.size() > 0)
            {
            	Extension[] exts = new Extension[extensions.size()];
            	for (int i = 0; i < extensions.size(); i++)
            		exts[i] = (Extension)extensions.elementAt(i); 
            	
            	file.setExtensions(exts);
			}
			            
            if (itsStore.getLogger() != null)
                itsStore.getLogger().info("DesktopFileRecord: Building file information as WBXML opaque data");
            
            //create a temporary file to hold the WBXML encoding of the file object
            if (itsWbxmlFile != null)
                itsWbxmlFile.delete();
            itsWbxmlFile = File.createTempFile("syncml_out_file_", null);
            itsWbxmlFile.deleteOnExit();
            wbxmlStream = new FileOutputStream(itsWbxmlFile);
            
            //write the initial file information
            ContentCodepage cpContent = new ContentCodepage(itsStore.getLogger(), null);
            Wbxml.writeHeader(wbxmlStream, Wbxml.VERSION_1_2, Wbxml.CHARSET_UTF8, null);
            cpContent.writeFileBegin(wbxmlStream, file, itsSendContent, itsFile.length());
				
            //write the file content if necessary
            if ( (itsSendContent) && (file.isFieldSet(ContentObject.FIELD_FILE_CONTENTS)) )
			{
                int dataRead;
                byte[] buffer = new byte[10240];
                fileStream = new FileInputStream(itsFile);
                while ((dataRead = fileStream.read(buffer)) != -1)
                    cpContent.writeFileData(wbxmlStream, file, buffer, dataRead);
			}

            //write the final file information
            cpContent.writeFileEnd(wbxmlStream, file, itsSendContent);
		}
		catch(Throwable e)
		{
			throw new StoreException("Failed to write file information for '" + itsFile.getPath() + "'", e);
		}
		finally
		{
            Utils.streamClose(fileStream);
            Utils.streamClose(wbxmlStream);
		}
	}
}

