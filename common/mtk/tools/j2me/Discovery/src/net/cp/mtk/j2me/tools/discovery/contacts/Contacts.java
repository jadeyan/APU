/**
 * Copyright � 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery.contacts;


import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.pim.Contact;
import javax.microedition.pim.ContactList;
import javax.microedition.pim.PIM;
import javax.microedition.pim.PIMItem;
import javax.microedition.pim.PIMList;

import net.cp.mtk.common.StringUtils;
import net.cp.mtk.j2me.contacts.ContactUtils;
import net.cp.mtk.j2me.tools.discovery.Logger;
import net.cp.mtk.j2me.tools.discovery.DiscoveryMIDlet;


public class Contacts
{
    //indicates if non-standard fields should be ignored when generating contacts
    private static final boolean SKIP_CUSTOM_FIELDS = true;
    
    //the number of test categories to add to each contact list
    private static final int CATEGORY_COUNT =         5;
    
    //the number of test contacts to add to each contact list
    private static final int CONTACT_COUNT =          1;

    
    private static int curContactIndex = 0;
    private static String defaultContactListName = null;
    private static String preferredVcardFormat = null;
    private static Vector contactUids = new Vector();
    
    
    public static void evaluate(DiscoveryMIDlet midlet)
    {
        Logger.log("");
        Logger.log("-----------------------------------");
        Logger.log("CONTACTS (JSR-75):");

        //show all contacts in the PIM
        midlet.setTestStatus("Testing contacts...");
        curContactIndex = 0;
        logContactLists(midlet);

        Logger.log("-----------------------------------");
    }

    public static void closePimList(PIMList list)
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
    

    private Contacts()
    {
        super();
    }
    
    
    private static void logContactLists(DiscoveryMIDlet midlet)
    {
        //make sure the PIM API is supported
        String pimVersion = System.getProperty("microedition.pim.version");
        if ( (pimVersion == null) || (pimVersion.length() <= 0) )
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList: the PIM API (JSR-75) is not supported");
            return;
        }
        
        //make sure we can access the PIM
        PIM pim = PIM.getInstance();
        if (pim == null)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList: could not retrieve PIM instance");
            return;
        }
        
        //check the list of supported vCard formats
        Logger.log("");
        Logger.log("Supported vCard formats:");
        String[] supportedFormats = pim.supportedSerialFormats(PIM.CONTACT_LIST);        
        if ( (supportedFormats != null) && (supportedFormats.length > 0) )
        {
            //examine each reported format
            for (int i = 0; i < supportedFormats.length; i++)
            {
                //make sure the format is valid
                String supportedFormat = supportedFormats[i];
                if ( (supportedFormat == null) || (supportedFormat.length() <= 0) )
                {
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList: supported vCard format ('" + supportedFormat + "') is invalid");
                    continue;
                }
                
                //check if it's one of the common formats
                if ( (! supportedFormat.equalsIgnoreCase("VCARD/2.1")) && (! supportedFormat.equalsIgnoreCase("VCARD/3.0")) )
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList: supported vCard format ('" + supportedFormat + "') is not recognised");
                
                if (preferredVcardFormat == null)
                    preferredVcardFormat = supportedFormat;

                Logger.log("    " + supportedFormat);
            }
        }
        else
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList: PIM doesn't support any vCard formats - a custom encoder/decoder will be required");
            Logger.log("    None.");
        }
        
        //examine the default contact list
        logContactList(midlet, pim, null);
        
        //get the list of all available contact lists
        String[] pimNames = pim.listPIMLists(PIM.CONTACT_LIST);
        if ( (pimNames != null) && (pimNames.length > 0) )
        {
            //examine each reported contact list
            for (int i = 0; i < pimNames.length; i++)
            {
                //make sure the name is valid
                String pimName = pimNames[i];
                if ( (pimName == null) || (pimName.length() <= 0) )
                {
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "PIM list name ('" + pimName + "') is invalid");
                    continue;
                }
    
                if (defaultContactListName != null)
                {
                    if (defaultContactListName.equals(pimName))
                    {
                        //skip the default contact list as we've already examined it
                        continue;
                    }
                    else if (i == 0)
                    {
                        //check that the first name in the list is the name of the default list
                        Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + pimName + "'): the first listed PIM is not the default PIM ('" + defaultContactListName + "')");
                    }
                }
                
                //examine the list
                logContactList(midlet, pim, pimName);
            }
        }
        else
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList: could not retrieve the names of the available contact lists");
        }
    }

    private static void logContactList(DiscoveryMIDlet midlet, PIM pim, String listName)
    {
        boolean isDefault = false;
        if (listName == null)
            isDefault = true;

        //open the specified contact list
        ContactList contactList = null;
        try
        {
            //try to open the list in read-write mode
            if (listName == null)
                contactList = (ContactList)pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_WRITE);
            else
                contactList = (ContactList)pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_WRITE, listName);
        }
        catch (Throwable e)
        {
            try
            {
                //try to open the list in read-only mode
                if (listName == null)
                {
                    contactList = (ContactList)pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY);
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): default list can only be opened in read-only mode", e);
                }
                else
                {
                    contactList = (ContactList)pim.openPIMList(PIM.CONTACT_LIST, PIM.READ_ONLY, listName);
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + listName + "'): list can only be opened in read-only mode", e);
                }
            }
            catch (Throwable e1)
            {
                if (listName == null)
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList (default): list could not be opened", e1);
                else
                    Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + listName + "'): list could not be opened", e1);
                return;
            }
        }
        
        try
        {
            if (isDefault)
                defaultContactListName = contactList.getName();

            //examine the list
            logContactList(midlet, contactList, isDefault);
        }
        finally
        {
            closePimList(contactList);
        }
    }
    
    private static void logContactList(DiscoveryMIDlet midlet, ContactList contactList, boolean isDefault)
    {
        String listName = contactList.getName();
        if ( (listName == null) || (listName.length() <= 0) )
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + listName + "'): list name is invalid");
            listName = null;
        }

        Logger.log("");
        Logger.log("Contact List ('" + listName + "'):");
        Logger.log("    Default list: " + isDefault);

        //show list of supported fields
        Logger.log("    Supported Fields:");
        int supportedFieldsCount = logSupportedFields(contactList);
        if (supportedFieldsCount <= 0)
            Logger.log("        None");
        
        //add some new categories (if we can)
        Vector addedCategories = addCategories(contactList, CATEGORY_COUNT);

        //show current list of categories
        Logger.log("    Categories:");
        logCategories(contactList);
        
        //add some new contacts
        Vector addedVcards = null;
        Vector addedContacts = addContacts(contactList, 1, CONTACT_COUNT, addedCategories);
        if (addedContacts != null)
        {
            //grab the vCards of the added contacts so we can test vCard encoding/decoding later
            addedVcards = new Vector(addedContacts.size());
            for (int i = 0; i < addedContacts.size(); i++)
            {
                Contact addedContact = (Contact)addedContacts.elementAt(i);
                String addedVcard = contactToVcard(addedContact);
                if ( (addedVcard != null) && (addedVcard.length() > 0) )
                    addedVcards.addElement(addedVcard);
            }
        }

        //modify each contact
        modifyContacts(contactList, addedContacts);
        
        //import some contacts from vCards
        Vector importedContacts = null;
        if (addedVcards != null)
            importedContacts = importContacts(contactList, addedVcards);
        
        //show current list of contacts
        Logger.log("    Contacts:");
        logContacts(midlet, contactList, addedContacts, addedCategories);
        
        //remove all the contacts we added
        removeContacts(contactList, addedContacts);
        removeContacts(contactList, importedContacts);
        
        //remove all the categories we added
        removeCategories(contactList, addedCategories);
    
        //check for reused UIDs
        if (contactUids.size() > 0)
        {
            //add some new contacts
            addedContacts = addContacts(contactList, 1, CONTACT_COUNT, null);
            if (addedContacts != null)
            {
                for (int i = 0; i < addedContacts.size(); i++)
                {
                    Contact contact = (Contact)addedContacts.elementAt(i);
                    if (contact.countValues(Contact.UID) != 1)
                        continue;
                        
                    String uid = contact.getString(Contact.UID, 0);
                    if ( (uid == null) || (uid.length() <= 0) )
                        continue;

                    //make sure we haven't seen this UID before
                    if (contactUids.contains(uid))
                        Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): value of field ('UID') is not unique ('" + uid + "') - the UIDs of previous/existing contacts are reused");
                }

                //cleanup
                removeContacts(contactList, addedContacts);
            }
        }
    }
    
    private static int logSupportedFields(ContactList contactList)
    {
        try
        {
            //get the list of supported fields
            int[] supportedFields = contactList.getSupportedFields();
            if ( (supportedFields == null) || (supportedFields.length <= 0) )
            {
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): list doesn't support any fields");
                return 0;
            }
            
            //examine each supported field
            boolean uidSupported = false;
            boolean revisionSupported = false;
            boolean nameSupported = false;
            boolean telSupported = false;
            Logger.log("        [" + DiscoveryMIDlet.pad("ID(Label)", 20) + "] [" + DiscoveryMIDlet.pad("Data Type", 11) + "] [" + DiscoveryMIDlet.pad("Max Size", 8) + "] [Attributes] [Elements]");
            for (int i = 0; i < supportedFields.length; i++)
            {
                //check for some standard fields
                int fieldId = supportedFields[i];
                if ( (fieldId == Contact.NAME) || (fieldId == Contact.FORMATTED_NAME) )
                    nameSupported = true;
                else if (fieldId == Contact.TEL)
                    telSupported = true;
                else if (fieldId == Contact.UID)
                    uidSupported = true;
                else if (fieldId == Contact.REVISION)
                    revisionSupported = true;

                //log the fields details
                logSupportedField(contactList, fieldId);
            }
            
            //check if the expected fields are supported
            if (! nameSupported)
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): list doesn't support the 'NAME' or 'FORMATTED_NAME' fields");
            if (! telSupported)
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): list doesn't support the 'TELEPHONE' field");
            if (! uidSupported)
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): list doesn't support the 'UID' field");
            if (! revisionSupported)
                Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): list doesn't support the 'REVISION' field");
            
            return supportedFields.length;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed to get supported fields", e);
            return 0;
        }
    }

    private static void logSupportedField(ContactList contactList, int fieldId)
    {
        String info = "";
        try
        {
            if (fieldId < 0)
                Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): list may have an invalid supported field ID ('" + fieldId + "')");
            
            //make sure the field is supported (it should be)
            info = "isSupportedField";
            if (! contactList.isSupportedField(fieldId))
            {
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldId + "') is reported as not being supported");
                return;
            }

            //check if the field has a localized label
            info = "getFieldLabel";
            String fieldLabel = contactList.getFieldLabel(fieldId);
            if ( (fieldLabel == null) || (fieldLabel.length() <= 0) )
            {
                Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): field ('" + fieldId + "') has no localized label ('" + fieldLabel + "')");
                fieldLabel = Integer.toString(fieldId);
            }
            else
            {
                fieldLabel = Integer.toString(fieldId) + "(" + fieldLabel + ")";
            }
            
            //check if the field is a recognised one
            String fieldName = ContactUtils.fieldIdToString(fieldId);
            if (fieldName == null)
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') is a non-standard field");
            else 
                fieldLabel = fieldName;
            
            //get the max number of values the field can have
            info = "maxValues";
            int maxFieldValues = contactList.maxValues(fieldId);
            if ( (maxFieldValues == 0) || (maxFieldValues < -1) )
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') has an invalid max values count (" + maxFieldValues + ")");
            
            //determine the data type of the field
            info = "getFieldDataType";
            int dataType = contactList.getFieldDataType(fieldId);
            String dataTypeString = ContactUtils.datatypeToString(dataType);
            if (dataTypeString == null)
            {
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') has an unrecognised data type ('" + dataType + "')");
                dataTypeString = Integer.toString(dataType);
            }

            //perform some additional checking for structured fields
            StringBuffer elementsBuffer = new StringBuffer();
            if (dataType == PIMItem.STRING_ARRAY)
            {
                info = "stringArraySize";
                int elementCount = contactList.stringArraySize(fieldId);
                if (elementCount <= 0)
                    Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') has an invalid number of elements (" + elementCount + ")");
                
                //determine the supported elements of the structured field
                info = "getSupportedArrayElements";
                int[] elementIds = contactList.getSupportedArrayElements(fieldId);
                if ( (elementIds == null) || (elementIds.length <= 0) )
                    Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') has no supported elements");
                else if (elementIds.length != elementCount)
                    Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') only supports " + elementIds.length + " out of " + elementCount + " elements");
                        
                if ( (elementIds != null) && (elementIds.length > 0) )
                {
                    //examine each element of the structured field 
                    for (int j = 0; j < elementIds.length; j++)
                    {
                        int elementId = elementIds[j];
                        if ( (elementId < 0) || (elementId > elementCount) )
                        {
                            Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') may have an invalid supported element ID ('" + elementId + "')");
                            continue;
                        }
                        
                        //make sure the element is supported (it should be)
                        info = "isSupportedArrayElement(" + elementId + ")";
                        if (! contactList.isSupportedArrayElement(fieldId, elementId))
                        {
                            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') element ID ('" + elementId + "') is reported as not being supported");
                            continue;
                        }
                        
                        //get the element label
                        info = "getArrayElementLabel(" + elementId + ")";
                        String elementLabel = contactList.getArrayElementLabel(fieldId, elementId);
                        if ( (elementLabel == null) || (elementLabel.length() <= 0) )
                        {
                            Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') element ID ('" + elementId + "') has no localized label ('" + elementLabel + "')");
                            elementLabel = Integer.toString(elementId);
                        }
                        else
                        {
                            elementLabel = Integer.toString(elementId) + "(" + elementLabel + ")";
                        }
                        
                        //check if the element is a recognised one
                        String elementName = ContactUtils.elementIdToString(fieldId, elementId);
                        if (elementName == null)
                            Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): structured field ('" + fieldLabel + "') supports a non-standard element ('" + elementLabel + "')");
                        else
                            elementLabel = elementName;
                        
                        if (elementsBuffer.length() > 0)
                            elementsBuffer.append(", ");
                        elementsBuffer.append(elementLabel);
                    }
                }
            }
            
            //examine each supported attribute
            StringBuffer attributesBuffer = new StringBuffer();
            info = "getSupportedAttributes";
            int[] supportedAttrs = contactList.getSupportedAttributes(fieldId);
            if ( (supportedAttrs != null) && (supportedAttrs.length > 0) )
            {
                for (int j = 0; j < supportedAttrs.length; j++)
                {
                    int attrId = supportedAttrs[j];
                    if (attrId < 0)
                    {
                        Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') may have an invalid supported attribute ('" + attrId + "')");
                        continue;
                    }
                    
                    //make sure the attribute is supported (it should be)
                    info = "isSupportedAttribute(" + attrId + ")";
                    if (! contactList.isSupportedAttribute(fieldId, attrId))
                    {
                        Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') attribute ('" + attrId + "') is reported as not being supported");
                        continue;
                    }
                    
                    //get the attribute label
                    info = "getAttributeLabel(" + attrId + ")";
                    String attrLabel = contactList.getAttributeLabel(attrId);
                    if ( (attrLabel == null) || (attrLabel.length() <= 0) )
                    {
                        Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') attribute ('" + attrId + "') has no localized label ('" + attrLabel + "')");
                        attrLabel = Integer.toString(attrId);
                    }
                    else
                    {
                        attrLabel = Integer.toString(attrId) + "(" + attrLabel + ")";
                    }
                    
                    //check if the attribute is a recognised one
                    String attrName = ContactUtils.attributeToString(attrId);
                    if (attrName == null)
                        Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') attribute ('" + attrLabel + "') is a non-standard attribute");
                    else
                        attrLabel = attrName;
                    
                    if (attributesBuffer.length() > 0)
                        attributesBuffer.append(", ");
                    attributesBuffer.append(attrLabel);
                }
            }

            //log the field details
            String label = DiscoveryMIDlet.pad(fieldLabel, 20);
            String type = DiscoveryMIDlet.pad(dataTypeString, 11);
            String maxValues = (maxFieldValues >= 0) ? DiscoveryMIDlet.pad(Integer.toString(maxFieldValues), 8) : "No Limit";
            String attrs = (attributesBuffer.length() > 0) ? attributesBuffer.toString() : "No Attributes";
            String elements = (elementsBuffer.length() > 0) ? elementsBuffer.toString() : "No Elements";
            if (dataType == PIMItem.STRING_ARRAY)
                Logger.log("        [" + label + "] [" + type + "] [" + maxValues + "] [" + attrs + "] [" + elements + "]");
            else
                Logger.log("        [" + label + "] [" + type + "] [" + maxValues + "] [" + attrs + "] [N/A]");
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed (at '" + info + "') to get details for field ('" + fieldId + "')", e);
        }
    }
    
    
    private static int logCategories(ContactList contactList)
    {
        try
        {
            //check if categories are supported
            int maxCategories = contactList.maxCategories();
            if (maxCategories == 0)
            {
                Logger.log("        Supported: No");
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): list doesn't support categories");
                return 0;
            }

            Logger.log("        Supported: Yes");
            if (maxCategories == -1)
                Logger.log("        Max Categories: No Limit");
            else
                Logger.log("        Max Categories: " + maxCategories);
            if (maxCategories < -1)
                Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): list has an invalid max categories value (" + maxCategories + ")");
            
            //get the current list of categories
            Logger.log("        Categories:");
            String[] categories = contactList.getCategories();
            if ( (categories == null) || (categories.length <= 0) )
            {
                Logger.log("            None.");
                return 0;
            }
            
            for (int i = 0; i < categories.length; i++)
            {
                String category = categories[i];
                if ( (category == null) || (category.length() <= 0) )
                    Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): list may have an invalid category ('" + category + "')");
                
                //make sure it's a category (it should be)
                if (! contactList.isCategory(category))
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): category ('" + category + "') is reported as not being a category");

                //log the category
                Logger.log("            [" + category + "]");
            }
            
            return categories.length;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed to get list of categories", e);
            return 0;
        }
    }
    
    private static Vector addCategories(ContactList contactList, int count)
    {
        try
        {
            //check if categories are supported
            int maxCategories = contactList.maxCategories();
            if (maxCategories == 0)
                return null;

            //check how many categories are already present
            String[] currentCategories = contactList.getCategories();
            if (currentCategories != null)
                count = count - currentCategories.length;
            if (count <= 0)
                return null;
                
            //add the required number of test categories
            Vector addedCategories = new Vector();
            for (int i = 0; i < count; i++)
            {
                String category = "TestCategory" + i;
                try
                {
                    contactList.addCategory(category);
                    if (! contactList.isCategory(category))
                        Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): added category ('" + category + "') is reported as not being a category - category may not have been added");

                    addedCategories.addElement(category);
                }
                catch (Throwable e)
                {
                    Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): failed to add category ('" + category + "')", e);
                }
            }
            
            return addedCategories;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): failed to add categories", e);
            return null;
        }
    }

    private static void removeCategories(ContactList contactList, Vector categories)
    {
        if ( (categories == null) || (categories.size() <= 0) )
            return;
        
        for (int i = 0; i < categories.size(); i++)
        {
            String category = (String)categories.elementAt(i);
            try
            {
                contactList.deleteCategory(category, false);
                if (contactList.isCategory(category))
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): removed category ('" + category + "') is still reported as being a category - category may not have been removed");
            }
            catch (Throwable e)
            {
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): failed to remove category ('" + category + "')", e);
            }
        }
    }

    private static void logContacts(DiscoveryMIDlet midlet, ContactList contactList, Vector addedContacts, Vector addedCategories)
    {
        try
        {
            //get all available contacts
            Enumeration contactEnum = contactList.items();
            if (contactEnum == null)
            {
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): could not retrieve contact list items");
                return;
            }

            int contactIndex = 0;
            if (contactEnum.hasMoreElements())
            {
                //log each contact
                Logger.log("            [" + DiscoveryMIDlet.pad("Field", 20) + "] [" + DiscoveryMIDlet.pad("Attributes", 20) + "]: Value");
                while (contactEnum.hasMoreElements())
                {
                    Contact contact = (Contact)contactEnum.nextElement();
                    curContactIndex++;
                    midlet.setTestStatus("Testing contact " + curContactIndex + "...");

                    //log the contact
                    logContact(contact, contactIndex++);
                    
                    //if this contact was just added by us, compare it to what we added 
                    Contact addedContact = findContact(addedContacts, contact);
                    if (addedContact != null)
                        checkAddedContact(addedContact, contact, addedCategories);
                }
            }
            else
            {
                Logger.log("        None");
            }
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed to retrieve contact list items", e);
        }
    }
    
    private static void logContact(Contact contact, int contactIndex)
    {
        ContactList contactList = (ContactList)contact.getPIMList();
        Logger.log("        " + (contactIndex+1) + ". Contact (" + contact + "):");

        try
        {
            //get each field and log each one
            int[] fieldIds = contact.getFields();
            for (int i = 0; i < fieldIds.length; i++)
                logContactField(contact, contactIndex, fieldIds[i]);
            
            //show the categories that the contact belongs to
            logContactCategories(contact);
            
            //show the contact vCard
            logContactVcard(contact);
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed to get the fields of contact ('" + contact + "')", e);
        }    
    }
    
    private static void logContactField(Contact contact, int contactIndex, int fieldId)
    {
        ContactList contactList = (ContactList)contact.getPIMList();

        String info = "";
        try
        {
            //get the field details
            String fieldLabel = getFieldLabel(contactList, fieldId);
            
            //log each field value
            info = "countValues";
            int fieldDataType = contactList.getFieldDataType(fieldId);
            int valueCount = contact.countValues(fieldId);
            if (valueCount <= 0)
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') in contact ('" + contact + "') should have more than " + valueCount + " values");
            for (int valueIndex = 0; valueIndex < valueCount; valueIndex++)
            {
                //get the value of the field
                StringBuffer value = new StringBuffer();
                if (fieldDataType == PIMItem.STRING)
                {
                    info = "getString(" + fieldId + ", " + valueIndex + ")";
                    value.append( contact.getString(fieldId, valueIndex) );
                }
                else if (fieldDataType == PIMItem.STRING_ARRAY)
                {
                    info = "getStringArray(" + fieldId + ", " + valueIndex + ")";
                    String[] values = contact.getStringArray(fieldId, valueIndex);
                    for (int j = 0; j < values.length; j++)
                    {
                        value.append("[");
                        value.append(values[j]);
                        value.append("] ");
                    }
                }
                else if (fieldDataType == PIMItem.INT)
                {
                    info = "getInt(" + fieldId + ", " + valueIndex + ")";
                    value.append( contact.getInt(fieldId, valueIndex) );
                }
                else if (fieldDataType == PIMItem.DATE)
                {
                    info = "getDate(" + fieldId + ", " + valueIndex + ")";
                    long date = contact.getDate(fieldId, valueIndex);
                    value.append( DiscoveryMIDlet.dateToString(date) );
                }
                else if (fieldDataType == PIMItem.BOOLEAN)
                {
                    info = "getBoolean(" + fieldId + ", " + valueIndex + ")";
                    value.append( contact.getBoolean(fieldId, valueIndex) );
                }
                else if (fieldDataType == PIMItem.BINARY)
                {
                    info = "getBinary(" + fieldId + ", " + valueIndex + ")";
                    byte[] data = contact.getBinary(fieldId, valueIndex);
                    value.append("binary[");
                    value.append(data.length);
                    value.append("]");
                }

                info = "getAttributes(" + fieldId + ", " + valueIndex + ")";
                String label = DiscoveryMIDlet.pad(fieldLabel, 20);
                String attributes = DiscoveryMIDlet.pad(attributesToString(contactList, fieldId, contact.getAttributes(fieldId, valueIndex) ), 20);
                Logger.log("            [" + label + "] [" + attributes + "]: " + value);
            }
            
            //check the value of the revision field
            if (fieldId == Contact.REVISION)
            {
                //check the data type
                if (fieldDataType == PIMItem.DATE)
                {
                    //make sure the revision time is valid
                    if (valueCount == 1)
                    {
                        long revisionTime = contact.getDate(Contact.REVISION, 0);
                        if (revisionTime > 0)
                        {
                            //check the granularity of the revision time
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(new Date(revisionTime));
                            int hour = cal.get(Calendar.HOUR_OF_DAY);
                            int minute = cal.get(Calendar.MINUTE);
                            int second = cal.get(Calendar.SECOND);
                            int millsecond = cal.get(Calendar.MILLISECOND);
                            if ( (hour <= 0) && (minute <= 0) && (second <= 0) && (millsecond <= 0) )
                                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') in contact ('" + contact + "') doesn't have time granularity (only date)");
                            else if (second <= 0)
                                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') in contact ('" + contact + "') doesn't have second granularity");
                            else if (millsecond <= 0)
                                Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') in contact ('" + contact + "') doesn't have millisecond granularity");
                        }
                        else
                        {
                            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') in contact ('" + contact + "') is invalid ('" + revisionTime + "')");
                        }
                    }
                    else
                    {
                        Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') in contact ('" + contact + "') should only have exactly 1 value (it actually has " + valueCount + " values)");
                    }
                }
                else
                {
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') has an incorrect data type ('" + fieldDataType + "') - it should be a DATE");
                }
            }
            
            //check the value of the UID field
            if (fieldId == Contact.UID)
            {
                //check the data type
                if (fieldDataType == PIMItem.STRING)
                {
                    //make sure the UID value is valid
                    if (valueCount == 1)
                    {
                        String uid = contact.getString(Contact.UID, 0);
                        if ( (uid == null) || (uid.length() <= 0) )
                            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') in contact ('" + contact + "') is invalid ('" + uid + "')");
                        else if (contact.toString().indexOf(uid) >= 0)
                            Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') in contact ('" + contact + "') might be based on the contacts memory address (actual UID is '" + uid + "')");

                        //check for duplicates
                        if (contactUids.contains(uid))
                            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): value of field ('" + fieldLabel + "') is not unique ('" + uid + "')");
                        else
                            contactUids.addElement(uid);
                    }
                    else
                    {
                        Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') in contact ('" + contact + "') should only have exactly 1 value (it actually has " + valueCount + " values)");
                    }
                }
                else
                {
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') has an incorrect data type ('" + fieldDataType + "') - it should be a STRING");
                }
            }
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed (at '" + info + "') to get details of contact ('" + contact + "')", e);
        }    
    }
    
    private static void logContactCategories(Contact contact)
    {
        try
        {
            //get the list of categories the contact belongs to
            Logger.log("            Categories:");
            String[] categories = contact.getCategories();
            if ( (categories == null) || (categories.length <= 0) )
            {
                Logger.log("                None");
                return;
            }
            
            for (int i = 0; i < categories.length; i++)
                Logger.log("                " + categories[i]);
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contact.getPIMList().getName() + "'): failed to get the associated categories of contact ('" + contact + "')", e);
        }
    }
    
    private static void logContactVcard(Contact contact)
    {
        String vCard = contactToVcard(contact);
        if ( (vCard == null) || (vCard.length() <= 0) )
            return;
        
        Logger.log("            vCard:");
        Logger.log( StringUtils.prefixLines(vCard, "                ") );
    }

    private static Vector getContacts(ContactList contactList, Contact searchContact)
    {
        try
        {
            //get all matching contacts
            Vector matchingContacts = new Vector();
            Enumeration contactEnum = contactList.items(searchContact);
            if (contactEnum == null)
            {
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): could not retrieve matching contacts");
                return null;
            }

            while (contactEnum.hasMoreElements())
            {
                matchingContacts.addElement( contactEnum.nextElement() );
            }

            return matchingContacts;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): failed to retrieve matching contacts", e);
            return null;
        }
    }

    private static Vector addContacts(ContactList contactList, int startIndex, int contactCount, Vector categories)
    {
        Vector addedContacts = new Vector();
        for (int i = 0; i < contactCount; i++)
        {
            Contact contact = addContact(contactList, i, categories);
            if (contact == null)
                continue;
            
            addedContacts.addElement(contact);
        }
        
        return addedContacts;
    }

    private static Contact addContact(ContactList contactList, int contactIndex, Vector categories)
    {
        try
        {
            //create a new contact
            Contact contact = createContact(contactList, contactIndex, categories);
            if (contact == null)
                return null;
            
            //commit it to the contact list
            if (! testCommitContact(contactList, contact))
                return null;
            
            //check if the contact has been modified (it shouldn't be)
            if (contact.isModified())
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): newly committed contact ('" + contact + "') is reported as being modified)");

            //try to read it back
            Vector matchingContacts = getContacts(contactList, contact);
            if (matchingContacts != null)
            {
                //check if we found a match
                int matchingContactsCount = matchingContacts.size();
                if (matchingContactsCount <= 0)
                {
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): could not find any contacts (using 'ContactList.items(contact)') that match the newly added contact ('" +  contact + "') - some contact data may not have been committed");
                }
                else if (matchingContactsCount > 1)
                {
                    Logger.logIssue(Logger.SEVERITY_LOW, "ContactList ('" + contactList.getName() + "'): found " + matchingContactsCount + " contacts (using 'ContactList.items(contact)') that match the newly added contact ('" +  contact + "')");
                }
            }

            return contact;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed to add contact", e);
            return null;
        }
    }

    private static void modifyContacts(ContactList contactList, Vector contacts)
    {
        if (contacts == null)
            return;

        for (int i = 0; i < contacts.size(); i++)
        {
            Contact contact = (Contact)contacts.elementAt(i);
            if (contact == null)
                continue;
            
            //modify the contact, checking the revision field of the first contact only
            modifyContact(contactList, contact, (i+1), (i==0));
        }
    }
    
    private static void modifyContact(ContactList contactList, Contact contact, int contactIndex, boolean checkRevisionField)
    {
        String info = "";
        try
        {
            //check the last revision time (if we haven't already tested this)
            long oldRevisionTime = 0;
            if ( (checkRevisionField) && (contactList.isSupportedField(Contact.REVISION)) && (contact.countValues(Contact.REVISION) == 1) )
            {
                oldRevisionTime = contact.getDate(Contact.REVISION, 0);
                    
                try
                {
                    //wait for a while before modifying the contact, so we can check if the revision time is updated
                    Thread.sleep(2000);
                }
                catch (Throwable e)
                {
                    //ignore
                }
            }
            
            //modify each supported field
            info = "getFields";
            int[] fieldIds = contact.getFields();
            for (int i = 0; i < fieldIds.length; i++)
            {
                int fieldId = fieldIds[i];

                //modify each value
                info = "countValues(" + fieldId + ")";
                int valueCount = contact.countValues(fieldId);
                for (int valueIndex = 0; valueIndex < valueCount; valueIndex++)
                {
                    info = "getAttributes(" + fieldId + ", " + valueIndex + ")";
                    int attributes = contact.getAttributes(fieldId, valueIndex);
                    setContactField(contactList, contact, fieldId, attributes, contactIndex, valueIndex, false);
                }
            }
            
            //remove the contact from all but the first category
            info = "getCategories";
            String[] categories = contact.getCategories();
            if (categories != null)
            {
                for (int i = 1; i < categories.length; i++)
                {
                    info = "removeFromCategory(" + categories[i] + ")";
                    contact.removeFromCategory( categories[i] );
                }
            }
            
            //check if the contact has been modified (it should be)
            info = "isModified";
            if (! contact.isModified())
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): modified contact ('" + contact + "') is reported as not being modified");
            
            //commit it to the contact list
            if (! testCommitContact(contactList, contact))
                return;
            
            //check if the contact has been modified (it shouldn't be)
            if (contact.isModified())
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): newly committed contact ('" + contact + "') is still reported as being modified");
            
            //check that the last revision time has changed (if we haven't already tested this)
            info = "countValues";
            if ( (checkRevisionField) && (oldRevisionTime > 0) && (contact.countValues(Contact.REVISION) == 1) )
            {
                info = "getDate(Contact.REVISION, 0)";
                long newRevisionTime = contact.getDate(Contact.REVISION, 0);
                if (newRevisionTime == oldRevisionTime)
                    Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): contact revision field ('" + Contact.REVISION + "') isn't updated when the contact is modified");
            }
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed (at '" + info + "') to modify contact ('" + contact + "')", e);
        }              
    }
    
    private static Vector importContacts(ContactList contactList, Vector vcards)
    {
        if ( (vcards == null) || (vcards.size() <= 0) )
            return null;
        
        Vector importedContacts = new Vector(vcards.size());
        for (int i = 0; i < vcards.size(); i++)
        {
            String vcard = (String)vcards.elementAt(i);
            Contact contact = importContact(contactList, vcard);
            if (contact != null)
                importedContacts.addElement(contact);
        }
     
        return importedContacts;
    }

    private static Contact importContact(ContactList contactList, String vcard)
    {
        //decode the cVard into a contact
        Contact contact = vcardToContact(contactList, vcard);
        if (contact == null)
            return null;
        
        try
        {
            //import the contact
            Contact importedContact = contactList.importContact(contact);
            if (importedContact == null)
            {
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): could not import contact ('" + contact + "')");
                return null;
            }
            
            //commit the contact
            if (! testCommitContact(contactList, importedContact))
                return null;

            return importedContact;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed to import contact ('" + contact + "')", e);
            return null;
        }
    }

    private static Contact createContact(ContactList contactList, int contactIndex, Vector categories)
    {
        String info = "";
        try
        {
            info = "createContact";
            Contact contact = contactList.createContact();

            //populate each supported field
            info = "getSupportedFields";
            int[] supportedFields = contactList.getSupportedFields();
            for (int i = 0; i < supportedFields.length; i++)
            {
                int fieldId = supportedFields[i];

                //make sure we don't exceed the maximum number of allowed values for the field
                info = "maxValues(" + fieldId + ")";
                int maxFieldValues = contactList.maxValues(fieldId);
                if (maxFieldValues == -1)
                    maxFieldValues = Integer.MAX_VALUE;

                //add one value for each supported attribute (up to the maximum number of allowed values)
                info = "getSupportedAttributes(" + fieldId + ")";
                int[] supportedAttrs = contactList.getSupportedAttributes(fieldId);
                if ( (supportedAttrs != null) && (supportedAttrs.length > 0) )
                {
                    //add a value for each supported attribute
                    for (int j = 0; (j < supportedAttrs.length) && (j < maxFieldValues); j++)
                        setContactField(contactList, contact, fieldId, supportedAttrs[j], contactIndex, j, true);
                }
                else
                {
                    //add a single value (if allowed)
                    if (maxFieldValues > 0)
                        setContactField(contactList, contact, fieldId, PIMItem.ATTR_NONE, contactIndex, 0, true);
                }
            }
            
            //add the contact to the test categories (if there are any)
            info = "maxCategories";
            int maxCategories = contact.maxCategories();
            if ( (categories != null) && ((maxCategories > 0) || (maxCategories == -1)) )
            {
                //determine how many categories the contact can belong to
                int categoriesToAdd = categories.size();
                info = "getCategories";
                String[] curCategories = contact.getCategories();
                if ( (curCategories != null) && (curCategories.length > 0) )
                    categoriesToAdd = categoriesToAdd - curCategories.length;
                if ( (maxCategories > 0) && (categoriesToAdd > maxCategories) )
                    categoriesToAdd = maxCategories;
                
                for (int i = 0; i < categoriesToAdd; i++)
                {
                    String category = (String)categories.elementAt(i);
                    info = "addToCategory(" + category + ")";
                    contact.addToCategory(category);
                }
            }
            
            return contact;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed (at '" + info + "') to create contact", e);
            return null;
        }              
    }

    private static boolean setContactField(ContactList contactList, Contact contact, int fieldId, int attributeId, int contactIndex, int valueIndex, boolean add)
    {
        //skip these fields as they shouldn't be modified
        if ( (fieldId == Contact.REVISION) || (fieldId == Contact.UID) || (fieldId == Contact.PHOTO_URL) || 
             (fieldId == Contact.PUBLIC_KEY) || (fieldId == Contact.PUBLIC_KEY_STRING) )
        {
            return false;
        }
        
        //skip non-standard fields (if required)
        if ( (SKIP_CUSTOM_FIELDS) && (! ContactUtils.isStandardField(fieldId)) )
            return false;

        //skip the "preferred" attribute
        if (attributeId == Contact.ATTR_PREFERRED)
            return false;

        String info = "";
        String fieldLabel = null;
        try
        {
            fieldLabel = getFieldLabel(contactList, fieldId);
            String contactIndexStr = DiscoveryMIDlet.pad(contactIndex, 5, '0');
            String valueIndexStr = DiscoveryMIDlet.pad(valueIndex, 2, '0');

            info = "getFieldDataType(" + fieldId + ")";
            int fieldDataType = contactList.getFieldDataType(fieldId);
            if (fieldDataType == PIMItem.STRING)
            {
                //generate valid values for the field
                String value = null;
                if (fieldId == Contact.EMAIL)
                    value = (add) ? (contactIndexStr + "." + valueIndexStr + "@acme.com") : (contactIndexStr + "." + valueIndexStr + "@mod.acme.com");
                else if (fieldId == Contact.FORMATTED_ADDR)
                    value = (add) ? ("Apartment " + valueIndexStr + ", Street " + contactIndexStr + ", Ireland") : ("Apartment " + valueIndexStr + ", Street " + contactIndexStr + ", Ireland, Mod");
                else if (fieldId == Contact.FORMATTED_NAME)
                    value = (add) ? (contactIndexStr + " " + valueIndexStr) : (contactIndexStr + " " + valueIndexStr + " Mod");
                else if (fieldId == Contact.NICKNAME)
                    value = (add) ? (contactIndexStr + " " + valueIndexStr) : (contactIndexStr + " " + valueIndexStr + " Mod");
                else if (fieldId == Contact.NOTE)
                    value = (add) ? ("This is contact " + contactIndexStr + ", value " + valueIndexStr) : ("This is contact " + contactIndexStr + ", value " + valueIndexStr + ", modified");
                else if (fieldId == Contact.ORG)
                    value = (add) ? ("CP (" + contactIndexStr + "," + valueIndexStr + ")") : ("CP (" + contactIndexStr + "," + valueIndexStr + ",Mod)");
                else if (fieldId == Contact.TEL)
                    value = (add) ? (contactIndexStr + valueIndexStr) : (contactIndexStr + valueIndexStr + "99");
                else if (fieldId == Contact.TITLE)
                    value = (add) ? ("Software Eng. (" + contactIndexStr + "," + valueIndexStr + ")") : ("Software Eng. (" + contactIndexStr + "," + valueIndexStr + ",Mod)");
                else if (fieldId == Contact.URL)
                    value = (add) ? ("http://foo.acme.com/" + contactIndexStr + "/" + valueIndexStr) : ("http://foo.acme.com/" + contactIndexStr + "/" + valueIndexStr + "/Mod");
                else
                    value = (add) ? (contactIndexStr + "-" + valueIndexStr) : (contactIndexStr + "-" + valueIndexStr + "-99");

                if (add)
                {
                    info = "addString(" + fieldId + ", " + attributeId + ")";
                    contact.addString(fieldId, attributeId, value);
                }
                else
                {
                    info = "setString(" + fieldId + ", " + valueIndex + ", " + attributeId + ")";
                    contact.setString(fieldId, valueIndex, attributeId, value);
                }
            }
            else if (fieldDataType == PIMItem.STRING_ARRAY)
            {
                //generate valid values for the field
                info = "stringArraySize(" + fieldId + ")";
                String[] values = new String[ contactList.stringArraySize(fieldId) ];
                for (int elementId = 0; elementId < values.length; elementId++)
                {
                    info = "isSupportedArrayElement(" + fieldId + ", " + elementId + ")";
                    if (! contactList.isSupportedArrayElement(fieldId, elementId))
                        continue;
                    
                    String value = null;
                    if (fieldId == Contact.ADDR)
                    {
                        if (elementId == Contact.ADDR_COUNTRY)
                            value = (add) ? ("Ireland-" + valueIndexStr) : ("Ireland-" + valueIndexStr + " (Mod)");
                        else if (elementId == Contact.ADDR_EXTRA)
                            value = (add) ? ("Extra-" + valueIndexStr) : ("Extra-" + valueIndexStr + " (Mod)");
                        else if (elementId == Contact.ADDR_LOCALITY)
                            value = (add) ? ("Dublin-" + valueIndexStr) : ("Dublin-" + valueIndexStr + " (Mod)");
                        else if (elementId == Contact.ADDR_POBOX)
                            value = (add) ? ("POBox-" + valueIndexStr) : ("POBox-" + valueIndexStr + " (Mod)");
                        else if (elementId == Contact.ADDR_POSTALCODE)
                            value = (add) ? ("90210") : ("90211");
                        else if (elementId == Contact.ADDR_REGION)
                            value = (add) ? ("Leinster-" + valueIndexStr) : ("Leinster-" + valueIndexStr + " (Mod)");
                        else if (elementId == Contact.ADDR_STREET)
                            value = (add) ? ("Street-" + valueIndexStr) : ("Street-" + valueIndexStr + " (Mod)");
                    }
                    else if (fieldId == Contact.NAME)
                    {
                        if (elementId == Contact.NAME_FAMILY)
                            value = (add) ? (contactIndexStr) : (contactIndexStr + "-Mod");
                        else if (elementId == Contact.NAME_GIVEN)
                            value = (add) ? (valueIndexStr) : (valueIndexStr + "-Mod");
                        else if (elementId == Contact.NAME_OTHER)
                            value = (add) ? (valueIndex + "") : (valueIndex + "-Mod");
                        else if (elementId == Contact.NAME_PREFIX)
                            value = (add) ? ("Mr.") : ("Dr.");
                        else if (elementId == Contact.NAME_SUFFIX)
                            value = (add) ? ("BSc.") : ("PhD.");
                    }
                    else
                    {
                        value = (add) ? (contactIndexStr + "-" + valueIndexStr) : (contactIndexStr + "-" + valueIndexStr + "-99");
                    }

                    values[elementId] = value;
                    //Logger.log("DE: " + fieldLabel + "[" + elementId + "] = " + values[elementId]);
                }

                if (add)
                {
                    info = "addStringArray(" + fieldId + ", " + attributeId + ")";
                    contact.addStringArray(fieldId, attributeId, values);
                }
                else
                {
                    info = "setStringArray(" + fieldId + ", " + valueIndex + ", " + attributeId + ")";
                    contact.setStringArray(fieldId, valueIndex, attributeId, values);
                }
            }
            else if (fieldDataType == PIMItem.INT)
            {
                //generate valid values for the field
                int value = 0;
                if (fieldId == Contact.CLASS)
                    value = (add) ? (Contact.CLASS_CONFIDENTIAL) : (Contact.CLASS_PUBLIC);
                else
                    value = (add) ? (0) : (1);

                if (add)
                {
                    info = "addInt(" + fieldId + ", " + attributeId + ")";
                    contact.addInt(fieldId, attributeId, value);
                }
                else
                {
                    info = "setInt(" + fieldId + ", " + valueIndex + ", " + attributeId + ")";
                    contact.setInt(fieldId, valueIndex, attributeId, value);
                }
            }
            else if (fieldDataType == PIMItem.DATE)
            {
                //generate valid values for the field
                long value = 0;
                if (fieldId == Contact.BIRTHDAY)
                    value = (add) ? (System.currentTimeMillis()) : (System.currentTimeMillis() + (1000 * 60 * 60 * 24));
                else
                    value = (add) ? (System.currentTimeMillis()) : (System.currentTimeMillis() + (1000 * 60 * 60 * 24));

                if (add)
                {
                    info = "addDate(" + fieldId + ", " + attributeId + ")";
                    contact.addDate(fieldId, attributeId, value);
                }
                else
                {
                    info = "setDate(" + fieldId + ", " + valueIndex + ", " + attributeId + ")";
                    contact.setDate(fieldId, valueIndex, attributeId, value);
                }
            }
            else if (fieldDataType == PIMItem.BOOLEAN)
            {
                //generate valid values for the field
                boolean value = (add) ? (true) : (false);

                if (add)
                {
                    info = "addBoolean(" + fieldId + ", " + attributeId + ")";
                    contact.addBoolean(fieldId, attributeId, value);
                }
                else
                {
                    info = "setBoolean(" + fieldId + ", " + valueIndex + ", " + attributeId + ")";
                    contact.setBoolean(fieldId, valueIndex, attributeId, value);
                }
            }
            else if (fieldDataType == PIMItem.BINARY)
            {
                //generate valid values for the field
                byte[] value = null;
                if (fieldId == Contact.PHOTO)
                    value = (add) ? (new byte[16]) : (new byte[32]);
                else
                    value = (add) ? (new byte[16]) : (new byte[32]);

                if (add)
                {
                    info = "addBinary(" + fieldId + ", " + attributeId + ")";
                    contact.addBinary(fieldId, attributeId, value, 0, value.length);
                }
                else
                {
                    info = "setBinary(" + fieldId + ", " + valueIndex + ", " + attributeId + ")";
                    contact.setBinary(fieldId, valueIndex, attributeId, value, 0, value.length);
                }
            }
            
            return true;
        }
        catch (Throwable e)
        {
            if (add)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed (at '" + info + "') to add field ('" + fieldLabel + "') to contact ('" + contact + "')", e);
            else
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed (at '" + info + "') to set field ('" + fieldLabel + "') in contact ('" + contact + "')", e);
            return false;
        }
    }
    
    private static void removeContacts(ContactList contactList, Vector contacts)
    {
        if ( (contacts == null) || (contacts.size() <= 0) )
            return;
        
        for (int i = 0; i < contacts.size(); i++)
        {
            Contact contact = (Contact)contacts.elementAt(i);
            testRemoveContact(contactList, contact);
        }
    }    

    private static boolean testCommitContact(ContactList contactList, Contact contact)
    {
        try
        {
            contact.commit();
            return true;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed to commit contact ('" + contact + "')", e);
            return false;
        }
    }

    private static boolean testRemoveContact(ContactList contactList, Contact contact)
    {
        try
        {
            contactList.removeContact(contact);
            return true;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed to remove contact ('" + contact + "')", e);
            return false;
        }
    }

    private static Contact vcardToContact(ContactList contactList, String vcard)
    {
        if ( (vcard == null) || (vcard.length() <= 0) )
            return null;
        
        try
        {
            Contact contact = ContactUtils.vcardToContact(vcard);
            if (contact == null)
            {
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): could not convert vCard to a contact");
                return null;
            }

            return contact;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contactList.getName() + "'): failed to convert vCard to a contact", e);
            return null;
        }
    }

    private static String contactToVcard(Contact contact)
    {
        if ( (contact == null) || (preferredVcardFormat == null) )
            return null;

        try
        {
            String vcard = ContactUtils.vcardFromContact(contact, preferredVcardFormat);
            if ( (vcard == null) || (vcard.length() <= 0) )
            {
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contact.getPIMList().getName() + "'): invalid vCard ('" + preferredVcardFormat + "') generated for contact ('" + contact + "')");
                return null;
            }

            return vcard;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "ContactList ('" + contact.getPIMList().getName() + "'): failed to convert contact ('" + contact + "') to vCard ('" + preferredVcardFormat + "')", e);
            return null;
        }
    }

    private static String attributesToString(ContactList contactList, int fieldId, int attributes)
    {
        if (attributes == PIMItem.ATTR_NONE)
            return "NONE";

        int[] supportedAttributes = contactList.getSupportedAttributes(fieldId);
        if ( (supportedAttributes == null) || (supportedAttributes.length <= 0) )
            return Integer.toString(attributes);
        
        StringBuffer attrs = new StringBuffer();
        for (int i = 0; i < supportedAttributes.length; i++)
        {
            int supportedAttribute = supportedAttributes[i];
            if ( (supportedAttribute == PIMItem.ATTR_NONE) || ((attributes & supportedAttribute) != supportedAttribute) )
                continue;

            String attrName = ContactUtils.attributeToString(supportedAttribute);
            if (attrName == null)
            {
                attrName = contactList.getAttributeLabel(supportedAttribute);
                if ( (attrName == null) || (attrName.length() <= 0) )
                    attrName = Integer.toString(supportedAttribute);
                else
                    attrName = Integer.toString(supportedAttribute) + "(" + attrName + ")";
            }
            
            if (attrs.length() > 0)
                attrs.append(", ");
            attrs.append(attrName);
        }
        
        return attrs.toString();
    }
    
    private static String getFieldLabel(ContactList contactList, int fieldId)
    {
        String fieldLabel = ContactUtils.fieldIdToString(fieldId);
        if (fieldLabel == null)
        {
            fieldLabel = contactList.getFieldLabel(fieldId);
            if ( (fieldLabel == null) || (fieldLabel.length() <= 0) )
                fieldLabel = Integer.toString(fieldId);
            else
                fieldLabel = Integer.toString(fieldId) + "(" + fieldLabel + ")";
        }
        
        return fieldLabel;
    }
    
    private static Contact findContact(Vector contacts, Contact searchContact)
    {
        if ( (contacts == null) || (contacts.size() <= 0) || (searchContact == null) )
            return null;
        
        for (int i = 0; i < contacts.size(); i++)
        {
            Contact contact = (Contact)contacts.elementAt(i);
            if ( (isEquals(contact, searchContact, Contact.UID, true)) || (isEquals(contact, searchContact, Contact.NAME, true)) ||
                 (isEquals(contact, searchContact, Contact.FORMATTED_NAME, true)) || (isEquals(contact, searchContact, Contact.EMAIL, true)) )
            {
                return contact;
            }
        }
        
        return null;
    }

    private static void checkAddedContact(Contact addedContact, Contact currentContact, Vector addedCategories)
    {
        //compare each field
        int[] addedFieldIds = addedContact.getFields();
        ContactList contactList = (ContactList)addedContact.getPIMList();
        for (int i = 0; i < addedFieldIds.length; i++)
        {
            int addedFieldId = addedFieldIds[i];
            if (! contactList.isSupportedField(addedFieldId))
                continue;
            
            int addedValueCount = addedContact.countValues(addedFieldId);
            if (addedValueCount <= 0)
                continue;

            //make sure the added field is actually present in the current contact
            String fieldLabel = getFieldLabel(contactList, addedFieldId);
            int currentValueCount = currentContact.countValues(addedFieldId);
            if (currentValueCount <= 0)
            {
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): field ('" + fieldLabel + "') is no longer present in newly added contact ('" + currentContact + "') - field may not actually be supported");
                continue;
            }
            
            //don't bother checking the values of these fields as they're set by the system
            if (addedFieldId == Contact.REVISION)
                continue;
            
            //check if the values of the field have changed
            isEquals(addedContact, currentContact, addedFieldId, false);
        }
        
        //check the categories
        if ( (addedCategories != null) && (contactList.maxCategories() != 0) )
        {
            String[] addedContactCats = addedContact.getCategories();
            String[] currentContactCats = currentContact.getCategories();
            if ( (addedContactCats.length > 0) && (currentContactCats.length <= 0) )
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): newly added contact ('" + currentContact + "') is no longer a member of any categories - categories may not actually be supported");
            else if (! StringUtils.isEquals(addedContactCats, currentContactCats))
                Logger.logIssue(Logger.SEVERITY_HIGH, "ContactList ('" + contactList.getName() + "'): newly added contact ('" + currentContact + "') is no longer a member of some categories");
        }
    }

    private static boolean isEquals(Contact contact1, Contact contact2, int fieldId, boolean silent)
    {
        //make sure the specified field is supported
        ContactList contactList = (ContactList)contact1.getPIMList();
        if (! contactList.isSupportedField(fieldId))
            return false;
        
        //determine the number of value to compare
        String fieldLabel = getFieldLabel(contactList, fieldId);
        int valueCount1 = contact1.countValues(fieldId);
        int valueCount2 = contact2.countValues(fieldId);
        
        //field is not present in one of the contact - treat this as a mismatch
        if ( (valueCount1 <= 0) || (valueCount2 <= 0) )
            return false;
        
        //check that each value of the specified field in the first contact is also present in the second contact  
        int matchingValueCount = 0;
        for (int i = 0; i < valueCount1; i++)
        {
            boolean matchFound = false;
            for (int j = 0; j < valueCount2; j++)
            {
                if (ContactUtils.isEquals(contact1, contact2, fieldId, i, j))
                {
                    matchFound = true;
                    matchingValueCount++;
                    break;
                }
            }
            
            if ( (! matchFound) && (! silent) )
            {
                String attrsLabel = attributesToString(contactList, fieldId, contact1.getAttributes(fieldId, i));
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "ContactList ('" + contactList.getName() + "'): the value of field ('" + fieldLabel + " [" + attrsLabel + "]') is no longer present in newly added contact ('" + contact2 + "') - value may have been altered during commit");
            }
        }

        //all values match - the fields are the same
        return (matchingValueCount == valueCount1);
    }
}
