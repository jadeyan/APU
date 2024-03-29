/**
 * Copyright � 2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.devinfo;


import net.cp.mtk.common.devinfo.DeviceInfo;
import net.cp.mtk.common.devinfo.NetworkIdentifier;


/**
 * A class providing various details about a Huawei devices.
 * 
 * @author Adrian Lanzillotta
 */
public class HuaweiDeviceInfo extends DeviceInfo
{
    /** Create a new device info object. */
    public HuaweiDeviceInfo()
	{
	    super();
	}
	
    /** 
     * Returns the manufacturer of the device.
     * 
     * @return the manufacturer of the device (i.e. "Motorola").
     */
    public String getManufacturer()
    {
        return "Huawei";
    }

    /** 
     * Returns the model of the device.
     * 
     * @return the model of the device, or null if it is unknown.
     */
    public String getModel()
    {
        return getProperty("microedition.platform");
    }

    /** 
     * Returns the type of network the device is connected to.
     * 
     * Note: determining the network type is not supported - we only support determining if a network is available.
     * 
     * @return the type of network (NETWORK_TYPE_XXX) the device is connected to, or NETWORK_TYPE_UNKNOWN if it is not known.
     */
	public byte getNetworkType()
	{
		return super.getNetworkType();
	}
    
    /** 
     * Returns the identifier of the network that the device is currently connected.
     * 
     * Note: determining the network ID isn't supported.
     * 
     * @return the identifier of the network that the device is currently connected to, or null if the network is unknown.
     */
	public NetworkIdentifier getNetworkId()
    {
        return super.getNetworkId();
    }

    /** 
     * Returns the roaming state of the device.
     * 
     * Note: determining the roaming state isn't supported.
     * 
     * @return the roaming state (ROAMING_XXX) of the device, or ROAMING_UNKNOWN if it is not known.
     */
    public byte getRoamingState()
    {
        return super.getRoamingState();
    }

    /** 
     * Returns the IEMI of the device.
     * 
     * @return the IEMI of the device, or null if it is not known.
     */
	public String getIMEI()
	{
		String deviceIMEI = null;
		deviceIMEI = System.getProperty("com.huawei.properties.IMEI");
		if (deviceIMEI != null)
			return deviceIMEI;
		
        return super.getIMEI();
	}

    /** 
     * Returns the IMSI of the SIM card on the device.
     * 
     * Note: determining the IMSI isn't supported.
     * 
     * @return the IMSI of the device, or null if it is not known.
     */
    public String getIMSI()
    {
		String deviceIMSI = null;
		deviceIMSI = System.getProperty("com.huawei.properties.IMSI");
		if (deviceIMSI != null)
			return deviceIMSI;
        
		return super.getIMSI();
    }

    /** 
     * Returns the MSISDN associated with the SIM card on the device.
     * 
     * @return the users MSISDN, or null if it is not known.
     */
    public String getMSISDN()
    {
    	
		String deviceMSISDN = null;
		deviceMSISDN = System.getProperty("com.huawei.properties.MSISDN");
		if (deviceMSISDN != null)
			return deviceMSISDN;
    	
    	return super.getMSISDN();
    }

    /** 
     * Returns the Push-To-Talk ID associated with the SIM card on the device.
     * 
     * @return the users Push-To-Talk ID, or null if it is not known.
     */
    public String getPTTId()
    {
		String devicePTT = null;
		devicePTT = System.getProperty("com.huawei.properties.PTT");
		if (devicePTT != null)
			return devicePTT;
        
    	return super.getPTTId();
    }
}
