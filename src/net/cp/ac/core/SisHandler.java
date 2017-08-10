/**
 * Copyright 2004-2011 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.core;

import java.util.Vector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;

import net.cp.engine.EngineSettings;
import net.cp.mtk.common.CommonUtils;
import net.cp.syncml.client.ServerAlert;
import net.cp.syncml.client.util.Logger;

/**
 * A class used to register for, listen for and process incoming server alerts received via binary SMS.
 *
 * This class implements the Singleton design pattern.
 */
public class SisHandler
{
    private static SisHandler instance = null;           //the single instance of the server alert handler
    private Logger logger;                               //the logger used to log activity

    /**
     * The context to use to register for server alerts (data SMS)
     */
    protected Context context;
    private SMSReceiver smsReceive;

    /**
     * The settings for SIS are read from this object
     */
    protected EngineSettings settings;

    /**
     * This service gets notified when a server alert arrives
     */
    protected SyncEngineService consumer;

    //Definition of server alert data types
    private static final byte ALERT_DATA_TEXT =          0x01;

    /**
     *
     * @param consumer The interface to be notified when a message arrives. Can be null.
     * @param context Context used to register for SMS intents. Usually the "Application" context.
     * @param settings EngineSettings used during Server Alert parsing.
     * @param logger A Logger to use during execution. Can be null.
     */
    public static synchronized SisHandler init(SyncEngineService consumer, Context context, EngineSettings settings, Logger logger)
    {
        if (instance == null)
        {
            //create the single instance of the server alert handler if necessary
            instance = new SisHandler(consumer, context, settings, logger);
            instance.register();
        }

        return instance;
    }

    /* Creates a new server alert handler - private to prevent object creation. */
    private SisHandler(SyncEngineService consumer, Context context, EngineSettings settings, Logger logger)
    {
        super();

        this.consumer = consumer;
        this.context = context;
        this.settings = settings;
        this.logger = logger;
        smsReceive = new SMSReceiver(this, logger);
    }

    /**
     * @return The instance of this class
     */
    public static SisHandler getInstance()
    {
        return instance;
    }

    /**
     * Opens the server alert handler.
     */
    public synchronized void register()
    {
        if(logger != null)
            logger.info("Registering for SIS on port: " + settings.sisPort);

        smsReceive.register(settings.sisPort);
    }

    /**
     * Closes the server alert handler.
     */
    public synchronized void unregister()
    {
        smsReceive.unregister();
    }

    /**
     *  Returns the SIS alert represented by the specified message or null if the message is invalid.
     */
    private ServerAlert parseMessage(SmsMessage message)
    {
        if (message == null)
            return null;

        try
        {
            //get the SMS data and parse it
            byte[] payloadData = message.getUserData();
            ServerAlert alert = ServerAlert.parse(payloadData);

            //validate the alert using the last nonse sent to the server (if there is one)
            boolean digestValid = false;

            if ( (settings.clientNonce != null) && (settings.clientNonce.length() > 0) )
            {
                if (logger != null)
                    logger.debug("Validating alert using client nonce '" + settings.clientNonce + "'");

                digestValid = alert.isValid(settings.httpSyncServerAddress, settings.userPassword, settings.clientNonce);
            }

            //if that didn't work, try using the old nonce sent to the server (if there is one)
            if ( (! digestValid) && (settings.oldClientNonce != null) && (settings.oldClientNonce.length() > 0) )
            {
                if (logger != null)
                    logger.debug("Validating alert using old client nonce '" + settings.oldClientNonce + "'");

                digestValid = alert.isValid(settings.httpSyncServerAddress, settings.userPassword, settings.oldClientNonce);
            }

            //if that didn't work, try using the last nonse received from the server (if there is one)
            if ( (! digestValid) && (settings.serverNonce != null) && (settings.serverNonce.length() > 0) )
            {
                if (logger != null)
                    logger.debug("Validating alert using server nonce '" + settings.serverNonce + "'");

                digestValid = alert.isValid(settings.httpSyncServerAddress, settings.userPassword, settings.serverNonce);
            }

            //if that didn't work, try using the default client nonce (if there is one)
            if ( (! digestValid) && (settings.defaultClientNonce != null) && (settings.defaultClientNonce.length() > 0) )
            {
                if (logger != null)
                    logger.debug("Validating alert using default client nonce '" + settings.defaultClientNonce + "'");

                digestValid = alert.isValid(settings.httpSyncServerAddress, settings.userPassword, settings.defaultClientNonce);
            }

            //make sure the alert is valid
            if (! digestValid)
            {
                if (logger != null)
                    logger.debug("Server alert is invalid - digest doesn't match");

                return null;
            }

            if (logger != null)
                logger.debug("Server alert is valid");

            return alert;
        }
        catch (Throwable e)
        {
            if (logger != null)
                logger.error("Failed to parse binary SMS", e);

            return null;
        }
    }

