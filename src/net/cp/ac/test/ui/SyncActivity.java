/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import java.io.InputStream;
import java.io.Serializable;

import net.cp.ac.R;
import net.cp.ac.ui.SyncUIActivity;
import net.cp.engine.EngineSettings;
import net.cp.engine.StatusCodes;
import net.cp.engine.SyncProgress;
import net.cp.syncml.client.ServerAlert;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class SyncActivity extends SyncUIActivity
{
    private Button syncAbortButton;
    private Button suspendResumeButton;
    private TextView sent;
    private TextView received;
    private TextView errors;
    private TextView progressHeading;
    private TextView progressDetails;
    private int sentItems = 0;
    private int receivedItems = 0;
    private int errorsCount = 0;
    private byte sync_type;
    private String sync_message;

    private Runnable statusRunner;
    private Runnable buttonRunner;

    private CharSequence progressHeadingText;
    private CharSequence progressDetailsText;

    private ButtonState buttonState;
    /**
     * Called when the activity is first created.
     * Initializes UI elements.
     * NOTE you cannot use any settings derived from Settings class, or assets,
     * here, as they may not have been initialized yet.
     */
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Find out how this sync was initiated.  If there's no extras, it was a CIS...
        Bundle bundle = getIntent().getExtras();

        if (bundle != null)
        {
            sync_type = (byte)bundle.getInt(EngineSettings.SYNC_TYPE);
            sync_message = bundle.getString(EngineSettings.SYNC_MESSAGE);
        }
        else
            sync_type = EngineSettings.SYNC_TYPE_CIS;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, 
        				WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setTheme(R.style.customTheme);

        setContentView(R.layout.sync);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        sentItems = 0;
        receivedItems = 0;

        progressHeadingText = getText(R.string.heading_placeholder);
        progressDetailsText = getText(R.string.details_placeholder);

        if(buttonState == null)
        {
            buttonState = new ButtonState();

            buttonState.suspendResumeButtonText = getText(R.string.suspend);
            buttonState.syncCancelButtonText = getText(R.string.abort);
        }

        syncAbortButton = (Button) findViewById(R.id.sync_abort_btn);
        suspendResumeButton = (Button) findViewById(R.id.sync_suspend_resume_btn);

        sent = (TextView) findViewById(R.id.status_sent);
        received = (TextView) findViewById(R.id.status_received);
        errors = (TextView) findViewById(R.id.status_errors);

        progressHeading = (TextView) findViewById(R.id.status_progressheading);
        progressDetails = (TextView) findViewById(R.id.status_progressdetails);

        statusRunner = new Runnable()
        {
            public void run()
            {
                received.setText(getText(R.string.status_received).toString() + receivedItems);
                sent.setText(getText(R.string.status_sent).toString() + sentItems);
                errors.setText(getText(R.string.status_errors).toString() + errorsCount);

                progressHeading.setText(progressHeadingText);
                progressDetails.setText(progressDetailsText);

                //make sure the items get updated
                received.refreshDrawableState();
                sent.refreshDrawableState();
                errors.refreshDrawableState();
                progressHeading.refreshDrawableState();
                progressDetails.refreshDrawableState();
            }
        };

        buttonRunner = new Runnable()
        {
            public void run()
            {
            	Drawable suspendDraw = suspendResumeButton.getBackground().mutate();
                suspendResumeButton.setText(buttonState.suspendResumeButtonText);
                syncAbortButton.setText(buttonState.syncCancelButtonText);

                if (buttonState.enableSuspendButton == false){
                    suspendResumeButton.setEnabled(false);
                    suspendDraw.setColorFilter(Color.argb(168, 255, 75, 75), PorterDuff.Mode.DST_OUT);
                }
                else{
                    suspendResumeButton.setEnabled(true);
                    suspendDraw.clearColorFilter();
                }
                Drawable abortDraw = syncAbortButton.getBackground().mutate();
                if(buttonState.enableAbortButton == false){
                	syncAbortButton.setEnabled(false);
                	abortDraw.setColorFilter(Color.argb(168, 255, 75, 75), PorterDuff.Mode.DST_OUT);
                }else{
                	syncAbortButton.setEnabled(true);
                	abortDraw.clearColorFilter();
                }
            }
        };
        
        alertShow = new Runnable(){
        	public void run(){

        		AlertDialog.Builder builder = new AlertDialog.Builder(SyncActivity.this);
                builder.setTitle(R.string.app_name)
                       .setIcon(R.drawable.ic_launcher_sync)
                       .setMessage(R.string.slow_sync_alert)
                       .setCancelable(false)
                       .setPositiveButton(R.string.cont, new DialogInterface.OnClickListener()
                       {
                           public void onClick(DialogInterface dialog, int id)
                           {
                           }
                       })
                       /*.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                       {
                           public void onClick(DialogInterface dialog, int id)
                           {
                           }
                       })*/;
                AlertDialog alert = builder.create();
                alert.show();
        	}
        };

        syncAbortButton.setOnClickListener(
            new OnClickListener(){

                public void onClick(View v)
                {
                    syncCancel();
                }
            }
        );

        suspendResumeButton.setOnClickListener(
            new OnClickListener(){
                public void onClick(View v)
                {
                    suspendResume();
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

        registerForServerAlerts();
        registerForCISAlerts();

        /**
         * Restore any previous UI status based on the current
         * state of the sync in progress (if applicable).
         */
        updateSyncProgress(getLastProgress());

        int syncState = getSyncState();

        // If there's so sync start one
        if(syncState == StatusCodes.NONE)
        {
            // Default message, but replace if another is available...
            String message = getString(R.string.syncing_contacts);

            if (sync_message != null)
                message = sync_message;

            doContactSync(message);
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

        super.onDestroy();
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

        syncCancel();
        return;
    }

    @Override
    protected InputStream getConfigInputStreamFromResource()
    {
        return null;
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onBindComplete()
     */
    @Override
    protected void onBindComplete()
    {
        registerForServerAlerts();
        registerForCISAlerts();

        /**
         * Restore any previous UI status based on the current
         * state of the sync in progress (if applicable).
         */

        updateSyncProgress(getLastProgress());

        int syncState = getSyncState();

        // If there's so sync start one
        if(syncState == StatusCodes.NONE)
        {
            // Default message, but replace if another is available...
            String message = getString(R.string.syncing_contacts);

            if (sync_message != null)
                message = sync_message;

            doContactSync(message);
        }
    }

     /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onGetChangesEnd()
     */
    @Override
    public void onGetChangesEnd()
    {
        // Do nothing
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onSyncEnd()
     */
    @Override
    public void onSyncEnd()
    {
        syncComplete();
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onSyncError()
     */
    @Override
    public void onSyncError()
    {
        errorsCount++;
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#updateSyncProgress(net.cp.engine.SyncProgress)
     */
    @Override
    public void updateSyncProgress(SyncProgress progress)
    {
        updateButtonState(progress);

        if(progress == null)
            return;

        String heading = SyncLogActivity.statusToString(getApplicationContext().getResources(),
                progress.statusHeading);
        String detail = SyncLogActivity.statusToString(getApplicationContext().getResources(),
                progress.statusDetail, progress.currentCount, progress.totalCount);
        setStatus(heading, detail);

        if(progress.statusHeading == StatusCodes.SYNC_SERVER_UPDATES)
            setReceivedCount(progress.currentCount);
        else if(progress.statusHeading == StatusCodes.SYNC_CLIENT_UPDATES)
            setSentCount(progress.currentCount);
    }

    /**
     * Enable/disable certain buttons, change labels, depending on the state of the sync
     *
     * @param progress The state of the current sync
     */
    private void updateButtonState(SyncProgress progress)
    {
        if(progress == null)
            return;

        if(progress.statusHeading == StatusCodes.SYNC_SUSPENDING)
        {
            buttonState.suspendResumeButtonText = getText(R.string.suspending);
            buttonState.enableSuspendButton = false;
            buttonState.syncCancelButtonText = getText(R.string.abort);
        }
        else if(progress.statusHeading == StatusCodes.SYNC_ABORTING)
        {
            buttonState.suspendResumeButtonText = getText(R.string.suspend);
            buttonState.enableSuspendButton = true;
            buttonState.syncCancelButtonText = getText(R.string.cancelling);
            buttonState.enableAbortButton = false;
        }
        else if(progress.statusHeading == StatusCodes.SYNC_SUSPENDED)
        {
            buttonState.suspendResumeButtonText = getText(R.string.resume);
            buttonState.enableSuspendButton = true;
            buttonState.syncCancelButtonText = getText(R.string.abort);
            buttonState.enableAbortButton = true;
        }
        else if(progress.statusHeading == StatusCodes.SYNC_RESUMING)
        {
            buttonState.suspendResumeButtonText = getText(R.string.resuming);
            buttonState.enableSuspendButton = false;
            buttonState.syncCancelButtonText = getText(R.string.abort);
            buttonState.enableAbortButton = true;
        }
        else if (progress.statusHeading == StatusCodes.SYNC_STARTING)
        {
            // Suspend button is disabled until the sync is properly started
            buttonState.suspendResumeButtonText = getText(R.string.suspend);
            buttonState.syncCancelButtonText = getText(R.string.abort);
            buttonState.enableSuspendButton = false;
            buttonState.enableAbortButton = true;
        }
        else if (canEnableSuspendButton(progress))
        {
            // enable Suspend button now sync has started (no longer just reading contacts)
            buttonState.suspendResumeButtonText = getText(R.string.suspend);
            buttonState.syncCancelButtonText = getText(R.string.abort);
            buttonState.enableSuspendButton = true;
            buttonState.enableAbortButton = true;
        }
        else //some other mid-sync state
        {
            buttonState.suspendResumeButtonText = getText(R.string.suspend);
            buttonState.syncCancelButtonText = getText(R.string.abort);
        }

        runOnUiThread(buttonRunner);
    }

    private boolean canEnableSuspendButton(SyncProgress progress)
    {
        if (progress.statusHeading == StatusCodes.SYNC_LOADING_STATE && progress.statusDetail == StatusCodes.SYNC_ENUMERATING_CONTACTS)
            return false;
        else
            return true;
    }

    /**
     * Updates the UI to show the number of items received.
     *
     * @param incomingAddCount The number of items received in the current sync session.
     */
    public void setReceivedCount(int incomingAddCount)
    {
        receivedItems = incomingAddCount;
        runOnUiThread(statusRunner);
    }

    /**
     * Updates the UI to show the number of items sent.
     *
     * @param outgoingAddSuccessCount The number of items sent in the current sync session.
     */
    public void setSentCount(int outgoingAddSuccessCount)
    {
        sentItems = outgoingAddSuccessCount;
        runOnUiThread(statusRunner);
    }

    /**
     * Updates the UI with the current progress of the sync
     * @param heading The new progress heading. If null, the old heading will be maintained.
     * @param detail The new progress detail.  If null, the old detail will be maintained.
     */
    public void setStatus(String heading, String detail)
    {
        if(heading != null)
            progressHeadingText = heading;

        if(detail != null)
            progressDetailsText = detail;

        runOnUiThread(statusRunner);
    }

    private void doContactSync(String message)
    {
        int batteryLevel = getBatteryPercent();

        if (batteryLevel == 0 && !isBound() && bindCalled())
        {
            if (logger != null)
                logger.debug("doContactSync - can't get batteryLevel as bind not complete, returning.");

            return;
        }

        if (batteryLevel < engineSettings.minBatteryLevel)
        {
            setStatus("Cannot sync due to low battery: " + batteryLevel + "%", "");
            startMainActivity();
            return;
        }

        setStatus(message, null);
        startSync(EngineSettings.MEDIA_TYPE_CONTACTS);
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onServerAlert(net.cp.syncml.client.ServerAlert)
     */
    public void onServerAlert(ServerAlert alert)
    {
        if (logger != null)
            logger.info("Server alerted sync received, but already syncing - ignoring alert");

        return;
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onItemsChanged(int, int)
     */
    public void onItemsChanged(int mediaType, int numberOfChanges)
    {
        if (logger != null)
            logger.info("Client initiated sync received, but already syncing - ignoring alert");

        return;
    }

    /* (non-Javadoc)
     * @see net.cp.ac.ui.SyncUIActivity#onStartSyncFailure()
     */
    public void onStartSyncFailure()
    {
        if (logger != null)
            logger.info("SyncActivity: sync failed!");

        // Only display the Post Sync page if it's a manual sync.
        if (sync_type == EngineSettings.SYNC_TYPE_MANUAL)
            startPostSyncActivity();
        else
            startMainActivity();
    }

    private void syncComplete()
    {
        if(logger != null)
            logger.info("SyncActivity: syncComplete()");

        // Only display the Post Sync page if it's a manual sync.
        if (sync_type == EngineSettings.SYNC_TYPE_MANUAL)
            startPostSyncActivity();
        else
            startMainActivity();
    }

    private void startMainActivity()
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

    private void startPostSyncActivity()
    {
        Intent myIntent = new Intent(getApplication(), PostSyncActivity.class);
        if (myIntent != null)
        {
            if (logger != null)
                logger.info("starting post sync activity");

            startActivity(myIntent);
            finish();
        }
    }

    /**
     * This method cancels a sync that's in progress.
     * If the sync is in progress/suspending/suspended, this will cancel it.
     */
    protected synchronized void syncCancel()
    {
        int syncState = getSyncState();

        //already syncing, cancel!
        if(syncState == StatusCodes.SYNC_IN_PROGRESS || syncState == StatusCodes.SYNC_SUSPENDED)
        {
            buttonState.syncCancelButtonText = getText(R.string.cancelling);
            buttonState.enableAbortButton = false;
            runOnUiThread(buttonRunner);
            abortSync();
        }
    }

    /**
     * This method toggles between suspend and resume.
     * If the sync is running, and not suspending/aborting/suspended, running this will suspend it.
     * If the sync is already suspended, running this will resume it.
     */
    protected synchronized void suspendResume()
    {
        int syncState = getSyncState();

        //already suspended, resume it!
        if(syncState == StatusCodes.SYNC_SUSPENDED)
        {
            buttonState.suspendResumeButtonText = getText(R.string.resuming);
            buttonState.enableSuspendButton = false;
            runOnUiThread(buttonRunner);
            resumeSync();
        }

        else if(syncState == StatusCodes.SYNC_IN_PROGRESS) //begin suspend
        {
            buttonState.suspendResumeButtonText = getText(R.string.suspending);
            buttonState.enableSuspendButton = false;
            runOnUiThread(buttonRunner);
            suspendSync();
        }
    }
    
    Runnable alertShow;

	@Override
	public void onAlertSlowSync() {
		this.runOnUiThread(alertShow);
	}
}

class ButtonState implements Serializable
{
    private static final long serialVersionUID = 4059795166044892341L;
    //used to decide button visibility/state

    public CharSequence suspendResumeButtonText;
    public CharSequence syncCancelButtonText;

    public boolean enableSuspendButton = true;
    public boolean enableAbortButton = true;
}
