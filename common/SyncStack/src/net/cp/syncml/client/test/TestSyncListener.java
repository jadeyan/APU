/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test;


import java.util.*;

import net.cp.syncml.client.*;
import net.cp.syncml.client.util.Logger;


/**
 * A default SyncListener class that simply logs sync session activity. 
 *
 * @author Denis Evoy
 */
public class TestSyncListener implements SyncListener
{
	private static final String[] STRINGS_ESCAPED =     { "\\\\", "\\r", "\\n" };
    private static final String[] STRINGS_UNESCAPED =   { "\\",   "\r",  "\n" };
    
    
    Logger syncLogger;
    SyncManager syncManager;

    int syncSuspendSendCount;
    int syncSuspendReceiveCount;
    int syncResumeDelay;
    Timer syncResumeTimer;
    
    boolean syncSuccess;

    int incomingAddCount;
    int incomingAddIncompleteCount;
    int incomingAddFailureCount;
    int incomingReplaceCount;
    int incomingReplaceFailureCount;
    int incomingDeleteCount;
    int incomingDeleteFailureCount;
    int incomingMoveCount;
    int incomingMoveFailureCount;
    int incomingCopyCount;
    int incomingCopyFailureCount;
    
    int outgoingAddCount;
    int outgoingAddSuccessCount;
    int outgoingAddFailCount;
    int outgoingAddAlreadyExistsCount;
    int outgoingAddIncompleteCount;

    int outgoingReplaceCount;
    int outgoingReplaceSuccessCount;
    int outgoingReplaceFailCount;
    int outgoingReplaceNoSuchRecordCount;
    
    int outgoingDeleteCount;
    int outgoingDeleteSuccessCount;
    int outgoingDeleteFailCount;
    int outgoingDeleteNoSuchRecordCount;
    
    int outgoingMoveCount;
    int outgoingMoveSuccessCount;
    int outgoingMoveFailCount;
    int outgoingMoveNoSuchRecordCount;
    
    int outgoingCopyCount;
    int outgoingCopySuccessCount;
    int outgoingCopyFailCount;
    int outgoingCopyNoSuchRecordCount;
    
    int outgoingMsgCount;
    int incomingMsgCount;
    
    int suspendCount;

    String displayAlert;
    
    int displayAlertStatus;
    int displayAlertDelay;
    
    /** 
     * Creates a new default listener which logs session activity to the specified logger.
     *  
     * @param logger the logger used to log activity. 
     */
    public TestSyncListener(Logger logger) 
    { 
        this(logger, 0, 0, 0, SyncML.STATUS_OK, 0);
    }

    /** 
     * Creates a new default listener which logs session activity to the specified logger, suspends the session at the specified points and resumes it after the specified delay.
     *  
     * @param logger                the logger used to log activity. 
     * @param suspendSendCount      suspend the sync session after this number of sync messages have been sent.
     * @param suspendReceiveCount   suspend the sync session after this number of sync messages have been received.  
     * @param resumeDelay           the number of seconds to wait before resuming the sync session again.           
     * @param dispAlertStatus       the status to return when an alert is received to display information to the user.
     * @param dispAlertDelay        the number of seconds to wait while displaying information to the user.
     */
    public TestSyncListener(Logger logger, int suspendSendCount, int suspendReceiveCount, int resumeDelay, int dispAlertStatus, int dispAlertDelay) 
    { 
        syncLogger = logger;

        syncSuspendSendCount = suspendSendCount;
        syncSuspendReceiveCount = suspendReceiveCount;
        syncResumeDelay = resumeDelay;
        displayAlertStatus = dispAlertStatus;
        displayAlertDelay = dispAlertDelay;
    }

    
    public void onSyncStart()
    {
        logInfo("Sync session starting");
        
        syncSuccess = false;
        
        incomingAddCount = 0;
        incomingAddIncompleteCount = 0;
        incomingAddFailureCount = 0;
        incomingReplaceCount = 0;
        incomingReplaceFailureCount = 0;
        incomingDeleteCount = 0;
        incomingDeleteFailureCount = 0;
        incomingMoveCount = 0;
        incomingMoveFailureCount = 0;
        incomingCopyCount = 0;
        incomingCopyFailureCount = 0;
        
        outgoingAddCount = 0;
        outgoingAddSuccessCount = 0;
        outgoingAddFailCount = 0;
        outgoingAddAlreadyExistsCount = 0;
        outgoingAddIncompleteCount = 0;

        outgoingReplaceCount = 0;
        outgoingReplaceSuccessCount = 0;
        outgoingReplaceFailCount = 0;
        outgoingReplaceNoSuchRecordCount = 0;
        
        outgoingDeleteCount = 0;
        outgoingDeleteSuccessCount = 0;
        outgoingDeleteFailCount = 0;
        outgoingDeleteNoSuchRecordCount = 0;
        
        outgoingMoveCount = 0;
        outgoingMoveSuccessCount = 0;
        outgoingMoveFailCount = 0;
        outgoingMoveNoSuchRecordCount = 0;
        
        outgoingCopyCount = 0;
        outgoingCopySuccessCount = 0;
        outgoingCopyFailCount = 0;
        outgoingCopyNoSuchRecordCount = 0;
        
        outgoingMsgCount = 0;
        incomingMsgCount = 0;
        
        suspendCount = 0;
    }
    
