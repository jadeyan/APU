/**
 * Copyright � 2004-2007 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.devinfo;


/**
 * A class representing the capabilities of a device. <br/><br/>
 * 
 * These capabilities define the content types handled by the device along with the 
 * functionality supported by the device.
 * 
 * @see Device#getCapabilities()
 *
 * @author Denis Evoy
 */
public class DeviceCapabilities
{
    private boolean utcSupported;                       //indicates if the device supports UTC based time
    private boolean largeObjectsSupported;              //indicates if the device supports handling large objects
    private boolean numberOfChangesSupported;           //indicates if the device supports handling "NumberOfChanges" information from the server
    private Extension[] extensions;                     //the non-standard extensions supported by the device (if any)

    
    /**
     * Creates a new set of capabilities with the specified content types and support.
     * 
     * @param supportedExtensions       the extensions supported by the device. May be null or empty.
     * @param supportsUtc               indicates if the device supports UTC based time.
     * @param supportsLargeObjects      indicates if the device supports handling large objects.
     * @param supportsNumberOfChanges   indicates if the device supports handling "NumberOfChanges" information from the server.
     */
    public DeviceCapabilities(Extension[] supportedExtensions, boolean supportsUtc, boolean supportsLargeObjects, boolean supportsNumberOfChanges)
    {
        extensions = supportedExtensions;
        utcSupported = supportsUtc;
        largeObjectsSupported = supportsLargeObjects;
        numberOfChangesSupported = supportsNumberOfChanges;
    }

    
    /**
     * Returns the non-standard experimental extensions supported by the device.
     * 
     * @return The non-standard experimental extensions supported by the device. May be null or empty.
     */
    public Extension[] getExtensions()
    {
        return extensions;
    }

    /**
     * Returns whether or not the device supports handling large objects.
     * 
     * @return <code>true</code> if the device supports handling large objects.
     */
    public boolean isLargeObjectsSupported()
    {
        return largeObjectsSupported;
    }

    /**
     * Returns whether or not if the device supports handling "NumberOfChanges" information from the server.
     * 
     * @return <code>true</code> if the device supports handling "NumberOfChanges" information from the server.
     */
    public boolean isNumberOfChangesSupported()
    {
        return numberOfChangesSupported;
    }

    /**
     * Returns whether or not the device supports UTC based time.
     * 
     * @return <code>true</code> if the device supports UTC based time.
     */
    public boolean isUtcSupported()
    {
        return utcSupported;
    }
}
