/**
 * Copyright © 2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.system;

import net.cp.engine.UtilityClass;

/**
 * A class representing a network identifier. <br/><br/>
 * 
 * A network is identified by:
 * <ol>
 *      <li> a network code (NID in CDMA and MNC in GSM) which identifies the operator (e.g. Vodafone, O2, etc).
 *      <li> a country code (ISO-3166 (2-alpha, 3-alpha or numeric) or ITU E.212). 
 * </ol>
 *
 * @author Herve Fayolle
 */
public class NetworkIdentifier
{
    public String[] isoCountryCodes;               //the ISO-3166 country codes
    public String[] ituCountryCodes;               //the ITU country codes
    public String networkCode;                     //the network code

    public NetworkIdentifier(String isoCountryCode, String ituCountryCode, String networkId)
    {
        if (isoCountryCode != null)
        {
            isoCountryCodes = new String[1];
            isoCountryCodes[0] = isoCountryCode;
        }
        else
        {
            ituCountryCodes = new String[1];
            ituCountryCodes[0] = ituCountryCode;
        }

        networkCode = networkId;
    }
    
    public NetworkIdentifier(String[] isoCountryCodeIds, String[] ituCountryCodeIds, String networkId)
    {
        isoCountryCodes = isoCountryCodeIds;
        ituCountryCodes = ituCountryCodeIds;
        networkCode = networkId;
    }
    
    
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        
        sb.append("NetworkCode=");
        sb.append(networkCode);
        
        if (isoCountryCodes != null)
        {
            sb.append(",Iso3166CountryCodes=");
            sb.append( UtilityClass.concatenate(isoCountryCodes, "|") );
        }
        
        if (ituCountryCodes != null)
        {
            sb.append(",ItuCountryCodes=");
            sb.append( UtilityClass.concatenate(ituCountryCodes, "|") );
        }
        
        return sb.toString();
    }
}
    