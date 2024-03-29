/**
 * Copyright � 2004-2010 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test.store;


import java.util.*;
import java.io.*;

import net.cp.mtk.common.CommonUtils;
import net.cp.syncml.client.*;
import net.cp.syncml.client.devinfo.*;
import net.cp.syncml.client.store.*;
import net.cp.syncml.client.util.*;


/**
 * A class implementing a record store containing contacts in VCard format.
 *
 * @author Denis Evoy
 */
public class DesktopVcardStore implements RecordStore
{
    private static final String PROP_SERVER_URI =                   "./address_book";
    private static final String PROP_CLIENT_URI =                   "contacts";
    private static final String PROP_DISPLAY_NAME =                 "DesktopContacts";
    
    private static final int[] SYNC_CAP =                           { SyncML.SYNC_TYPE_TWO_WAY, SyncML.SYNC_TYPE_TWO_WAY_SLOW, SyncML.SYNC_TYPE_ONE_WAY_CLIENT, SyncML.SYNC_TYPE_REFRESH_CLIENT, SyncML.SYNC_TYPE_ONE_WAY_SERVER, SyncML.SYNC_TYPE_REFRESH_SERVER, SyncML.SYNC_TYPE_SERVER_ALERTED };
    
    private static final ContentType CT_CONTACT =                   ContentType.CT_CONTACT_2_1;
    private static final ContentTypeCapabilities CT_CAP_CONTACT =   new ContentTypeCapabilities(CT_CONTACT, null, false);
    private static final ContentTypeCapabilities[] CT_CAPS =        { CT_CAP_CONTACT };
    
    private static final RecordStoreCapabilities STORE_CAP =        new RecordStoreCapabilities(CT_CAPS, CT_CONTACT, CT_CONTACT, SYNC_CAP, 255, false);
    
    
    private File vcardDirectory;                        //the name of the directory containing the vCard files
    private Logger logger;                              //the logger to use
    private String conflictResolution;                  //the conflict resolution for this store
    private int syncType;                               //the type of sync being performed
    private boolean updateRevision;                     //indicates whether or not the vCard REV field should be updated 
    private int inErrorStatusCode;                      //indicates the SyncML status code to generate during an incoming server operation
    private String inErrorStatusData;                   //indicates the additional status data (e.g. reason code) to generate during an incoming server operation

    private String lastAnchor;                          //the anchor of the last successful sync session
    private String nextAnchor;                          //the anchor of the current sync session
    private File vcardStateFile;                        //the name of the file containing the serialized record state at the last sync 
    private ConsumableStack allRecords;                 //contains references to all records in currently in the store 
	private ConsumableStack changedRecords;             //contains references to changed records available for sync'ing 
    private SyncState lastSyncState;                    //the state of the records at the last sync
    private SyncState currentState;                     //the current state of the records
    private String[] metaInfoExtensions;                //the EMI extensions sent by the server

    private File inTmpFile;                             //the temporary file used to cache incoming Add/Replace data
    private OutputStream inTmpOutputStream;             //the output stream used to cache incoming Add/Replace data to a temporary file
    private File inReplaceFile;                         //the file that is to be replaced when performing an incoming Replace request
    private long beginChangelogTime;                    //initial time used to calculate ClientChangelogTime

    
    public DesktopVcardStore(String pathName, Logger syncLogger, String conflictRes, int defaultSyncType, boolean updateRev, int errorStatusCode, String errorStatusData)
    {
    	vcardDirectory = new File(pathName);
        if (! vcardDirectory.isDirectory())
            throw new IllegalArgumentException("The specified pathname is not a valid directory: " + pathName);
        
        vcardStateFile = new File(pathName + File.separator + ".last-sync-state");
        conflictResolution = conflictRes;
      	logger = syncLogger;
        syncType = defaultSyncType;
        updateRevision = updateRev;
        inErrorStatusCode = errorStatusCode;
        inErrorStatusData = errorStatusData;

        Date now = new Date();
        beginChangelogTime = now.getTime();
    }

    
    public String getServerURI()
    {
        return PROP_SERVER_URI;
    }
    
    public String getClientURI()
    {
        return PROP_CLIENT_URI;
    }

    public String getDisplayName()
    {
        return PROP_DISPLAY_NAME;
    }

    public RecordStoreCapabilities getCapabilities()
    {
        return STORE_CAP;
    }

    public int getSyncType() 
    {
        return syncType;
    }
    
