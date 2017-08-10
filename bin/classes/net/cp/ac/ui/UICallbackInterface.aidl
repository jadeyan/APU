/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.ui;
import net.cp.ac.core.ParcelableSyncProgress;

/**
 * Defines the interface the service will use to send information to the UI.
 * It is almost identical to UIInterface, except it contains RPC parcelling information.
 *
 * @see net.cp.engine.UIInterface
 */
interface UICallbackInterface
{
    /** 
     * Updates the sync session progress with the specified details.
     * 
     * @param progress that current status of the sync
     */
    void updateSyncProgress(in ParcelableSyncProgress progress);
    
    /** Called when the engine has finished calculating the changes. */
    void onGetChangesEnd();
    
    /** Called when an error occurs during the sync session. */
    void onSyncError();
    
    /** Called when the sync has ended, no matter how. */
    void onSyncEnd();
    
    /** Only called if this UI is registered to receive Server Alerts.
     *  Called when a verified Server Alert has been received.
     *  @param data The server alert payload, from which the full alert can be re-created
     */
    void serverAlertReceived(in byte[] data);
    
   /** Called when syncable items have changed, and it is time to sync.
     *  Only called if this UI is registered to receive CIS notifications.
     *  @param mediaType the type of items that have changed.
     *  Currently only EngineSettings.MEDIA_TYPE_CONTACTS supported
     *  @param numberOfChanges the number of changes detected,
     *  or -1 if this information could not be determined.
     *  Note that if settings.contactMinSyncLimit is set to 0, numberOfChanges will always be -1.
     */
    void onItemsChanged(int mediaType, int numberOfChanges);
    
    void onAlertSlowSync();
}