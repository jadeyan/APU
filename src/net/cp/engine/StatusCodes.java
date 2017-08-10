/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

/**
 *
 * This class contains all the status codes that can be passed from the engine to the UI.
 * These codes are used to communicate sync status, progress, and errors.
 *
 * @author joconnor
 *
 */
public final class StatusCodes
{
    //Used where no message is necessary
    public static final int NONE =                     0;

    //Status Headings - General stage of the sync in progress
    public static final int SYNC_STARTING =            303;
    public static final int SYNC_SERVER_UPDATES =      311;
    public static final int SYNC_CLIENT_UPDATES =      313;
    public static final int SYNC_COMPLETE =            305;
    public static final int SYNC_CHECKING_CONTACTS =   350;

    /**
     * Used to indicate we are in fact syncing (not suspended, suspending, resuming, aborting, or complete).
     */
    public static final int SYNC_IN_PROGRESS =         339;

    /**
     * Used when the session has been successfully suspended
     */
    public static final int SYNC_SUSPENDED =           340;

    /**
     * Used when the session is in the process of suspending
     */
    public static final int SYNC_SUSPENDING =          341;
    public static final int SYNC_RESUMING =            342;
    public static final int SYNC_ABORTING =            336;

    //Status Details - Errors
    public static final int SYNC_ERROR_USER_ABORT =    207;
    public static final int SYNC_ERROR_CONNECTION =    204;
    public static final int SYNC_ERROR_CREDENTIALS =   205;
    public static final int SYNC_ERROR_DEVICE_FULL =   206;
    public static final int SYNC_ERROR_RESUME_FAILURE= 344;
    public static final int SYNC_ERROR_START_FAILED =  335;
    public static final int SYNC_ERROR_PIM_FULL =      356;


    //Status Details - The particular operation in progress
    public static final int SYNC_INITIALIZING =        304;
    public static final int SYNC_ENUMERATING_CONTACTS =302;
    public static final int SYNC_RECEIVING_UPDATE =    312;
    public static final int SYNC_SENDING_UPDATE =      316;
    public static final int SYNC_LOADING_STATE =       317;
    public static final int SYNC_NO_UPDATES =          314;
    public static final int SYNC_DELETED_CONTACT =     352;
    //Note a suspended sync session is considered a session in progress
    public static final int SYNC_MANUALLY_SUSPENDED =  346;
    public static final int SYNC_SUDDENLY_SUSPENDED =  343;

    //Sync Log - overall status of last sync
    public static final int SYNC_FAILED =              200;
    public static final int SYNC_START_FAILED =        203;
    public static final int SYNC_SUCCESS =             201;
    public static final int SYNC_INCOMPLETE =          202;

    //Sync Log - individual item error
    public static final int SYNC_OP_FAILED =           308;
    public static final int SYNC_OP_CONFLICT =         309;

    //Sync Log - operation ID
    public static final int SYNC_ADD =                 320;
    public static final int SYNC_UPDATE =              321;
    public static final int SYNC_DELETE =              322;
    public static final int SYNC_COPY =                323;
    public static final int SYNC_MOVE =                324;

    //Sync Log - sync type
    public static final int SYNC_CONTACT =             325;
    public static final int SYNC_VIDEOS =              326;
    public static final int SYNC_MUSICS =              327;
    public static final int SYNC_PHOTOS =              328;
    public static final int SYNC_OTHERS =              329;
    public static final int SYNC_FOLDER =              332;

    //Sync Log - target device
    public static final int SYNC_TO_PHONE =            330;
    public static final int SYNC_TO_SERVER =           331;

    public static final String STRING_ENCODING =       "UTF-8";
}