    public void setSyncType(int type)
    {
        syncType = type;
        
        if (syncType == SyncML.SYNC_TYPE_REFRESH_SERVER)
        {
            //remove all local vCards
            File[] vcardFiles = vcardDirectory.listFiles();
            if ( (vcardFiles != null) && (vcardFiles.length > 0) )
            {
                for (int i = 0; i < vcardFiles.length; i++)
                {
                    //determine the name of the file and ignore anything that isn't a vCard
                    File vcardFile = vcardFiles[i];
                    String filename = vcardFile.getName(); 
                    if ( (! filename.endsWith(".vcard")) && (! filename.endsWith(".vcf")) )
                        continue;
                    
                    vcardFile.delete();
                }
            }
        }
    }

    public String getLastAnchor()
    {
        return lastAnchor;
    }

    public String getNextAnchor()
    {
        //generate the next sync anchor if required
        if (nextAnchor == null)
        {
            Date now = new Date();
            nextAnchor = Long.toString( now.getTime() );
        }
        
        return nextAnchor;
    }

    
    public String [] getMetaInfoExtensions()
    {
        Vector emiExtensions = new Vector();
        
        String nowDate = Utils.toDateTime().toString();
        emiExtensions.add(EMI_PARAM_CLIENT_TIME + "=" + nowDate);
        if ( (conflictResolution != null) && (conflictResolution.length() > 0) )
            emiExtensions.add(EMI_PARAM_CONFLICT_RES + "=" + conflictResolution);

        Date now = new Date();
        long changelogTime = now.getTime() - beginChangelogTime;
        emiExtensions.add(EMI_PARAM_CHANGELOG_TIME + "=" + changelogTime);
    	
        String[] extensions = new String[ emiExtensions.size() ];
        return (String[])emiExtensions.toArray(extensions);
    }
    	
    public void setMetaInfoExtensions(String[] extensions)
    {
        metaInfoExtensions = extensions;
    	  	
        for (int i = 0; i < metaInfoExtensions.length; i++)
            logDebug("Received extension: " + metaInfoExtensions[i]);
    }

    
    public boolean isUpdateRevision()
    {
        return updateRevision;
    }
    
    
    public void onSyncStart() 
        throws StoreException
    {
        logInfo("Sync session starting");
        
        //load the last sync state and retrieve the last anchor
        lastSyncState = SyncState.loadState( vcardStateFile.getAbsolutePath() );
        if (lastSyncState != null)
            lastAnchor = lastSyncState.lastAnchor;

        //determine the type of sync to perform - no last anchor implies a slow sync will be required
        if (syncType != SyncML.SYNC_TYPE_REFRESH_CLIENT && syncType != SyncML.SYNC_TYPE_REFRESH_SERVER)
        {
	        if ( (lastAnchor == null) || (lastAnchor.length() <= 0) )
	        {
        		syncType = SyncML.SYNC_TYPE_TWO_WAY_SLOW;
	        }
        }

        //load the current state of the records
        allRecords = new ConsumableStack();
        currentState = getCurrentState(allRecords);
    }
    
    public void onSyncSuspend()
    {
        logInfo("Sync session has been suspended");

        //no implementation required
    }
    
    public void onSyncResume()
        throws StoreException
    {
        logInfo("Sync session is being resumed");

        //TODO: check if the session can be resumed
        
        //clear the next anchor so a new one will be generated
        nextAnchor = null;
    }

    public void onSyncEnd(boolean success, int statusCode, String statusData)
    {
        if (success)
        {
            logInfo("Sync session ending - success");

            try
            {
                //calculate the new state and save it
                SyncState newState = getCurrentState(null);
                newState.lastAnchor = nextAnchor;
                SyncState.saveState(vcardStateFile.getAbsolutePath(), newState);
            }
            catch (StoreException e)
            {
                //ignore
            }
        }
        else
        {
            logInfo("Sync session ending - failure - status '" + statusCode + "' - reason '" + statusData + "'");
        }
        
        //clear data
        cleanupRequest();
        allRecords = null;
        changedRecords = null;
        lastSyncState = null;
        currentState = null;
        lastAnchor = null;
        nextAnchor = null;
    }

    
    public ConsumableStack getAllRecords() 
        throws StoreException
    {
        //just return all cached records
        return allRecords;
    }
    
