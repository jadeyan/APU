/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import net.cp.ac.R;
import net.cp.ac.core.AndroidConnectionState;
import net.cp.ac.core.SisHandler;
import net.cp.ac.test.ui.MainActivity;
import net.cp.ac.test.ui.SettingsActivity;
import net.cp.ac.test.ui.SyncLogActivity;
import net.cp.ac.ui.SyncUIActivity;
import net.cp.engine.EngineSettings;

import net.cp.engine.StatusCodes;

import net.cp.engine.ConnectionState;
import net.cp.engine.SyncCounters;
import net.cp.engine.SyncError;
import net.cp.engine.SyncLog;
import net.cp.engine.SyncProgress;
import net.cp.engine.UtilityClass;
import net.cp.syncml.client.ServerAlert;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


/**
 *
 *
 * This is the main activity of the test UI. It allows the user to initiate a sync session,
 * and shows feedback from any session in progress.
 *
 * This is a test UI that demonstrates the basic features of the sync framework.
 * It does not necessarily represent best-practice in UI design or architecture,
 * but demonstrates how the features of the framework can be used.
 *
 */
public class MainActivity extends SyncUIActivity
{
    public static final int DLG_LOGIN = 0;

    private Button syncButton;
//    private TextView lastSyncDate;
//    private String syncDateText = "";

//    private Runnable lastStatusRunner;
//    private String lastStatusText;

    protected AndroidConnectionState connectionState;

