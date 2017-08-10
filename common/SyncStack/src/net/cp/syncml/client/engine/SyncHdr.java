/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


/**
 * A class representing SyncML header information information used during a SyncML session.
 *
 * @author Denis Evoy
 */
public class SyncHdr
{
    public String dtdVersion;                   //the version of the SyncML representation used in the message 
    public String protocolVersion;              //the version of the SyncML protocol used in the message
    public String sessionId;                    //the ID of the session
    public int messageId;                       //the ID of the message
    public String sourceUri;                    //the URI of the device that sent the message
    public String sourceName;                   //the name of the device that sent the message
    public String targetUri;                    //the URI of the device that received the message
    public String targetName;                   //the name of the device that received the message
    public String responseUri;                  //the URI where responses to the message should be sent
    public boolean noResponse;                  //indicates that a response is not required for any command in the message
    public Cred credentials;                    //the supplied credentials (in response to a challenge)
    public Metinf metinf;                       //the meta information associated with the sync header

    
    public SyncHdr()
    {
        super();
        
        clear();
    }


    public void clear()
    {
        dtdVersion = null;
        protocolVersion = null;
        sessionId = null;
        messageId = -1;
        sourceUri = null;
        sourceName = null;
        targetUri = null;
        targetName = null;
        responseUri = null;
        noResponse = false;
        credentials = null;
        metinf = null;
    }
}
