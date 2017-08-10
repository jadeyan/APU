/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import net.cp.ac.R;
import net.cp.ac.core.AppLogger;
import net.cp.engine.EngineSettings;
import net.cp.syncml.client.util.Logger;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

public class PreSyncActivity extends Activity
{
    Logger logger;

    private EngineSettings engineSettings;

    private Button syncButton;
    private Button cancelButton;

    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        //get the logger if available
        logger = AppLogger.getUIInstance();

        // get the settings
        engineSettings = EngineSettings.getInstance();

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setTheme(R.style.customTheme);

        setContentView(R.layout.presync);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        syncButton = (Button) findViewById(R.id.pre_sync_ok_btn);
        cancelButton = (Button) findViewById(R.id.pre_sync_cancel_btn);

        syncButton.setOnClickListener(
                new OnClickListener(){

                    public void onClick(View v)
                    {
                        Intent myIntent = new Intent(getApplication(), SyncActivity.class);
                        if (myIntent != null)
                        {
                            if (logger != null)
                                logger.info("starting sync activity");

                            Bundle bundle = new Bundle();

                            bundle.putInt(EngineSettings.SYNC_TYPE, EngineSettings.SYNC_TYPE_MANUAL);

                            myIntent.putExtras(bundle);

                            startActivity(myIntent);
                            finish();
                        }
                    }
                }
            );

        cancelButton.setOnClickListener(
                new OnClickListener(){

                    public void onClick(View v)
                    {
                        Intent myIntent = new Intent(getApplication(), MainActivity.class);
                        myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        if (myIntent != null)
                        {
                            if (logger != null)
                                logger.info("starting main activity");

                            startActivity(myIntent);
                            finish();
                        }
                    }
                }
            );
    }


    @Override
    protected void onResume()
    {
        super.onResume();

        if (logger != null)
            logger.info("PreSyncActivity - onResume start");

        if (logger != null)
            logger.info("showPreSyncActivity is : " + engineSettings.showPreSyncActivity());

        if (!engineSettings.showPreSyncActivity())
        {
            Intent myIntent = new Intent(getApplication(), SyncActivity.class);
            if (myIntent != null)
            {
                if (logger != null)
                    logger.info("starting sync activity");

                Bundle bundle = new Bundle();

                bundle.putInt(EngineSettings.SYNC_TYPE, EngineSettings.SYNC_TYPE_MANUAL);

                myIntent.putExtras(bundle);

                startActivity(myIntent);
                finish();
            }
        }
    }
}
