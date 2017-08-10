/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.contacts;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.microedition.pim.Contact;
import javax.microedition.pim.PIM;
import javax.microedition.pim.PIMException;
import javax.microedition.pim.PIMItem;
import javax.microedition.pim.PIMList;

import net.cp.mtk.common.StringUtils;
import net.cp.mtk.common.CommonUtils;
import net.cp.mtk.common.io.StreamUtils;


public abstract class ContactUtils
{
    public static final String VCARD_FORMAT_2_1 = "VCARD/2.1";
    public static final String VCARD_FORMAT_3_0 = "VCARD/3.0";
    
    
    private ContactUtils()
    {
        super();
    }
    
    
    /**
     * Closes the specified PIM list, ignoring any exceptions that occur.
     * 
     * @param list the list to close. May be null.
     */
    public static void closeList(PIMList list)
    {
        try
        {
            if (list != null)
                list.close();
        }
        catch (Throwable ex)
        {
            //ignore
        }
    }


    /**
     * Converts the specified vCard formatted string into a Contact.
     * 
     * @param vcard the vCard string (usually VCARD/2.1 or VCARD/3.0 format) to be parsed. Must not be null.
     * @return the Contact represented by the specified vCard.
     * @throws PIMException if the vCard couldn't be parsed.
     * @throws UnsupportedEncodingException if "UTF-8" encoding isn't supported.
     */
    public static Contact vcardToContact(String vcard) 
        throws PIMException, UnsupportedEncodingException
    {
        ByteArrayInputStream inputStream = null; 
        try
        {
            inputStream = new ByteArrayInputStream( vcard.getBytes("UTF-8") );
            
            PIMItem[] contacts = PIM.getInstance().fromSerialFormat(inputStream, "UTF-8");
            if ( (contacts == null) || (contacts.length <= 0) )
                return null;

            return (Contact)contacts[0];
        }
        finally
        {
            StreamUtils.closeStream(inputStream);
        }
    }

    /**
     * Converts the specified contact into a vCard formatted string.
     * 
     * @param contact       the contact to be formatted. Must not be null.
     * @param vcardFormat   the required vCard format (VCARD_FORMAT_XXX).
     * @return the vCard representation of the contact. 
     * @throws PIMException if the vCard couldn't be created.
     * @throws UnsupportedEncodingException if "UTF-8" encoding isn't supported.
     */
    public static String vcardFromContact(Contact contact, String vcardFormat)
        throws PIMException, UnsupportedEncodingException
    {
        ByteArrayOutputStream outputStream = null; 
        try
        {
            outputStream = new ByteArrayOutputStream(256); 
            PIM.getInstance().toSerialFormat(contact, outputStream, "UTF-8", vcardFormat);
            return new String(outputStream.toByteArray(), "UTF-8");
        }
        finally
        {
            StreamUtils.closeStream(outputStream);
        }
    }
    

    /**
     * Returns TRUE if the specified field is a standard contact field.
     * 
     * @param fieldId the field ID to examine.
     * @return TRUE if the specified field is a standard contact field, or FALSE if the field is unrecognised. 
     */
    public static boolean isStandardField(int fieldId)
    {
        return (fieldIdToString(fieldId) != null);
    }
    
    /**
     * Returns the string representation of the specified data type.
     * 
     * @param dataType the data type to examine. 
     * @return the string representation of the data type, or null if the data type is unrecognised.
     */
    public static String datatypeToString(int dataType)
    {
        if (dataType == PIMItem.BINARY)
            return "BINARY";
        else if (dataType == PIMItem.BOOLEAN)
            return "BOOLEAN";
        else if (dataType == PIMItem.DATE)
            return "DATE";
        else if (dataType == PIMItem.INT)
            return "INTEGER";
        else if (dataType == PIMItem.STRING)
            return "STRING";
        else if (dataType == PIMItem.STRING_ARRAY)
            return "STRINGARRAY";
        
        return null;
    }

    /**
     * Returns the string representation of the specified contact field ID.
     * 
     * @param fieldId the field ID to examine.
     * @return the string representation of the field ID, or null if the field ID is unrecognised.
     */
    public static String fieldIdToString(int fieldId)
    {
        if (fieldId == Contact.ADDR)
            return "ADDRESS";
        else if (fieldId == Contact.BIRTHDAY)
            return "BIRTHDAY";
        else if (fieldId == Contact.CLASS)
            return "CLASS";
        else if (fieldId == Contact.EMAIL)
            return "EMAIL";
        else if (fieldId == Contact.FORMATTED_ADDR)
            return "FORMATTED_ADDRESS";
        else if (fieldId == Contact.FORMATTED_NAME)
            return "FORMATTED_NAME";
        else if (fieldId == Contact.NAME)
            return "NAME";
        else if (fieldId == Contact.NICKNAME)
            return "NICKNAME";
        else if (fieldId == Contact.NOTE)
            return "NOTE";
        else if (fieldId == Contact.ORG)
            return "ORG";
        else if (fieldId == Contact.PHOTO)
            return "PHOTO";
        else if (fieldId == Contact.PHOTO_URL)
            return "PHOTO_URL";
        else if (fieldId == Contact.PUBLIC_KEY)
            return "PUBLIC_KEY";
        else if (fieldId == Contact.PUBLIC_KEY_STRING)
            return "PUBLIC_KEY_STRING";
        else if (fieldId == Contact.REVISION)
            return "REVISION";
        else if (fieldId == Contact.TEL)
            return "TELEPHONE";
        else if (fieldId == Contact.TITLE)
            return "TITLE";
        else if (fieldId == Contact.UID)
            return "UID";
        else if (fieldId == Contact.URL)
            return "URL";
        
        return null;
    }
    
