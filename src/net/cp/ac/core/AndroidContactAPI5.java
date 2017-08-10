/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.util.ArrayList;

import net.cp.engine.UtilityClass;
import net.cp.engine.contacts.Contact;
import net.cp.engine.contacts.ContactList;
import net.cp.syncml.client.util.Logger;
import android.content.ContentValues;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts.Data;

public class AndroidContactAPI5 extends AndroidContact {

    /*
     * The list of raw contacts associated with this contact (>1 if it's an aggregate)
     */
    private ArrayList<String> rawContactIds;

    public AndroidContactAPI5(ContactList list, Logger logger) {
        super(list, logger);
    }

    public ArrayList<String> getRawContactIds() {
        return rawContactIds;
    }

    public void setRawContactIds(ArrayList<String> ids) {
        rawContactIds = ids;
    }

    /**
     * Get information suitable for the Android "People" table.
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getPersonValues() {
        int numNameValues = countValues(NAME);

        if (numNameValues > 0) {
            ContentValues[] rows = new ContentValues[numNameValues];

            for (int i = 0; i < numNameValues; i++) {
                ContentValues personValue = new ContentValues();

                personValue.put(Data.RAW_CONTACT_ID, getUID());
                personValue.put(ContactsContract.Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
                personValue.put(StructuredName.DISPLAY_NAME, getString(FORMATTED_NAME, 0));

                String[] names = getStringArray(Contact.NAME, 0);

                if (names != null && names.length > 0) {
                    if (names[NAME_PREFIX] != null && names[NAME_PREFIX].length() > 0) personValue.put(StructuredName.PREFIX, names[NAME_PREFIX]);

                    if (names[NAME_GIVEN] != null && names[NAME_GIVEN].length() > 0) personValue.put(StructuredName.GIVEN_NAME, names[NAME_GIVEN]);

                    if (names[NAME_FAMILY] != null && names[NAME_FAMILY].length() > 0) personValue.put(StructuredName.FAMILY_NAME, names[NAME_FAMILY]);

                    if (names[NAME_SUFFIX] != null && names[NAME_SUFFIX].length() > 0) personValue.put(StructuredName.SUFFIX, names[NAME_SUFFIX]);
                }

                rows[i] = personValue;
            }
            return rows;
        }
        return null;
    }

    /**
     * Get photo info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or a null valued set if there is no photo
     */
    @Override
    public ContentValues getPhotoValues() {
        int numValues = countValues(PHOTO);

        if (numValues > 0) {
            ContentValues photoValues = new ContentValues();

            photoValues.put(ContactsContract.Data.IS_SUPER_PRIMARY, 1);
            photoValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE);

            byte[] photoData = getBinary(PHOTO, 0);

            if (photoData == null || photoData.length <= 0)
                photoValues.putNull(ContactsContract.CommonDataKinds.Photo.PHOTO);
            else {
                photoValues.put(ContactsContract.CommonDataKinds.Photo.PHOTO, photoData);

                if (logger != null) logger.info("retreived photo of length: " + photoData.length);
            }

            return photoValues;
        }

