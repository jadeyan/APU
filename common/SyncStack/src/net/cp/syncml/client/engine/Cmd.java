/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;

import java.util.Vector;


/**
 * A class representing a SyncML command.
 *
 * @author Denis Evoy
 */
public class Cmd
{
    /* Defines the possible SyncML commands. */
    public static final String CMD_ADD =       "Add";
    public static final String CMD_ALERT =     "Alert";
    public static final String CMD_ATOMIC =    "Atomic";
    public static final String CMD_COPY =      "Copy";
    public static final String CMD_DELETE =    "Delete";
    public static final String CMD_EXEC =      "Exec";
    public static final String CMD_GET =       "Get";
    public static final String CMD_MAP =       "Map";
    public static final String CMD_MOVE =      "Move";
    public static final String CMD_PUT =       "Put";
    public static final String CMD_REPLACE =   "Replace";
    public static final String CMD_RESULTS =   "Results";
    public static final String CMD_SEARCH =    "Search";
    public static final String CMD_SEQUENCE =  "Sequence";
    public static final String CMD_SYNC =      "Sync";

    
    public int messageId;                       //the ID of the message that contains the command

    public String command;                      //the command to perform
    public int commandId;                       //the ID of the command
    public boolean noResponse;                  //indicates that the command requires no response
    public Cred credentials;                    //the credentials for the command
    public Metinf metinf;                       //the meta information associated with the command
    public Vector items;                        //the items associated with the command - a collection of SyncItem objects
    public Cmd parentCmd;                       //the parent command of the command 
    
    public boolean archivedDelete;              //for DELETE commands, indicates that a record should be archived before being deleted
    public boolean softDelete;                  //for DELETE commands, indicates that only a soft delete should be performed
    
    public String language;                     //for GET, PUT and SEARCH commands, indicates the preferred language of the results 

    public int numberOfChanges;                 //for SYNC commands, indicates the number of changes to expect
    public String sourceUri;                    //for SYNC, SEARCH and MAP commands, indicates the source URI
    public String targetUri;                    //for SYNC, SEARCH and MAP commands, indicates the target URI
    
    public Vector mapItems;                     //for MAP commands, indicates the map items - a collection of MapItem objects
    
    public int alertCode;                       //for ALERT commands, indicates the associated alert code
    
    public boolean noResults;                   //for SEARCH and RESULTS commands, indicates that the command requires no results
    public int refCommandId;                    //for RESULTS commands, indicates the ID of the command that the results refers to
    public int refMessageId;                    //for RESULTS commands, the ID of the message containing the command that the results refer to
    public String refItemTargetUri;             //for RESULTS commands, indicates the target URI specified in the original SEARCH command
    public String refItemSourceUri;             //for RESULTS commands, indicates the source URI specified in the original SEARCH command
    
    public byte[] data;                         //for SEARCH commands, indicates additional search data 
    
    
    public Cmd(String cmd)
    {
        super();
        
        command = cmd;
        clear();
    }


    public void clear()
    {
        messageId = -1;
        
        commandId = -1;
        noResponse = false;
        credentials = null;
        metinf = null;
        items = new Vector();
        parentCmd = null;
        
        archivedDelete = false;
        softDelete = false;
        
        language = null;
        
        numberOfChanges = -1;
        sourceUri = null;
        targetUri = null;

        mapItems = new Vector();
        
        alertCode = -1;
        
        noResults = false;
        refCommandId = -1;
        refMessageId = -1;
        refItemTargetUri = null;
        refItemSourceUri = null;
        
        data = null;
    }
}
