/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.store;


import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.devinfo.*;


/**
 * A class representing the capabilities of a local record store. <br/><br/>
 * 
 * These capabilities define the content types handled by the record store along with any 
 * limitations (e.g. memory size, etc) the store might have.
 *
 * @see RecordStore#getCapabilities()
 * 
 * @author Denis Evoy
 */
public class RecordStoreCapabilities
{
    private int maxGuid;                                //the maximum allowed GUID size (in bytes)
    private long maxFreeMemory;                         //the maximum amount of free memory available (in bytes)
    private long maxRecordCount;                        //the maximum number of records that can be stored
    private long maxSize;                               //the maximum size of a record that can be stored
    private boolean sharedMemory;                       //indicates if the stores' memory is shared
    private ContentType ctTxPreferred;                  //the content type the record store prefers to receive
    private ContentType ctRxPreferred;                  //the content type the record store prefers to transmit
    private ContentType[] ctTxSupported;                //the content types the record store can receive
    private ContentType[] ctRxSupported;                //the content types the record store can transmit
    private ContentTypeCapabilities[] ctCaps;           //the content types supported by the record store
    private int[] syncTypes;                            //the types of sync supported by the record store
    private boolean ctHierarchicalSyncSupported;        //indicates if the store supports syncing hierarchical objects

    
    /**
     * Creates a new set of capabilities with the specified preferred content types and supported sync types.
     * 
     * @param contentTypeCaps           the content type capabilities supported by the record store. Must not be null or empty.
     * @param preferredTxContentType    the content type the record store prefers to transmit. Must not be null.
     * @param preferredRxContentType    the content type the record store prefers to receive. Must not be null.
     * @param supportedSyncTypes        the sync types supported by the record store. Must not be null or empty.
     * @param maxGuidSize               the maximum allowed GUID size (in bytes). Must be non-zero and positive.
     * @param hierarchicalSyncSupported indicates if the record store supports syncing hierarchical objects.
     */
    public RecordStoreCapabilities(ContentTypeCapabilities[] contentTypeCaps, ContentType preferredTxContentType, ContentType preferredRxContentType, int[] supportedSyncTypes, int maxGuidSize, boolean hierarchicalSyncSupported)
    {
        this(contentTypeCaps, preferredTxContentType, preferredRxContentType, supportedSyncTypes, maxGuidSize, null, null, 0, 0, 0, false, hierarchicalSyncSupported);
    }

    /**
     * Creates a new set of capabilities with the specified content types and supported sync types.
     * 
     * @param contentTypeCaps           the content type capabilities supported by the record store. Must not be null or empty.
     * @param preferredTxContentType    the content type the record store prefers to transmit. Must not be null.
     * @param preferredRxContentType    the content type the record store prefers to receive. Must not be null.
     * @param supportedSyncTypes        the sync types supported by the record store. Must not be null or empty.
     * @param maxGuidSize               the maximum allowed GUID size (in bytes). Must be non-zero and positive.
     * @param supportedTxContentTypes   the content types that the record store can transmit. May be null or empty.
     * @param supportedRxContentTypes   the content types that the record store can receive. May be null or empty.
     * @param hierarchicalSyncSupported indicates if the record store supports syncing hierarchical objects.
     */
    public RecordStoreCapabilities(ContentTypeCapabilities[] contentTypeCaps, ContentType preferredTxContentType, ContentType preferredRxContentType, int[] supportedSyncTypes, int maxGuidSize, ContentType[] supportedTxContentTypes, ContentType[] supportedRxContentTypes, boolean hierarchicalSyncSupported)
    {
        this(contentTypeCaps, preferredTxContentType, preferredRxContentType, supportedSyncTypes, maxGuidSize, supportedTxContentTypes, supportedRxContentTypes, 0, 0, 0, false, hierarchicalSyncSupported);
    }

