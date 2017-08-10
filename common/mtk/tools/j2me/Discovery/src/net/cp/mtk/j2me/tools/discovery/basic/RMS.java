/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.tools.discovery.basic;


import java.util.Calendar;
import java.util.Date;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;

import net.cp.mtk.common.CommonUtils;
import net.cp.mtk.j2me.tools.discovery.Logger;
import net.cp.mtk.j2me.tools.discovery.DiscoveryMIDlet;


public class RMS
{
    private static final int RECORD_COUNT = 10;
    private static final int RECORD_SIZE =  10240;
    
    private static float averageAddTime = 0;
    private static float averageGetTime = 0;
    private static float averageSetTime = 0;
    private static float averageDeleteTime = 0;
    private static float averageEnumTime = 0;
        
    public static void evaluate(DiscoveryMIDlet midlet)
    {
        Logger.log("");
        Logger.log("-----------------------------------");
        Logger.log("RMS:");
        Logger.log("");

        //show memory size
        midlet.setTestStatus("Testing RMS...");
        logRMS();

        Logger.log("-----------------------------------");
    }

    
    private RMS()
    {
        super();
    }

    private static void logRMS()
    {
        String name = "TestRMS1";
        RecordStore rms = null;
        try
        {
            //create a new RMS
            rms = openRMS(name);
            if (rms == null)
                return;
            
            //get RMS details
            int usedSize = rms.getSize();
            int freeSize = rms.getSizeAvailable();
            
            //log some details
            Logger.log("Used size: " + usedSize + " bytes (" + (usedSize/1024) + " KB)");
            Logger.log("Free size: " + freeSize + " bytes (" + (freeSize/1024) + " KB)");
            Logger.log("Records:   " + rms.getNumRecords());
            Logger.log("Version:   " + rms.getVersion());
            
            //make sure the ID of the first record is '1'
            int firstId = rms.getNextRecordID();
            Logger.log("Next ID:   " + firstId);
            if (firstId != 1)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): ID of first record ('" + firstId + "') is incorrect (it should be '1')");
            
            //last modified time
            long lastModifiedTime = rms.getLastModified();
            Logger.log("Modified:  " + DiscoveryMIDlet.dateToString(lastModifiedTime));
            if (lastModifiedTime > 0)
            {
                //check the granularity of the modified time
                Calendar cal = Calendar.getInstance();
                cal.setTime(new Date(lastModifiedTime));
                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);
                int second = cal.get(Calendar.SECOND);
                int millsecond = cal.get(Calendar.MILLISECOND);
                if ( (hour <= 0) && (minute <= 0) && (second <= 0) && (millsecond <= 0) )
                    Logger.logIssue(Logger.SEVERITY_MEDIUM, "RMS ('" + name + "'): RMS last modified time doesn't have time granularity (only date)");
                else if (second <= 0)
                    Logger.logIssue(Logger.SEVERITY_MEDIUM, "RMS ('" + name + "'): RMS last modified time doesn't have second granularity");
                else if (millsecond <= 0)
                    Logger.logIssue(Logger.SEVERITY_LOW, "RMS ('" + name + "'): RMS last modified time doesn't have millisecond granularity");
            }
            else
            {
                Logger.logIssue(Logger.SEVERITY_MEDIUM, "RMS ('" + name + "'): RMS last modified time ('" + lastModifiedTime + "') is invalid");
            }
            
            //add some records
            addRMSRecords(rms, RECORD_COUNT, RECORD_SIZE);
            
            //determine how much storage overhead (i.e how much extra storage is needed to store X bytes of application data)
            int totalRecordSize = rms.getSize() - usedSize;
            int totalOverhead = totalRecordSize - (RECORD_COUNT * RECORD_SIZE);
            Logger.log("Overhead:  " + (totalOverhead / RECORD_COUNT) + " bytes per record");
            
            //enumerate all the records
            enumRMSRecords(rms);
            
            //update each record
            setRMSRecords(rms, 1, RECORD_COUNT, RECORD_SIZE);
            
            //delete half of the records
            deleteRMSRecords(rms, 1, RECORD_COUNT/2);
            