    /**
     * Returns the string representation of the specified contact element ID of the specified string-array field.
     * 
     * @param fieldId   the field ID to examine.
     * @param elementId the element ID to examine.
     * @return the string representation of the element ID, or null if the field ID or element ID are unrecognised.
     */
    public static String elementIdToString(int fieldId, int elementId)
    {
        if (fieldId == Contact.ADDR)
        {
            if (elementId == Contact.ADDR_COUNTRY)
                return "COUNTRY";
            else if (elementId == Contact.ADDR_EXTRA)
                return "EXTRA";
            else if (elementId == Contact.ADDR_LOCALITY)
                return "LOCALITY";
            else if (elementId == Contact.ADDR_POBOX)
                return "POBOX";
            else if (elementId == Contact.ADDR_POSTALCODE)
                return "POSTALCODE";
            else if (elementId == Contact.ADDR_REGION)
                return "REGION";
            else if (elementId == Contact.ADDR_STREET)
                return "STREET";
        }
        else if (fieldId == Contact.NAME)
        {
            if (elementId == Contact.NAME_FAMILY)
                return "FAMILY";
            else if (elementId == Contact.NAME_GIVEN)
                return "GIVEN";
            else if (elementId == Contact.NAME_OTHER)
                return "OTHER";
            else if (elementId == Contact.NAME_PREFIX)
                return "PREFIX";
            else if (elementId == Contact.NAME_SUFFIX)
                return "SUFFIX";
        }
        
        return null;
    }
    
    /**
     * Returns the string representation of the specified contact attribute.
     * 
     * @param attribute the attribute to examine.
     * @return the string representation of the attribute, or null if the attribute is unrecognised.
     */
    public static String attributeToString(int attribute)
    {
        if (attribute == Contact.ATTR_ASST)
            return "ASSISTANT";
        else if (attribute == Contact.ATTR_AUTO)
            return "AUTO";
        else if (attribute == Contact.ATTR_FAX)
            return "FAX";
        else if (attribute == Contact.ATTR_HOME)
            return "HOME";
        else if (attribute == Contact.ATTR_MOBILE)
            return "MOBILE";
        else if (attribute == Contact.ATTR_OTHER)
            return "OTHER";
        else if (attribute == Contact.ATTR_PAGER)
            return "PAGER";
        else if (attribute == Contact.ATTR_PREFERRED)
            return "PREFERRED";
        else if (attribute == Contact.ATTR_SMS)
            return "SMS";
        else if (attribute == Contact.ATTR_WORK)
            return "WORK";
        else if (attribute == PIMItem.ATTR_NONE)
            return "NONE";
        
        return null;
    }


    /**
     * Returns whether or not the value at the specified index in the specified field in the specified contacts is the same.
     *  
     * @param contact1      the first contact to compare. Must not be null.
     * @param contact2      the second contact to compare. Must not be null.
     * @param fieldId       the ID of the field to compare. Must be a supported field.
     * @param valueIndex1   the index of the first value to compare. Must be >=0 and < the number of values present for the field.
     * @param valueIndex2   the index of the second value to compare. Must be >=0 and < the number of values present for the field.
     * @return TRUE if both values are the same, FALSE otherwise.
     */
    public static boolean isEquals(Contact contact1, Contact contact2, int fieldId, int valueIndex1, int valueIndex2)
    {
        int fieldDataType = contact1.getPIMList().getFieldDataType(fieldId);
        if (fieldDataType == PIMItem.STRING)
        {
            String value1 = contact1.getString(fieldId, valueIndex1);
            String value2 = contact2.getString(fieldId, valueIndex2);
            if (StringUtils.isEquals(value1, value2))
                return true;
        }
        else if (fieldDataType == PIMItem.STRING_ARRAY)
        {
            String[] value1 = contact1.getStringArray(fieldId, valueIndex1);
            String[] value2 = contact2.getStringArray(fieldId, valueIndex2);
            if (StringUtils.isEquals(value1, value2))
                return true;
        }
        else if (fieldDataType == PIMItem.INT)
        {
            int value1 = contact1.getInt(fieldId, valueIndex1);
            int value2 = contact2.getInt(fieldId, valueIndex2);
            if (value1 == value2)
                return true;
        }
        else if (fieldDataType == PIMItem.DATE)
        {
            long value1 = contact1.getDate(fieldId, valueIndex1);
            long value2 = contact2.getDate(fieldId, valueIndex2);
            if (value1 == value2)
                return true;
        }
        else if (fieldDataType == PIMItem.BOOLEAN)
        {
            boolean value1 = contact1.getBoolean(fieldId, valueIndex1);
            boolean value2 = contact2.getBoolean(fieldId, valueIndex2);
            if (value1 == value2)
                return true;
        }
        else if (fieldDataType == PIMItem.BINARY)
        {
            byte[] value1 = contact1.getBinary(fieldId, valueIndex1);
            byte[] value2 = contact2.getBinary(fieldId, valueIndex2);
            if (CommonUtils.isEquals(value1, value2))
                return true;
        }
        
        return false;
    }
}
