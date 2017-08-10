/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import java.util.Date;

import net.cp.ac.R;
import net.cp.ac.core.AppLogger;
import net.cp.engine.EngineSettings;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncCounters;
import net.cp.engine.SyncError;
import net.cp.engine.SyncLog;
import net.cp.syncml.client.util.Logger;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class PostSyncActivity extends Activity
{
    Logger logger;

    private EngineSettings engineSettings;

    private Button okButton;
    private TextView lastSync;

    public static final int DLG_LOGIN = 0;

    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        //get the logger if available
        logger = AppLogger.getUIInstance();

        // Get a copy of the EngineSettings, and force a reread as the login credentials might have changed.
        engineSettings = EngineSettings.getInstance(getApplicationContext(), logger);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setTheme(R.style.customTheme);

        setContentView(R.layout.postsync);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        okButton = (Button) findViewById(R.id.post_sync_ok_btn);
        lastSync = (TextView) findViewById(R.id.post_sync_status);

        SyncError lastStatus = SyncLog.getLastStatus();
        // Find out the last sync date for the sync summary...
        try
        {
            SyncCounters counters = SyncLog.getLastSyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);
            if (counters.lastSyncDate != 0)
            {
                Date date = new Date(counters.lastSyncDate);
                lastSync.setText(SyncLogActivity.statusToString(getApplicationContext().getResources(), lastStatus.errorCode)
                        + getResources().getString(R.string.status_on) + " " +
                        date.toLocaleString());
            }
        }
        catch (Exception e)
        {
            if(logger != null)
                logger.error("Error getting last sync counters", e);
        }

        final int errorCode = lastStatus.errorCode;

        okButton.setOnClickListener(
                new OnClickListener(){

                    public void onClick(View v)
                    {
                        // If the user has entered invalid credentials, launch the login activity
                        // Otherwise, go back to the main view.
                        if (errorCode == StatusCodes.SYNC_ERROR_CREDENTIALS)
                        {
                            engineSettings.userName = "";
                            engineSettings.userPassword = "";
                            engineSettings.writeAllSettings();

                            startLoginActivity();
                        }
                        else
                        {
                            Intent myIntent = new Intent(getApplication(), MainActivity.class);
                            myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            if (myIntent != null)
                            {
                                if (logger != null)
                                    logger.info("starting main activity");

                                startActivity(myIntent);
                                finish();
                            }
                        }
                    }
                }
            );
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
                                       finish();
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

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
                && keyCode == KeyEvent.KEYCODE_BACK
                && event.getRepeatCount() == 0)
        {
            // Take care of calling this method on earlier versions of
            // the platform where it doesn't exist.
            onBackPressed();
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed()
    {
        // This will be called either automatically for you on 2.0
        // or later, or by the code above on earlier versions of the
        // platform

        Intent myIntent = new Intent(getApplication(), MainActivity.class);
        myIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (myIntent != null)
        {
            if (logger != null)
                logger.info("starting main activity");

            startActivity(myIntent);
            finish();
        }
        return;
    }
}
