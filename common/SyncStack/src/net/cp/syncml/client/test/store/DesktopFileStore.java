/**
 * Copyright � 2004-2010 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test.store;


import java.io.*;
import java.util.*;

import net.cp.mtk.common.CommonUtils;
import net.cp.syncml.client.*;
import net.cp.syncml.client.devinfo.*;
import net.cp.syncml.client.store.*;
import net.cp.syncml.client.test.store.Utils;
import net.cp.syncml.client.util.*;
import net.cp.syncml.client.util.content.*;
import net.cp.syncml.client.util.wbxml.*;


/**
 * A class implementing a record store containing files and folders.
 *
 * @author Herve Fayolle
 */
public class DesktopFileStore implements RecordStore, ContentHandler
{
	private static class DesktopFileRecordPathComparator implements Comparator
	{
		public DesktopFileRecordPathComparator()
		{
		    super();
		}
		
		public int compare(Object o1, Object o2)
		{
			DesktopFileRecord r1 = (DesktopFileRecord)o1;
			DesktopFileRecord r2 = (DesktopFileRecord)o2;
			
			return r2.getFile().getPath().compareTo(r1.getFile().getPath());
		}
	}

	
	private static class DeletedFile
	{
        public String localId;
	    public File file;
		
		public DeletedFile(String fileLocalId, File file)
		{
			this.localId = fileLocalId;
            this.file = file;
		}
	}
	
	
    private static final ContentType[] CT_SUPPORTED =               { ContentCodepage.CT_CP_FILE_WBXML, ContentCodepage.CT_CP_FOLDER_WBXML };
    
    private static final ContentTypeCapabilities CT_CAP_FILE =      new ContentTypeCapabilities(ContentCodepage.CT_CP_FILE_WBXML, null, true);
    private static final ContentTypeCapabilities CT_CAP_FOLDER =    new ContentTypeCapabilities(ContentCodepage.CT_CP_FOLDER_WBXML, null, true);
    private static final ContentTypeCapabilities[] CT_CAPS =        { CT_CAP_FILE, CT_CAP_FOLDER };

    private static final int[] SYNC_CAP = { SyncML.SYNC_TYPE_TWO_WAY, SyncML.SYNC_TYPE_TWO_WAY_SLOW };
    
    private static final int RECORDS_NONE =         0;
	private static final int RECORDS_ALL =          1;
	private static final int RECORDS_CHANGED =      2;

    private static final String FILENAME_MAP =      "data.map";
    private static final String FILENAME_SYNC =     "data.sync";
    private static final String FILENAME_ANCHOR =   "anchor.txt";
    
    private static final String KEY_DIR_PREFIX =    "/D/";
    private static final String KEY_FILE_PREFIX =   "/F/";

    private static final int KEY_PREFIX_LEN =       3;
    
    private static final int MATCH_FAIL =           0;
    private static final int MATCH_ALL =            1;
    private static final int MATCH_NAME =           2;
    private static final int MATCH_PARENT =         3;
    private static final int MATCH_CONTENT =        4;
    

    private File itsRoot;                           //the directory containing the files and folders
    private String itsMcardId;                      //the memory card ID (if any)
    private Logger itsLogger;                       //the logger used to log activity
    private long itsMaxRecordSize;                  //the max size of a record that can be stored 
    private int itsSyncType;                        //the current type of synchronisation performed by the stack
    private int inErrorStatusCode;                  //indicates the SyncML status code to generate during an incoming server operation
    private String inErrorStatusData;               //indicates the additional status data (e.g. reason code) to generate during an incoming server operation
    private boolean useFileRep;                     //indicates if file representation should be sent first when uploading files 

    private File itsData;                           //the directory containing the files and folders to sync
    private File itsMcardData;                      //the directory containing the memory card files and folders to sync
    private File itsMapFile;                        //the file containing the 'itsLocalIdKeyMap' mappings
    private File itsStateFile;                      //the file containing the 'itsLocalIdHashMap' mappings
    private File itsAnchorFile;                     //the file containing the last sync anchor
    private String itsNextAnchor;                   //the anchor for this sync session 
    private String itsLastAnchor;                   //the anchor of the last successful sync session
    private long itsLocalIdPrefix;                  //the prefix to add to each generated local ID
    private RecordStoreCapabilities storeCapabilities; //the capabilities of the record store
    
    private Hashtable itsLocalIdKeyMap;             //maps local IDs (String) to file/folder keys (String), and vice versa 
    private Hashtable itsLocalIdHashMap;			//maps local IDs (String) to the MD5 hash of the file/folder (String)
    private Hashtable itsHashLocalIdsMap;			//maps MD5 hashes (String) to a collection of local IDs (Vector of Strings) - used to identify copies and moves
    private Hashtable itsGlobalIdLocalIdMap;        //maps global IDs (String) to local IDs (String) - used to identify parent IDs added by the client during the same session
    private Vector itsDeleted;						//the collection of files/folders (DeletedFile) that have been deleted since the last sync
    private Vector itsMoved;						//the collection of files/folders (DeletedFile) that have been moved since the last sync
    private ConsumableStack itsRecords;				//the records available for syncing 
    private int itsRecordsType;                     //indicates what type of records are present (all, changes, etc)
    
    private String[] inMetaInfoExtensions;          //the EMI extensions sent by the server
    private String inGlobalId;                      //the global ID of the incoming Add/Replace record
    private String inParentId;                      //the local parent ID of the incoming Add/Replace record
    private String inParentGlobalId;                //the global parent ID of the incoming Add/Replace record
    private int inChangeType;                       //indicates the type of incoming change (add, replace, delete, copy or move)
    private ContentType inContentType;              //the content type of the incoming Add/Replace record
    private File inReplaceFile;                     //the file that is to be replaced when performing an incoming Replace request
    private String inDestFileName;                  //the name of the destination file in the case of incoming Copy/Move requests 
    private File inWbxmlFile;                       //the temporary file used to cache incoming Add/Replace data in WBXML format
    private OutputStream inWbxmlOutputStream;       //the output stream used to cache incoming Add/Replace data in WBXML format
    private File inFile;                            //the file containing the actual file data during an incoming Add/Replace request (i.e. after parsing the WBXML data)
    private OutputStream inFileOutputStream;        //the output stream used to write the actual file data during an incoming Add/Replace request (i.e. after parsing the WBXML data)
    private boolean inNeedFullFile;                 //indicates if the full file is required i.e. the incoming command is a file representation, and the client doesn't have the file 
    private boolean inFileValidate;                 //indicates if the incoming Add/Replace data should just be validated 
    private boolean inFileValidated;                //indicates if the incoming Add/Replace data has been validated
    private boolean inSuccess;                      //indicates if the incoming record was added/replaced successfully
    private boolean inExists;                       //indicates if the incoming record already exists
    private String inNewLocalId;                    //the local ID of the newly added record (or its existing local ID if it already exists) 
    
    
    public DesktopFileStore(String pathName, String memcard, Logger logger, long maxRecordSize, int syncMode, int errorStatusCode, String errorStatusData, boolean disableFileRep)
    	throws StoreException
    {
    	try
    	{
            itsLogger = logger;
            itsMaxRecordSize = maxRecordSize;
            itsSyncType = syncMode;
            inErrorStatusCode = errorStatusCode;
            inErrorStatusData = errorStatusData;
            useFileRep = (! disableFileRep);
            
            //make sure the required parameters are valid
            itsRoot = new File(pathName);
            if (! itsRoot.exists())
                throw new StoreException("Specified root folder '" + itsRoot.getPath() + "' doesn't exist");

            //create the data directory (which will contain the actual synced files/folders), if necessary
            itsData = new File(itsRoot, "data");
            if (! itsData.exists())
            {
                if (! itsData.mkdir())
                    throw new StoreException("Failed to create data folder '" + itsData.getPath() + "'");
            }
            
            //determine the file containing the last sync anchor
            itsAnchorFile = new File(itsRoot, FILENAME_ANCHOR);
            
            //create the memory card directory (which will contain the synced memory card files/folders), if necessary
            itsMcardData = null;
            itsMcardId = memcard;
            if ( (itsMcardId != null) && (itsMcardId.length() > 0) )
            {
                itsMcardData = new File(itsData, itsMcardId);
                if (! itsMcardData.exists())
                {
                    if (! itsMcardData.mkdir())
                        throw new StoreException("Failed to create memory card folder '" + itsMcardData.getPath() + "'");
                }
            }
            
            //generate a unique local ID prefix
	        itsLocalIdPrefix = System.currentTimeMillis();

	        //initialize capabilities
            storeCapabilities = new RecordStoreCapabilities(CT_CAPS, ContentCodepage.CT_CP_FILE_WBXML, ContentCodepage.CT_CP_FILE_WBXML, SYNC_CAP, 255, CT_SUPPORTED, CT_SUPPORTED, 0, 0, itsMaxRecordSize, false, true);
	        
            //read the local ID to file/folder key mappings
            itsLocalIdKeyMap = new Hashtable();
            itsMapFile = new File(itsRoot, FILENAME_MAP);
            if (itsMapFile.exists())
                itsLocalIdKeyMap = readHashtable(itsMapFile);
            
            //read the local ID to MD5 hash mappings
            itsLocalIdHashMap = new Hashtable();
            itsHashLocalIdsMap = new Hashtable();
            itsStateFile = new File(itsRoot, FILENAME_SYNC);
            if (itsStateFile.exists())
            {
                itsLocalIdHashMap = readHashtable(itsStateFile);
                
                //build the MD5 hash to local IDs mappings
                Enumeration localIdsEnum = itsLocalIdHashMap.keys();
                while (localIdsEnum.hasMoreElements())
                {
                    String localId = (String)localIdsEnum.nextElement();
                    String md5Hash = (String)itsLocalIdHashMap.get(localId);
                    setLocalIdHash(localId, md5Hash);
                }
            }
            
	    	itsGlobalIdLocalIdMap = new Hashtable();
	        itsRecords = null;
	        itsRecordsType = RECORDS_NONE;
    	}
    	catch (Throwable e)
    	{
    		logError("Failed to initialize DesktopFileStore", e);
    		throw new StoreException("Failed to initialize DesktopFileStore", e);
    	}
	}