        return null;
    }

    public ContentValues getNicknameValues() {
        int numValues = countValues(NICKNAME);

        if (numValues > 0) {
            ContentValues nicknameValue = new ContentValues();

            nicknameValue.put(Data.RAW_CONTACT_ID, getUID());
            nicknameValue.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE);
            nicknameValue.put(ContactsContract.CommonDataKinds.Nickname.TYPE, ContactsContract.CommonDataKinds.Nickname.TYPE_DEFAULT);
            nicknameValue.put(ContactsContract.CommonDataKinds.Nickname.NAME, getString(NICKNAME, 0));

            return nicknameValue;
        }

        return null;
    }

    public ContentValues getNoteValues() {
        int numValues = countValues(NOTE);

        if (numValues > 0) {
            ContentValues noteValue = new ContentValues();

            noteValue.put(Data.RAW_CONTACT_ID, getUID());
            noteValue.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE);
            noteValue.put(ContactsContract.CommonDataKinds.Note.NOTE, getString(NOTE, 0));

            return noteValue;
        }

        return null;
    }

    /**
     * Get organization info in a format suitable for Android contacts DB.
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getOrganizationValues() {
        int numOrgValues = countValues(ORG);

        ContentValues orgValues = null;

        // add orgs
        if (numOrgValues > 0) {
            ContentValues[] rows = new ContentValues[numOrgValues];

            for (int i = 0; i < numOrgValues; i++) {
                orgValues = new ContentValues();

                /**
                 * We only support one title, and put it in the first organization.
                 * Chances are there will only be one organization in vast majority of cases.
                 */
                if (i == 0) {
                    String titleValue = getString(TITLE, 0);

                    if (titleValue != null && titleValue.length() > 0) orgValues.put(ContactsContract.CommonDataKinds.Organization.TITLE, titleValue);
                }

                String[] org = getStringArray(ORG, i);
                int type = getOrgTypeFromAttributes(getAttributes(ORG, i));

                // By default, one value is returned, ORG_COMPANY
                if (org[ORG_COMPANY] != null && org[ORG_COMPANY].length() > 0)
                    orgValues.put(ContactsContract.CommonDataKinds.Organization.COMPANY, org[ORG_COMPANY]);

                // But, if present, ORG_DEPT will also be set
                if (org.length == 2 && org[ORG_DEPT] != null && org[ORG_DEPT].length() > 0)
                    orgValues.put(ContactsContract.CommonDataKinds.Organization.DEPARTMENT, org[ORG_DEPT]);

                orgValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE);
                orgValues.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, getUID());
                orgValues.put(ContactsContract.CommonDataKinds.Organization.TYPE, type);

                rows[i] = orgValues;

                if (logger != null) {
                    logger.debug("adding organization value: Company : " + org[ORG_COMPANY] + " Type : " + type);
                    if (org.length == 2 && org[ORG_DEPT] != null && org[ORG_DEPT].length() > 0)
                        logger.debug("adding organization value: Dept: " + org[ORG_DEPT]);
                }
            }

            return rows;
        }

        return null;
    }

    /**
     * Get website info in a format suitable for Android contacts DB.
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getWebValues() {
        int numWebValues = countValues(URL);

        // add websites
        if (numWebValues > 0) {
            ContentValues[] rows = new ContentValues[numWebValues];

            for (int i = 0; i < numWebValues; i++) {
                ContentValues webValues = new ContentValues();

                String value = getString(URL, i);
                int type = getWebTypeFromAttributes(getAttributes(URL, i));

                webValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE);
                webValues.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, getUID());
                webValues.put(ContactsContract.CommonDataKinds.Website.URL, value);
                webValues.put(ContactsContract.CommonDataKinds.Website.TYPE, type);

                rows[i] = webValues;

                if (logger != null) logger.debug("adding website value: " + value + " with type : " + type);
            }

            return rows;
        }

        return null;
    }

    /**
     * Get phone number info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    @Override
    public ContentValues[] getPhoneValues() {
        int numValues = countValues(TEL);

        if (numValues > 0) {
            ContentValues[] rows = new ContentValues[numValues];

            // process each number and add it to the array
            for (int i = 0; i < numValues; i++) {
                ContentValues phoneValues = new ContentValues();

                phoneValues.put(Data.RAW_CONTACT_ID, getUID());
                phoneValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);

                String value = getString(TEL, i);
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.NUMBER, value);

                int type = getPhoneTypeFromAttributes(getAttributes(TEL, i));
                phoneValues.put(ContactsContract.CommonDataKinds.Phone.TYPE, type);

                rows[i] = phoneValues;

                if (logger != null) logger.debug("adding phone number: " + value + " of type " + type);
            }

            return rows;
        }

        return null;
    }

    /**
     * Get email info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getEmailValues() {
        int numValues = countValues(EMAIL);

        if (numValues > 0) {
            ContentValues[] rows = new ContentValues[numValues];

            // process each email and add it to the array
            for (int i = 0; i < numValues; i++) {
                ContentValues emailValues = new ContentValues();

                emailValues.put(Data.RAW_CONTACT_ID, getUID());
                emailValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);

                String value = getString(EMAIL, i);

                int type = getEMailTypeFromAttributes(getAttributes(EMAIL, i));
                emailValues.put(ContactsContract.CommonDataKinds.Email.TYPE, type);

                emailValues.put(ContactsContract.CommonDataKinds.Email.DATA, value);

                if (logger != null) logger.debug("adding email: " + value + " with type: " + type);

                rows[i] = emailValues;
            }

            return rows;
        }

        return null;
    }

    /**
     * Get address info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getAddressValues() {
        int numValues = countValues(ADDR);

        if (numValues > 0) {
            ContentValues[] rows = new ContentValues[numValues];

            // process each email and add it to the array
            for (int i = 0; i < numValues; i++) {
                ContentValues addrValues = new ContentValues();

                addrValues.put(Data.RAW_CONTACT_ID, getUID());
                addrValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE);

                int type = getAddressTypeFromAttributes(getAttributes(ADDR, i));
                addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, type);

                String[] addr = getStringArray(ADDR, i);

                if (addr.length > ADDR_POBOX && addr[ADDR_POBOX] != null && addr[ADDR_POBOX].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.POBOX, addr[ADDR_POBOX]);

                if (addr.length > ADDR_EXTRA && addr[ADDR_EXTRA] != null && addr[ADDR_EXTRA].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.NEIGHBORHOOD, addr[ADDR_EXTRA]);

                if (addr.length > ADDR_STREET && addr[ADDR_STREET] != null && addr[ADDR_STREET].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.STREET, addr[ADDR_STREET]);

                if (addr.length > ADDR_LOCALITY && addr[ADDR_LOCALITY] != null && addr[ADDR_LOCALITY].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.CITY, addr[ADDR_LOCALITY]);

                if (addr.length > ADDR_REGION && addr[ADDR_REGION] != null && addr[ADDR_REGION].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.REGION, addr[ADDR_REGION]);

                if (addr.length > ADDR_POSTALCODE && addr[ADDR_POSTALCODE] != null && addr[ADDR_POSTALCODE].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE, addr[ADDR_POSTALCODE]);

                if (addr.length > ADDR_COUNTRY && addr[ADDR_COUNTRY] != null && addr[ADDR_COUNTRY].length() > 0)
                    addrValues.put(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY, addr[ADDR_COUNTRY]);

                if (logger != null) {
                    logger.debug("AndroidContactAPI5 read Contact Address : ");
                    logger.debug("ADDRESS TYPE : " + type);
                    if (addr.length > ADDR_POBOX) logger.debug("PO BOX       : " + addr[ADDR_POBOX]);
                    if (addr.length > ADDR_EXTRA) logger.debug("NEIGHBORHOOD : " + addr[ADDR_EXTRA]);
                    if (addr.length > ADDR_STREET) logger.debug("STREET       : " + addr[ADDR_STREET]);
                    if (addr.length > ADDR_LOCALITY) logger.debug("CITY         : " + addr[ADDR_LOCALITY]);
                    if (addr.length > ADDR_REGION) logger.debug("STATE        : " + addr[ADDR_REGION]);
                    if (addr.length > ADDR_POSTALCODE) logger.debug("POSTCODE     : " + addr[ADDR_POSTALCODE]);
                    if (addr.length > ADDR_COUNTRY) logger.debug("COUNTRY      : " + addr[ADDR_COUNTRY]);
                }

                rows[i] = addrValues;
            }

            return rows;
        }

        return null;
    }

    /**
     * Get events info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getEventsValues() {
        int numBdays = countValues(BIRTHDAY);
        int numAnn = countValues(ANNIVERSARY);
        int numValues = 0;

        if (numBdays > 0) numValues += numBdays;

        if (numAnn > 0) numValues += numAnn;

        if (numValues > 0) {
            ContentValues[] rows = new ContentValues[numValues];

            // process each event and add it to the array, first birthdays
            if (numBdays > 0) {
                for (int i = 0; i < numBdays; i++) {
                    ContentValues eventValues = new ContentValues();
                    String birthday = getString(BIRTHDAY, i);

                    eventValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
                    eventValues.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, getUID());
                    eventValues.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY);
                    eventValues.put(ContactsContract.CommonDataKinds.Event.START_DATE, birthday);

                    rows[i] = eventValues;

                    if (logger != null) logger.debug("adding birthday: " + birthday);
                }
            } else
                numBdays = 0;  // was -1

            // now add anniversaries
            if (numAnn > 0) {
                if (rows == null) rows = new ContentValues[numAnn];

                for (int i = 0; i < numAnn; i++) {
                    ContentValues eventValues = new ContentValues();
                    String birthday = getString(ANNIVERSARY, i);

                    eventValues.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE);
                    eventValues.put(ContactsContract.Contacts.Data.RAW_CONTACT_ID, getUID());
                    eventValues.put(ContactsContract.CommonDataKinds.Event.TYPE, ContactsContract.CommonDataKinds.Event.TYPE_ANNIVERSARY);
                    eventValues.put(ContactsContract.CommonDataKinds.Event.START_DATE, birthday);

                    rows[numBdays + i] = eventValues;

                    if (logger != null) logger.debug("adding anniversary: " + birthday);
                }
            }

            return rows;
        }

        return null;
    }

    /**
     * Get contact methods info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getContactMethodsValues() {
        return null;
    }

    @Override
    protected int getPhoneTypeFromAttributes(int attributes) {
        if (UtilityClass.isFlagSet(attributes, ATTR_FAX) && UtilityClass.isFlagSet(attributes, ATTR_HOME))
            return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME;

        if (UtilityClass.isFlagSet(attributes, ATTR_FAX) && UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK;

        if (UtilityClass.isFlagSet(attributes, ATTR_PAGER) && UtilityClass.isFlagSet(attributes, ATTR_HOME))
            return ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;

        if (UtilityClass.isFlagSet(attributes, ATTR_PAGER) && UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER;

        if (UtilityClass.isFlagSet(attributes, ATTR_MOBILE) && UtilityClass.isFlagSet(attributes, ATTR_HOME))
            return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;

        if (UtilityClass.isFlagSet(attributes, ATTR_MOBILE) && UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE;

        if (UtilityClass.isFlagSet(attributes, ATTR_HOME)) return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;

        if (UtilityClass.isFlagSet(attributes, ATTR_WORK)) return ContactsContract.CommonDataKinds.Phone.TYPE_WORK;

        if (UtilityClass.isFlagSet(attributes, ATTR_OTHER)) return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;

        return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
    }

    /**
     * Convert from "MIDP style" attributes to Android organization types
     *
     * @param attributes e.g. ATTR_WORK
     * @return org type  e.g. ContactsContract.CommonDataKinds.Organization.TYPE_WORK
     */
    @Override
    protected int getOrgTypeFromAttributes(int attributes) {
        if (UtilityClass.isFlagSet(attributes, ATTR_WORK)) return ContactsContract.CommonDataKinds.Organization.TYPE_WORK;

        if (UtilityClass.isFlagSet(attributes, ATTR_OTHER)) return ContactsContract.CommonDataKinds.Organization.TYPE_OTHER;

        return ContactsContract.CommonDataKinds.Organization.TYPE_WORK;
    }

    /**
     * Convert from "MIDP style" attributes to Android website types
     *
     * @param attributes e.g. ATTR_WORK
     * @return org type  e.g. ContactsContract.CommonDataKinds.Website.TYPE_WORK
     */
    protected int getWebTypeFromAttributes(int attributes) {
        if (UtilityClass.isFlagSet(attributes, ATTR_HOME)) return ContactsContract.CommonDataKinds.Website.TYPE_HOME;

        if (UtilityClass.isFlagSet(attributes, ATTR_WORK)) return ContactsContract.CommonDataKinds.Website.TYPE_WORK;

        return ContactsContract.CommonDataKinds.Website.TYPE_OTHER;
    }

    /**
     * Convert from "MIDP style" attributes to Android address types
     *
     * @param attributes e.g. ATTR_WORK
     * @return org type  e.g. ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK
     */
    protected int getAddressTypeFromAttributes(int attributes) {
        if (UtilityClass.isFlagSet(attributes, ATTR_HOME)) return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME;

        if (UtilityClass.isFlagSet(attributes, ATTR_WORK)) return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK;

        if (UtilityClass.isFlagSet(attributes, ATTR_OTHER)) return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER;

        return ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER;
    }

    /**
     * Convert from "MIDP style" attributes to Android Email types
     *
     * @param attributes e.g. ATTR_WORK
     * @return org type  e.g. ContactsContract.CommonDataKinds.Email.TYPE_WORK
     */
    protected int getEMailTypeFromAttributes(int attributes) {
        if (UtilityClass.isFlagSet(attributes, ATTR_HOME)) return ContactsContract.CommonDataKinds.Email.TYPE_HOME;

        if (UtilityClass.isFlagSet(attributes, ATTR_WORK)) return ContactsContract.CommonDataKinds.Email.TYPE_WORK;

        if (UtilityClass.isFlagSet(attributes, ATTR_OTHER)) return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;

        return ContactsContract.CommonDataKinds.Email.TYPE_OTHER;
    }

    /**
     *
     * Convert from Android contact address types to "MIDP style" attributes
     *
     * @param contactMethodType e.g. ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
     * @return attributes e.g. ATTR_HOME
     */
    @Override
    protected int getAttributesFromAddressType(int addressMethodType) {
        switch (addressMethodType) {
        case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME:
            return Contact.ATTR_HOME;
        case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK:
            return Contact.ATTR_WORK;
        case ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER:
            return Contact.ATTR_ADDR_OTHER;
        default:
            return Contact.ATTR_ADDR_OTHER;
        }
    }

    /**
     *
     * Convert from Android contact email types to "MIDP style" attributes
     *
     * @param contactMethodType e.g. ContactsContract.CommonDataKinds.Email.TYPE_HOME
     * @return attributes e.g. ATTR_HOME
     */
    @Override
    protected int getAttributesFromEmailType(int contactMethodType) {
        switch (contactMethodType) {
        case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
            return Contact.ATTR_HOME;
        case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
            return Contact.ATTR_WORK;
        case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
            return Contact.ATTR_ADDR_OTHER;
        case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
            return Contact.ATTR_ADDR_OTHER;
        default:
            return Contact.ATTR_ADDR_OTHER;
        }
    }

    /**
     * Convert from Android phone types to "MIDP style" attributes
     *
     * @param phoneType e.g. Contacts.Phones.TYPE_FAX_HOME
     * @return attributes e.g. ATTR_FAX | ATTR_HOME
     */
    @Override
    protected int getAttributesFromPhoneType(int phoneType) {
        switch (phoneType) {
        case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
            return (ATTR_FAX | ATTR_HOME);
        case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
            return (ATTR_FAX | ATTR_WORK);
        case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
            return ATTR_HOME;
        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
            return ATTR_WORK;
        case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
            return (ATTR_MOBILE | ATTR_HOME);
        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
            return (ATTR_MOBILE | ATTR_WORK);
        case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
            return (ATTR_PAGER | ATTR_HOME);
        case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
            return (ATTR_PAGER | ATTR_WORK);
        case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
            return ATTR_ADDR_OTHER;
        case BaseTypes.TYPE_CUSTOM:
            return ATTR_ADDR_OTHER;
        default:
            return ATTR_ADDR_OTHER;
        }
    }

    /**
     * Convert from Android organization types to "MIDP style" attributes
     *
     * @param orgType e.g. ContactsContract.CommonDataKinds.Organization.TYPE_WORK
     * @return attributes e.g. ATTR_NONE
     */
    @Override
    protected int getAttributesFromOrgType(int orgType) {
        switch (orgType) {
        case ContactsContract.CommonDataKinds.Organization.TYPE_WORK:
            return ATTR_WORK;
        case ContactsContract.CommonDataKinds.Organization.TYPE_OTHER:
            return ATTR_OTHER;
        case BaseTypes.TYPE_CUSTOM:
            return ATTR_OTHER;

        default:
            return ATTR_ADDR_OTHER;
        }
    }

    /**
     *
     * Convert from Android contact web url types to "MIDP style" attributes
     *
     * @param contactMethodType e.g. ContactsContract.CommonDataKinds.Email.TYPE_HOME
     * @return attributes e.g. ATTR_HOME
     */
    protected int getAttributesFromWebType(int webType) {
        switch (webType) {
        case ContactsContract.CommonDataKinds.Website.TYPE_HOME:
            return Contact.ATTR_HOME;
        case ContactsContract.CommonDataKinds.Website.TYPE_WORK:
            return Contact.ATTR_WORK;
        case ContactsContract.CommonDataKinds.Website.TYPE_OTHER:
            return Contact.ATTR_ADDR_OTHER;
        default:
            return Contact.ATTR_ADDR_OTHER;
        }
    }
}
