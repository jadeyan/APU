/**
 * Copyright © 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.engine;


import java.io.*;

import net.cp.syncml.client.devinfo.*;
import net.cp.syncml.client.store.*;
import net.cp.syncml.client.util.Logger;
import net.cp.syncml.client.util.wbxml.*;


/**
 * A class defining a WBXML codepage for SyncML DevInf.
 * 
 * This codepage conforms with SyncML DevInf versions 1.1 and 1.2.
 *
 * @author Denis Evoy
 */
public class DevInfCodepage extends Codepage
{
    //DevInf content types
    public static final String CT_WBXML =                      "application/vnd.syncml-devinf+wbxml";
    public static final String CT_XML =                        "application/vnd.syncml-devinf+xml";

    //DevInf DTD versions
    public static final String VER_DTD_1_1 =                   "1.1";
    public static final String VER_DTD_1_2 =                   "1.2";

    //DevInf URIs
    public static final String DOC_URI_1_1 =                   "./devinf11";
    public static final String DOC_URI_1_2 =                   "./devinf12";
    
    
    //DevInf DTD public IDs
    private static final String DOC_ID_1_1 =                   "-//SYNCML//DTD DevInf 1.1//EN"; 
    private static final String DOC_ID_1_2 =                   "-//SYNCML//DTD DevInf 1.2//EN"; 
    
    //DevInf v1.1 codepage definition
    private static final byte TAG_CT_CAP =                     0x05;
    private static final byte TAG_CT_TYPE =                    0x06;
    private static final byte TAG_DATASTORE =                  0x07;
    private static final byte TAG_DATATYPE =                   0x08;
    private static final byte TAG_DEV_ID =                     0x09;
    private static final byte TAG_DEV_INF =                    0x0A;
    private static final byte TAG_DEV_TYPE =                   0x0B;
    private static final byte TAG_DISPLAY_NAME =               0x0C;
    private static final byte TAG_DS_MEM =                     0x0D;
    private static final byte TAG_EXT =                        0x0E;
    private static final byte TAG_FW_VERSION =                 0x0F;
    private static final byte TAG_HW_VERSION =                 0x10;
    private static final byte TAG_MANUFACTURER =               0x11;
    private static final byte TAG_MAX_GUID_SIZE =              0x12;
    private static final byte TAG_MAX_ID =                     0x13;
    private static final byte TAG_MAX_MEM =                    0x14;
    private static final byte TAG_MODEL =                      0x15;
    private static final byte TAG_OEM =                        0x16;
    private static final byte TAG_PARAM_NAME =                 0x17;
    private static final byte TAG_PROP_NAME =                  0x18;
    private static final byte TAG_RX =                         0x19;
    private static final byte TAG_RX_PREF =                    0x1A;
    private static final byte TAG_SHARED_MEM =                 0x1B;
    private static final byte TAG_MAX_SIZE =                   0x1C;     //"Size" in v1.1
    private static final byte TAG_SOURCE_REF =                 0x1D;
    private static final byte TAG_SW_VERSION =                 0x1E;
    private static final byte TAG_SYNC_CAP =                   0x1F;
    private static final byte TAG_SYNC_TYPE =                  0x20;
    private static final byte TAG_TX =                         0x21;
    private static final byte TAG_TX_PREF =                    0x22;
    private static final byte TAG_VAL_ENUM =                   0x23;
    private static final byte TAG_VER_CT =                     0x24;
    private static final byte TAG_VER_DTD =                    0x25;
    private static final byte TAG_EXT_NAME =                   0x26;
    private static final byte TAG_EXT_VALUE =                  0x27;
    private static final byte TAG_UTC =                        0x28;
    private static final byte TAG_SUPPORT_NUMBER_OF_CHANGES =  0x29;
    private static final byte TAG_SUPPORT_LARGE_OBJECTS =      0x2A;
    
