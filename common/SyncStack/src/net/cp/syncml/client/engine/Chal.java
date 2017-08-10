/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


/**
 * A class representing an authentication challenge used during a SyncML session.
 *
 * @author Denis Evoy
 */
public class Chal
{
    public Metinf metinf;                       //the meta information associated with the challenge

    
    public Chal()
    {
        super();
        
        clear();
    }


    public void clear()
    {
        metinf = null;
    }
}
