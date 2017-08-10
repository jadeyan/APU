/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client;


import net.cp.syncml.client.devinfo.Device;
import net.cp.syncml.client.store.RecordStore;
import net.cp.syncml.client.util.Logger;


/**
 * An interface allowing notifications of sync session activity to be acted upon. <br/><br/>
 * 
 * Implementations will typically use this interface to inform the user of the progress of 
 * the current sync session.
 * 
 * @see SyncManager#SyncManager(Device, Transport, String, String, SyncListener, Logger)
 *
 * @author Denis Evoy
 */
public interface SyncListener
{
    /** 
     * Called to indicate that a sync session is being started. <br/><br/>
     * 
     * This method will be called just before a new sync session is started (i.e. before any communication
     * has taken place with the SyncML server).
     * 
     * @see RecordStore#onSyncStart()
     */
    public void onSyncStart();
    
    /** 
     * Called to indicate that the sync session has been suspended. <br/><br/>
     * 
     * This method will be called if the sync session has been suspended, regardless of how the
     * suspension was triggered (i.e. intentional or unintentional).  <br/><br/>
     * 
     * Once a session has been suspended, it can either be {@link SyncManager#resumeSync(String) resumed} 
     * or {@link SyncManager#stopSync() cancelled}.
     * 
     * @see RecordStore#onSyncSuspend()
     */
    public void onSyncSuspend();
    
    /** 
     * Called to indicate whether or not the sync session could be resumed. <br/><br/>
     * 
     * This method will be called to indicate if the request to {@link SyncManager#resumeSync(String) resume} 
     * a previously suspended sync session was made successfully. Note that this does not indicate that the 
     * session has actually been successfully resumed. For example, when syncing multiple stores in the same 
     * session, the SyncML server may refuse to resume the session for one store but allow it for another one.
     * 
     * @param success set to <code>true</code> if the request to resume the session was handled by the server or <code>false</code> if the request could not be made (e.g. because of a connection error).
     * @see RecordStore#onResumeResult(int, String)
     */
    public void onSyncResume(boolean success);

    /**
     * Called to indicate that a sync session has ended. <br/><br/>
     * 
     * This method will be called as the sync session ends, regardless of how the end of the session was 
     * reached (i.e. normal session end, user-initiated session stop or error condition). <br/><br/>
     * 
     * Note that the <code>success</code> parameter only refers to the overall success of the session 
     * (i.e. whether or not the session reached completion). Therefore <code>success</code> may be 
     * <code>true</code> even if one of the record stores couldn't be synced. <br/><br/>
     * 
     * If the session has ended in failure, the specified <code>statusCode</code>, if non-zero, indicates 
     * the reason for the failure with the specified <code>statusData</code> possibly containing additional 
     * information.
     * 
     * @param success set to <code>true</code> if the sync session was successful.
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     * 
     * @see RecordStore#onSyncEnd(boolean, int, String)
     */
    public void onSyncEnd(boolean success, int statusCode, String statusData);
    
    
    /** 
     * Called to indicate the SyncML server response to an intentional suspend request from the client.
     *  
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     */ 
    public void onSuspendResult(int statusCode, String statusData);
    
    /** 
     * Called to indicate the SyncML server response to an add request from the client.
     *  
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     */ 
    public void onAddResult(int statusCode, String statusData);
    
    /** 
     * Called to indicate the SyncML server response to a replace request from the client.
     *  
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     */ 
    public void onReplaceResult(int statusCode, String statusData);
    
    /** 
     * Called to indicate the SyncML server response to a delete request from the client.
     *  
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     */
    public void onDeleteResult(int statusCode, String statusData);

    /** 
     * Called to indicate the SyncML server response to a move request from the client.
     *  
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     */
    public void onMoveResult(int statusCode, String statusData);
    
    /** 
     * Called to indicate the SyncML server response to a copy request from the client.
     *  
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating why the session failed. Will be zero or a valid SyncML status code.
     * @param statusData any additional status information (e.g. {@link SyncML#REASON_TOTAL_DATA_LIMIT_EXCEEDED reason code}, etc). May be null.
     */
    public void onCopyResult(int statusCode, String statusData);
    
        
    /** 
     * Called to indicate that the client has received an add request from the SyncML server.
     * 
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating the result of the add by the client.
     */ 
    public void onAddRequest(int statusCode);
    
    /** 
     * Called to indicate that the client has received a replace request from the SyncML server.
     * 
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating the result of the replace by the client.
     */ 
    public void onReplaceRequest(int statusCode);

    /** 
     * Called to indicate that the client has received a delete request from the SyncML server.
     * 
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating the result of the delete by the client.
     */ 
    public void onDeleteRequest(int statusCode);

    /** 
     * Called to indicate that the client has received a move request from the SyncML server.
     * 
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating the result of the move by the client.
     */ 
    public void onMoveRequest(int statusCode);

    /** 
     * Called to indicate that the client has received a copy request from the SyncML server.
     * 
     * @param statusCode the {@link SyncML#STATUS_OK status code} indicating the result of the copy by the client.
     */ 
    public void onCopyRequest(int statusCode);
    
    /** 
     * Called to indicate that the client has received a request from the SyncML server to display the specified data to the user.
     * 
     * @param data the data to display to the user. May be null or empty.
     * @throws SyncException if the data could not be displayed.
     */
    public void onDisplayRequest(byte[] data)
        throws SyncException;
    

    /** Called to indicate that the client is about to send a message to the SyncML server. */ 
    public void onMessageSend();

    /** Called to indicate that the client is about to receive a message from the SyncML server. */ 
    public void onMessageReceive();
}
