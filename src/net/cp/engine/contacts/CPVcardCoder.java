/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine.contacts;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import net.cp.ac.core.AndroidContact;
import net.cp.engine.UtilityClass;
import net.cp.mtk.common.CommonUtils;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;
import android.util.SparseArray;

/**
 * A class implementing a custom vCard encoder/decoder.
 *
 * @author  Denis Evoy
 */
public class CPVcardCoder extends VcardCoder {
    /* Definition of property names */

    protected static final String PROP_ADDRESS = "ADR";
    protected static final String PROP_ANNIVERSARY = "X-ANNIVERSARY";
    protected static final String PROP_BIRTHDATE = "BDAY";
    protected static final String PROP_CLASS = "CLASS";
    protected static final String PROP_EMAIL = "EMAIL";
    protected static final String PROP_FORMATTED_NAME = "FN";
    protected static final String PROP_GEOGRAPHIC_POSITION = "GEO";
    protected static final String PROP_PUBLIC_KEY = "KEY";
    protected static final String PROP_DELIVERY_LABEL = "LABEL";
    protected static final String PROP_LOGO = "LOGO";
    protected static final String PROP_MAILER = "MAILER";
    protected static final String PROP_NAME = "N";
    protected static final String PROP_ORG = "ORG";
    protected static final String PROP_NICKNAME = "NICKNAME";
    protected static final String PROP_NOTE = "NOTE";
    protected static final String PROP_PHOTO = "PHOTO";
    protected static final String PROP_REVISION = "REV";
    protected static final String PROP_ROLE = "ROLE";
    protected static final String PROP_SOUND = "SOUND";
    protected static final String PROP_TELEPHONE = "TEL";
    protected static final String PROP_TITLE = "TITLE";
    protected static final String PROP_TIMEZONE = "TZ";
    protected static final String PROP_UID = "UID";
    protected static final String PROP_URL = "URL";
    protected static final String PROP_VERSION = "VERSION";

    /* Definition of property parameter names */
    protected static final String PARAM_ENCODING = "ENCODING";
    protected static final String PARAM_CHARSET = "CHARSET";         // vCard 2.1 only
    protected static final String PARAM_LANGUAGE = "LANGUAGE";
    protected static final String PARAM_TYPE = "TYPE";
    protected static final String PARAM_VALUE = "VALUE";

    /* Definition of possible values for the 'TYPE' parameter */
    protected static final String PARAM_TYPE_FAX = "FAX";
    protected static final String PARAM_TYPE_HOME = "HOME";
    protected static final String PARAM_TYPE_CELL = "CELL";
    protected static final String PARAM_TYPE_VOICE = "VOICE";
    protected static final String PARAM_TYPE_PAGER = "PAGER";
    protected static final String PARAM_TYPE_PREFERRED = "PREF";
    protected static final String PARAM_TYPE_WORK = "WORK";
    protected static final String PARAM_TYPE_MSG = "MSG";
    protected static final String PARAM_TYPE_OTHER = "OTHER";

    /* Definition of possible values for the 'ENCODING' parameter */
    protected static final String PARAM_ENCODING_BINARY = "B";               // vCard 3.0 only
    protected static final String PARAM_ENCODING_7BIT = "7BIT";
    protected static final String PARAM_ENCODING_8BIT = "8BIT";
    protected static final String PARAM_ENCODING_BASE64 = "BASE64";
    protected static final String PARAM_ENCODING_QP = "QUOTED-PRINTABLE";

    /* Definition of general vCard strings */
    protected static final String VCARD_BEGIN = "BEGIN:VCARD";
    protected static final String VCARD_END = "END:VCARD";

    /**
     * Separates multiple parameters
     */
    protected static final String VCARD_SEP_PARAMS = ";";

    /**
     * Separates parameter name and value
     */
    protected static final String VCARD_SEP_PARAM_VALUE = "=";

    /**
     * Separates multiple parameter values
     */
    protected static final String VCARD_SEP_PARAM_VALUES = ",";

    /**
     * Separates property name and value
     */
    protected static final String VCARD_SEP_VALUE = ":";

