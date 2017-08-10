/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import net.cp.engine.contacts.ContactSyncError;

/**
 * A class encapsulating the sync counters for a particular type of media.
 */
public class SyncCounters
{
    /**
     * the media type for which the counters are maintained
     */
    public int mediaType;

    /**
     * the date when the counters were last reset
     */
    public long lastResetDate;

    /**
     * the number of syncs that have been performed
     */
    public int syncCount;

    /**
     * the date of the last sync that was performed
     */
    public long lastSyncDate;

    /**
     * the status (StatusCodes.SYNC_STATUS_SUCCESS) of the last sync that was performed
     */
    public short lastSyncStatus;

    /**
     * the error which caused the last sync to fail
     */
    public SyncError lastSyncError;

    /**
     * the anchor used during the last successful sync session
     */
    public String lastSyncAnchor;

    /**
     * the amount of data (in bytes) that has been received by the phone
     */
    public long inDataSize;

    /**
     * the number of items that have been added to the phone
     */
    public int inItemsAdded;

    /**
     * the number of items that have been replaced on the phone
     */
    public int inItemsReplaced;

    /**
     * the number of items that have been deleted from the phone
     */
    public int inItemsDeleted;

    /**
     * the number of items that have been copied on the phone
     */
    public int inItemsCopied;

    /**
     * the number of items that have been added to the phone
     */
    public int inItemsMoved;

    /**
     * the number of items that could not be synced to the phone
     */
    public int inItemsFailed;

    /**
     * the amount of data (in bytes) that has been sent to the server
     */
    public long outDataSize;

    /**
     * the number of items that have been added to the server
     */
    public int outItemsAdded;

    /**
     * the number of items that have been replaced on the server
     */
    public int outItemsReplaced;

    /**
     * the number of items that have been deleted from the server
     */
    public int outItemsDeleted;

    /**
     * the number of items that have been copied on the server
     */
    public int outItemsCopied;

    /**
     * the number of items that have been added to the server
     */
    public int outItemsMoved;

    /**
     * the number of items that could not be synced to the server
     */
    public int outItemsFailed;

    /**
     * the number of items were in conflict on the server
     */
    public int outItemsConflict;

    /**
     * Creates a new set of counters for the specified media type.
     *
     * @param counterMediaType the media type (Settings.MEDIA_TYPE_XXX) that the counters refer to.
     */
    public SyncCounters(int counterMediaType)
    {
        mediaType = counterMediaType;
        reset();
    }

    /** Resets all counters. */
    public void reset()
    {
        lastResetDate = System.currentTimeMillis();

        syncCount = 0;

        lastSyncDate = 0;
        lastSyncStatus = EngineSettings.SYNC_STATUS_NONE;
        lastSyncError = new ContactSyncError();
        lastSyncAnchor = "";

        inDataSize = 0;
        inItemsAdded = 0;
        inItemsReplaced = 0;
        inItemsDeleted = 0;
        inItemsCopied = 0;
        inItemsMoved = 0;
        inItemsFailed = 0;

        outDataSize = 0;
        outItemsAdded = 0;
        outItemsReplaced = 0;
        outItemsDeleted = 0;
        outItemsCopied = 0;
        outItemsMoved = 0;
        outItemsFailed = 0;
        outItemsConflict = 0;
    }