    public ConsumableStack getChangedRecords() 
        throws StoreException
    {
        if (lastSyncState == null)
            throw new StoreException("The previous sync state is not known - changes can't be determined");

        //just return the cached changed records (if we already know them)
        if (changedRecords != null)
            return changedRecords;
        
        //examine each record currently in the store to detect added and modified records
        changedRecords = new ConsumableStack();
        Enumeration keysEnum = currentState.recordState.keys();
        while (keysEnum.hasMoreElements())
        {
            //get the record details
            File vcardFile = new File( vcardDirectory + File.separator + (String)keysEnum.nextElement() );
            String vcardHash = (String)currentState.recordState.get( vcardFile.getName() );
            
            //determine its previous state
            String vcardHashPrev = (String)lastSyncState.recordState.get( vcardFile.getName() );
            if (vcardHashPrev == null)
            {
                //record wasn't previously present - record has been added
                changedRecords.push( new DesktopVcard(this, vcardFile, Record.CHANGE_TYPE_ADD) );
            }
            else if (! vcardHash.equals(vcardHashPrev))
            {
                //record hashes are different - record has been modified
                changedRecords.push( new DesktopVcard(this, vcardFile, Record.CHANGE_TYPE_REPLACE) );
            }
        }
        
        //examine each record previously in the store to detect deleted records
        keysEnum = lastSyncState.recordState.keys();
        while (keysEnum.hasMoreElements())
        {
            //get the record details
            File vcardFile = new File(vcardDirectory + File.separator + (String)keysEnum.nextElement());
            
            //determine its current state
            String vcardHash = (String)currentState.recordState.get( vcardFile.getName() );
            if (vcardHash == null)
            {
                //record isn't currently present - record has been deleted
                changedRecords.push( new DesktopVcard(this, vcardFile, Record.CHANGE_TYPE_DELETE) );
            }
        }
        
        return changedRecords;
    }

    
    public void onResumeResult(int statusCode, String statusData)
    {
        if (statusCode == SyncML.STATUS_REFRESH_REQUIRED)
            logWarn("Failed to resume the sync session for record store '" + getClientURI() + "' - slow sync required");
        else if (SyncML.isSuccessStatus(statusCode))
            logDebug("Sync session successfully resumed for record store '" + getClientURI() + "'");
        else
            logError("Failed to resume the sync session for record store '" + getClientURI() + "' - status '" + statusCode + "' - reason '" + statusData + "'", null);
    }
    