    public Logger getLogger()
    {
        return itsLogger;
    }

    public boolean useFileRepresentation()
    {
        return useFileRep;
    }

    public String getServerURI()
    {
        return "./content";
    }

    public String getClientURI()
    {
        return "DesktopFileStore";
    }

    public String getDisplayName()
    {
        return "Desktop File Store";
    }
    
    public RecordStoreCapabilities getCapabilities()
    {
        return storeCapabilities;
    }    

    public int getSyncType() 
    {
        return itsSyncType;
    }
    
    public void setSyncType(int type)
    {
        itsSyncType = type;
    }

    public String getLastAnchor()
    {
        return itsLastAnchor;
    }
        
    public String getNextAnchor()
    {
        //generate the next sync anchor
        if (itsNextAnchor == null)
            itsNextAnchor = Long.toString( System.currentTimeMillis() );
        
        return itsNextAnchor;
    }

    public String [] getMetaInfoExtensions()
    {
        return null;
    }
    
    public void setMetaInfoExtensions(String[] extensions)
    {
        inMetaInfoExtensions = extensions;
        
        for (int i = 0; i < inMetaInfoExtensions.length; i++)
            logDebug("Received extension: " + inMetaInfoExtensions[i]);
    }

    public void onSyncStart() 
        throws StoreException
    {
        logInfo("Sync session starting");
        
        try
        {
            //read last anchor
            itsLastAnchor = readAnchor(itsAnchorFile);
        }
        catch(IOException e)
        {
            logError("Can't read anchor", e);
        }
        
        //determine the type of sync to perform - no last anchor implies a slow sync will be required
        if ( (itsSyncType != SyncML.SYNC_TYPE_REFRESH_CLIENT) && (itsSyncType != SyncML.SYNC_TYPE_REFRESH_SERVER) )
        {
            if ( (itsLastAnchor == null) || (itsLastAnchor.length() <= 0) )
                itsSyncType = SyncML.SYNC_TYPE_TWO_WAY_SLOW;
        }
    }
    
    public void onSyncSuspend()
    {
        logInfo("Sync session has been suspended");
    }
    
    public void onSyncResume()
        throws StoreException
    {
        logInfo("Sync session is being resumed");
        
        //clear the next anchor so a new one will be generated
        itsNextAnchor = null;
    }
    
    public void onSyncEnd(boolean success, int statusCode, String statusData)
    {
        if (success)
        {
            logInfo("Sync session ending - success");
            
            try
            {
                //calculate the new state and save it - only do this if the session actually completed
                if ( (itsNextAnchor != null) && (itsNextAnchor.length() > 0) )
                {
                    if (itsDeleted != null)
                    {
                        //remove the state information for any files/folders that have been deleted
                        for (int i = 0; i < itsDeleted.size(); i++)
                        {
                            DeletedFile deletedFile  = (DeletedFile)itsDeleted.elementAt(i);
                            setLocalIdKey(deletedFile.localId, null);
                            setLocalIdHash(deletedFile.localId, null);
                        }
                    }
                    
                    writeAnchor(itsAnchorFile, itsNextAnchor);
                    writeHashtable(itsMapFile, itsLocalIdKeyMap);
                    writeHashtable(itsStateFile, itsLocalIdHashMap);
                    
                    dumpTables();
                }
            }
            catch (Throwable e)
            {
                logError("Failed to save sync state", e);
            }
        }
        else
        {
            logInfo("Sync session ending - failure - status '" + statusCode + "' - reason '" + statusData + "'");
        }
        
        cleanupRequest();        
    }
    
