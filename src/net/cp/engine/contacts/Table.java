package net.cp.engine.contacts;

import java.util.Enumeration;

import net.cp.syncml.client.util.Logger;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;


public abstract class Table {


    /*
     * SQLLite throws a android.database.sqlite.SQLiteDiskIOException: disk I/O error if the results passed back from a query exceed a certain number. the
     * exception seems to happen on calls to Cursor.getCount(). This function is called by Cursor.moveToNext(), moveToLast, etc. The solution is to ensure that
     * queries return chunked subsets of their full results.
     */
    // Original was 200. Increased to 500 and it seems to work OK in all devices
    public static int MAX_DB_RESULT_CHUNK_SIZE = 500;

    public static abstract class CursorEnumeration implements Enumeration<Cursor> {

        @Override
        public abstract boolean hasMoreElements();

        @Override
        /**
         * Returns the next result as a Cursor
         * Note: Each Cursor is a unique reference owned by the caller, and MUST be closed. 
         */
        public abstract Cursor nextElement();

    }

    protected SQLiteDatabase db;
    protected String tableName;
    private static final String GET_RECORD_COUNT_PART_QUERY = "SELECT COUNT(*) FROM ";
    private String iRecordCountQuery;

    public Table(SQLiteDatabase aDb, String tableName, String createSQL, boolean create) {
        db = aDb;
        this.tableName = tableName;
        buildRecordCountQuery();

        try {
            if (create && createSQL != null) {
                db.execSQL(createSQL);
            }
        } catch (Throwable e) {
        	e.printStackTrace();
//            Ln.e(e, "Failed to create table " + tableName);
        }
    }

    public long insert(ContentValues initialValues) {
        return db.insert(tableName, null, initialValues);
    }

    public abstract void init(SQLiteDatabase aDb, Logger logger);

    protected void updateLong(String column, long value) {
        ContentValues args = new ContentValues();
        args.put(column, value);
        db.update(tableName, args, null, null);
    }

    protected long getLong(String column) {
        long result = 0L;
        Cursor mCursor = null;
        try {
            mCursor = db.query(true, tableName, new String[] { column }, null, null, null, null, null, null);
            if (mCursor != null && mCursor.moveToFirst()) {
                result = mCursor.getLong(0);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return result;
    }

    protected String getString(String column) {
        String result = null;
        Cursor mCursor = null;
        try {
            mCursor = db.query(true, tableName, new String[] { column }, null, null, null, null, null, null);
            if (mCursor != null && mCursor.moveToFirst()) {
                result = mCursor.getString(0);
            }
        } finally {
            if (mCursor != null) mCursor.close();
        }
        return result;
    }

    protected void updateString(String clLocale, String locale) {
        ContentValues args = new ContentValues();
        args.put(clLocale, locale);
        db.update(tableName, args, null, null);
    }

    public void close() {
        if (db != null) db.close();
    }

    protected String buildRecordCountQuery() {
        iRecordCountQuery = GET_RECORD_COUNT_PART_QUERY + this.tableName;
        return iRecordCountQuery;
    }

    protected String getRecordCountQuery() {
        return iRecordCountQuery;
    }

    /**
     * Executes a SQL query to get the number of records in the table.
     */
    public long queryRowCount() {
        long result = 0;
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(iRecordCountQuery, null);
            if (null != cursor) {

                try {
                    cursor.moveToFirst();
                    result = cursor.getLong(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } finally {
            cursor.close();
        }
        return result;
    }

    /**
     * The SQLlite implementation cannot handle accesses using Cursors encapsulating a window of more than a certain number of elements. A safe window is
     * Table.MAX_DB_RESULT_CHUNK_SIZE This function provides an enumeration of limited Cursors over the entire table.
     * 
     * @return
     */
    public CursorEnumeration getItems() {

        return new Table.CursorEnumeration() {

            long iSize = queryRowCount();
            long iFrom = 0;

            @Override
            public boolean hasMoreElements() {
                return iFrom < iSize;
            }

            @Override
            /**
             * Returns the next result as a Cursor
             * Note: Each Cursor is a unique reference owned by the caller, and MUST be closed. 
             */
            public Cursor nextElement() {
                Cursor result = null;
                String limitTerm = "LIMIT " + iFrom + ", " + Table.MAX_DB_RESULT_CHUNK_SIZE;
                String query = "SELECT * FROM " + tableName + " " + limitTerm;
                try {
                    result = db.rawQuery(query, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                iFrom = iFrom + Table.MAX_DB_RESULT_CHUNK_SIZE;
                return result;
            }

        };
    }


}
