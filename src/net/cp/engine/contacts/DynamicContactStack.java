/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import android.util.Log;
import net.cp.syncml.client.store.*;
import net.cp.syncml.client.util.ConsumableStack;

/**
 * A class representing a stack where the next record to be consumed is determined dynamically. <br/><br/>
 *
 * This class is intended for use in cases where the client updates to be sent to the SyncML server
 * are too large to be held in memory all at once. Instead, this class allows the updates to be determined
 * only as they are required. <br/><br/>
 *
 * To do this, the "ContactStore" object specified in the constructor of this class must implement
 * the "getNextRecord()" method.
 *
 * @author Denis Evoy
 */
public class DynamicContactStack extends ConsumableStack
{
    private static final long serialVersionUID = 2034406475412557468L;

    /**
     * the record store containing the contacts
     */
    protected ContactStore contactStore;

    /**
     * indicates whether or not only changed records should be returned
     */
    protected boolean changesOnly;

    /**
     * the total number of contacts present (if syncing all contacts)
     */
    protected int contactCount;

    /**
     * the total number of changes present (if syncing changes)
     */
    protected int contactChanges;


    /**
     * Initializes fields.
     *
     * @param syncContactStore
     * @param syncChangesOnly
     * @param totalContacts
     * @param totalChanges
     */
    public DynamicContactStack(ContactStore syncContactStore, boolean syncChangesOnly, int totalContacts, int totalChanges)
    {
        contactStore = syncContactStore;
        contactCount = totalContacts;
        contactChanges = totalChanges;

        setChangesOnly(syncChangesOnly);
    }


    /** Returns TRUE if the stack will return changed contacts, or FALSE if it will returns all contacts. */
    public boolean getChangesOnly()
    {
        return changesOnly;
    }

    /** Sets whether or not the stack should return changed contacts or all contacts. */
    public void setChangesOnly(boolean syncChangesOnly)
    {
        changesOnly = syncChangesOnly;
    }


    /* (non-Javadoc)
     * @see java.util.Vector#size()
     */
    public int size()
    {
        return (changesOnly) ? contactChanges : contactCount;
    }


    /* (non-Javadoc)
     * @see java.util.Stack#empty()
     */
    public boolean empty()
    {
    	
		return (size() <= 0);
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.util.ConsumableStack#consume()
     */
    public Object consume()
    {
        //nothing more to do if the stack is empty
        if (empty())
        {
            return null;
        }
        
        //decrement the number of remaining records in the stack
        if (changesOnly)
            contactChanges--;
        else
            contactCount--;

        //try to get the next record
        return getNextRecord();
    }


    /** Returns the next record or null if there is none. */
    protected Record getNextRecord()
    {
        return contactStore.getNextRecord(changesOnly);
    }
}
