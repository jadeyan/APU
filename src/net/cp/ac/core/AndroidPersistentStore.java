/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import net.cp.engine.PersistentStore;
import net.cp.syncml.client.store.StoreException;
import net.cp.syncml.client.util.Logger;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.BaseColumns;

/**
 * A class encapsulating a record store which can be used to persist data on the device. <br/><br/>
 *
 * This class encapsulates the standard MIDP RecordStore implementation to allow records to be stored on the Android device.
 *
 * @author James O'Connor
 */
public class AndroidPersistentStore implements PersistentStore {
    private final String storeName;                   // the unique name of the record store
    private final Logger logger;                      // the logger to use to log activity
    private SQLiteDatabase store;               // the DB where we will store all our records
    private Cursor storeCursor;                 // the cursor which will be used to enumerate the records
    private final Context context;                    // context used to create DB

    /**
     * The prefix to use when generating DB names
     */
    protected static final String RECORDSTORE_PREFIX = "RecordStore-";

    /**
     * The table name to use when creating DBs
     */
    private static final String RECORDSTORE_TABLE_NAME = "RECORD_TABLE";

    /**
     * The column name for the binary data
     */
    private static final String COLUMN_NAME_DATA = "DATA";

    private static final String[] RECORD_PROJECTION = { BaseColumns._ID, COLUMN_NAME_DATA };

    private static final int COLUMN_INDEX_ID = 0;   // the column index for the ID of the record

    private static final int COLUMN_INDEX_DATA = 1; // the column index for the data of the record

