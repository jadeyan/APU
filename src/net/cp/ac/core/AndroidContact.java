/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import net.cp.engine.PersistentStore;
import net.cp.engine.contacts.Contact;
import net.cp.engine.contacts.ContactList;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;
import android.content.ContentValues;
import android.os.Build;

/**
 *
 * This class is the Base Android implementation of the Contact class, similar to the J2ME Contact class.
 * It stores the different contact field types, and helps integrate with the Android contact database.
 * @see AndroidContactList
 *
 *
 *
 */

public abstract class AndroidContact extends Contact {
    // key: Integer fieldID, value: StringField
    private final Hashtable<Integer, Field> fields;

    /*
     * The vcard hash of the contact when it was first created+populated. This is used to detect conflicts where the contact was changed by the user while
     * syncing
     */
    private byte[] hash;

    /**
     *
     * @param list The ContactList this contact will belong to.
     * @param logger The logger to use.
     */
    public AndroidContact(ContactList list, Logger logger) {
        super(list, logger);
        fields = new Hashtable<Integer, Field>();

        contactList = list;

        if (logger != null) logger.debug("Created new Contact:" + getClass().getName());
    }

    private static Map<String, Constructor<AndroidContact>> clazzCache = new HashMap<String, Constructor<AndroidContact>>();

    public static AndroidContact createInstance(ContactList list, Logger logger) {
        AndroidContact sInstance = null;
        String className;

        /*
         * Check the version of the SDK we are running on. Choose an implementation class designed for that version of the SDK. Unfortunately we have to use
         * strings to represent the class names. If we used the conventional ContactAccessorSdk5.class.getName() syntax, we would get a ClassNotFoundException
         * at runtime on pre-Eclair SDKs. Using the above syntax would force Dalvik to load the class and try to resolve references to all other classes it
         * uses. Since the pre-Eclair does not have those classes, the loading of ContactAccessorSdk5 would fail.
         */

        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ECLAIR) {
            className = "net.cp.ac.core.AndroidContactAPI3";
        } else {
            className = "net.cp.ac.core.AndroidContactAPI5";
        }

        if (clazzCache.isEmpty()) {

            try {
                clazzCache.put("net.cp.ac.core.AndroidContactAPI3", getConstructor("net.cp.ac.core.AndroidContactAPI3"));
                clazzCache.put("net.cp.ac.core.AndroidContactAPI5", getConstructor("net.cp.ac.core.AndroidContactAPI5"));
            } catch (Exception e) {
                if (logger != null) logger.error("Error getting required class", e);

                throw new IllegalStateException(e);
            }

        }

        Constructor<AndroidContact> constructor = clazzCache.get(className);
        if (constructor == null) throw new IllegalStateException("Error trying to instantiate contact class " + className);

        try {
            sInstance = constructor.newInstance(list, logger);
        } catch (Exception e) {
            if (logger != null) logger.error("Error getting required class", e);

            throw new IllegalStateException(e);
        }

