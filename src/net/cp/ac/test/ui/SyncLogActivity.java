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
import net.cp.engine.SyncLog;
import net.cp.syncml.client.util.Logger;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

/**
 *
 * This is a very basic Activity that shows some summary information from the Sync Log.
 *
 * @see net.cp.engine.SyncCounters
 * @see net.cp.engine.SyncLog
 *
 */
public class SyncLogActivity extends Activity
{
    Logger logger;
    private TextView lastSync;
    private TextView phoneAdded;
    private TextView serverAdded;
    private TextView phoneEdited;
    private TextView serverEdited;
    private TextView phoneDeleted;
    private TextView serverDeleted;
    private TextView phoneError;
    private TextView serverError;
    private TextView phoneConflict;
    private TextView toDate;
    private Runnable logRunner;
    private Button reset;

    /* (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        //get the logger if available
        logger = AppLogger.getUIInstance();

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setTheme(R.style.customTheme);

        setContentView(R.layout.log);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        lastSync = (TextView) findViewById(R.id.log_last_syncdate);
        phoneAdded = (TextView) findViewById(R.id.phone_added);
        serverAdded = (TextView) findViewById(R.id.server_added);
        phoneEdited = (TextView) findViewById(R.id.phone_edited);
        serverEdited = (TextView) findViewById(R.id.server_edited);
        phoneDeleted = (TextView) findViewById(R.id.phone_deleted);
        serverDeleted = (TextView) findViewById(R.id.server_deleted);
        phoneError = (TextView) findViewById(R.id.phone_error);
        serverError = (TextView) findViewById(R.id.server_error);
        phoneConflict = (TextView) findViewById(R.id.server_conflict);
        toDate = (TextView) findViewById(R.id.log_sync_to_date);

        reset = (Button)findViewById(R.id.log_sync_reset);
        reset.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				try
	            {
	                SyncLog.resetOverallSyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);
	                runOnUiThread(logRunner);
	            }
	            catch (Exception e)
	            {
	                if(logger != null)
	                    logger.error("Error resetting sync log", e);
	            }				
			}});
        
        logRunner = new Runnable()
        {
            public void run()
            {
                try
                {
                    SyncCounters counters = SyncLog.getOverallSyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);

                    String lastSyncDateText;
                    if (counters.lastSyncDate != 0)
                    {
                        Date lastSyncDate = new Date(counters.lastSyncDate);
                        lastSyncDateText = lastSyncDate.toLocaleString();
                    }
                    else
                    {
                        lastSyncDateText = getApplication().getString(R.string.status_default_sync_date);
                    }

                    lastSync.setText(lastSyncDateText);
                    phoneAdded.setText("" + counters.inItemsAdded);
                    serverAdded.setText("" + counters.outItemsAdded);
                    phoneEdited.setText("" + counters.inItemsReplaced);
                    serverEdited.setText("" + counters.outItemsReplaced);
                    phoneDeleted.setText("" + counters.inItemsDeleted);
                    serverDeleted.setText("" + counters.outItemsDeleted);
                    phoneError.setText("" + counters.inItemsFailed);
                    serverError.setText("" + counters.outItemsFailed);
                    // Conflicts only apply in the client to server direction
                    phoneConflict.setText("" + counters.outItemsConflict);

                    String txtToDate = getApplication().getString(R.string.times_start) + " "
                                        + counters.syncCount + " "
                                        + getApplication().getString(R.string.times_end);
                    toDate.setText(txtToDate);
                }
                catch (Exception e)
                {
                    if(logger != null)
                        logger.error("Error reading sync log", e);
                }
            }
        };
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onStart()
     */
    @Override
    public void onStart()
    {
        super.onStart();

        //set the text of the sync log
        runOnUiThread(logRunner);
    }

    /**
     * Sets up the options menu with buttons to view the sync log, or edit settings.
     */
    public boolean onCreateOptionsMenu(android.view.Menu menu)
    {
        boolean supRetVal = super.onCreateOptionsMenu(menu);

        MenuItem resetItem = menu.add(0, 0,android.view.Menu.NONE, R.string.menu_reset_sync_log);
        MenuItem aboutItem = menu.add(0, 1,android.view.Menu.NONE, R.string.menu_help);

        resetItem.setIcon(R.drawable.ic_menu_reset_sync_log);
        aboutItem.setIcon(R.drawable.ic_menu_help_faq_guide);

        return supRetVal;
    }

    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    public boolean onOptionsItemSelected(android.view.MenuItem item)
    {
        switch (item.getItemId())
        {
        // Reset the Sync Log
        case 0:
            try
            {
                SyncLog.resetOverallSyncCounters(EngineSettings.MEDIA_TYPE_CONTACTS);
                runOnUiThread(logRunner);
            }
            catch (Exception e)
            {
                if(logger != null)
                    logger.error("Error resetting sync log", e);
            }
            break;

        // start Help Activity
        case 1:
            Intent myIntent = new Intent(getApplication(), HelpActivity.class);
            if (myIntent != null)
            {
                if (logger != null)
                    logger.info("starting help activity");

                startActivity(myIntent);
                finish();
            }
            return true;
        }
        return false;
    }