    /**
     * Called when the activity is first created.
     * Initializes UI elements.
     * NOTE you cannot use any settings derived from Settings class, or assets,
     * here, as they may not have been initialized yet.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setTheme(R.style.customTheme);

        setContentView(R.layout.main);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        connectionState = AndroidConnectionState.getInstance();

        syncButton = (Button) findViewById(R.id.sync_btn);
//        lastSyncDate = (TextView) findViewById(R.id.syncdate);

        // Find out the last sync date for the sync summary...
//        try
//        {
//            SyncCounters counters = SyncLog.getLastSyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);
//            if (counters.lastSyncDate != 0)
//            {
//                Date date = new Date(counters.lastSyncDate);
//                syncDateText = date.toLocaleString();
//                lastSyncDate.setText(syncDateText);
//            }
//        }
//        catch (Exception e)
//        {
//            if(logger != null)
//                logger.error("Error getting last sync counters", e);
//        }


//        lastStatusRunner = new Runnable()
//        {
//            public void run()
//            {
//                String syncDateString = lastStatusText;
//
//                if (syncDateText.length() > 0)
//                    syncDateString = syncDateString + getText(R.string.status_on) + " " + syncDateText;
//
//                lastSyncDate.setText(syncDateString);
//                lastSyncDate.refreshDrawableState();
//            }
//        };


        syncButton.setOnClickListener(
            new OnClickListener(){

                public void onClick(View v)
                {
                    if (!areCredentialsSet())
                    {
                        startLoginActivity();
                    }
                    else
                    {
                        Intent myIntent = new Intent(getApplication(), PreSyncActivity.class);
                        if (myIntent != null)
                        {
                            if (logger != null)
                                logger.info("starting pre sync activity");

                            startActivity(myIntent);
                        }
                    }
                }
            }
        );
    }

    @Override
    /**
     * Called when the app is paused / goes off screen.
     * We unregister for SIS and CIS alerts, as the UI is not visible.
     *
     */
    protected void onPause()
    {
        unRegisterForServerAlerts();
        unRegisterForCISAlerts();

        super.onPause();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onStart()
     */
    @Override
    public void onStart()
    {
        super.onStart();

        if(uiSettings == null)
            return; //setup failed, do no more
        this.setTitle(uiSettings.title);
    }

    /**
     * Called when the UI is resumed.
     * We re-register for SIS and CIS alerts, as the UI is now visible again.
     */
    @Override
    protected void onResume()
    {
        super.onResume();

//        SyncError lastStatus = SyncLog.getLastStatus();
//        if(lastStatus != null)
//            setLastStatus(SyncLogActivity.statusToString(getApplicationContext().getResources(), lastStatus.errorCode));

        registerForServerAlerts();
        registerForCISAlerts();

        if (!areCredentialsSet())
        {
            startLoginActivity();
        }
    }

    /**
     * Called when the UI is about to be destroyed.
     * We unregister for SIS and CIS alerts.
     */
    @Override
    protected void onDestroy()
    {
        unRegisterForServerAlerts();
        unRegisterForCISAlerts();
        engineSettings.close();
        super.onDestroy();
    }

    /**
     * Sets up the options menu with buttons to view the sync log, or edit settings.
     */
    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
        boolean supRetVal = super.onCreateOptionsMenu(menu);
        MenuItem settingsItem = menu.add(0, 0,android.view.Menu.NONE, R.string.menu_settings);
        MenuItem syncLogItem = menu.add(0, 1,android.view.Menu.NONE, R.string.menu_sync_log);
        MenuItem aboutItem = menu.add(0, 2, android.view.Menu.NONE, R.string.menu_about);
        MenuItem helpItem = menu.add(0, 3, android.view.Menu.NONE, R.string.menu_help);
        MenuItem exitItem = menu.add(0, 4, android.view.Menu.NONE, R.string.menu_exit);

        settingsItem.setIcon(R.drawable.ic_menu_settings);
        syncLogItem.setIcon(R.drawable.ic_menu_sync_log);
        aboutItem.setIcon(R.drawable.ic_menu_info);
        helpItem.setIcon(R.drawable.ic_menu_help_faq_guide);
        exitItem.setIcon(R.drawable.ic_menu_exit_quit);

        return supRetVal;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    public boolean onOptionsItemSelected(android.view.MenuItem item)
    {
        Intent myIntent = null;

        switch (item.getItemId())
        {
        //start settings Activity
        case 0:
            myIntent = new Intent(getApplication(), SettingsActivity.class);
            if (myIntent != null)
            {
                if(logger != null)
                    logger.info("starting settings activity");

                startActivity(myIntent);
            }
            return true;

        //start sync log Activity
        case 1:
            myIntent = new Intent(getApplication(), SyncLogActivity.class);
            if (myIntent != null)
            {
                if(logger != null)
                    logger.info("starting sync log activity");

                startActivity(myIntent);
            }
            return true;

        case 2:
            AboutDialog.makeDialog(this).show();
            return true;

        case 3:
            myIntent = new Intent(getApplication(), HelpActivity.class);
            if (myIntent != null)
            {
                if (logger != null)
                    logger.info("starting help activity");

                startActivity(myIntent);
            }
            return true;

        case 4:
            engineSettings.close();
            this.finish();
        }
        return false;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode)
        {
            case DLG_LOGIN:
            {
                boolean result = (resultCode == RESULT_OK) ? true : false;
                if (result)
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.app_name)
                           .setIcon(R.drawable.ic_launcher_sync)
                           .setMessage(R.string.check_credentials_text)
                           .setCancelable(false)
                           .setPositiveButton(R.string.cont, new DialogInterface.OnClickListener()
                           {
                               public void onClick(DialogInterface dialog, int id)
                               {
                                   Intent myIntent = new Intent(getApplication(), PreSyncActivity.class);
                                   if (myIntent != null)
                                   {
                                       if (logger != null)
                                           logger.info("starting pre sync activity");

                                       startActivity(myIntent);
                                   }
                               }
                           })
                           .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                           {
                               public void onClick(DialogInterface dialog, int id)
                               {
                                   // Clear the username/password settings and save
                                   engineSettings.userName = "";
                                   engineSettings.userPassword = "";

                                   engineSettings.writeAllSettings();

                                   startLoginActivity();
                               }
                           });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                else
                    finish();
                break;
            }
            default:
                break;
        }
    }

    public void startSyncActivity(Bundle bundle)
    {
        Intent myIntent = new Intent(getApplication(), SyncActivity.class);
        if (myIntent != null)
        {
            if (logger != null)
                logger.info("starting sync activity");

            myIntent.putExtras(bundle);

            startActivity(myIntent);
        }
    }

    private void startLoginActivity()
    {
        Intent myIntent = new Intent(getApplication(), LoginActivity.class);
        if (myIntent != null)
        {
            if (logger != null)
                logger.info("starting login activity");

            startActivityForResult(myIntent, DLG_LOGIN);
        }
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onGetChangesEnd()
     */
    public void onGetChangesEnd()
    {
        //we don't care
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onSyncError()
     */
    public void onSyncError()
    {
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#updateSyncProgress(net.cp.engine.SyncProgress)
     */
    public void updateSyncProgress(SyncProgress progress)
    {
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onStartSyncFailure()
     */
    public void onStartSyncFailure()
    {
    }

//    private void setLastStatus(String text)
//    {
//        lastStatusText = text;
//        //output counters for last sync
//        try
//        {
//            SyncCounters counters = SyncLog.getLastSyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);
//
//            syncDateText = "";
//            if (counters.lastSyncDate != 0)
//            {
//                Date date = new Date(counters.lastSyncDate);
//                syncDateText = date.toLocaleString();
//            }
//        }
//        catch (Exception e)
//        {
//            if(logger != null)
//                logger.error("Error getting last sync counters", e);
//        }
//        runOnUiThread(lastStatusRunner);
//    }

    /**
     * {@inheritDoc}
     *
     * Reads the data from an Asset called features.properties.
     */
    protected InputStream getConfigInputStreamFromResource()
    {
        try
        {
            InputStream configStream = getAssets().open("features.properties");

            return configStream;
        }
        catch (IOException e)
        {
            if(logger != null)
                logger.error("Error reading resources", e);

            e.printStackTrace();
        }

        return null;

    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onServerAlert(net.cp.syncml.client.ServerAlert)
     */
    public void onServerAlert(ServerAlert alert)
    {
        if (logger != null)
            logger.info("MainActivity: onServerAlert() called.");

        // Are we configured to accept server alerts?
        if (!engineSettings.contactSisAllowed || !engineSettings.sisEnable)
        {
            if (logger != null)
                logger.info("Server alerted sync is not allowed or is disabled - ignoring alert");

            return;
        }

        // a null indicates that the alert is invalid
        if (alert == null)
        {
            if (logger != null)
                logger.info("Server alert is not valid - ignoring alert");

            return;
        }

        // check if we are already syncing, suspending etc...
        if(getSyncState() != StatusCodes.NONE)
        {
            if (logger != null)
                logger.info("Server alert received, but sync in progress - ignoring alert");

            return;
        }

        // check if server alerted sync is allowed during the current sync mode
        int syncMode = getSyncModeFromConnection();

        if (syncMode == EngineSettings.SYNC_MODE_OFF)
        {
            if (logger != null)
                logger.info("Server alert received, but sync mode is set to off - ignoring alert");

            return;
        }

        String message = getString(R.string.server_alert);
        String detail = "";
        if(alert.getVendorData() != null)
            detail = SisHandler.getVendorText(alert);

        final String theMessage = message;
        final String theDetail = detail;

        if (syncMode == EngineSettings.SYNC_MODE_AUTO)
        {
            if (logger != null)
                logger.info("Server alert received, and sync mode is set to auto - syncing");

            Bundle bundle = new Bundle();

            bundle.putInt(EngineSettings.SYNC_TYPE, EngineSettings.SYNC_TYPE_SIS);
            bundle.putString(EngineSettings.SYNC_MESSAGE, theMessage);

            startSyncActivity(bundle);
            return;
        }

        Runnable sisRun = new Runnable()
        {
            public void run()
            {
                Context context = getApplicationContext();
                CharSequence text = theMessage + " " + theDetail;
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                try
                {
                    //let the user see the server alert message for a while
                    Thread.sleep(5000);
                }
                catch (InterruptedException e){}

                Bundle bundle = new Bundle();

                bundle.putInt(EngineSettings.SYNC_TYPE, EngineSettings.SYNC_TYPE_SIS);
                bundle.putString(EngineSettings.SYNC_MESSAGE, theMessage);

                startSyncActivity(bundle);
            }
        };

        new Thread(sisRun).start();
    }
    
    public void onPeriodicSync(){
    	if(logger != null)
            logger.info("MainActivity: onPeriodic sync called");

        // Is Client initiated sync configured?
        if (!engineSettings.periodicAllowed)
        {
            if (logger != null)
                logger.info("Client initiated sync is not allowed - ignoring alert");

            return;
        }

        // Check if we are already syncing, suspending etc...
        if (getSyncState() != StatusCodes.NONE)
        {
            if (logger != null)
                logger.info("Client alert received, but sync in progress - ignoring alert");

            return;
        }

        // check if client alerted sync is allowed during the current sync mode
        int syncMode = getSyncModeFromConnection();

        if (syncMode == EngineSettings.SYNC_MODE_OFF)
        {
            if (logger != null)
                logger.info("Client initiated sync alert received, but sync mode is set to off - ignoring alert");

            return;
        }

        //we are ready to sync
        Runnable cisRun = new Runnable()
        {
            public void run()
            {
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, "periodic sync", duration);
                toast.show();

                try
                {
                    //let the user see the message for a while
                    Thread.sleep(5000);
                }
                catch (InterruptedException e){}

                Bundle bundle = new Bundle();

                bundle.putInt(EngineSettings.SYNC_TYPE, EngineSettings.SYNC_TYPE_CIS);
                bundle.putString(EngineSettings.SYNC_MESSAGE, "periodic sync");

                startSyncActivity(bundle);
            }
        };

        new Thread(cisRun).start();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onItemsChanged(int, int)
     */
    public void onItemsChanged(int mediaType, int numberOfChanges)
    {
        if(logger != null)
            logger.info("MainActivity: onItemsChanged() called. mediaType: " + mediaType
                    + " numberOfChanges: " + numberOfChanges);

        // Is Client initiated sync configured?
        if (!engineSettings.contactCisAllowed)
        {
            if (logger != null)
                logger.info("Client initiated sync is not allowed - ignoring alert");

            return;
        }

        // Have enough contacts have changed?
        if(engineSettings.contactMinSyncLimit > 0 && numberOfChanges < engineSettings.contactMinSyncLimit)
        {
            if (logger != null)
            {
                logger.info("Not enough contacts have changed -  minimum changes before sync is "
                        + engineSettings.contactMinSyncLimit);
            }

            return;
        }

        // Check if we are already syncing, suspending etc...
        if (getSyncState() != StatusCodes.NONE)
        {
            if (logger != null)
                logger.info("Client alert received, but sync in progress - ignoring alert");

            return;
        }

        // check if client alerted sync is allowed during the current sync mode
        int syncMode = getSyncModeFromConnection();

        if (syncMode == EngineSettings.SYNC_MODE_OFF)
        {
            if (logger != null)
                logger.info("Client initiated sync alert received, but sync mode is set to off - ignoring alert");

            return;
        }

        final int nChanges = numberOfChanges;
        final String theMessage = nChanges + getString(R.string.contacts_changed);

        //we are ready to sync
        Runnable cisRun = new Runnable()
        {
            public void run()
            {
                Context context = getApplicationContext();
                CharSequence text = theMessage;
                int duration = Toast.LENGTH_LONG;

                Toast toast = Toast.makeText(context, text, duration);
                toast.show();

                try
                {
                    //let the user see the message for a while
                    Thread.sleep(5000);
                }
                catch (InterruptedException e){}

                Bundle bundle = new Bundle();

                bundle.putInt(EngineSettings.SYNC_TYPE, EngineSettings.SYNC_TYPE_CIS);
                bundle.putString(EngineSettings.SYNC_MESSAGE, theMessage);

                startSyncActivity(bundle);
            }
        };

        new Thread(cisRun).start();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onSyncEnd()
     */
    @Override
    public void onSyncEnd()
    {
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onBindComplete()
     */
    protected void onBindComplete()
    {
        registerForServerAlerts();
        registerForCISAlerts();
    }

    public int getSyncModeFromConnection()
    {
        int connections = connectionState.getConnectionState();

        //try no cost sync first
        if(UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NO_COST))
            return engineSettings.syncModeNoCost;

        //try normal cost sync
        if(UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NORMAL_COST))
            return engineSettings.syncModeNormalCost;

        //try high cost sync
        if(UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_HIGH_COST))
            return engineSettings.syncModeHighCost;

        //not connected
        if(UtilityClass.isFlagSet(connections, ConnectionState.CONNECTION_STATE_NOT_CONNECTED))
        {
            if(logger != null)
                logger.info("not connected!");

            return EngineSettings.SYNC_MODE_OFF;
        }


        if(logger != null)
            logger.info("connection state unknown!");

        return EngineSettings.SYNC_MODE_OFF;
    }

    private boolean areCredentialsSet()
    {
        // Sometimes the engineSettings seems empty here!
        try
        {
            engineSettings.readSettings(null);
        }
        catch (Exception e)
        {
            if(logger != null)
                logger.error("Failed to read settings!");
        }

        if (engineSettings.userName.length() == 0 || engineSettings.userPassword.length() == 0 )
        {
            return false;
        }

        return true;
    }

	@Override
	public void onAlertSlowSync() {
		// TODO Auto-generated method stub
	}
}