        return sInstance;
    }

    @SuppressWarnings("unchecked")
    private static Constructor<AndroidContact> getConstructor(String className) throws Exception {
        Class<? extends AndroidContact> realClass = Class.forName(className).asSubclass(AndroidContact.class);
        return (Constructor<AndroidContact>) realClass.getConstructor(new Class[] { net.cp.engine.contacts.ContactList.class,
                net.cp.syncml.client.util.Logger.class });
    }

    /**
     * This method sets the MD5 hash of the vcard representation of this contact.
     * This information is used to detect changes.
     *
     * @param hash The MD5 hash
     */
    protected void setHash(byte[] hash) {
        this.hash = hash;
    }

    /**
     * Gets the MD5 hash of the vcard representation of this contact.
     * This information is used to detect changes.
     *
     * @return The MD5 hash
     */
    protected byte[] getHash() {
        return hash;
    }

    /**
     * Adds a new field to the contact.
     * Not to be confused with adding a <b>value</b> to an existing field.
     *
     * @param field The new field to be added to the contact.
     */
    protected void addField(Field field) {
        fields.put(new Integer(field.id()), field);
    }

    /**
     * Returns the requested field, it it exists.
     *
     * @param field the fieldId
     * @return the Field object that corresponds to the supplied fieldId, or null if it doesn't exist
     */
    protected Field getField(int field) {
        return fields.get(new Integer(field));
    }

    /**
     * Returns the requested field, it it exists.
     *
     * @param field the fieldId
     * @return the StringField object that corresponds to the supplied fieldId, or null if it doesn't exist
     */
    protected StringField getStringField(int field) {
        return (StringField) fields.get(new Integer(field));
    }

    /**
     * Returns the requested field, it it exists.
     *
     * @param field the fieldId
     * @return the StringArrayField object that corresponds to the supplied fieldId, or null if it doesn't exist
     */
    protected StringArrayField getArrayField(int field) {
        return (StringArrayField) fields.get(new Integer(field));
    }

    /**
     * @param field the fieldId
     * @return the number of values in the specified field for this contact, or -1 if the field does not exist
     */
    @Override
    public int countValues(int field) {
        Field myField = getField(field);

        if (myField != null) return myField.getValueCount();

        return -1;
    }

    /**
     * @return The ContactList that owns this contact
     *
     */
    @Override
    public ContactList getContactList() {
        return contactList;
    }

    /**
     * @return the IDs of the fields in this contact.
     */
    @Override
    public int[] getFields() {
        int[] result = new int[fields.size()];
        int i = 0;
        for (Enumeration<Integer> e = fields.keys(); e.hasMoreElements();) {
            result[i] = e.nextElement();
            i++;
        }

        return result;
    }

    /**
     * Gets the attributes associated with a particular field value.
     * E.g. ATTR_HOME, ATTR_WORK ...
     *
     * @param fieldId
     * @param valueIndex
     * @return a bitmask of attributes associated with this field value
     */
    @Override
    public int getAttributes(int fieldId, int valueIndex) {
        Field myField = fields.get(new Integer(fieldId));

        return myField.getAttributes(valueIndex);
    }

    /**
     * Gets the string value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the string value
     */
    @Override
    public String getString(int fieldId, int valueIndex) {
        StringField myField = (StringField) fields.get(new Integer(fieldId));

        if (myField != null && myField.getValueCount() > valueIndex) return myField.getValue(valueIndex);

        return "";
    }

    /**
     * Gets the string array value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the string array value
     */
    @Override
    public String[] getStringArray(int fieldId, int valueIndex) {
        StringArrayField myField = (StringArrayField) fields.get(new Integer(fieldId));

        if (myField != null && myField.getValueCount() > valueIndex) return myField.getValue(valueIndex);

        return null;
    }

    /**
     * Gets the byte array value at the specified index of the specified field
     *
     * @param fieldId the field to read
     * @param valueIndex the index into the field to read
     * @return the byte array values
     */
    @Override
    public byte[] getBinary(int fieldId, int valueIndex) {
        BinaryField myField = (BinaryField) fields.get(new Integer(fieldId));

        if (myField != null && myField.getValueCount() > valueIndex) {
            try {
                return myField.getValue(valueIndex);
            }

            catch (Throwable e) {
                if (logger != null) logger.error("Error getting binary field value", e);
            }
        }
        return null;
    }

    /**
     * Adds a string with attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ATTR_WORK
     * @param fieldValue the value to set for this field
     */
    @Override
    public void addString(int fieldId, int fieldAttributes, String fieldValue) {
        StringField myField = (StringField) fields.get(new Integer(fieldId));

        if (myField == null) {
            myField = new StringField(fieldId, fieldValue, fieldAttributes);
            fields.put(new Integer(fieldId), myField);
        } else
            myField.addValue(fieldValue, fieldAttributes);
    }

    /**
     * Adds a string array with attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ATTR_WORK
     * @param valueArray the value to set for this field
     */
    @Override
    public void addStringArray(int fieldId, int fieldAttributes, String[] valueArray) {
        StringArrayField myField = (StringArrayField) fields.get(new Integer(fieldId));

        if (myField == null) {
            myField = new StringArrayField(fieldId, valueArray, fieldAttributes);
            fields.put(new Integer(fieldId), myField);
        } else
            myField.addValue(valueArray, fieldAttributes);
    }

    /**
     * Adds a byte array with attributes to the specified field
     *
     * @param fieldId the field to update
     * @param fieldAttributes a bitmask of attributes associated with this field value.
     * E.g. ATTR_WORK
     * @param fieldValue the value to set for this field
     * @param offset the offset into the fieldValue to start reading from
     * @param length the amount of data to read from the fieldValue
     */
    @Override
    public void addBinary(int fieldId, int fieldAttributes, byte[] fieldValue, int offset, int length) {
        BinaryField myField = (BinaryField) fields.get(new Integer(fieldId));

        // TODO: obey offset and length params!!!

        try {
            if (myField == null) {
                myField = new BinaryField(fieldId, fieldValue, fieldAttributes);
                fields.put(new Integer(fieldId), myField);
            }

            else
                myField.addValue(fieldValue, fieldAttributes);
        }

        catch (StoreException e) {
            if (logger != null) logger.error("Error getting binary field value", e);
        }
    }

    /**
     * Not supported. We do not support this data type on Android
     */
    @Override
    public int getInt(int field, int index) {
        return -1;
    }

    /**
     * Not supported. We do not support this data type on Android
     */
    @Override
    public void addInt(int field, int attributes, int value) {}

    /**
     * Not supported. We do not support this data type on Android
     */
    @Override
    public long getDate(int field, int index) {
        return 0;
    }

    /**
     * Not supported. We do not support this data type on Android
     */
    @Override
    public void addDate(int field, int attributes, long value) {}

    /**
     * Removes the value from the specified field at the specified index.
     *
     * @param field the id of the field to modify
     * @param index the index of the field to remove the value from.
     */
    @Override
    public void removeValue(int field, int index) {
        Field myField = fields.get(new Integer(field));

        if (myField != null && myField.getValueCount() > index) myField.removeValueByIndex(index);
    }

    /**
     * Get photo info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or a null valued set if there is no photo
     */
    public abstract ContentValues getPhotoValues();

    /**
     * Get phone number info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public abstract ContentValues[] getPhoneValues();

    protected abstract int getPhoneTypeFromAttributes(int attributes);

    /**
     * Convert from Android phone types to "MIDP style" attributes
     *
     * @param phoneType e.g. Contacts.Phones.TYPE_FAX_HOME
     * @return attributes e.g. ATTR_FAX | ATTR_HOME
     */
    protected abstract int getAttributesFromPhoneType(int phoneType);

    /**
     * Convert from Android organization types to "MIDP style" attributes
     *
     * @param orgType e.g. Contacts.Organizations.TYPE_WORK
     * @return attributes e.g. ATTR_NONE
     */
    protected abstract int getAttributesFromOrgType(int orgType);

    /**
     * Convert from "MIDP style" attributes to Android organization types
     *
     * @param attributes e.g. ATTR_WORK
     * @return org type  e.g. Contacts.ContactMethods.TYPE_WORK
     */
    protected abstract int getOrgTypeFromAttributes(int attributes);

    /**
     *
     * Convert from Android contact address types to "MIDP style" attributes
     *
     * @param contactMethodType e.g. ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
     * @return attributes e.g. ATTR_HOME
     */
    protected abstract int getAttributesFromAddressType(int addressMethodType);

    /**
     *
     * Convert from Android contact email types to "MIDP style" attributes
     *
     * @param contactMethodType e.g. ContactsContract.CommonDataKinds.Email.TYPE_HOME
     * @return attributes e.g. ATTR_HOME
     */
    protected abstract int getAttributesFromEmailType(int contactMethodType);

    /**
     * Returns the unique ID of this contact.
     *
     * @return UID as String, or "" if no UID is set
     */
    public String getUID() {
        return getString(UID, 0);
    }

    /**
     * Returns the unique ID of this contact.
     *
     * @return UID as String, or "" if no UID is set
     */
    public void setUID(String uid) {
        StringField contactId = new StringField(UID, uid, ATTR_NONE);
        addField(contactId);
    }

    /**
     * Returns the values of a certain field
     *
     * @param fieldId the id of the field in question
     * @return a Vector of the currently set values for this field
     */
    protected Vector<String> currentFieldValues(int fieldId) {
        int numValues = this.countValues(fieldId);
        Vector<String> values = new Vector<String>();

        for (int i = 0; i < numValues; i++)
            values.add(getString(fieldId, i));

        return values;
    }

    /**
     * This class implements the storage of string values in a field.
     * @see Field
     *
     *
     *
     */
    public static class StringField implements Field {
        /**
         * The ID of this field
         */
        public int id;

        private final Vector<Object[]> values;

        /**
         *
         * @param id the ID for this field
         */
        public StringField(int id) {
            this.id = id;
            values = new Vector<Object[]>(0);
        }

        /**
         *
         * @param id the ID for this field
         * @param initialValue an initial value to add to the field, or null for no initial value
         * @param valueAttributes the attributes associate with the initialValue, or 0 if initialValue is null
         */
        public StringField(int id, String initialValue, int valueAttributes) {
            this(id);

            if (initialValue != null) addValue(initialValue, valueAttributes);
        }

        /**
         * Adds a value in this field
         * @param value the value to add/update
         * @param attributes the attributes associated with this value
         */
        public void addValue(String value, int attributes) {
            Object[] valueAttributes = new Object[] { value, new Integer(attributes) };

            values.add(valueAttributes);
        }

        /**
         * Removes the value at the specified index from this field.
         * @param valueIndex the index to remove the value from.
         */
        @Override
        public void removeValueByIndex(int valueIndex) {
            values.removeElementAt(valueIndex);
        }

        /*
         * (non-Javadoc)
         * @see net.cp.ac.core.Field#getValueCount()
         */
        @Override
        public int getValueCount() {
            return values.size();
        }

        /*
         * (non-Javadoc)
         * @see net.cp.ac.core.Field#getAttributes(int)
         */
        @Override
        public int getAttributes(int valueIndex) {
            if (valueIndex < values.size()) {
                Object[] tuple = values.get(valueIndex);
                return ((Integer) tuple[1]).intValue();
            }

            return ATTR_NONE;
        }

        /**
         *
         * @param valueIndex the index of the value requested
         * @return the value at the specified index.
         */
        public String getValue(int valueIndex) {
            Object[] tuple = values.get(valueIndex);
            return (String) tuple[0];
        }

        /**
         * @return the ID of the field
         */
        @Override
        public int id() {
            return id;
        }
    }

    /**
     * This class implements the storage of string array values in a field.
     * @see Field
     *
     *
     *
     */
    public static class StringArrayField implements Field {
        /**
         * The ID of this field
         */
        public int id;

        private final Vector<Object[]> values;

        /**
         *
         * @param id the ID for this field
         */
        public StringArrayField(int id) {
            this.id = id;
            values = new Vector<Object[]>(0);
        }

        /**
         *
         * @param id the ID for this field
         * @param initialValue an initial value to add to the field, or null for no initial value
         * @param valueAttributes the attributes associate with the initialValue, or 0 if initialValue is null
         */
        public StringArrayField(int id, String[] initialValues, int valueAttributes) {
            this(id);

            if (initialValues != null) addValue(initialValues, valueAttributes);
        }

        /**
         * Adds a value in this field
         * @param value the value to add/update
         * @param attributes the attributes associated with this value
         */
        public void addValue(String[] fieldValues, int attributes) {
            Object[] valueAttributes = new Object[] { fieldValues, new Integer(attributes) };

            values.add(valueAttributes);
        }

        /*
         * (non-Javadoc)
         * @see net.cp.ac.core.Field#removeValueByIndex(int)
         */
        @Override
        public void removeValueByIndex(int valueIndex) {
            values.removeElementAt(valueIndex);
        }

        /**
         * @return the number of values in this field
         */
        @Override
        public int getValueCount() {
            return values.size();
        }

        /**
         * Gets the attributes associated with a field value.
         *
         * @param valueIndex indicates the value whose attributes should be returned.
         * @return a bitmask of the attributes for the value. E.g. ATTR_WORK | ATTR_FAX
         */
        @Override
        public int getAttributes(int valueIndex) {
            if (valueIndex < values.size()) {
                Object[] tuple = values.get(valueIndex);
                return ((Integer) tuple[1]).intValue();
            }
            return ATTR_NONE;
        }

        /**
         *
         * @param valueIndex the index of the value requested
         * @return the value at the specified index.
         */
        public String[] getValue(int valueIndex) {
            Object[] tuple = values.get(valueIndex);
            return (String[]) tuple[0];
        }

        /*
         * (non-Javadoc)
         * @see net.cp.ac.core.Field#id()
         */
        @Override
        public int id() {
            return id;
        }
    }

    /**
     * This class implements the storage of binary values in a field.
     * @see Field
     *
     *
     *
     */
    public static class BinaryField implements Field {
        /**
         * The ID of this field
         */
        public int id;

        private final Vector<Object[]> values;

        private PersistentStore store;

        /**
         *
         * @param id the ID for this field
         */
        public BinaryField(int id) {
            this.id = id;

            values = new Vector<Object[]>(0);

        }

        /**
         *
         * @param id the ID for this field
         * @param initialValue an initial value to add to the field, or null for no initial value
         * @param valueAttributes the attributes associate with the initialValue, or 0 if initialValue is null
         * @throws StoreException
         */
        public BinaryField(int id, byte[] initialValue, int valueAttributes) throws StoreException {
            this(id);

            store = AndroidPersistentStoreManager.getTemporaryStore();
            store.close();

            if (initialValue != null) addValue(initialValue, valueAttributes);
        }

        /**
         * Adds a value in this field
         * @param value the value to add/update
         * @param attributes the attributes associated with this value
         */
        public void addValue(byte[] fieldValue, int attributes) throws StoreException {
            store.open(false);

            int recordID = store.writeRecord(0, fieldValue);
            store.close();

            Object[] valueAttributes = new Object[] { new Integer(recordID), new Integer(attributes) };

            values.add(valueAttributes);
        }

        /**
         * Removes the value at the specified index from this field.
         * @param valueIndex the index to remove the value from.
         */
        @Override
        public void removeValueByIndex(int valueIndex) {
            values.removeElementAt(valueIndex);
        }

        /**
         * @return the number of values in this field
         */
        @Override
        public int getValueCount() {
            return values.size();
        }

        /**
         * Gets the attributes associated with a field value.
         *
         * @param valueIndex indicates the value whose attributes should be returned.
         * @return a bitmask of the attributes for the value. E.g. ATTR_WORK | ATTR_FAX
         */
        @Override
        public int getAttributes(int valueIndex) {
            if (valueIndex < values.size()) {
                Object[] tuple = values.get(valueIndex);
                return ((Integer) tuple[1]).intValue();
            }
            return Contact.ATTR_NONE;
        }

        /**
         *
         * @param valueIndex the index of the value requested
         * @return the value at the specified index.
         * @throws StoreException if the value could not be read from storage
         */
        public byte[] getValue(int valueIndex) throws StoreException {
            Object[] tuple = values.get(valueIndex);
            int recordID = ((Integer) tuple[0]).intValue();

            store.open(false);
            byte[] value = store.readRecord(recordID);
            store.close();

            return value;
        }

        /**
         * @return the ID of the field
         */
        @Override
        public int id() {
            return id;
        }
    }
}
