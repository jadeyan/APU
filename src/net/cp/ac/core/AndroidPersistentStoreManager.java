/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.util.Vector;

import android.content.Context;

import net.cp.engine.PersistentStore;
import net.cp.engine.PersistentStoreManager;

import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.*;


/**
 * A class encapsulating a record store manager which can be used to manage persistent data on the device. <br/><br/>
 *
 * This class implements the Singleton design pattern.
 *
 * @author James O'Connor
 */
public class AndroidPersistentStoreManager implements PersistentStoreManager
{
    private static Logger logger;                      //the logger to use to log activity
    private static Context context;
    private static AndroidPersistentStoreManager instance;
    private static final String CONFIG_STORE_NAME = "features.properties";
    private static final String TEMP_STORE_NAME = "TEMP";

    private AndroidPersistentStoreManager()
    {

    }

    /**
     * This should not be called before the class has been initialized by a call to init().
     * @return returns the instance of this class.
     */
    public static AndroidPersistentStoreManager getInstance()
    {
        if(context == null)
            throw new IllegalStateException("static fields have not been initialized!");

        return instance;
    }

    /**
     * This method should be called before any attempt is made to get an instance from the class
     *
     * @param context The Context used to access the database. Usually Application.
     * @param storeLogger An optional logger that can be used to log information
     */
    public static void init(Context theContext, Logger storeLogger)
    {

        context = theContext;
        logger = storeLogger;
        instance = new AndroidPersistentStoreManager();

    }

    /* (non-Javadoc)
     * @see net.cp.engine.PersistentStoreManager#openRecordStore(java.lang.String)
     */
    public PersistentStore openRecordStore(String storeName)
        throws StoreException
    {
        return openRecordStore(storeName, true);
    }

    /**
     * Opens and returns the temporary record store, creating it if necessary.
     *
     * @return the temp record store.
     * @throws StoreException if the record store could not be opened or created.
     */
    public static PersistentStore getTemporaryStore()
    {
        try
        {
            return getInstance().openRecordStore(TEMP_STORE_NAME, true);
        }

        catch (StoreException e)
        {
            if(logger != null)
                logger.error("Failed to open TEMP PersistentStore", e);
        }

        return null;
    }

    /**
     * Deletes the temporary record store.
     */
    public static void deleteTemporaryStore()
    {
        try
        {
            instance.deleteRecordStore(TEMP_STORE_NAME);
        }
        catch (Throwable e)
        {
            if(logger != null)
                logger.error("Failed to delete TEMP PersistentStore", e);
        }
    }

    /* (non-Javadoc)
     * @see net.cp.engine.PersistentStoreManager#openRecordStore(java.lang.String, boolean)
     */
    public PersistentStore openRecordStore(String storeName, boolean create)
        throws StoreException
    {
        AndroidPersistentStore store = new AndroidPersistentStore(storeName, context, logger);
        if (! store.open(create))
        {
            store.close();
            return null;
        }

        return store;
    }

    /* (non-Javadoc)
     * @see net.cp.engine.PersistentStoreManager#closeRecordStore(net.cp.engine.PersistentStore)
     */
    public void closeRecordStore(PersistentStore store)
    {
        if (store != null)
            store.close();
    }

    /* (non-Javadoc)
     * @see net.cp.engine.PersistentStoreManager#deleteRecordStore(java.lang.String)
     */
    public void deleteRecordStore(String storeName)
        throws StoreException
    {
        String DBName = AndroidPersistentStore.RECORDSTORE_PREFIX + storeName;
        context.deleteDatabase(DBName);
    }


    /* (non-Javadoc)
     * @see net.cp.engine.PersistentStoreManager#listRecordStores()
     */
    public String[] listRecordStores()
    {

        String[] DBs = context.databaseList();

        Vector<String> list = new Vector<String>(DBs.length);

        for(int i=0; i<DBs.length; i++)
        {
            if(DBs[i].startsWith(AndroidPersistentStore.RECORDSTORE_PREFIX))
                list.add(DBs[i]);
        }

        int size = list.size();
        if(size > 0)
        {
            String[] stores = new String[size];
            String fullName;
            int startIndex = AndroidPersistentStore.RECORDSTORE_PREFIX.length();

            for(int i=0; i<size; i++)
            {
                fullName = (String) list.get(i);
                stores[i] = fullName.substring(startIndex);
            }

            return stores;
        }

        return null;
    }

    /**
     *
     * @param context The context to use
     * @param logger The logger to use
     * @return an InputStream from which the config properties can be read, or null if the config is not present in the Store
     */
    public static byte[] getConfigFromStore(Context context, Logger logger)
    {
        PersistentStore store = null;
        try
        {

            if(instance == null)
                init(context, logger);

            store = instance.openRecordStore(CONFIG_STORE_NAME, false);

            if(store == null)
                return null;

            //this store should only ever contain one record, with id 1
            byte[] configData = store.readRecord(1);

            return configData;
        }

        catch (StoreException e)
        {
            if(logger != null)
                logger.error("Error getting config from store: " + e);
        }

        finally
        {
            //always close the store
            if(store != null)
                store.close();
        }

        return null;
    }

    /**
     * Writes the supplied config data into the persistent storage of the device
     * @param configInputStream an InputStream from which the config properties can be read
     * @param context The context to use
     * @param logger The logger to use
     */
    public static void writeConfigToStore(InputStream configInputStream, Context context, Logger logger)
    {
        if(configInputStream != null)
        {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);

            PersistentStore store = null;

            try
            {
                while(configInputStream.available() > 0)
                    dout.write(configInputStream.read());

                dout.flush();
                bout.flush();

                if(instance == null)
                    init(context, logger);

                store = instance.openRecordStore(CONFIG_STORE_NAME, true);

                //0 means write new record
                store.writeRecord(0, bout.toByteArray());

            }
            catch (Exception e)
            {
                if(logger != null)
                    logger.error("Error writing config to store: " + e);
            }

            finally
            {
                store.close();
            }
        }
    }
}
