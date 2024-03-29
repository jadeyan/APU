/**
 * Copyright � 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.j2me.devinfo;


import net.cp.mtk.common.devinfo.DeviceInfo;
import net.cp.mtk.common.devinfo.NetworkIdentifier;


/**
 * A class providing various details about a Nokia device.
 * 
 * @author Denis Evoy
 */
public class NokiaDeviceInfo extends DeviceInfo
{
    /** Create a new device info object. */
    public NokiaDeviceInfo()
	{
	    super();
	}
	
    /** 
     * Returns the manufacturer of the device.
     * 
     * @return the manufacturer of the device (i.e. "Nokia").
     */
    public String getManufacturer()
    {
        return "Nokia";
    }

    /** 
     * Returns the model of the device.
     * 
     * @return the model of the device, or null if it is unknown.
     */
    public String getModel()
    {
        String model = getProperty("DEVICE_MODEL");
        if (model != null)
            return model;
        
        return super.getModel();
    }

    /** 
     * Returns the type of network the device is connected to.
     * 
     * Note: determining the network type requires at least S60 3rd Edition FP 2 or S40 3rd Edition.
     * 
     * @return the type of network (NETWORK_TYPE_XXX) the device is connected to, or NETWORK_TYPE_UNKNOWN if it is not known.
     */
	public byte getNetworkType()
	{
		String availability = getProperty("com.nokia.mid.networkavailability");
        if ( (availability != null) && (availability.equalsIgnoreCase("unavailable")) )
            return NETWORK_TYPE_NONE;
		
        String access = getProperty("com.nokia.network.access");
        if ( (access == null) || (access.equalsIgnoreCase("na")) )
            return NETWORK_TYPE_UNKNOWN;
        else if (access.equalsIgnoreCase("wlan"))
            return NETWORK_TYPE_WLAN;
        else if (access.equalsIgnoreCase("bt_pan"))
            return NETWORK_TYPE_BLUETOOTH;
        else if ( (access.equalsIgnoreCase("pd")) || (access.equalsIgnoreCase("pd.EDGE")) || (access.equalsIgnoreCase("pd.3G")) || 
                  (access.equalsIgnoreCase("pd.HSDPA")) || (access.equalsIgnoreCase("pd.csd")) ) 
            return NETWORK_TYPE_MOBILE;
        
		return super.getNetworkType();
	}
    
    /** 
     * Returns the identifier of the network that the device is currently connected.
     * 
     * @return the identifier of the network that the device is currently connected to, or null if the network is unknown.
     */
	public NetworkIdentifier getNetworkId()
    {
	    //get the country code
        String mcc = getProperty("com.nokia.mid.countrycode");
        if (mcc == null)
            return null;

        //get the network ID
        String mnc = getProperty("com.nokia.mid.networkid");
        if (mnc != null)
        {
            //the ID is in the form "<NetworkId> (NetworkShortName>)", so we need to extract the ID
            for (int i = 0; i < mnc.length(); i++)
            {
                char c = mnc.charAt(i);
                if ( (c <= 32) || (c == '(') )
                {
                    mnc = mnc.substring(0, i);
                    break;
                }
            }
        }
        if ( (mnc == null) || (mnc.length() <= 0) )
            return null;
        
        //According to Nokia docs, System.getProperty("com.nokia.mid.countrycode") returns the two-letter country code 
        //defined in ISO-3361, but in practice some devices return the ITU country code
        char c = mcc.charAt(0);
        if ( (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') )
            return new NetworkIdentifier(mcc, null, mnc);

        return new NetworkIdentifier(null, mcc, mnc);
    }

    /** 
     * Returns the roaming state of the device.
     * 
     * Note: determining the roaming state requires at least S60 3rd Edition FP 2.
     * 
     * @return the roaming state (ROAMING_XXX) of the device, or ROAMING_UNKNOWN if it is not known.
     */
    public byte getRoamingState()
    {
        String status = getProperty("com.nokia.mid.networkstatus");
        if (status == null)
            return ROAMING_UNKNOWN;
        else if (status.equalsIgnoreCase("home"))
            return ROAMING_NO;
        else if (status.equalsIgnoreCase("roaming"))
            return ROAMING_YES;
        
        return super.getRoamingState();
    }

    /** 
     * Returns the IEMI of the device.
     * 
     * Note: determining the IMEI requires at least S60 3rd Edition FP 2 or S40 3rd Edition FP 1. On S40 devices, the 
     * application must also belong to the operator or manufacturer domains. See the following link for more details:
     *      http://wiki.forum.nokia.com/index.php/How_to_get_IMEI_in_Java_ME
     * 
     * @return the IEMI of the device, or null if it is not known.
     */
	public String getIMEI()
	{
        String imei = getProperty("com.nokia.mid.imei");
        if (imei != null)
            return imei;

        return super.getIMEI();
	}

    /** 
     * Returns the IMSI of the SIM card on the device.
     * 
     * Note: determining the IMSI requires at least S60 3rd Edition FP 2 or S40 3rd Edition FP 1. The application must 
     * also belong to the operator or manufacturer domains and requires the <code>com.nokia.mid.Mobinfo.IMSI</code> 
     * permission.
     * 
     * @return the IMSI of the device, or null if it is not known.
     */
    public String getIMSI()
    {
        String imsi = getProperty("com.nokia.mid.imsi");
        if (imsi != null)
            return imsi;

        //try some CDMA specific properties 
        imsi = getProperty("IMSI");
        if (imsi != null)
            return imsi;

        //try some depreciated CDMA specific properties 
        imsi = getProperty("device_id_imsi");
        if (imsi != null)
            return imsi;
        
        return super.getIMSI();
    }

    /** 
     * Returns the MSISDN associated with the SIM card on the device.
     * 
     * Note: determining the MSISDN requires at least S60 3rd Edition FP 2 (Java Runtime 1.3) or S40 6th Edition. The 
     * application must also belong to the operator or manufacturer domains
     * 
     * @return the users MSISDN, or null if it is not known.
     */
    public String getMSISDN()
    {
        String msisdn = getProperty("com.nokia.mid.msisdn");
        if (msisdn != null)
        {
            //returns a space separated list of MSISDNs - just return the first one
            int index = msisdn.indexOf(' ');
            if (index > 0)
                return msisdn = msisdn.substring(0, index);

            return msisdn;
        }

        return super.getMSISDN();
    }

    /** 
     * Returns the Push-To-Talk ID associated with the SIM card on the device.
     * 
     * Note: determining the PTT ID is not currently supported.
     * 
     * @return the users Push-To-Talk ID, or null if it is not known.
     */
    public String getPTTId()
    {
        return super.getPTTId();
    }
}
