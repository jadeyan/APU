/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import net.cp.syncml.client.util.Logger;

/**
 * This class represents a subset of the functionality offered by the standard MIDP Contact
 *
 * It was created to allow the same sync engine code to work on MIDP and other Java platforms (like Android)
 * with little or no modification.
 *
 *
 * @see javax.microedition.pim.Contact
 *
 */
public abstract class Contact
{
    protected ContactList contactList = null;
    protected Logger logger;

    //attributes
    public static final int ATTR_NONE = 0;
    public static final int ATTR_ASST = 1;
    public static final int ATTR_AUTO = 2;
    public static final int ATTR_FAX = 4;
    public static final int ATTR_HOME = 8;
    public static final int ATTR_MOBILE = 16;
    public static final int ATTR_OTHER = 32;
    public static final int ATTR_PAGER = 64;
    public static final int ATTR_PREFERRED = 128;
    public static final int ATTR_SMS = 256;
    public static final int ATTR_WORK = 512;
    public static final int ATTR_ADDR_OTHER = 1024;

    //class ?
    public static final int CLASS = 102;
    public static final int CLASS_CONFIDENTIAL = 200;
    public static final int CLASS_PRIVATE = 201;
    public static final int CLASS_PUBLIC = 202;

    //field IDs
    public static final int ADDR = 100;
    public static final int ADDR_COUNTRY = 6;
    public static final int ADDR_EXTRA = 1;
    public static final int ADDR_LOCALITY = 3;
    public static final int ADDR_POBOX = 0;
    public static final int ADDR_POSTALCODE = 5;
    public static final int ADDR_REGION = 4;
    public static final int ADDR_STREET = 2;
    public static final int BIRTHDAY = 101;
    public static final int EMAIL = 103;
    public static final int FORMATTED_ADDR = 104;
    public static final int FORMATTED_NAME = 105;
    public static final int NAME = 106;
    public static final int NAME_FAMILY = 0;
    public static final int NAME_GIVEN = 1;
    public static final int NAME_OTHER = 2;
    public static final int NAME_PREFIX = 3;
    public static final int NAME_SUFFIX = 4;
    public static final int NICKNAME = 107;
    public static final int NOTE = 108;
    public static final int ORG = 109;
    public static final int ORG_COMPANY = 0;
    public static final int ORG_DEPT = 1;
    public static final int PHOTO = 110;
    public static final int PHOTO_URL = 111;
    public static final int PUBLIC_KEY = 112;
    public static final int PUBLIC_KEY_STRING = 113;
    public static final int REVISION = 114;
    public static final int TEL = 115;
    public static final int TITLE = 116;
    public static final int UID = 117;
    public static final int URL = 118;
    public static final int ANNIVERSARY = 200;
    
    /** Not a database column but a calculated string */
    public static final int VERSION = 201;
    
    //field data types
    public static final int BINARY = 0;
    public static final int BOOLEAN = 1;
    public static final int DATE = 2;
    public static final int EXTENDED_ATTRIBUTE_MIN_VALUE = 16777216;
    public static final int EXTENDED_FIELD_MIN_VALUE = 16777216;
    public static final int INT = 3;
    public static final int STRING = 4;
    public static final int STRING_ARRAY = 5;

