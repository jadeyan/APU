/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

import java.util.Random;
import java.util.Vector;

import net.cp.syncml.client.devinfo.*;

/**
 * A class representing a mobile phone device for use in the SyncML stack.
 *
 * @author Denis Evoy
 */
public class MobileDevice implements Device
{
    private String id;                                      //the unique ID of the device
    private EngineSettings settings;                        //the user settings
    private String appVersion;                              //the full version number of the application

    private DeviceCapabilities capabilities;                //the capabilities of the device

    private Vector<String> memoryCardIds;                            //the IDs of memory cards currently plugged into the phone

    private String clientNonce;                             //the client nonce that should be sent to the server


    /**
     * Creates a new mobile device.
     *
     * @param deviceId          the unique ID of the device.
     * @param currentSettings   the application settings containing the device properties.
     * @param versionNumber     the full version number of the application.
     */
    public MobileDevice(String deviceId, EngineSettings currentSettings, String versionNumber)
    {
        id = deviceId;
        settings = currentSettings;
        appVersion = versionNumber;

        capabilities = null;

        memoryCardIds = null;

        //generate a new random nonce value
        Random rand = new Random( System.currentTimeMillis() );
        long hash = deviceId.hashCode() + rand.nextLong();
        clientNonce = Long.toString(hash, 16);
    }


    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getDeviceID()
     */
    public String getDeviceID()
    {
        return id;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getDeviceType()
     */
    public String getDeviceType()
    {
        return settings.deviceType;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getManufacturer()
     */
    public String getManufacturer()
    {
        return settings.deviceManf;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getModel()
     */
    public String getModel()
    {
        return settings.deviceModel;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getOem()
     */
    public String getOem()
    {
        return settings.deviceOem;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getHardwareVersion()
     */
    public String getHardwareVersion()
    {
        return settings.deviceHwVersion;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getFirmwareVersion()
     */
    public String getFirmwareVersion()
    {
        return settings.deviceFwVersion;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getSoftwareVersion()
     */
    public String getSoftwareVersion()
    {
        //return the actual version number as it may be used by the server to identify the correct PAB-vCard
        //mappings to use

        String version = "";

        //#ifdef appversion
        //#expand version = "%appversion%";
        //#endif

        return version;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getCapabilities()
     */
    public DeviceCapabilities getCapabilities()
    {
        if (capabilities == null)
        {
            //add an extension defining the name of the application
            Vector<Extension> extensionList = new Vector<Extension>();
            extensionList.addElement( new Extension(Device.DEVICE_EXT_CP_APP_NAME, "Mobile Client") );

            //add an extension defining the full version of the application
            if ( (appVersion != null) && (appVersion.length() > 0) )
                extensionList.addElement( new Extension(Device.DEVICE_EXT_CP_APP_VERSION, appVersion) );

            //add an extension defining the capability ID of the application
            String clientCapability = settings.appCapabilityId;
            if ( (clientCapability != null) && (clientCapability.length() > 0) )
                extensionList.addElement( new Extension(Device.DEVICE_EXT_CP_CAP_ID, clientCapability) );

            //device display name as the server cannot always rely on manufacturer and model to build a proper display name
            if ( (settings.deviceDisplayName != null) && (settings.deviceDisplayName.length() > 0) )
                extensionList.addElement( new Extension(Device.DEVICE_EXT_CP_DEVICE_NAME, settings.deviceDisplayName) );

            //add the unique IDs of any memory cards attached to the device
            if ( (memoryCardIds != null) && (memoryCardIds.size() > 0) )
            {
                String[] memCards = new String[memoryCardIds.size()];
                for (int j = 0; j < memoryCardIds.size(); j++)
                    memCards[j] = (String)memoryCardIds.elementAt(j);

                   extensionList.addElement( new Extension(Device.DEVICE_EXT_CP_MEM_CARD_IDS, memCards) );
               }

            //assemble the extensions and create the device capabilities
            Extension[] extensions = new Extension[extensionList.size()];
            for (int i = 0; i < extensionList.size(); i++)
                extensions[i] = (Extension)extensionList.elementAt(i);

            capabilities = new DeviceCapabilities(extensions, true, true, true);
        }

        return capabilities;
    }


    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#getClientNonce()
     */
    public String getClientNonce()
    {
        return clientNonce;
    }

    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#onClientNonceSent()
     */
    public void onClientNonceSent()
    {
        //persist the client nonce, taking care to keep the previous nonce
        if ( (clientNonce != null) && (clientNonce.length() > 0) )
        {
            settings.oldClientNonce = settings.clientNonce;
            settings.clientNonce = clientNonce;

            //write these values immediately in case the application terminates unexpectedly
            settings.writeStateSettings();
        }
    }


    /* (non-Javadoc)
     * @see net.cp.syncml.client.devinfo.Device#setServerNonce(java.lang.String)
     */
    public void setServerNonce(String nonce)
    {
        //persist the server nonce - this value will be saved when the session ends
        if ( (nonce != null) && (nonce.length() > 0) )
            settings.serverNonce = nonce;
    }


    /**
     * Registers a memory card with the specified ID as a card that is currently associated with the device.
     *
     * @param memCardId the unique ID of the memory card to register.
     */
    public void registerMemoryCardId(String memCardId)
    {
        if ( (memCardId != null) && (memCardId.length() > 0) )
        {
            if (memoryCardIds == null)
                memoryCardIds = new Vector<String>();

            memoryCardIds.addElement(memCardId);
        }
    }
}