    /**
     * @param recordStoreName The name to use for this store
     * @param context The context to use to acces the DB subsystem
     * @param storeLogger The logger to use
     */
    public AndroidPersistentStore(String recordStoreName, Context context, Logger storeLogger) {
        super();

        storeName = recordStoreName;
        logger = storeLogger;
        this.context = context;
        store = null;

    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#open(boolean)
     */
    @Override
    public synchronized boolean open(boolean create) throws StoreException {
        // nothing more to do if the store is already open
        if (store != null) return true;

        try {
            if (logger != null) logger.info("Opening the record store with name '" + storeName + "'");

            // try to open the DB
            // Log.e("AndroidPersistentStentStore", "open");
            if (create)
                store = context.openOrCreateDatabase(RECORDSTORE_PREFIX + storeName, Context.MODE_PRIVATE, null);

            else {
                String[] DBs = context.databaseList();

                for (String db : DBs) {
                    // DB already exists, open it
                    if (db.equals(RECORDSTORE_PREFIX + storeName)) {
                        store = context.openOrCreateDatabase(RECORDSTORE_PREFIX + storeName, Context.MODE_PRIVATE, null);
                        break;
                    }
                }
            }

            if (store == null) {
                if (!create)
                    return false;
                else
                    throw new Exception("Failed to create DB");
            }

            // create the table if necessary
            store.execSQL("CREATE TABLE IF NOT EXISTS " + RECORDSTORE_TABLE_NAME + " (" + BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_NAME_DATA + " BLOB);");
            return true;
        } catch (SQLiteException e) {
            if (logger != null) logger.error("Record store with name '" + storeName + "' doesn't exist or could not be opened", e);

            // don't throw an exception if we weren't asked to create the record store
            if (!create) return false;

            throw new StoreException("Record store with name '" + storeName + "' doesn't exist or could not be opened", e);
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to open the record store with name '" + storeName + "'", e);

            throw new StoreException("Failed to open the record store with name '" + storeName + "'", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#close()
     */
    @Override
    public synchronized void close() {
        // nothing more to do if the store hasn't been opened
        if (store == null) return;

        if (logger != null) logger.info("Closing the record store with name '" + storeName + "'");

        try {
            // stop any enumeration that may be in progress
            stopEnumeration();
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to close the record store - ignoring", e);
        }

        finally {
            // close the RMS
            if (store != null) store.close();
        }

        store = null;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#getNumRecords()
     */
    @Override
    public synchronized int getNumRecords() throws StoreException {
        try {
            int count = -1;

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(RECORDSTORE_TABLE_NAME);
            Cursor c = builder.query(store, RECORD_PROJECTION, null, null, null, null, null);
            count = c.getCount();

            if (logger != null) logger.info("getNumRecords() for " + storeName + " is: " + count);

            c.deactivate();
            c.close();

            return count;
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to determine the number of records in the store", e);

            throw new StoreException("Failed to determine the number of records in the store", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#startEnumeration()
     */
    @Override
    public synchronized void startEnumeration() throws StoreException {
        // make sure an enumeration is not already in progress
        if (storeCursor != null) throw new IllegalStateException("Enumeration of record store '" + storeName + "' is already in progress");

        try {
            if (logger != null) logger.info("Starting enumeration of record store with name '" + storeName + "'");

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(RECORDSTORE_TABLE_NAME);
            storeCursor = builder.query(store, RECORD_PROJECTION, null, null, null, null, null);

            if (storeCursor != null && storeCursor.getCount() > 0) {
                storeCursor.moveToFirst();
            }
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to start the enumerate of records in the store", e);

            throw new StoreException("Failed to start the enumerate of records in the store", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#stopEnumeration()
     */
    @Override
    public synchronized void stopEnumeration() {
        // nothing more to do if no enumeration is in progress
        if (storeCursor == null) return;

        if (logger != null) logger.info("Stopping enumeration of record store with name '" + storeName + "'");

        storeCursor.close();
        storeCursor = null;
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#enumerationStarted()
     */
    @Override
    public synchronized boolean enumerationStarted() {
        return (storeCursor != null);
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#nextRecordId()
     */
    @Override
    public synchronized int nextRecordId() throws StoreException {
        // make sure an enumeration has been started and is not complete
        if (storeCursor == null) return 0;

        try {
            int count = storeCursor.getCount();
            if (count > 0) {
                // look for the next ID to return
                int nextId = storeCursor.getInt(COLUMN_INDEX_ID);

                if (logger != null) logger.info("Enumerating store: '" + storeName + "'  record ID: '" + nextId + "'");

                if (storeCursor.isLast())
                    stopEnumeration();

                else
                    // go to the next record
                    storeCursor.moveToNext();

                return nextId;
            }

            // stop the enumeration as there are no records to return
            stopEnumeration();
            return 0;
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to enumerate the next record", e);

            // stop the enumeration
            stopEnumeration();

            throw new StoreException("Failed to enumerate the next record", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#readRecord(int)
     */
    @Override
    public synchronized byte[] readRecord(int recordId) throws StoreException {
        Cursor c = null;
        try {
            if (logger != null) logger.debug("Reading the record with ID '" + recordId + "' from record store '" + storeName + "'");

            SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
            builder.setTables(RECORDSTORE_TABLE_NAME);

            String where = BaseColumns._ID + "=?";
            String[] args = {String.valueOf(recordId)};
            //c = builder.query(store, RECORD_PROJECTION, null, null, null, null, null);
            c = builder.query(store, RECORD_PROJECTION, where, args, null, null, null);

            byte[] data = null;
            if (c != null) {
                if (logger != null)
                    logger.debug("store: " + store + " name: " + this.storeName + " rows: " + c.getCount() + " columns: " + c.getColumnCount());

                if (c.moveToFirst()) {
                    data = c.getBlob(COLUMN_INDEX_DATA);
                }
            }

            return data;
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to read the record with ID '" + recordId + "'", e);

            throw new StoreException("Failed to read the record with ID '" + recordId + "'", e);
        } finally {
            if (c != null) c.close();
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#writeRecord(int, byte[])
     */
    @Override
    public synchronized int writeRecord(int recordId, byte[] data) throws StoreException {
        try {
            ContentValues values = new ContentValues();

            if (recordId <= 0) {
                // no RMS ID present - add a new record
                values.put(COLUMN_NAME_DATA, data);

                // this should be ok, we don't expect an enourmous number of records
                recordId = (int) store.insert(RECORDSTORE_TABLE_NAME, "", values);

                if (recordId < 0) throw new Exception("failed to add record!");

                if (logger != null) logger.debug("Added new record with ID '" + recordId + "' to record store '" + storeName + "'");
            } else {
                values.put(COLUMN_NAME_DATA, data);

                store.update(RECORDSTORE_TABLE_NAME, values, BaseColumns._ID + "=?", new String[] { "" + recordId });

                if (logger != null) logger.debug("Updated the record with ID '" + recordId + "' in record store '" + storeName + "'");
            }

            return recordId;
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to write the record with ID '" + recordId + "'", e);

            throw new StoreException("Failed to write the record with ID '" + recordId + "'", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#deleteRecord(int)
     */
    @Override
    public synchronized void deleteRecord(int recordId) throws StoreException {
        try {
            // delete the record
            store.delete(RECORDSTORE_TABLE_NAME, BaseColumns._ID + "=?", new String[] { "" + recordId });

            if (logger != null) logger.debug("Deleted the record with ID '" + recordId + "' from record store '" + storeName + "'");
        } catch (Throwable e) {
            if (logger != null) logger.error("Failed to delete the record with ID '" + recordId + "'", e);

            throw new StoreException("Failed to delete the record with ID '" + recordId + "'", e);
        }
    }

    /*
     * (non-Javadoc)
     * @see net.cp.engine.PersistentStore#getSizeAvailable()
     */
    @Override
    public synchronized int getSizeAvailable() {
        return Integer.MAX_VALUE;
    }

}
