/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


/**
 * A class representing authentication credentials used during a SyncML session.
 *
 * @author Denis Evoy
 */
public class Cred
{
    public static final String AUTH_TYPE_BASIC =    "syncml:auth-basic";
    public static final String AUTH_TYPE_MD5 =      "syncml:auth-md5";

    
    public Metinf metinf;                       //the meta information associated with the credentials
    public byte[] data;                         //the data associated with the credentials 

    
    public Cred()
    {
        super();
        
        clear();
    }


    public void clear()
    {
        metinf = null;
        data = null;
    }
}
