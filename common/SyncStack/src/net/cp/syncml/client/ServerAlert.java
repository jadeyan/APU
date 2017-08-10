/**
 * Copyright © 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.syncml.client;


import net.cp.mtk.common.CommonUtils;
import net.cp.mtk.common.security.MD5;


/**
 * A class encapsulating an alert received from the SyncML server. <br/><br/>
 * 
 * These alerts are usually used perform server alerted sync on one or more data stores.
 * 
 * @author Denis Evoy
 */
public class ServerAlert
{
    /** A class encapsulating a data store that should be synced. */
    public static class Store
    {
        String serverUri;            //the server URI identifying the store to sync 
        byte syncType;               //the type of sync (SyncML.ALERT_SYNC_XXX) that should be performed for the store


        /* Creates a new store - package visibility only. */
        Store()
        {
            super();
            
            serverUri = null;
            syncType = 0;
        }

        /** Returns the URI of the store. */
        public String getServerUri()
        {
            return serverUri;
        }
        
        /** Returns type of sync (SyncML.ALERT_SYNC_XXX) that should be performed on the store. */
        public byte getSyncType()
        {
            return syncType;
        }
    }
    
    
    //the encoding used to encode/decode strings
    private static final String ENCODING = "UTF-8";

    //the character used to separate parts of the digest
    private static final byte[] DIGEST_SEPARATOR = { ':' };

    //the offsets in the binary data where various parts of the server alert can be found
    private static final int DIGEST_OFFSET =            6;
    private static final int DIGEST_LENGTH =            16;
    private static final int SESSION_ID_OFFSET =        DIGEST_OFFSET + DIGEST_LENGTH + 5;
    private static final int SESSION_ID_LENGTH =        2;
    private static final int SERVER_ID_LENGTH_OFFSET =  SESSION_ID_OFFSET + SESSION_ID_LENGTH;
    private static final int SERVER_ID_LENGTH_LENGTH =  1;
    private static final int SERVER_ID_OFFSET =         SERVER_ID_LENGTH_OFFSET + SERVER_ID_LENGTH_LENGTH;
    private static final int SYNC_NUM_OFFSET =          SERVER_ID_OFFSET;                   // + ServerIdLength
    private static final int SYNC_NUM_LENGTH =          1;
    private static final int SYNC_TYPE_OFFSET =         SYNC_NUM_OFFSET + SYNC_NUM_LENGTH;  // + ServerIdLength

    //the offsets used to parse data for individual stores - there may be multiple store details in the server alert
    private static final int STORE_SYNC_TYPE_OFFSET =   0;
    private static final int STORE_SYNC_TYPE_LENGTH =   1;
    private static final int STORE_CT_OFFSET =          STORE_SYNC_TYPE_OFFSET + STORE_SYNC_TYPE_LENGTH;
    private static final int STORE_CT_LENGTH =          3;
    private static final int STORE_URI_LENGHT_OFFSET =  STORE_CT_OFFSET + STORE_CT_LENGTH;
    private static final int STORE_URI_LENGHT_LENGTH =  1;
    private static final int STORE_URI_OFFSET =         STORE_URI_LENGHT_OFFSET + STORE_URI_LENGHT_LENGTH;
    
    
    private byte[] digest;                       //the digest found in the message (used to verify the message)
    private String serverId;                     //the server ID found in the message
    private Store[] syncStores;                  //the data stores that should be synced
    private byte[] vendorData;                   //any additional vendor-specific data

    private byte[] messageData;                  //the full binary message data
    
    
    /* Private constructor to prevent external creation. */
    private ServerAlert()
    {
        digest = null;
        serverId = null;
        syncStores = null;
        vendorData = null;
        
        messageData = null;
    }


    /** Returns the ID of the server contained in the alert. */
    public String getServerId()
    {
        return serverId;
    }

    /** Returns data stores contained in the alert. */
    public Store[] getSyncStores()
    {
        return syncStores;
    }
    
    /** Returns any vendor-specific data included in the alert, or null if there was none. */
    public byte[] getVendorData()
    {
        return vendorData;
    }

    
    /** Returns TRUE if the server alert is valid based on the specified details. */
    public boolean isValid(String serverName, String password, String nonce) 
        throws SyncException
    {
        //make sure the parameters are supplied
        if ( (serverName == null) || (serverName.length() <= 0) || (password == null) || (password.length() <= 0) || (nonce == null) || (nonce.length() <= 0) )
            return false;

        //compute the digest using the given details
        byte[] computedDigest = encodeDigest(serverName, password, nonce, messageData, DIGEST_OFFSET + DIGEST_LENGTH, messageData.length - DIGEST_OFFSET - DIGEST_LENGTH);
        if (computedDigest.length != digest.length)
            return false;
        
        //compare the computed digest with the one received in the alert
        for (int i = 0; i < computedDigest.length; i++)
        {
            if (computedDigest[i] != digest[i])
                return false;
        }
        
        return true;
    }