    /**
     * Separates multiple values
     */
    protected static final String VCARD_SEP_VALUES = ";";

    /**
     * Separates multiple properties
     */
    protected static final String VCARD_SEP_PROPERTY = "\r\n";

    /**
     * The separator character used to separate vCard property types
     */
    protected String typeSep;

    /**
     * The vCard version (2.1 or 3.0) actually being decoded
     */
    protected String decodeVersion;

    /** Creates an encoder/decoder to process UTF-8, version 3.0 vCards. */
    public CPVcardCoder(ContactStore ContactStore, Logger syncLogger) {
        this(ContactStore, "UTF-8", VERSION_3_0, syncLogger);
    }

    /** Creates an encoder/decoder to process vCards in the specified character set and preferred vCard version. */
    public CPVcardCoder(ContactStore ContactStore, String characterSet, String vcardVersion, Logger syncLogger) {
        super(ContactStore, characterSet, vcardVersion, syncLogger);

        // determine the separator for vCard property types - for vCard 2.1, each type is a separate parameter
        // while for vCard 3.0, each type is comma-separated parameter value
        typeSep = VCARD_SEP_PARAMS;
        if (version.equals(VERSION_3_0)) typeSep = VCARD_SEP_PARAM_VALUES;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.VcardCoder#getContactIdentifier(java.lang.String)
     */
    @Override
    public String getContactIdentifier(String vcardString) {
        if ((vcardString == null) || (vcardString.length() <= 0)) return null;

        try {
            if (logger != null) logger.info("VCARD_DECODER: Looking for vCard identity");

            return decode(vcardString, null, true);
        } catch (Throwable e) {
            if (logger != null) logger.error("VCARD_DECODER: Failed to determine the contact identifier - ignoring", e);

            return null;
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.VcardCoder#isFieldSupported(net.cp.engine.contacts.Contact, int)
     */
    @Override
    public boolean isFieldSupported(Contact contact, int fieldId) {
        // check if the field has a valid vCard property name mapping
        return (PROPERTY_NAMES.get(fieldId) != null);
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.contacts.VcardCoder#decode(net.cp.engine.contacts.ContactList, byte[])
     */
    @Override
    public Contact decode(ContactList contactList, byte[] vcardData) throws StoreException {
        try {
            // convert to a string that we can parse
            String vcardString = new String(vcardData, charset);

            return decode(contactList, vcardString);
        } catch (UnsupportedEncodingException e) {
            if (logger != null) logger.error("VCARD_DECODER: Failed to '" + charset + "' encode vCard data", e);

            throw new StoreException("Failed to '" + charset + "' encode vCard data", e);
        }
    }

    @Override
    public Contact decode(ContactList contactList, String vcardString) throws StoreException {
        try {
            // create an empty contact in the contact list and populate it from the vCard data
            Contact contact = contactList.createContact();
            decode(vcardString, contact, false);

            vcardString = null;
            // System.gc();

            return contact;
        } catch (Throwable e) {
            if (logger != null) logger.error("VCARD_DECODER: Failed to decode vCard data", e);

            throw new StoreException("Failed to decode vCard data", e);
        }
    }

    /** Reads the specified vCard string, populates the specified contact and returns the identity of the contact. */
    // TODO this can be optimized
    protected String decode(String vcardString, Contact contact, boolean identityOnly) throws StoreException {
        if (logger != null) logger.debug("VCARD_DECODER: Decoding vCard data");

        // reset state
        decodeVersion = null;

        // read and parse each vCard property
        boolean inVcard = false;
        StringBuffer vcardProperty = new StringBuffer();
        int strLen = vcardString.length();
        ContactList contacts = contact.getContactList();

        for (int vcardIndex = 0; vcardIndex < strLen;) {
            // read the property from the vCard data
            vcardProperty.setLength(0);
            vcardIndex = readProperty(vcardString, vcardIndex, vcardProperty);
            if (vcardProperty.length() <= 0) continue;
            String property = vcardProperty.toString();

            // check for the beginning and ending of the vCard
            if (property.equals(VCARD_BEGIN)) {
                if (logger != null) logger.debug("VCARD_DECODER: Beginning vCard decode");

                inVcard = true;
                continue;
            } else if (property.equals(VCARD_END)) {
                if (logger != null) logger.debug("VCARD_DECODER: Ending vCard decode");

                inVcard = false;
                continue;
            }

            // ignore all properties if we have not yet found the start of the vCard data
            if (!inVcard) continue;

            // split the property into name/parameters and value
            String propName = null;
            String propValue = "";
            int valueIndex = property.indexOf(VCARD_SEP_VALUE);
            if (valueIndex <= 0) continue;
            propName = property.substring(0, valueIndex);
            if ((valueIndex + 1) < property.length()) propValue = property.substring(valueIndex + 1);

            // parse the parameters from the name
            Vector<String> propParams = new Vector<String>();
            UtilityClass.getTokens(propName, propParams, ';');
            if (propParams.size() > 0) {
                // the first "parameter" is actually the property name - extract it
                propName = propParams.elementAt(0);
                propParams.removeElementAt(0);
            }

            // check for the version property
            if (propName.equals(PROP_VERSION)) {
                decodeVersion = propValue;
                if (logger != null) logger.debug("VCARD_DECODER: Decoding vCard version '" + decodeVersion + "'");
                continue;
            }

            // check if we're only interested in the identifier
            if (identityOnly) {
                String identity = getIdentity(propName, propValue);
                if ((identity != null) && (identity.length() > 0)) {
                    if (logger != null) logger.debug("VCARD_DECODER: Retrieved vCard identity '" + identity + "'");
                    return identity;
                }

                continue;
            }

            // get the field ID from the name of the vCard property - ignore fields with no associated ID
            int fieldId = REVERSE_PROPERTY_NAMES.get(propName);
            if (fieldId <= 0) {
                if (logger != null) logger.debug("VCARD_DECODER: Property '" + propName + "' ignored - unknown Contact field ID");
                continue;
            }

            // check that the field is supported
            if (!contacts.isSupportedField(fieldId)) {
                if (logger != null) logger.debug("VCARD_DECODER: Property '" + propName + "' ignored - unsupported field ID '" + fieldId + "'");
                continue;
            }

            // get the field attributes from the vCard property parameters - ignore fields with unsupported attributes
            int fieldAttributes = getFieldAttributes(contact, fieldId, propParams);
            if (fieldAttributes < 0) {
                if (logger != null) logger.debug("VCARD_DECODER: Property '" + propName + "' ignored - unsupported parameters '" + propParams + "'");
                continue;
            }

            // check if we have already processed all the allowed values for the field
            // String fieldLabel = contact.getPIMList().getFieldLabel(fieldId);
            int maxFieldValues = contact.getContactList().maxValues(fieldId);
            if ((maxFieldValues > 0) && (contact.countValues(fieldId) >= maxFieldValues)) {
                if (logger != null)
                    logger.debug("VCARD_DECODER: Property '" + propName + "' ignored - max values '" + maxFieldValues + "' already present for field '"
                            + fieldId + "'");
                continue;
            }

            if (logger != null)
                logger.debug("VCARD_DECODER: Property '" + propName + "' params " + propParams + ": field '" + fieldId + "' attributes '" + fieldAttributes
                        + "'");

            // set the value of the contact from the vCard property value
            setFieldValue(contact, fieldId, fieldAttributes, propValue);

        }

        // make sure that we reached the end of the vCard data
        if (inVcard) throw new StoreException("Incomplete vCard data");

        if (logger != null) logger.info("VCARD_ENCODER: Finished decoding vCard data");

        return null;
    }

    private static int[] supportedFields = null;
    private static SparseArray<String> PROPERTY_NAMES = new SparseArray<String>();
    static {
        PROPERTY_NAMES.put(Contact.ADDR, PROP_ADDRESS);
        PROPERTY_NAMES.put(Contact.ANNIVERSARY, PROP_ANNIVERSARY);
        PROPERTY_NAMES.put(Contact.BIRTHDAY, PROP_BIRTHDATE);
        PROPERTY_NAMES.put(Contact.CLASS, PROP_CLASS);
        PROPERTY_NAMES.put(Contact.EMAIL, PROP_EMAIL);
        PROPERTY_NAMES.put(Contact.FORMATTED_ADDR, PROP_DELIVERY_LABEL);
        PROPERTY_NAMES.put(Contact.FORMATTED_NAME, PROP_FORMATTED_NAME);
        PROPERTY_NAMES.put(Contact.NAME, PROP_NAME);
        PROPERTY_NAMES.put(Contact.NICKNAME, PROP_NICKNAME);
        PROPERTY_NAMES.put(Contact.NOTE, PROP_NOTE);
        PROPERTY_NAMES.put(Contact.ORG, PROP_ORG);
        PROPERTY_NAMES.put(Contact.REVISION, PROP_REVISION);
        PROPERTY_NAMES.put(Contact.TEL, PROP_TELEPHONE);
        PROPERTY_NAMES.put(Contact.TITLE, PROP_TITLE);
        PROPERTY_NAMES.put(Contact.URL, PROP_URL);
        PROPERTY_NAMES.put(Contact.PHOTO, PROP_PHOTO);
    }

    private static Map<String, Integer> REVERSE_PROPERTY_NAMES = new HashMap<String, Integer>();
    static {
        for (int i = 0; i < PROPERTY_NAMES.size(); i++) {
            REVERSE_PROPERTY_NAMES.put(PROPERTY_NAMES.valueAt(i), PROPERTY_NAMES.keyAt(i));
        }
    }

    @Override
    public void encode(Contact contact, OutputStream stream) throws StoreException {
        try {

            long start = System.currentTimeMillis();
            if (logger != null) logger.debug("VCARD_ENCODER: Encoding vCard data");

            if (supportedFields == null) {
                // get the list of supported fields - nothing more to do if there are none
                ContactList contactList = contact.getContactList();
                supportedFields = contactList.getSupportedFields();
                if ((supportedFields == null) || (supportedFields.length <= 0)) throw new StoreException("PIM does not support any fields");
            }

            // write the vCard header (including vCard version)
            writeString(stream, VCARD_BEGIN + VCARD_SEP_PROPERTY);
            writeString(stream, PROP_VERSION + VCARD_SEP_VALUE + version + VCARD_SEP_PROPERTY);

            // write each supported contact field as a vCard property
            int[] contactFields = getFieldIds(contact);
            for (int fieldId : contactFields) {

                if (!UtilityClass.contains(supportedFields, fieldId)) continue;

                // get the vCard property name from the Contact field ID - ignore fields with no associated property name
                String propertyName = PROPERTY_NAMES.get(fieldId);
                if ((propertyName == null) || (propertyName.length() <= 0)) {
                    if (logger != null) logger.debug("VCARD_ENCODER: Field '" + fieldId + "' ignored - unknown vCard property name");
                    continue;
                }

                // write each Contact field name/value pair as a vCard property
                for (int valueIndex = 0; valueIndex < getFieldValueCount(contact, fieldId); valueIndex++) {
                    // get the value of the vCard property from the Contact field data - ignore fields with no values (but empty values are allowed)
                    String propertyValue = getFieldValue(contact, fieldId, valueIndex);
                    if (propertyValue == null) {
                        if (logger != null) logger.debug("VCARD_ENCODER: Field '" + fieldId + "' value[" + valueIndex + "] ignored - no value");
                        continue;
                    }

                    // get the vCard property parameters from the Contact field attributes
                    String propertyParams = getPropertyParams(contact, fieldId, valueIndex);

                    // build the vCard property in the form "<propName>[;<propParams>]:<propValue>"
                    StringBuilder vcardProperty = new StringBuilder();
                    vcardProperty.append(propertyName);
                    if ((propertyParams != null) && (propertyParams.length() > 0)) {
                        vcardProperty.append(VCARD_SEP_PARAMS);
                        vcardProperty.append(propertyParams);
                    }
                    vcardProperty.append(VCARD_SEP_VALUE);
                    vcardProperty.append(propertyValue);

                    String vcardPropertyString = vcardProperty.toString();
                    writeString(stream, vcardPropertyString);
                    writeString(stream, VCARD_SEP_PROPERTY);
                    if (logger != null) logger.debug("VCARD_ENCODER: Field '" + fieldId + "' value[" + valueIndex + "]: " + vcardPropertyString);
                }
            }

            // write the vCard footer
            writeString(stream, VCARD_END);

            if (logger != null)
                logger.info("VCARD_ENCODER: Finished encoding vCard data for contact="
                        + ((contact instanceof AndroidContact) ? ((AndroidContact) contact).getUID() : "<unknown>") + " on "
                        + (System.currentTimeMillis() - start) + " ms.");
        } catch (Throwable e) {
            if (logger != null) logger.error("VCARD_ENCODER: Failed to encode vCard data", e);

            throw new StoreException("Failed to encode vCard data", e);
        }
    }

    /** Returns the IDs of the fields in the specified contact. */
    protected int[] getFieldIds(Contact contact) {
        return contact.getFields();
    }

    /** Returns the number of values in the specified contact field. */
    protected int getFieldValueCount(Contact contact, int fieldId) {
        return contact.countValues(fieldId);
    }

    /** Returns the maximum number of values that can be set for the specified contact field.
     *  This implementation only supports a max of 1 value per field
     */
    protected int getMaxFieldValueCount(Contact contact, int fieldId) {
        return 1;
    }

    /** Returns the value of the 'TYPE' vCard property parameter that is associated with the specified Contact field attribute. */
    protected String getPropertyTypeParam(Contact contact, int fieldId, int valueIndex, int fieldAttribute) {
        // map standard Contact field attributes to vCard property parameter values
        if (fieldAttribute == Contact.ATTR_FAX) return PARAM_TYPE_FAX;
        if (fieldAttribute == Contact.ATTR_HOME) return PARAM_TYPE_HOME;
        if (fieldAttribute == Contact.ATTR_MOBILE) return PARAM_TYPE_CELL;
        if (fieldAttribute == Contact.ATTR_OTHER) return PARAM_TYPE_VOICE;
        if (fieldAttribute == Contact.ATTR_PAGER) return PARAM_TYPE_PAGER;
        if (fieldAttribute == Contact.ATTR_PREFERRED) return PARAM_TYPE_PREFERRED;
        if (fieldAttribute == Contact.ATTR_SMS) return PARAM_TYPE_MSG;
        if (fieldAttribute == Contact.ATTR_WORK) return PARAM_TYPE_WORK;
        if (fieldAttribute == Contact.ATTR_ADDR_OTHER) return PARAM_TYPE_OTHER;

        return null;
    }

    /** Returns the Contact field attribute that is associated with the specified value of the 'TYPE' vCard property parameter. */
    protected int getFieldTypeAttribute(Contact contact, int fieldId, String typeParam) {
        // map standard vCard property 'TYPE' parameter values to Contact field attributes
        if (typeParam.equals(PARAM_TYPE_FAX)) return Contact.ATTR_FAX;
        if (typeParam.equals(PARAM_TYPE_HOME)) return Contact.ATTR_HOME;
        if (typeParam.equals(PARAM_TYPE_CELL)) return Contact.ATTR_MOBILE;
        if (typeParam.equals(PARAM_TYPE_VOICE)) return Contact.ATTR_OTHER;
        if (typeParam.equals(PARAM_TYPE_PAGER)) return Contact.ATTR_PAGER;
        if (typeParam.equals(PARAM_TYPE_PREFERRED)) return Contact.ATTR_PREFERRED;
        if (typeParam.equals(PARAM_TYPE_MSG)) return Contact.ATTR_SMS;
        if (typeParam.equals(PARAM_TYPE_WORK)) return Contact.ATTR_WORK;
        if (typeParam.equals(PARAM_TYPE_OTHER)) return Contact.ATTR_ADDR_OTHER;

        return 0;
    }

    /** Returns the vCard property parameters (separated by ';') that are associated with the specified Contact field/value. */
    protected String getPropertyParams(Contact contact, int fieldId, int valueIndex) {
        if (fieldId == Contact.PHOTO) // add encoding param. Photo has no "type". return
            return PARAM_ENCODING + VCARD_SEP_PARAM_VALUE + PARAM_ENCODING_BASE64;

        // map standard Contact field attributes to vCard property parameters
        int fieldAttributes = contact.getAttributes(fieldId, valueIndex);

        if (logger != null) logger.debug("found field attributes: " + fieldAttributes + " field: " + fieldId);

        if (fieldAttributes <= 0) return null;

        // build the values of the 'TYPE' parameter based on the attributes that are set for the specified field
        StringBuffer typeParams = new StringBuffer();
        int[] supportedAttrs = contact.getContactList().getSupportedAttributes(fieldId);
        for (int supportedAttr : supportedAttrs) {
            // ignore attributes that are not set
            if ((fieldAttributes & supportedAttr) == 0) continue;

            // get the name of the attribute
            String typeParam = getPropertyTypeParam(contact, fieldId, valueIndex, supportedAttr);
            if ((typeParam != null) && (typeParam.length() > 0)) appendString(typeParams, typeParam, typeSep, false);
        }

        // trim the trailing separator character
        if (typeParams.length() > 0) typeParams.deleteCharAt(typeParams.length() - 1);

        // for vCard 3.0, the 'TYPE=' prefix is required - for vCard 2.1, it isn't
        if ((typeParams.length() > 0) && (version.equals(VERSION_3_0))) typeParams.insert(0, PARAM_TYPE + VCARD_SEP_PARAM_VALUE);

        return (typeParams.length() > 0) ? typeParams.toString() : null;
    }

    /** Returns the Contact field attributes that are associated with the specified vCard property types. */
    protected int getFieldTypeAttributes(Contact contact, int fieldId, String types) {
        // parse the parameter value as it may be comma-separated
        int result = 0;
        Vector<String> typeValues = new Vector<String>();
        UtilityClass.getTokens(types, typeValues, ',');
        for (int j = 0; j < typeValues.size(); j++) {
            String value = typeValues.elementAt(j);
            if ((value == null) || (value.length() <= 0)) continue;

            // map the value to an attribute - ignore values with no associated attribute
            int typeAttr = getFieldTypeAttribute(contact, fieldId, value);
            if (typeAttr <= 0) continue;

            // check if the attribute is supported
            if (!contact.getContactList().isSupportedAttribute(fieldId, typeAttr)) {
                // it doesn't matter if the phone doesn't support the 'PREF' attribute as it isn't
                // used to specify the type of the field (only if the field is the default one or not)
                if (typeAttr == Contact.ATTR_PREFERRED) continue;

                // ignore the field completely if any of the other attributes are not supported
                return -1;
            }

            // add the attribute to the current bit-mask
            result = result | typeAttr;
        }

        return result;
    }

    /** Returns the Contact field attributes that are associated with the specified vCard property parameters. */
    protected int getFieldAttributes(Contact contact, int fieldId, Vector<String> propParams) {
        // parse each parameter to create a bit-mask of attributes
        int attributes = 0;
        for (int i = 0; i < propParams.size(); i++) {
            String propParam = propParams.elementAt(i);

            // split each parameter into name and value
            String propParamName = null;
            String propParamValue = null;
            int index = propParam.indexOf(VCARD_SEP_PARAM_VALUE);
            if (index > 0) {
                propParamName = propParam.substring(0, index);
                if ((index + 1) < propParam.length()) propParamValue = propParam.substring(index + 1);
            } else if (index < 0) {
                // no name/value separator implies it's the 'TYPE' parameter
                propParamName = PARAM_TYPE;
                propParamValue = propParam;
            } else {
                // badly formed parameter - ignore
                continue;
            }

            // special handling for the 'TYPE' parameter
            if (propParamName.equals(PARAM_TYPE)) {
                int typeAttributes = getFieldTypeAttributes(contact, fieldId, propParamValue);
                attributes = attributes | typeAttributes;
            }
        }

        return attributes;
    }

    /**
     * Returns the String value of the specified field.
     *
     * @throws StoreException if the value of the field couldn't be retrieved.
     */
    protected String getFieldValue(Contact contact, int fieldId, int valueIndex) throws StoreException {
        // map the value of the field to a String
        int fieldDataType = contact.getContactList().getFieldDataType(fieldId);
        if (fieldDataType == Contact.STRING) {
            return escapeString(contact.getString(fieldId, valueIndex), "\\,");
        } else if (fieldDataType == Contact.STRING_ARRAY) {
            StringBuffer fieldValue = new StringBuffer();
            String[] fieldData = contact.getStringArray(fieldId, valueIndex);
            for (String element : fieldData)
                appendString(fieldValue, (element != null) ? element : "", VCARD_SEP_VALUES, true);

            // trim the trailing separator character
            if (fieldValue.length() > 0) fieldValue.deleteCharAt(fieldValue.length() - 1);

            return fieldValue.toString();
        } else if (fieldDataType == Contact.BINARY) {
            return CommonUtils.base64Encode(contact.getBinary(fieldId, valueIndex));
        }
        return null;
    }

    /**
     * Sets the String value of the specified field.
     *
     * @throws StoreException if the value of the field couldn't be set.
     */
    protected void setFieldValue(Contact contact, int fieldId, int fieldAttributes, String fieldValue) throws StoreException {
        // nothing more to do if there is no value
        if ((fieldValue == null) || (fieldValue.length() <= 0)) return;

        // map the String to the value of the field
        // String fieldLabel = contact.getPIMList().getFieldLabel(fieldId);
        int fieldDataType = contact.getContactList().getFieldDataType(fieldId);
        if (fieldDataType == Contact.STRING) {
            fieldValue = unescapeString(fieldValue, "\\;,");
            if (logger != null) logger.debug("VCARD_DECODER: Adding '" + fieldId + "' field with string value: '" + fieldValue + "'");
            contact.addString(fieldId, fieldAttributes, fieldValue);
        } else if (fieldDataType == Contact.BINARY) {
            if (logger != null) logger.debug("VCARD_DECODER: Adding '" + fieldId + "' field with binary value of length: '" + fieldValue.length() + "'");

            byte[] binaryBytes = CommonUtils.base64Decode(fieldValue);
            fieldValue = null; // try to save memory
            contact.addBinary(fieldId, fieldAttributes, binaryBytes, 0, binaryBytes.length);
        }

        else if (fieldDataType == Contact.STRING_ARRAY) {
            Vector<String> values = new Vector<String>();
            UtilityClass.getTokens(fieldValue, values, ';');
            if (values.size() > 0) {
                // determine the max number of values allowed in the array
                int maxValues = contact.getContactList().stringArraySize(fieldId);
                if (maxValues <= 0) maxValues = values.size();

                // populate the array up to the maximum number of allowed values
                String[] valueArray = new String[maxValues];
                for (int i = 0; i < maxValues; i++) {
                    if (i < values.size())
                        valueArray[i] = values.elementAt(i);
                    else
                        valueArray[i] = "";

                    if (logger != null)
                        logger.debug("VCARD_DECODER: Adding '" + fieldId + "' field with string array value[" + i + "]: '" + valueArray[i] + "'");
                }
                contact.addStringArray(fieldId, fieldAttributes, valueArray);
            }
        }
    }

    /** Returns an identifier that can be used by the user to identify the contact containing the specified vCard property value. */
    protected String getIdentity(String propName, String propValue) {
        // check if the property is one of the name properties
        if (propName.equals(PROP_FORMATTED_NAME)) {
            return propValue;
        } else if (propName.equals(PROP_NAME)) {
            Vector<String> names = new Vector<String>();
            UtilityClass.getTokens(propValue, names, ';');
            if (names.size() > 0) {
                String firstName = null;
                String lastName = names.elementAt(0);
                if (names.size() > 1) firstName = names.elementAt(1);

                if ((firstName != null) && (firstName.length() > 0) && (lastName != null) && (lastName.length() > 0))
                    return firstName + " " + lastName;
                else if ((firstName != null) && (firstName.length() > 0))
                    return firstName;
                else if ((lastName != null) && (lastName.length() > 0)) return lastName;
            }
        }

        return null;
    }

    private static void appendString(StringBuffer list, String value, String separator, boolean escape) {
        // escape any separator characters in the string if necessary
        if (escape) value = escapeString(value, "\\" + separator);

        if (value != null) {
            list.append(value);
            list.append(separator);
        }
    }

    /** Escapes the specified characters in the specified string. */
    private static String escapeString(String string, String escapeChars) {
        if ((string == null) || (string.length() <= 0)) return string;

        int stringLen = string.length();
        StringBuffer escapedBuffer = new StringBuffer(stringLen * 2);
        for (int i = 0; i < stringLen; i++) {
            char c = string.charAt(i);

            // replace new-line (CRLF and LF) characters with "\n"
            if ((c == '\r') || (c == '\n')) {
                escapedBuffer.append("\\n");
                if ((c == '\r') && ((i + 1) < stringLen) && (string.charAt(i + 1) == '\n')) i++;
                continue;
            }

            // escape the specified characters
            if (escapeChars.indexOf(c) >= 0) escapedBuffer.append('\\');

            escapedBuffer.append(c);
        }

        return escapedBuffer.toString();
    }

    /** Un-escapes the specified characters in the specified string. */
    private static String unescapeString(String string, String unescapeChars) {
        if ((string == null) || (string.length() <= 0)) return string;

        int stringLen = string.length();
        StringBuffer unescapedBuffer = new StringBuffer(stringLen);
        for (int i = 0; i < stringLen; i++) {
            char c = string.charAt(i);

            // check for escaped characters
            if ((c == '\\') && ((i + 1) < stringLen)) {
                char cNext = string.charAt(i + 1);
                if ((unescapeChars == null) || (unescapeChars.indexOf(cNext) >= 0)) continue;
            }

            unescapedBuffer.append(c);
        }

        return unescapedBuffer.toString();
    }

    /** Writes the specified string to the specified output stream. */
    private void writeString(OutputStream stream, String string) throws StoreException {
        if ((string == null) || (string.length() <= 0)) return;

        try {
            // write the characters encoded in the specified character set
            stream.write(string.getBytes(charset));
        } catch (IOException e) {
            if (logger != null) logger.error("VCARD_ENCODER: Failed to write vCard string", e);

            throw new StoreException("Failed to write vCard string", e);
        }
    }

    /** Reads the next property from the specified vCard data. */
    private int readProperty(String vcardString, int vcardIndex, StringBuffer property) {
        int vcardLen = vcardString.length();
        for (; vcardIndex < vcardLen; vcardIndex++) {
            // check for end-of-line (CR or CRLF)
            char c = vcardString.charAt(vcardIndex);
            if ((c == '\r') || (c == '\n')) {
                // skip past the new line (CR or CRLF)
                if ((c == '\r') && ((vcardIndex + 1) < vcardLen) && (vcardString.charAt(vcardIndex + 1) == '\n')) vcardIndex++;
                vcardIndex++;

                // check for folded lines (CR or CRLF followed by space or tab)
                if ((vcardIndex < vcardLen) && ((vcardString.charAt(vcardIndex) == ' ') || (vcardString.charAt(vcardIndex) == '\t'))) continue;

                // end of line found
                break;
            }

            // replace "\n" or "\N" with new-line (CRLF) characters
            if ((c == '\\') && ((vcardIndex + 1) < vcardLen) && ((vcardString.charAt(vcardIndex + 1) == 'n') || ((vcardString.charAt(vcardIndex + 1) == 'N')))) {
                property.append("\r\n");
                vcardIndex++;
                continue;
            }

            property.append(c);
        }

        return vcardIndex;
    }
}
