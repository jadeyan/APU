/**
 * Copyright © 2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.mtk.common.devinfo;


import net.cp.mtk.common.StringUtils;


/**
 * A class representing a network identifier.
 * 
 * A network is identified by:
 * <ol>
 *      <li> A network code (NID in CDMA and MNC in GSM) which identifies the operator (e.g. Vodafone, O2, etc).
 *      <li> A country code (ISO-3166 (2-alpha, 3-alpha or numeric) or ITU E.212). 
 * </ol>
 *
 * @author Herve Fayolle
 */
public class NetworkIdentifier
{
    private String[] mccIso;               //the ISO-3166 country codes
    private String[] mccItu;               //the ITU country codes
	private String mnc;                    //the network code


	/**
     * Creates a network identifier representing the specified country and network.
     * 
	 * @param isoCountryCode   the country code in ISO-3166 format. May be null or empty (but only if <code>ituCountryCode</code>) is not.
	 * @param ituCountryCode   the country code in ITU-T E.212 format. May be null or empty (but only if <code>isoCountryCode</code>) is not.
	 * @param networkId        the network code (Network Identification Number (NID) in CDMA or Mobile Network Code (MNC) in GSM). Must not be null or empty.
	 * @throws IllegalArgumentException if no country codes or network ID is specified.
	 */
	public NetworkIdentifier(String isoCountryCode, String ituCountryCode, String networkId)
	{
	    if ( ((isoCountryCode == null) || (isoCountryCode.length() <= 0)) && ((ituCountryCode == null) || (ituCountryCode.length() <= 0)) )
	        throw new IllegalArgumentException("Either an ISO-3166 or ITU-T E.212 country code must be supplied");
        if ( (networkId == null) || (networkId.length() <= 0) )
            throw new IllegalArgumentException("A network ID must be supplied");
	            
		if (isoCountryCode != null)
		{
		    mccIso = new String[1];
		    mccIso[0] = isoCountryCode;
		}
		else
		{
		    mccItu = new String[1];
		    mccItu[0] = ituCountryCode;
		}

		mnc = networkId;
	}
	
    /**
     * Creates a network identifier representing the specified country and network.
     * 
     * This constructor is to cater for the case where a single country is identified by multiple country codes. In this
     * case, all supplied country codes must identify the same country.
     * 
     * @param isoCountryCodes  the country code in ISO-3166 format. May be null or empty (but only if <code>ituCountryCode</code>) is not.
     * @param ituCountryCodes  the country code in ITU-T E.212 format. May be null or empty (but only if <code>isoCountryCode</code>) is not.
     * @param networkId        the network code (Network Identification Number (NID) in CDMA or Mobile Network Code (MNC) in GSM). Must not be null or empty.
     * @throws IllegalArgumentException if no country codes or network ID is specified.
     */
	public NetworkIdentifier(String[] isoCountryCodes, String[] ituCountryCodes, String networkId)
	{
        if ( ((isoCountryCodes == null) || (isoCountryCodes.length <= 0)) && ((ituCountryCodes == null) || (ituCountryCodes.length <= 0)) )
            throw new IllegalArgumentException("Either ISO-3166 or ITU-T E.212 country codes must be supplied");
        if ( (networkId == null) || (networkId.length() <= 0) )
            throw new IllegalArgumentException("A network ID must be supplied");
	    
	    mccIso = isoCountryCodes;
	    mccItu = ituCountryCodes;
	    mnc = networkId;
	}
	
	
    /**
     * Returns the ISO-3166 country codes used to identify the network. 
     *  
     * @return the country codes in ISO-3166 format. May be null or empty.
     */
    public String[] getISOCountryCodes()
    {
        return mccIso;
    }

    /**
     * Returns the ISO-3166 country code used to identify the network.
     * 
     * If multiple codes are present, the first one will be returned.  
     *  
     * @return the country code in ISO-3166 format. May be null.
     */
    public String getISOCountryCode()
    {
        return ((mccIso != null) && (mccIso.length > 0)) ? mccIso[0] : null;
    }

    /**
     * Returns the ITU-T E.212 country codes used to identify the network. 
     *  
     * @return the country codes in ITU-T E.212 format. May be null or empty.
     */
    public String[] getITUCountryCodes()
    {
        return mccItu;
    }

    /**
     * Returns the ITU-T E.212 country code used to identify the network. 
     * 
     * If multiple codes are present, the first one will be returned.  
     *  
     * @return the country code in ITU-T E.212 format. May be null.
     */
    public String getITUCountryCode()
    {
        return ((mccItu != null) && (mccItu.length > 0)) ? mccItu[0] : null;
    }

    /**
     * Returns the network ID identifying the network.
     * 
     * @return the network code (Network Identification Number (NID) in CDMA or Mobile Network Code (MNC) in GSM). Will not be null or empty.
     */
    public String getNetworkId()
    {
        return mnc;
    }


    /**
     * Returns whether or not two network identifiers represent the same network.
     * 
     * Two networks identifiers are considered to be the same if the network ID is the same, and they have at least one
     * country code in common.
     * 
     * @param obj the object to compare to. May be null.
     * @return TRUE if the specified object is a network identifier that identifies the same network as this network identifier.
     */
    public boolean equals(Object obj)
    {
        if ( (obj == null) || (!(obj instanceof NetworkIdentifier)) )
            return false;
        
        //check if the network IDs are the same
        NetworkIdentifier network = (NetworkIdentifier)obj;
        if (! mnc.equalsIgnoreCase(network.mnc))
            return false;
        
        //check if they have at least one ISO country code in common
        if ( (mccIso != null) && (network.mccIso != null) )
        {
            for (int i = 0; i < mccIso.length; i++)
            {
                if (StringUtils.indexOf(network.mccIso, mccIso[i], true) >= 0)
                    return true;
            }
        }
        
        //check if they have at least one ITU country code in common
        if ( (mccItu != null) && (network.mccItu != null) )
        {
            for (int i = 0; i < mccItu.length; i++)
            {
                if (StringUtils.indexOf(network.mccItu, mccItu[i], true) >= 0)
                    return true;
            }
        }
        
        return false;
    }

	/**
	 * Returns the string representation of the network identifier.
	 * 
	 * The returned string will be in the form: "[CountryCode],[CountryCode],...:[NetworkCode]".
	 * 
	 * @return the string representation of the network identifier. Will not be null or empty.
	 */
	public String toString()
	{
		StringBuffer buffer = new StringBuffer();
		
        if ( (mccIso != null) && (mccIso.length > 0) )
            buffer.append( StringUtils.concatStrings(mccIso, ",") );
        
        if ( (mccItu != null) && (mccItu.length > 0) )
            buffer.append( StringUtils.concatStrings(mccItu, ",") );

        buffer.append(':');
        buffer.append(mnc);
        
		return buffer.toString();
	}
}
	