    public ConsumableStack getAllRecords()
        throws StoreException 
    {
        if (itsRecordsType == RECORDS_NONE)
        {
            //process all file/folders in the data directory
            itsRecords = new ConsumableStack();
            getAllRecords(itsData, Record.ROOT_ID, itsRecords);
            itsRecordsType = RECORDS_ALL;
            
            //save the current state
            writeHashtable(itsMapFile, itsLocalIdKeyMap);
        }
        
        return itsRecords;
    }
    
    
    public ConsumableStack getChangedRecords()
        throws StoreException
    {
        if (itsRecordsType == RECORDS_NONE)
        {
            //process changed file/folders in the data directory
            itsRecords = new ConsumableStack();
            getChangedRecords(itsData, Record.ROOT_ID, itsRecords);
            itsRecordsType = RECORDS_CHANGED;
        }
        
        return itsRecords;
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
        if (SyncML.isSuccessStatus(statusCode))
        {
            //success
            logInfo("Server successfully added record '" + localId + "' - status '" + statusCode + "'");
        }
        else if (statusCode == SyncML.STATUS_INCOMPLETE_COMMAND)
        {
            try
            {
                logDebug("Server requires file contents for record '" + localId + "'");

                //server asks for the full file to be sent - build another record and pre-pend it to the list of changes
                File file = getFileFromId(localId, null);
                String parentId = getIdFromFile(file.getParentFile(), true);
                itsRecords.insertElementAt(new DesktopFileRecord(this, file, localId, parentId, getMemCardIdFromLocalId(localId), Record.CHANGE_TYPE_ADD, true, file.isDirectory()), 0);
            }
            catch (StoreException e)
            {
                //ignore
                logDebug("Failed to re-add file for full upload '" + localId + "'");
            }
        }
        else if (statusCode == SyncML.STATUS_ITEM_ALREADY_EXISTS)
        {
            //record already exists - ignore
            logWarn("Record '" + localId + "' already exists on the server");
        }
        else
        {
            //failure
            logError("Failed to add record '" + localId + "' to the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
            
            //remove item from sync hash, so it will be resync'ed next time
            setLocalIdHash(localId, null);
        }
    }
   
    public void onReplaceResult(String localId, int statusCode, String statusData)
    {
        if (SyncML.isSuccessStatus(statusCode))
        {
            //success
            logInfo("Server successfully replaced record '" + localId + "' - status '" + statusCode + "'");
            
            processMovedIds(false, localId, "onReplaceResult");
        }
        else
        {
            //failure
            logError("Failed to replace record '" + localId + "' on the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
            
            //update the list of moved/renamed files
            if (! unregisterMoved(localId))
            {
                //it was not a rename - reset the hash to ensure the next sync will find a difference again
                setLocalIdHash(localId, Long.toString(System.currentTimeMillis()));
            }
        }
    }
        
    public void onDeleteResult(String localId, int statusCode, String statusData)
    {
        if (SyncML.isSuccessStatus(statusCode))
        {
            //success
            logInfo("Server successfully deleted record '" + localId + "' - status '" + statusCode + "'");

            //remove item from hash table, so it won't be detected as removed on next sync
            setLocalIdKey(localId, null);
            setLocalIdHash(localId, null);
        }
        else if ( (statusCode == SyncML.STATUS_NOT_FOUND) || (statusCode == SyncML.STATUS_ITEM_GONE) )
        {
            //item not found on the server
            logWarn("Record '" + localId + "' doesn't exist on the server");
            
            //remove item from hash table, so it won't be detected as removed on next sync
            setLocalIdKey(localId, null);
            setLocalIdHash(localId, null);
        }
        else if (statusCode == SyncML.STATUS_DELETE_FAILED_NOT_EMPTY)
        {
            //item is not empty on the server and should be re-added on the client
            logWarn("Record '" + localId + "' is not empty on the server - re-adding folder");

            try
            {
                File directory = getFileFromId(localId, null, true);
                if (directory != null)
                    directory.mkdirs();
            }
            catch (Throwable e)
            {
                logError("Failed to re-add folder with local ID '" + localId + "' on the client", e);
            }
        }
        else
        {
            //failure
            logError("Failed to delete record '" + localId + "' on the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
            
            //unregister the delete so that the delete will be sent again during the next sync
            unregisterDeleted(localId);
        }
    }
    
    public void onMoveResult(String localId, int statusCode, String statusData)
    {
        if (SyncML.isSuccessStatus(statusCode))
        {
            //success
            logInfo("Server successfully moved record '" + localId + "' - status '" + statusCode + "'");
            
            processMovedIds(true, localId, "onMoveResult");
        }
        else if ( (statusCode == SyncML.STATUS_NOT_FOUND) || (statusCode == SyncML.STATUS_ITEM_GONE) )
        {
            //item to move is gone or modified on server.
            logWarn("Record '" + localId + "' doesn't exist on the server");

            //clear the state for this record so it will be seen as a new record during the next sync
            unregisterDeleted(localId);
            setLocalIdKey(localId, null);
            setLocalIdHash(localId, null);
        }
        else
        {
            //failure
            logError("Failed to move record '" + localId + "' on the server - status '" + statusCode + "' - reason '" + statusData + "'", null);

            unregisterDeleted(localId);
        }
    }
    
    public void onCopyResult(String localId, String targetLocalId, int statusCode, String statusData)
    {
        if (SyncML.isSuccessStatus(statusCode))
        {
            //success
            logInfo("Server successfully copied record '" + localId + "' to '" + targetLocalId + "' - status '" + statusCode + "'");
        }
        else if (statusCode == SyncML.STATUS_NOT_FOUND || statusCode == SyncML.STATUS_ITEM_GONE)
        {
            //item to copy is gone or modified on server.
            logWarn("Record '" + localId + "' doesn't exist on the server");
            
            //clear the state for this record so it will be seen as a new record during the next sync
            setLocalIdKey(targetLocalId, null);
            setLocalIdHash(targetLocalId, null);
        }
        else
        {
            //failure
            logError("Failed to copy record '" + localId + "' to '" + targetLocalId + "' on the server - status '" + statusCode + "' - reason '" + statusData + "'", null);
            
            setLocalIdHash(targetLocalId, null);
        }
    }
    
    public void onNumberOfChanges(int changeCount)
    {
        logInfo("Expecting " + changeCount + " changes from the server");
    }
    
    public void addRecordBegin(String parentId, String parentGlobalId, String globalId, ContentType contentType) 
        throws StoreException
    {
    	logDebug("addRecordBegin('" + parentId + "', '" + parentGlobalId + "', '" + globalId + "', '" + contentType.toString() + "')");
    	
    	//indicate the incoming request type 
    	inChangeType = Record.CHANGE_TYPE_ADD;
    	
        if (inWbxmlOutputStream != null)
            throw new StoreException("Beginning record add without finishing previous request");
    
        try
        {
            //setup a temporary file into which we will cache the incoming data
            inWbxmlFile = File.createTempFile("syncml_in_record_", null);
            inWbxmlFile.deleteOnExit();
            inWbxmlOutputStream = new FileOutputStream(inWbxmlFile);
            
            inGlobalId = globalId;
            inParentId = parentId;
            inParentGlobalId = parentGlobalId;
            inContentType = contentType;

            inSuccess = false;
            inNeedFullFile = false;
            inFileValidated = false;
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to begin adding new record", e);
        }
    }

    public void addRecordData(byte[] data) 
        throws StoreException
    {
    	logDebug("addRecordData()");
    	
    	//indicate the incoming request type 
    	inChangeType = Record.CHANGE_TYPE_ADD;
    	
    	if (inWbxmlOutputStream == null)
            throw new StoreException("Adding record data without beginning new record");
        
        try
        {
            //write the data to the temporary file
            inWbxmlOutputStream.write(data);
            inWbxmlOutputStream.flush();
            
            //attempt to validate the data if we haven't already done so - this is so we catch problems 
            //before receiving all the record data (which may be quite large) 
            if (! inFileValidated)
            {
                logDebug("Attempting to validate record data");

                try
                {
                    inExists = false;
                    inFileValidate = true;
                    parseWbxmlFile(inWbxmlFile, inContentType);
                }
                catch (Throwable e)
                {
                    //ignore exception as we may not have yet received enough data to parse
                }
                
                //check the result of the validation
                if (inFileValidated)
                {
                    logDebug("Completed validating record data - valid=" + inSuccess);
                    if (! inSuccess)
                        throw new StoreException("Record is not valid", SyncML.STATUS_DATA_STORE_FAILURE, SyncML.REASON_INVALID_FILENAME);
                }
                else
                {
                    logDebug("Not enough data to validate record");
                }
            }
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to cache record data for adding", e);
        }
    }
    
    public String addRecordEnd(boolean commit) 
        throws StoreException, AlreadyExistsException
    {
    	logDebug("addRecordEnd(" + commit + ")");

    	//indicate the incoming request type 
    	inChangeType = Record.CHANGE_TYPE_ADD;
    	
        if (! commit)
        {
            //discard temporary data
            cleanupRequest();
            return null;
        }
        
        try
        {
            //generate an error if required
            if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
                throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);

            if (inWbxmlOutputStream == null)
                throw new StoreException("Finishing record add without beginning new record");
            inWbxmlOutputStream.close();
            inWbxmlOutputStream = null;
            
            //add the file/folder record to the local store
            inSuccess = false;
            inExists = false;
            inFileValidate = false;            
            parseWbxmlFile(inWbxmlFile, inContentType);
            if (inExists)
            {
                throw new AlreadyExistsException(inNewLocalId);
            }
            if (! inSuccess)
            {
            	if (inNeedFullFile)
            		throw new StoreException("Failed to write record", SyncML.STATUS_INCOMPLETE_COMMAND);
            	
                throw new StoreException("Failed to write record");
            }

            if ( (inGlobalId != null) && (inGlobalId.length() > 0) )
            {
            	logDebug("Adding global ID to hash table: '" + inGlobalId + "' <-> '" + inNewLocalId + "'");
            	itsGlobalIdLocalIdMap.put(inGlobalId, inNewLocalId);
            }
            
            return inNewLocalId;
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to finish adding record data", e);
        }
        finally
        {
            cleanupRequest();            
        }
    }

    public void replaceRecordBegin(String localId, ContentType contentType, boolean fieldLevelReplace) 
        throws StoreException, NoSuchRecordException
    {
    	logDebug("replaceRecordBegin('" + localId + "', '" + contentType.toString() + "', '" + fieldLevelReplace + "')");

    	//indicate the incoming request type
    	inChangeType = Record.CHANGE_TYPE_REPLACE;
    	
        if (inWbxmlOutputStream != null)
            throw new StoreException("Beginning record replace without finishing previous request");
        
        //check if the file exists
        inReplaceFile = getFileFromId(localId, null);
        if (! inReplaceFile.exists())
            throw new NoSuchRecordException("The specified record '" + localId + "' doesn't exist");
        
        try
        {
            //setup a temporary file into which we will cache the incoming data
            inWbxmlFile = File.createTempFile("syncml_in_record_", null);
            inWbxmlFile.deleteOnExit();
            inWbxmlOutputStream = new FileOutputStream(inWbxmlFile);

            inContentType = contentType;
            
            inFileValidate = false;  
            inSuccess = false;
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to begin replacing record", e);
        }
    }

    public void replaceRecordData(byte[] data) 
        throws StoreException
    {
    	logDebug("replaceRecordData()");
    	
    	//indicate the incoming request type
    	inChangeType = Record.CHANGE_TYPE_REPLACE;
    	
        if (inWbxmlOutputStream == null)
            throw new StoreException("Replacing record data without beginning record replace");
        
        try
        {
            //write the data to the temporary file
            inWbxmlOutputStream.write(data);
            inWbxmlOutputStream.flush();
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to cache record data for replacing", e);
        }
    }
    
    public String replaceRecordEnd(boolean commit) 
        throws StoreException
    {
    	logDebug("replaceRecordend(" + commit + ")");
    	
    	//indicate the incoming request type
    	inChangeType = Record.CHANGE_TYPE_REPLACE;
    	
        if (! commit)
        {
            //discard temporary data
            cleanupRequest();
            return null;
        }

        try
        {
            //generate an error if required
            if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
                throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);
            
            if (inWbxmlOutputStream == null)
                throw new StoreException("Finishing record replace without beginning new record");
            inWbxmlOutputStream.close();
            inWbxmlOutputStream = null;
            
            //replace the file/folder record in the local store
            inSuccess = false;
            inFileValidate = false;            
            parseWbxmlFile(inWbxmlFile, inContentType);
            if (! inSuccess)
                throw new StoreException("Failed to write record");

            return null;
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to finish replacing record", e);
        }
        finally
        {
            cleanupRequest();            
        }
    }

    public void deleteRecord(String localId)
    	throws StoreException, NoSuchRecordException
    {
    	logDebug("deleteRecord('" + localId + "')");
    	
    	//indicate the incoming request type
    	inChangeType = Record.CHANGE_TYPE_DELETE;
    	
        //generate an error if required
        if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
            throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);
        
        //determine the file or folder to delete
        File file = getFileFromId(localId, null, true);
        if ( (file == null) || (! file.exists()) )
            throw new NoSuchRecordException("The file/folder '" + localId + "' doesn't exist");
        
        //delete the file (if it's a directory, it must be empty)
        boolean isDirectory = file.isDirectory(); 
        if (! file.delete())
            throw new StoreException("Failed to delete the file/folder '" + file.getPath() + "'");
        
        //update hashes - remove the mappings for the deleted file
        updateHashes(file, null, null, null, isDirectory);
    }

