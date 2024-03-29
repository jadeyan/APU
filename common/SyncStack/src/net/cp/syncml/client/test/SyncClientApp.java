/**
 * Copyright � 2004-2011 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.syncml.client.test;


import java.util.*;

import net.cp.syncml.client.SyncML;
import net.cp.syncml.client.SyncManager;
import net.cp.syncml.client.Transport;
import net.cp.syncml.client.devinfo.*;
import net.cp.syncml.client.store.*;
import net.cp.syncml.client.test.store.*;
import net.cp.syncml.client.util.Logger;



/**
 * Definition of a Java application used to test data conversion functionality.
 *
 * @author  Denis Evoy
 */
public class SyncClientApp
{
	private static int EXIT_CODE_SUCCESS	= 0;
	private static int EXIT_CODE_USAGE_ERR	= 1;
	private static int EXIT_CODE_SYNC_ERR	= 2;
	private static int EXIT_CODE_INIT_ERR	= 3;
	private static int EXIT_CODE_SYNC_HUNG	= 4;
	private static int EXIT_CODE_SYNC_INTERRUPT	= 5;
	
	private TestSyncListener listener = null;
	
	
    /** Private constructor to prevent instantiation. */
    private SyncClientApp() { super(); }
    

    /** Displays the usage information of the application. */
    private static void showUsage()
    {
        System.out.println();
        System.out.println("USAGE: SyncClientApp [GENERAL-OPTIONS] [CONTACT_OPTIONS] [CONTENT_OPTIONS]");
        System.out.println();
        System.out.println("General options include:");
        System.out.println("     -h <Hostname> : the hostname of the SyncML server. Must be supplied.");
        System.out.println("     -p <Port> : the HTTP port number of the SyncML server. Default is 9080.");
        System.out.println("     -u <Username> : the username to use to authenticate with the SyncML server. Must be supplied.");
        System.out.println("     -w <Password> : the password to use to authenticate with the SyncML server. Must be supplied.");
        System.out.println("     -ph <Hostname> : the hostname of the proxy server.");
        System.out.println("     -pp <Port> : the HTTP port number of the proxy server. Default is 80.");
        System.out.println("     -max-msg-size <MaxMessageSize> : the maximum allowed size of a SyncML message. Default is 8192 bytes.");
        System.out.println();
        System.out.println("     -dev-id <DeviceId> : the unique ID of the desktop device. Must be supplied.");
        System.out.println("     -dev-type <DeviceType> : the type of the device. One of 'phone', 'pager', 'handheld', 'pda',");
        System.out.println("                              'smartphone', 'server' or 'workstation'. Default is 'workstation'.");
        System.out.println("     -dev-manufacturer <DeviceManufacturer> : the manufacturer of device. Default is 'Critical Path'.");
        System.out.println("     -dev-model <DeviceModel> : the model of device. Default is 'Mobile Client'.");
        System.out.println("     -app-name <ApplicationName> : the name of the sync application. Default is 'Mobile Client'.");
        System.out.println("     -app-version <ApplicationVersion> : the version of the sync application. Default is '1.0'.");
        System.out.println("     -app-capability-id <ApplicationCapabilities> : the capabilities allowed by the sync application. No default.");
        System.out.println("     -sw-version <SoftwareVersion> : the version of the application software. Default is '1.0'.");
        System.out.println();
        System.out.println("     -suspend-send-count <N> : suspends the session after sending the Nth SyncML message. Default is 0.");
        System.out.println("     -suspend-recv-count <N> : suspends the session after receiving the Nth SyncML message. Default is 0.");
        System.out.println("     -resume-delay <Seconds> : resumes a suspended session after the specified number of seconds. Default is 0.");
        System.out.println("     -error-output-count <N> : generate an IO error before sending the Nth SyncML message (suspends the session). Default is 0.");
        System.out.println("     -error-input-count <N> : generate an IO error before receiving the Nth SyncML message (suspends the session). Default is 0.");
        System.out.println();
        System.out.println("     -display-alert-status <status> : indicates the status code to return to server display alerts. Default is 200 (success)");
        System.out.println("     -display-alert-delay <delay> : indicates the delay in seconds, to wait for, before returning the alert status code. Default is 0 (no delay)");
        System.out.println();
        System.out.println("     -server-uri <uri> : defines the server URI to use in SyncML requests. Default value is /syncml");        
        System.out.println("     -http-headers <header=value,..., header-value> : defines HTTP header(s) to add in the HTTP requests");
        System.out.println();
        System.out.println("Contact options include:");
        System.out.println("     -contact-dir <ContactsDirectory> : the directory containing the vCard files to");
        System.out.println("                                        be synced. Must be supplied.");
        System.out.println("     -contact-cr <ContactConflictRes> : the conflict resolution method to use. One of");
        System.out.println("                                        'duplicate', 'ignore', 'client_wins', 'server_wins'");
        System.out.println("                                        or 'recent_wins'. Default is 'client_wins'.");
        System.out.println("     -contact-sync-type <SyncType> : the type of sync to request. One of '1' (two-way), '2' (two-way slow),");
        System.out.println("                                     '3' (one-way from client), '4' (refresh from client)");
        System.out.println("                                     '5' (one-way from server), '6' (refresh from server) or");
        System.out.println("                                     '7' (server alerted). Default is '1'.");
        System.out.println("     -contact-force-rev : update the vCard 'REV' field with the current date.");
        System.out.println("     -contact-error-status-code <StatusCode>: generate an exception containing the specified SyncML status code. Default is 0.");
        System.out.println("     -contact-error-status-data <StatusData>: generate an exception containing the specified status data.");
        System.out.println("                                              Possible status data are 'DSMemExceeded'. Default is 0.");
        System.out.println();
        System.out.println("Content options include:");
        System.out.println("     -content-dir <ContentDirectory> : the directory containing the files and folders to be");
        System.out.println("                                       synced in a 'data' sub-directory. Must be supplied.");
        System.out.println("     -content-mcard-id <MemCardId> : the optional memory card id. Memory card files and folders"); 
        System.out.println("                                     are taken in <content-dir>/data/<mem card id>");
        System.out.println("     -content-max-record-size <MaxSizeBytes> : the maximum number of bytes that can be stored in a record. No limit by default.");
        System.out.println("     -content-sync-type <SyncType> : the type of sync to request. One of '1' (two-way), '2' (two-way slow),");
        System.out.println("                                     '3' (one-way from client), '4' (refresh from client)");
        System.out.println("                                     '5' (one-way from server), '6' (refresh from server) or");
        System.out.println("                                     '7' (server alerted). Default is '1'.");
        System.out.println("     -content-error-status-code <StatusCode>: generate an exception containing the specified SyncML status code. Default is 0.");
        System.out.println("     -content-error-status-data <StatusData>: generate an exception containing the specified status data.");
        System.out.println("                                              Possible status data are 'TotalDataLimitExceeded', 'DSMemExceeded',");
        System.out.println("                                              and 'InvalidFileName'. Default is 0.");
        System.out.println("     -content-disable-file-rep: do not send file representation on file upload. Default is false (file representation is enabled)");        
        System.out.println();
    }
    
    
    /** 
     * Command-line entry point for testing purposes.
     *  
     * @param args the application arguments. 
     */
    public static void main(String[] args)
    {
    	SyncClientApp app = new SyncClientApp();
    	int ret = app.run(args, null, true);
    	if (ret == EXIT_CODE_USAGE_ERR)
    		showUsage();
    	
    	System.exit(ret);
    }

