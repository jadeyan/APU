/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test.store;


import java.io.*;

import net.cp.syncml.client.devinfo.ContentType;
import net.cp.syncml.client.store.*;


/**
 * A class implementing a record in VCard format.
 *
 * @author Denis Evoy
 */
public class DesktopVcard implements Record
{
    private DesktopVcardStore recordStore;		//the store containing the record 							
    private int changeType;                     //indicates whether the vCard has been added, deleted, or replaced
    private File vcardFile;                     //the file containing the original vCard
    
    private File tempVcardFile;                 //the temporary file containing the modified vCard
    private InputStream tempVcardStream;        //the input stream used to read the vCard file
    private int bytesRead;                      //the number of bytes that we have read from the vCard file



    /**
     * Creates a new vCard record with the specified name and associated with the specified record store.
     * 
     * @param store     the record store associated with the record.
     * @param file      the file containing the vCard file.
     */
    public DesktopVcard(DesktopVcardStore store, File file)
    {
        this(store, file, Record.CHANGE_TYPE_ADD);
    }
    
    /**
     * Creates a new vCard record with the specified name and change type and associated with the specified record store.
     * 
     * @param store             the record store associated with the record.
     * @param file              the file containing the vCard file.
     * @param vcardChangeType   indicates the change-type of this record.
     */
    public DesktopVcard(DesktopVcardStore store, File file, int vcardChangeType)
    {
    	recordStore = store;
    	vcardFile = file;
        changeType = vcardChangeType;
    }

    
    public void close()
    {
        Utils.streamClose(tempVcardStream);
        tempVcardStream = null;
        bytesRead = 0;
        
        //cleanup temporary files
        if ( (tempVcardFile != null) && (tempVcardFile.exists()) )
            tempVcardFile.delete();
        tempVcardFile = null;
    }

    
    public RecordStore getRecordStore()
    {
    	return recordStore;
    }
    
    public int getChangeType()
    {
        return changeType;
    }
    
    public boolean isFieldLevelReplace()
    {
    	//field level replace is not supported for contacts
    	return false;
    }

    public ContentType getContentType()
    {
        return ContentType.CT_CONTACT_2_1;
    }

    public String getLocalId()
    {
        return vcardFile.getName();
    }

    public String getParentId()
    {
        return null;
    }
    
    public String getTargetId()
    {
    	return null;
    }
    
    public String getTargetParentId()
    {
    	return null;
    }
    
    public long getDataSize()
        throws StoreException
    {
        if (tempVcardFile == null)
            setupTempVcard();
        
        return tempVcardFile.length();
    }
    
    public int getData(byte[] buffer, int length) 
        throws StoreException
    {
        if (tempVcardFile == null)
            setupTempVcard();
        
        try
        {
            //open the input stream if necessary
            if (tempVcardStream == null)
                tempVcardStream = new FileInputStream(tempVcardFile);

            //read the data
            int readCount = tempVcardStream.read(buffer, 0, length);
            if (readCount > 0)
                bytesRead += readCount;
            
            return readCount;
        }
        catch (Throwable e)
        {
            throw new StoreException("Failed to read vCard data", e);
        }
    }
    
    /* Parses the vCard and creates a temporary copy containing any additional fields we need. */
    private void setupTempVcard()
        throws StoreException
    {
        //create a temporary copy of the vCard
        BufferedWriter bufferedWriter = null;
        BufferedReader bufferedReader = null;
        try
        {
            //create a temporary file
            if ( (tempVcardFile != null) && (tempVcardFile.exists()) )
                tempVcardFile.delete();
            tempVcardFile = null;
            tempVcardFile = File.createTempFile(vcardFile.getName(), "tmp");
            tempVcardFile.deleteOnExit();
            
            //parse the vCard
            String line;
            boolean updateRevision = recordStore.isUpdateRevision();
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tempVcardFile), "UTF-8"));
            bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(vcardFile), "UTF-8"));
            while ((line = bufferedReader.readLine()) != null)
            {
                //skip the REV field if required - we'll add it the end
            	if (updateRevision)
            	{
	                String normalizedLine = line.trim().toUpperCase();
	                if ( (normalizedLine.startsWith("REV:")) || (normalizedLine.startsWith("END:VCARD")) )
	                    continue;
            	}
                
            	recordStore.logDebug(line);
            	bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            
            //add an updated REV field if required
            if (updateRevision)
            {
	            String lastModifiedDate = Utils.toDateTime().toString();
	            line = "REV:" + lastModifiedDate;
                recordStore.logDebug(line);
	            bufferedWriter.write(line);
	            bufferedWriter.newLine();
	            
                line = "END:VCARD";
                recordStore.logDebug(line);
	            bufferedWriter.write(line);
            }
        }
        catch (Throwable e)
        {
            throw new StoreException("Failed to setup temporary vCard file", e);
        }
        finally
        {
            Utils.readerClose(bufferedReader);
            Utils.writerClose(bufferedWriter);
        }
    }
}
