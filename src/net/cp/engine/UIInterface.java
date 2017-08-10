/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

/**
 * An interface defining how the sync engine can interact with the application.
 * Note that not all these methods are relevant in every given context.
 *
 * @author James O'Connor
 */
public interface UIInterface
{
    /**
     * Updates the sync session progress with the specified details.
     *
     * @param progress that current status of teh sync
     */
    public void updateSyncProgress(SyncProgress progress);

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
    public int getSyncState();

    /** Called when the engine has finished calculating the changes. */
    public void onGetChangesEnd();

    /** Called when an error occurs during the sync session. */
    public void onSyncError();

    /** Called when the sync has ended, no matter how. */
    public void onSyncEnd();

    /** Only called if this UI is registered to receive Server Alerts.
     *  Called when a verified Server Alert has been received.
     *  @param data The server alert payload, from which the full alert can be re-created
     */
    public void serverAlertReceived(byte[] data);

    /** Called when syncable items have changed, and it is time to sync.
     *  It handles the timeout, and ignores changes during sync.
     *  Only called if this UI is registered to receive CIS notifications.
     *
     *  @param mediaType the type of items that have changed.
     *  Currently only EngineSettings.MEDIA_TYPE_CONTACTS supported
     *
     *  @param numberOfChanges the number of changes detected,
     *  or -1 if this information could not be determined.
     *  Note that if settings.contactMinSyncLimit is set to 0, numberOfChanges will always be -1.
     */
    public void onItemsChanged(int mediaType, int numberOfChanges);

	public void onAlertSlowSync();
}