    public static SyncClientApp getInstance()
    {
    	return new SyncClientApp();
    }
    
    /** 
     * Harness entry point for testing purposes.
     *  
     * @param args      the application arguments. 
     * @param logger    the logger used to log activity.
     * @return the application exit status. 
     */
    public int run(String[] args, Logger logger)
    {
    	return run(args, logger, false);
    }
    
    /** Common function for both command line and harness entry points. */ 
    private int run(String[] args, Logger logger, boolean usage)  
    {
        //Get and parse the arguments
        CmdLineParser pCmdLine = new CmdLineParser(args);
        String hostname = pCmdLine.getArgument("h");
        String port = pCmdLine.getArgument("p");
        String username = pCmdLine.getArgument("u");
        String password = pCmdLine.getArgument("w");
        String deviceId = pCmdLine.getArgument("dev-id");
        String deviceType = pCmdLine.getArgument("dev-type");
        String deviceManufacturer = pCmdLine.getArgument("dev-manufacturer");
        String deviceModel = pCmdLine.getArgument("dev-model");
        String applicationName = pCmdLine.getArgument("app-name");
        String applicationVersion = pCmdLine.getArgument("app-version");
        String softwareVersion = pCmdLine.getArgument("software-version");
        String applicationCapabilityId = pCmdLine.getArgument("app-capability-id");
        String proxyHostname = pCmdLine.getArgument("ph");
        String proxyPort = pCmdLine.getArgument("pp");
        String maxMsgSize = pCmdLine.getArgument("max-msg-size");
        String suspendSendCount = pCmdLine.getArgument("suspend-send-count");
        String suspendRecvCount = pCmdLine.getArgument("suspend-recv-count");
        String errorOutputCount = pCmdLine.getArgument("error-output-count");
        String errorInputCount = pCmdLine.getArgument("error-input-count");
        String resumeDelay = pCmdLine.getArgument("resume-delay");
        
        String displayAlertStatus = pCmdLine.getArgument("display-alert-status");
        String displayAlertDelay = pCmdLine.getArgument("display-alert-delay");
        String serverUri = pCmdLine.getArgument("server-uri");
        String httpHeaders = pCmdLine.getArgument("http-headers");
        
        String contactDir = pCmdLine.getArgument("contact-dir");
        String contactCr = pCmdLine.getArgument("contact-cr");
        String contactSyncType = pCmdLine.getArgument("contact-sync-type");
        boolean contactForceRev = pCmdLine.isOption("contact-force-rev");
        String contactErrorStatusCode = pCmdLine.getArgument("contact-error-status-code");
        String contactErrorStatusData = pCmdLine.getArgument("contact-error-status-data"); 
        
        String contentDir = pCmdLine.getArgument("content-dir");
        String mcardId = pCmdLine.getArgument("content-mcard-id");
        String contentMaxRecordSize = pCmdLine.getArgument("content-max-record-size");
        String contentSyncType = pCmdLine.getArgument("content-sync-type");
        String contentErrorStatusCode = pCmdLine.getArgument("content-error-status-code");
        String contentErrorStatusData = pCmdLine.getArgument("content-error-status-data");
        boolean contentDisableFileRep = pCmdLine.isOption("content-disable-file-rep");

        if (logger == null)
        	logger = new TestSyncLogger();
        
        //check for mandatory parameters
        if ( (deviceId == null) || (deviceId.length() <= 0) )
        {
        	logger.error("The unique ID of the desktop device must be specified");
            return EXIT_CODE_USAGE_ERR;
        }
        if ( (hostname == null) || (hostname.length() <= 0) )
        {
            logger.error("The hostname of the sync server must be specified");
            return EXIT_CODE_USAGE_ERR;
        }
        if ( (username == null) || (username.length() <= 0) )
        {
            logger.error("The username to use during authentication must be specified");
            return EXIT_CODE_USAGE_ERR;
        }
        if ( (password == null) || (password.length() <= 0) )
        {
            logger.error("The password to use during authentication must be specified");
            return EXIT_CODE_USAGE_ERR;
        }

        //set default argument values
        if ( (port == null) || (port.length() <= 0) )
            port = "9080";
        if ( (proxyPort == null) || (proxyPort.length() <= 0) )
            proxyPort = "80";
        
        if ( (deviceType == null) || (deviceType.length() <= 0) )
            deviceType = Device.DEVICE_TYPE_WORKSTATION;
        if ( (deviceManufacturer == null) || (deviceManufacturer.length() <= 0) )
            deviceManufacturer = "Critical Path";
        if ( (deviceModel == null) || (deviceModel.length() <= 0) )
            deviceModel = "Mobile Client";
        if ( (applicationName == null) || (applicationName.length() <= 0) )
            applicationName = "Mobile Client";
        if ( (applicationVersion == null) || (applicationVersion.length() <= 0) )
            applicationVersion = "1.0";
        if ( (softwareVersion == null) || (softwareVersion.length() <= 0) )
        	softwareVersion = "1.0";
        if ( (maxMsgSize == null) || (maxMsgSize.length() <= 0) )
            maxMsgSize = "8192";
        
        if ( (suspendSendCount == null) || (suspendSendCount.length() <= 0) )
            suspendSendCount = "0";
        if ( (suspendRecvCount == null) || (suspendRecvCount.length() <= 0) )
            suspendRecvCount = "0";
        if ( (errorOutputCount == null) || (errorOutputCount.length() <= 0) )
            errorOutputCount = "0";
        if ( (errorInputCount == null) || (errorInputCount.length() <= 0) )
            errorInputCount = "0";
        if ( (resumeDelay == null) || (resumeDelay.length() <= 0) )
            resumeDelay = "0";
        
        if ( (displayAlertStatus == null) || (displayAlertStatus.length() <= 0) )
        	displayAlertStatus = "200"; // success
        if ( (displayAlertDelay == null) || (displayAlertDelay.length() <= 0) )
        	displayAlertDelay = "0"; // no delay
        
        if (serverUri == null || serverUri.length() <= 0)
        	serverUri = "/syncml";
        
        if ( (contactSyncType == null) || (contactSyncType.length() <= 0) )
            contactSyncType = "1";
        if ( (contactCr == null) || (contactCr.length() <= 0) )
            contactCr = RecordStore.EMI_CONFLICT_RES_CLIENT_WINS;
        if ( (contactErrorStatusCode == null) || (contactErrorStatusCode.length() <= 0) )
            contactErrorStatusCode = "0";
        
        if ( (contentSyncType == null) || (contentSyncType.length() <= 0) )
            contentSyncType = "1";
        if ( (contentMaxRecordSize == null) || (contentMaxRecordSize.length() <= 0) )
            contentMaxRecordSize = "0";
        if ( (contentErrorStatusCode == null) || (contentErrorStatusCode.length() <= 0) )
            contentErrorStatusCode = "0";
        
        int contactSyncMode = getSyncType(contactSyncType);
        if (contactSyncMode <= 0)
        {
        	logger.error("Invalid contact sync type specified: " + contactSyncType);
        	return EXIT_CODE_USAGE_ERR;
        }
        
        int contentSyncMode = getSyncType(contentSyncType);
        if (contentSyncMode <= 0)
        {
        	logger.error("Invalid content sync type specified: " + contentSyncType);
        	return EXIT_CODE_USAGE_ERR;
        }
        
        if ( (contactDir != null) && (contactDir.length() > 0) )
	    {
	        contactCr = getConflictResolution(contactCr);
	        if (contactCr == null || contactCr.length() <= 0)
	        {
	        	logger.error("Invalid contact conflict resolution specified: " + contactCr);
	        	return EXIT_CODE_USAGE_ERR;
	        }
	    }

        //create the sync manager
        listener = new TestSyncListener(logger, Integer.parseInt(suspendSendCount), Integer.parseInt(suspendRecvCount), Integer.parseInt(resumeDelay), Integer.parseInt(displayAlertStatus), Integer.parseInt(displayAlertDelay));
        Device desktopDevice = new DesktopDevice(deviceId, deviceType, deviceManufacturer, deviceModel, applicationName, applicationVersion, softwareVersion, applicationCapabilityId, mcardId);
        Transport httpTransport = new HTTPTransport(hostname, Integer.parseInt(port), proxyHostname, Integer.parseInt(proxyPort), serverUri, Integer.parseInt(maxMsgSize), Integer.parseInt(errorOutputCount), Integer.parseInt(errorInputCount), httpHeaders);
        SyncManager manager = new SyncManager(desktopDevice, httpTransport, username, password, listener, logger);
        listener.setSyncManager(manager);        
        
        //create the stores to be synced 
        RecordStore[] stores = null;
        try
        {
            Vector storeList = new Vector();
	        if ( (contactDir != null) && (contactDir.length() > 0) )
	            storeList.addElement( new DesktopVcardStore(contactDir, logger, contactCr, contactSyncMode, contactForceRev, Integer.parseInt(contactErrorStatusCode), contactErrorStatusData));
	        
	        if ( (contentDir != null) && (contentDir.length() > 0) )
	        	storeList.addElement( new DesktopFileStore(contentDir, mcardId, logger, Long.parseLong(contentMaxRecordSize), contentSyncMode, Integer.parseInt(contentErrorStatusCode), contentErrorStatusData, contentDisableFileRep));

	        if (storeList.size() <= 0)
	        {
	            logger.error("You must specify the location of at least one record store to sync.");
	            return EXIT_CODE_USAGE_ERR;
	        }
	        
	        stores = new RecordStore[storeList.size()];
	        for (int i = 0; i < storeList.size(); i++)
	            stores[i] = (RecordStore)storeList.elementAt(i);
        }
        catch (StoreException e)
        {
        	logger.error("Data store initialization exception" + e);
            return EXIT_CODE_INIT_ERR;
        }

        //start the session
        Date now = new Date();
        manager.startSync(stores, Long.toString(now.getTime()));
        
        //wait for it to complete
        int retCode = EXIT_CODE_SUCCESS;
        while (manager.isSyncRunning())
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
            	retCode = EXIT_CODE_SYNC_INTERRUPT;
                break;
            }
        }
        
        //check if the session is still running
        if (manager.isSyncRunning())
        {
            logger.warn("Sync session is still running on shutdown");
            retCode = EXIT_CODE_SYNC_HUNG;
        }
        
        //check the listener status
        if ( (retCode == EXIT_CODE_SUCCESS) && (! listener.isSyncSuccess()) )
        	retCode = EXIT_CODE_SYNC_ERR;
        
        return retCode;
    }
    
    public TestSyncListener getListener()
    {
    	return listener;
    }
    
    private static int getSyncType(String type)
    {
        int syncType = Integer.parseInt(type);
        if (syncType == 1)
            return SyncML.SYNC_TYPE_TWO_WAY;
        else if (syncType == 2)
            return SyncML.SYNC_TYPE_TWO_WAY_SLOW;
        else if (syncType == 3)
            return SyncML.SYNC_TYPE_ONE_WAY_CLIENT;
        else if (syncType == 4)
            return SyncML.SYNC_TYPE_REFRESH_CLIENT;
        else if (syncType == 5)
            return SyncML.SYNC_TYPE_ONE_WAY_SERVER;
        else if (syncType == 6)
            return SyncML.SYNC_TYPE_REFRESH_SERVER;
        else if (syncType == 7)
            return SyncML.SYNC_TYPE_SERVER_ALERTED;
        
        return -1;
    }

    private static String getConflictResolution(String type)
    {
        if (type.equalsIgnoreCase(RecordStore.EMI_CONFLICT_RES_DUPLICATE))
            return RecordStore.EMI_CONFLICT_RES_DUPLICATE;
        else if (type.equalsIgnoreCase(RecordStore.EMI_CONFLICT_RES_CLIENT_WINS))
            return RecordStore.EMI_CONFLICT_RES_CLIENT_WINS;
        else if (type.equalsIgnoreCase(RecordStore.EMI_CONFLICT_RES_SERVER_WINS))
            return RecordStore.EMI_CONFLICT_RES_SERVER_WINS;
        else if (type.equalsIgnoreCase(RecordStore.EMI_CONFLICT_RES_IGNORE))
            return RecordStore.EMI_CONFLICT_RES_IGNORE;
        else if (type.equalsIgnoreCase(RecordStore.EMI_CONFLICT_RES_RECENT_WINS))
            return RecordStore.EMI_CONFLICT_RES_RECENT_WINS;
        
        return null;
    }
}