    //DevInf v1.2 codepage definition
    private static final byte TAG_PROPERTY =                   0x2B;
    private static final byte TAG_PROP_PARAM =                 0x2C;
    private static final byte TAG_MAX_OCCUR =                  0x2D;
    private static final byte TAG_NO_TRUNCATE =                0x2E;
    //private static final byte TAG_RESERVED =                   0x2F;     //reserved for future use
    //private static final byte TAG_FILTER_RX =                  0x30;
    //private static final byte TAG_FILTER_CAP =                 0x31;
    //private static final byte TAG_FILTER_KEYWORD =             0x32;
    private static final byte TAG_FIELD_LEVEL =                0x33;
    private static final byte TAG_SUPPORT_HIERARCH_SYNC =      0x34;

    private static final String[] TAG_NAMES =                  { "CTCap", "CTType", "DataStore", "DataType", "DevId", "DevInf", "DevTyp", "DisplayName", "DSMem", "Ext", "FwV", "HwV", "Man", "MaxGuidSize", "MaxId", "MaxMem", "Mod", "OEM", "ParamName", "PropName", "Rx", "Rx-Pref", "SharedMem", "Size", "SourceRef", "SwV", "SyncCap", "SyncType", "Tx", "Tx-Pref", "ValEnum", "VerCt", "VerDtd", "XNam", "XVal", "UTC", "SupportNumberOfChanges", "SupportLargeObjs", "Property", "PropParam", "MaxOccur", "NoTruncate", "Reserved", "Filter-Rx", "FilterCap", "FilterKeyword", "FieldLevel", "SupportHierarchicalSync" };

    
    /** 
     * Creates a new SyncML DevInf codepage
     * 
     * @param logger the logger used to log activity. 
     */
    public DevInfCodepage(Logger logger)
    {
        super(logger);
    }
    

