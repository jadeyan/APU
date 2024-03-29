/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test;


import java.util.Vector;
import java.util.Random;

import net.cp.syncml.client.devinfo.*;


/**
 * A class implementing a desktop device for the purposes of SyncML testing.
 *
 * @author Denis Evoy
 */
public class DesktopDevice implements Device
{
    private String id;                              //the unique ID of the device
    private String devType;                         //the type of the device
    private String devManf;                         //the manufacturer of the device
    private String devModel;                        //the model of the device
    private String softwareVersion;					//the software version
    private DeviceCapabilities deviceCaps;          //the capabilities of the device
    private String clientNonce;                     //the client nonce to use when challenging the server for authentication 
    
    
    DesktopDevice(String deviceId, String deviceType, String deviceManufacturer, String deviceModel, String applicationName, String applicationVersion, String swVersion, String applicationCapabilityID, String memCardId)
    {
        id = deviceId;
        devType = deviceType;
        devManf = deviceManufacturer;
        devModel = deviceModel;
        softwareVersion = swVersion;
        
        //build the device capability extensions
        Extension[] exts = null;
        Vector extensions = new Vector();
        if ( (applicationName != null) && (applicationName.length() > 0) )
            extensions.addElement( new Extension(Device.DEVICE_EXT_CP_APP_NAME, applicationName) );
        if ( (applicationVersion != null) && (applicationVersion.length() > 0) )
            extensions.addElement( new Extension(Device.DEVICE_EXT_CP_APP_VERSION, applicationVersion) );
        if ( (applicationCapabilityID != null) && (applicationCapabilityID.length() > 0) )
            extensions.addElement( new Extension(Device.DEVICE_EXT_CP_CAP_ID, applicationCapabilityID) );
        if ( (memCardId != null) && (memCardId.length() > 0) )
            extensions.addElement( new Extension(Device.DEVICE_EXT_CP_MEM_CARD_IDS, memCardId) );
        if (extensions.size() > 0)
        {
            exts = new Extension[extensions.size()];
            for (int i = 0; i < extensions.size(); i++)
                exts[i] = (Extension)extensions.get(i);
        }
            
        //build the capabilities of the device
        deviceCaps = new DeviceCapabilities(exts, true, true, true);
        
        //generate a new random nonce value
        Random rand = new Random(System.currentTimeMillis());
        long hash = deviceId.hashCode() + rand.nextLong();
        clientNonce = Long.toHexString(hash);
    }

    
    public String getDeviceID()
    {
        return id;
    }

    public String getDeviceType()
    {
        return devType;
    }

    public String getManufacturer()
    {
        return devManf;
    }

    public String getModel()
    {
        return devModel;
    }

    public String getOem()
    {
        return "None";
    }
    
    public String getHardwareVersion()
    {
        return "1.0";
    }

    public String getFirmwareVersion()
    {
        return "1.0";
    }

    public String getSoftwareVersion()
    {
        return softwareVersion;
    }

    public DeviceCapabilities getCapabilities()
    {
        return deviceCaps;
    }


    public String getClientNonce()
    {
        return clientNonce;
    }
    
    public void onClientNonceSent()
    {
        //not implemented
    }
    
    
    public void setServerNonce(String nonce)
    {
        //not implemented
    }
}
