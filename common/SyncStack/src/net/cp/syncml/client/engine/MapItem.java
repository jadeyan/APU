/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


/**
 * A class representing the mapping of a source and target item.
 *
 * @author Denis Evoy
 */
public class MapItem
{
    public String targetUri;                    //the target URI of the item
    public String sourceUri;                    //the target URI of the item

    
    public MapItem()
    {
        super();
        
        clear();
    }


    public void clear()
    {
        targetUri = null;
        sourceUri = null;
    }
}
