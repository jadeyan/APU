/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import net.cp.syncml.client.store.StoreException;

/**
 * Class encapsulating the sync state of a contact where the PIM 'UID' field is supported. <br/><br/>
 * 
 * The 'UID' field allows to easily find a PIM entry from it's unique ID (UID). The UID is part of
 * the data stored in RMS so the application can match a contact with the sync state information.<br/><br/>
 * 
 * The PIM entry modification detection is implemented by storing MD5 hash of the vCard data in RMS
 * and comparing this hash with the hash of entries in the PIM.
 * 
 * Also note that we the local ID contains the RMS id of the corresponding entry so we  that we have
 * a direct mapping between the two. The local id is actually made of <RMS id>-<unique> where unique is
 * a unique string made up at RMS creation time and stored in the RMS record. This guarantees to
 * generate unique local id even when the RMS is wiped out and rebuilt, as RMS id values are
 * re-used when rebuilding.
 * 
 * @author Herve Fayolle
 */

public class UidContactRecord extends ContactRecord
{
    /**
     * UID identifying the contact in PIM
     */
	public String uid;
    
	/**
	 * Indicate how the record has changed since the last sync
	 */
	public byte changeType;
    
	/**
	 * RMS ID of the record where the sync state is stored
	 */
	public int rmsId;
	
	/**
	 * calculated version for PIM contact, organized with its contains raw contacts _ID
	 * i.e. "ID1,ID2,...IDn"
	 */
	public String version;
	
    /** Creates a new sync state record for a contact associated with the specified record store. */
    public UidContactRecord(ContactStore store)
    {
        super(store);
        
        uid = null;
        rmsId = 0;
        changeType = 0;
        
        version = null;
    }

    
    /* (non-Javadoc)
     * @see net.cp.engine.contacts.ContactRecord#close()
     */
    public void close()
    {
        uid = null;
        rmsId = 0;
        changeType = 0;

        version= null;
        super.close();
    }

    
    /* (non-Javadoc)
     * @see net.cp.syncml.client.store.Record#getLocalId()
     */
    public String getLocalId()
    {
        return ((UidContactStateManager)contactStore.stateManager).getLocalId(rmsId);
    }

    /* (non-Javadoc)
     * @see net.cp.engine.contacts.ContactRecord#getChangeType()
     */
    public int getChangeType()
    {
        return changeType;
    }
    
    /* (non-Javadoc)
     * @see net.cp.engine.contacts.ContactRecord#getVcardData()
     */
    protected byte[] getVcardData()
        throws StoreException
    {
        //use the sync manager to read the data
        UidContactStateManager stateManager = (UidContactStateManager)contactStore.getStateManager();
        return stateManager.getVCardByUID(uid);
    }
}
