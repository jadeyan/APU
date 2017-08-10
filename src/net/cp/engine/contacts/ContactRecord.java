/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import net.cp.syncml.client.devinfo.ContentType;
import net.cp.syncml.client.store.*;

/**
 * An abstract class representing a contact and its associated vCard data.
 * 
 * @author Denis Evoy
 */
public abstract class ContactRecord implements Record
{
    /**
     * The record store containing the contact
     */
    protected ContactStore contactStore;

    /**
     * The vCard representation of the contact
     */
    protected byte[] vcardData;
    
    /**
     * The number of bytes of the vCard representation that have been read
     */
    protected int vcardBytesRead;


    /** 
     * Creates a new contact record associated with the specified record store.
     *  
     * @param store the store in which the contact resides. 
     */
    public ContactRecord(ContactStore store)
    {
        contactStore = store;
        
        vcardData = null;
        vcardBytesRead = 0;
    }

    
    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#close()
     */
    public void close()
    {
        vcardData = null;
        vcardBytesRead = 0;
    }
    

    /** 
     * Returns the vCard data of the contact.
     *  
     * @return the vCard representation of the contact. 
     * @throws StoreException if the vCard representation couldn't be retrieved.
     */
    protected abstract byte[] getVcardData()
        throws StoreException;
    
    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getChangeType()
     */
    public abstract int getChangeType();

    
    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getRecordStore()
     */
    public RecordStore getRecordStore()
    {
        return contactStore;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getContentType()
     */
    public ContentType getContentType()
    {
        //the content is defined by the store
        return contactStore.getContentType();
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getParentId()
     */
    public String getParentId()
    {
        //contacts do not have any parents
        return null;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getTargetId()
     */
    public String getTargetId()
    {
        //this method is only used for copy/move operations which are not supported
        return null;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getTargetParentId()
     */
    public String getTargetParentId()
    {
        //this method is only used for copy/move operations which are not supported
        return null;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#isFieldLevelReplace()
     */
    public boolean isFieldLevelReplace()
    {
        if (getChangeType() == Record.CHANGE_TYPE_REPLACE)
            return true;

        return false;
    }

    
    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getDataSize()
     */
    public long getDataSize() 
        throws StoreException
    {
        //load the vCard data if necessary
        if (vcardData == null)
            vcardData = getVcardData();

        return vcardData.length;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getData(byte[], int)
     */
    public int getData(byte[] buffer, int length) 
        throws StoreException
    {
        boolean finished = false;
        try
        {
            //load the vCard data if necessary
            if (vcardData == null)
                vcardData = getVcardData();
            
            //copy the required amount of data
            System.arraycopy(vcardData, vcardBytesRead, buffer, 0, length);
            vcardBytesRead += length;

            //check if we have finished reading all the data
            if (vcardBytesRead >= vcardData.length)
                finished = true;
            
            return length;
        }
        catch (Throwable e)
        {
            finished = true;
            throw new StoreException("Failed to read data for contact with local ID '" + getLocalId() + "'", e);
        }
        finally
        {
            //cleanup
            if (finished)
                vcardData = null;
        }
    }
}