    /** Returns a server alert from the specified binary data. */
    public static ServerAlert parse(byte[] data) 
        throws SyncException
    {
        //create a new alert to encapsulate the binary data
        ServerAlert alert = new ServerAlert();
        alert.messageData = data;

        //read the digest
        alert.digest = getBinaryData(data, DIGEST_OFFSET, DIGEST_LENGTH);

        //read the server ID
        byte serverIdLength = data[SERVER_ID_LENGTH_OFFSET];
        alert.serverId = getStringData(data, SERVER_ID_OFFSET, serverIdLength);

        //read the number of sync stores that have been requested for sync - shift 4 highest bits 
        //right, and reset the 4 highest
        int syncStoreCount = (byte)(data[SYNC_NUM_OFFSET + serverIdLength] & (byte)0xF0);
        syncStoreCount = (byte)((syncStoreCount >> 4) & 0x0F);

        //read the details of each sync store that has been requested for sync
        alert.syncStores = new Store[syncStoreCount];
        int syncStoreOffset = SYNC_TYPE_OFFSET + serverIdLength;
        for (int i = 0; i < alert.syncStores.length; i++)
        {
            alert.syncStores[i] = new Store();
            syncStoreOffset += parseStore(data, syncStoreOffset, alert.syncStores[i]);
        }
        
        //read any remaining vendor-specific data
        int vendorDataLength = data.length - syncStoreOffset;
        if (vendorDataLength > 0)
            alert.vendorData = getBinaryData(data, syncStoreOffset, vendorDataLength);
        
        return alert;
    }
    
    /* Parses the data store details from the specified data and returns the number of bytes parsed. */
    private static int parseStore(byte[] data, int offset, Store store) 
        throws SyncException
    {
        //read the sync type for the store - taking only 4 highest bits
        store.syncType = (byte)((data[offset + STORE_SYNC_TYPE_OFFSET] >>> 4) & 0x0F);
    
        //read the server URI of the store
        int serverUriLength = data[offset + STORE_URI_LENGHT_OFFSET] & 0xFF;
        store.serverUri = getStringData(data, offset + STORE_URI_OFFSET, serverUriLength); 

        //return the number of bytes parsed
        return STORE_URI_LENGHT_OFFSET + STORE_URI_LENGHT_LENGTH + serverUriLength;
    }
    
    /* Returns the binary data at the specified position in the specified data. */
    private static byte[] getBinaryData(byte[] data, int offset, int length)
        throws SyncException
    {
        try
        {
            byte[] dest = new byte[length];
            System.arraycopy(data, offset, dest, 0, length);
            return dest;
        }
        catch (Throwable e)
        {
            throw new SyncException("Could not retrieve binary data of length '" + length + "' from offset '" + offset + "'");
        }
    }
    
    /* Returns the UTF-8 encoded string at the specified position in the specified data. */
    private static String getStringData(byte[] data, int offset, int length)
        throws SyncException
    {
        try
        {
            byte[] dest = new byte[length];
            System.arraycopy(data, offset, dest, 0, length);
            return new String(dest, ENCODING);
        }
        catch (Throwable e)
        {
            throw new SyncException("Could not retrieve string data of length '" + length + "' from offset '" + offset + "'");
        }
    }
    

    /* Encodes the specified details as MD5(Base64(MD5(<ServerId>:<Password>)):<LastNonce>:Base64(MD5(<Notification>))). */
    private static byte[] encodeDigest(String serverId, String password, String nonce, byte[] notification, int notificationOffset, int notificationLength) 
        throws SyncException
    {
        try
        {
            byte[] serverIdData = serverId.getBytes(ENCODING);
            byte[] passwordData = password.getBytes(ENCODING);
            byte[] nonceData = nonce.getBytes(ENCODING);

            //encode the credentials as Base64(MD5(<ServerId>:<Password>))
            MD5 md5Credentials = new MD5(serverIdData);
            md5Credentials.update(DIGEST_SEPARATOR, 0, DIGEST_SEPARATOR.length);
            md5Credentials.update(passwordData, 0, passwordData.length);
            byte[] hashedCredentials = md5Credentials.doFinal();
            byte[] encodedCredentials = CommonUtils.base64Encode(hashedCredentials).getBytes(ENCODING);

            //encode the notification as Base64(MD5(<Notification>))
            MD5 md5Notification = new MD5();
            md5Notification.update(notification, notificationOffset, notificationLength);
            byte[] hashedNotification = md5Notification.doFinal();
            byte[] encodedNotification = CommonUtils.base64Encode(hashedNotification).getBytes(ENCODING);

            //compute the full digest as MD5(<Credentials>:<LastNonce>:<Notification>)
            MD5 md5 = new MD5(encodedCredentials);
            md5.update(DIGEST_SEPARATOR, 0, DIGEST_SEPARATOR.length);
            md5.update(nonceData, 0, nonceData.length);
            md5.update(DIGEST_SEPARATOR, 0, DIGEST_SEPARATOR.length);
            md5.update(encodedNotification, 0, encodedNotification.length);

            return md5.doFinal();
        }
        catch (Throwable e)
        {
            throw new SyncException("Failed to encode digest using server ID '" + serverId + "' and nonce '" + nonce + "'");
        }
    }
}
