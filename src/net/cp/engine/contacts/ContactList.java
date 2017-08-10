/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import java.util.HashMap;
import java.util.List;

import net.cp.syncml.client.util.Logger;

/**
 * This class represents a subset of the functionality offered by the standard MIDP ContactList
 *
 * It was created to allow the same sync engine code to work on MIDP and other Java platforms (like Android)
 * with little or no modification.
 *
 *
 * @see javax.microedition.pim.ContactList
 */
public abstract class ContactList {

    /**
     * The ContactStore associated with this ContactList.
     */
    protected ContactStore store = null;

    /**
     * The logger to use.
     */
    protected Logger logger = null;

    /**
     * The implementing class should populate this array with the list of supported fields.
     */
    protected static int[] supportedFieldList = null;

    /**
     * The implementing class should populate this array with the list of supported attributes.
     */
    public static int[] supportedAttributesList = null;

    /**
     * The name of this contact list.
     */
    public static String CONTACTLIST_NAME = "PEOPLE";

    /**
     * Flag Android API 3 or Android API 5.
     */
    public String FLAG;

    public static final String FLAG_API_3 = "ANDROID_API_3";

    public static final String FLAG_API_5 = "ANDROID_API_5";

    /**
     * Standard constructor.
     *
     * @param store The ContactStore associated with this contact list.
     * @param logger The logger to use.
     */
    public ContactList(ContactStore store, Logger logger) {
        this.store = store;
        this.logger = logger;
    }

    public abstract Contact readContact(String UID);

    public abstract Contact readMinContact(String UID);

    /**
     * Creates a new contact object.
     * Note the contact is not saved on the device until commit() is called.
     * @return The newly created contact.
     */
    public abstract Contact createContact();

    /**
     *
     * @return an array of field IDs of supported fields
     */
    public abstract int[] getSupportedFields();

    /**
     * Returns the attributes (like Contact.ATTR_WORK)
     * supported by a given field.
     *
     * @param fieldId The field in question.
     * @return The attributes for the field in question.
     */
    public abstract int[] getSupportedAttributes(int fieldId);

    /**
     * Does this field support this attribute.
     *
     * @param fieldId The field in question
     * @param typeAttr The attribute in question
     * @return true if the attribute is supported for the given field, otherwise false.
     */
    public abstract boolean isSupportedAttribute(int fieldId, int typeAttr);

    /**
     * @param field the field ID
     * @return true if the field is supported
     */
    public abstract boolean isSupportedField(int field);

    /**
     * @param field the ID of the field in question
     * @return The DataType of the field. E.g Contact.BINARY
     */
    public abstract int getFieldDataType(int field);

    /**
     * @param stringArrayField the field ID
     * @return the size of this String Array field
     */
    public abstract int stringArraySize(int stringArrayField);

    /**
     * @return The name of this contact list
     */
    public abstract String getName();

    /**
     * Closes this ContactList
     */
    public abstract void close();

    /**
     * Removes the specified contact from the device.
     *
     * @param contact The contact to remove.
     * @throws Exception
     */
    public abstract void removeContact(Contact contact) throws Exception;

    /**
     * Commits the specified contact to the device.
     *
     * @param contact The contact to commit.
     * @throws Exception If there was a problem commiting the contact.
     */
    public abstract void commit(Contact contact) throws Exception;

    /**
     * Returns the label associated with a field.
     *
     * @param field The field in question.
     *
     * @return The label describing the field in question.
     */
    public abstract String getFieldLabel(int field);

    /**
     * @return The number of contacts in this contact list.
     */
    public abstract int size();

    /**
     * Returns the maximum number of values that can be stored in a given field.
     *
     * @param field The field in question.
     * @return The maximum number of values that can be stored in the field specified.
     */
    public abstract int maxValues(int field);

    /**
     * Returns an array of the supported elements of a string array for the given field.
     * @param field The field in question
     *
     * @return An array of the supported elements of a string array for the given field.
     */
    public abstract int[] getSupportedArrayElements(int field);

    /**
     *
     * @param name A String[] containing name parts in the order defined by J2ME Contact object.
     * The array must contain 5 elements, with empty String elements as placeholders for missing name parts.
     * @return A String representing the formatted name.
     */
    public static String getFormattedNameFromArray(String[] name) {
        if (name == null) return "";

        // e.g. "Mr Thomas D. Anderson Esquire"

        String formattedName = "";

        if (name[Contact.NAME_PREFIX] != null && !name[Contact.NAME_PREFIX].equals("")) formattedName = name[Contact.NAME_PREFIX];

        if (name[Contact.NAME_GIVEN] != null && !name[Contact.NAME_GIVEN].equals("")) formattedName = formattedName + " " + name[Contact.NAME_GIVEN];

        if (name[Contact.NAME_OTHER] != null && !name[Contact.NAME_OTHER].equals("")) formattedName = formattedName + " " + name[Contact.NAME_OTHER];

        if (name[Contact.NAME_FAMILY] != null && !name[Contact.NAME_FAMILY].equals("")) formattedName = formattedName + " " + name[Contact.NAME_FAMILY];

        if (name[Contact.NAME_SUFFIX] != null && !name[Contact.NAME_SUFFIX].equals("")) formattedName = formattedName + " " + name[Contact.NAME_SUFFIX];

        return formattedName.trim();
    }

    public abstract HashMap<String, String> readAllContactIdAndVersion();

    public abstract List<Contact> readListOfContacts(List<String> ids);
}