    public void moveRecord(String localId, String toParentLocalId, String toParentGlobalId, byte[] data) 
        throws StoreException, NoSuchRecordException
    {
    	logDebug("moveRecord('" + localId + "', '" + toParentLocalId + "', '" + toParentGlobalId + "', data[" + ((data != null) ? data.length : 0) + "])");

    	//indicate the incoming request type
    	inChangeType = Record.CHANGE_TYPE_MOVE;
    	inDestFileName = null;
    	
        //generate an error if required
        if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
            throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);
        
        //determine the file to move
        File file = getFileFromId(localId, null);
        if (! file.exists())
            throw new NoSuchRecordException("The file '" + file.getPath() + "' doesn't exist");

        //determine the parent record (i.e. the location where the record should be moved to)
        File parentFile = getFileFromId(toParentLocalId, toParentGlobalId);
        if (! parentFile.exists())
            throw new StoreException("The parent folder '" + parentFile.getPath() + "' doesn't exist");
        
        //get destination file name from data
        if (data != null)
        {
        	//parse the data to set inDestFileName
            parseWbxmlData(data);
        }
        
        if (inDestFileName == null)
        	inDestFileName = file.getName();
        
        //check if the destination file already exists
        File destFile = new File(parentFile, inDestFileName);
        if (destFile.exists())
        {
            //nothing more to do if the file has already been moved
            if (destFile.equals(file))
            {
                logInfo("File '" + file.getPath() + "' has already been moved - ignoring move");
                return;
            }

            //TODO: conflict resolution required here?
            logInfo("Destination file '" + destFile.getPath() + "' already exists");
            throw new StoreException("Destination file '" + destFile.getPath() + "' already exists");
        }
        
        //move the file to the new location 
        if (! file.renameTo(destFile))
            throw new StoreException("Failed to move file '" + file.getPath() + "'");
        
