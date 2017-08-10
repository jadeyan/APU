/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import net.cp.syncml.client.util.Logger;


/**
 * This Interface documents the methods that must be implemented by any class that wishes to act as the
 * business logic for an Android sync program.
 * The implementing class will have to decide how to handle
 * CIS (Client Initiated Sync) and SIS (Server Initiated Sync) events, and how these interact with settings and UIs.
 */
public interface BusinessLogic
{
    /**
     *
     * @param service The SyncEngineService.
     * @param logger The logger to use, can be null.
     */
    public void init(SyncEngineService service, Logger logger);

    /** Only called if this class is registered to receive Server Alerts.
     *  Called when a verified Server Alert has been received.
     *  @param data The server alert payload, from which the full alert can be re-created
     */
    public void serverAlertReceived(byte[] data);

    /**
     * Called when syncable items have changed on the device
     *
     * @param mediaType The type of items that have changed
     */
    public void onCISIntent(int mediaType);

    /**
     *
     * @return The type of connection to use for the next sync.
     * This method checks what connections are available,
     * and combines this information with the configured settings to pick a connection.
     * E.g ConnectionState.CONNECTION_TYPE_NO_COST
     *
     */
    public int getSyncConnectionType();

    /**
     * Called when periodic sync id enabled and it's time to sync
     */
	public void onPeriodicSync();
}
