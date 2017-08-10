/**
 * Copyright � 2004-2011 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


import java.io.*;
import java.util.*;

import android.util.Log;

import net.cp.mtk.common.CommonUtils;
import net.cp.mtk.common.security.MD5;
import net.cp.syncml.client.SyncException;
import net.cp.syncml.client.SyncManager;
import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.Transport;
import net.cp.syncml.client.devinfo.ContentType;
import net.cp.syncml.client.store.AlreadyExistsException;
import net.cp.syncml.client.store.NoSuchRecordException;
import net.cp.syncml.client.store.Record;
import net.cp.syncml.client.store.RecordStore;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.ConsumableStack;
import net.cp.syncml.client.util.CountingOutputStream;
import net.cp.syncml.client.util.Logger;
import net.cp.syncml.client.util.wbxml.Wbxml;
import net.cp.syncml.client.util.wbxml.WbxmlException;
import net.cp.syncml.client.util.wbxml.Codepage;


/**
 * A class representing a sync session. <br/><br/>
 * 
 * This class encapsulates all of the session state information and manages the 
 * Sync protocol via a background thread that is responsible for the sending and 
 * receiving of Sync protocol data.
 *
 * @author Denis Evoy
 */
public class Session implements Runnable
{
    /* A class containing any session state associated with a particular record store. */
    private static class RecordStoreState
    {
        public RecordStore store;                       //the record store that the state refers to
        public long maxRecordSize;                      //maximum allowed record size for the record store
        public boolean sessionSuccess;                  //indicates if the sync session was a success for the record store
        public int sessionStatusCode;                   //the status code indicating the result of the session for the record store
        public String sessionStatusData;                //any additional status data indicating the result of the session for the record store
        public Hashtable outStatusCmds;                 //the status of commands that the client has processed for the record store - a collection of Status objects
        public Hashtable outLocalIds;                   //the local IDs of records that the client has added - a collection of String objects
        public Cmd outSyncCmd;                          //the Sync command that was sent to the server

        
        public RecordStoreState(RecordStore recordStore)
        {
            store = recordStore;
            resetState();
        }
        
        public void resetState()
        {
            sessionSuccess = true;
            sessionStatusCode = 0;
            sessionStatusData = null;
            outStatusCmds = new Hashtable();
            outLocalIds = new Hashtable();
            outSyncCmd = null;
        }
    }
    
    
    //Possible session states
    private static final int STATE_CLIENT_INIT =     1;
    private static final int STATE_SERVER_INIT =     2;
    private static final int STATE_CLIENT_UPDATES =  3;
    private static final int STATE_SERVER_UPDATES =  4;
    private static final int STATE_CLIENT_MAPS =     5;
    private static final int STATE_COMPLETE =        6;
    
    //The encoding to use when handling strings
    private static final String ENCODING_UTF8 =      "UTF-8";
    
    //The prefix used when logging session activity
    private static final String LOG_PREFIX =         "SESSION: ";

    
    //data provided by the application when creating the session
    private SyncManager syncManager;            //the manager controlling the session
    private RecordStore[] syncStores;           //the collection of stores that are to be synced during the session
    private String sessionId;                   //the unique ID of the current session
    private Logger log;                         //the logger used to log activity
    
    //session state information
    private int sessionState;                   //the current state of the sync session
    private long maxMsgSize;                    //maximum allowed message size
    private boolean stopSession;                //indicates if the sync session should be stopped
    private boolean sessionStopped;             //indicates if the sync session is currently stopped
    private int sessionStatusCode;              //the status code indicating the result of the session
    private String sessionStatusData;           //any additional status data indicating the result of the session
    private boolean suspendSession;             //indicates if the sync session should be suspended
    private boolean sessionSuspended;           //indicates if the sync session is currently suspended
    private boolean resumeSession;              //indicates if the sync session should be resumed
    private boolean sessionResumed;             //indicates if the sync session has been resumed
    private int suspendedSessionState;          //the state of the sync session when it was suspended
    private RecordStoreState[] storeStates;     //the collection of sync session states - one for each record store 
    
    //state information used when receiving messages from the server
    private SyncHdr inSyncHeader;               //the Sync header from the last message received
    private Cmd inSyncCmd;                      //the Sync command received from the server
    private int inServerAuthCount;              //the number of times the server has attempted to authenticate with the client
    private Chal inServerChal;                  //the server layer authentication challenge received in the last message
    private SyncItem inChunkedItem;             //the last item chunk received
    private RecordStoreState inActiveStore;     //the record store last referred to in a Status or Sync command
    
    //state information used when sending messages to the server
    private int outMessageId;                   //the ID of the message that is to be sent to the server
    private int outCommandId;                   //the ID of the command that is to be sent to the server
    private Chal outClientChal;                 //the server layer authentication challenge sent to the server
    private Vector outStatusCmds;               //the status of commands that the client has processed - a collection of Status objects
    private Vector outReplyCmds;                //the reply to any commands that the client has processed (only those that require a reply) - a collection of Cmd objects
    private Vector outPendingUpdateCmds;        //the client update commands that were sent to the server and haven't been acknowledged yet - a collection of Cmd objects
    private Cmd outMapCmd;                      //the Map command that is to be sent to the server
    private int outClientAuthCount;             //the number of times the client has attempted to authenticate with the server
    private RecordStoreState outStoreState;     //the record store state whose changes are to be sent to the server
    private ConsumableStack outRecords;         //the records to be sent to the server
    private Record outRecord;                   //the record to be sent to the server (possibly in multiple chunks)
    private long outChunkedBytesSent;           //the number of bytes of the current record that have been sent to the server
    

    /**
     * Creates a new session to sync the specified stores with a remote server.
     * 
     * @param manager   the manager controlling the session.
     * @param stores    the record stores that are to be synced during the session.
     * @param id        the unique ID of the session.
     */
    public Session(SyncManager manager, RecordStore[] stores, String id)
    {
        if (manager == null)
            throw new IllegalArgumentException("no sync manager specified");
        if ( (stores == null) || (stores.length <= 0) )
            throw new IllegalArgumentException("no record stores specified");
        if ( (id == null) || (id.length() <= 0) )
            throw new IllegalArgumentException("no session ID specified");
        
        syncManager = manager;
        syncStores = stores;
        sessionId = id;
        log = manager.getSyncLogger();
        
        storeStates = new Session.RecordStoreState[ stores.length ];
        for (int i = 0; i < stores.length; i++)
            storeStates[i] = new Session.RecordStoreState(stores[i]);
    }

    
    /* Reads the next SyncML message from the server via the specified input stream. */
    private void readMessage(InputStream inputStream)
        throws SyncException, IOException
    {
        if (log != null)
            log.info(LOG_PREFIX + "Reading message from the server");
        
        try
        {
            //parse the WBXML response using the SyncML codepages
            MetInfCodepage cpMetinf = new MetInfCodepage(log, this);
            SyncMLCodepage cpSyncml = new SyncMLCodepage(log, this, cpMetinf);
            Wbxml.parse(inputStream, new Codepage[] { cpSyncml, cpMetinf } );
        }
        catch (WbxmlException e)
        {
            throw new SyncException("failed to parse WBXML message", e);
        }
    }
    
    /* Processes the end of a SyncML package received from the server. */
    public void onSyncPkgEnd()
    {
        if (log != null)
            log.info(LOG_PREFIX + "Received end of package from the server - updating session state");
        
        //update the session state as appropriate
        if (sessionState == Session.STATE_SERVER_INIT)
        {
            //server initialization has finished - change state so we send client updates to the server
            sessionState = Session.STATE_CLIENT_UPDATES;
            if (log != null)
                log.info(LOG_PREFIX + "Server initialization completed - sending client updates to server in next message");
        }
        else if (sessionState == Session.STATE_SERVER_UPDATES)
        {
            //finished receiving server updates - change state so we send maps back to the server
            sessionState = Session.STATE_CLIENT_MAPS;
            if (log != null)
                log.info(LOG_PREFIX + "Server updates received - sending maps to the server in next message");
        }
        else if (sessionState == Session.STATE_CLIENT_MAPS)
        {
            //finished sending maps back to the server - change state to indicate that session is complete
            sessionState = Session.STATE_COMPLETE;
            if (log != null)
                log.info(LOG_PREFIX + "Session complete");
        }
    }

    /* Processes the SyncML header received from the server. */
    public void onSyncHeader(SyncHdr header)
    {
        if (log != null)
            log.info(LOG_PREFIX + "Received SyncML header from the server");

        //make sure the message is for the correct device
        if ( (header.targetUri == null) || (! header.targetUri.equals(syncManager.getDevice().getDeviceID())) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "Missing or invalid target URI in SyncML header: " + header.targetUri, null);
            stopSession(SyncML.STATUS_BAD_REQUEST, null);
            return;
        }
        