    public void onAddResult(String localId, int statusCode, String statusData)
    {
        if (statusCode == SyncML.STATUS_ITEM_ADDED)
            logDebug("Record '" + localId + "' was added by the server");
        else if (statusCode == SyncML.STATUS_ITEM_ALREADY_EXISTS)
            logDebug("Record '" + localId + "' already exists on the server");
        else if (SyncML.isSuccessStatus(statusCode))
            logDebug("Record '" + localId + "' was processed by the server");
        else
            logError("Failed to add record '" + localId + "' to the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
    }
   
    public void onReplaceResult(String localId, int statusCode, String statusData)
    {
        if (statusCode == SyncML.STATUS_ITEM_GONE)
            logDebug("Record '" + localId + "' no longer exists on the server");
        else if (SyncML.isSuccessStatus(statusCode))
            logDebug("Record '" + localId + "' was replaced on the server");
        else
            logError("Failed to replace record '" + localId + "' on the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
    }

    public void onDeleteResult(String localId, int statusCode, String statusData)
    {
        if (statusCode == SyncML.STATUS_ITEM_GONE)
            logDebug("Record '" + localId + "' no longer exists on the server");
        else if (statusCode == SyncML.STATUS_ITEM_NOT_DELETED)
            logDebug("Record '" + localId + "' no longer exists on the server");
        else if (SyncML.isSuccessStatus(statusCode))
            logDebug("Record '" + localId + "' was deleted on the server");
        else
            logError("Failed to delete record '" + localId + "' on the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
    }

    public void onMoveResult(String localId, int statusCode, String statusData)
    {
    	logWarn("Unexpected move result for record: " + localId);
    }
    
    public void onCopyResult(String localId, String targetLocalId, int statusCode, String statusData)
    {
        logWarn("Unexpected copy result for record: " + localId);
    }

    
    public void onNumberOfChanges(int changeCount)
    {
        logInfo("Expecting " + changeCount + " changes from the server");
    }
    
    public void addRecordBegin(String parentId, String parentGlobalId, String globalId, ContentType contentType) 
        throws StoreException
    {
        if (inTmpOutputStream != null)
            throw new StoreException("Beginning record add without finishing previous request");
    
        try
        {
            //setup a temporary file into which we will cache the incoming data
            inTmpFile = File.createTempFile("syncml_contact_", "tmp");
            inTmpFile.deleteOnExit();
            
            inTmpOutputStream = new FileOutputStream(inTmpFile);
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to begin adding new record", e);
        }
    }
    
    public void addRecordData(byte[] data) 
        throws StoreException
    {
        if (inTmpOutputStream == null)
            throw new StoreException("Adding record data without beginning record add");
        
        try
        {
            //write the data to the temporary file
            inTmpOutputStream.write(data);
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to cache record data", e);
        }
    }
    
    public String addRecordEnd(boolean commit) 
        throws StoreException, AlreadyExistsException
    {
        if (! commit)
        {
            //discard temporary data
            cleanupRequest();
            return null;
        }

        //generate an error if required
        if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
            throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);
            
        try
        {
            //add the vCard record to the local store
            inTmpOutputStream.close();
            inTmpOutputStream = null;
            
            //generate a new name for the vCard file
            String newVcardFilename = null;
            File[] vcardFiles = vcardDirectory.listFiles();
            for (int i = 1; ; i++)
            {
                boolean found = false; 
                newVcardFilename = "CONTACT_" + i + ".vcf";
                for (int j = 0; j < vcardFiles.length; j++)
                {
                    if (vcardFiles[j].getName().equalsIgnoreCase(newVcardFilename))
                    {
                        found = true;
                        break;
                    }
                }
                
                if (found == false)
                    break;
            }
            File vcardFile = new File(vcardDirectory + File.separator + newVcardFilename);
            
            //rename the temporary file
            inTmpFile.renameTo(vcardFile);
            inTmpFile = null;
        
            return vcardFile.getName();
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to write record data", e);
        }
        finally
        {
            cleanupRequest();            
        }
    }

    public void replaceRecordBegin(String localId, ContentType contentType, boolean fieldLevelReplace) 
        throws StoreException, NoSuchRecordException
    {
        if (inTmpOutputStream != null)
            throw new StoreException("Beginning record replace without finishing previous request");
    
        if (fieldLevelReplace)    
            throw new StoreException("Field level replace is not supported");
    
        //check if the file exists
        inReplaceFile = new File(vcardDirectory + File.separator + localId);
        if (! inReplaceFile.exists())
            throw new NoSuchRecordException("The specified record doesn't exist: " + localId);
        
        try
        {
            //setup a temporary file into which we will cache the incoming data
            inTmpFile = File.createTempFile("syncml_contact_", "tmp");
            inTmpFile.deleteOnExit();
            
            inTmpOutputStream = new FileOutputStream(inTmpFile);
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to begin replacing record", e);
        }
    }

    public void replaceRecordData(byte[] data) 
        throws StoreException
    {
        if (inTmpOutputStream == null)
            throw new StoreException("Replacing record data without beginning record replace");
        
        try
        {
            //write the data to the temporary file
            inTmpOutputStream.write(data);
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to cache record data", e);
        }
    }
    
    public String replaceRecordEnd(boolean commit) 
        throws StoreException
    {
        if (! commit)
        {
            //discard temporary data
            cleanupRequest();
            return null;
        }

        //generate an error if required
        if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
            throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);
        
        InputStream tmpFileStream = null;
        OutputStream outFileStream = null;
        try
        {
            //replace the vCard record in the local store
            inTmpOutputStream.close();
            inTmpOutputStream = null;
            
            //write the vCard
            int dataRead;
            byte[] buffer = new byte[4096];
            tmpFileStream = new FileInputStream(inTmpFile);
            outFileStream = new FileOutputStream(inReplaceFile);
            while ((dataRead = tmpFileStream.read(buffer)) != -1)
                outFileStream.write(buffer, 0, dataRead);

            return null;
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to write record data", e);
        }
        finally
        {
            try
            {
                if (outFileStream != null)
                    outFileStream.close();
            }
            catch (IOException e)
            {
                //ignore
            }

            try
            {
                if (tmpFileStream != null)
                    tmpFileStream.close();
            }
            catch (IOException e)
            {
                //ignore
            }
            
            cleanupRequest();            
        }
    }

    public void deleteRecord(String localId) 
        throws StoreException, NoSuchRecordException
    {
        //generate an error if required
        if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
            throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);

        //check if the file exists
        File vcardFile = new File( vcardDirectory + File.separator + localId );
        if (! vcardFile.exists())
            throw new NoSuchRecordException("The specified record doesn't exist: " + localId);
        
        //delete the vCard file
        if (! vcardFile.delete())        
            throw new StoreException("Failed to delete the specified record: " + vcardFile.getAbsolutePath());
    }

    
    public void moveRecord(String localId, String toParentLocalId, String toParentGlobalId, byte[] data) 
        throws StoreException, NoSuchRecordException
    {
        throw new StoreException("Moving records is not supported");
    }

