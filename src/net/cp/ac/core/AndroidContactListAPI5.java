/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cp.engine.EngineSettings;
import net.cp.engine.UIInterface;
import net.cp.engine.contacts.Contact;
import net.cp.engine.contacts.ContactList;
import net.cp.engine.contacts.ContactStore;
import net.cp.syncml.client.util.Logger;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.SyncAdapterType;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

/**
 *
 * This class is the Android (API 5) implementation of the ContactList class, similar to the MIDP ContactList class.
 * It is where the Android Contacts DB and our "MIDP like" model of representing contacts meet.
 * This class is responsible for reading the contact info from the DB, and creating a list of contacts that
 * can be read and manipulated by the program.
 *
 * @see AndroidContact
 * @see ContactList
 *
 *
 *
 */
public class AndroidContactListAPI5 extends AndroidContactList {
    // cache all the raw contact ids so we do not need to query the raw ids each time for a contact id
    // also the raw ids do not contains read only raw contacts
    private Map<String, ArrayList<String>> mRawContactIdCache = new HashMap<String, ArrayList<String>>();
    
    /**
     * @param store The ContactStore that is associated with this ContactList
     * @param resolver The ContentResolver to use to query the contact DB
     * @param ui The UIInterface to use to give feedback on contact enumeration
     * @param logger The logger to use
     */
    public AndroidContactListAPI5(ContactStore store, ContentResolver resolver, UIInterface ui, Logger logger) {
        super(store, resolver, ui, logger);

        supportedFieldList = new int[] { Contact.ADDR, Contact.ANNIVERSARY, Contact.BIRTHDAY, Contact.EMAIL, Contact.FORMATTED_NAME, Contact.NAME,
                Contact.NICKNAME, Contact.NOTE, Contact.ORG, Contact.PHOTO, Contact.TEL, Contact.TITLE, Contact.UID, Contact.URL };

        supportedAttributesList = new int[] { Contact.ATTR_NONE, Contact.ATTR_FAX, Contact.ATTR_HOME, Contact.ATTR_MOBILE, Contact.ATTR_OTHER,
                Contact.ATTR_PAGER, Contact.ATTR_PREFERRED, Contact.ATTR_SMS, Contact.ATTR_WORK, Contact.ATTR_ADDR_OTHER };

        if (logger != null) logger.info("AndroidContactListAPI5()");
    }

    /**
     *
     * @param formattedName The formatted name of the contact,
     * @return A String[] containing name parts in the order defined by MIDP Contact object.
     * The array may contain empty elements as placeholders for missing name parts
     */
    private String[] getNameStringArray(Cursor personCursor) {
        String[] name = new String[5];

        name[Contact.NAME_PREFIX] = personCursor.getString(personCursor.getColumnIndex(StructuredName.PREFIX));
        name[Contact.NAME_GIVEN] = personCursor.getString(personCursor.getColumnIndex(StructuredName.GIVEN_NAME));
        name[Contact.NAME_OTHER] = personCursor.getString(personCursor.getColumnIndex(StructuredName.MIDDLE_NAME));
        name[Contact.NAME_FAMILY] = personCursor.getString(personCursor.getColumnIndex(StructuredName.FAMILY_NAME));
        name[Contact.NAME_SUFFIX] = personCursor.getString(personCursor.getColumnIndex(StructuredName.SUFFIX));

        if (logger != null) {
            logger.debug("AndroidContactListAPI5 read Person Details : ");
            logger.debug("PREFIX : " + name[Contact.NAME_PREFIX]);
            logger.debug("GIVEN  : " + name[Contact.NAME_GIVEN]);
            logger.debug("MIDDLE : " + name[Contact.NAME_OTHER]);
            logger.debug("FAMILY : " + name[Contact.NAME_FAMILY]);
            logger.debug("SUFFIX : " + name[Contact.NAME_SUFFIX]);
        }

        return name;
    }