        //update hashes - update the mappings for the moved file - map the existing local ID to a new file key
        updateHashes(file, destFile, null, null, destFile.isDirectory());
    }
    
    public String copyRecord(String localId, String toParentLocalId, String toParentGlobalId, byte[] data) 
        throws StoreException, NoSuchRecordException, AlreadyExistsException
    {
    	logDebug("copyRecord('" + localId + "', '" + toParentLocalId + "', '" + toParentGlobalId + "', data[" + ((data != null) ? data.length : 0) + "])");
    	
    	//indicate the incoming request type
    	inChangeType = Record.CHANGE_TYPE_COPY;
    	inDestFileName = null;
    	
        //generate an error if required
        if ( (inErrorStatusCode > 0) || (inErrorStatusData != null) )
            throw new StoreException("Generating dummy error", inErrorStatusCode, inErrorStatusData);

    	//determine the file to copy
        File file = getFileFromId(localId, null);
        if (! file.exists())
            throw new NoSuchRecordException("The file '" + file.getPath() + "' doesn't exist");

        //determine the parent record (i.e. the location where the record should be moved to)
        File parentFile = getFileFromId(toParentLocalId, toParentGlobalId);
        if (! parentFile.exists())
            throw new StoreException("The parent folder '" + parentFile.getPath() + "' doesn't exist");
        
        //get destination file name from data
        if (data != null)
        {
        	//parse the data to set inDestFileName
            parseWbxmlData(data);
        }
        
        if (inDestFileName == null)
        	inDestFileName = file.getName();
        	
        //check if the destination file already exists
        File destFile = new File(parentFile, inDestFileName);
        if (destFile.exists())
        {
            //nothing more to do if there is already a mapping for this record
            String destLocalId = getIdFromFile(destFile, destFile.isDirectory());
            if (destLocalId != null)
            {
                logInfo("ile '" + destFile.getPath() + "' already exists and is mapped to local ID '" + destLocalId + "' - ignoring copy");
                return destLocalId;
            }

            //TODO: conflict resolution required here?
            logInfo("Destination file '" + destFile.getPath() + "' already exists");
            throw new StoreException("Destination file '" + destFile.getPath() + "' already exists");
        }

        try
        {
            //copy the file to its new location
            Utils.copyFile(file, destFile);
            
            //update hashes - add new mappings for the copied file
            String copiedLocalId = newLocalId(destFile);
            updateHashes(null, destFile, copiedLocalId, null, destFile.isDirectory());
            
            return copiedLocalId;
        }
        catch (IOException e)
        {
            throw new StoreException("Failed to copy file '" + file.getPath() + "'");
        }
    }

    public void onFolder(ContentFolder folder)
    {
        try
        {
            String folderName = folder.getName();
            if (inChangeType == Record.CHANGE_TYPE_COPY || inChangeType == Record.CHANGE_TYPE_MOVE)
            {
                //in case of copy or move, we only keep track of the name
                inDestFileName = folderName;
                return;
            }
            
            if (inChangeType == Record.CHANGE_TYPE_ADD)
            {
                //nothing more to do if we're just validating the record
                if (inFileValidate)
                {
                    logDebug("Validating folder with name '" + folderName + "'");
                    inSuccess = isValidFilename(folderName);
                    inFileValidated = true;
                    return;
                }

                //add - check if the folder already exists
                File parentFile = getFileFromId(inParentId, inParentGlobalId);
                File addedFile = new File(parentFile, folderName);
                if (addedFile.exists())
                {
                    if (! addedFile.isDirectory())
                    {
                        inSuccess = false;
                        logError("Failed to add folder '" + addedFile.getPath() + "' - a file with the same name exists in the way", null);
                        return;
                    }

                    //nothing more to do if there is already a mapping for this file
                    inNewLocalId = getIdFromFile(addedFile, addedFile.isDirectory());
                    if (inNewLocalId != null)
                    {
                        logInfo("Folder '" + addedFile.getPath() + "' already exists and is mapped to local ID '" + inNewLocalId + "' - ignoring add");
                        inSuccess = true;
                        return;
                    }

                    //TODO: conflict resolution required here?
                    logInfo("Folder '" + addedFile.getPath() + "' already exists");
                    inSuccess = true;
                }
                else
                {
                    //add the folder
                    inSuccess = addedFile.mkdir();
                    if (! inSuccess)
                    {
                        logError("Failed to add folder '" + addedFile.getPath() + "'", null);
                        return;
                    }
                }

                //update hashes - add new mappings for the new folder
                inNewLocalId = newLocalId(addedFile);
                updateHashes(null, addedFile, inNewLocalId, inGlobalId, true);
            }
            else
            {
                //replace - just update the relevant fields
                if (folder.isFieldSet(ContentObject.FIELD_NAME))
                {
                    //rename the folder if necessary
                    File renamedFile = new File(inReplaceFile.getParentFile(), folder.getName());
                    if (renamedFile.equals(inReplaceFile))
                        return;
        
                    inSuccess = inReplaceFile.renameTo(renamedFile);
                    if (! inSuccess)
                    {
                        logError("Failed to rename folder '" + inReplaceFile.getPath() + "' to '" + renamedFile.getPath() + "'", null);
                        return;
                    }
        
                    //update hashes - update the mappings to refer to the renamed folder
                    updateHashes(inReplaceFile, renamedFile, null, null, true);
                }
            }
        }
        catch (Throwable e)
        {
            logError("Failed to handle folder", e);
            inSuccess = false;
            return;
        }
    }
    
    public void onFileBegin(ContentFile file, boolean bodyPresent)
    {
        try
        {
            String fileName = file.getName();
            if ( (inChangeType == Record.CHANGE_TYPE_COPY) || (inChangeType == Record.CHANGE_TYPE_MOVE) )
            {
                //in case of copy or move, we only keep track of the name
                inDestFileName = fileName;
                return;
            }

            //nothing more to do if we're just validating the record
            if (inFileValidate)
            {
                logDebug("Validating file with name '" + fileName + "'");
                inSuccess = isValidFilename(fileName);
                inFileValidated = true;
                return;
            }
            
            //create the temporary file output stream to store the file contents
            if (bodyPresent)
            {
                inFile = File.createTempFile("syncml_in_file_", null);
                inFile.deleteOnExit();
                inFileOutputStream = new FileOutputStream(inFile);
            }
        }
        catch (Throwable e)
        {
            logError("Failed to begin handling file", e);
            inSuccess = false;
            return;
        }
    }

    public void onFileData(ContentFile file, byte[] data, int length)
    {
        //ignore data on copy or move (there should be none anyway)
        if ( (inChangeType == Record.CHANGE_TYPE_COPY) || (inChangeType == Record.CHANGE_TYPE_MOVE) )
            return;
        
        try
        {
            //nothing more to do if we're just validating the record
            if (inFileValidate)
                return;

            //write the data to the temporary file
            if (inFileOutputStream != null)
            {
                inFileOutputStream.write(data, 0, length);
                inFileOutputStream.flush();
            }
        }
        catch (Throwable e)
        {
            logError("Failed to handle file data", e);
            inSuccess = false;
            return;
        }
    }

    public void onFileEnd(ContentFile file, boolean commit)
    {
        //ignore file end on copy or move
        if ( (inChangeType == Record.CHANGE_TYPE_COPY) || (inChangeType == Record.CHANGE_TYPE_MOVE) )
            return;
        
        try
        {
            //nothing more to do if we're just validating the record
            if (inFileValidate)
                return;

            //close the temporary file
            Utils.streamClose(inFileOutputStream);
            inFileOutputStream = null;
            
            if (! commit)
                return;
            
            if (inChangeType == Record.CHANGE_TYPE_ADD)
            {
                //add - check if the file already exists
                File parentFile = getFileFromId(inParentId, inParentGlobalId, true);
                File addedFile = new File(parentFile, file.getName());
                
                //get hash if any
                String inHash = null;
                Extension[] extensions = file.getExtensions();
                if (extensions != null)
                {
                    for (int i = 0; i < extensions.length; i++)
                    {
                        if (ContentFile.EXT_CP_HASH_CONTENT.equalsIgnoreCase(extensions[i].getName()))
                        {
                            String [] values = extensions[i].getValues();
                            if ( (values != null) && (values.length > 0) )
                                inHash = values[0];
                            break;
                        }
                    }
                }

                //file representation
                if ( (inHash != null) && (inHash.length() > 0) )
                {
                    String hash = null;
                    if (! addedFile.exists())
                    {
                        // new file
                        logInfo("File '" + addedFile.getPath() + "' doesn't exist - full file required from the server");
                        inNeedFullFile = true;
                        inSuccess = false;
                    }
                    else
                    {
                        //file exists - get the hash
                        hash = CommonUtils.base64Encode(Utils.md5HashFile(addedFile));
                        if (inHash.equals(hash))
                        {
                            //same file
                            logInfo("File '" + addedFile.getPath() + "' already exists with the same content");
                            
                            //update hashes - add new mappings for the new file
                            inNewLocalId = newLocalId(addedFile);
                            updateHashes(null, addedFile, inNewLocalId, inGlobalId, false);
                            
                            inSuccess = true;
                        }
                        else
                        {   //conflict
                            logError("File '" + addedFile.getPath() + "' already exists but is different", null);
                            inSuccess = false;
                        }
                    }
                    
                    return;
                }
                
                //full file received
                if (addedFile.exists())
                {
                    //conflict
                    logError("File '" + addedFile.getPath() + "' already exists", null);
                    inSuccess = false;
                    return;
                }
                
                //create a new file from the temporary data
                inSuccess = inFile.renameTo(addedFile);
                if (!inSuccess)
                {
                    logError("Failed to rename temporary file '" + inFile.getPath() + "' to new file '" + addedFile.getPath() + "'", null);
                    return;
                }
                
                //update hashes - add new mappings for the new file
                inNewLocalId = newLocalId(addedFile);
                updateHashes(null, addedFile, inNewLocalId, inGlobalId, false);
            }       
            else
            {
                //replace - just update the relevant fields
                if (file.isFieldSet(ContentObject.FIELD_FILE_CONTENTS))
                {
                    //backup the existing file 
                    File backupFile = File.createTempFile("syncml_backup_file_", null);
                    backupFile.delete();
                    inSuccess = inReplaceFile.renameTo(backupFile);
                    if (! inSuccess)
                    {
                        logError("Failed to rename existing file '" + inReplaceFile.getPath() + "' to backup file '" + backupFile.getPath() + "'", null);
                        return;
                    }
            
                    //update the file contents from the temporary data
                    inSuccess = inFile.renameTo(inReplaceFile);
                    if (! inSuccess)
                    {
                        logError("Failed to rename temporary file '" + inFile.getPath() + "' to existing file '" + inReplaceFile.getPath() + "'", null);
                        backupFile.renameTo(inReplaceFile);
                        return;
                    }
                    backupFile.delete();
                    
                    //update the hash
                    String fileKey = buildFileKey(inReplaceFile, false);
                    String localId = (String)itsLocalIdKeyMap.get(fileKey);
                    if (localId != null)
                        setLocalIdHash(localId, getHash(inReplaceFile));
                }
                if (file.isFieldSet(ContentObject.FIELD_NAME))
                {
                    //rename the file if necessary
                    File renamedFile = new File(inReplaceFile.getParentFile(), file.getName());
                    if (! renamedFile.equals(inReplaceFile))
                    {
                        inSuccess = inReplaceFile.renameTo(renamedFile);
                        if (! inSuccess)
                        {
                            logError("Failed to rename existing file '" + inReplaceFile.getPath() + "' to '" + renamedFile.getPath() + "'", null);
                            return;
                        }

                        //update hashes - update the mappings to refer to the renamed file
                        updateHashes(inReplaceFile, renamedFile, null, null, false);
                    }
                }
            }
        }
        catch (Throwable e)
        {
            logError("Failed to finish handling file", e);
            inSuccess = false;
            return;
        }
        finally
        {
            if (inFile != null)            
                inFile.delete();
            inFile = null;
        }
    }

    private void cleanupRequest()
    {
        Utils.streamClose(inWbxmlOutputStream);
        inWbxmlOutputStream = null;
        
        if ( (inWbxmlFile != null) && (inWbxmlFile.exists()) )
            inWbxmlFile.delete();
        inWbxmlFile = null;
        
        inContentType = null;
        inParentId = null;
        inParentGlobalId = null;
        inReplaceFile = null;
        inNeedFullFile = false;
    }
    
    private void getAllRecords(File directory, String directoryLocalId, ConsumableStack records)
        throws StoreException
    {
        try
        {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                boolean isDirectory = file.isDirectory();

                String fileKey = buildFileKey(file, isDirectory);
                String fileLocalId = (String)itsLocalIdKeyMap.get(fileKey);
                if (fileLocalId == null)
                {
                    fileLocalId = newLocalId(file);
                    
                    itsLocalIdKeyMap.put(fileLocalId, fileKey);
                    itsLocalIdKeyMap.put(fileKey, fileLocalId);
                    setLocalIdHash(fileLocalId, getHash(file));
                }
                else
                {
                    if (!itsLocalIdKeyMap.containsKey(fileLocalId))
                        itsLocalIdKeyMap.put(fileLocalId, fileKey);
                    
                    if (itsLocalIdHashMap.containsKey(fileLocalId))   
                        setLocalIdHash(fileLocalId, getHash(file));
                }
                
                records.add( new DesktopFileRecord(this, file, fileLocalId, directoryLocalId, getMemCardIdFromFile(file), Record.CHANGE_TYPE_ADD, isDirectory) );
                
                if (isDirectory)
                    getAllRecords(file, fileLocalId, records);
            }
        }
        catch(IOException e)
        {
            throw new StoreException("Failed to get all records", e);
        }
    }

    private void getChangedRecords(File directory, String directoryLocalId, ConsumableStack records)
        throws StoreException
    {
        itsDeleted = new Vector();
        itsMoved = new Vector();
        
        //first try to detect deleted files/folders by examining the records that have already been synced (if any)
        ConsumableStack deletedRecords = new ConsumableStack();
        Enumeration fileKeyEnum = itsLocalIdKeyMap.keys();
        while (fileKeyEnum.hasMoreElements())
        {
            //get the key, ignoring anything that isn't a file key (as this map also contains local IDs)
            String fileKey = (String)fileKeyEnum.nextElement();
            if (! isFileKey(fileKey))
                continue;
            
            //check if the file/folder has been deleted
            DesktopFileRecord deletedRecord = buildDeletedRecord(getFileFromKey(fileKey), isFileKeyDirectory(fileKey));
            if (deletedRecord != null)
                deletedRecords.add(deletedRecord);
        }
        
        //then examine all files on disk to detect any changes
        ConsumableStack changedRecords = new ConsumableStack();
        getChangedRecords(directory, directoryLocalId, deletedRecords, changedRecords);
        
        //separate into files and folders as we will need to send the deletes separately - also sort them so that we delete the leaves first
        ConsumableStack deletedFiles = new ConsumableStack();
        ConsumableStack deletedFolders = new ConsumableStack();
        Object deletedRecordsArray[] = deletedRecords.toArray();
        Arrays.sort(deletedRecordsArray, new DesktopFileRecordPathComparator());
        for (int i = 0; i < deletedRecordsArray.length; i++)
        {
            DesktopFileRecord deletedRecord = (DesktopFileRecord)deletedRecordsArray[i];
            String deletedFileKey = (String)itsLocalIdKeyMap.get(deletedRecord.getLocalId());
            if ( (deletedFileKey == null) || (isFileKeyDirectory(deletedFileKey)) )
                deletedFolders.add(deletedRecord);
            else
                deletedFiles.add(deletedRecord);
        }

        //build the final list of changes - send deleted files first, to free up some space on the server
        records.addAll(deletedFiles);
        
        //send any other changes (add/replace/copy/move)
        records.addAll(changedRecords);
        
        //finally send any deleted folders - we can't do this earlier as a deleted folder might be the target of a copy/move
        records.addAll(deletedFolders);
    }

    private void getChangedRecords(File directory, String directoryLocalId, ConsumableStack deletedRecords, ConsumableStack records)
        throws StoreException
    {
        try
        {
            File[] files = directory.listFiles();
            for (int i = 0; i < files.length; i++)
            {
                File file = files[i];
                boolean isDirectory = file.isDirectory();
                boolean fileProcessed = false;
                
                String fileKey = buildFileKey(file, isDirectory);
                String fileHash = getHash(file);
                String fileLocalId = (String)itsLocalIdKeyMap.get(fileKey);
                if (fileLocalId == null)
                {
                    logDebug("Found new file '" + file.getPath() + "'");
                    
                    //found a new file - check if it matches any deleted file, as this would indicate a move
                    for (int j = 0; j < deletedRecords.size(); j++)
                    {
                        DesktopFileRecord deletedRecord = (DesktopFileRecord)deletedRecords.get(j);
                        int cmp = compareFile(deletedRecord.getFile(), deletedRecord.getLocalId(), file, fileHash);
                        if (cmp == MATCH_ALL)
                        {
                            //found an exact match to a deleted file - can't happen - ignore change
                            logDebug("New file '" + file.getPath() + "' had been deleted - ignoring change");
                            
                            deletedRecords.removeElementAt(j);
                            unregisterDeleted(deletedRecord.getLocalId());
                            fileProcessed = true;
                            break;
                        }
                        else if (cmp == MATCH_PARENT)
                        {
                            //parent is the same but file name is different - send a rename
                            logDebug("New file '" + file.getPath() + "' had been renamed - sending field level replace of file name");
                            
                            deletedRecords.removeElementAt(j);
                            registerMovedFromDeleted(file, deletedRecord.getLocalId());
                            deletedRecord.setChangeType(Record.CHANGE_TYPE_REPLACE);
                            deletedRecord.setFile(file, isDirectory);
                            deletedRecord.setFields(ContentObject.FIELD_NAME);
                            if (isDirectory)
                            {
                                //for folder, the hash is the name, so update the local ID to hash mapping
                                setLocalIdHash(deletedRecord.getLocalId(), fileHash);
                            }
                            
                            records.add(deletedRecord);
                            fileProcessed = true;
                            break;
                        }
                        else if (cmp == MATCH_NAME)
                        {
                            //name is the same but parent is different - send a move 
                            logDebug("New file '" + file.getPath() + "' had been moved - sending move");
                            
                            deletedRecords.removeElementAt(j);
                            File toFile = new File(file.getParentFile(), deletedRecord.getFile().getName());
                            registerMovedFromDeleted(toFile, deletedRecord.getLocalId());
                            deletedRecord.setChangeType(Record.CHANGE_TYPE_MOVE);
                            deletedRecord.setFile(toFile, isFileKeyDirectory(fileKey));
                            deletedRecord.setParentId( getIdFromFile(file.getParentFile(), true) );
                            
                            records.add(deletedRecord);
                            fileProcessed = true;
                            break;
                        }
                        else if (cmp == MATCH_CONTENT)
                        {
                            //file content is the same but parent and name are different - send a move and rename
                            logDebug("New file '" + file.getPath() + "' had been moved and renamed - sending move with field level replace of file name");
    
                            deletedRecords.removeElementAt(j);
                            registerMovedFromDeleted(file , deletedRecord.getLocalId());
                            deletedRecord.setChangeType(Record.CHANGE_TYPE_MOVE);
                            deletedRecord.setFile(file, isFileKeyDirectory(fileKey));
                            deletedRecord.setParentId( getIdFromFile(file.getParentFile(), true) );
                            deletedRecord.setFields(ContentObject.FIELD_NAME);
                            if (isDirectory)
                            {
                                //for folder, the hash is the name, so update the hash sync table
                                setLocalIdHash(deletedRecord.getLocalId(), fileHash);
                            }
                            
                            records.add(deletedRecord);
                            fileProcessed = true;
                            break;
                        }
                    }
                    
                    //the new file is not a move or a rename - check if it is a copy
                    if ( (! fileProcessed) && (file.isFile()) )
                    {
                        //look for files with the same hash
                        Vector localIds = (Vector)itsHashLocalIdsMap.get(fileHash);
                        if (localIds != null)
                        {
                            for (int j = 0; j < localIds.size(); j++)
                            {
                                String otherFileLocalId = (String)localIds.get(j);
                                String otherFileKey = (String)itsLocalIdKeyMap.get(otherFileLocalId);
                                File otherFile = getFileFromKey(otherFileKey);
                                String otherFileParentId = getIdFromFile(otherFile.getParentFile(), true);
                                int cmp = compareFile(otherFile, otherFileLocalId, file, fileHash);
                                if (cmp == MATCH_ALL)
                                {
                                    //found an exact match to an existing file - can't happen - ignore change
                                    logDebug("New file '" + file.getPath() + "' had been an exact copy of '" + otherFileKey + "' - ignoring change");
                                    
                                    fileProcessed = true;
                                    break;
                                }
                                else if (cmp == MATCH_NAME || cmp == MATCH_PARENT || cmp == MATCH_CONTENT)
                                {
                                    //name, parent or content is the same - send a copy
                                    logDebug("New file '" + file.getPath() + "' had been copied from '" + otherFileKey + "' - sending copy");
    
                                    records.add( buildCopiedRecord(otherFileLocalId, otherFileParentId, fileKey, file, fileHash, (cmp == MATCH_PARENT || cmp == MATCH_CONTENT)) );
                                    fileProcessed = true;
                                    break;
                                }
                            }
                        }
                    }
                    
                    //the new file doesn't match any current or deleted files - send it as a real Add
                    if (! fileProcessed)
                    {
                        logDebug("New file '" + file.getPath() + "' - sending add");
    
                        fileLocalId = newLocalId(file);
                        itsLocalIdKeyMap.put(fileLocalId, fileKey);
                        itsLocalIdKeyMap.put(fileKey, fileLocalId);
                        setLocalIdHash(fileLocalId, fileHash);
                        records.add( new DesktopFileRecord(this, file, fileLocalId, directoryLocalId, getMemCardIdFromFile(file), Record.CHANGE_TYPE_ADD, isDirectory) );
                    }
                }
                else
                {
                    //found an existing file - check if it has been modified
                    String oldFileHash = (String)itsLocalIdHashMap.get(fileLocalId);
                    if (oldFileHash == null)
                    {
                        //existing file with no existing hash - send an add
                        logDebug("Existing file '" + file.getPath() + "' has no hash - sending add");
                        
                        setLocalIdHash(fileLocalId, fileHash);
                        records.add( new DesktopFileRecord(this, file, fileLocalId, directoryLocalId, getMemCardIdFromFile(file), Record.CHANGE_TYPE_ADD, isDirectory) );
                    }
                    else
                    {
                        if (! fileHash.equals(oldFileHash))
                        {
                            //existing file with different hash - send a replace
                            logDebug("Existing file '" + file.getPath() + "' is different - sending replace");
    
                            setLocalIdHash(fileLocalId, fileHash);
                            records.add(new DesktopFileRecord(this, file, fileLocalId, directoryLocalId, getMemCardIdFromFile(file), Record.CHANGE_TYPE_REPLACE, true, isDirectory));
                        }
                    }
                }
                
                //recursively process any sub-directories
                if ( (! fileProcessed) && (isDirectory) )
                    getChangedRecords(file, fileLocalId, deletedRecords, records);
            }
        }
        catch(IOException e)
        {
            throw new StoreException("Failed to get changed records", e);
        }
    }

    private DesktopFileRecord buildDeletedRecord(File file, boolean isDirectory)
    {
        //nothing more to do if the file/folder exists
        if (file.exists())
            return null;
        
        logDebug("Found deleted file '" + file.getPath() + "'");
        
        String fileKey = buildFileKey(file, isDirectory);
        String fileLocalId = (String)itsLocalIdKeyMap.get(fileKey);
        if (fileLocalId != null)
        {
            //check if this file ever successfully synced
            String fileHash = (String)itsLocalIdHashMap.get(fileLocalId);
            if (fileHash != null)
            {
                logDebug("Deleted file '" + file.getPath() + "' - sending delete");
                
                String parentLocalId = getIdFromFile(file.getParentFile(), true);
                registerDeleted(file, fileLocalId);
                return new DesktopFileRecord(this, file, fileLocalId, parentLocalId, getMemCardIdFromFile(file), Record.CHANGE_TYPE_DELETE, isFileKeyDirectory(fileKey));
            }
        }

        logDebug("File '" + file.getPath() + "' was never synced - ignoring it");
        
        return null;
    }

    private DesktopFileRecord buildCopiedRecord(String sourceLocalId, String parentCopiedId, String targetFileKey, File targetFile, String targetFileHash, boolean renamed)
    {
        String targetLocalId = newLocalId(targetFile);
        itsLocalIdKeyMap.put(targetLocalId, targetFileKey);
        itsLocalIdKeyMap.put(targetFileKey, targetLocalId);
        setLocalIdHash(targetLocalId, targetFileHash);
    
        String targetParentLocalId = getIdFromFile(targetFile.getParentFile(), true);
        DesktopFileRecord targetRecord = new DesktopFileRecord(this, targetFile, sourceLocalId, parentCopiedId, getMemCardIdFromFile(targetFile), Record.CHANGE_TYPE_COPY, false);
        targetRecord.setParentTargetId(targetParentLocalId);
        targetRecord.setTargetId(targetLocalId);
        if (renamed)
            targetRecord.setFields(ContentObject.FIELD_NAME);
        
        return targetRecord;
    }
    
    private int compareFile(File file1, String localId1, File file2, String fileHash2)
    {
        if (file2.isDirectory())
        {
            //disallow folder move, as our Phone Backup client doesn't support this feature
            //for performance reasons. So it is disabled in the test application to behave
            //identically to PBC as much as possible.
            return MATCH_FAIL;
        }
        
        String fileHash1 = (String)itsLocalIdHashMap.get(localId1);
        String fileKey1 = (String)itsLocalIdKeyMap.get(localId1);
        if ( (fileHash1 == null) || (fileKey1 == null) || (! fileHash1.equals(fileHash2)) || (isFileKeyDirectory(fileKey1)) )
            return MATCH_FAIL;
        
        //check if the names are the same
        String fileName1 = file1.getName();
        String fileName2 = file2.getName();
        boolean sameNames = (fileName1.equals(fileName2));
        
        //check if they have the same parent
        File parentFile1 = file1.getParentFile();
        File parentFile2 = file2.getParentFile();
        if (getRelativePath(parentFile1).equals(getRelativePath(parentFile2)))
            return (sameNames) ? MATCH_ALL : MATCH_PARENT;

        return (sameNames) ? MATCH_NAME : MATCH_CONTENT;
    }
    
    private void parseWbxmlData(byte[] data)
        throws StoreException
    {
        ByteArrayInputStream inputStream = null;
        try
        {
            logInfo("Parsing file/folder information as WBXML opaque data");
            
            //parse the data - calls this.onFolder() and this.onFileXXX() methods
            inputStream = new ByteArrayInputStream(data);
            ContentCodepage fileFolderCodepage = new ContentCodepage(itsLogger, this);
            Wbxml.parse(inputStream, new Codepage[] { fileFolderCodepage });
        }
        catch (Throwable e)
        {
            throw new StoreException("Failed to parse WBXML data", e);
        }
        finally
        {
            Utils.streamClose(inputStream);
        }
    }
    
    private void parseWbxmlFile(File wbxmlFile, ContentType contentType)
        throws StoreException
    {
        //we only support WBXML encoding
        if ( (! contentType.toString().equals("application/vnd.omads-file+cpwbxml")) && (! contentType.toString().equals("application/vnd.omads-folder+cpwbxml")) )
            throw new StoreException("Unsupported content type '" + contentType + "'");
        
        InputStream inputStream = null;
        try
        {
            logInfo("Parsing file/folder information as WBXML opaque data");
            
            //parse the temporary file - calls this.onFolder() and this.onFileXXX() methods
            inputStream = new FileInputStream(wbxmlFile);
            ContentCodepage fileFolderCodepage = new ContentCodepage(itsLogger, this);
            Wbxml.parse(inputStream, new Codepage[] { fileFolderCodepage });
        }
        catch (Throwable e)
        {
            throw new StoreException("Failed to parse WBXML data", e);
        }
        finally
        {
            Utils.streamClose(inputStream);
        }
    }

    private boolean isValidFilename(String name)
    {
        if ( (name == null) || (name.length() <= 0) )
        {
            logError("Invalid filename: no filename specified", null);
            return false;
        }

        //check the length
        if (name.length() > 255)
        {
            logError("Invalid filename: filename exceeds max allowed (" + 255 + " characters)", null);
            return false;
        }
        
        //check for invalid characters
        for (int i = 0; i < name.length(); i++)
        {
            char c = name.charAt(i);
            if ( (c == '<') || (c == '>') || (c == ':') || (c == '/') || (c == '\\') || (c == '|') || (c == '?') || (c == '*') )
            {
                logError("Invalid filename: filename contains invalid character '" + c + "'", null);
                return false;
            }
        }
        
        return true;
    }

    public String getHash(File file)
        throws IOException
    {
        if (! file.exists())
            throw new IOException("The specified file/folder '" + file.getPath() + "' doesn't exist");
        
        if (file.isDirectory())
            return "#" + file.getName();
        
        byte[] md5Hash = Utils.md5HashFile(file);
        return CommonUtils.base64Encode(md5Hash);
    }

    private void registerDeleted(File file, String localId)
    {
        itsDeleted.add( new DeletedFile(localId, file) );
    }
        
    private void unregisterDeleted(String localId)
    {
        for (int i = 0; i < itsDeleted.size(); i++)
        {
            DeletedFile deletedFile = (DeletedFile)itsDeleted.get(i);
            if (localId.equals(deletedFile.localId))
            {
                itsDeleted.remove(i);
                break;
            }
        }
    }

    private void registerMovedFromDeleted(File file, String localId)
    {
        for (int i = 0; i < itsDeleted.size(); i++)
        {
            DeletedFile deletedFile = (DeletedFile)itsDeleted.get(i);
            if (deletedFile.localId.equals(localId))
            {
                deletedFile.file = file;
                itsMoved.add(deletedFile);
                itsDeleted.removeElementAt(i);
                break;
            }
        }
    }
    
    private boolean unregisterMoved(String localId)
    {
    	for (int i = 0; i < itsMoved.size(); i++)
    	{
    	    DeletedFile deletedFile = (DeletedFile)itsMoved.get(i);
    		if (localId.equals(deletedFile.localId))
			{
				itsMoved.remove(i);
				return true;
			}
    	}
    	
    	return false;
    }
        
    private void processMovedIds(boolean moved, String localId, String onString)
    {
        //update the hash table, as the item was successfully moved
		if ( (itsMoved == null) || (itsMoved.size() <= 0) )
		{
			if (moved)
				logWarn("Unexpected " + onString + " " + localId + " as empty moved map");
			
			return;
		}
		
		for (int i = 0; i < itsMoved.size(); i++)
    	{
		    DeletedFile deletedFile = (DeletedFile)itsMoved.get(i);
    		if (localId.equals(deletedFile.localId))
    		{
				String newFileKey = buildFileKey(deletedFile.file, deletedFile.file.isDirectory());
                setLocalIdKey(localId, newFileKey);
				
                itsMoved.remove(i);
				break;
    		}
    	}
    }

    private String newLocalId(File file)
    {
        itsLocalIdPrefix++;
        return Long.toString(itsLocalIdPrefix) + "-" + file.getName();
    }

    private void setLocalIdKey(String localId, String fileKey)
    {
        //remove the existing mappings
        String oldFileKey = (String)itsLocalIdKeyMap.get(localId);
        if (oldFileKey != null)
            itsLocalIdKeyMap.remove(oldFileKey);
        itsLocalIdKeyMap.remove(localId);

        //add a new file key if one has been specified
        if ( (fileKey != null) && (fileKey.length() > 0) )
        {
            itsLocalIdKeyMap.put(localId, fileKey);
            itsLocalIdKeyMap.put(fileKey, localId);
        }
    }
    
    void setLocalIdHash(String localId, String newHash)
    {
        //remove the existing hash
        itsLocalIdHashMap.remove(localId);
        Enumeration hashEnum = itsHashLocalIdsMap.keys();
        while (hashEnum.hasMoreElements())
        {
            String hash = (String)hashEnum.nextElement();
            Vector localIds = (Vector)itsHashLocalIdsMap.get(hash);
            if (localIds != null)
            {
                boolean localIdFound = false;
                for (int i = 0; i < localIds.size(); i++)
                {
                    if (localId.equals(localIds.get(i)))
                    {
                        localIds.removeElementAt(i);
                        if (localIds.size() == 0)
                            itsHashLocalIdsMap.remove(hash);
                        localIdFound = true;
                        break;
                    }
                }

                if (localIdFound)
                    break;
            }
        }

        //add a new hash if one has been specified
        if ( (newHash != null) && (newHash.length() > 0) )
        {
            itsLocalIdHashMap.put(localId, newHash);
            Vector localIds = (Vector)itsHashLocalIdsMap.get(newHash);
            if (localIds == null)
            {
                localIds = new Vector();
                localIds.add(localId);
                itsHashLocalIdsMap.put(newHash, localIds);
            }
            else
            {
                int i;
                for (i = 0; i < localIds.size(); i++)
                {
                    if (localId.equals(localIds.get(i)))
                        return;
                }

                if (i >= localIds.size())
                    localIds.addElement(localId);
            }

            return;
        }
    }

    private void updateHashes(File oldFile, File file, String localId, String globalId, boolean isDirectory)
        throws StoreException
    {
        //remove the old mappings if necessary
        String oldFileLocalId = null;
        if (oldFile != null)
        {
            String oldFileKey = buildFileKey(oldFile, isDirectory);
            oldFileLocalId = (String)itsLocalIdKeyMap.get(oldFileKey);
            itsLocalIdKeyMap.remove(oldFileKey);
            
            if (oldFileLocalId != null)
            {
                itsLocalIdKeyMap.remove(oldFileLocalId);
                setLocalIdHash(oldFileLocalId, null);
            }
        }
        
        //add new mappings
        if (file != null)
        {
            //use the old local ID if an explicit one hasn't been specified
            if (localId == null)
            {
                localId = oldFileLocalId;
                if (localId == null)
                    throw new IllegalArgumentException("no local ID specified");
            }
            
            //add the mapping between local ID and the file key
            String fileKey = buildFileKey(file, isDirectory);
            itsLocalIdKeyMap.put(localId, fileKey);
            itsLocalIdKeyMap.put(fileKey, localId);
        
            //add the mapping between the local ID and the global ID
            if ( (globalId != null) && (globalId.length() > 0) )
                itsGlobalIdLocalIdMap.put(globalId, localId);
        
            try
            {
                //add the mapping between the local ID and a hash of the record contents
                setLocalIdHash(localId, getHash(file));
            }
            catch (Throwable e)
            {
                throw new StoreException("Failed to add sync hash", e);
            }
        }
    
        //TODO: handle the case where we're dealing with a folder
    }

    private String getRelativePath(File file)
    {
        int rootPathLength = itsRoot.getPath().length();
        return file.getPath().substring(rootPathLength);
    }
    
    private String buildFileKey(File file, boolean isDirectory)
    {
        int dataPathLength = itsData.getPath().length();
        String prefix = (isDirectory) ? KEY_DIR_PREFIX : KEY_FILE_PREFIX;
        return prefix + file.getPath().substring(dataPathLength);
    }
    
    private File getFileFromKey(String fileKey)
    {
        return new File(itsData, fileKey.substring(KEY_PREFIX_LEN));
    }
    
    private File getFileFromId(String localId, String globalId)
        throws StoreException
    {
        return getFileFromId(localId, globalId, false);
    }
    
    private File getFileFromId(String localId, String globalId, boolean canBeMissing)
        throws StoreException
    {
        //if a global ID is specified, use it to map to the associated local ID
        if (globalId != null)
        {
            String id = (String)itsGlobalIdLocalIdMap.get(globalId);
            if (id != null)
            {
                localId = id;
                logDebug("Global id '" + globalId + "' is specified. Retrieved corresponding local ID '" + localId + "'");
            }
            else
            {
                logDebug("Global id '" + globalId + "' is specified, but no corresponding local ID found");
            }
        }
    
        //make sure the local ID is not empty
        if (localId.length() <= 0)
            throw new StoreException("Unexpected empty local ID");
        
        //check if the ID refers to the root folder
        if (localId.equals(Record.ROOT_ID))
            return itsData;
        
        //map the local ID to a File
        String fileKey = (String)itsLocalIdKeyMap.get(localId);
        if (fileKey == null)
        {
            logDebug("Key not found for ID '" + localId + "', try as a global ID");
            
            // maybe the id is a global id
            String id = (String)itsGlobalIdLocalIdMap.get(localId);
            if (id != null)
            {
                localId = id;
                fileKey = (String)itsLocalIdKeyMap.get(localId);
            }
        }
        
        if ( (fileKey == null) || (fileKey.length() <= 0) )
        {
            logDebug("Key not found for ID '" + localId + "'");
            if (! canBeMissing)
                throw new StoreException("Unknown local ID '" + localId + "'");
            return null;
        }
        
        File file = getFileFromKey(fileKey);
        if ( (file == null) || (! file.exists()) )
        {
            if (! canBeMissing)
                throw new StoreException("File/folder '" + fileKey + "' does not exist");
        }
        
        return file;
    }

    private String getIdFromFile(File file, boolean isDirectory)
    {
        String fileKey = buildFileKey(file, isDirectory);
        String localId = (String)itsLocalIdKeyMap.get(fileKey);
        if (localId == null)
            return Record.ROOT_ID;

        return localId;
    }
    
    private String getMemCardIdFromLocalId(String localId)
        throws StoreException
    {
        return getMemCardIdFromFile(getFileFromId(localId, null, false));
    }
    
    private String getMemCardIdFromFile(File file)
    {
        if (file == null)
            return null;
        
        String filePath = file.getPath();
        if ( (itsMcardData == null) || (! filePath.startsWith(itsMcardData.getPath())) )
            return null;
        
        return itsMcardId;          
    }
    
    private void logError(String message, Throwable cause)
    {
        if (itsLogger != null)
            itsLogger.error("FILE_STORE: " + message, cause);
    }

    private void logDebug(String message)
    {
        if (itsLogger != null)
            itsLogger.debug("FILE_STORE: " + message);
    }

    private void logWarn(String message)
    {
        if (itsLogger != null)
            itsLogger.warn("FILE_STORE: " + message);
    }

    private void logInfo(String message)
    {
        if (itsLogger != null)
            itsLogger.info("FILE_STORE: " + message);
    }
    
    private void dumpTables()
    {
        if (itsLogger == null)
            return;
        
        itsLogger.info("FILE_STORE: Dumping sync state:");
        Enumeration fileKeyEnum = itsLocalIdKeyMap.keys();
        while (fileKeyEnum.hasMoreElements())
        {
            String fileKey = (String)fileKeyEnum.nextElement();
            if (isFileKey(fileKey))
            {
                String localId = (String)itsLocalIdKeyMap.get(fileKey);
                if (localId != null)
                {
                    String hash = (String)itsLocalIdHashMap.get(localId);
                    if (hash != null)
                        itsLogger.info("File key: '" + fileKey + "' -> '" + localId + "' -> '" + hash + "'");
                    else
                        itsLogger.info("File key: '" + fileKey + "' -> '" + localId + "'");
                }
            }
        }
    }
    

    private static String readAnchor(File anchorFile)
        throws IOException
    {
        if (! anchorFile.exists())
            return null;
        
        FileInputStream anchorFileStream = null;
        BufferedReader anchorFileReader = null;
        try
        {
            anchorFileStream = new FileInputStream(anchorFile);
            anchorFileReader = new BufferedReader(new InputStreamReader(anchorFileStream));
            return anchorFileReader.readLine();
        }
        finally
        {
            Utils.streamClose(anchorFileStream);
            Utils.readerClose(anchorFileReader);
        }
    }

    private static void writeAnchor(File anchorFile, String anchor)
        throws IOException
    {
        if (! anchorFile.exists())
            anchorFile.createNewFile();
        
        FileOutputStream anchorFileStream = null;
        try
        {
            anchorFileStream = new FileOutputStream(anchorFile);
            anchorFileStream.write( anchor.getBytes("UTF-8") );
        }
        finally
        {
            Utils.streamClose(anchorFileStream);
        }
    }

    private static Hashtable readHashtable(File file)
        throws StoreException
    {
        FileInputStream fileStream = null;
        ObjectInputStream objectStream = null;
        try
        {
            fileStream = new FileInputStream(file);
            objectStream = new ObjectInputStream(fileStream);

            return (Hashtable)objectStream.readObject();
        }
        catch(Throwable e)
        {
            throw new StoreException("Failed to read hash file '" + file.getPath() + "'" + e);
        }
        finally
        {
            Utils.streamClose(objectStream);
            Utils.streamClose(fileStream);
        }
    }
    
    private static void writeHashtable(File file, Hashtable hashtable)
        throws StoreException
    {
        FileOutputStream fileStream = null;
        ObjectOutputStream objectStream = null;
        try
        {
            fileStream = new FileOutputStream(file);
            objectStream = new ObjectOutputStream(fileStream);
            objectStream.writeObject(hashtable);
        }
        catch(Throwable e)
        {
            throw new StoreException("Failed to write hash file '" + file.getPath() + "'" + e);
        }
        finally
        {
            Utils.streamClose(objectStream);
            Utils.streamClose(fileStream);
        }
    }
    
    private static boolean isFileKey(String string)
    {
        return ( (string.startsWith(KEY_DIR_PREFIX)) || (string.startsWith(KEY_FILE_PREFIX)) );
    }
    
    private static boolean isFileKeyDirectory(String string)
    {
         return (string.startsWith(KEY_DIR_PREFIX));
    }
}