    public String copyRecord(String localId, String toParentLocalId, String toParentGlobalId, byte[] data) 
        throws StoreException, NoSuchRecordException, AlreadyExistsException
    {
        throw new StoreException("Copying records is not supported");
    }

    
    /* Called to cleanup temporary data from a previous Add or Replace request. */
    private void cleanupRequest()
    {
        try
        {
            if (inTmpOutputStream != null)
                inTmpOutputStream.close();
        }
        catch (IOException e)
        {
            //ignore
        }
        inTmpOutputStream = null;
        
        if ( (inTmpFile != null) && (inTmpFile.exists()) )
            inTmpFile.delete();
        inTmpFile = null;
        
        inReplaceFile = null;
    }
    
    /* Returns the current state of the vCards. */
    private SyncState getCurrentState(ConsumableStack allCurrentRecords)
        throws StoreException
    {
        //load the current state of the records
        SyncState result = new SyncState();
        File[] vcardFiles = vcardDirectory.listFiles();
        if ( (vcardFiles != null) && (vcardFiles.length > 0) )
        {
            for (int i = 0; i < vcardFiles.length; i++)
            {
                //determine the name of the file and ignore anything that isn't a vCard
                File vcardFile = vcardFiles[i];
                String filename = vcardFile.getName(); 
                if ( (! filename.endsWith(".vcard")) && (! filename.endsWith(".vcf")) )
                    continue;
    
                //cache a reference to the vCard for later use
                if (allCurrentRecords != null)
                    allCurrentRecords.push( new DesktopVcard(this, vcardFile) );
                
                try
                {
                    //hash the file contents and save the state
                    String vcardHash = CommonUtils.base64Encode( Utils.md5HashFile(vcardFile) );
                    result.recordState.put(vcardFile.getName(), vcardHash);
                }
                catch (IOException e)
                {
                    throw new StoreException("Failed to compute record hash", e);
                }
            }
        }
        
        return result;
    }
    
    
    public void logError(String message, Throwable cause)
    {
        if (logger != null)
            logger.error("VCARD_STORE: " + message, cause);
    }

    public void logDebug(String message)
    {
        if (logger != null)
            logger.debug("VCARD_STORE: " + message);
    }

    public void logWarn(String message)
    {
        if (logger != null)
            logger.warn("VCARD_STORE: " + message);
    }

    public void logInfo(String message)
    {
        if (logger != null)
            logger.info("VCARD_STORE: " + message);
    }
    
    
    /* A class representing the state of a sync session */
    static class SyncState implements Serializable
    {
        String lastAnchor;                              //the last sync anchor
        Hashtable recordState = new Hashtable();        //the state of each record
        
        static SyncState loadState(String syncStateFilename)
        {
            ObjectInputStream inputStream = null;
            try
            {
                //de-serialize the object
                inputStream = new ObjectInputStream( new FileInputStream(syncStateFilename) );
                SyncState syncState = (SyncState)inputStream.readObject();
                
                //make sure it's valid
                if ( (syncState.lastAnchor == null) || (syncState.lastAnchor.length() <= 0) )
                    return null;
                
                return syncState;
            }
            catch (IOException e)
            {
                //ignore
            }
            catch (ClassNotFoundException e)
            {
                //ignore
            }
            finally
            {
                try
                {
                    if (inputStream != null)
                        inputStream.close();
                }
                catch (IOException e)
                {
                    //ignore
                }
            }
            
            return null;
        }
        
        static void saveState(String syncStateFilename, SyncState state)
        {
            ObjectOutputStream outputStream = null;
            try
            {
                //serialize the object
                outputStream = new ObjectOutputStream( new FileOutputStream(syncStateFilename) );
                outputStream.writeObject(state);
            }
            catch (IOException e)
            {
                //ignore
            }
            finally
            {
                try
                {
                    if (outputStream != null)
                        outputStream.close();
                }
                catch (IOException e)
                {
                    //ignore
                }
            }
        }
    }
}