    //field labels
    public static final String LABEL_ADDRESS                = "ADDRESS";
    public static final String LABEL_FORMATTED_ADDRESS      = "FORMATTED_ADDRESS";
    public static final String LABEL_BIRTHDATE              = "BIRTHDATE";
    public static final String LABEL_CLASS                  = "CLASS";
    public static final String LABEL_EMAIL                  = "EMAIL";
    public static final String LABEL_FORMATTED_NAME         = "FORMATTED_NAME";
    public static final String LABEL_GEOGRAPHIC_POSITION    = "GEOGRAPHIC_POSITION";
    public static final String LABEL_PUBLIC_KEY             = "PUBLIC_KEY";
    public static final String LABEL_DELIVERY_LABEL         = "DELIVERY_LABEL";
    public static final String LABEL_LOGO                   = "LOGO";
    public static final String LABEL_MAILER                 = "MAILER";
    public static final String LABEL_NAME                   = "NAME";
    public static final String LABEL_ORG                    = "ORG";
    public static final String LABEL_NICKNAME               = "NICKNAME";
    public static final String LABEL_NOTE                   = "NOTE";
    public static final String LABEL_PHOTO                  = "PHOTO";
    public static final String LABEL_REVISION               = "REVISION";
    public static final String LABEL_ROLE                   = "ROLE";
    public static final String LABEL_SOUND                  = "SOUND";
    public static final String LABEL_TELEPHONE              = "TELEPHONE";
    public static final String LABEL_TITLE                  = "TITLE";
    public static final String LABEL_TIMEZONE               = "TIMEZONE";
    public static final String LABEL_UID                    = "UID";
    public static final String LABEL_URL                    = "URL";
    public static final String LABEL_VERSION                = "VERSION";
    public static final String LABEL_ANNIVERSARY            = "ANNIVERSARY";

    public Contact(ContactList list, Logger logger)
    {
        contactList = list;
        this.logger = logger;
    }

    /**
     * @param field the fieldId
     * @return the number of values in the specified field for this contact
     */
    public abstract int countValues(int field);

    /**
     * @return The ContactList that owns this contact
     *
     */
    public abstract ContactList getContactList();

    /**
     * @return the IDs of the fields in this contact.
     */
    public abstract int[] getFields();

    /**
     * Gets the attributes associated with a particular field value.
     * E.g. ContactList.ATTR_HOME, ContactList.ATTR_WORK ...
     *
     * @param fieldId
     * @param valueIndex
     * @return a bitmask of attributes associated with this field value
     */
    public abstract int getAttributes(int fieldId, int valueIndex);

    /**
     * Gets the string value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the string value
     */
    public abstract String getString(int fieldId, int valueIndex);

    /**
     * Gets the string array value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the string array value
     */
    public abstract String[] getStringArray(int fieldId, int valueIndex);

    /**
     * Adds a string with attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ContactList.ATTR_WORK
     * @param fieldValue the value to set for this field
     */
    public abstract void addString(int fieldId, int fieldAttributes, String fieldValue);

    /**
     * Adds a string array with attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ContactList.ATTR_WORK
     * @param valueArray the value to set for this field
     */
    public abstract void addStringArray(int fieldId, int fieldAttributes, String[] valueArray);

    /**
     * Gets the integer value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the int value
     */
    public abstract int getInt(int fieldId, int valueIndex);

    /**
     * Adds an integer value and attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ContactList.ATTR_WORK
     * @param fieldValue the value to set for this field
     */
    public abstract void addInt(int fieldId, int fieldAttributes, int value);

    /**
     * Gets the date value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the date value
     */
    public abstract long getDate(int fieldId, int valueIndex);

    /**
     * Adds a date value and attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ContactList.ATTR_WORK
     * @param fieldValue the value to set for this field
     */
    public abstract void addDate(int fieldId, int fieldAttributes, long value);

    /**
     * Removes a value from the specified field.
     *
     * @param fieldId the ID if the field to modify
     *
     * @param valueIndex the index from which to remove the value.
     */
    public abstract void removeValue(int fieldId, int valueIndex);

    /**
     * Gets the byte array value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the byte array values
     */
    public abstract byte[] getBinary(int fieldId, int valueIndex);

    /**
     * Adds a byte array with attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ContactList.ATTR_WORK
     * @param fieldValue the value to set for this field
     * @param offset the offset into the fieldValue to start reading from
     * @param length the amount of data to read from the fieldValue
     */
    public abstract void addBinary(int fieldId, int fieldAttributes, byte[] fieldValue, int offset, int length);
}