    /**
     * Adds the specified counter values to the counters.
     *
     * @param counters the counters whose values should be merged with these counters.
     */
    public void addCounters(SyncCounters counters)
    {
        //make sure the media types are the same
        if ((mediaType & counters.mediaType) != counters.mediaType)
            throw new IllegalArgumentException("Counter media types don't match");

        //always keep the maximum sync count
        if (counters.syncCount > syncCount)
            syncCount = counters.syncCount;

        //always keep the most recent last sync details
        if (counters.lastSyncDate > lastSyncDate)
        {
            lastSyncDate = counters.lastSyncDate;
            lastSyncStatus = counters.lastSyncStatus;

            if(mediaType == EngineSettings.MEDIA_TYPE_CONTACTS && !(counters.lastSyncError instanceof ContactSyncError))
                throw new IllegalArgumentException("lastSyncError is wrong type for media_type: " + mediaType);

            lastSyncError = counters.lastSyncError;
        }

        //the anchor may not be associated with the last sync date - so keep any non-empty anchors
        if ( (counters.lastSyncAnchor != null) && (counters.lastSyncAnchor.length() > 0) )
            lastSyncAnchor = counters.lastSyncAnchor;

        inDataSize += counters.inDataSize;
        inItemsAdded += counters.inItemsAdded;
        inItemsReplaced += counters.inItemsReplaced;
        inItemsDeleted += counters.inItemsDeleted;
        inItemsCopied += counters.inItemsCopied;
        inItemsMoved += counters.inItemsMoved;
        inItemsFailed += counters.inItemsFailed;

        outDataSize += counters.outDataSize;
        outItemsAdded += counters.outItemsAdded;
        outItemsReplaced += counters.outItemsReplaced;
        outItemsDeleted += counters.outItemsDeleted;
        outItemsCopied += counters.outItemsCopied;
        outItemsMoved += counters.outItemsMoved;
        outItemsFailed += counters.outItemsFailed;
        outItemsConflict += counters.outItemsConflict;
    }

    /**
     * Writes the counters to the specified data stream.
     *
     * @param stream the output stream that the counter data should be written to.
     * @throws IOException if the counter data couldn't be written.
     */
    public void writeCounters(DataOutputStream stream)
        throws IOException
    {
        stream.writeInt(mediaType);

        stream.writeLong(lastResetDate);

        stream.writeInt(syncCount);

        stream.writeLong(lastSyncDate);
        stream.writeByte(lastSyncStatus);

        lastSyncError.writeToStream(stream);

        stream.writeUTF(lastSyncAnchor);

        stream.writeLong(inDataSize);
        stream.writeInt(inItemsAdded);
        stream.writeInt(inItemsReplaced);
        stream.writeInt(inItemsDeleted);
        stream.writeInt(inItemsCopied);
        stream.writeInt(inItemsMoved);
        stream.writeInt(inItemsFailed);

        stream.writeLong(outDataSize);
        stream.writeInt(outItemsAdded);
        stream.writeInt(outItemsReplaced);
        stream.writeInt(outItemsDeleted);
        stream.writeInt(outItemsCopied);
        stream.writeInt(outItemsMoved);
        stream.writeInt(outItemsFailed);
        stream.writeInt(outItemsConflict);
    }

    /**
     * Reads the counters to the specified data stream.
     *
     * @param stream the input stream that the counter data should be read from.
     * @throws IOException if the counter data couldn't be read.
     */
    public void readCounters(DataInputStream stream)
        throws IOException
    {
        mediaType = stream.readInt();

        lastResetDate = stream.readLong();

        syncCount = stream.readInt();

        lastSyncDate = stream.readLong();
        lastSyncStatus = stream.readByte();

        if(mediaType == EngineSettings.MEDIA_TYPE_CONTACTS)
            lastSyncError = ContactSyncError.readFromStream(stream);
        else
            lastSyncError = SyncError.readFromStream(stream);

        lastSyncAnchor = stream.readUTF();

        inDataSize = stream.readLong();
        inItemsAdded = stream.readInt();
        inItemsReplaced = stream.readInt();
        inItemsDeleted = stream.readInt();
        inItemsCopied = stream.readInt();
        inItemsMoved = stream.readInt();
        inItemsFailed = stream.readInt();

        outDataSize = stream.readLong();
        outItemsAdded = stream.readInt();
        outItemsReplaced = stream.readInt();
        outItemsDeleted = stream.readInt();
        outItemsCopied = stream.readInt();
        outItemsMoved = stream.readInt();
        outItemsFailed = stream.readInt();
        outItemsConflict = stream.readInt();
    }
}