    public void onSyncSuspend()
    {
        logInfo("Sync session has been suspended - resuming in " + syncResumeDelay + " seconds");
        
        suspendCount++;

        //schedule the session to be resumed after the configured delay
        if (syncResumeDelay > 0)
        {
            syncResumeTimer = new Timer();
            syncResumeTimer.schedule(new ResumeSessionTask(syncManager), (syncResumeDelay * 1000));
        }
    }
    
    public void onSyncResume(boolean success)
    {
        if (success)
            logInfo("Request to resume session - success");
        else
            logInfo("Request to resume session - failed");
            
        if (syncResumeTimer != null)
        {
            syncResumeTimer.cancel();
            syncResumeTimer = null;
        }
    }

    public void onSyncEnd(boolean success, int statusCode, String statusData)
    {
        syncSuccess = success;
        if (success)
            logInfo("Sync session ending - success");
        else
            logInfo("Sync session ending - failure - status '" + statusCode + "' - reason '" + statusData + "'");

        logInfo("Outgoing add:           " + outgoingAddCount);
        logInfo("    Successful add:     " + outgoingAddSuccessCount);
        logInfo("    Failed add:         " + outgoingAddFailCount);
        logInfo("    Already exists:     " + outgoingAddAlreadyExistsCount);
        logInfo("    Incomplete:         " + outgoingAddIncompleteCount);

        logInfo("Outgoing replace:       " + outgoingReplaceCount);
        logInfo("    Successful replace: " + outgoingReplaceSuccessCount);
        logInfo("    Failed replace:     " + outgoingReplaceFailCount);
        logInfo("    No such record:     " + outgoingReplaceNoSuchRecordCount);

        logInfo("Outgoing delete:        " + outgoingDeleteCount);
        logInfo("    Successful delete:  " + outgoingDeleteSuccessCount);
        logInfo("    Failed delete:      " + outgoingDeleteFailCount);
        logInfo("    Already gone:       " + outgoingDeleteNoSuchRecordCount);

        logInfo("Outgoing move:          " + outgoingMoveCount);
        logInfo("    Successful move:    " + outgoingMoveSuccessCount);
        logInfo("    Failed move:        " + outgoingMoveFailCount);
        logInfo("    Already gone:       " + outgoingMoveNoSuchRecordCount);
        
        logInfo("Outgoing copy:          " + outgoingCopyCount);
        logInfo("    Successful copy:    " + outgoingCopySuccessCount);
        logInfo("    Failed copy:        " + outgoingCopyFailCount);
        logInfo("    Already gone:       " + outgoingCopyNoSuchRecordCount);
        
        logInfo("Incoming add:           " + incomingAddCount);
        logInfo("Incoming add incomplete:" + incomingAddIncompleteCount);
        logInfo("Incoming failed add:    " + incomingAddFailureCount);
        logInfo("Incoming replace:       " + incomingReplaceCount);
        logInfo("Incoming failed replace:" + incomingReplaceFailureCount);
        logInfo("Incoming delete:        " + incomingDeleteCount);
        logInfo("Incoming failed delete: " + incomingDeleteFailureCount);
        logInfo("Incoming move:          " + incomingMoveCount);
        logInfo("Incoming failed move:   " + incomingMoveFailureCount);
        logInfo("Incoming copy:          " + incomingCopyCount);
        logInfo("Incoming failed copy:   " + incomingCopyFailureCount);

        logInfo("Outgoing messages:      " + outgoingMsgCount);
        logInfo("Incoming messages:      " + incomingMsgCount);
        
        logInfo("Session suspended:      " + suspendCount);
    }
    
    
    public void onSuspendResult(int statusCode, String statusData)
    {
        if (SyncML.isSuccessStatus(statusCode))
            logInfo("Suspend request result from server - success - status '" + statusCode + "'");
        else
            logInfo("Suspend request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
    }
    
    public void onResumeResult(int statusCode, String statusData)
    {
        if (SyncML.isSuccessStatus(statusCode))
            logInfo("Resume request result from server - success - status '" + statusCode + "'");
        else
            logInfo("Resume request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
    }

    public void onAddResult(int statusCode, String statusData)
    {
        outgoingAddCount++;
        if (statusCode == SyncML.STATUS_ITEM_ADDED)
        {
            logInfo("Add request result from server - new record added");
            outgoingAddSuccessCount++;
        }
        else if (statusCode == SyncML.STATUS_ITEM_ALREADY_EXISTS)
        {
            logInfo("Add request result from server - record already exists");
            outgoingAddAlreadyExistsCount++;
        }
        else if (statusCode == SyncML.STATUS_INCOMPLETE_COMMAND)
        {
            logInfo("Add request result from server - incomplete command, need the full item");
            outgoingAddIncompleteCount++;
        }
        else if (SyncML.isSuccessStatus(statusCode))
        {
            logInfo("Add request result from server - success - status '" + statusCode + "'");
            outgoingAddSuccessCount++;
        }
        else
        {
            logInfo("Add request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
            outgoingAddFailCount++;
        }
    }
    
    public void onReplaceResult(int statusCode, String statusData)
    {
        outgoingReplaceCount++;
        if (statusCode == SyncML.STATUS_ITEM_GONE)
        {
            logInfo("Replace request result from server - record doesn't exist");
            outgoingReplaceNoSuchRecordCount++;
        }
        else if (SyncML.isSuccessStatus(statusCode))
        {
            logInfo("Replace request result from server - success - status '" + statusCode + "'");
            outgoingReplaceSuccessCount++;
        }
        else
        {
            logInfo("Replace request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
            outgoingReplaceFailCount++;
        }
    }
    
    public void onDeleteResult(int statusCode, String statusData)
    {
        outgoingDeleteCount++;
        if (statusCode == SyncML.STATUS_ITEM_GONE)
        {
            logInfo("Delete request result from server - record doesn't exist");
            outgoingDeleteNoSuchRecordCount++;
        }
        else if (statusCode == SyncML.STATUS_ITEM_NOT_DELETED)
        {
            logInfo("Delete request result from server - record doesn't exist");
            outgoingDeleteNoSuchRecordCount++;
        }
        else if (SyncML.isSuccessStatus(statusCode))
        {
            logInfo("Delete request result from server - success - status '" + statusCode + "'");
            outgoingDeleteSuccessCount++;
        }
        else
        {
            logInfo("Delete request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
            outgoingDeleteFailCount++;
        }
    }
    
    public void onMoveResult(int statusCode, String statusData)
    {
        outgoingMoveCount++;
        if (statusCode == SyncML.STATUS_ITEM_GONE)
        {
            logInfo("Move request result from server - record doesn't exist");
            outgoingMoveNoSuchRecordCount++;
        }
        else if (statusCode == SyncML.STATUS_ITEM_NOT_DELETED)
        {
            logInfo("Move request result from server - record doesn't exist");
            outgoingMoveNoSuchRecordCount++;
        }
        else if (SyncML.isSuccessStatus(statusCode))
        {
            logInfo("Move request result from server - success - status '" + statusCode + "'");
            outgoingMoveSuccessCount++;
        }
        else
        {
            logInfo("Move request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
            outgoingMoveFailCount++;
        }
    }
    
    public void onCopyResult(int statusCode, String statusData)
    {
        outgoingCopyCount++;
        if (statusCode == SyncML.STATUS_ITEM_GONE)
        {
            logInfo("Copy request result from server - record doesn't exist");
            outgoingCopyNoSuchRecordCount++;
        }
        else if (statusCode == SyncML.STATUS_ITEM_NOT_DELETED)
        {
            logInfo("Copy request result from server - record doesn't exist");
            outgoingCopyNoSuchRecordCount++;
        }
        else if (SyncML.isSuccessStatus(statusCode))
        {
            logInfo("Copy request result from server - success - status '" + statusCode + "'");
            outgoingCopySuccessCount++;
        }
        else
        {
            logInfo("Copy request result from server - failure - status '" + statusCode + "' - reason '" + statusData + "'");
            outgoingCopyFailCount++;
        }
    }
    

    public void onAddRequest(int statusCode)
    {
    	if (SyncML.isSuccessStatus(statusCode))
    	{
	        logInfo("Add request from server");
	        incomingAddCount++;
	    }
    	else if (statusCode == SyncML.STATUS_INCOMPLETE_COMMAND)
    	{
    		logInfo("Add request from server requires full add");
        	incomingAddIncompleteCount++;
    	}
    	else
	    {
	        logInfo("Failed to process add request from server");
	        incomingAddFailureCount++;
	    }
    }
    
    public void onReplaceRequest(int statusCode)
    {
    	if (SyncML.isSuccessStatus(statusCode))
    	{
	        logInfo("Replace request from server");
	        incomingReplaceCount++;
	    }
    	else
		{
		    logInfo("Failed to process Replace request from server");
		    incomingReplaceFailureCount++;
		}
    }
    
    public void onDeleteRequest(int statusCode)
    {
    	if (SyncML.isSuccessStatus(statusCode))
    	{
	        logInfo("Delete request from server");
	        incomingDeleteCount++;
	    }
    	else
	    {
	        logInfo("Failed to process Delete request from server");
	        incomingDeleteFailureCount++;
	    }
    }
    
    public void onMoveRequest(int statusCode)
    {
    	if (SyncML.isSuccessStatus(statusCode))
    	{
	        logInfo("Move request from server");
	        incomingMoveCount++;
	    }
    	else
    	{
	        logInfo("Failed to process Move request from server");
	        incomingMoveFailureCount++;
	    }
    }
    	
    public void onCopyRequest(int statusCode)
    {
    	if (SyncML.isSuccessStatus(statusCode))
    	{
	        logInfo("Copy request from server");
	        incomingCopyCount++;
	    }
    	else
    	{
    		logInfo("Failed to process Copy request from server");
	        incomingCopyFailureCount++;
    	}
    }
    
    public void onDisplayRequest(byte[] data)
        throws SyncException
    {
    	try
    	{
    		if (data != null)
    			displayAlert = new String(data, "UTF-8");
    	}
    	catch(Throwable e)
    	{
    		logInfo("Failed to UTF-8 decode display data: " + e);
    		throw new SyncException("Failed to UTF-8 decode display data", e);
    	}
    	
        //wait a while if required
    	if (displayAlertDelay > 0)
    	{
        	try
            {
                Thread.sleep(displayAlertDelay * 1000);
            }
            catch (InterruptedException e)
            {
                //ignore
            }
    	}
    	
    	if (! SyncML.isSuccessStatus(displayAlertStatus))
    	    throw new SyncException("Failed to display data - status '" + displayAlertStatus + "'", displayAlertStatus);
    }
    
    public void onMessageSend()
    {
        logInfo("Outgoing message to the server");
        outgoingMsgCount++;
        
        //suspend the session if required
        if ( (syncManager != null) && (outgoingMsgCount == syncSuspendSendCount) )
        {
            logInfo("Suspending session after sending message " + outgoingMsgCount);
            syncManager.suspendSync();
        }
    }

    public void onMessageReceive()
    {
        logInfo("Incoming message from the server");
        incomingMsgCount++;
        
        //suspend the session if required
        if ( (syncManager != null) && (incomingMsgCount == syncSuspendReceiveCount) )
        {
            logInfo("Suspending session after receiving message " + outgoingMsgCount);
            syncManager.suspendSync();
        }
    }
    

    public void setSyncManager(SyncManager manager)
    {
        syncManager = manager;
    }
    
    public boolean isSyncSuccess()
    {
        return syncSuccess;
    }
    
    public int getIncomingAddCount()
    {
    	return incomingAddCount;
    }
    
    public int getIncomingAddIncompleteCount()
    {
    	return incomingAddIncompleteCount;
    }
    
    public int getIncomingAddFailureCount()
    {
    	return incomingAddFailureCount;
    }
    
    public int getIncomingReplaceCount()
    {
    	return incomingReplaceCount;
    }
    
    public int getIncomingReplaceFailureCount()
    {
    	return incomingReplaceFailureCount;
    }
    
    public int getIncomingDeleteCount()
    {
    	return incomingDeleteCount;
    }
    
    public int getIncomingDeleteFailureCount()
    {
    	return incomingDeleteFailureCount;
    }
    
    public int getIncomingMoveCount()
    {
    	return incomingMoveCount;
    }
    
    public int getIncomingMoveFailureCount()
    {
    	return incomingMoveFailureCount;
    }
    
    public int getIncomingCopyCount()
    {
    	return  incomingCopyCount;
    }
    
    public int getIncomingCopyFailureCount()
    {
    	return  incomingCopyFailureCount;
    }
    
    public int getOutgoingAddCount()
    {
    	return outgoingAddCount;
    }
    
    public int getOutgoingAddSuccessCount()
    {
    	return outgoingAddSuccessCount;
    }
    
    public int getOutgoingAddFailCount()
    {
    	return outgoingAddFailCount;
    }
    
    public int getOutgoingAddAlreadyExistsCount()
    {
    	return outgoingAddAlreadyExistsCount;
    }
    
    public int getOutgoingAddIncompleteCount()
    {
    	return outgoingAddIncompleteCount;
    }

    public int getOutgoingReplaceCount()
    {
    	return outgoingReplaceCount;
    }
    
    public int getOutgoingReplaceSuccessCount()
    {
    	return outgoingReplaceSuccessCount;
    }
    
    public int getOutgoingReplaceFailCount()
    {
    	return outgoingReplaceFailCount;
    }
    
    public int getOutgoingReplaceNoSuchRecordCount()
    {
    	return outgoingReplaceNoSuchRecordCount;
    }
    
    public int getOutgoingDeleteCount()
    {
    	return outgoingDeleteCount;
    }
    
    public int getOutgoingDeleteSuccessCount()
    {
    	return outgoingDeleteSuccessCount;
    }
    
    public int getOutgoingDeleteFailCount()
    {
    	return outgoingDeleteFailCount;
    }
    
    public int getOutgoingDeleteNoSuchRecordCount()
    {
    	return outgoingDeleteNoSuchRecordCount;
    }
    
    public int getOutgoingMoveCount()
    {
    	return outgoingMoveCount;
    }
    
    public int getOutgoingMoveSuccessCount()
    {
    	return outgoingMoveSuccessCount;
    }
    
    public int getOutgoingMoveFailCount()
    {
    	return outgoingMoveFailCount;
    }
    
    public int getOutgoingMoveNoSuchRecordCount()
    {
    	return outgoingMoveNoSuchRecordCount;
    }
    
    public int getOutgoingCopyCount()
    {
    	return outgoingCopyCount;
    }
    
    public int getOutgoingCopySuccessCount()
    {
    	return outgoingCopySuccessCount;
    }
    
    public int getOutgoingCopyFailCount()
    {
    	return outgoingCopyFailCount;
    }
    
    public int getOutgoingCopyNoSuchRecordCount()
    {
    	return outgoingCopyNoSuchRecordCount;
    }
    
    public int getOutgoingMessageCount()
    {
    	return outgoingMsgCount;
    }
    
    public int getIncomingMessageCount()
    {
    	return incomingMsgCount;
    }
    
    public int getSuspendCount()
    {
        return suspendCount;
    }
    
    public int getAllOutgoingFailureCount(boolean strict)
    {
    	int count = 0;
    	if (strict)
    		count = outgoingAddIncompleteCount;

    	return count + outgoingAddFailCount
            		 + outgoingAddAlreadyExistsCount
            		 + outgoingReplaceFailCount
            		 + outgoingReplaceNoSuchRecordCount
            		 + outgoingDeleteFailCount
            		 + outgoingDeleteNoSuchRecordCount
            	 	 + outgoingMoveFailCount
            	 	 + outgoingMoveNoSuchRecordCount
            		 + outgoingCopyFailCount
            		 + outgoingCopyNoSuchRecordCount;
    }
    
    public int getAllOutgoingSuccessCount()
    {
    	return outgoingAddSuccessCount
            		+ outgoingReplaceSuccessCount
            		+ outgoingDeleteSuccessCount
            		+ outgoingMoveSuccessCount
            		+ outgoingCopySuccessCount;
    }
    
    public int getAllOutgoingChangeCount(boolean strict)
    {
        return getAllOutgoingSuccessCount() + getAllOutgoingFailureCount(strict);
    }

    public int getAllIncomingChangeCount(boolean strict)
    {
        return getAllIncomingSuccessCount() + getAllIncomingFailureCount(strict); 
    }
    
    public int getAllIncomingSuccessCount()
    {
        return incomingAddCount
                    + incomingReplaceCount 
                    + incomingDeleteCount 
                    + incomingMoveCount
                    + incomingCopyCount;
    }
    
    public int getAllIncomingFailureCount(boolean strict)
    {
    	int count = 0;
    	if (strict)
    		count = incomingAddIncompleteCount;
    		
    	return count + incomingAddFailureCount 
        	         + incomingReplaceFailureCount 
        	         + incomingDeleteFailureCount 
        	         + incomingMoveFailureCount
        	         + incomingCopyFailureCount;
    }

    public String getDisplayAlert()
    {
    	return displayAlert;
    }
    
    /**
     * Updates the result of the sync with the specified details. 
     * 
     * This is used by the test harness when an external sync test application is used. The external application
     * prints the sync status to SDTOUT, and the test harness parses this and manually populates the listener.
     */
    public void setResult(String name, String value)
    {
    	if ( (name == null) || (name.length() <= 0) )
    		return;
   		
    	if (name.equalsIgnoreCase("Sync session ending"))
    	    syncSuccess = (value.equalsIgnoreCase("success"));

    	else if (name.equalsIgnoreCase("Incoming add"))
    		incomingAddCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming add incomplete"))
    		incomingAddIncompleteCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming failed add"))
    		incomingAddFailureCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Incoming replace"))
    		incomingReplaceCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming failed replace"))
    		incomingReplaceFailureCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Incoming delete"))
    		incomingDeleteCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming failed delete"))
    		incomingDeleteFailureCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Incoming move"))
    		incomingMoveCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming failed move"))
    		incomingMoveFailureCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Incoming copy"))
    		incomingCopyCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming failed copy"))
    		incomingCopyFailureCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Outgoing add"))
    		outgoingAddCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Successful add"))
    		outgoingAddSuccessCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Failed add"))
    		outgoingAddFailCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Already exists add"))
    		outgoingAddAlreadyExistsCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incomplete add"))
    		outgoingAddIncompleteCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Outgoing replace"))
    		outgoingReplaceCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Successful replace"))
    	    outgoingReplaceSuccessCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Failed replace"))
    		outgoingReplaceFailCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("No such record replace"))
    		outgoingReplaceNoSuchRecordCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Outgoing delete"))
    		outgoingDeleteCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Successful delete"))
    		outgoingDeleteSuccessCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Failed delete"))
    		outgoingDeleteFailCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Already gone delete"))
    		outgoingDeleteNoSuchRecordCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Outgoing move"))
    		outgoingMoveCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Successful move"))
    		outgoingMoveSuccessCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Failed move"))
    		outgoingMoveFailCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Already gone move"))
    		outgoingMoveNoSuchRecordCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Outgoing copy"))
    		outgoingCopyCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Successful copy"))
    		outgoingCopySuccessCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Failed copy"))
    		outgoingCopyFailCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Already gone copy"))
    		outgoingCopyNoSuchRecordCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Outgoing messages"))
    		outgoingMsgCount = Integer.parseInt(value);
    	else if (name.equalsIgnoreCase("Incoming messages"))
    		incomingMsgCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("Session suspended"))
    		suspendCount = Integer.parseInt(value);
    	
    	else if (name.equalsIgnoreCase("displayAlert"))
    		displayAlert = unescape(value, STRINGS_ESCAPED, STRINGS_UNESCAPED);
    }
    
    public static String replaceAll(String data, String oldString, String newString)
	{
        if ( (data == null) || (data.length() <= 0) )
            return data;

        int index = -1; 
        StringBuffer buffer = new StringBuffer();
	    while ((index = data.indexOf(oldString)) > -1)
	    {
	        buffer.append(data.substring(0, index));
	        buffer.append(newString);

	        data = data.substring(index + oldString.length());
	    }
	    buffer.append(data);
	    
	    return buffer.toString();
	}
	
    public static String unescape(String data, String[] oldStrings, String[] newStrings)
    	throws IllegalArgumentException
    {
        if ( (data == null) || (data.length() <= 0) )
            return data;
        
        if (oldStrings.length != newStrings.length)
            throw new IllegalArgumentException("Incorrect number of replacement strings");
        
        for (int i = 0; i < oldStrings.length; i++)
            data = replaceAll(data, oldStrings[i], newStrings[i]);
        
        return data;
    }
    
    private void logInfo(String message)
    {
        if (syncLogger != null)
            syncLogger.info("LISTENER: " + message);        
    }
    
    
    /* Task which resumes the current sync session. */
    private static class ResumeSessionTask extends TimerTask
    {
        private SyncManager syncManager;
        
        public ResumeSessionTask(SyncManager manager)
        {
            syncManager = manager;
        }
        
        public void run()
        {
            Date now = new Date();
            syncManager.resumeSync(Long.toString(now.getTime()));
        }
    }
}
