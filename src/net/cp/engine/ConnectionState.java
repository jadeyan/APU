/**
 * Copyright 2004-2009 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.engine;

/**
 * This class outlines how the connection state is represented.
 * 
 * 
 *
 */
public abstract class ConnectionState
{
    /**
     * The connection state could not be determined.
     * We may not be connected, or we may be connected but the cost is unknown.
     */
    public static final int CONNECTION_STATE_UNKNOWN = 1;
    
    /**
     * The connection state is that we are not connected.
     */
    public static final int CONNECTION_STATE_NOT_CONNECTED = 2;
    
    /**
     * The connection state is that we are connected to a "no cost"
     * connection. E.g. WIFI, Bluetooth PAN.
     */
    public static final int CONNECTION_STATE_NO_COST = 4;
    
    /**
     * The connection state is that we are connected to a "normal cost"
     * connection. E.g GPRS, EDGE, 3G.
     */
    public static final int CONNECTION_STATE_NORMAL_COST = 8;
    
    /**
     * The connection state is that we are connected to a "high cost"
     * connection. E.g roaming on 3G.
     */
    public static final int CONNECTION_STATE_HIGH_COST = 16;
    
    /**
     * Indicates no connection
     */
    public static final int CONNECTION_TYPE_NONE = 1;
    
    /**
     * Indicates a "no cost" connection type.
     * connection. E.g. WIFI, Bluetooth PAN.
     */
    public static final int CONNECTION_TYPE_NO_COST = 2;
    
    /**
     * Indicates a "normal cost" connection type.
     * E.g GPRS, EDGE, 3G.
     */
    public static final int CONNECTION_TYPE_NORMAL_COST = 3;
    
    /**
     * Indicates a "high cost" connection type.
     * E.g roaming on 3G.
     */
    public static final int CONNECTION_TYPE_HIGH_COST = 4;
    
    /**
     * Indicates any connection type.
     * E.g  WIFI, 3G.
     */
    public static final int CONNECTION_TYPE_ANY = 5;
    
    /**
     * 
     * @return a bitfield comprising one or more of the following:<br>
     * <br>
     * CONNECTION_STATE_UNKNOWN<br>
     * CONNECTION_STATE_NOT_CONNECTED<br>
     * CONNECTION_STATE_NO_COST<br>
     * CONNECTION_STATE_NORMAL_COST<br>
     * CONNECTION_STATE_HIGH_COST<br>
     * <br>
     * E.g. a device connected to both WIFI and 3g would have the following value: <br>
     * CONNECTION_STATE_NO_COST | CONNECTION_STATE_NORMAL_COST
     */
    public abstract int getConnectionState();

    /**
     * 
     * @param inetAddr a byte array containing a IPv4 address in the form returned by
     * InetAddress.getAddress(). That is, in network order, with the MSB in inetAddr[0].
     * 
     * @return an integer representation of the supplied address
     */
    public static int inetAddrByteArrayToInt(byte[] inetAddr)
    {
        int addr = 0;
        
        for (int i = 0; i < 4; i++)
        {
            int shift = i * 8;
            addr += (inetAddr[i] & 0x000000FF) << shift;
        }
        
        return addr;
    }
}