            //log performance
            Logger.log("");
            Logger.log("Time (Add Record):        " + averageAddTime + "ms per record (" + RECORD_SIZE + " bytes per record)");
            Logger.log("Time (Get Record):        " + averageGetTime + "ms per record (" + RECORD_SIZE + " bytes per record)");
            Logger.log("Time (Set Record):        " + averageSetTime + "ms per record (" + RECORD_SIZE + " bytes per record)");
            Logger.log("Time (Delete Record):     " + averageDeleteTime + "ms per record (" + RECORD_SIZE + " bytes per record)");
            Logger.log("Time (Enumerate Records): " + averageEnumTime + "ms (" + RECORD_COUNT + " records)");
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed to log RMS details", e);
        }
        finally
        {
            closeRMS(rms);
            deleteRMS(name);
        }
    }
    
    private static RecordStore openRMS(String name)
    {
        try
        {
            return RecordStore.openRecordStore(name, true);            
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed to open record store", e);
            return null;
        }
    }
    
    private static void closeRMS(RecordStore rms)
    {
        if (rms == null)
            return;
            
        String name = null;
        try
        {
            name = rms.getName();
            rms.closeRecordStore();
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed to close record store", e);
        }
    }
    
    private static void deleteRMS(String name)
    {
        try
        {
            RecordStore.deleteRecordStore(name);            
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed to delete record store", e);
        }
    }
    
    private static boolean addRMSRecords(RecordStore rms, int count, int size)
    {
        for (int i = 0; i < count; i++)
        {
            if (addRMSRecord(rms, new byte[size], i) <= 0)
                return false;
        }
        
        return true;
    }
    
    private static int addRMSRecord(RecordStore rms, byte[] data, int index)
    {
        if (rms == null)
            return 0;
            
        String info = "";
        String name = null;
        try
        {
            info = "getName";
            name = rms.getName();
            info = "getNumRecords";
            int oldRecordCount = rms.getNumRecords();
            info = "getNextRecordID";
            int nextRecordId = rms.getNextRecordID();
            info = "getVersion";
            int oldVersion = rms.getVersion();
            
            //add the record and make sure the returned record ID is valid
            info = "addRecord";
            long startTime = System.currentTimeMillis();
            int recordId = rms.addRecord(data, 0, data.length);
            long endTime = System.currentTimeMillis();
            averageAddTime = ((averageAddTime * index) + (endTime - startTime)) / (index + 1);
            if (recordId <= 0)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): ID of new record ('" + recordId + "') is invalid");

            //make sure the returned record ID is as expected
            if (recordId != nextRecordId)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): ID of new record ('" + recordId + "') doesn't match expected record ID ('" + nextRecordId + "')");

            //make sure the number of records present has increased by 1
            info = "getNumRecords";
            int newRecordCount = rms.getNumRecords();
            if (newRecordCount != (oldRecordCount + 1))
                Logger.logIssue(Logger.SEVERITY_HIGH, "RMS ('" + name + "'): unexpected number of records reported ('" + newRecordCount + "') after adding a record - there should now be " + (oldRecordCount + 1) + " records present");

            //make sure the version was updated
            info = "getVersion";
            int newVersion = rms.getVersion();
            if (newVersion == oldVersion)
                Logger.logIssue(Logger.SEVERITY_HIGH, "RMS ('" + name + "'): RMS version number hasn't been updated after adding a record");
            
            //make sure the size of the record is correct
            info = "getRecordSize";
            int recordSize = rms.getRecordSize(recordId);
            if (recordSize != data.length)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): size of new record ('" + recordSize + "' bytes) doesn't match expected record size ('" + data.length + "' bytes)");
            
            //read the record and make sure the data is correct
            info = "getRecord";
            startTime = System.currentTimeMillis();
            byte[] recordData = rms.getRecord(recordId);
            endTime = System.currentTimeMillis();
            averageGetTime = ((averageGetTime * index) + (endTime - startTime)) / (index + 1);
            if (! CommonUtils.isEquals(recordData, data))
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): contents of new record doesn't match the data that was actually written to the record");
            
            return recordId;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed (at '" + info + "') to add new record", e);
            return 0;
        }
    }
    
    private static boolean setRMSRecords(RecordStore rms, int start, int count, int size)
    {
        for (int i = 0; i < count; i++)
        {
            if (! setRMSRecord(rms, (start + i), new byte[size], i))
                return false;
        }
        
        return true;
    }
    
    private static boolean setRMSRecord(RecordStore rms, int recordId, byte[] data, int index)
    {
        if (rms == null)
            return false;
            
        String info = "";
        String name = null;
        try
        {
            info = "getName";
            name = rms.getName();
            info = "getNumRecords";
            int oldRecordCount = rms.getNumRecords();
            info = "getVersion";
            int oldVersion = rms.getVersion();
            
            //set the record
            info = "setRecord";
            long startTime = System.currentTimeMillis();
            rms.setRecord(recordId, data, 0, data.length);
            long endTime = System.currentTimeMillis();
            averageSetTime = ((averageSetTime * index) + (endTime - startTime)) / (index + 1);

            //make sure the number of records present remained the same
            info = "getNumRecords";
            int newRecordCount = rms.getNumRecords();
            if (newRecordCount != oldRecordCount)
                Logger.logIssue(Logger.SEVERITY_HIGH, "RMS ('" + name + "'): unexpected number of records reported ('" + newRecordCount + "') after updating a record - there should still be " + oldRecordCount + " records present");

            //make sure the version was updated
            info = "getVersion";
            int newVersion = rms.getVersion();
            if (newVersion == oldVersion)
                Logger.logIssue(Logger.SEVERITY_HIGH, "RMS ('" + name + "'): RMS version number hasn't been updated after updating a record");
            
            //make sure the size of the record is correct
            info = "getRecordSize";
            int recordSize = rms.getRecordSize(recordId);
            if (recordSize != data.length)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): size of updated record ('" + recordSize + "' bytes) doesn't match expected record size ('" + data.length + "' bytes)");
            
            //read the record and make sure the data is correct
            info = "getRecord";
            byte[] recordData = rms.getRecord(recordId);
            if (! CommonUtils.isEquals(recordData, data))
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): contents of updated record doesn't match the data that was actually written to the record");
            
            return true;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed (at '" + info + "') to set record", e);
            return false;
        }
    }
    
    private static boolean deleteRMSRecords(RecordStore rms, int start, int count)
    {
        for (int i = 0; i < count; i++)
        {
            if (! deleteRMSRecord(rms, (start + i), i))
                return false;
        }
        
        return true;
    }
    
    private static boolean deleteRMSRecord(RecordStore rms, int recordId, int index)
    {
        if (rms == null)
            return false;
            
        String info = "";
        String name = null;
        try
        {
            info = "getName";
            name = rms.getName();
            info = "getNumRecords";
            int oldRecordCount = rms.getNumRecords();
            info = "getVersion";
            int oldVersion = rms.getVersion();
            
            //delete the record
            info = "deleteRecord";
            long startTime = System.currentTimeMillis();
            rms.deleteRecord(recordId);
            long endTime = System.currentTimeMillis();
            averageDeleteTime = ((averageDeleteTime * index) + (endTime - startTime)) / (index + 1);

            //make sure the number of records present has decreased by 1
            info = "getNumRecords";
            int newRecordCount = rms.getNumRecords();
            if (newRecordCount != (oldRecordCount - 1))
                Logger.logIssue(Logger.SEVERITY_HIGH, "RMS ('" + name + "'): unexpected number of records reported ('" + newRecordCount + "') after deleting a record - there should now be " + (oldRecordCount - 1) + " records present");

            //make sure the version was updated
            info = "getVersion";
            int newVersion = rms.getVersion();
            if (newVersion == oldVersion)
                Logger.logIssue(Logger.SEVERITY_HIGH, "RMS ('" + name + "'): RMS version number hasn't been updated after deleting a record");
            
            try
            {
                //try to read the record and make that it isn't present
                info = "getRecord";
                rms.getRecord(recordId);
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): record ('" + recordId + "') is still present after deleting it");
            }
            catch (Throwable e)
            {
                //ignore - this is expected as the record has been deleted
            }
            
            return true;
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed (at '" + info + "') to delete record", e);
            return false;
        }
    }
    
    private static void enumRMSRecords(RecordStore rms)
    {
        if (rms == null)
            return;
            
        String info = "";
        String name = null;
        RecordEnumeration recordEnum = null;
        try
        {
            info = "getName";
            name = rms.getName();
            info = "getNumRecords";
            int recordCount = rms.getNumRecords();

            //enumerate all records
            info = "enumerateRecords(null, null, false)";
            long startTime = System.currentTimeMillis();
            recordEnum =  rms.enumerateRecords(null, null, false);
            if (recordEnum == null)
            {
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): could not retrieve record enumeration");
                return;
            }

            int count = 0;
            while (recordEnum.hasNextElement())
            {
                recordEnum.nextRecordId();
                count++;
            }
            long endTime = System.currentTimeMillis();
            averageEnumTime = (endTime - startTime);
            
            //make sure we got all records
            int enumCount = recordEnum.numRecords();
            if (enumCount != count)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): record enumeration reports an incorrect number of records (" + enumCount + " records reported, " + count + " records actually present)");
            if (count != recordCount)
                Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): record enumeration failed to enumerate all records (" + count + " records enumerated, " + recordCount + " records actually present)");
        }
        catch (Throwable e)
        {
            Logger.logIssue(Logger.SEVERITY_CRITICAL, "RMS ('" + name + "'): failed (at '" + info + "') to enumerate records", e);
        }
        finally
        {
            if (recordEnum != null)
                recordEnum.destroy();
        }
    }
}