    /**
     * Creates a new set of capabilities with the specified content types, supported sync types and memory limitations.
     * 
     * @param contentTypeCaps           the content type capabilities supported by the record store. Must not be null or empty.
     * @param preferredTxContentType    the content type that the record store prefers to transmit. Must not be null.
     * @param preferredRxContentType    the content type that the record store prefers to receive. Must not be null.
     * @param supportedSyncTypes        the sync types supported by the record store. Must not be null or empty.
     * @param maxGuidSize               the maximum allowed GUID size (in bytes). Must be non-zero and positive.
     * @param supportedTxContentTypes   the content types that the record store can transmit. May be null or empty.
     * @param supportedRxContentTypes   the content types that the record store can receive. May be null or empty.
     * @param maxFreeMem                the maximum amount of free memory available (in bytes). Must be zero or positive.
     * @param maxRecords                the maximum number of records that can be stored. Must be zero or positive.
     * @param maxRecordSize             the maximum size of a record (in bytes) that can be stored. Must be zero or positive.
     * @param sharedMem                 set to <code>true</code> if the record stores memory is shared.
     * @param hierarchicalSyncSupported indicates if the record store supports syncing hierarchical objects.
     */
    public RecordStoreCapabilities(ContentTypeCapabilities[] contentTypeCaps, ContentType preferredTxContentType, ContentType preferredRxContentType, int[] supportedSyncTypes, int maxGuidSize, ContentType[] supportedTxContentTypes, ContentType[] supportedRxContentTypes, long maxFreeMem, long maxRecords, long maxRecordSize, boolean sharedMem, boolean hierarchicalSyncSupported)
    {
        if (contentTypeCaps == null)
            throw new IllegalArgumentException("no content type capabilities specified");
        if (preferredTxContentType == null)
            throw new IllegalArgumentException("no preferred transmitted content type specified");
        if (preferredRxContentType == null)
            throw new IllegalArgumentException("no preferred received content type specified");
        if ( (supportedSyncTypes == null) || (supportedSyncTypes.length <= 0) )
            throw new IllegalArgumentException("no supported sync types specified");
        if (maxGuidSize <= 0)
            throw new IllegalArgumentException("invalid max GUID size specified: " + maxGuidSize);
        if (maxFreeMem < 0)
            throw new IllegalArgumentException("invalid max free memory size specified: " + maxFreeMem);
        if (maxRecords < 0)
            throw new IllegalArgumentException("invalid max record count specified: " + maxRecords);
        if (maxRecordSize < 0)
            throw new IllegalArgumentException("invalid max record size specified: " + maxRecordSize);

        ctCaps = contentTypeCaps;
        ctTxPreferred = preferredTxContentType;
        ctRxPreferred = preferredRxContentType;
        ctTxSupported = supportedTxContentTypes;
        ctRxSupported = supportedRxContentTypes;
        syncTypes = supportedSyncTypes;
        maxGuid = maxGuidSize;
        maxFreeMemory = maxFreeMem;
        maxRecordCount = maxRecords;
        maxSize = maxRecordSize;
        sharedMemory = sharedMem;
        ctHierarchicalSyncSupported = hierarchicalSyncSupported;
    }

    
    /**
     * Returns the content type that the record store prefers to receive.
     * 
     * @return The preferred content type. Will not be null or empty.
     */
    public ContentType getPreferredReceivedContentType()
    {
        return ctRxPreferred;
    }

    /**
     * Returns the content type that the record store prefers to transmit.
     * 
     * @return The preferred content type. Will not be null or empty.
     */
    public ContentType getPreferredTransmittedContentType()
    {
        return ctTxPreferred;
    }

    
    /**
     * Returns the content types that the record store can receive.
     * 
     * @return The supported content types. May be null or empty.
     */
    public ContentType[] getSupportedReceivedContentTypes()
    {
        return ctRxSupported;
    }

    /**
     * Returns the content types that the record store can transmit.
     * 
     * @return The supported content types. May be null or empty.
     */
    public ContentType[] getSupportedTransmittedContentTypes()
    {
        return ctTxSupported;
    }

    
    /**
     * Returns the content types and associated properties that the record store supports.
     * 
     * @return The content type capabilities. Will not be null or empty.
     */
    public ContentTypeCapabilities[] getContentTypeCapabilities()
    {
        return ctCaps;
    }

    
    /**
     * Returns the {@link SyncML#SYNC_TYPE_ONE_WAY_CLIENT sync types} that the record store supports.
     * 
     * @return The supported sync types. Will not be null or empty.
     */
    public int[] getSupportedSyncTypes()
    {
        return syncTypes;
    }

    
    /**
     * Returns the maximum allowed size (in bytes) of a GUID in the record store.
     * 
     * @return The maximum allowed GUID size. Will be non-zero and positive.
     */
    public int getMaxGuidSize()
    {
        return maxGuid;
    }

    /**
     * Returns the maximum amount of free memory available (in bytes) in the record store.
     * 
     * @return The maximum amount of free memory available or 0 if not set. Will be zero or positive.
     */
    public long getMaxFreeMemory()
    {
        return maxFreeMemory;
    }

    /**
     * Returns the maximum size of a record (in bytes) that can be stored.
     * 
     * @return The maximum size of a record (in bytes) that can be stored. Will be zero or positive.
     */
    public long getMaxRecordCount()
    {
        return maxRecordCount;
    }

    /**
     * Returns the maximum number of records that can be stored in the record store.
     * 
     * @return The maximum number of records that can be stored or 0 if not set. Will be zero or positive.
     */
    public long getMaxRecordSize()
    {
        return maxSize;
    }

    /**
     * Returns whether or not the record stores' memory is shared with other stores.
     * 
     * @return <code>true</code> if the record stores' memory is shared.
     */
    public boolean isSharedMem()
    {
        return sharedMemory;
    }

    /**
     * Returns whether or not the record store supports syncing hierarchical objects.
     * 
     * @return <code>true</code> if the record store supports syncing hierarchical objects.
     */
    public boolean isHierarchicalSyncSupported()
    {
        return ctHierarchicalSyncSupported;
    }
}
