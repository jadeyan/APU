/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;


/**
 * 
 * 
 * This Interface defines a field that can be part of an AndroidContact.
 * The implementation should also provide a way to read a value from the field.
 * However the method signature will be different depending on the type of the field.
 * 
 * @see AndroidContact
 * @see AndroidContactList
 */
public interface Field
{
    /**
     * @return the number of values in this field
     */
    public int getValueCount();
    
    /**
     * Gets the attributes associated with a field value.
     * 
     * @param valueIndex indicates the value whose attributes should be returned.
     * @return a bitmask of the attributes for the value. E.g. ContactList.ATTR_WORK | ContactList.ATTR_FAX 
     */
    public int getAttributes(int valueIndex);
    
    /**
     * Removes the value at the specified index from this field.
     * @param valueIndex the index to remove the value from.
     */
    public void removeValueByIndex(int valueIndex);
    
    /**
     * @return the ID of the field
     */
    public int id();
}