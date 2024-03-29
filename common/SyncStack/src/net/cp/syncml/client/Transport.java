/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.cp.syncml.client.devinfo.Device;
import net.cp.syncml.client.util.Logger;


/**
 * An interface defining a transport mechanism used to carry the SyncML protocol. <br/><br/>
 *
 * Implementations should provide an input stream and output stream that the SyncML client
 * can use to read data from and write data to the remote SyncML server.
 * 
 * The SyncML client will typically use the Transport instance during a sync session as follows:
 * <pre>
 *      Transport transport = getTransport();
 *      ...
 *      
 *      while (syncSession)
 *      {
 *          //send data to the server
 *          OutputStream outputStream = transport.getOutputStream();
 *          sendMessageToServer(outputStream);
 *          ...
 *      
 *          //read server response
 *          InputStream inputStream = transport.getIntputStream();
 *          readServerResponse(outputStream);
 *          ...
 *          
 *          //cleanup this message exchange
 *          transport.cleanup();
 *      }
 *      ...
 * </pre>
 *
 * @see SyncManager#SyncManager(Device, Transport, String, String, SyncListener, Logger)
 * 
 * @author Denis Evoy
 */
public interface Transport
{
    /** Defines a content type representing SyncML over XML. */
    public static final String CONTENT_TYPE_XML =   "application/vnd.syncml+xml";
    
    /** Defines a content type representing SyncML over WBXML. */
    public static final String CONTENT_TYPE_WBXML = "application/vnd.syncml+wbxml";

    
    /**
     * Called to retrieve the target URI of the transport. <br/><br/>
     * 
     * For example, in the case of HTTP, this should be the full URL.
     * 
     * @return The target URI. Must not be null or empty.
     */
    public String getTargetURI();

    /**
     * Called to retrieve the maximum message size (in bytes) supported by the transport.
     * 
     * @return The maximum message size (in bytes) or 0 if not set. Must be a zero or positive number. 
     */
    public int getMaxMsgSize();

    
    /**
     * Called to retrieve an input stream suitable for reading messages from the SyncML server. <br/><br/>
     * 
     * If the SyncML server can't be contacted for some reason, implementations should attempt to 
     * reconnect a couple of times before throwing an <code>IOException</code>. This exception will 
     * cause the sync session to be suspended. The session may then be either 
     * {@link SyncManager#stopSync() cancelled} or {@link SyncManager#resumeSync(String) resumed} at a 
     * later time.
     * 
     * @return The input stream which is ready for reading. Must not be null.
     * @throws IOException      if the SyncML server couldn't be contacted.
     * @throws SyncException    if some other error occurred. 
     */
    public InputStream getInputStream() 
        throws SyncException, IOException;

    /**
     * Called to retrieve an output stream suitable for writing messages to the SyncML server. <br/><br/>
     * 
     * If the SyncML server can't be contacted for some reason, implementations should attempt to 
     * reconnect a couple of times before throwing an <code>IOException</code>. This exception will 
     * cause the sync session to be suspended. The session may then be either 
     * {@link SyncManager#stopSync() cancelled} or {@link SyncManager#resumeSync(String) resumed} at a 
     * later time.
     * 
     * @return The output stream which is ready for writing. Must not be null.
     * @throws IOException      if the SyncML server couldn't be contacted.
     * @throws SyncException    if some other error occurred. 
     */
    public OutputStream getOutputStream() 
        throws SyncException, IOException;


    /**
     * Called to retrieve the content type of the incoming message.
     * 
     * @return The name of the content type of the incoming message. Must not be null or empty.
     */
    public String getContentType();
    
    
    /**
     * Called to clean up any transport specific data after a message exchange. <br/><br/>
     * 
     * A message exchange is defined as one call to {@link #getOutputStream()}
     * followed by one call to {@link #getInputStream()}.
     */
    public void cleanup();
}