    private String[] getAddressStringArray(Cursor addressCursor) {
        String[] address = new String[7];

        address[Contact.ADDR_POBOX] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.POBOX));
        address[Contact.ADDR_EXTRA] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.NEIGHBORHOOD));
        address[Contact.ADDR_STREET] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.STREET));
        address[Contact.ADDR_LOCALITY] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.CITY));
        address[Contact.ADDR_REGION] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.REGION));
        address[Contact.ADDR_POSTALCODE] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.POSTCODE));
        address[Contact.ADDR_COUNTRY] = addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.COUNTRY));

        if (logger != null) {
            logger.debug("AndroidContactListAPI5 read Contact Address : ");
            logger.debug("ADDRESS TYPE : " + addressCursor.getString(addressCursor.getColumnIndex(StructuredPostal.TYPE)));
            logger.debug("PO BOX       : " + address[Contact.ADDR_POBOX]);
            logger.debug("NEIGHBORHOOD : " + address[Contact.ADDR_EXTRA]);
            logger.debug("STREET       : " + address[Contact.ADDR_STREET]);
            logger.debug("CITY         : " + address[Contact.ADDR_LOCALITY]);
            logger.debug("STATE        : " + address[Contact.ADDR_REGION]);
            logger.debug("POSTCODE     : " + address[Contact.ADDR_POSTALCODE]);
            logger.debug("COUNTRY      : " + address[Contact.ADDR_COUNTRY]);
        }

        return address;
    }

    private String[] getOrgStringArray(Cursor orgCursor) {
        String[] org = new String[2];

        org[Contact.ORG_COMPANY] = orgCursor.getString(orgCursor.getColumnIndex(Organization.COMPANY));
        org[Contact.ORG_DEPT] = orgCursor.getString(orgCursor.getColumnIndex(Organization.DEPARTMENT));

        if (logger != null) {
            logger.debug("AndroidContactListAPI5 read Organization details : ");
            logger.debug("COMPANY    : " + org[Contact.ORG_COMPANY]);
            logger.debug("DEPARTMENT : " + org[Contact.ORG_DEPT]);
        }

        return org;
    }

    private int getPhotoRow(String contactId) {
        Uri queryUri = Data.CONTENT_URI;
        String where = Data.CONTACT_ID + " = ? AND " + Data.MIMETYPE + " = ?";
        String[] projection = new String[] { BaseColumns._ID };
        String[] whereParams = new String[] { contactId, Photo.CONTENT_ITEM_TYPE };

        Cursor cursor = doQuery(queryUri, projection, where, whereParams, null);

        int photoRow = -1;

        if (cursor != null) {
            int idIdx = cursor.getColumnIndexOrThrow(BaseColumns._ID);

            if (cursor.moveToFirst()) photoRow = cursor.getInt(idIdx);

            cursor.close();
        }

        return photoRow;
    }

    // resolver wrapper methods - purely for logging...

    private Cursor doQuery(Uri queryUri, String[] projection, String where, String[] whereParams, String order) {
        if (logger != null) {
            logger.debug("doQuery - about to query Uri " + queryUri + " with where cluse of " + where);
            if (whereParams != null) {
                logger.debug(" and whereParams of");
                for (String whereParam : whereParams) {
                    logger.debug(" " + whereParam);
                }
            }
        }

        return resolver.query(queryUri,    // Uri to query
                projection,  // Which columns to return
                where,       // WHERE clause; which rows to return
                whereParams, // WHERE clause selection arguments
                order);      // Order-by clause
    }

    private Uri doInsert(Uri insertUri, ContentValues values) {
        if (logger != null) logger.debug("doInsert - about to insert to " + insertUri);

        return resolver.insert(insertUri, values);
    }

    private int doUpdate(Uri updateUri, ContentValues values, String where, String[] selectionArgs) {
        if (logger != null) logger.debug("doUpdate - about to update to " + updateUri);

        return resolver.update(updateUri, values, where, selectionArgs);
    }

    private int doDelete(Uri deleteUri, String where, String[] selectionArgs) {
        if (logger != null) logger.debug("doDelete - about to delete " + deleteUri);

        return resolver.delete(deleteUri, where, selectionArgs);
    }

    /**
     * writeContentValues.  Writes the ContentValues to each of the raw contact ids
     * present
     * @param raw_ids the ArrayList of raw contact ids associated with this contact
     * @param value the ContentValues to be added.
     */
    private void writeContentValues(ArrayList<String> raw_ids, ContentValues value) throws Exception {
        if (value != null) {
            // If there's more than one raw_id present, we need to update each one.
            for (String string : raw_ids) {
                if (doInsert(generateUri(string), value) == null) {
                    if (logger != null) logger.debug("writeContentValues: error inserting ContentValues");

                    throw new Exception("writeContentValues - unable to write to contact");
                }
            }
        }
    }

    /**
     * writeContentValues.  Writes all the ContentValues to each of the raw contact ids
     * present
     * @param raw_ids the ArrayList of raw contact ids associated with this contact
     * @param value the array of ContentValues to be added.
     */
    private void writeContentValues(ArrayList<String> raw_ids, ContentValues[] values) throws Exception {
        if (values != null) {
            // If there's more than one raw_id present, we need to update each one.
            for (String string : raw_ids) {
                Uri insertUri = generateUri(string);

                for (ContentValues value : values) {
                    if (doInsert(insertUri, value) == null) {
                        if (logger != null) logger.debug("writeContentValues: error inserting ContentValues");

                        throw new Exception("writeContentValues - unable to write array to contact");
                    }
                }
            }
        }
    }

    /**
     * writeContactPhoto.  Writes the Photo ContentValues to each of the raw contact ids
     * present
     * @param raw_ids the ArrayList of raw contact ids associated with this contact
     * @param value the photo ContentValues to be added.
     */
    private void writeContactPhoto(ArrayList<String> raw_ids, ContentValues value) throws Exception {
        if (value != null) {
            // If there's more than one raw_id present, we need to update each one.
            for (String contactId : raw_ids) {
                Uri photoUri = generateUri(contactId);

                int photoRow = getPhotoRow(contactId);
                if (photoRow >= 0) {
                    String where = BaseColumns._ID + " = " + photoRow;

                    if (doUpdate(photoUri, value, where, null) != 1) {
                        if (logger != null) logger.debug("writeContactPhoto: error updating photo field");

                        throw new Exception("writeContactPhoto: error updating photo field");
                    }
                } else {
                    if (doInsert(photoUri, value) == null) {
                        if (logger != null) logger.debug("writeContactPhoto: error updating photo field");

                        throw new Exception("writeContactPhoto: error inserting photo field");
                    }
                }
            }
        }
    }

    /**
     * generateUri.  Generates a Uri to a contacts content directory
     * @param contactId the id to use
     * @return the Uri to the contacts content directory
     */
    private Uri generateUri(String contactId) {
        Uri contactUri = Uri.withAppendedPath(ContactsContract.RawContacts.CONTENT_URI, contactId);
        Uri targetUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Data.CONTENT_DIRECTORY);

        return targetUri;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#createContact()
     */
    @Override
    public Contact createContact() {
        Contact c = AndroidContact.createInstance(this, logger);
        return c;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#getSupportedFields()
     */
    @Override
    public int[] getSupportedFields() {
        return supportedFieldList;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#getSupportedAttributes(int)
     */
    @Override
    public int[] getSupportedAttributes(int fieldId) {
        return supportedAttributesList;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#isSupportedAttribute(int, int)
     */
    @Override
    public boolean isSupportedAttribute(int fieldId, int typeAttr) {
        for (int element : supportedAttributesList) {
            if (element == typeAttr) return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#isSupportedField(int)
     */
    @Override
    public boolean isSupportedField(int field) {
        for (int element : supportedFieldList) {
            if (element == field) return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#getFieldDataType(int)
     */
    @Override
    public int getFieldDataType(int field) {

        switch (field) {
        case Contact.EMAIL:
            return Contact.STRING;
        case Contact.FORMATTED_NAME:
            return Contact.STRING;
        case Contact.NAME:
            return Contact.STRING_ARRAY;
        case Contact.ORG:
            return Contact.STRING_ARRAY;
        case Contact.NOTE:
            return Contact.STRING;
        case Contact.TEL:
            return Contact.STRING;
        case Contact.UID:
            return Contact.STRING;
        case Contact.URL:
            return Contact.STRING;
        case Contact.ADDR:
            return Contact.STRING_ARRAY;
        case Contact.BIRTHDAY:
            return Contact.STRING;
        case Contact.ANNIVERSARY:
            return Contact.STRING;
        case Contact.NICKNAME:
            return Contact.STRING;
        case Contact.PHOTO:
            return Contact.BINARY;
        case Contact.PHOTO_URL:
            return Contact.STRING;
        case Contact.REVISION:
            return Contact.STRING;
        case Contact.TITLE:
            return Contact.STRING;

        default:
            return -1;
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#stringArraySize(int)
     */
    @Override
    public int stringArraySize(int stringArrayField) {
        if (stringArrayField == Contact.NAME)
            return 5;
        else if (stringArrayField == Contact.ORG)
            return 2;
        else if (stringArrayField == Contact.ADDR) return 7;

        return 0;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#getName()
     */
    @Override
    public String getName() {
        return CONTACTLIST_NAME;
    }

    @Override
    public void close() {
        allContacts = null;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#removeContact(net.cp.engine.contacts.Contact)
     */
    @Override
    public void removeContact(Contact contact) throws Exception {
        AndroidContactAPI5 aContact = (AndroidContactAPI5) contact;
        String cid = aContact.getUID();
        
        String selection = RawContacts.CONTACT_ID + "=? AND " + excludeReadonlyRawContacts();

        int result = doDelete(RawContacts.CONTENT_URI, selection, new String[] { cid });
        if (logger != null) {
            logger.debug(result + "raw contact deleted with contact id of " + cid);  
        }
        
        if (result == 0) {
            if (logger != null) logger.debug("Unable to remove raw contact with contact id of " + cid);

            throw new RuntimeException("AndroidContactListAPI5 Unable to remove contact");
        }
    }

    /**
     * Add or update the supplied contact
     * @param contact This MUST be an instance of AndroidContact
     * @return True - if the contact is new added; False -  if the contact is aggregated to an exist PIM contact
     * @throws Exception if the contact could not be committed for any reason
     */
    @Override
    public void commit(Contact contact) throws Exception {
        AndroidContactAPI5 aContact = (AndroidContactAPI5) contact;
        String contactId = aContact.getUID();

        if (logger != null) {
            logger.debug("AndroidContactListAPI5 DISPLAY_NAME: " + contact.getString(Contact.FORMATTED_NAME, 0));
            logger.debug("AndroidContactListAPI5 NAME: " + getFormattedNameFromArray(contact.getStringArray(Contact.NAME, 0)));
        }

        Uri rawContactUri = null;
        ArrayList<String> raw_ids = new ArrayList<String>();

        // we are adding a new contact
        if (contactId == null || contactId.equals("")) {
            ContentValues personValue = new ContentValues();

            rawContactUri = doInsert(RawContacts.CONTENT_URI, personValue);
            if (rawContactUri == null) {
                throw new Exception("AndroidContactListAPI5 Unable to create new contact");
            }
            
            contactId = String.valueOf(getContactIdByRawContact(rawContactUri));
            
            aContact.setUID(contactId);
            
            raw_ids.add(String.valueOf(ContentUris.parseId(rawContactUri)));

            if (logger != null) logger.debug("AndroidContactListAPI5 New contact created, contact Id is : " + contactId);
        } else {
            // updating an existing contact
            if (logger != null) logger.debug("AndroidContactListAPI5 Found existing contact with Id [" + contactId + "] begin contact hash check");

            // check that the contact hasn't changed locally since we started syncing
            // TODO do we really need to handle this rare case?
//            AndroidContactAPI5 localContact = readContact(contactId);
//
//            if (localContact == null) {
//                if (logger != null) logger.debug("AndroidContactListAPI5 Unable to read existing contact");
//
//                throw new Exception("AndroidContactListAPI5 Unable to read existinga contact");
//            }

//            String localVersion = localContact.getString(Contact.VERSION, 0);
//            String currentVersion = aContact.getString(Contact.VERSION, 0);
//            
//            if (localVersion == null || localContact.equals(currentVersion) == false) {
//                throw new StoreException("Conflict: Contact has been modified locally since sync began", SyncML.STATUS_CONFLICT);
//            }
//
//            if (logger != null) logger.debug("end hash check: contact not modified locally during sync");

            // empty the contact(s) for clean writing... 
            ArrayList<String> rawIds = mRawContactIdCache.get(contactId);
            if (rawIds == null) {
                // should never happen, the cache should already be there
                // TODO read raw ids from contact id
                if (logger != null) {
                    logger.warn("no raw contacts found in cache with coantact id:" + contactId);
                }
            } else {
                deleteContactRows(rawIds);
            }
            
            raw_ids.addAll(rawIds);
        }

        // update the various details
        writeContentValues(raw_ids, aContact.getPersonValues());
        writeContentValues(raw_ids, aContact.getNoteValues());
        writeContentValues(raw_ids, aContact.getNicknameValues());
        writeContactPhoto(raw_ids, aContact.getPhotoValues());
        writeContentValues(raw_ids, aContact.getPhoneValues());
        writeContentValues(raw_ids, aContact.getEmailValues());
        writeContentValues(raw_ids, aContact.getAddressValues());
        writeContentValues(raw_ids, aContact.getOrganizationValues());
        writeContentValues(raw_ids, aContact.getWebValues());
        writeContentValues(raw_ids, aContact.getEventsValues());

        String version = getRawContactVersion(contactId);
        AndroidContact.StringField versionField = new AndroidContact.StringField(Contact.VERSION, version, Contact.ATTR_NONE);
        aContact.addField(versionField);

        if (logger != null) logger.debug("AndroidContactListAPI5 Contact commited");
    }
    
    private long getContactIdByRawContact(Uri rawContactUri) {
        String[] projection = {RawContacts.CONTACT_ID};
        Cursor cursor = doQuery(rawContactUri, projection, null, null, null);
        
        if (cursor != null && cursor.moveToFirst()) {
            long result = cursor.getLong(0);
            cursor.close();
            return result;
        } else {
            // should never happen
            throw new RuntimeException("No contact found for raw contact uri: " + rawContactUri);
        }
    }

    private String getRawContactVersion(String id) {
        String result = null;

        String[] projection = new String[] { ContactsContract.RawContacts.VERSION };
        String where = BaseColumns._ID + "=?";
        String[] whereArgs = new String[] { id };

        Cursor rawCursor = doQuery(RawContacts.CONTENT_URI, projection, where, whereArgs, null);
        if (rawCursor != null && rawCursor.moveToFirst()) {
            result = String.valueOf(rawCursor.getInt(0));

            rawCursor.close();
        }

        if (logger != null) {
            logger.debug("Read raw contact version string - contact id: " + id + " version: " + result);
        }

        return result;
    }

    private void deleteContactRows(ArrayList<String> rawIds) throws Exception {
        for (String id : rawIds) {
            int count = doDelete(ContactsContract.Data.CONTENT_URI, ContactsContract.Data.RAW_CONTACT_ID + "=?", new String[] { id });
            if (count == 0) {
                if (logger != null) logger.debug("Unable to delete contact rows");
                throw new Exception("Unable to delete contact rows");
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#getFieldLabel(int)
     */
    @Override
    public String getFieldLabel(int field) {
        switch (field) {
        case Contact.EMAIL:
            return Contact.LABEL_EMAIL;
        case Contact.FORMATTED_NAME:
            return Contact.LABEL_FORMATTED_NAME;
        case Contact.NAME:
            return Contact.LABEL_NAME;
        case Contact.ORG:
            return Contact.LABEL_ORG;
        case Contact.NOTE:
            return Contact.LABEL_NOTE;
        case Contact.TEL:
            return Contact.LABEL_TELEPHONE;
        case Contact.UID:
            return Contact.LABEL_UID;
        case Contact.ADDR:
            return Contact.LABEL_ADDRESS;
        case Contact.FORMATTED_ADDR:
            return Contact.LABEL_FORMATTED_ADDRESS;
        case Contact.ANNIVERSARY:
            return Contact.LABEL_ANNIVERSARY;
        case Contact.BIRTHDAY:
            return Contact.LABEL_BIRTHDATE;
        case Contact.NICKNAME:
            return Contact.LABEL_NICKNAME;
        case Contact.PHOTO:
            return Contact.LABEL_PHOTO;
        case Contact.PUBLIC_KEY:
            return Contact.LABEL_PUBLIC_KEY;
        case Contact.REVISION:
            return Contact.LABEL_REVISION;
        case Contact.TITLE:
            return Contact.LABEL_TITLE;
        case Contact.URL:
            return Contact.LABEL_URL;

        default:
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#size()
     */
    @Override
    public int size() {
        return getContactsSize();
    }
    
    private int getContactsSize() {
        Cursor cursor = queryRawContacts();

        if (cursor != null && cursor.moveToFirst()) {
            int cidColumn = cursor.getColumnIndexOrThrow(RawContacts.CONTACT_ID);
            int count = 1;
            
            long currentContactId = cursor.getLong(cidColumn);
            
            while (cursor.moveToNext()) {
                long cid = cursor.getLong(2);
                
                if (cid == currentContactId) {
                    continue;
                } else {
                    currentContactId = cid;
                    count++;
                }
            }
            
            cursor.close();
            
            return count;
        } else {
            return 0;
        } 
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.ContactList#maxValues(int)
     */
    @Override
    public int maxValues(int field) {
        switch (field) {
        case Contact.EMAIL:
            return ANDROID_MAX_EMAILS;
        case Contact.FORMATTED_NAME:
            return 1;
        case Contact.NAME:
            return 2;
        case Contact.ORG:
            return ANDROID_MAX_ORG;
        case Contact.NOTE:
            return 2;
        case Contact.TEL:
            return ANDROID_MAX_PHONES;
        case Contact.UID:
            return 1;
        case Contact.ADDR:
            return ANDROID_MAX_STRUCTURED_ADDR;
        case Contact.PHOTO:
            return 1;
        case Contact.TITLE:
            return 1;
        case Contact.ANNIVERSARY:
            return 1;
        case Contact.BIRTHDAY:
            return 1;
        case Contact.URL:
            return ANDROID_MAX_URLS;

        default:
            return 0;
        }
    }

    /**
     * Not supported on Android
     */
    @Override
    public int[] getSupportedArrayElements(int field) {
        return null;
    }

    /**
     * This method reads a single contact from the Android contacts DB.
     * This method should not be used to read the entire contacts DB (instead use refreshContactsList())
     *
     * @param UID The UID for the contact to read
     * @return the contact with the given UID, or null if it could not be found
     */
    @Override
    public AndroidContactAPI5 readContact(String uid) {
        return readContactInternal(uid, false);
    }

    @Override
    public AndroidContactAPI5 readMinContact(String uid) {
        return readContactInternal(uid, true);
    }

    /** Try to figure out which are unnecessary */
    private static final String[] DATA_PROJECTION = new String[] { Data._ID, Data.CONTACT_ID, Data.RAW_CONTACT_ID, Data.MIMETYPE, Data.DISPLAY_NAME, Data.LOOKUP_KEY,
            Data.DATA1, Data.DATA2, Data.DATA3, Data.DATA4, Data.DATA5, Data.DATA6, Data.DATA7, Data.DATA8, Data.DATA9, Data.DATA10, Data.DATA11, Data.DATA12,
            Data.DATA13, Data.DATA14, Data.DATA15 };

    private static List<String> SUPPORTED_MIMETYPES = Arrays.asList(StructuredName.CONTENT_ITEM_TYPE, Nickname.CONTENT_ITEM_TYPE, Note.CONTENT_ITEM_TYPE,
            Photo.CONTENT_ITEM_TYPE, Phone.CONTENT_ITEM_TYPE, Email.CONTENT_ITEM_TYPE, StructuredPostal.CONTENT_ITEM_TYPE, Organization.CONTENT_ITEM_TYPE,
            Website.CONTENT_ITEM_TYPE, Event.CONTENT_ITEM_TYPE);
    
    private List<String> getRawContactsIds(String contactId) {
        // should already be here, then this would be a fast query
        List<String> result = mRawContactIdCache.get(contactId);
        
        if (result == null) {
            // fallback - in case the contact is not in the cache
            if (logger != null) {
                logger.debug("Raw contacts are  not in cache, about to query from the database...");
            }
        
            result = new ArrayList<String>();
            
            String selection = RawContacts.CONTACT_ID + "=? AND " + excludeReadonlyRawContacts();
            String[] args = {contactId};
            String[] projection = { BaseColumns._ID, RawContacts.CONTACT_ID };

            if (logger != null) {
                logger.debug("about to query raw contacts with query: " + selection);
            }
            Cursor cursor = doQuery(RawContacts.CONTENT_URI, projection, selection, args, null);
 
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    long rid = cursor.getLong(0);
                    result.add(String.valueOf(rid));
                } while (cursor.moveToNext());
            }
        }
 
        return result;
    }

    private AndroidContactAPI5 readContactInternal(String uid, boolean minimal) {
        // long start = System.currentTimeMillis();
        List<String> rawIds = getRawContactsIds(uid);
        
        if (rawIds.size() == 0) {
            if (logger != null) {
                logger.debug("Could not find raw contacts for UID: " + uid);
            }
            
            return null;
        } 
        
        StringBuffer sb = new StringBuffer(Data.RAW_CONTACT_ID + " IN (");
        for (String id: rawIds) {
            sb.append(id).append(",");
        }
        sb.setLength(sb.length() - 1);  // Remove the extra comma
        sb.append(")");
        
        String where = sb.toString();
        String sortBy = Data.MIMETYPE + " ASC";

        // long startQuery = System.currentTimeMillis();
        Cursor dataCursor = doQuery(Data.CONTENT_URI, DATA_PROJECTION, where, null, sortBy);
        // logger.info("PERFORMANCE: query contact:" + uid + " - time=" + (System.currentTimeMillis() - startQuery));
        if (dataCursor != null && dataCursor.moveToFirst()) {
            int mimeTypeCol = dataCursor.getColumnIndexOrThrow(Data.MIMETYPE);

            // set ID
            AndroidContactAPI5 contact = (AndroidContactAPI5) AndroidContact.createInstance(this, logger);
            contact.setUID(uid);

            if (logger != null) {
                logger.debug("AndroidContactListAPI5 read contact with UID: " + uid);
            }

            if (!minimal) {
                // long startSetContact = System.currentTimeMillis();
                do {
                    String mimetype = dataCursor.getString(mimeTypeCol);
                    if (mimetype == null || !SUPPORTED_MIMETYPES.contains(mimetype)) continue;

                    setContactData(contact, dataCursor, mimetype);
                } while (dataCursor.moveToNext());

                dataCursor.close();

                // logger.info("PERFORMANCE: Total set contact time=" + (System.currentTimeMillis() - startSetContact));
            }

            // logger.info("PERFORMANCE: End reading contact:" + uid + " - time=" + (System.currentTimeMillis() - start));

            return contact;
        }

        return null;
    }

    private void setContactData(AndroidContactAPI5 contact, Cursor dataCursor, String mimetype) {

        // long start = System.currentTimeMillis();

        if (mimetype.equals(StructuredName.CONTENT_ITEM_TYPE)) {
            setContactName(dataCursor, contact);
        } else if (mimetype.equals(Phone.CONTENT_ITEM_TYPE)) {
            setContactPhone(dataCursor, contact);
        } else if (mimetype.equals(Email.CONTENT_ITEM_TYPE)) {
            setContactEmail(dataCursor, contact);
        } else if (mimetype.equals(Photo.CONTENT_ITEM_TYPE)) {
            setContactPhoto(dataCursor, contact);
        } else if (mimetype.equals(Note.CONTENT_ITEM_TYPE)) {
            setContactNote(dataCursor, contact);
        } else if (mimetype.equals(StructuredPostal.CONTENT_ITEM_TYPE)) {
            setContactAddress(dataCursor, contact);
        } else if (mimetype.equals(Organization.CONTENT_ITEM_TYPE)) {
            setContactOrganization(dataCursor, contact);
        } else if (mimetype.equals(Website.CONTENT_ITEM_TYPE)) {
            setContactWebsite(dataCursor, contact);
        } else if (mimetype.equals(Event.CONTENT_ITEM_TYPE)) {
            setContactEvent(dataCursor, contact);
        } else if (mimetype.equals(Nickname.CONTENT_ITEM_TYPE)) {
            setContactNickName(dataCursor, contact);
        }
        // logger.info("PERFORMANCE: Read contact " + mimetype + " time=" + (System.currentTimeMillis() - start));
    }

    private void setContactEvent(Cursor dataCursor, AndroidContactAPI5 contact) {
        while (true) {
            int eventType = dataCursor.getInt(dataCursor.getColumnIndex(Event.TYPE));

            // We only care about Birthdays or Anniversaries
            if (eventType == Event.TYPE_BIRTHDAY) {
                String value = dataCursor.getString(dataCursor.getColumnIndex(Event.START_DATE));
                AndroidContact.StringField field = new AndroidContact.StringField(Contact.BIRTHDAY, value, 0);
                contact.addField(field);

                if (logger != null) logger.debug("AndroidContactListAPI5 readContactEventDetails: Found Birthday " + value);
            } else if (eventType == Event.TYPE_ANNIVERSARY) {
                String value = dataCursor.getString(dataCursor.getColumnIndex(Event.START_DATE));
                AndroidContact.StringField field = new AndroidContact.StringField(Contact.ANNIVERSARY, value, 0);
                contact.addField(field);

                if (logger != null) logger.debug("AndroidContactListAPI5 readContactEventDetails: Found Anniversary " + value);
            }

            String nextMimeType = getNextItemMimeType(dataCursor);
            if (Website.CONTENT_ITEM_TYPE.equals(nextMimeType)) {
                dataCursor.moveToNext();
            } else {
                break;
            }
        }
    }

    private void setContactWebsite(Cursor dataCursor, AndroidContactAPI5 contact) {
        AndroidContact.StringField field = null;
        while (true) {
            int webType = dataCursor.getInt(dataCursor.getColumnIndex(Website.TYPE));
            String value = dataCursor.getString(dataCursor.getColumnIndex(Website.URL));
            int attributes = contact.getAttributesFromWebType(webType);

            field = contact.getStringField(Contact.URL);
            if (field == null) {
                field = new AndroidContact.StringField(Contact.URL, value, attributes);
                contact.addField(field);
            } else
                field.addValue(value, attributes);

            if (logger != null)
                logger.debug("AndroidContactListAPI5 readContactWebsiteDetails: " + value + " with attributes: " + attributes + " of type " + webType);

            String nextMimeType = getNextItemMimeType(dataCursor);
            if (Website.CONTENT_ITEM_TYPE.equals(nextMimeType)) {
                dataCursor.moveToNext();
            } else {
                break;
            }
        }
    }

    private void setContactOrganization(Cursor dataCursor, AndroidContactAPI5 contact) {
        // set the title based on the first org entry
        AndroidContact.StringField titleField = new AndroidContact.StringField(Contact.TITLE, null, 0);
        AndroidContact.StringArrayField orgField = null;
        while (true) {
            int type = dataCursor.getInt(dataCursor.getColumnIndex(Organization.TYPE));
            int attributes = contact.getAttributesFromOrgType(type);
            String[] orgArray = getOrgStringArray(dataCursor);

            orgField = contact.getArrayField(Contact.ORG);
            if (orgField == null) {
                orgField = new AndroidContact.StringArrayField(Contact.ORG, orgArray, attributes);
                contact.addField(orgField);
            } else
                orgField.addValue(orgArray, attributes);

            String title = dataCursor.getString(dataCursor.getColumnIndex(Organization.TITLE));

            // set the title if applicable (we only support one title)
            if (title != null && title.length() > 0 && titleField.getValueCount() <= 0) {
                titleField.addValue(title, attributes);

                if (logger != null) logger.debug("AndroidContactListAPI5 readContactOrganizationDetails found title - " + title);
            }

            String nextMimeType = getNextItemMimeType(dataCursor);
            if (Organization.CONTENT_ITEM_TYPE.equals(nextMimeType)) {
                dataCursor.moveToNext();
            } else {
                break;
            }
        }

        contact.addField(titleField);
    }

    private String getNextItemMimeType(Cursor cursor) {
        cursor.moveToNext();
        if (cursor.isAfterLast()) return null;

        String result = cursor.getString(cursor.getColumnIndexOrThrow(Data.MIMETYPE));
        cursor.moveToPrevious();
        return result;
    }

    private void setContactEmail(Cursor dataCursor, AndroidContactAPI5 contact) {
        AndroidContact.StringField field = null;
        while (true) {
            int emailType = dataCursor.getInt(dataCursor.getColumnIndex(Email.TYPE));
            String value = dataCursor.getString(dataCursor.getColumnIndex(Email.DATA));
            int attributes = contact.getAttributesFromEmailType(emailType);

            field = contact.getStringField(Contact.EMAIL);
            if (field == null) {
                field = new AndroidContact.StringField(Contact.EMAIL, value, attributes);
                contact.addField(field);
            } else {
                field.addValue(value, attributes);
            }

            if (logger != null)
                logger.debug("AndroidContactListAPI5 readContactEmailAddresses: " + value + " with attributes: " + attributes + " of type " + emailType);

            String nextMimeType = getNextItemMimeType(dataCursor);
            if (Email.CONTENT_ITEM_TYPE.equals(nextMimeType)) {
                dataCursor.moveToNext();
            } else {
                break;
            }
        }
    }

    private void setContactAddress(Cursor dataCursor, AndroidContactAPI5 contact) {
        AndroidContact.StringArrayField addressField = null;
        while (true) {
            int type = dataCursor.getInt(dataCursor.getColumnIndex(StructuredPostal.TYPE));
            int attributes = contact.getAttributesFromAddressType(type);
            String[] addressArray = getAddressStringArray(dataCursor);

            addressField = contact.getArrayField(Contact.ADDR);
            if (addressField == null) {
                addressField = new AndroidContact.StringArrayField(Contact.ADDR, addressArray, attributes);
                contact.addField(addressField);
            } else {
                addressField.addValue(addressArray, attributes);
            }

            String nextMimeType = getNextItemMimeType(dataCursor);
            if (StructuredPostal.CONTENT_ITEM_TYPE.equals(nextMimeType)) {
                dataCursor.moveToNext();
            } else {
                break;
            }
        }
    }

    private void setContactPhone(Cursor dataCursor, AndroidContactAPI5 contact) {
        // add phone number details
        AndroidContact.StringField field = new AndroidContact.StringField(Contact.TEL, null, Contact.ATTR_NONE);

        int phoneTypeCol = dataCursor.getColumnIndexOrThrow(Phone.TYPE);
        int phoneCol = dataCursor.getColumnIndexOrThrow(Phone.NUMBER);
        while (true) {
            int phoneType = dataCursor.getInt(phoneTypeCol);
            String value = dataCursor.getString(phoneCol);
            int attributes = contact.getAttributesFromPhoneType(phoneType);
            field.addValue(value, attributes);

            if (logger != null) {
                logger.debug("AndroidContactListAPI5 read phone number: " + value + " with attributes: " + attributes + " of type " + phoneType);
            }

            String nextMimeType = getNextItemMimeType(dataCursor);
            if (Phone.CONTENT_ITEM_TYPE.equals(nextMimeType)) {
                dataCursor.moveToNext();
            } else {
                break;
            }
        }

        contact.addField(field);
    }

    private void setContactPhoto(Cursor dataCursor, AndroidContactAPI5 contact) {
        try {
            byte[] data = dataCursor.getBlob(dataCursor.getColumnIndex(Photo.PHOTO));

            if (data != null && data.length > 0) {
                AndroidContact.BinaryField photo = new AndroidContact.BinaryField(Contact.PHOTO, data, Contact.ATTR_NONE);
                contact.addField(photo);
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("AndroidContactListAPI5 Unable to read photo from contact", e);
        }
    }

    private void setContactNote(Cursor dataCursor, AndroidContactAPI5 contact) {
        String note = dataCursor.getString(dataCursor.getColumnIndex(Note.NOTE));

        AndroidContact.StringField noteField = new AndroidContact.StringField(Contact.NOTE, note, Contact.ATTR_NONE);
        contact.addField(noteField);

        if (logger != null) logger.debug("AndroidContactListAPI5 setContactNote: " + note);
    }

    private void setContactNickName(Cursor dataCursor, AndroidContactAPI5 contact) {
        String nickname = dataCursor.getString(dataCursor.getColumnIndex(Nickname.NAME));

        AndroidContact.StringField nicknameField = new AndroidContact.StringField(Contact.NICKNAME, nickname, Contact.ATTR_NONE);
        contact.addField(nicknameField);

        if (logger != null) logger.debug("AndroidContactListAPI5 setContactNickName: Nickname is " + nickname);
    }

    private void setContactName(Cursor dataCursor, AndroidContactAPI5 contact) {
        // set name
        String[] nameArray = getNameStringArray(dataCursor);
        AndroidContact.StringArrayField name = new AndroidContact.StringArrayField(Contact.NAME, nameArray, Contact.ATTR_NONE);
        contact.addField(name);

        String formattedName = dataCursor.getString(dataCursor.getColumnIndex(Data.DISPLAY_NAME));
        AndroidContact.StringField displayName = new AndroidContact.StringField(Contact.FORMATTED_NAME, formattedName, Contact.ATTR_NONE);
        contact.addField(displayName);

        if (logger != null) logger.debug("AndroidContactListAPI5 setContactName: " + formattedName + " structure names: " + nameArray);
    }

    /**
     * This method is used for parsing of contact lookup keys to find the raw contact ids
     * associated with this contact.  This could be one id, or more than one if the contact
     * is an aggregated contact
     *
     *  @param lookupKey - The lookup key to parse.  We only care about contacts lookup keys here
     *  @param contactId - The id of the contact this key came from
     *  @return ArrayList<String> of ids present in the lookup key.
     *
     */
    public ArrayList<String> parseLookupKey(String lookupKey, String contactId) {
        ArrayList<String> list = new ArrayList<String>();

        String string = Uri.decode(lookupKey);
        int offset = 0;
        int length = string.length();

        // NB: lookupKey is a '.' separated list of ids

        while (offset < length) {
            char c = 0;

            // Skip over the hash code (digits at start of string)
            while (offset < length) {
                c = string.charAt(offset++);
                if (c < '0' || c > '9') break;
            }

            // We're only interested in the raw contact id,
            // it's only present if the lookup type is 'r'
            if (c == 'r') {
                int dash = -1;
                int start = offset;
                while (offset < length) {
                    c = string.charAt(offset);
                    if (c == '-' && dash == -1) dash = offset;

                    offset++;
                    if (c == '.') break;
                }

                if (dash != -1) {
                    String id = string.substring(start, dash);

                    if (logger != null) logger.debug("Parsing lookup key - Found raw contact id " + id);

                    list.add(id);
                }
            } else {
                // its not a raw contact lookup id, so move to next
                // id in the list
                if (logger != null) logger.debug("Parsing lookup key - unwanted lookup type found - " + c);

                while (offset < length) {
                    c = string.charAt(offset++);
                    if (c == '.') break;
                }
            }
        }

        // If the list is still empty, there were no raw contacts
        // present in the key, so the contact this came from was a raw
        // contact, so add that to the list.
        if (list.isEmpty()) list.add(contactId);

        return list;
    }

    public abstract class CursorEnumeration implements Enumeration<Cursor> {
        String[] projection;
        String orderByColumn;

        /**
         * Uses default projection and sort order (reading contact IDs only)
         */
        public CursorEnumeration() {}

        public CursorEnumeration(String[] projection, String orderByColumn) {
            this.projection = projection;
            this.orderByColumn = orderByColumn;
        }

        @Override
        public abstract boolean hasMoreElements();

        @Override
        /**
         * Returns the next result as a Cursor
         * Note: Each Cursor is a unique reference owned by the caller, and MUST be closed.
         */
        public abstract Cursor nextElement();

    }

    protected String validContactsSelection() {
        return null;
    }

    @Override
    public HashMap<String, String> readAllContactIdAndVersion() {
        return getContactsIdAndVersion();
    }
    
    private HashMap<String, String> getContactsIdAndVersion() {
        mRawContactIdCache.clear();
        
        HashMap<String, String> result = new HashMap<String, String>();

        Cursor cursor = queryRawContacts();
        if (cursor != null && cursor.moveToFirst()) {
            long lastContactId = cursor.getLong(2);
            StringBuffer sb = new StringBuffer();
            ArrayList<String> rawIds = new ArrayList<String>();
            
            do {
                long id = cursor.getLong(0);
                int version = cursor.getInt(1);
                long cid = cursor.getLong(2);
                
                if (cid == lastContactId) {
                    // aggregated version format: rawId1|rawVersion1::rawId2|rawVersion2::
                    sb.append(id).append("|").append(version).append("::");
                    rawIds.add(String.valueOf(id));
                    
                    continue;
                } else {
                    String contactId = String.valueOf(lastContactId);
                    result.put(contactId, sb.toString());
                    mRawContactIdCache.put(contactId, rawIds);
                    
                    lastContactId = cid;
                    sb.setLength(0);
                    sb.append(id).append("|").append(version).append("::");
                    rawIds = new ArrayList<String>();
                    rawIds.add(String.valueOf(id));
                }
            } while (cursor.moveToNext());
            
            result.put(String.valueOf(lastContactId), sb.toString());
            mRawContactIdCache.put(String.valueOf(lastContactId), rawIds);
        }

        if (cursor != null) {
            cursor.close();
        }

        return result;
    }

    private Cursor queryRawContacts() {
        String selection = excludeReadonlyRawContacts();
        String[] projection = { BaseColumns._ID, RawContacts.VERSION, RawContacts.CONTACT_ID};

        String orderBy = RawContacts.CONTACT_ID + "," + RawContacts._ID + " ASC";

        return doQuery(ContactsContract.RawContacts.CONTENT_URI, projection, selection, null, orderBy);
    }
    
    private String excludeReadonlyRawContacts() {
        AndroidEngineSettings engineSettings = (AndroidEngineSettings) EngineSettings.getInstance();
        boolean syncReadOnlyContacts = engineSettings.syncReadOnlyContacts;

        StringBuilder sb = new StringBuilder(RawContacts.DELETED + "=0");
        if (!syncReadOnlyContacts) {
            ArrayList<String> readOnlyAccounts = new ArrayList<String>();

            for (SyncAdapterType syncAdapter : ContentResolver.getSyncAdapterTypes()) {
                if (!syncAdapter.supportsUploading()) {
                    readOnlyAccounts.add(syncAdapter.accountType);
                }
            }

            if (readOnlyAccounts.size() != 0) {
                sb.append(" AND " + RawContacts.ACCOUNT_TYPE + " NOT IN(");
                for (String account : readOnlyAccounts) {
                    sb.append('\'').append(account).append('\'').append(",");
                }
                sb.setLength(sb.length() - 1);  // Remove the extra comma
                sb.append(")");
            }
        }

        return sb.toString();
    }

    @Override
    public List<Contact> readListOfContacts(List<String> ids) {
        List<Contact> result = new ArrayList<Contact>();

        if (ids.size() == 0) {
            return result;
        }
        
        List<String> rawIds = new ArrayList<String>();
        for (String id : ids) {
            List<String> rawIdsForContact = getRawContactsIds(id);
            
            if (rawIdsForContact.size() == 0) {
                if (logger != null) {
                    logger.warn("Could not find raw contacts for UID: " + id);
                }
            } 
            
            rawIds.addAll(rawIdsForContact);
        }
        
        if (rawIds.size() == 0) {
            return result;
        }
                
        StringBuffer sb = new StringBuffer(Data.RAW_CONTACT_ID + " IN (");
        for (String id: rawIds) {
            sb.append(id).append(",");
        }
        sb.setLength(sb.length() - 1);  // Remove the extra comma
        sb.append(")");
        
        String where = sb.toString();
        String sortBy = Data.CONTACT_ID + "," + Data.MIMETYPE + " ASC";

        Cursor dataCursor = doQuery(Data.CONTENT_URI, DATA_PROJECTION, where, null, sortBy);
        if (dataCursor != null && dataCursor.moveToFirst()) {
            int contactIdCol = dataCursor.getColumnIndexOrThrow(Data.CONTACT_ID);
            int mimeTypeCol = dataCursor.getColumnIndexOrThrow(Data.MIMETYPE);

            Long currentContactId = null;
            AndroidContactAPI5 currentContact = null;

            do {
                Long contactId = dataCursor.getLong(contactIdCol);
                if (!contactId.equals(currentContactId)) {
                    currentContact = (AndroidContactAPI5) AndroidContact.createInstance(this, logger);
                    currentContactId = contactId;

                    String strCurrentContactId = String.valueOf(currentContactId);
                    currentContact.setUID(strCurrentContactId);

                    result.add(currentContact);
                }

                String mimetype = dataCursor.getString(mimeTypeCol);
                if (mimetype == null || !SUPPORTED_MIMETYPES.contains(mimetype)) continue;

                setContactData(currentContact, dataCursor, mimetype);
            } while (dataCursor.moveToNext());

            dataCursor.close();
        }

        return result;
    }
}
