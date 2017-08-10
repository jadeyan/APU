/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;

import java.util.Vector;


/**
 * A class representing the status of a SyncML command.
 *
 * @author Denis Evoy
 */
public class Status
{
    public int commandId;                       //the ID of the status command
    public int refMessageId;                    //the ID of the message containing the command that this status refers to
    public int refCommandId;                    //the ID of the command that this status refers to
    public String refCommand;                   //the command that this status refers to
    public Vector refItemTargetUris;            //the target URIs of the items within the command that this status refers to - a collection of String objects
    public Vector refItemSourceUris;            //the source URIs of the items within the command that this status refers to - a collection of String objects
    public int statusCode;                      //the result of the command that this status refers to
    public Cred credentials;                    //the credentials to use
    public Chal challenge;                      //the authentication challenge
    public Vector items;                        //additional status items - a collection of SyncItem objects

    
    public Status()
    {
        super();
        
        clear();
    }

    
    public void clear()
    {
        commandId = -1;
        refMessageId = -1;
        refCommandId = -1;
        refCommand = null;
        refItemTargetUris = new Vector();
        refItemSourceUris = new Vector();
        statusCode = -1;
        credentials = null;
        challenge = null;
        items = new Vector();
    }
}
