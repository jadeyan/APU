/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.util;


import java.util.Stack;


/**
 * A class representing a stack that allows the bottom of the stack to be consumed. <br/><br/>
 * 
 * Consumed items are removed from the stack and returned.
 *
 * @author  Denis Evoy
 */
public class ConsumableStack extends Stack
{
    /**
     * Removes and returns the object from the bottom of the stack.
     * 
     * @return The object from the bottom of the stack. May be null (if the stack is empty).
     */
    public Object consume()
    {
        if (size() == 0)
            return null;

        Object o = elementAt(0);
        removeElementAt(0);
        return o;
    }
}
