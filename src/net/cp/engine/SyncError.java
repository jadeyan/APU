/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * In the J2ME client, error strings were constructed directly in the engine using the Resources class.
 * This class was created to allow all references to Resources to be removed from the engine.
 * This class records all the information necessary to reconstruct the full context of the error,
 * which will allow the UI to supply the apropriate strings to the user when they view the log.
 * 
 */
public class SyncError
{
    /**
     * sync type. Something like StatusCodes.SYNC_CONTACT
     */
    public int syncType = -1;
    
    /**
     * "media type" of sync. Something like Settings.MEDIA_TYPE_CONTACTS
     */
    public int mediaType = -1;
    
    /**
     * something like StatusCodes.SYNC_ADD, StatusCodes.SYNC_DELETE
     */
    public int operationId = -1;
    
    /**
     * operation direction. Something like StatusCodes.SYNC_TO_PHONE, StatusCodes.SYNC_TO_SERVER
     */
    public int targetDevice = -1;
    
    /**
     * error code for an individual sync item. Something like StatusCodes.SYNC_OP_FAILED, StatusCodes.SYNC_OP_CONFLICT.
     * Also used to store the last sync status. Something like StatusCodes.SYNC_SUCCESS
     */
    public int errorCode = -1;
    
    /**
     * SyncML status code. Something like SyncML.STATUS_DEVICE_FULL, SyncML.STATUS_OPERATION_CANCELLED
     */
    public int syncMLStatusCode = -1;
    
    /**
     * Reads a SyncError from the supplied stream.
     *
     * @param in The stream to read the error from.
     * @return The decoded error.
     * @throws IOException if there was a problem reading from the stream.
     */
    public static SyncError readFromStream(DataInputStream in) throws IOException
    {
        SyncError error = new SyncError();
        
        error.syncType = in.readInt();
        error.mediaType = in.readInt();
        error.operationId = in.readInt();
        error.targetDevice = in.readInt();
        error.errorCode = in.readInt();
        error.syncMLStatusCode = in.readInt();
        
        return error;
    }
    
    /**
     * Writes this SyncError to the supplied output stream.
     * 
     * @param out The stream to write the error out to.
     * @throws IOException if there was a problem writing to the stream.
     */
    public void writeToStream(DataOutputStream out) throws IOException
    {
        out.writeInt(syncType);
        out.writeInt(mediaType);
        out.writeInt(operationId);
        out.writeInt(targetDevice);
        out.writeInt(errorCode);
        out.writeInt(syncMLStatusCode);
    }
    
}
