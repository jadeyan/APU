/**
 * Copyright 2004-2010 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.net.InetAddress;
import java.net.UnknownHostException;

import net.cp.engine.ConnectionState;
import net.cp.engine.EngineSettings;
import net.cp.system.NetworkIdentifier;
import net.cp.syncml.client.util.Logger;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;

/**
 * This class is the ConnectionState implementation for the Android platform.
 * It allows us to know what networks we are connected to (high, normal, or no cost).
 * This class implements the singleton design pattern.
 *
 * @author joconnor
 *
 */
public class AndroidConnectionState extends ConnectionState
{
    /**
     * This is used to query the connection state from the system
     */
    protected static ConnectivityManager connManager;

    /**
     * This is used to query the GSM network state from the system
     */
    protected static TelephonyManager teleManager;

    /**
     * The logger to use
     */
    protected static Logger logger;

    /**
     * The single instance of the class (This class implements the Singleton pattern)
     */
    protected static AndroidConnectionState instance;


    /**
     * The settings relating to the service / engine
     */
    protected EngineSettings settings;

    /**
     *
     * @return Returns the instance of this class, or null if it has not been initialised
     */
    public static AndroidConnectionState getInstance()
    {
        return instance;
    }

    /**
     * This initialises the class. It must be called once before the other methods can be used.
     *
     * @param context The context, used to query the network state
     * @param logger The logger to use. Can be null.
     */
    public synchronized static void init(Context context, EngineSettings settings, Logger logger)
    {
        if(instance == null)
        {
            instance = new AndroidConnectionState(context, settings, logger);
        }
    }

    private AndroidConnectionState(Context context, EngineSettings settings, Logger logger)
    {
        AndroidConnectionState.logger = logger;
        this.settings = settings;

        connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        teleManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    /* (non-Javadoc)
     * @see net.cp.engine.ConnectionState#getConnectionState()
     */
    @Override
    public int getConnectionState()
    {
        if(logger != null)
            logger.info("Determining connection state");

        NetworkInfo[] netInfos = connManager.getAllNetworkInfo();

        if(netInfos == null)
        {
            if(logger != null)
                logger.error("netInfo was null, CONNECTION_STATE_UNKNOWN");

            return CONNECTION_STATE_UNKNOWN;
        }

        NetworkInfo netInfo = null;
        int connectionState = CONNECTION_STATE_UNKNOWN;

        //check each connection and set the flags accordingly
        for(int i=0; i<netInfos.length; i++)
        {
            netInfo = netInfos[i];

            if(!netInfo.isConnected())
            {
                if(logger != null)
                    logger.info("netInfo: not connected");

                if(connectionState == CONNECTION_STATE_UNKNOWN)
                    connectionState = CONNECTION_STATE_NOT_CONNECTED;

                continue;
            }

            if(netInfo.getType() == ConnectivityManager.TYPE_WIFI)
            {
                if(logger != null)
                    logger.info("netInfo: connected to wifi");

                //clear "not connected" flag, add "no cost" flag
                connectionState = (connectionState & (~CONNECTION_STATE_NOT_CONNECTED)) | CONNECTION_STATE_NO_COST;
            }

            if(netInfo.getType() == ConnectivityManager.TYPE_MOBILE)
            {
                if(logger != null)
                    logger.info("netInfo: connected to mobile net");

                if(isRoaming())
                {
                    if(logger != null)
                        logger.info("roaming");

                    //clear "not connected" flag, add "high cost" flag
                    connectionState = (connectionState & (~CONNECTION_STATE_NOT_CONNECTED)) | CONNECTION_STATE_HIGH_COST;
                }

                else
                {
                    if(logger != null)
                        logger.info("not roaming");

                    //clear "not connected" flag, add "normal cost" flag
                    connectionState = (connectionState & (~CONNECTION_STATE_NOT_CONNECTED)) | CONNECTION_STATE_NORMAL_COST;
                }
            }
        }

        return connectionState;
    }

    /**
     * Even if the device indicates it is roaming, there might be a roaming agreement that provides
     * same price data access. This method compares the current network to the "home networks" list.
     *
     * @return If the "home network" list is defined, returns false if the current network is on the list. Otherwise true.<br>
     * If no "home network" list is defined, then we return the devices roaming status verbatim.
     */
    private boolean isRoaming()
    {
        //first check if the current network is in our list of home networks
        if(settings.hasHomeNetwork())
        {
            NetworkIdentifier currentNetworkId = getCurrentNetworkId();
            if (currentNetworkId != null)
                return settings.isHomeNetwork(currentNetworkId);

        }

        //otherwise, try to determine the current roaming state
        return teleManager.isNetworkRoaming();
    }

    /** Returns the identifier of the network that the device is currently connected to or null if the network is unknown. */
    protected NetworkIdentifier getCurrentNetworkId()
    {
        String networkID = teleManager.getNetworkOperator();

        if(logger != null)
            logger.info("networkID: " + networkID);

        if (networkID != null)
        {
            //first 3 chars are MCC,
            //rest is MNC
            String mnc = networkID.substring(3);

            return new NetworkIdentifier(teleManager.getNetworkCountryIso(), null, mnc);
        }

        return null;

    }

    public static boolean setConnectionType(int connectionType, String host)
    {
        //shortcut if we don't care which connection we use
        if(connectionType == ConnectionState.CONNECTION_TYPE_ANY)
            return true;

        InetAddress addr;
        try
        {
            addr = InetAddress.getByName(host);

            byte[] addrBytes = addr.getAddress();
            int addrInt = inetAddrByteArrayToInt(addrBytes);

            if(addrBytes.length != 4)
            {
                if(logger != null)
                    logger.error("Unsupported IP address type. length: " + addrBytes.length);

                return false;
            }

            if(connectionType == ConnectionState.CONNECTION_TYPE_NO_COST)
                return connManager.requestRouteToHost(ConnectivityManager.TYPE_WIFI, addrInt);

            if(connectionType == ConnectionState.CONNECTION_TYPE_NORMAL_COST ||
                    connectionType == ConnectionState.CONNECTION_TYPE_HIGH_COST)
                return connManager.requestRouteToHost(ConnectivityManager.TYPE_MOBILE, addrInt);
        }

        catch (UnknownHostException e)
        {
            if(logger != null)
                logger.error("setConnectionType: failed to resolve " + host);
        }

        return false;

    }

    /**
     *
     * @return true if we are allowed to use data in the background without a UI present.
     * Otherwise false.
     */
    public boolean backgroundDataAllowed()
    {
        return connManager.getBackgroundDataSetting();
    }

}