        //make sure the session ID is correct
        if ( (header.sessionId == null) || (! header.sessionId.equals(sessionId)) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "Missing or invalid session ID in SyncML header: " + header.sessionId, null);
            stopSession(SyncML.STATUS_BAD_REQUEST, null);
            return;
        }
        
        //handle any meta information
        if (header.metinf != null)
        {
            //handle the max message size
            if ( (header.metinf.maxMsgSize > 0) && (header.metinf.maxMsgSize < maxMsgSize) )
                maxMsgSize = header.metinf.maxMsgSize;
            if(log!= null)
            	log.debug(LOG_PREFIX + "SyncML Maximum message size is: " + maxMsgSize);
            
            //handle any EMI extensions
            if ( (header.metinf.emiExtensions != null) && (header.metinf.emiExtensions.size() > 0) )
            {
                String[] extensions = new String[ header.metinf.emiExtensions.size() ];
                for (int i = 0; i < header.metinf.emiExtensions.size(); i++)
                    extensions[i] = (String)header.metinf.emiExtensions.elementAt(i);
                
                //notify all stores of the extensions
                for (int i = 0; i < syncStores.length; i++)
                    syncStores[i].setMetaInfoExtensions(extensions);
            }
        }
        
        //handle any credentials that were sent by the server
        int statusCode = SyncML.STATUS_OK;
        if (header.credentials != null)
        {
            inServerAuthCount++;

            //indicate that the client nonce (which was sent in the previous message) has been received 
            //by the server - we do this even if the server credentials are invalid
            if (outClientChal != null)
                syncManager.getDevice().onClientNonceSent();
            
            //determine the username/password to use when validating the server credentials
            String password = syncManager.getAuthPasword();
            String username = syncManager.getAuthUsername();
            if ( (header.sourceName != null) && (header.sourceName.length() > 0) )
            {
                //if the server provided a username, use that instead - for example, the CP SyncML server will provide the 
                //users real username, if the original username provided by the client was an alias
                username = header.sourceName; 
            }

            //validate the server credentials
            if (! isCredentialsValid(header.credentials, outClientChal, username, password))
            {
                if (inServerAuthCount >= 2)
                {
                    //the server credentials are invalid - stop the session as we've attempted to authenticate enough times
                    if (log != null)
                        log.error(LOG_PREFIX + "Server layer authentication failed - server credentials are invalid", null);
                    statusCode = SyncML.STATUS_INVALID_CREDENTIALS;
                    stopSession(statusCode, null);
                }
                else
                {
                    //the server credentials are invalid - ask the server to authenticate again
                    if (log != null)
                        log.error(LOG_PREFIX + "Server layer authentication failed - requesting new credentials from the server", null);
                    statusCode = SyncML.STATUS_INVALID_CREDENTIALS;
                    outClientChal = null;
                    
                    //reset the state so we send our initialization package again
                    if (sessionState == Session.STATE_SERVER_INIT)
                        sessionState = Session.STATE_CLIENT_INIT;
                }
            }
            else
            {
                //the server credentials are correct - use status code 212 to indicate that further 
                //credentials are not required
                if (log != null)
                    log.info(LOG_PREFIX + "Server layer authentication accepted - server credentials are correct");
                statusCode = SyncML.STATUS_AUTH_ACCEPTED;
            }
        }

        //store the header so we can reference it later
        inSyncHeader = header;
        
        //set the status to return for the header
        addOutgoingStatus(null, newStatus(statusCode));
    }
    
    /* Store the challenge and set the last nonce in the device */
    private void setChallenge(Chal challenge)
    {
        inServerChal = challenge;
        if (challenge.metinf != null && challenge.metinf.nextNonce != null)
            syncManager.getDevice().setServerNonce(challenge.metinf.nextNonce);
    }
    
    /* Processes the servers status of a SyncML command previously sent to the server. */
    public void onSyncStatus(Status status)
    {
        if (log != null)
            log.info(LOG_PREFIX + "Received status of '" + status.statusCode + "' for command '" + status.refCommand + "' from the server");

        //retrieve the relevant info
        String origCmd = status.refCommand;
        int statusCode = status.statusCode;

        String sourceUri = null;
        if ( (status.refItemSourceUris != null) && (status.refItemSourceUris.size() > 0) )
            sourceUri = (String)status.refItemSourceUris.elementAt(0);
        
        String targetUri = null;
        if ( (status.refItemTargetUris != null) && (status.refItemTargetUris.size() > 0) )
            targetUri = (String)status.refItemTargetUris.elementAt(0);
        
        //retrieve any additional status data (e.g. reason code, etc)
        String statusData = null;
        if ( (status.items != null) && (status.items.size() > 0) )
        {
            byte[] data = ((SyncItem)status.items.elementAt(0)).data;
            if ( (data != null) && (data.length > 0) )
            {
                try
                {
                    statusData = new String(data, "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    //ignore
                }
            }
        }
        
        if (origCmd.equals("SyncHdr"))
        {
            if (statusCode == SyncML.STATUS_OK)
            {
                //server authentication success - reuse existing challenge unless a new one was received
                if (status.challenge != null)
                    setChallenge(status.challenge);
                return;
            }
            else if (statusCode == SyncML.STATUS_AUTH_ACCEPTED)
            {
                //server authentication success - no further authentication required unless a new challenge
                //was received
                inServerChal = null;
                if (status.challenge != null)
                    setChallenge(status.challenge);
                return;
            }
            else if ( (statusCode == SyncML.STATUS_INVALID_CREDENTIALS) || (statusCode == SyncML.STATUS_MISSING_CREDENTIALS) )
            {
                //server authentication failure - nothing more to do if we've attempted to authenticate a sufficient number of times - we always
                //attempt to authenticate at least twice as the server challenge we used may have been out of date
                if ( (statusCode == SyncML.STATUS_INVALID_CREDENTIALS) && (outClientAuthCount >= 2) )
                {
                    //set the challenge anyway to keep track of the last nonce
                    if (status.challenge != null)
                        setChallenge(status.challenge);
                    
                    if (log != null)
                        log.error(LOG_PREFIX + "Server layer authentication failed - credentials rejected by the server", null);
                    stopSession(SyncML.STATUS_INVALID_CREDENTIALS, null);
                    return;
                }
                
                //obtain the challenge
                if (status.challenge == null)
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Server layer authentication failed - no challenge received from the server", null);
                    stopSession(SyncML.STATUS_BAD_REQUEST, null);
                    return;
                }
                
                //set the server challenge so the credentials can be sent in the next message
                setChallenge(status.challenge);
                if (log != null)
                    log.info(LOG_PREFIX + "Server layer authentication required - challenge accepted from the server");
                
                //reset the state so we send our initialization package again
                if (sessionState == Session.STATE_SERVER_INIT)
                    sessionState = Session.STATE_CLIENT_INIT;
                
                return;
            }
            else if (! SyncML.isSuccessStatus(statusCode))
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Server rejected SyncML header", null);
                stopSession(statusCode, null);
                return;
            }
        }        
        else if (origCmd.equals(Cmd.CMD_ALERT))
        {
            if ( (statusCode == SyncML.STATUS_INVALID_CREDENTIALS) || (statusCode == SyncML.STATUS_MISSING_CREDENTIALS) )
            {
                //ignore authentication errors as they are handled in SyncHdr 
            }
            else
            {
                //identify the referenced record store based on the source URI (if any)
                RecordStoreState storeState = null;
                if ( (sourceUri != null) && (sourceUri.length() > 0) )
                    storeState = getRecordStore(sourceUri);
                
                //check if we were trying to suspend the session and indicate if it is now suspended
                if (suspendSession)
                {
                    onSuspendResult(statusCode, statusData);
                    return;
                }
                
                //check if we were trying to resume the session and indicate if it is now resumed
                if (resumeSession)
                {
                    onResumeResult(sourceUri, statusCode, statusData);
                    return;
                }
                
                //check if the server has requested a slow sync 
                if (statusCode == SyncML.STATUS_REFRESH_REQUIRED)
                {
                    if (storeState != null)
                    {
                        if (log != null)
                            log.info(LOG_PREFIX + "Server has requested a slow sync for record store '" + storeState.store.getClientURI() + "'");
                        storeState.store.setSyncType(SyncML.SYNC_TYPE_TWO_WAY_SLOW);
                    }
                    
                    return;
                }
                
                //handle the case where the alert was rejected
                if (! SyncML.isSuccessStatus(statusCode))
                {
                    if (storeState != null)
                    {
                        //only stop the session for the referenced record store
                        if (log != null)
                            log.info(LOG_PREFIX + "Server rejected Alert command for record store '" + storeState.store.getClientURI() + "'");
                        stopSession(storeState, statusCode, statusData);
                    }
                    else
                    {
                        //stop the session for all stores
                        if (log != null)
                            log.error(LOG_PREFIX + "Server rejected Alert command", null);
                        stopSession(statusCode, statusData);
                    }
                    
                    return;
                }
            }
        }
        else if (origCmd.equals(Cmd.CMD_ADD))
        {
            //handle the result of the update and check if the update is complete 
            if (! onClientUpdateResult(sourceUri, status))
                return;

            //indicates the servers final response to an add request
            syncManager.getSyncListener().onAddResult(statusCode, statusData);
            if (inActiveStore != null)
                inActiveStore.store.onAddResult(sourceUri, statusCode, statusData);
        }
        else if (origCmd.equals(Cmd.CMD_REPLACE))
        {
            //handle the result of the update and check if the update is complete 
            if (! onClientUpdateResult(sourceUri, status))
                return;

            //indicates the servers final response to a replace request
            syncManager.getSyncListener().onReplaceResult(statusCode, statusData);
            if (inActiveStore != null)
                inActiveStore.store.onReplaceResult(sourceUri, statusCode, statusData);
        }
        else if (origCmd.equals(Cmd.CMD_DELETE))
        {
            //handle the result of the update and check if the update is complete 
            if (! onClientUpdateResult(sourceUri, status))
                return;

            //indicates the servers response to a delete request
            syncManager.getSyncListener().onDeleteResult(statusCode, statusData);
            if (inActiveStore != null)
                inActiveStore.store.onDeleteResult(sourceUri, statusCode, statusData);
        }
        else if (origCmd.equals(Cmd.CMD_MOVE))
        {
            //handle the result of the update and check if the update is complete 
            if (! onClientUpdateResult(sourceUri, status))
                return;

            //indicates the servers response to a move request
            syncManager.getSyncListener().onMoveResult(statusCode, statusData);
            if (inActiveStore != null)
                inActiveStore.store.onMoveResult(sourceUri, statusCode, statusData);
        }
        else if (origCmd.equals(Cmd.CMD_COPY))
        {
            //handle the result of the update and check if the update is complete 
            if (! onClientUpdateResult(sourceUri, status))
                return;

            //indicates the servers response to a copy request
            syncManager.getSyncListener().onCopyResult(statusCode, statusData);
            if (inActiveStore != null)
                inActiveStore.store.onCopyResult(sourceUri, targetUri, statusCode, statusData);
        }
        else if (origCmd.equals(Cmd.CMD_SYNC))
        {
            //indicates the response to a Sync command - remember the associated store so we can refer to it later
            inActiveStore = getRecordStore(sourceUri);
            if (inActiveStore == null)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Failed to identify local record store with URI '" + sourceUri + "'", null);
                stopSession(SyncML.STATUS_NOT_FOUND, null);
                return;
            }
            
            //stop the session for the store if the Sync command was rejected by the server
            if (! SyncML.isSuccessStatus(statusCode))
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Server rejected Sync command for record store");
                stopSession(inActiveStore, statusCode, statusData);
                return;
            }
        }
        else if (origCmd.equals(Cmd.CMD_MAP))
        {
            //handle the result of the Map 
            if (! SyncML.isSuccessStatus(statusCode))
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Server rejected Map command");
                return;
            }
        }
    }    

    /* Processes the SyncML command received from the server. */
    public void onSyncCommand(Cmd command)
    {
        if (log != null)
            log.info(LOG_PREFIX + "Received '" + command.command + "' command from the server");

        Status status = null;
        if (command.command.equals(Cmd.CMD_ADD))
        {
            status = doAddCmd(command);
            
            //do not notify intermediate chunks
            if (status.statusCode != SyncML.STATUS_DATA_CHUNK_ACCEPTED)
            	syncManager.getSyncListener().onAddRequest(status.statusCode);
        }
        else if (command.command.equals(Cmd.CMD_ALERT))
        {
            status = doAlertCmd(command);
        }
        else if (command.command.equals(Cmd.CMD_COPY))
        {
            status = doCopyCmd(command);
            syncManager.getSyncListener().onCopyRequest(status.statusCode);
        }
        else if (command.command.equals(Cmd.CMD_DELETE))
        {
            status = doDeleteCmd(command);
            syncManager.getSyncListener().onDeleteRequest(status.statusCode);
        }
        else if (command.command.equals(Cmd.CMD_GET))
        {
            status = doGetCmd(command);
        }
        else if (command.command.equals(Cmd.CMD_MAP))
        {
            if (log != null)
                log.warn(LOG_PREFIX + "Ignoring Map command");
            status = newStatus(SyncML.STATUS_OK);
        }
        else if (command.command.equals(Cmd.CMD_MOVE))
        {
            status = doMoveCmd(command);
            syncManager.getSyncListener().onMoveRequest(status.statusCode);
        }
        else if (command.command.equals(Cmd.CMD_PUT))
        {
            status = doPutCmd(command);
        }
        else if (command.command.equals(Cmd.CMD_REPLACE))
        {
            status = doReplaceCmd(command);
            
            //do not notify intermediate chunks
            if (status.statusCode != SyncML.STATUS_DATA_CHUNK_ACCEPTED)
            	syncManager.getSyncListener().onReplaceRequest(status.statusCode);
        }
        else if (command.command.equals(Cmd.CMD_SYNC))
        {
            status = doSyncCmdStart(command);
        }
        else
        {
            if (log != null)
                log.error(LOG_PREFIX + "Unsupported command: " + command.command, null);
            status = newStatus(SyncML.STATUS_NOT_SUPPORTED);
        }
        
        //set the status to return for the command
        addOutgoingStatus(command, status);
    }
    
    /* Processes the end of the SyncML command received from the server. */
    public void onSyncCommandEnd(Cmd command)
    {
        if (command.command.equals(Cmd.CMD_SYNC))
        {
            doSyncCmdEnd(command);
        }
        else
        {
            if (log != null)
                log.error(LOG_PREFIX + "Unsupported command: " + command.command, null);
        }
    }
    
    /* Processes the SyncML 'Add' command received from the server. */
    private Status doAddCmd(Cmd command)
    {
        Status result = null;
        String localId = null;
        SyncItem item = null;
        try
        {
            //make sure the user hasn't aborted the session
            if (isCancelled())
            {
                result = doCmdError(command, "Session aborted by the user - skipping command", SyncML.STATUS_OPERATION_CANCELLED, null, null);
                return result;
            }
            
            //make sure it's part of a Sync command
            if (inSyncCmd == null)
            {
                result = doCmdError(command, "Add command is not part of a valid Sync command", SyncML.STATUS_BAD_REQUEST, null, null);
                return result;
            }
            
            //make sure exactly one item is specified to be added
            if ( (command.items == null) || (command.items.size() != 1) )
            {
                result = doCmdError(command, "Invalid number of items to add", SyncML.STATUS_BAD_REQUEST, null, null);
                return result;
            }
            
            //get and handle the command item
            item = (SyncItem)command.items.elementAt(0);
            int itemStatus = doCmdItem(command, item);
            if (itemStatus != 0)
            {
                if (! SyncML.isSuccessStatus(itemStatus))
                    result = doCmdError(command, "Error accepting command item", itemStatus, null, item);
                else
                    result = newStatus(itemStatus);
                
                return result;
            }
            
            //if this is the first (or only) chunk, notify the store that we are starting to add a record
            if (inChunkedItem == null)
            {
                ContentType ctType = null;                 
                if ( (command.metinf != null) && (command.metinf.contentType != null) )
                    ctType = new ContentType(command.metinf.contentType);
                inActiveStore.store.addRecordBegin(item.targetParentUri, item.sourceParentUri, item.sourceUri, ctType);
            }
 
            //pass the data to the record store
            inChunkedItem = item;
            inActiveStore.store.addRecordData(item.data);
            
            //nothing more to do if there is more data to receive - also note that we don't cache the 
            //status as it is not the final status for the item 
            if (item.moreData)
            {
                if (log != null)
                    log.debug(LOG_PREFIX + "Accepted data chunk");
                return newStatus(SyncML.STATUS_DATA_CHUNK_ACCEPTED);
            }
 
            int statusCode;
            inChunkedItem = null;
            try
            {
                //add the record to the record store
                localId = inActiveStore.store.addRecordEnd(true);
                statusCode = SyncML.STATUS_ITEM_ADDED;
            }
            catch (AlreadyExistsException e)
            {
                localId = e.getLocalId();
                if (log != null)
                    log.warn(LOG_PREFIX + "Record already exists in the local store: " + localId);
                statusCode = SyncML.STATUS_ITEM_ALREADY_EXISTS;
            }
            
            //make sure a local ID has been assigned to the new record
            if ( (localId == null) || (localId.length() <= 0) )
            {
                result = doCmdError(command, "No local ID returned for new record item '" + item.sourceUri + "'", SyncML.STATUS_DATA_STORE_FAILURE, null, item);
                return result;
            }

            //add a Map for the new record (to map its global ID to the local ID)
            addOutgoingMap(item, localId);
         
            result = newStatus(statusCode);
            return result;
        }
        catch (StoreException e)
        {
            result = doCmdError(command, "Error accessing local record store '" + inActiveStore.store.getClientURI() + "'", SyncML.STATUS_DATA_STORE_FAILURE, e, item);
            return result;
        }
        finally
        {
            //cache the result of this command in case we need resend it later (which can occur after a suspend/resume)
            if ( (item != null) && (result != null) )
            {
                String itemUri = item.getUri();
                if ( (itemUri != null) && (itemUri.length() > 0) )
                {
                    inActiveStore.outStatusCmds.put(itemUri, new Integer(result.statusCode));
                    if ( (localId != null) && (localId.length() > 0) )
                        inActiveStore.outLocalIds.put(itemUri, localId);
                }
            }
        }
    }
    
    /* Processes the Item associated with a command received from the server. */
    private int doCmdItem(Cmd cmd, SyncItem item)
    {
        //make sure a source or target URI is specified which identifies the item
        String itemUri = item.getUri();
        if ( (itemUri == null) || (itemUri.length() <= 0) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "No source/target URI specified for command item", null);
            return SyncML.STATUS_BAD_REQUEST;
        }
        
        if (log != null)
            log.debug(LOG_PREFIX + "Processing data chunk for item '" + itemUri + "'");
        
        //check the content type (if specified)
        if ( (cmd.metinf != null) && (cmd.metinf.contentType != null) && (! ContentType.isValidContentType(cmd.metinf.contentType)) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "Invalid content type found: " + cmd.metinf.contentType, null);
            return SyncML.STATUS_BAD_REQUEST;
        }
        
        //check if this is a resent chunk - this can only occur after a suspend/resume and is indicated
        //by a chunk with no more data to come and an explicit size specified. This situation is only 
        //allowed (according to the sync specification) when resuming a chunked item.
        if ( (sessionResumed) && (!item.moreData) && (cmd.metinf != null) && (cmd.metinf.size > 0) )
        {
            //check if we already have a status for this record
            Integer previousStatus = (Integer)inActiveStore.outStatusCmds.get(itemUri);
            if (previousStatus != null)
            {
                //indicates that we have already processed this item - return the previous status code
                int statusCode = previousStatus.intValue();
                if (log != null)
                    log.debug(LOG_PREFIX + "Detected a resend of the last chunk of the already processed item '" + itemUri + "' - returning previous status code '" + statusCode + "'");
                
                //if we're dealing with a resent Add command, we need to also return the previous Map (if there was one)
                if ( (cmd.command.equals(Cmd.CMD_ADD)) && (SyncML.isSuccessStatus(statusCode)) )
                {
                    String localId = (String)inActiveStore.outLocalIds.get(itemUri);
                    if (localId != null)
                    {
                        if (log != null)
                            log.debug(LOG_PREFIX + "Returning previous Map for local ID '" + localId + "'");
                        addOutgoingMap(item, localId);
                    }
                }
                
                return statusCode;
            }
        }

        //process the chunk normally
        if (inChunkedItem == null)
        {
            //indicates that this is the first chunk of the item
            if (item.moreData)
            {
                //there are more chunks to receive - make sure the total size is specified
                if ( (cmd.metinf == null) || (cmd.metinf.size <= 0) )
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Missing or invalid size information for first chunk", null);
                    return SyncML.STATUS_SIZE_REQUIRED;
                }
                
                //make sure the total size doesn't exceed the max allowed
                if ( (inActiveStore.maxRecordSize > 0) && (cmd.metinf.size > inActiveStore.maxRecordSize) )
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Total item size is too large ('" + cmd.metinf.size + "' bytes) - max size is '" + inActiveStore.maxRecordSize + "' bytes", null);
                    return SyncML.STATUS_SIZE_TOO_LARGE;
                }
                
                item.totalSize = cmd.metinf.size;
                item.chunkedBytesReceived = item.data.length;
            }
        }
        else
        {
            //this is an additional chunk - make sure it's for the same item as previous chunks
            if (! itemUri.equals(inChunkedItem.getUri()))
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Chunk for item doesn't match existing chunked item '" + inChunkedItem.getUri() + "'", null);
                addOutgoingAlert(SyncML.ALERT_NO_END_OF_DATA, inChunkedItem.sourceUri, inChunkedItem.targetUri);
                return SyncML.STATUS_BAD_REQUEST;
            }
            
            //check if we are resuming the reception of a data chunk - if so, validate the specified remaining size
            if ( (sessionResumed) && (cmd.metinf != null) && (cmd.metinf.size > 0) )
            {
                long resumedTotalSize = cmd.metinf.size + inChunkedItem.chunkedBytesReceived;
                if (resumedTotalSize == inChunkedItem.totalSize)
                {
                    //indicates that this is the next chunk - handle it as normal
                    if (log != null)
                        log.debug(LOG_PREFIX + "Detected the next data chunk of resumed item");
                }
                else if (resumedTotalSize < inChunkedItem.totalSize)
                {
                    //indicates that we will never receive the expected amount of data
                    if (log != null)
                        log.error(LOG_PREFIX + "Chunk size mismatch for resumed item - total resumed size is " + resumedTotalSize + " bytes, expected at least " + inChunkedItem.totalSize + " bytes", null);
                    return SyncML.STATUS_DATA_CHUNK_SIZE_MISMATCH;
                }
                else if (resumedTotalSize > inChunkedItem.totalSize)
                {
                    //indicate that we have already received some or all of this chunk
                    if (log != null)
                        log.debug(LOG_PREFIX + "Detected a resent data chunk of size " + item.data.length + " bytes for resumed item");
                    
                    //make sure that this chunk matches at least some of the last one received 
                    int validateSize = Math.min(inChunkedItem.data.length, item.data.length);
                    if (! isDataEqual(inChunkedItem.data, 0, item.data, 0, validateSize))
                    {
                        if (log != null)
                            log.error(LOG_PREFIX + "Resent data chunk does not match original chunk", null);
                        return SyncML.STATUS_BAD_REQUEST;
                    }
                    
                    //handle the case where the resent chunk isn't exactly the same size as the original chunk
                    if (item.data.length <= inChunkedItem.data.length)
                    {
                        //resent chunk is smaller than before - ignore this chunk along with some of the next one
                        inChunkedItem.discardCount = inChunkedItem.data.length - item.data.length;
                        if (log != null)
                            log.debug(LOG_PREFIX + "Ignoring the resent data chunk and " + inChunkedItem.discardCount + " bytes of the next chunk");
                        return SyncML.STATUS_DATA_CHUNK_ACCEPTED;
                    }
                    else if (item.data.length > inChunkedItem.data.length)
                    {
                        //resent chunk is larger than before - ignore what we have already received and handle the remainder as normal 
                        inChunkedItem.discardCount = inChunkedItem.data.length;
                        if (log != null)
                            log.debug(LOG_PREFIX + "Ignoring " + item.discardCount + " bytes of the resent data chunk");
                    }
                }
            }
            
            //discard some of the data if required
            if (inChunkedItem.discardCount > 0)
            {
                if (log != null)
                    log.debug(LOG_PREFIX + "Discarding " + inChunkedItem.discardCount + " bytes of data chunk (data already received)");
                
                //only discard the data if it is exactly the same as the data we have already received
                if (! isDataEqual(inChunkedItem.data, inChunkedItem.data.length - inChunkedItem.discardCount, item.data, 0, inChunkedItem.discardCount))
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Attempting to discard data that does not match the original data", null);
                    return SyncML.STATUS_BAD_REQUEST;
                }

                //create an new array containing all but the discarded data
                int remainingDataSize = item.data.length - inChunkedItem.discardCount;
                byte[] remainingData = new byte[remainingDataSize];
                System.arraycopy(item.data, inChunkedItem.discardCount, remainingData, 0, remainingDataSize);
                item.data = remainingData;
                inChunkedItem.discardCount = 0;
            }

            //update how much data we have received so far
            item.totalSize = inChunkedItem.totalSize;
            item.chunkedBytesReceived = inChunkedItem.chunkedBytesReceived + item.data.length;
            
            //if this is the last chunk, make sure we have received what we were expecting
            if (! item.moreData)
            {
                if (item.chunkedBytesReceived != item.totalSize)
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Chunk size mismatch for item - received " + item.chunkedBytesReceived + " bytes, expected " + item.totalSize + " bytes", null);
                    return SyncML.STATUS_DATA_CHUNK_SIZE_MISMATCH;
                }
            }
        }
        
        return 0;
    }

    /* Handle any errors that occurred when processing a Sync command received from the server. */
    private Status doCmdError(Cmd command, String errorString, int statusCode, SyncException cause, SyncItem item)
    {
        //check if we were handling chunked items
        if (inChunkedItem != null)
        {
            //cleanup if the new chunk applies to the same record as the existing chunk
            if ( (item == null) || (item.getUri() == null) || (item.getUri().equals(inChunkedItem.getUri())) )
            {
                try
                {
                    //cancel the operation 
                    if (command.command.equals(Cmd.CMD_ADD))
                        inActiveStore.store.addRecordEnd(false);
                    else if (command.command.equals(Cmd.CMD_REPLACE))
                        inActiveStore.store.replaceRecordEnd(false);
                }
                catch (StoreException e)
                {
                    //ignore
                }
    
                inChunkedItem = null;
            }
        }
        
        Status status = newStatus(statusCode, cause);
        if (log != null)
            log.error(LOG_PREFIX + errorString + " - status code '" + status.statusCode + "'", cause);
        return status;
    }

    /* Processes the SyncML 'Alert' command received from the server. */
    private Status doAlertCmd(Cmd command)
    {
        //make sure the user hasn't aborted the session
        if (isCancelled())
        {
            if (log != null)
                log.error(LOG_PREFIX + "Session aborted by the user - skipping command", null);
            return newStatus(SyncML.STATUS_OPERATION_CANCELLED);
        }
        
        //make sure exactly one item is specified
        if ( (command.items == null) || (command.items.size() != 1) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "Invalid number of items for alert", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }
        
        //get the item
        SyncItem item = (SyncItem)command.items.elementAt(0);

        //handle a request to display some data to the user
        if (command.alertCode == SyncML.ALERT_DISPLAY)
            return doAlertDisplay(item.data);
        
        //make sure a target URI is specified
        if ( (item.targetUri == null) || (item.targetUri.length() <= 0) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "No target URI specified for Alert item", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }
        
        if ( (command.alertCode >= SyncML.ALERT_SYNC_CLIENT_TWO_WAY) && (command.alertCode <= SyncML.ALERT_SYNC_SERVER_REFRESH_TO_CLIENT) )
        {
            //identify the referenced record store based on the target URI
            RecordStoreState storeState = getRecordStore(item.targetUri);
            if (storeState == null)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Failed to identify local record store with URI '" + item.sourceUri + "'", null);
                return newStatus(SyncML.STATUS_NOT_FOUND);
            }
            
            //map the alert code to a sync type and notify the record store
            int syncType = SyncML.alertToSyncType(command.alertCode);
            if (syncType > 0)
                storeState.store.setSyncType(syncType);

            //handle the max object size (if specified)
            if ( (command.metinf != null) && (command.metinf.maxObjSize > 0) && (command.metinf.maxObjSize < storeState.maxRecordSize) )
                storeState.maxRecordSize = command.metinf.maxObjSize;
            
            return newStatus(SyncML.STATUS_OK);
        }
        else if (command.alertCode == SyncML.ALERT_NO_END_OF_DATA)
        {
            onClientUpdateResult(item.targetUri, null);
            return newStatus(SyncML.STATUS_OK);
        }
        else
        {
            if (log != null)
                log.error(LOG_PREFIX + "Unsupported alert code: " + command.alertCode, null);
            return newStatus(SyncML.STATUS_NOT_SUPPORTED);
        }
    }
    
    /* Processes the request to display the specified data to the user. */
    private Status doAlertDisplay(byte[] displayData)
    {
        try
        {
            //pass the request on to the listener
            syncManager.getSyncListener().onDisplayRequest(displayData);
            return newStatus(SyncML.STATUS_OK);
        }
        catch (SyncException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Error displaying data to the user", e);
            return newStatus(SyncML.STATUS_COMMAND_FAILED, e);
        }
    }
    
    /* Processes the SyncML 'Copy' command received from the server. */
    private Status doCopyCmd(Cmd command)
    {
        try
        {
            //make sure the user hasn't aborted the session
            if (isCancelled())
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Session aborted by the user - skipping command", null);
                return newStatus(SyncML.STATUS_OPERATION_CANCELLED);
            }

            //make sure we were not in the middle of receiving a chunked item
            if (inChunkedItem != null)
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Failed to received complete data for chunked item '" + inChunkedItem.getUri() + "'");
                addOutgoingAlert(SyncML.ALERT_NO_END_OF_DATA, inChunkedItem.sourceUri, inChunkedItem.targetUri);
                inChunkedItem = null;
            }

            //make sure it's part of a Sync command
            if (inSyncCmd == null)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Copy command is not part of a valid Sync command", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            //make sure exactly one item is specified to be copied
            if ( (command.items == null) || (command.items.size() != 1) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Invalid number of items to copy", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }

            //get the item and make sure a target URI is specified
            SyncItem item = (SyncItem)command.items.elementAt(0);
            if ( (item.targetUri == null) || (item.targetUri.length() <= 0) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "No target URI specified for Copy item", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            int statusCode;
            String localId;
            try
            {
                //copy the item in the record store 
                localId = inActiveStore.store.copyRecord(item.targetUri, item.targetParentUri, item.sourceParentUri, item.data);
                statusCode = SyncML.STATUS_ITEM_ADDED;

            }
            catch (AlreadyExistsException e)
            {
                localId = e.getLocalId();
                if (log != null)
                    log.warn(LOG_PREFIX + "Record already exists in the local store: " + localId);
                statusCode = SyncML.STATUS_ITEM_ALREADY_EXISTS;
            }
            
            //make sure a local ID has been assigned to the copied record
            if ( (localId == null) || (localId.length() <= 0) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "No local ID returned for copied record item '" + item.sourceUri + "'", null);
                return newStatus(SyncML.STATUS_DATA_STORE_FAILURE);
            }

            //add a Map for the copied record (to map its global ID to the local ID)
            addOutgoingMap(item, localId);
            
            return newStatus(statusCode);
        }
        catch (NoSuchRecordException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Record not found in the local store", e);
            return newStatus(SyncML.STATUS_ITEM_GONE);
        }
        catch (StoreException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Error accessing local record store '" + inActiveStore.store.getClientURI() + "'", e);
            return newStatus(SyncML.STATUS_DATA_STORE_FAILURE, e);
        }
    }
    
    /* Processes the SyncML 'Delete' command received from the server. */
    private Status doDeleteCmd(Cmd command)
    {
        try
        {
            //make sure the user hasn't aborted the session
            if (isCancelled())
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Session aborted by the user - skipping command", null);
                return newStatus(SyncML.STATUS_OPERATION_CANCELLED);
            }

            //make sure we were not in the middle of receiving a chunked item
            if (inChunkedItem != null)
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Failed to received complete data for chunked item '" + inChunkedItem.getUri() + "'");
                addOutgoingAlert(SyncML.ALERT_NO_END_OF_DATA, inChunkedItem.sourceUri, inChunkedItem.targetUri);
                inChunkedItem = null;
            }

            //reject a soft deletes as we do not support them 
            if (command.softDelete)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Soft delete is not supported", null);
                return newStatus(SyncML.STATUS_NOT_SUPPORTED);
            }

            //make sure it's part of a Sync command
            if (inSyncCmd == null)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Delete command is not part of a valid Sync command", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            //make sure exactly one item is specified to be deleted
            if ( (command.items == null) || (command.items.size() != 1) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Invalid number of items to delete", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            //get the item and make sure a target URI is specified
            SyncItem item = (SyncItem)command.items.elementAt(0);
            if ( (item.targetUri == null) || (item.targetUri.length() <= 0) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "No target URI specified for Delete item", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            int statusCode;
            try
            {
                //delete the record from the record store
                inActiveStore.store.deleteRecord(item.targetUri);
                if (command.archivedDelete)
                    statusCode = SyncML.STATUS_DELETE_WITHOUT_ARCHIVE;
                else
                    statusCode = SyncML.STATUS_OK;
            }
            catch (NoSuchRecordException e)
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Record not found in the local store: " + item.targetUri);
                statusCode = SyncML.STATUS_ITEM_NOT_DELETED;
            }

            return newStatus(statusCode);
        }
        catch (StoreException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Error accessing local record store '" + inActiveStore.store.getClientURI() + "'", e);
            return newStatus(SyncML.STATUS_DATA_STORE_FAILURE, e);
        }
    }
    
    /* Processes the SyncML 'Get' command received from the server. */
    private Status doGetCmd(Cmd command)
    {
        //make sure the user hasn't aborted the session
        if (isCancelled())
        {
            if (log != null)
                log.error(LOG_PREFIX + "Session aborted by the user - skipping command", null);
            return newStatus(SyncML.STATUS_OPERATION_CANCELLED);
        }

        //make sure exactly one item is specified to be retrieved
        if ( (command.items == null) || (command.items.size() != 1) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "Invalid number of items to get", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }

        //get the item and make sure a target URI is specified
        SyncItem item = (SyncItem)command.items.elementAt(0);
        if ( (item.targetUri == null) || (item.targetUri.length() <= 0) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "No target URI specified for Get item", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }
        
        //we only support handling the device information - return it as a Result command
        if ( (item.targetUri.equals(DevInfCodepage.DOC_URI_1_1)) || (item.targetUri.equals(DevInfCodepage.DOC_URI_1_2)) )
        {
            try
            {
                //create the WBXML codepage
                DevInfCodepage cpDevinf = new DevInfCodepage(log);
                
                //determine which DTD version was requested
                String dtdVersion = DevInfCodepage.VER_DTD_1_1;
                if (item.targetUri.equals(DevInfCodepage.DOC_URI_1_2))
                    dtdVersion = DevInfCodepage.VER_DTD_1_2;
                
                //create a Results command which will be sent in the next message
                Cmd resultCmd = new Cmd(Cmd.CMD_RESULTS);
                resultCmd.messageId = outMessageId + 1;
                resultCmd.commandId = outCommandId++;
                resultCmd.refMessageId = inSyncHeader.messageId;
                resultCmd.refCommandId = command.commandId;
                resultCmd.metinf = new Metinf();
                resultCmd.metinf.contentType = DevInfCodepage.CT_WBXML;
                SyncItem resultItem = new SyncItem();
                resultItem.sourceUri = item.targetUri;
                resultItem.data = cpDevinf.getDevinf(syncManager.getDevice(), syncStores, dtdVersion);
                resultCmd.items.addElement(resultItem);
                outReplyCmds.addElement(resultCmd);
                
                return newStatus(SyncML.STATUS_OK);
            }
            catch (WbxmlException e)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Failed to format WBXML Devinf opaque data", e);
                return newStatus(SyncML.STATUS_COMMAND_FAILED);
            }
            catch (IOException e)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Failed to write WBXML Devinf opaque data", e);
                return newStatus(SyncML.STATUS_COMMAND_FAILED);
            }
        }
        
        if (log != null)
            log.error(LOG_PREFIX + "Get of item '" + item.targetUri + "' is not supported", null);
        return newStatus(SyncML.STATUS_NOT_SUPPORTED);
    }
    
    /* Processes the SyncML 'Move' command received from the server. */
    private Status doMoveCmd(Cmd command)
    {
        try
        {
            //make sure the user hasn't aborted the session
            if (isCancelled())
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Session aborted by the user - skipping command", null);
                return newStatus(SyncML.STATUS_OPERATION_CANCELLED);
            }

            //make sure we were not in the middle of receiving a chunked item
            if (inChunkedItem != null)
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Failed to received complete data for chunked item '" + inChunkedItem.getUri() + "'");
                addOutgoingAlert(SyncML.ALERT_NO_END_OF_DATA, inChunkedItem.sourceUri, inChunkedItem.targetUri);
                inChunkedItem = null;
            }

            //make sure it's part of a Sync command
            if (inSyncCmd == null)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Move command is not part of a valid Sync command", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            //make sure exactly one item is specified to be moved
            if ( (command.items == null) || (command.items.size() != 1) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Invalid number of items to move", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }

            //get the item and make sure a target URI is specified
            SyncItem item = (SyncItem)command.items.elementAt(0);
            if ( (item.targetUri == null) || (item.targetUri.length() <= 0) )
            {
                if (log != null)
                    log.error(LOG_PREFIX + "No target URI specified for Move item", null);
                return newStatus(SyncML.STATUS_BAD_REQUEST);
            }
            
            int statusCode;
            try
            {
                //move the item in the store
                inActiveStore.store.moveRecord(item.targetUri, item.targetParentUri, item.sourceParentUri, item.data);
                statusCode = SyncML.STATUS_OK;
            }
            catch (NoSuchRecordException e)
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Record not found in the local store: " + item.targetUri);
                statusCode = SyncML.STATUS_ITEM_GONE;
            }
            
            return newStatus(statusCode);
        }
        catch (StoreException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Error accessing local record store '" + inActiveStore.store.getClientURI() + "'", e);
            return newStatus(SyncML.STATUS_DATA_STORE_FAILURE, e);
        }
    }
    
    /* Processes the SyncML 'Put' command received from the server. */
    private Status doPutCmd(Cmd command)
    {
        //make sure the user hasn't aborted the session
        if (isCancelled())
        {
            if (log != null)
                log.error(LOG_PREFIX + "Session aborted by the user - skipping command", null);
            return newStatus(SyncML.STATUS_OPERATION_CANCELLED);
        }

        //make sure exactly one item is specified to be put
        if ( (command.items == null) || (command.items.size() != 1) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "Invalid number of items to put", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }

        //get the item and make sure a source URI is specified
        SyncItem item = (SyncItem)command.items.elementAt(0);
        if ( (item.sourceUri == null) || (item.sourceUri.length() <= 0) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "No source URI specified for Put item", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }
        
        //we only support handling the device information - don't bother parsing it - just return success
        if ( (item.sourceUri.equals(DevInfCodepage.DOC_URI_1_1)) || (item.sourceUri.equals(DevInfCodepage.DOC_URI_1_2)) )
            return newStatus(SyncML.STATUS_OK);
        
        if (log != null)
            log.error(LOG_PREFIX + "Put of item '" + item.sourceUri + "' is not supported", null);
        return newStatus(SyncML.STATUS_NOT_SUPPORTED);
    }
    
    /* Processes the SyncML 'Replace' command received from the server. */
    private Status doReplaceCmd(Cmd command)
    {
        Status result = null;
        SyncItem item = null;
        try
        {
            //make sure the user hasn't aborted the session
            if (isCancelled())
            {
                result = doCmdError(command, "Session aborted by the user - skipping command", SyncML.STATUS_OPERATION_CANCELLED, null, null);
                return result;
            }

            //make sure it's part of a Sync command
            if (inSyncCmd == null)
            {
                result = doCmdError(command, "Replace command is not part of a valid Sync command", SyncML.STATUS_BAD_REQUEST, null, null);
                return result;
            }
            
            //make sure exactly one item is specified to be replaced
            if ( (command.items == null) || (command.items.size() != 1) )
            {
                result = doCmdError(command, "Invalid number of items to replace", SyncML.STATUS_BAD_REQUEST, null, null);
                return result;
            }

            //get the item and make sure a target URI is specified
            item = (SyncItem)command.items.elementAt(0);
            if ( (item.targetUri == null) || (item.targetUri.length() <= 0) )
            {
                result = doCmdError(command, "No target URI specified for Replace item", SyncML.STATUS_BAD_REQUEST, null, item);
                return result;
            }

            //handle the command item
            int itemStatus = doCmdItem(command, item);
            if (itemStatus != 0)
            {
                if (! SyncML.isSuccessStatus(itemStatus))
                    result = doCmdError(command, "Error accepting command item", itemStatus, null, item);
                else
                    result = newStatus(itemStatus);
                
                return result;
            }
            
            //if this is the first (or only) chunk, notify the store that we are starting to replace a record
            if (inChunkedItem == null)
            {
                ContentType ctType = null;                
                if ( (command.metinf != null) && (command.metinf.contentType != null) )
                    ctType = new ContentType(command.metinf.contentType);

                boolean fieldReplace = false;
                if (command.metinf != null)
                    fieldReplace = command.metinf.fieldLevelReplace;
                
                //notify the store that we are starting to replace a record
                inActiveStore.store.replaceRecordBegin(item.targetUri, ctType, fieldReplace);
            }
            
            //pass the data to the record store
            inActiveStore.store.replaceRecordData(item.data);
            inChunkedItem = item;
            
            //nothing more to do if there is more data to receive - also note that we don't cache the 
            //status as it is not the final status for the item 
            if (item.moreData)
            {
                if (log != null)
                    log.debug(LOG_PREFIX + "Accepted data chunk");
                return newStatus(SyncML.STATUS_DATA_CHUNK_ACCEPTED);
            }
            
            //all data received - go ahead and replace the record
            inChunkedItem = null;
            String newLocalId = inActiveStore.store.replaceRecordEnd(true);
            int statusCode = SyncML.STATUS_ITEM_ADDED;
            
            //add a Map for the record if its local ID has been changed (to map its global ID to the new local ID)
            if ( (newLocalId != null) && (! newLocalId.equals(item.targetUri)) )
                addOutgoingMap(item, newLocalId);
            
            result = newStatus(statusCode);
            return result;
        }
        catch (NoSuchRecordException e)
        {
            result = doCmdError(command, "Record not found in the local store '" + inActiveStore.store.getClientURI() + "'", SyncML.STATUS_DATA_STORE_FAILURE, e, item);
            return result;
        }
        catch (StoreException e)
        {
            result = doCmdError(command, "Error accessing local record store '" + inActiveStore.store.getClientURI() + "'", SyncML.STATUS_DATA_STORE_FAILURE, e, item);
            return result;
        }
        finally
        {
            //cache the result of this command in case we need resend it later (which can occur after a suspend/resume)
            if ( (item != null) && (result != null) )
            {
                String itemUri = item.getUri();
                if ( (itemUri != null) && (itemUri.length() > 0) )
                    inActiveStore.outStatusCmds.put(itemUri, new Integer(result.statusCode));
            }
        }
    }
    
    /* Processes the start of the SyncML 'Sync' command received from the server. */
    private Status doSyncCmdStart(Cmd command)
    {
        //make sure a source URI is specified
        if ( (command.sourceUri == null) || (command.sourceUri.length() <= 0) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "No source URI specified for Sync command", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }

        //make sure a target URI is specified
        if ( (command.targetUri == null) || (command.targetUri.length() <= 0) )
        {
            if (log != null)
                log.error(LOG_PREFIX + "No target URI specified for Sync command", null);
            return newStatus(SyncML.STATUS_BAD_REQUEST);
        }
        
        //select the active record store based on the target URI
        inActiveStore = getRecordStore(command.targetUri);
        if (inActiveStore == null)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Failed to identify local record store with URI '" + command.sourceUri + "'", null);
            return newStatus(SyncML.STATUS_NOT_FOUND);
        }

        //handle the max object size (if specified)
        if ( (command.metinf != null) && (command.metinf.maxObjSize > 0) && (command.metinf.maxObjSize < inActiveStore.maxRecordSize) )
            inActiveStore.maxRecordSize = command.metinf.maxObjSize;
        
        //store the Sync command so we can refer to it later
        inSyncCmd = command;

        //inform the record store of the number of changes to expect
        if (command.numberOfChanges >= 0)
            inActiveStore.store.onNumberOfChanges(command.numberOfChanges);
        
        return newStatus(SyncML.STATUS_OK);
    }

    /* Processes the end of the SyncML 'Sync' command received from the server. */
    private void doSyncCmdEnd(Cmd command)
    {
        //clear the command
        inSyncCmd = null;
        
        //add the Map command (if any) to the list of replies to be sent
        if (outMapCmd != null)
            outReplyCmds.addElement(outMapCmd);
        outMapCmd = null;
        
        //clear the active store
        inActiveStore = null;
    }
    
    /* Creates a newStatus with the specified status code. */
    private Status newStatus(int statusCode)
    {
        Status status = new Status();
        status.statusCode = statusCode;
        return status;
    }
    
    /* Creates a new Status from the specified exception. */
    private Status newStatus(int statusCode, SyncException cause)
    {
        //just use the specified status code if no exception was provided
        if (cause == null)
            return newStatus(statusCode);
        
        //the status code in the exception always overrides the specified one
        Status status = new Status();
        int code = cause.getStatusCode();
        if ( (code > 0) && (! SyncML.isSuccessStatus(code)) )
            status.statusCode = code;
        else
            status.statusCode = statusCode;
        
        //get any additional status data
        String statusData = cause.getStatusData();
        if ( (statusData != null) && (statusData.length() > 0) )
        {
            try
            {
                SyncItem statusItem = new SyncItem();
                statusItem.data = statusData.getBytes(ENCODING_UTF8);
                status.items.addElement(statusItem);
            }
            catch (UnsupportedEncodingException enc)
            {
                //ignore
            }
        }
        
        return status;
    }

    /* Adds an outgoing Status command with the specified status for the specified command. */
    private void addOutgoingStatus(Cmd command, Status status)
    {
        //nothing more to do if no response is required
        if ( (inSyncHeader == null) || (inSyncHeader.noResponse) || ((command != null) && (command.noResponse)) )
            return;
        
        //update the status for the specified command
        status.commandId = outCommandId++;
        status.refMessageId = inSyncHeader.messageId;
        if (command != null)
        {
            status.refCommandId = command.commandId;
            status.refCommand = command.command;
            
            //copy the command source/target URIs if the command applied to a single item - no URIs 
            //will imply that the status refers to all items  
            if ( (command.items != null) && (command.items.size() == 1) )
            {
                SyncItem cmdItem = (SyncItem)command.items.elementAt(0);
                
                if ( (cmdItem.sourceUri != null) && (cmdItem.sourceUri.length() > 0) )
                    status.refItemSourceUris.addElement(cmdItem.sourceUri);

                if ( (cmdItem.targetUri != null) && (cmdItem.targetUri.length() > 0) )
                    status.refItemTargetUris.addElement(cmdItem.targetUri);
            }
        }
        else
        {
            //no command implies the status is for the sync header
            status.refCommandId = 0;
            status.refCommand = "SyncHdr";
            status.refItemSourceUris.addElement( syncManager.getTransport().getTargetURI() );
            status.refItemTargetUris.addElement( syncManager.getDevice().getDeviceID() );
            
            //get the next client nonce to send to the server
            String clientNonce = syncManager.getDevice().getClientNonce();
            if ( (clientNonce != null) && (clientNonce.length() > 0) ) 
            {
                //only create the challenge if the client nonce is different from the previous one
                if ( (outClientChal == null) || (! outClientChal.metinf.nextNonce.equals(clientNonce)) )
                {
                    outClientChal = new Chal();
                    outClientChal.metinf = new Metinf();
                    outClientChal.metinf.contentType = Cred.AUTH_TYPE_MD5;
                    outClientChal.metinf.encoding = Metinf.ENC_BASE64;
                    outClientChal.metinf.nextNonce = clientNonce;
                    status.challenge = outClientChal;
                }
            }
        }
        
        //add the status to the list of outgoing status commands
        if (log != null)
            log.info(LOG_PREFIX + "Setting status of '" + status.refCommand + "' command to " + status.statusCode);
        outStatusCmds.addElement(status);
    }
    
    /* Adds an outgoing Map command for the specified item and local ID. */
    private void addOutgoingMap(SyncItem item, String localId)
    {
        //if necessary, create a Map command for the database being synced
        if (outMapCmd == null)
        {
            outMapCmd = new Cmd(Cmd.CMD_MAP);
            outMapCmd.messageId = outMessageId;
            outMapCmd.commandId = outCommandId++;
            outMapCmd.sourceUri = inSyncCmd.targetUri;
            outMapCmd.targetUri = inSyncCmd.sourceUri;
        }
        
        //create the map item and add it to the Map command
        MapItem mapItem = new MapItem();
        mapItem.sourceUri = localId;
        mapItem.targetUri = item.sourceUri;
        
        if (log != null)
            log.info(LOG_PREFIX + "Creating map of global ID '" + mapItem.targetUri + "' to local ID '" + localId + "'");
        outMapCmd.mapItems.addElement(mapItem);
    }
    
    /* Adds an outgoing Alert command for the specified URIs. */
    private void addOutgoingAlert(int alertCode, String sourceUri, String trgetUri)
    {
        //create an Alert command with the specified details
        Cmd alertCmd = new Cmd(Cmd.CMD_ALERT);
        alertCmd.messageId = outMessageId;
        alertCmd.commandId = outCommandId++;
        alertCmd.alertCode = alertCode;
        SyncItem alertItem = new SyncItem();
        alertItem.sourceUri = sourceUri;
        alertItem.targetUri = trgetUri;
        alertCmd.items.addElement(alertItem);
        
        //add the alert to the list of outgoing reply commands
        if (log != null)
            log.info(LOG_PREFIX + "Sending alert code of '" + alertCode + "' for source URI '" + sourceUri + "' and target URI '" + trgetUri + "'");
        outReplyCmds.addElement(alertCmd);
    }

    /* Returns the record store with the specified client URI. */
    private RecordStoreState getRecordStore(String clientUri)
    {
        for (int i = 0; i < storeStates.length; i++)
        {
            if (clientUri.equals(storeStates[i].store.getClientURI()))
                return storeStates[i];
        }
        
        return null;
    }
    
    /* Returns TRUE if the data in both arrays are the same. */
    private boolean isDataEqual(byte[] data1, int offset1, byte[] data2, int offset2, int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (data1[offset1 + i] != data2[offset2 + i])
                return false;
        }
        
        return true;
    }

    /* Send the next SyncML message to the server via the specified output stream. */
    private void sendMessage(CountingOutputStream outputStream)
        throws SyncException, IOException
    {
        if (log != null)
            log.info(LOG_PREFIX + "Sending message " + outMessageId + " to the server");
        
        try
        {
            //create the WBXML codepages 
            MetInfCodepage cpMetinf = new MetInfCodepage(log, this);
            SyncMLCodepage cpSyncml = new SyncMLCodepage(log, this, cpMetinf);
            
            //build and write the SyncML header
            SyncHdr header = new SyncHdr();
            header.dtdVersion = SyncMLCodepage.VER_DTD_1_2;
            header.protocolVersion = SyncMLCodepage.VER_PROTO_1_2;
            header.sessionId = sessionId;
            header.messageId = outMessageId;
            header.sourceUri = syncManager.getDevice().getDeviceID();
            header.sourceName = syncManager.getAuthUsername();
            header.targetUri = syncManager.getTransport().getTargetURI();
            header.credentials = getCredentials(inServerChal, syncManager.getAuthUsername(), syncManager.getAuthPasword());
            if (header.credentials != null)
            {
                outClientAuthCount++;
            }
            if (maxMsgSize > 0)
            {
                header.metinf = new Metinf();
                header.metinf.maxMsgSize = maxMsgSize;
                if(log!= null)
                	log.debug(LOG_PREFIX + "Device: SyncML Header Maximum message size is: " + maxMsgSize);
            }
            cpSyncml.writeHeader(outputStream, header);
            
            //write the Status for any server commands we have just processed 
            while (outStatusCmds.size() > 0)
            {
                Status status = (Status)outStatusCmds.elementAt(0);
                cpSyncml.writeStatus(outputStream, status);
                outStatusCmds.removeElementAt(0);
            }
                
            //write any outgoing commands (e.g. Map, etc) that were the result of commands from the server
            while (outReplyCmds.size() > 0)
            {
                Cmd cmd = (Cmd)outReplyCmds.elementAt(0);
                cpSyncml.writeCommand(outputStream, cmd);
                outReplyCmds.removeElementAt(0);
            }
            
            //write any additional SyncML commands based on the state of the session
            boolean finalMsg = true;
            if (stopSession)
                finalMsg = false;
            else if (suspendSession)
                finalMsg = sendSuspendCommands(outputStream, cpSyncml);
            else if (sessionState == STATE_CLIENT_INIT)
                finalMsg = sendInitCommands(outputStream, cpSyncml);
            else if (sessionState == STATE_CLIENT_UPDATES)
                finalMsg = sendClientUpdateCommands(outputStream, cpSyncml);
//            if(log!=null)
//            	log.debug(LOG_PREFIX+"Output size:"+ outputStream.);
        
            //write the message footer
            cpSyncml.writeFooter(outputStream, finalMsg);
        }
        catch (WbxmlException e)
        {
            throw new SyncException("failed to format WBXML message", e);
        }
    }
    
    /* Send session initialization commands to the server via the specified output stream. */
    private boolean sendInitCommands(CountingOutputStream outputStream, SyncMLCodepage cpSyncml)
        throws SyncException, WbxmlException, IOException
    {
        if (log != null)
            log.info(LOG_PREFIX + "Sending sync initialization commands to the server");

        //create the WBXML codepages 
        DevInfCodepage cpDevinf = new DevInfCodepage(log);

        //send a Put command containing the Devinf
        Cmd putCmd = new Cmd(Cmd.CMD_PUT);
        putCmd.messageId = outMessageId;
        putCmd.commandId = outCommandId++;
        putCmd.metinf = new Metinf();
        putCmd.metinf.contentType = DevInfCodepage.CT_WBXML;
        SyncItem putItem = new SyncItem();
        putItem.sourceUri = DevInfCodepage.DOC_URI_1_2;
        putItem.data = cpDevinf.getDevinf(syncManager.getDevice(), syncStores, DevInfCodepage.VER_DTD_1_2);
        putCmd.items.addElement(putItem);
        cpSyncml.writeCommand(outputStream, putCmd);

        //send an Alert command for each record store
        int alertCmdCount = 0;
        for (int i = 0; i < storeStates.length; i++)
        {
            //ignore record stores whose sync session has already failed
            if (! storeStates[i].sessionSuccess)
                continue;
            
            //if resuming the session, make sure the record store can actually be resumed
            RecordStore store = storeStates[i].store;
            if (resumeSession)
            {
                try 
                {
                    store.onSyncResume();
                }
                catch (StoreException e)
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Can't resume session for record store '" + store.getClientURI() + "'", e);
                    storeStates[i].sessionSuccess = false;
                    continue;
                }
            }
            
            //send the Alert command for the record store
            Cmd alertCmd = new Cmd(Cmd.CMD_ALERT);
            alertCmd.messageId = outMessageId;
            alertCmd.commandId = outCommandId++;
            if (resumeSession)
                alertCmd.alertCode = SyncML.ALERT_RESUME;
            else
                alertCmd.alertCode = SyncML.syncTypeToAlert(store.getSyncType());
            SyncItem alertItem = new SyncItem();
            alertItem.sourceUri = store.getClientURI();
            alertItem.targetUri = store.getServerURI();
            alertItem.metinf = new Metinf();
            alertItem.metinf.nextAnchor = store.getNextAnchor();
            if (! resumeSession)
                alertItem.metinf.lastAnchor = store.getLastAnchor();
            if (storeStates[i].maxRecordSize > 0)
                alertItem.metinf.maxObjSize = storeStates[i].maxRecordSize;
            alertItem.metinf.addExtensions(store.getMetaInfoExtensions());
            alertCmd.items.addElement(alertItem);
            cpSyncml.writeCommand(outputStream, alertCmd);
            alertCmdCount++;
        }
        
        //make sure at least one Alert command was sent
        if (alertCmdCount <= 0)
            throw new SyncException("failed to initialize the sync session for even one record store");
        
        //client initialization package completed - expect server to send its initialization package
        sessionState = Session.STATE_SERVER_INIT;
        
        //finished the initialization package
        return true;
    }
    
    /* Send session suspend commands to the server via the specified output stream. */
    private boolean sendSuspendCommands(CountingOutputStream outputStream, SyncMLCodepage cpSyncml)
        throws WbxmlException, IOException
    {
        if (log != null)
            log.info(LOG_PREFIX + "Sending sync session suspend commands to the server");

        //send an Alert command to initiate the suspension of the session
        Cmd alertCmd = new Cmd(Cmd.CMD_ALERT);
        alertCmd.messageId = outMessageId;
        alertCmd.commandId = outCommandId++;
        alertCmd.alertCode = SyncML.ALERT_SUSPEND;
        SyncItem alertItem = new SyncItem();
        alertItem.sourceUri = syncManager.getDevice().getDeviceID();
        alertItem.targetUri = syncManager.getTransport().getTargetURI();
        alertCmd.items.addElement(alertItem);
        cpSyncml.writeCommand(outputStream, alertCmd);
        
        return false;
    }
    
    /* Send client update commands to the server via the specified output stream. */
    private boolean sendClientUpdateCommands(CountingOutputStream outputStream, SyncMLCodepage cpSyncml)
        throws SyncException, WbxmlException, IOException
    {
        if (log != null)
            log.info(LOG_PREFIX + "Sending sync client update commands to the server");

        //make sure that there is at least one record store to be synced
        int storeCount = 0;
        for (int i = 0; i < storeStates.length; i++)
        {
            if (storeStates[i].sessionSuccess)
                storeCount++;
        }
        if (storeCount <= 0)
            throw new SyncException("failed to initialize the sync session for even one record store");
        
        //check if we're resuming the session
        boolean sendRemainingSize = false;
        if (sessionResumed)
        {
            if (suspendedSessionState == STATE_CLIENT_UPDATES)
            {
                if (log != null)
                    log.info(LOG_PREFIX + "Previous session was suspended while sending client updates");
                suspendedSessionState = 0;
                
                //resend any pending client update commands - if there were any, wait until the next message 
                //before sending any new updates
                if (retryPendingClientUpdates(outputStream, cpSyncml) > 0)
                    return false;
                
                //if we were in the middle of sending chunks (and we haven't resent a pending chunk), we need 
                //to send the remaining size in the next chunk
                if (outChunkedBytesSent > 0)
                    sendRemainingSize = true;
            }
            else if ( (suspendedSessionState == STATE_SERVER_UPDATES) || (suspendedSessionState == STATE_CLIENT_MAPS) )
            {
                if (log != null)
                    log.info(LOG_PREFIX + "Previous session was suspended while receiving server updates or sending client maps");
                suspendedSessionState = 0;
                
                //just send an empty client update package (i.e. empty Sync commands for each store)
                for (int i = 0; i < storeStates.length; i++)
                {
                    if (! storeStates[i].sessionSuccess)
                        continue;
                    
                    Cmd syncCmd = new Cmd(Cmd.CMD_SYNC);
                    syncCmd.messageId = outMessageId;
                    syncCmd.commandId = outCommandId++;
                    syncCmd.sourceUri = storeStates[i].store.getClientURI();
                    syncCmd.targetUri = storeStates[i].store.getServerURI();
                    cpSyncml.writeCommand(outputStream, syncCmd);
                    cpSyncml.writeCommandEnd(outputStream, syncCmd);
                }
                
                sessionState = Session.STATE_SERVER_UPDATES;
                return true;
            }
        }
        
        //send a Sync command for each record store
        for (int i = 0; i < storeStates.length; i++)
        {
            //get the next store to process (if we have finished with the previous one)
            if (outStoreState == null)
            {
                //ignore record stores whose sync session has already failed
                if (! storeStates[i].sessionSuccess)
                    continue;
                
                outStoreState = storeStates[i];
                outRecords = null;
                outRecord = null;
            }
            
            //send the start of the Sync command for the store
            if (outStoreState.outSyncCmd == null)
            {
                //we hang on to the Sync command object as it is referenced by each client update command (see "Cmd.parentCmd")
                outStoreState.outSyncCmd = new Cmd(Cmd.CMD_SYNC);
                outStoreState.outSyncCmd.sourceUri = outStoreState.store.getClientURI();
                outStoreState.outSyncCmd.targetUri = outStoreState.store.getServerURI();
            }
            outStoreState.outSyncCmd.messageId = outMessageId;
            outStoreState.outSyncCmd.commandId = outCommandId++;
            cpSyncml.writeCommand(outputStream, outStoreState.outSyncCmd);
            
            //get the records to send to the server (if we have finished with the previous ones)
            if (outRecords == null)
            {
                int syncType = outStoreState.store.getSyncType();
                if ( (syncType == SyncML.SYNC_TYPE_TWO_WAY) || (syncType == SyncML.SYNC_TYPE_ONE_WAY_CLIENT) )
                    outRecords = outStoreState.store.getChangedRecords();
                else if ( (syncType == SyncML.SYNC_TYPE_TWO_WAY_SLOW) || (syncType == SyncML.SYNC_TYPE_REFRESH_CLIENT) ) 
                    outRecords = outStoreState.store.getAllRecords();
            }

            //send an update command for each record
            if (outRecords != null)
            {
            	int num=0;
                while ( (outRecord != null) || (! outRecords.empty()) )
                {
                    //make sure the user hasn't aborted the session
                    if (isCancelled())
                    {
                        if (log != null)
                            log.info(LOG_PREFIX + "Session aborted by the user - skipping remaining client updates");
                        cpSyncml.writeCommandEnd(outputStream, outStoreState.outSyncCmd);
                        return false;
                    }

                    //trigger the packet send if we have exceeded the max message size
                    long freeSpace = maxMsgSize - 64 - outputStream.getByteCount();
                    if (freeSpace <= 0)
                    {
                        cpSyncml.writeCommandEnd(outputStream, outStoreState.outSyncCmd);
                        return false;
                    }
                    
                    //get the next record to send (if we're finished with the previous one)
                    if (outRecord == null)
                    {
                        outRecord = (Record)outRecords.consume();
                        outChunkedBytesSent = 0;
                    }

                    //get the change type of the record
                    int changeType = outRecord.getChangeType();
                    if (changeType <= 0)
                        changeType = Record.CHANGE_TYPE_ADD;
                    
                    //determine if the record content must be supplied
                    boolean sendContent = false;
                    if ( (changeType == Record.CHANGE_TYPE_ADD) || (changeType == Record.CHANGE_TYPE_REPLACE) || (outRecord.isFieldLevelReplace()) )
                        sendContent = true;
                    
                    //determine if the data must be chunked
                    long dataSize = 0;
                    int chunkSize = 0; 
                    boolean moreData = false;
                    if (sendContent)
                    {
                        //get the size of the record to be sent
                        dataSize = outRecord.getDataSize();
                        if (dataSize <= 0)
                            throw new SyncException("invalid data size specified for record '" + outRecord.getLocalId() + "'");
                        
                        //check if we need to split the record data into multiple chunks
                        long remainingDataSize = dataSize - outChunkedBytesSent;
                        if (remainingDataSize > freeSpace)
                        {
                            moreData = true;
                            chunkSize = (int)freeSpace;
                        }
                        else
                            chunkSize = (int)remainingDataSize;
                    }
                    
                    //create the appropriate update command based on the change type 
                    Cmd updateCmd = null; 
                    if (changeType == Record.CHANGE_TYPE_ADD)
                        updateCmd = new Cmd(Cmd.CMD_ADD);
                    else if (changeType == Record.CHANGE_TYPE_REPLACE)
                        updateCmd = new Cmd(Cmd.CMD_REPLACE);
                    else if (changeType == Record.CHANGE_TYPE_DELETE)
                        updateCmd = new Cmd(Cmd.CMD_DELETE);
                    else if (changeType == Record.CHANGE_TYPE_MOVE)
                        updateCmd = new Cmd(Cmd.CMD_MOVE);
                    else if (changeType == Record.CHANGE_TYPE_COPY)
                        updateCmd = new Cmd(Cmd.CMD_COPY);
                    else
                        throw new SyncException("invalid change type '" + changeType + "' specified for record '" + outRecord.getLocalId() + "'");
                    updateCmd.messageId = outMessageId;
                    updateCmd.commandId = outCommandId++;
                    updateCmd.parentCmd = outStoreState.outSyncCmd;
                    
                    if (sendContent)
                    {
                        updateCmd.metinf = new Metinf();
                        updateCmd.metinf.contentType = outRecord.getContentType().toString();
                        
                        if (changeType == Record.CHANGE_TYPE_REPLACE)
                            updateCmd.metinf.fieldLevelReplace = outRecord.isFieldLevelReplace();
                        
                        if ( (moreData) && (outChunkedBytesSent <= 0) )
                            updateCmd.metinf.size = dataSize;
                        else if (sendRemainingSize) 
                            updateCmd.metinf.size = dataSize - outChunkedBytesSent;
                    }

                    SyncItem updateItem = new SyncItem();
                    updateItem.sourceUri = outRecord.getLocalId();
                    updateItem.sourceParentUri = outRecord.getParentId();
                    if (changeType == Record.CHANGE_TYPE_COPY)
                    {
                        updateItem.targetUri = outRecord.getTargetId();
                        updateItem.targetParentUri = outRecord.getTargetParentId();
                    }
                    if (sendContent)
                    {
                        updateItem.data = new byte[chunkSize];
                        int readCount = outRecord.getData(updateItem.data, chunkSize);
                        if (readCount != chunkSize)
                            throw new SyncException("unexpected data size while reading record '" + outRecord.getLocalId() + "'");

                        outChunkedBytesSent += chunkSize;
                        updateItem.totalSize = dataSize;
                        updateItem.chunkedBytesSent = outChunkedBytesSent; 
                        if (outChunkedBytesSent < dataSize)
                            updateItem.moreData = true;
                    }
                    updateCmd.items.addElement(updateItem);
                    outPendingUpdateCmds.addElement(updateCmd);
                    cpSyncml.writeCommand(outputStream, updateCmd);
                    
                    //nothing more to do for this message if we need to send more chunks
                    if (updateItem.moreData)
                    {
                        cpSyncml.writeCommandEnd(outputStream, outStoreState.outSyncCmd);
                        return false;
                    }
                    
                    if(log!= null)
                    {
                    	log.info("Current record number:"+ (++num));
                    }
                    //move on to the next record
                    outRecord.close();
                    outRecord = null;
                }
            }
            
            //move on to the next store
            cpSyncml.writeCommandEnd(outputStream, outStoreState.outSyncCmd);
            outStoreState = null;
        }
        
        //don't send the final message of the package if there are still updates to be acknowledged by the server
        if (outPendingUpdateCmds.size() > 0)
            return false;
        
        //client update package completed - expect server to send its update package
        sessionState = Session.STATE_SERVER_UPDATES;

        //finished the client update package
        return true;
    }
    
    /* Send any pending client update commands to the server via the specified output stream. */
    private int retryPendingClientUpdates(CountingOutputStream outputStream, SyncMLCodepage cpSyncml)
        throws WbxmlException, IOException
    {
        if (log != null)
            log.info(LOG_PREFIX + "Resending any pending client update commands to the server");
        
        //resend any pending update commands (i.e. commands that we didn't get a Status for)        
        Cmd syncCmd = null;
        int pendingUpdateCount = 0;
        for (int i = 0; i < outPendingUpdateCmds.size(); i++)
        {
            Cmd cmd = (Cmd)outPendingUpdateCmds.elementAt(i);
            if ( (cmd.parentCmd == null) || (! cmd.parentCmd.command.equals(Cmd.CMD_SYNC)) )
                continue;
            
            //send the start of the Sync command for the current update command
            if ( (syncCmd == null) || (! syncCmd.sourceUri.equals(cmd.parentCmd.sourceUri)) )
            {
                //write the end of the last Sync command (if any)
                if (syncCmd != null)
                    cpSyncml.writeCommandEnd(outputStream, syncCmd);
                    
                syncCmd = cmd.parentCmd;
                syncCmd.messageId = outMessageId;
                syncCmd.commandId = outCommandId++;
                cpSyncml.writeCommand(outputStream, syncCmd);
            }
                
            //send the current update command
            cmd.messageId = outMessageId;
            cmd.commandId = outCommandId++;
            if (cmd.items.size() > 0)
            {
                //if sending chunked data, recalculate the remaining size
                SyncItem cmdItem = (SyncItem)cmd.items.elementAt(0);
                if ( (cmd.metinf != null) && (cmdItem.data != null) )
                    cmd.metinf.size = cmdItem.totalSize - (cmdItem.chunkedBytesSent - cmdItem.data.length);
            }
            cpSyncml.writeCommand(outputStream, cmd);
            pendingUpdateCount++;
        }

        //write the end of the last Sync command (if any)
        if (syncCmd != null)
            cpSyncml.writeCommandEnd(outputStream, syncCmd);
        
        if (log != null)
            log.info(LOG_PREFIX + "Resent " + pendingUpdateCount + " pending client update commands");
        return pendingUpdateCount;
    }

    /* Handles the result of a client update command that was previously sent to the server - returns TRUE if the client update is complete. */
    private boolean onClientUpdateResult(String localId, Status status)
    {
        if (status != null)
        {
            //remove the associated pending update command (so that it won't be retried if the session is suspended/resumed)
            int index = indexOfCommand(status.refMessageId, status.refCommandId, outPendingUpdateCmds);
            if (index >= 0)
            {
                outPendingUpdateCmds.removeElementAt(index);
            }
            else
            {
                if (log != null)
                    log.warn(LOG_PREFIX + "Failed to find pending client update command with message ID '" + status.refMessageId + "' and command ID '" + status.refCommandId + "'");
            }
            
            //nothing more to do if there are more chunks to be sent
            if (status.statusCode == SyncML.STATUS_DATA_CHUNK_ACCEPTED)
            {
                if (log != null)
                    log.debug(LOG_PREFIX + "Server has accepted data chunk for client update");
                return false;
            }
        }
        
        //clear the current record as we're done with it
        if ( (outRecord != null) && (outRecord.getLocalId().equals(localId)) )
        {
            outRecord.close();
            outRecord = null;
        }
        
        return true;
    }
    
    /* Returns the credentials requested by the specified challenge (based on the specified username/password). */
    private Cred getCredentials(Chal chal, String username, String password)
        throws SyncException
    {
        //nothing more to do if no challenge was made
        if ( (chal == null) || (chal.metinf == null) )
            return null;
        
        //get the challenge parameters
        String chalType = chal.metinf.contentType;
        String chalNextNonce = chal.metinf.nextNonce;
        
        //get the user credentials
        String userCred = username + ":" + password;
        
        try
        {
            //create the credentials
            Cred cred = new Cred();
            cred.metinf = new Metinf();
            if (chalType.equals(Cred.AUTH_TYPE_BASIC))
            {
                String encodedCred = CommonUtils.base64Encode( userCred.getBytes(ENCODING_UTF8) );
                
                cred.metinf.contentType = Cred.AUTH_TYPE_BASIC;
                cred.metinf.encoding = Metinf.ENC_BASE64;
                cred.data = encodedCred.getBytes(ENCODING_UTF8);
            }
            else if (chalType.equals(Cred.AUTH_TYPE_MD5))
            {
                if ( (chalNextNonce == null) || (chalNextNonce.length() <= 0) )
                    throw new SyncException("no next nonce specified for MD5 authentication");

                String hashedUserCred = CommonUtils.base64Encode( MD5.encode(userCred.getBytes(ENCODING_UTF8)) );
                String nonceCred = hashedUserCred + ":" + chalNextNonce;
                byte[] hashedNonceCred = MD5.encode(nonceCred.getBytes(ENCODING_UTF8));
                String encodedCred = CommonUtils.base64Encode(hashedNonceCred);
                
                cred.metinf.contentType = Cred.AUTH_TYPE_MD5;
                cred.metinf.encoding = Metinf.ENC_BASE64;
                cred.data = encodedCred.getBytes(ENCODING_UTF8);
            }
            else
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Unsupported authentication type: " + chalType, null);
                return null;
            }
            
            return cred;
        }
        catch (IOException e)
        {
            throw new SyncException("failed to encode the required credentials");
        }
    }
    
    /* Returns TRUE if the specified credentials are valid according to the specified challenge and username/password. */
    private boolean isCredentialsValid(Cred cred, Chal chal, String username, String password)
    {
        if ( (cred == null) || (cred.data == null) || (chal == null) )
            return false;
        
        try
        {
            //get the expected credentials based on the specified challenge 
            Cred expectedCred = getCredentials(chal, username, password);
            if ( (expectedCred == null) || (expectedCred.data == null) || (expectedCred.data.length != cred.data.length) )
                return false;
            
            //compare the expected credentials with the specified credentials
            for (int i = 0; i < expectedCred.data.length; i++)
            {
                if (expectedCred.data[i] != cred.data[i])
                    return false;
            }
            
            return true;
        }
        catch (SyncException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Failed to validate credentials", e);
            return false;
        }
    }

    /* Returns the index of the command with the specified message/command ID in the specified collection. */
    private int indexOfCommand(int messageId, int commandId, Vector commands)
    {
        for (int i = 0; i < commands.size(); i++)
        {
            Cmd cmd = (Cmd)commands.elementAt(i);
            if ( (cmd.messageId == messageId) && (cmd.commandId == commandId) )
                return i;
        }
        
        return -1;
    }
    
    /* Resets the state of the sync session. */
    private void resetSession()
    {
        sessionState = Session.STATE_CLIENT_INIT;
        maxMsgSize = syncManager.getTransport().getMaxMsgSize();
        stopSession = false;
        sessionStopped = false;
        sessionStatusCode = 0;
        sessionStatusData = null;
        suspendSession = false;
        sessionSuspended = false;
        resumeSession = false;
        sessionResumed = false;
        suspendedSessionState = 0;
        for (int i = 0; i < storeStates.length; i++)
        {
            storeStates[i].resetState();
            storeStates[i].maxRecordSize = storeStates[i].store.getCapabilities().getMaxRecordSize();
        }
        
        inSyncHeader = null;
        inSyncCmd = null;
        inServerAuthCount = 0;
        inServerChal = null;
        inChunkedItem = null;
        inActiveStore = null;
        
        outMessageId = 1;
        outCommandId = 1;
        outClientChal = null;
        outStatusCmds = new Vector();
        outReplyCmds = new Vector();
        outPendingUpdateCmds = new Vector();
        outMapCmd = null;
        outClientAuthCount = 0;
        outStoreState = null;
        outRecords = null;
        if (outRecord != null)
        {
            outRecord.close();
            outRecord = null;
        }
        outChunkedBytesSent = 0;
        
        if(log!= null)
        	log.debug(LOG_PREFIX + "Device Maximum message size is: " + maxMsgSize);
    }

    
    /* Handles the case where the session is first started. */
    private boolean sessionStarting()
    {
        //nothing more to do if the session is being started after being previously suspended
        if (sessionSuspended)
            return true;
        
        //initialize the session state
        resetSession();

        //notify the listener that the session is starting
        syncManager.getSyncListener().onSyncStart();

        int startedIndex = 0;
        try
        {
            //notify each record store that the session is starting
            for (startedIndex = 0; startedIndex < syncStores.length; startedIndex++)
                syncStores[startedIndex].onSyncStart();
        }
        catch (StoreException e)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Failed to start the sync session for all record stores", e);

            //retrieve the status code from the exception
            sessionStatusCode = e.getStatusCode();
            sessionStatusData = e.getStatusData();
            
            //shutdown any stores that have been started
            for (int i = 0; i <= startedIndex; i++)
                syncStores[i].onSyncEnd(false, sessionStatusCode, sessionStatusData);

            syncManager.getSyncListener().onSyncEnd(false, sessionStatusCode, sessionStatusData);
            
            sessionStopped = true;
            return false;
        }
        
        return true;
    }
    

    /* Requests that the sync session be stopped due to a user request to do so. */
    public synchronized void cancelSession()
    {
        if (log != null)
            log.info(LOG_PREFIX + "Cancelling the sync session");
        stopSession(SyncML.STATUS_OPERATION_CANCELLED, null);
    }
    
    /* Returns TRUE if the sync session is stopped due to a user request to do so. */
    public synchronized boolean isCancelled()
    {
        return ( (stopSession == true) && (sessionStatusCode == SyncML.STATUS_OPERATION_CANCELLED) );
    }

    /* Requests that the sync session be stopped for the specified reason. */
    public synchronized void stopSession(int statusCode, String statusData)
    {
        if (log != null)
            log.info(LOG_PREFIX + "Stopping the sync session - status '" + statusCode + "'");
        
        //request that the session be stopped
        stopSession = true;

        //indicate the reason why the session is being stopped
        sessionStatusCode = statusCode;
        sessionStatusData = statusData;
        
        //if the session is suspended (and we're not resuming it), just stop the session immediately
        if ( (sessionSuspended) && (! resumeSession) )
        {
            sessionStopped(false);
            return;
        }
    }
    
    /* Requests that the sync session for the specified record store be stopped for the specified reason. */
    private void stopSession(RecordStoreState storeState, int statusCode, String statusData)
    {
        if (log != null)
            log.info(LOG_PREFIX + "Stopping the sync session for record store '" + storeState.store.getClientURI() + "' - status '" + statusCode + "' (" + statusData + ")");
        
        //indicate the result of the sync session for the store
        storeState.sessionSuccess = SyncML.isSuccessStatus(statusCode);
        storeState.sessionStatusCode = statusCode;
        storeState.sessionStatusData = statusData;

        //clear any outgoing data associated with this store
        if ( (outStoreState != null) && (outStoreState.store.getClientURI().equals(storeState.store.getClientURI())) )
        {
            outStoreState = null;
            outRecords = null;
            if (outRecord != null)
            {
                outRecord.close();
                outRecord = null;
            }
            outChunkedBytesSent = 0;
        }
        
        //clear any pending client update commands for this store
        for (int i = 0; i < outPendingUpdateCmds.size(); )
        {
            Cmd cmd = (Cmd)outPendingUpdateCmds.elementAt(i);
            if ( (cmd.parentCmd != null) && (cmd.parentCmd.sourceUri != null) && (cmd.parentCmd.sourceUri.equals(storeState.store.getClientURI())) )
            {
                outPendingUpdateCmds.removeElementAt(i);
                continue;
            }
            i++;
        }            

        //clear any incoming data associated with this store
        if ( (inActiveStore != null) && (inActiveStore.store.getClientURI().equals(storeState.store.getClientURI())) )
        {
            inActiveStore = null;
            inSyncCmd = null;
            inChunkedItem = null;
        }
    }    
    
    /* Handles the case where the session is stopped. */
    private void sessionStopped(boolean success)
    {
        //notify each record store that the session is finished
        for (int i = 0; i < storeStates.length; i++)
        {
            //determine if the session was successful for the record store
            boolean sessionSuccess = success;
            if (! storeStates[i].sessionSuccess)
                sessionSuccess = false;

            //determine the session status for the record store
            int statusCode = sessionStatusCode;
            String statusData = sessionStatusData; 
            if (storeStates[i].sessionStatusCode != 0)
            {
                statusCode = storeStates[i].sessionStatusCode;
                statusData = storeStates[i].sessionStatusData;
            }

            //notify the store that the session is finished
            storeStates[i].store.onSyncEnd(sessionSuccess, statusCode, statusData);
        }

        //also notify the listener that the session is finished
        syncManager.getSyncListener().onSyncEnd(success, sessionStatusCode, sessionStatusData);
        
        //cleanup
        resetSession();
        sessionStopped = true;
    }
    
    /* Requests that the sync session be suspended. */
    public synchronized boolean suspendSession()
    {
        if (log != null)
            log.info(LOG_PREFIX + "Suspending the sync session");
        
        //check if the session should be suspended
        if ( (sessionState == Session.STATE_CLIENT_INIT) || (sessionState == Session.STATE_SERVER_INIT) )
        {
            if (log != null)
                log.info(LOG_PREFIX + "Session hasn't started yet - cancelling session");
            cancelSession();
            return false;
        }
        else if ( (sessionState == Session.STATE_CLIENT_MAPS) || (sessionState == Session.STATE_COMPLETE) )
        {
            if (log != null)
                log.info(LOG_PREFIX + "Session almost completed - ignoring suspend");
            return false;
        }
        
        //suspend the session - indicates that the next message to the server should contain a suspend Alert
        suspendSession = true;
        return true;
    }
    
    /* Indicates the servers response to a suspend Alert (intentional suspend only). */
    private synchronized void onSuspendResult(int statusCode, String statusData)
    {
        suspendSession = false;
        sessionSuspended = SyncML.isSuccessStatus(statusCode);

        //notify the listener of the suspend result
        syncManager.getSyncListener().onSuspendResult(statusCode, statusData);
    }
    
    /* Handles the case where the session is suspended (either intentionally or unintentionally). */
    private void sessionSuspended()
    {
        suspendSession = false;
        sessionSuspended = true;
        
        //notify each record store that the session is suspended
        for (int i = 0; i < syncStores.length; i++)
            syncStores[i].onSyncSuspend();

        //notify the listener that the session is suspended
        syncManager.getSyncListener().onSyncSuspend();
    }    
    
    /* Returns whether or not the session is currently suspended. */
    public synchronized boolean isSuspended()
    {
        return sessionSuspended;
    }
    
    /* Returns whether or not the session is currently stopped. */
    public synchronized boolean isStopped()
    {
        return sessionStopped;
    }


    /* Requests that the currently suspended sync session be resumed. */
    public synchronized void resumeSession(String id)
    {
        //make sure the new ID is different from the last one
        if (id.equals(sessionId))
            throw new IllegalArgumentException("session ID '" + id + "' is the same as the previous session ID '" + sessionId + "'");
        
        if (log != null)
            log.info(LOG_PREFIX + "Resuming the sync session with new ID '" + id + "'");
        
        //use the new session ID
        sessionId = id;
        
        //reset the session state 
        suspendedSessionState = sessionState;
        sessionState = Session.STATE_CLIENT_INIT;
        stopSession = false;
        suspendSession = false;
        outMessageId = 1;
        outCommandId = 1;

        //reset the authentication counters to allow re-authentication - this is necessary as the nonce on the client/server  
        //may be out of sync (especially if the session had been suspended due to a connection error)
        outClientAuthCount = 0;
        inServerAuthCount = 0;

        //cleanup old data
        outStatusCmds.removeAllElements();
        outReplyCmds.removeAllElements();
        
        //resume the session - indicates that the next message to the server should contain a resume Alert
        resumeSession = true;
    }
    
    /* Returns whether or not the session is currently being resumed. */
    public synchronized boolean isResuming()
    {
        return resumeSession;
    }
    
    /* Indicates the servers response to our resume Alert. */
    private synchronized void onResumeResult(String clientUri, int statusCode, String statusData)
    {
        resumeSession = false;
        sessionResumed = true;
        
        //notify the listener that the session is being resumed - only do this once
        if (sessionSuspended)
        {
            sessionSuspended = false;
            syncManager.getSyncListener().onSyncResume(true);
        }
     
        //determine the record store that the resume result refers to
        RecordStoreState storeState = getRecordStore(clientUri);
        if (storeState == null)
        {
            if (log != null)
                log.error(LOG_PREFIX + "Failed to identify local record store with URI '" + clientUri + "'", null);
            stopSession(SyncML.STATUS_NOT_FOUND, null);
            return;
        }

        //notify the record store of the result of the resume request
        storeState.store.onResumeResult(statusCode, statusData);
        
        //stop the session for the store if the the resume request has failed
        if (! SyncML.isSuccessStatus(statusCode))
            stopSession(storeState, statusCode, statusData);
    }
    
    
    /* Runs the sync session as a separate thread. */
    public void run()
    {
        //run the session
        runSession();
    }

    /* Executes the sync session. */
    private void runSession()
    {
        //try to start the session
        if (! sessionStarting())
            return;
        //run the session until it is completed
        while (sessionState != Session.STATE_COMPLETE)
        {
            try
            {
                //check if the session should be stopped due to an error - if so, there is no point trying 
                //to send any status results back to the server
                if ( (stopSession) && (! isCancelled()) )
                {
                    sessionStopped(false);
                    return;
                }

                //check if the session has been suspended (and we're not trying to resume it)
                if ( (sessionSuspended || suspendSession) && (! resumeSession) )
                {
                    sessionSuspended();
                    return;
                }
                
//                //now the sync type is confirmed, prepare the contacts to sync
//                if(sessionState == Session.STATE_CLIENT_UPDATES)
//                {
//                	for (int startedIndex = 0; startedIndex < syncStores.length; startedIndex++)
//                        syncStores[startedIndex].onUpdateContacts();
//                }

                //get the output stream 
                OutputStream outputStream = syncManager.getTransport().getOutputStream();
                if (outputStream == null)
                    throw new SyncException("no transport output stream specified");
                
                //send the next message using the output stream
                syncManager.getSyncListener().onMessageSend();
                sendMessage( new CountingOutputStream(outputStream) );
                outMessageId++;
                outCommandId = 1;
                
                //get the input stream 
                InputStream inputStream = syncManager.getTransport().getInputStream();
                if (inputStream == null)
                    throw new SyncException("no transport input stream specified");
                String contentType = syncManager.getTransport().getContentType();
                if ( (contentType == null) || (! contentType.equals(Transport.CONTENT_TYPE_WBXML)) )
                    throw new SyncException("unknown or unsupported content type '" + contentType + "'");

                //check if the session should be stopped due to a user request - we only check this 
                //at this point to ensure that any status results have been returned to the server 
                if ( (stopSession == true) && (isCancelled()) )
                {
                    sessionStopped(false);
                    return;
                }
                
                //read the servers response using the input stream
                syncManager.getSyncListener().onMessageReceive();
                readMessage(inputStream);
            }
            catch (IOException e)
            {
                if (resumeSession)
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Temporary error during sync session resume - session is still suspended", e);
                    syncManager.getSyncListener().onSyncResume(false);
                    
                    //restore suspended state
                    resumeSession = false;
                    sessionResumed = false;
                    sessionState = suspendedSessionState;
                }
                else if ( (sessionState == STATE_CLIENT_UPDATES) || (sessionState == STATE_SERVER_UPDATES) || (sessionState == STATE_CLIENT_MAPS) )
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Temporary error during sync session - suspending the session", e);
                    sessionSuspended();
                }
                else
                {
                    if (log != null)
                        log.error(LOG_PREFIX + "Temporary error during sync session - ending the session", e);
                    sessionStatusCode = SyncML.STATUS_SERVICE_UNAVAILABLE;
                    sessionStatusData = null;
                    sessionStopped(false);
                }

                return;
            }
            catch (SyncException e)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "Sync exception during sync session - ending the session", e);
                sessionStatusCode = e.getStatusCode();
                sessionStatusData = e.getStatusData();
                sessionStopped(false);
                return;
            }
            catch (Throwable e)
            {
                if (log != null)
                    log.error(LOG_PREFIX + "General exception during sync session - ending the session", e);
                sessionStatusCode = SyncML.STATUS_SYNC_FAILURE;
                sessionStatusData = null;
                sessionStopped(false);
                return;
            }
            finally
            {
                //cleanup the transport in all cases
                syncManager.getTransport().cleanup();
            }
        }
        
        //session completed successfully
        sessionStopped(true);
    }
}
