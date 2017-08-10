package net.cp.engine.contacts;

import java.util.HashMap;

import net.cp.syncml.client.util.Logger;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class RawContactTable extends Table{
	
	public static final String RAW_ID = BaseColumns._ID;
	
//	public static final String contact_ID = RawContacts.CONTACT_ID;
	
	public static final String version = RawContacts.VERSION;

    public static final String TABLE_NAME = "rawcontacttable";
    
    Logger logger;
    
    // create string
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME + " (_mid integer primary key autoincrement, " + RAW_ID + " integer,"
            /*+ contact_ID + " integer," */+ version + " integer);";
	
    private static RawContactTable instance;
	
    private RawContactTable(SQLiteDatabase aDb, boolean create) {
		super(aDb, TABLE_NAME, CREATE_TABLE, create);
	}


    public static RawContactTable getTable(SQLiteDatabase db) {
        if (instance == null) instance = new RawContactTable(db, false);

        return instance;
    }

    public static RawContactTable createTable(SQLiteDatabase db) {
        if (instance == null) instance = new RawContactTable(db, true);

        return instance;
    }
    
	@Override
	public void init(SQLiteDatabase aDb, Logger logger) {
		db = aDb;
		this.logger = logger;
	}

//	public void insert(long rawID, long contact_id, int ver){
//		ContentValues values = new ContentValues();
//		values.put(RAW_ID, rawID);
//		values.put(version, ver);
//		db.insert(TABLE_NAME, null, values);
//	}
	
	protected void updateRawVersion(String raw_id, int value) {
		Cursor cur = null;
		try{
			String selection = RAW_ID+"=?";
			String[] selectArg = new String[]{raw_id};
			cur = db.query(TABLE_NAME, new String[]{RAW_ID, version}, selection, selectArg, null, null, null);
	        ContentValues args = new ContentValues();
	        args.put(RAW_ID, Long.parseLong(raw_id));
	        args.put(version, value);
			if(cur == null || !cur.moveToFirst())
			{
				db.insert(TABLE_NAME, null, args);
			}
			else
			{
				db.update(TABLE_NAME, args, selection, selectArg);
			}
		} catch(Exception e){
			e.printStackTrace();
			if(logger != null)
			{
				logger.error("database exception -- updateRawVersion:" + e.getMessage());
				logger.error("update database failed. raw id:" + raw_id + ", raw version: " + value);
			}
		}finally{
			if(cur!= null)
				cur.close();
		}
    }
	
	protected void deleteItem(String raw_id){
		String selection = RAW_ID+"=?";
		String[] selectArg = {raw_id};
		db.delete(TABLE_NAME, selection, selectArg);
	}
	
//	public void update(String key, long value){
//		db.query(TABLE_NAME, new String[]{RAW_ID, contact_ID, version}, key+"=?", selectionArgs, null, null, null);
//		ContentValues values = new ContentValues();
//		values.put(key, value);
//		db.update(TABLE_NAME, values, key+Long.toString(n), whereArgs);
//	}
	
	/**
     * getAllIds - get a map of all aggregate and raw pim ids and their versions present in the SyncChangeLog db table. As we only want syncable contact ids,
     * skip any from Connect Social accounts
     * 
     * @param raw
     *            the HashMap of raw ids and versions
     */
    public void getAllIds(HashMap<String, Integer> raw) {

        Cursor cur = null;
        try {

            // We skip any Changelog items for retries so they can be added again

            cur = db.query(TABLE_NAME, new String[] {RAW_ID, /*contact_ID, */version}, null, null, null, null, null);

            // If there's nothing there...
            if (cur == null || !cur.moveToFirst()) return;

            int rIdCol = cur.getColumnIndex(BaseColumns._ID);
            int verColIdx = cur.getColumnIndex(RawContacts.VERSION);

            int queryCount = cur.getCount();
//            Ln.d("Changelog query returned %d results", queryCount);

            // Ln.i("***ADDRESSBOOKID,PIMID,URI,TYPE,VERSION,RETRIES");
            for (int i = 0; i < queryCount; i++) {

                long rawId = cur.getLong(rIdCol);
                int version = cur.getInt(verColIdx);

                // Add to the correct array based on type
                raw.put(Long.toString(rawId), Integer.valueOf(version));
                cur.moveToNext();
            }
        }catch(Exception e){
        	e.printStackTrace();
        	if(logger != null)
			{
				logger.error("database exception -- get all raw ids:"+e.getMessage());
			}
//        	Log.e("data base", "not open");
        } finally {
            if (cur != null) cur.close();
        }
    }
}
