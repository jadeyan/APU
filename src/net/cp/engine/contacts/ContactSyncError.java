/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.cp.engine.EngineSettings;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncError;

/**
 * Encapsulates all the data associated with an error that occurred while syncing contacts.
 *
 */
public class ContactSyncError extends SyncError
{

    /**
     * The id of the contact that caused the failure
     */
    public String contactId;

    /**
     * The maximum number of contacts we can handle
     */
    public int maxContacts = -1;

    /**
     *
     */
    public ContactSyncError()
    {
        contactId = null;

        syncType = StatusCodes.SYNC_CONTACT;

        mediaType = EngineSettings.MEDIA_TYPE_CONTACTS;
    }

    /**
     * Construct a new ContactSyncError from an InputStream.
     *
     * @param in The error info will be read from here
     * @return a new ContactSyncError
     * @throws IOException If the data could not be read properly from the stream.
     */
    public static ContactSyncError readFromStream(DataInputStream in) throws IOException
    {
        ContactSyncError error = new ContactSyncError();

        error.syncType = in.readInt();
        error.mediaType = in.readInt();
        error.operationId = in.readInt();
        error.targetDevice = in.readInt();
        error.errorCode = in.readInt();
        error.syncMLStatusCode = in.readInt();
        error.contactId = in.readUTF();
        error.maxContacts = in.readInt();

        return error;
    }

    /* (non-Javadoc)
     * @see net.cp.engine.SyncError#writeToStream(java.io.DataOutputStream)
     */
    public void writeToStream(DataOutputStream out) throws IOException
    {
        out.writeInt(syncType);
        out.writeInt(mediaType);
        out.writeInt(operationId);
        out.writeInt(targetDevice);
        out.writeInt(errorCode);
        out.writeInt(syncMLStatusCode);

        if(contactId == null)
            out.writeUTF("");
        else
            out.writeUTF(contactId);

        out.writeInt(maxContacts);
    }
}
