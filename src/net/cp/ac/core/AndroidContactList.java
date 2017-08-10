/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.lang.reflect.Constructor;
import java.util.Hashtable;

import net.cp.engine.UIInterface;
import net.cp.engine.contacts.Contact;
import net.cp.engine.contacts.ContactList;
import net.cp.engine.contacts.ContactStore;
import net.cp.syncml.client.util.Logger;
import android.content.ContentResolver;
import android.os.Build;

public abstract class AndroidContactList extends ContactList
{
    protected Hashtable<String, Contact> allContacts;
    
    /**
     * This ContentResolver is used to access the contact DB
     */
    protected ContentResolver resolver;

    /**
     * Used to give feedback on contact enumeration
     */
    protected UIInterface ui;


    //TODO: Find sensible limits

    /**
     * The maximum number of phone numbers we support
     */
    public static int ANDROID_MAX_PHONES = 16;

    /**
     * The maximum number of email addresses we support
     */
    public static int ANDROID_MAX_EMAILS = 16;

    /**
     * The maximum number of organizations we support
     */
    public static int ANDROID_MAX_ORG = 16;

    /**
     * The maximum number of formatted addresses we support
     */
    public static int ANDROID_MAX_FORMATTED_ADDR = 16;

    /**
     * The maximum number of structured addresses we support
     */
    public static int ANDROID_MAX_STRUCTURED_ADDR = 16;

    /**
     * The maximum number of urls we support
     */
    public static int ANDROID_MAX_URLS = 16;


    private static AndroidContactList sInstance;

    /**
     * @param store The ContactStore that is associated with this ContactList
     * @param resolver The ContentResolver to use to query the contact DB
     * @param ui The UIInterface to use to give feedback on contact enumeration
     * @param logger The logger to use
     */
    public AndroidContactList(ContactStore store, ContentResolver resolver, UIInterface ui, Logger logger)
    {
        super(store, logger);

        supportedAttributesList = new int[]{
                Contact.ATTR_NONE,
                Contact.ATTR_FAX,
                Contact.ATTR_HOME,
                Contact.ATTR_MOBILE,
                Contact.ATTR_OTHER,
                Contact.ATTR_PAGER,
                Contact.ATTR_PREFERRED,
                Contact.ATTR_SMS,
                Contact.ATTR_WORK
                };

        this.resolver = resolver;
        this.ui = ui;
    }

    @SuppressWarnings("unchecked")
    public static AndroidContactList getInstance(ContactStore store, ContentResolver resolver, UIInterface ui, Logger logger) {
        if (sInstance == null) {
            String className;

            /*
             * Check the version of the SDK we are running on. Choose an
             * implementation class designed for that version of the SDK.
             *
             * Unfortunately we have to use strings to represent the class
             * names. If we used the conventional ContactAccessorSdk5.class.getName()
             * syntax, we would get a ClassNotFoundException at runtime on pre-Eclair SDKs.
             * Using the above syntax would force Dalvik to load the class and try to
             * resolve references to all other classes it uses. Since the pre-Eclair
             * does not have those classes, the loading of ContactAccessorSdk5 would fail.
             */

            if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ECLAIR)
            {
                className = "net.cp.ac.core.AndroidContactListAPI3";
            }
            else
            {
                className = "net.cp.ac.core.AndroidContactListAPI5";
            }

            /*
             * Find the required class by name and instantiate it.
             */
            try {
                Class<? extends AndroidContactList> realClass =
                        Class.forName(className).asSubclass(AndroidContactList.class);
                Constructor<AndroidContactList> constructor =
                    (Constructor<AndroidContactList>) realClass.getConstructor(
                         new Class[] {net.cp.engine.contacts.ContactStore.class,
                                      android.content.ContentResolver.class,
                                      net.cp.engine.UIInterface.class,
                                      net.cp.syncml.client.util.Logger.class  });
                sInstance = (AndroidContactList) constructor.newInstance(store, resolver, ui, logger);
            } catch (Exception e) {
                if(logger != null)
                    logger.error("Error getting required class", e);

                throw new IllegalStateException(e);
            }
        }

        return sInstance;
    }
}
