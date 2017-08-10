/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import android.content.ContentValues;
import android.provider.Contacts;
import android.provider.Contacts.People;
import net.cp.engine.UtilityClass;
import net.cp.engine.contacts.ContactList;
import net.cp.syncml.client.util.Logger;

@SuppressWarnings("deprecation")
public class AndroidContactAPI3 extends AndroidContact
{

    public AndroidContactAPI3(ContactList list, Logger logger)
    {
        super(list, logger);

        if(logger != null)
            logger.info("AndroidContactAPI3()");
    }

    /**
     * Get information suitable for the Android "People" table.
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues getPersonValues()
    {
        //we need at least one entry between the following 3 fields in order to proceed
        int numValues = countValues(NAME) > 0 ? 1:0;
        numValues += countValues(FORMATTED_NAME) > 0 ? 1:0;
        numValues += countValues(NOTE) > 0 ? 1:0;
        numValues += countValues(TEL) > 0 ? 1:0;

        ContentValues personValues = null;

        if(numValues > 0)
        {
            personValues = new ContentValues();

            /**
             * If there is no name entry, find something to put in there
             */

            String value = AndroidContactList.getFormattedNameFromArray(getStringArray(NAME, 0));

            if(value == null || value.length() <= 0)
                value = getString(FORMATTED_NAME, 0);

            if(value == null || value.length() <= 0)
                value = getString(NOTE, 0);

            if(value == null || value.length() <= 0)
                value = getString(TEL, 0);

            personValues.put(People.NAME, value);

            String value2 = getString(NOTE, 0);

            personValues.put(People.NOTES, value2);