    /**
     * Called when a new message arrives.
     * Causes the message to be parsed into a ServerAlert, which is passed to the consumer.
     * Only valid messages are passed on.
     *
     * @param messages a collection of messages received.
     * Usually only one, but if the message is multipart (unlikely) there could be more
     */
    protected void messagesReceived(Vector<SmsMessage> messages)
    {
        SmsMessage message = null;

        for(int i=0; i<messages.size(); i++)
        {
            message = messages.get(i);
            ServerAlert alert = parseMessage(message);

            if(alert != null) //we only want to send the byte[], as we can't parcel the ServerAlert for RPC
                consumer.serverAlertReceived(message.getUserData());
        }
    }

    /**
     * Extract the vendor text from the server alert (data SMS)
     * @param alert The ServerAlert to parse
     * @return The Vendor specific text contained in the server alert.
     * Returns null if no text was present, or there was something else wrong with the alert.
     */
    public static String getVendorText(ServerAlert alert)
    {
        //parse any vendor data (which may contain some text to display to the user)
        String alertText = null;
        byte[] vendorData = alert.getVendorData();
        if ( (vendorData != null) && (vendorData.length > 0) )
        {
            try
            {
                //the first byte indicates the type of vendor data present
                byte dataType = vendorData[0];
                if (dataType == ALERT_DATA_TEXT)
                {
                    //the vendor data is additional text that should be displayed to the user - this is
                    //formatted as: <Length : 2 Bytes><UTF-8 : N Bytes>
                    if (vendorData.length < 3)
                        throw new Exception("Badly formed alert text");

                    //the next two bytes indicate the length of the text
                    int alertTextLength = ((vendorData[1] & 0xFF) << 8) + (vendorData[2] & 0xFF);
                    if (alertTextLength > 0)
                    {
                        //make sure the expected amount of data is present
                        if (alertTextLength > (vendorData.length - 3))
                            throw new Exception("Insufficient data in alert text - expected '" + alertTextLength + "' bytes");

                        //extract the alert text
                        alertText = new String(vendorData, 3, alertTextLength, "UTF-8");
                    }
                }
                else
                {
                    throw new Exception("Unknown vendor data type '" + dataType + "'");
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }

        return alertText;
    }

    private final class SMSReceiver extends BroadcastReceiver
    {
        public static final String DATA_SMS_RECEIVED_ACTION = "android.intent.action.DATA_SMS_RECEIVED";
        public static final String SMS_RECEIVED_ACTION = "android.intent.action.SMS_RECEIVED";

        Vector<SmsMessage> messages = null;

        SisHandler handler = null;

        //The port number to "listen on".
        private int listenPort = 0;

        private Logger logger;

        public SMSReceiver(SisHandler handler, Logger logger)
        {
            this.handler = handler;
            this.logger = logger;
        }

        /**
         *
         * @param port The sms port number to listen on for Server Alerts
         */
        public void register(int port)
        {
            if(logger != null)
                logger.info("Registering for SMS on port: " + port);

            IntentFilter filter = new IntentFilter(DATA_SMS_RECEIVED_ACTION);
            this.listenPort = port;
            filter.addDataScheme("sms");
            filter.addDataAuthority("localhost", ""+port);
            handler.context.registerReceiver(this, filter);

            IntentFilter filter2 = new IntentFilter(SMS_RECEIVED_ACTION);

            filter2.addDataScheme("sms");
            filter2.addDataAuthority("localhost", ""+port);
            handler.context.registerReceiver(this, filter2);

            IntentFilter filter3 = new IntentFilter(SMS_RECEIVED_ACTION);

            filter3.addDataScheme("sms");
            filter3.addDataAuthority("", ""+port);
            handler.context.registerReceiver(this, filter3);

            IntentFilter filter4 = new IntentFilter(DATA_SMS_RECEIVED_ACTION);

            filter4.addDataScheme("sms");
            filter4.addDataAuthority("", ""+port);
            handler.context.registerReceiver(this, filter4);
        }

        public void unregister()
        {
            if(logger != null)
                logger.info("Unregistering for SMS");

            handler.context.unregisterReceiver(this);
        }

        public void onReceive(Context context, Intent intent)
        {
            String intentAction = intent.getAction();

            if (intentAction.equals(DATA_SMS_RECEIVED_ACTION) || intentAction.equals(SMS_RECEIVED_ACTION))
            {
                if(logger != null)
                {
                    if(intentAction.equals(DATA_SMS_RECEIVED_ACTION))
                        logger.info("DATA SMS Received");
                    else
                        logger.info("PLAIN SMS Received");
                }

                Bundle bundle = intent.getExtras();

                //this magic was revealed in the Android OS source code: SMSDispatcher.java
                Uri theUri= intent.getData();
                int port = theUri.getPort();

                if(port == listenPort)
                {
                    Object[] PDUs = (Object[]) bundle.get("pdus");

                    messages = new Vector<SmsMessage>(0);

                    for (int i=0; i<PDUs.length; i++)
                    {
                        SmsMessage message =  SmsMessage.createFromPdu((byte[]) PDUs[i]);
                        messages.add(message);

                        if(logger != null)
                        {
                            logger.debug("Processed PDU: " + CommonUtils.hexEncode((byte[]) PDUs[i]));
                        }
                    }
                    handler.messagesReceived(messages);
                }
            }
        }
    }
}

