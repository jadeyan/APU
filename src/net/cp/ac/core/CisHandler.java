/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import net.cp.engine.EngineSettings;
import net.cp.syncml.client.util.Logger;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Build;
import android.util.Log;


/**
 *
 *
 * Handles CIS (Client Initiated Sync) related events.
 * What this means is that when syncable items are modified on the device, this class is informed,
 * and it passes on the information to the appropriate listener (The SyncEngineService in this case).
 *
 * This class implements the Singleton design pattern.
 *
 */
public class CisHandler
{
    private static CisHandler instance = null;           //the single instance of the handler
    private Logger logger;                               //the logger used to log activity


    /**
     * The context to use to register for broadcast intent
     */
    protected Context context;

    private CisContentObserver contactObserver;

    /**
     * The consumer of the CIS information
     */
    protected SyncEngineService consumer;

    /**
     *
     * @param consumer The interface to be notified when contacts change. Can be null.
     * @param context Context used to listen for contacts change events. Usually the "Application" context.
     * @param logger A Logger to use during execution. Can be null.
     */
    public static synchronized CisHandler init(SyncEngineService consumer, Context context, Logger logger)
    {
        if (instance == null)
        {
            //create the single instance of the server alert handler if necessary
            instance = new CisHandler(consumer, context, logger);
            instance.register();
        }

        return instance;
    }

    /**
     *  Creates a new server alert handler - private to prevent object creation.
     */
    private CisHandler(SyncEngineService consumer, Context context, Logger logger)
    {
        super();

        this.consumer = consumer;
        this.context = context;
        this.logger = logger;
        contactObserver = new CisContentObserver(this);
    }


    /**
     * @return the instance of this class
     */
    public static CisHandler getInstance()
    {
        return instance;
    }

    /**
     * Opens the server alert handler.
     *
     */
    public synchronized void register()
    {
        if(logger != null)
            logger.info("Registering for CIS");

        contactObserver.register();
    }

    /**
     * Closes the server alert handler.
     */
    public synchronized void unregister()
    {
        if(logger != null)
            logger.info("Unregistering for CIS");

        contactObserver.unregister();
    }

    /**
     * Called when the contact DB has been updated
     */
    protected void onChange()
    {
        if(logger != null)
            logger.debug("CisHandler.onChange()");

        consumer.onCISIntent(EngineSettings.MEDIA_TYPE_CONTACTS);
    }

    private class CisContentObserver extends ContentObserver
    {

        private CisHandler handler;

        /**
         * @param handler the CisHandler to inform when a change occurs
         */
        public CisContentObserver(CisHandler handler)
        {
            super(null);
            this.handler = handler;
        }

        /* (non-Javadoc)
         * @see android.database.ContentObserver#onChange(boolean)
         */
        @Override
        public void onChange(boolean selfChange)
        {
            super.onChange(selfChange);
            handler.onChange();
        }

        /**
         * register with Android as a content observer
         */
        protected void register()
        {
            String className;
            Uri contentUri;

            if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.ECLAIR)
            {
                className = "android.provider.Contacts$People";
            }
            else
            {
                className = "android.provider.ContactsContract$Contacts";
            }

            // Find the required class by name and get the CONTENT_URI for it.
            try
            {
                Class<?> c = Class.forName(className);
                contentUri = (Uri) c.getField("CONTENT_URI").get(null);
            }
            catch (Exception e)
            {
                if(logger != null)
                    logger.error("Error getting required class", e);

                throw new IllegalStateException(e);
            }

            handler.context.getContentResolver().registerContentObserver(contentUri, true, this);

        }

        /**
         * un-register with Android as a content observer
         */
        protected void unregister()
        {
            handler.context.getContentResolver().unregisterContentObserver(this);
        }
    }
}