    /**
     * Converts a status code to a string.
     *
     * @param status any valid status code. Something like: StatusCodes.SYNC_STARTING
     * @return A string to represent this error code, or empty string if nothing matches
     */
    public static String statusToString(Resources r, int status)
    {
        return statusToString(r, status, -1, -1);
    }
    /**
     * Converts a status code to a string.
     *
     * @param status any valid status code. Something like: StatusCodes.SYNC_STARTING
     * @param currentCount The current count if available, otherwise -1
     * @param totalCount The total count if available, otherwise -1
     * @return A string to represent this error code, or empty string if nothing matches
     */
    public static String statusToString(Resources r, int status, int currentCount, int totalCount)
    {
        String message = "";

        switch (status)
        {
            case StatusCodes.SYNC_STARTING: return r.getString(R.string.status_starting);
            case StatusCodes.SYNC_RESUMING: return r.getString(R.string.status_resuming);
            case StatusCodes.SYNC_SUSPENDING: return r.getString(R.string.status_suspending);
            case StatusCodes.SYNC_INITIALIZING: return r.getString(R.string.status_initializing);
            case StatusCodes.SYNC_SUSPENDED: return r.getString(R.string.status_suspended);
            case StatusCodes.SYNC_SUDDENLY_SUSPENDED: return r.getString(R.string.status_temp_problem);
            case StatusCodes.SYNC_MANUALLY_SUSPENDED: return r.getString(R.string.status_press_resume);
            case StatusCodes.SYNC_SUCCESS: return r.getString(R.string.status_success);
            case StatusCodes.SYNC_FAILED: return r.getString(R.string.status_failed);
            case StatusCodes.SYNC_START_FAILED: r.getString(R.string.status_check_settings);
            case StatusCodes.SYNC_OP_FAILED: return r.getString(R.string.status_operation_failed);
            case StatusCodes.SYNC_OP_CONFLICT: return r.getString(R.string.status_operation_conflict);
            case StatusCodes.SYNC_NO_UPDATES: return r.getString(R.string.status_no_updates);
            case StatusCodes.SYNC_INCOMPLETE: return r.getString(R.string.status_incomplete);
            case StatusCodes.SYNC_ERROR_USER_ABORT: return r.getString(R.string.status_aborted_by_user);
            case StatusCodes.SYNC_ERROR_CONNECTION: return r.getString(R.string.status_connection_error);
            case StatusCodes.SYNC_ERROR_CREDENTIALS: return r.getString(R.string.status_credentials_error);
            case StatusCodes.SYNC_ERROR_DEVICE_FULL: return r.getString(R.string.status_device_full);
            case StatusCodes.SYNC_SERVER_UPDATES: return r.getString(R.string.status_receiving_updates);
            case StatusCodes.SYNC_CLIENT_UPDATES: return r.getString(R.string.status_sending_updates);
            case StatusCodes.SYNC_COMPLETE: return r.getString(R.string.status_sync_complete);
            case StatusCodes.SYNC_RECEIVING_UPDATE: message = r.getString(R.string.status_receiving_updates); break;
            case StatusCodes.SYNC_CHECKING_CONTACTS: message = r.getString(R.string.status_checking_contacts); break;
            case StatusCodes.SYNC_SENDING_UPDATE: message = r.getString(R.string.status_sending_updates); break;
            case StatusCodes.SYNC_LOADING_STATE: message = r.getString(R.string.status_loading_contacts); break;
            case StatusCodes.SYNC_DELETED_CONTACT: message = r.getString(R.string.status_deleting_contact); break;
        }

        if(currentCount >= 0)
            message = message + " " + currentCount;

        //if we have a valid total, show it
        if(totalCount >=0 && currentCount <= totalCount)
            message = message + "/" + totalCount;

        return message;
    }
}
