/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import net.cp.ac.ui.UICallbackInterface;
import net.cp.ac.core.ParcelableSyncProgress;

/**
 * This interface defines the most important methods of the SyncEngineService.
 * It is also used to define the RPC interface between the service and any attached UIs
 */
interface SyncEngineInterface
{
    /**
     * Initiate sync sessions for the given types
     * @param syncMediaTypes The types of items to sync
     * @return true if the sync was successfully started, otherwise false.
     */
    boolean startSync(int syncMediaTypes);
    
    /**
     * This method is used to identify what state the sync is in.
     * @return one of the following in order of decreasing precedance:
     * 
     * StatusCodes.SYNC_ABORTING
     * StatusCodes.SYNC_SUSPENDING
     * StatusCodes.SYNC_RESUMING
     * StatusCodes.SYNC_SUSPENDED
     * StatusCodes.SYNC_IN_PROGRESS
     * StatusCodes.NONE
     * 
     */
    int getSyncState();
    
    /** call this to abort a currently running sync */
    void abortSync();
    
    /**
     * Called to suspend the sync session in progress.
     * The session can be resumed for a certain period of time before it is aborted.
     */
    void suspendSync();
     
    /**
     * Called to resume the suspended sync session.
     */
    void resumeSync();
    
    /** get the lastest progress/status of the current sync. null if we are not currently syncing */
    ParcelableSyncProgress getLastProgress();
    
    /** pass in an interface to a remote UI. This will be used to report sync status and progress */
    void registerCallback(in UICallbackInterface uiInterface);
    
    /**
     * pass in an interface to a remote UI. This will be the UI that responds to server alerts.
     * Only one UI can get the alerts, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
    void setServerAlertConsumer(in UICallbackInterface uiInterface);
    
    /**
     * pass in an interface to a remote UI. This will be the UI that responds to CIS event.
     * Only one UI can get the event, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
    void setCISConsumer(in UICallbackInterface uiInterface);
    
    /**
     * pass in an interface to a remote UI. This will be the UI that responds to periodic sync event.
     * Only one UI can get the event, so subsequent calls to this method replace the old consumer.
     * Calling this with null removes the last set consumer
     */
    void setPISConsumer(in UICallbackInterface uiInterface);
    
    /**
     * This calculates the number of contacts that have changed since the last sync,
     * by running through the contacts and calculating the sync state.
     * This is an expensive operation, so this method should be called sparingly.

     * @param saveChangelogs if true, the changelogs calculated here will be used in the subsequent sync. 
     * @return the number of contacts that have changed since the last sync,
     * or -1 if the operation could not be completed.
     */
    int getNumberChangedContacts(boolean saveChangelogs);
    
    /**
     * @return The battery level in percent, or 100 if unknown
     */
    int getBatteryPercent();
}