    public String[] getTagNames()
    {
        return TAG_NAMES;
    }
    
    
    /**
     * Returns the specified device information in WBXML format.
     * 
     * @param device        the device information to write.
     * @param stores        the record store information to write.
     * @param dtdVersion    the {@link #VER_DTD_1_1 DTD version} to use.  
     * @return a buffer containing the device information in WBXML format.
     * @throws IOException      if the device information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public byte[] getDevinf(Device device, RecordStore[] stores, String dtdVersion)
        throws WbxmlException, IOException
    {
        if (log != null)
            log.info("Building device information as WBXML opaque data");
        
        ByteArrayOutputStream devinfStream = new ByteArrayOutputStream(256);
        writeDevinf(devinfStream, device, stores, dtdVersion);
        byte[] devinfData = devinfStream.toByteArray();
        
        try
        {
            devinfStream.close();
        }
        catch (IOException e)
        {
            //ignore
        }
        
        return devinfData;
    }
    
    /**
     * Writes the specified device information in WBXML format to the specified output stream.
     * 
     * @param outputStream  the output stream to write the command to.
     * @param device        the device information to write.
     * @param stores        the record store information to write.
     * @param dtdVersion    the {@link #VER_DTD_1_1 DTD version} to use.  
     * @throws IOException      if the device information couldn't be written.
     * @throws WbxmlException   if there was a WBXML formatting error.
     */
    public void writeDevinf(OutputStream outputStream, Device device, RecordStore[] stores, String dtdVersion)
        throws WbxmlException, IOException
    {
        //determine which DevInf version to use
        String docId;
        if (dtdVersion.equals(VER_DTD_1_1))
            docId = DOC_ID_1_1;
        else if (dtdVersion.equals(VER_DTD_1_2))
            docId = DOC_ID_1_2;
        else
            throw new WbxmlException("invalid DTD version specified: " + dtdVersion);

        //write WBXML header
        Wbxml.writeHeader(outputStream, Wbxml.VERSION_1_2, Wbxml.CHARSET_UTF8, docId);
        
        //get the DevInf details
        String devId = device.getDeviceID();
        String devType = device.getDeviceType();
        String devMan = device.getManufacturer();
        String devModel = device.getModel();
        String devOem = device.getOem();
        String devHwVer = device.getHardwareVersion();
        String devSwVer = device.getSoftwareVersion();
        String devFwVer = device.getFirmwareVersion();
        boolean largeObjs = device.getCapabilities().isLargeObjectsSupported();
        boolean numOfChanges = device.getCapabilities().isNumberOfChangesSupported();
        boolean utcSupported = device.getCapabilities().isUtcSupported();
        
        writeTag(outputStream, TAG_DEV_INF, true);

        writeTag(outputStream, TAG_VER_DTD, dtdVersion);
        
        writeTag(outputStream, TAG_DEV_ID, devId);
        
        writeTag(outputStream, TAG_DEV_TYPE, devType);
        
        if ( (devMan != null) && (devMan.length() > 0) )
            writeTag(outputStream, TAG_MANUFACTURER, devMan);
        
        if ( (devModel != null) && (devModel.length() > 0) )
            writeTag(outputStream, TAG_MODEL, devModel);
        
        if ( (devOem != null) && (devOem.length() > 0) )
            writeTag(outputStream, TAG_OEM, devOem);
        
        if ( (devSwVer != null) && (devSwVer.length() > 0) )
            writeTag(outputStream, TAG_SW_VERSION, devSwVer);
        
        if ( (devHwVer != null) && (devHwVer.length() > 0) )
            writeTag(outputStream, TAG_HW_VERSION, devHwVer);
        
        if ( (devFwVer != null) && (devFwVer.length() > 0) )
            writeTag(outputStream, TAG_FW_VERSION, devFwVer);
        
        if (largeObjs)
            writeTag(outputStream, TAG_SUPPORT_LARGE_OBJECTS, false);
        
        if (numOfChanges)
            writeTag(outputStream, TAG_SUPPORT_NUMBER_OF_CHANGES, false);
        
        if (utcSupported)
            writeTag(outputStream, TAG_UTC, false);

        //write information for each data store
        for (int i = 0; i < stores.length; i++)
        {
            RecordStore store = stores[i];
            
            writeTag(outputStream, TAG_DATASTORE, true);

            writeTag(outputStream, TAG_SOURCE_REF, store.getClientURI());

            String displayName = store.getDisplayName();
            if ( (displayName != null) && (displayName.length() > 0) )
                writeTag(outputStream, TAG_DISPLAY_NAME, displayName);
            
            RecordStoreCapabilities storeCaps = store.getCapabilities();
            
            int maxGuidSize = storeCaps.getMaxGuidSize();
            if (maxGuidSize >= 0)
                writeTag(outputStream, TAG_MAX_GUID_SIZE, Integer.toString(maxGuidSize));
            
            long maxFreeMemory = storeCaps.getMaxFreeMemory();
            long maxRecordCount = storeCaps.getMaxRecordCount();
            boolean isSharedMem = storeCaps.isSharedMem();
            if ( (isSharedMem) || (maxFreeMemory > 0) || (maxRecordCount > 0) )
            {
                writeTag(outputStream, TAG_DS_MEM, true);

                if (isSharedMem)
                    writeTag(outputStream, TAG_SHARED_MEM, false);
                if (maxFreeMemory > 0)
                    writeTag(outputStream, TAG_MAX_MEM, Long.toString(maxFreeMemory));
                if (maxRecordCount > 0)
                    writeTag(outputStream, TAG_MAX_ID, Long.toString(maxRecordCount));
                
                writeTagEnd(outputStream, TAG_DS_MEM);
            }

            writeTag(outputStream, TAG_SYNC_CAP, true);
            int[] syncTypes = store.getCapabilities().getSupportedSyncTypes();
            for (int j = 0; j < syncTypes.length; j++)
                writeTag(outputStream, TAG_SYNC_TYPE, Integer.toString(syncTypes[j]));
            writeTagEnd(outputStream, TAG_SYNC_CAP);
            
            ContentType[] supportedReceivedCts = storeCaps.getSupportedReceivedContentTypes();
            if ( (supportedReceivedCts != null) && (supportedReceivedCts.length > 0) )
            {
                writeTag(outputStream, TAG_RX, true);
                for (int j = 0; j < supportedReceivedCts.length; j++)
                    writeCt(outputStream, supportedReceivedCts[j]);
                writeTagEnd(outputStream, TAG_RX);
            }

            ContentType preferredReceivedCt = storeCaps.getPreferredReceivedContentType();
            writeTag(outputStream, TAG_RX_PREF, true);
            writeCt(outputStream, preferredReceivedCt);
            writeTagEnd(outputStream, TAG_RX_PREF);
            
            ContentType[] supportedTransmittedCts = storeCaps.getSupportedTransmittedContentTypes();
            if ( (supportedTransmittedCts != null) && (supportedTransmittedCts.length > 0) )
            {
                writeTag(outputStream, TAG_TX, true);
                for (int j = 0; j < supportedTransmittedCts.length; j++)
                    writeCt(outputStream, supportedTransmittedCts[j]);
                writeTagEnd(outputStream, TAG_TX);
            }

            ContentType preferredTransmittedCt = storeCaps.getPreferredTransmittedContentType();
            writeTag(outputStream, TAG_TX_PREF, true);
            writeCt(outputStream, preferredTransmittedCt);
            writeTagEnd(outputStream, TAG_TX_PREF);

            if (dtdVersion.equals(VER_DTD_1_2))
            {
                if (storeCaps.isHierarchicalSyncSupported())
                    writeTag(outputStream, TAG_SUPPORT_HIERARCH_SYNC, false);

                ContentTypeCapabilities[] ctCapabilities = storeCaps.getContentTypeCapabilities();
                writeCtCaps(outputStream, ctCapabilities, dtdVersion);
            }
            
            writeTagEnd(outputStream, TAG_DATASTORE);
        }

        if (dtdVersion.equals(VER_DTD_1_1))
        {
            for (int i = 0; i < stores.length; i++)
                writeCtCaps(outputStream, stores[i].getCapabilities().getContentTypeCapabilities(), dtdVersion);
        }
        
        //write device extensions
        Extension[] extensions = device.getCapabilities().getExtensions();
        if ( (extensions != null) && (extensions.length > 0) )
        {
            writeTag(outputStream, TAG_EXT, true);
            
            for (int i = 0; i < extensions.length; i++)
            {
                Extension extension = extensions[i];
                writeTag(outputStream, TAG_EXT_NAME, extension.getName());
                
                String[] extValues = extension.getValues();
                if (extValues != null)
                {
                    for (int j = 0; j < extValues.length; j++)
                        writeTag(outputStream, TAG_EXT_VALUE, extValues[j]);
                }
            }
            
            writeTagEnd(outputStream, TAG_EXT);
        }

        writeTagEnd(outputStream, TAG_DEV_INF);
    }
    
    
    /* Writes the specified content type to the specified output stream. */
    private void writeCt(OutputStream outputStream, ContentType contentType)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_CT_TYPE, contentType.toString());

        String ctVersion = contentType.getVersion();
        if ( (ctVersion != null) && (ctVersion.length() > 0) )
            writeTag(outputStream, TAG_VER_CT, ctVersion);
    }    

    /* Writes the specified content type capabilities to the specified output stream. */
    private void writeCtCaps(OutputStream outputStream, ContentTypeCapabilities[] ctCapabilities, String dtdVersion)
        throws WbxmlException, IOException
    {
        if ( (ctCapabilities == null) || (ctCapabilities.length <= 0) )
            return;
        
        if (dtdVersion.equals(VER_DTD_1_1))
            writeTag(outputStream, TAG_CT_CAP, true);
        
        for (int i = 0; i < ctCapabilities.length; i++)
        {
            ContentTypeCapabilities cap = ctCapabilities[i];
            
            if (dtdVersion.equals(VER_DTD_1_2))
                writeTag(outputStream, TAG_CT_CAP, true);

            writeTag(outputStream, TAG_CT_TYPE, cap.toString());
            
            if (dtdVersion.equals(VER_DTD_1_2))
            {
                String ctVersion = cap.getVersion();
                if ( (ctVersion != null) && (ctVersion.length() > 0) )
                    writeTag(outputStream, TAG_VER_CT, ctVersion);

                if (cap.isFieldLevelSupported())
                    writeTag(outputStream, TAG_FIELD_LEVEL, false);
            }

            ContentTypeProperty[] capProps = cap.getProperties();
            if (capProps != null)
            {
                for (int j = 0; j < capProps.length; j++)
                {
                    if (dtdVersion.equals(VER_DTD_1_2))
                        writeTag(outputStream, TAG_PROPERTY, true);
                        
                    writeCtProperty(outputStream, capProps[j], dtdVersion);

                    if (dtdVersion.equals(VER_DTD_1_2))
                        writeTagEnd(outputStream, TAG_PROPERTY);
                }
            }
            
            if (dtdVersion.equals(VER_DTD_1_2))
                writeTagEnd(outputStream, TAG_CT_CAP);
        }
        
        if (dtdVersion.equals(VER_DTD_1_1))
            writeTagEnd(outputStream, TAG_CT_CAP);
    }
    
    /* Writes the specified content type property to the specified output stream. */
    private void writeCtProperty(OutputStream outputStream, ContentTypeProperty ctProp, String dtdVersion)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_PROP_NAME, ctProp.getName());

        String propDisplayname = ctProp.getDisplayName();
        if ( (propDisplayname != null) && (propDisplayname.length() >= 0) )
            writeTag(outputStream, TAG_DISPLAY_NAME, propDisplayname);
        
        String propDataType = ctProp.getDataType();
        if ( (propDataType != null) && (propDataType.length() >= 0) )
            writeTag(outputStream, TAG_DATATYPE, propDataType);

        int propDataSize = ctProp.getMaxSize();
        if (propDataSize > 0)
            writeTag(outputStream, TAG_MAX_SIZE, Integer.toString(propDataSize));

        if (dtdVersion.equals(VER_DTD_1_1))
        {
            if (ctProp.isNoTruncate())
                writeTag(outputStream, TAG_NO_TRUNCATE, false);
    
            int propMaxOccurance = ctProp.getMaxOccurance();
            if (propMaxOccurance > 0)
                writeTag(outputStream, TAG_MAX_OCCUR, Integer.toString(propMaxOccurance));
        }
        
        String[] ctPropEnumValues = ctProp.getEnumValues();
        if (ctPropEnumValues != null)
        {
            for (int i = 0; i < ctPropEnumValues.length; i++)
                writeTag(outputStream, TAG_VAL_ENUM, ctPropEnumValues[i]);
        }

        ContentTypeProperty.Parameter[] ctPropParams = ctProp.getParameters();
        if (ctPropParams != null)
        {
            for (int i = 0; i < ctPropParams.length; i++)
            {
                if (dtdVersion.equals(VER_DTD_1_2))
                    writeTag(outputStream, TAG_PROP_PARAM, true);
                    
                writeCtPropertyParam(outputStream, ctPropParams[i], dtdVersion);

                if (dtdVersion.equals(VER_DTD_1_2))
                    writeTagEnd(outputStream, TAG_PROP_PARAM);
            }
        }
    }
    
    /* Writes the specified content type property parameter to the specified output stream. */
    private void writeCtPropertyParam(OutputStream outputStream, ContentTypeProperty.Parameter ctPropParam, String dtdVersion)
        throws WbxmlException, IOException
    {
        writeTag(outputStream, TAG_PARAM_NAME, ctPropParam.getName());
        
        String paramDisplayname = ctPropParam.getDisplayName();
        if ( (paramDisplayname != null) && (paramDisplayname.length() >= 0) )
            writeTag(outputStream, TAG_DISPLAY_NAME, paramDisplayname);
        
        String propParamDataType = ctPropParam.getDataType();
        if ( (propParamDataType != null) && (propParamDataType.length() >= 0) )
            writeTag(outputStream, TAG_DATATYPE, propParamDataType);

        if (dtdVersion.equals(VER_DTD_1_1))
        {
            int propParamDataSize = ctPropParam.getMaxSize();
            if (propParamDataSize > 0)
                writeTag(outputStream, TAG_MAX_SIZE, Integer.toString(propParamDataSize));
        }
      
        String[] ctPropParamEnumValues = ctPropParam.getEnumValues();
        if (ctPropParamEnumValues != null)
        {
            for (int l = 0; l < ctPropParamEnumValues.length; l++)
                writeTag(outputStream, TAG_VAL_ENUM, ctPropParamEnumValues[l]);
        }
    }        
}