            if(logger != null)
                logger.debug("adding name: " + value + " and note: " + value2);
        }

        return personValues;

    }

    /**
     * Get photo info in a format suitable for Android contacts DB
     *
     * @return ContentValues: a set of column names and associated values, or a null valued set if there is no photo
     */
    public ContentValues getPhotoValues()
    {
        int numValues = countValues(PHOTO);

        ContentValues photoValues = new ContentValues();

        if(numValues > 0)
        {
            byte[] photoData = getBinary(PHOTO, 0);

            if(photoData == null || photoData.length <= 0)
                photoValues.putNull(Contacts.PhotosColumns.DATA);

            else
            {
                photoValues.put(Contacts.PhotosColumns.DATA, photoData);

                if(logger != null)
                    logger.info("retreived photo of length: " + photoData.length);
            }
        }

        else //Android OS won't let us delete a contact photo, so enter a null value.
            photoValues.putNull(Contacts.PhotosColumns.DATA);

        return photoValues;
    }

    /**
     * Get organization info in a format suitable for Android contacts DB.
     *
     * @return ContentValues: a set of column names and associated values, or null if there are is no appropriate data
     */
    public ContentValues[] getOrganizationValues()
    {
        int numOrgValues = countValues(ORG);

        ContentValues orgValues = null;

        //add orgs
        if(numOrgValues > 0)
        {
            ContentValues[] rows = new ContentValues[numOrgValues];

            for(int i=0; i<numOrgValues; i++)
            {
                orgValues = new ContentValues();

                /**
                 * We only support one title, and put it in the first organization.
                 * Chances are there will only be one organization in vast majority of cases.
                 */
                if(i == 0)
                {
                    String titleValue = getString(TITLE, 0);

                    if(titleValue != null && titleValue.length() > 0)
                        orgValues.put(Contacts.Organizations.TITLE, titleValue);
                }

                String value = getString(ORG, i);

                orgValues.put(Contacts.Organizations.COMPANY, value);

                int type = getOrgTypeFromAttributes(getAttributes(ORG, i));
                orgValues.put(Contacts.Organizations.TYPE, type);

                rows[i] = orgValues;

                if(logger != null)
                    logger.debug("adding organization value: " + value);
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
    public ContentValues[] getPhoneValues()
    {
        int numValues = countValues(TEL);

        if(numValues > 0)
        {

            ContentValues[] rows = new ContentValues[numValues];

            //process each number and add it to the array
            for(int i=0; i<numValues; i++)
            {
                ContentValues phoneValues = new ContentValues();

                String value = getString(TEL, i);

                phoneValues.put(Contacts.Phones.NUMBER, value);

                int type = getPhoneTypeFromAttributes(getAttributes(TEL, i));

                phoneValues.put(Contacts.Phones.TYPE, type);

                rows[i] = phoneValues;

                if(logger != null)
                    logger.debug("adding phone number: " + value);
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
    public ContentValues[] getContactMethodsValues()
    {
        int numEmailValues = countValues(EMAIL);
        int numAddressValues = countValues(FORMATTED_ADDR);

        ContentValues[] rows = null;

        //add emails
        if(numEmailValues > 0)
        {
            int rowCount = numEmailValues;
            if(numAddressValues > 0)
                rowCount += numAddressValues;

            rows = new ContentValues[rowCount];

            for(int i=0; i<numEmailValues; i++)
            {
                ContentValues contactMethodValues = new ContentValues();

                String value = getString(EMAIL, i);
                contactMethodValues.put(Contacts.ContactMethods.KIND, Contacts.KIND_EMAIL);

                int type = getContactMethodTypeFromAttributes(getAttributes(EMAIL, i));
                contactMethodValues.put(Contacts.ContactMethods.TYPE, type);

                contactMethodValues.put(Contacts.ContactMethods.DATA, value);

                if(logger != null)
                    logger.debug("adding email: " + value + " with type: " + type);

                rows[i] = contactMethodValues;
            }
        }

        else
            numEmailValues = 0; // could have been -1

        //add addresses
        if(numAddressValues > 0)
        {
            if(rows == null)
                rows = new ContentValues[numAddressValues];

            for(int i=0; i<numAddressValues; i++)
            {
                ContentValues contactMethodValues = new ContentValues();
                String value = getString(FORMATTED_ADDR, i);
                contactMethodValues.put(Contacts.ContactMethods.KIND, Contacts.KIND_POSTAL);

                int type = getContactMethodTypeFromAttributes(getAttributes(FORMATTED_ADDR, i));
                contactMethodValues.put(Contacts.ContactMethods.TYPE, type);

                contactMethodValues.put(Contacts.ContactMethods.DATA, value);

                rows[numEmailValues + i] = contactMethodValues;

                if(logger != null)
                    logger.debug("adding address: " + value);
            }
        }
        return rows;
    }

    protected int getPhoneTypeFromAttributes(int attributes)
    {
        if(UtilityClass.isFlagSet(attributes, ATTR_FAX)
                && UtilityClass.isFlagSet(attributes, ATTR_HOME))
            return Contacts.Phones.TYPE_FAX_HOME;

        if(UtilityClass.isFlagSet(attributes, ATTR_FAX)
                && UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return Contacts.Phones.TYPE_FAX_WORK;

        if(UtilityClass.isFlagSet(attributes, ATTR_HOME))
            return Contacts.Phones.TYPE_HOME;

        if(UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return Contacts.Phones.TYPE_WORK;

        if(UtilityClass.isFlagSet(attributes, ATTR_MOBILE))
            return Contacts.Phones.TYPE_MOBILE;

        if(UtilityClass.isFlagSet(attributes, ATTR_PAGER))
            return Contacts.Phones.TYPE_PAGER;

        if(UtilityClass.isFlagSet(attributes, ATTR_OTHER))
            return Contacts.Phones.TYPE_OTHER;

        return Contacts.Phones.TYPE_OTHER;
    }

    /**
     * Convert from Android phone types to "MIDP style" attributes
     *
     * @param phoneType e.g. Contacts.Phones.TYPE_FAX_HOME
     * @return attributes e.g. ATTR_FAX | ATTR_HOME
     */
    protected int getAttributesFromPhoneType(int phoneType)
    {
        switch (phoneType)
        {
            case Contacts.Phones.TYPE_FAX_HOME: return (ATTR_FAX | ATTR_HOME);
            case Contacts.Phones.TYPE_FAX_WORK: return (ATTR_FAX | ATTR_WORK);
            case Contacts.Phones.TYPE_HOME: return ATTR_HOME;
            case Contacts.Phones.TYPE_WORK: return ATTR_WORK;
            case Contacts.Phones.TYPE_MOBILE: return ATTR_MOBILE;
            case Contacts.Phones.TYPE_PAGER: return ATTR_PAGER;
            case Contacts.Phones.TYPE_OTHER: return ATTR_OTHER;
            case Contacts.Phones.TYPE_CUSTOM: return ATTR_NONE;
            default: return ATTR_NONE;
        }
    }

    /**
     * Convert from Android organization types to "MIDP style" attributes
     *
     * @param orgType e.g. Contacts.Organizations.TYPE_WORK
     * @return attributes e.g. ATTR_NONE
     */
    protected int getAttributesFromOrgType(int orgType)
    {
        switch (orgType)
        {
            case Contacts.Organizations.TYPE_WORK: return ATTR_WORK;
            case Contacts.Organizations.TYPE_OTHER: return ATTR_OTHER;
            case Contacts.Organizations.TYPE_CUSTOM: return ATTR_OTHER;

            default: return ATTR_NONE;
        }
    }

    /**
     * Convert from "MIDP style" attributes to Android organization types
     *
     * @param attributes e.g. ATTR_WORK
     * @return org type  e.g. Contacts.ContactMethods.TYPE_WORK
     */
    protected int getOrgTypeFromAttributes(int attributes)
    {
        if(UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return Contacts.ContactMethods.TYPE_WORK;

        if(UtilityClass.isFlagSet(attributes, ATTR_OTHER))
            return Contacts.Organizations.TYPE_OTHER;

        return Contacts.Organizations.TYPE_WORK;
    }

    /**
     *
     * Convert from Android contact method types to "MIDP style" attributes
     *
     * @param contactMethodType e.g. Contacts.ContactMethods.TYPE_HOME
     * @return attributes e.g. ATTR_HOME
     */
    protected int getAttributesFromContactMethodType(int contactMethodType)
    {
        switch (contactMethodType)
        {
            case Contacts.ContactMethods.TYPE_HOME: return ATTR_HOME;
            case Contacts.ContactMethods.TYPE_WORK: return ATTR_WORK;
            case Contacts.ContactMethods.TYPE_OTHER: return ATTR_NONE;
            case Contacts.ContactMethods.TYPE_CUSTOM: return ATTR_NONE;
            default: return ATTR_NONE;
        }
    }

    protected int getContactMethodTypeFromAttributes(int attributes)
    {
        if(UtilityClass.isFlagSet(attributes, ATTR_HOME))
            return Contacts.ContactMethods.TYPE_HOME;

        if(UtilityClass.isFlagSet(attributes, ATTR_WORK))
            return Contacts.ContactMethods.TYPE_WORK;

        if(UtilityClass.isFlagSet(attributes, ATTR_OTHER))
            return Contacts.ContactMethods.TYPE_OTHER;

        return Contacts.ContactMethods.TYPE_OTHER;
    }

    protected int getAttributesFromAddressType(int addressMethodType)
    {
        return 0;
    }

    protected int getAttributesFromEmailType(int contactMethodType)
    {
        return 0;
    }
}
