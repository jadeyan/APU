/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import net.cp.ac.R;
import net.cp.ac.logic.PeriodicSyncAlarmManager;
import net.cp.engine.EngineSettings;
import android.content.Context;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.widget.Toast;

public class SettingsActivity extends PreferenceActivity
{
    private EngineSettings engineSettings;

    // Declare the preferences a user can change
    private EditTextPreference username;
    private EditTextPreference password;
    private EditTextPreference syncUrl;
    private EditTextPreference syncPort;
    private EditTextPreference detect;
    private EditTextPreference period_days;
    private CheckBoxPreference changes;
    private CheckBoxPreference online;
    private CheckBoxPreference use_ssl;
    private CheckBoxPreference period_sync;
    private ListPreference sync_mode;
    private ListPreference roam_mode;
    private ListPreference wifi_mode;
    private ListPreference conflict_res;
    private TimePickerPreference picker;

    private Runnable detectChangesRunner;


    protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        engineSettings = EngineSettings.getInstance();

        setPreferenceScreen(createPreferenceScreens());
    }

    // read the values and set them
    @Override
    protected void onPause()
    {
        super.onPause();

        engineSettings.userName = EngineSettings.buildUsername(username.getText(), engineSettings.userDomainDefault);
        engineSettings.userPassword = password.getText();

        if (engineSettings.showHostname())
        {
            engineSettings.httpSyncServerAddress = syncUrl.getText();
        }

        if (engineSettings.showPort())
        {
            engineSettings.httpSyncServerPort = Integer.parseInt(syncPort.getText());
        }

        if (engineSettings.showSSL())
        {
            engineSettings.httpUseSSL = use_ssl.isChecked();
        }

        if (engineSettings.showSIS())
        {
            engineSettings.contactSisAllowed = online.isChecked();
        }
        engineSettings.contactCisAllowed = changes.isChecked();
        engineSettings.contactMinSyncLimit = Integer.parseInt(detect.getText());
        engineSettings.syncModeNormalCost = convertSyncModeValue(sync_mode.getValue());
        engineSettings.syncModeHighCost = convertSyncModeValue(roam_mode.getValue());
        engineSettings.syncModeNoCost = convertSyncModeValue(wifi_mode.getValue());
        engineSettings.contactConflictResolution = convertConflictResolutionValue(conflict_res.getValue());
        engineSettings.periodicAllowed = period_sync.isChecked();
        engineSettings.periodicSyncDaysLimits = Integer.parseInt(period_days.getText());
        engineSettings.periodicSyncHourOfDay = picker.getHour();
        engineSettings.periodicSyncMinute = picker.getMinute();
        
        engineSettings.writeAllSettings();
        updateAlarm();
    }
    
    private void updateAlarm(){
    	if(PeriodicSyncAlarmManager.getInstance() != null){
    		PeriodicSyncAlarmManager instance = PeriodicSyncAlarmManager.getInstance();
    		if(instance == null)
    		{
    			PeriodicSyncAlarmManager.startSyncService(getApplicationContext());
    			return;
    		}	
			if(engineSettings.periodicAllowed)
			{
				instance.startAlarm(getApplication());
			}else
			{
				instance.cancelAlarm(getApplication());
			}
    	}
    }

    private PreferenceScreen createPreferenceScreens()
    {
        PreferenceScreen main = getPreferenceManager().createPreferenceScreen(this);

        // Add in the General Settings PreferenceCategory
        main.addPreference(createPreferenceCategory(R.string.gen_settings));

        // Add in the Account Details PreferenceScreen
        main.addPreference(createGeneralPreferenceScreen());

        // Add in the Sync Options PreferenceCategory
        main.addPreference(createPreferenceCategory(R.string.sync_options));

        // Add in the Auto Sync PreferenceScreen
        main.addPreference(createAutoSyncPreferenceScreen());

        // Add in the Home network Sync mode ListPreferenceCategory
        sync_mode = createListPreference(R.string.sync_mode,
                R.string.sync_mode_summary,
                R.array.sync_mode_options,
                R.array.sync_mode_options_values);
        String value = "" + engineSettings.syncModeNormalCost;
        sync_mode.setDefaultValue(value);
        main.addPreference(sync_mode);

        // Add in the Roaming mode ListPreferenceCategory
        roam_mode = createListPreference(R.string.roam_mode,
                R.string.roam_mode_summary,
                R.array.sync_mode_options,
                R.array.sync_mode_options_values);
        value = "" + engineSettings.syncModeHighCost;
        roam_mode.setDefaultValue(value);
        main.addPreference(roam_mode);

        // Add in the Wifi mode ListPreferenceCategory
        wifi_mode = createListPreference(R.string.wifi_mode,
                R.string.wifi_mode_summary,
                R.array.sync_mode_options,
                R.array.sync_mode_options_values);
        value = "" + engineSettings.syncModeNoCost;
        wifi_mode.setDefaultValue(value);
        main.addPreference(wifi_mode);

        // Add in the Contact options PreferenceCategory
        main.addPreference(createPreferenceCategory(R.string.contact_options));

        // Add in the conflict resolution ListPreferenceCategory
        conflict_res = createListPreference(R.string.conflict_res,
                R.string.conflict_res_summary,
                R.array.conflict_res_options,
                R.array.conflict_res_options_values);
        value = "" + engineSettings.contactConflictResolution;
        conflict_res.setDefaultValue(value);
        main.addPreference(conflict_res);
        
        return main;
    }

    private PreferenceCategory createPreferenceCategory(int title)
    {
        PreferenceCategory category = new PreferenceCategory(this);

        category.setTitle(title);

        return category;
    }

    private EditTextPreference createEditTextPreference(int title, String defValue)
    {
        EditTextPreference txtPref = new EditTextPreference(this);

        txtPref.setTitle(title);
        txtPref.setDialogTitle(title);
        txtPref.setDefaultValue(defValue);

        return txtPref;
    }

    private CheckBoxPreference createCheckBoxPreference(int title, Boolean defValue)
    {
        CheckBoxPreference pref = new CheckBoxPreference(this);

        pref.setTitle(title);
        pref.setDefaultValue(defValue);

        return pref;
    }

    private ListPreference createListPreference(int title, int summary, int entries, int values)
    {
        ListPreference list = new ListPreference(this);

        list.setTitle(title);
        list.setSummary(summary);
        list.setEntries(entries);
        list.setEntryValues(values);

        return list;
    }
    
    private String getUserNameNoDomain(String name){
    	if(name!=null){
    		String[] userAndDomain = name.split("@");
    		return userAndDomain[0];
    	}
    	return null;
    }

    private PreferenceScreen createGeneralPreferenceScreen()
    {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

        screen.setTitle(R.string.acc_details);
        screen.setSummary(R.string.acc_details_summary);

        // Add in the username and password EditTextPreferences
        username = createEditTextPreference(R.string.acc_username, getUserNameNoDomain(engineSettings.userName));
        screen.addPreference(username);
        password = createEditTextPreference(R.string.acc_password, engineSettings.userPassword);
        password.getEditText().setTransformationMethod(new PasswordTransformationMethod());

        screen.addPreference(password);

        if (engineSettings.showHostname())
        {
            // Add in the server name EditTextPreferences
            syncUrl = createEditTextPreference(R.string.acc_server_hostname, engineSettings.httpSyncServerAddress);
            screen.addPreference(syncUrl);
        }

        if (engineSettings.showPort())
        {
            // Add in the server port EditTextPreferences (the setDefaultValue doesn't seem to like ints, so convert to String first)
            String port = "" + engineSettings.httpSyncServerPort;
            syncPort = createEditTextPreference(R.string.acc_server_port, port);
            syncPort.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
            screen.addPreference(syncPort);
        }

        if (engineSettings.showSSL())
        {
            // Add in the remember useSSL CheckBoxPreference
            use_ssl = createCheckBoxPreference(R.string.acc_use_ssl, engineSettings.httpUseSSL);
            screen.addPreference(use_ssl);
        }

        return screen;
    }

    private PreferenceScreen createAutoSyncPreferenceScreen()
    {
        PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(this);

        screen.setTitle(R.string.auto_sync);
        screen.setSummary(R.string.auto_sync_summary);

        if (engineSettings.showSIS())
        {
            online = createCheckBoxPreference(R.string.auto_sync_online, engineSettings.contactSisAllowed);
            online.setSummary(R.string.auto_sync_online_summary);
            screen.addPreference(online);
        }

        changes = createCheckBoxPreference(R.string.auto_sync_changes, engineSettings.contactCisAllowed);
        changes.setSummary(R.string.auto_sync_changes_summary);
        screen.addPreference(changes);

        String min = "" + engineSettings.contactMinSyncLimit;
        detect = createEditTextPreference(R.string.auto_sync_detect, min);
        detect.setSummary(R.string.auto_sync_detect_summary);
        detect.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);

        // detect changes should only be enabled is CIS is allowed
        if (!engineSettings.contactCisAllowed)
            detect.setEnabled(false);

        screen.addPreference(detect);

        detectChangesRunner = new Runnable()
        {
            public void run()
            {
                boolean value = changes.isChecked();

                if (value)
                    detect.setEnabled(true);
                else
                    detect.setEnabled(false);
            }
        };

        changes.setOnPreferenceClickListener(new
                Preference.OnPreferenceClickListener()
                {
                    public boolean onPreferenceClick(Preference preference)
                    {
                        runOnUiThread(detectChangesRunner);
                        return true;
                    }
                });

        detect.setOnPreferenceChangeListener(new
                Preference.OnPreferenceChangeListener()
                {
                    public boolean onPreferenceChange(Preference preference, Object newValue)
                    {
                        // only called if user clicks positive button.  So validate new text
                        try
                        {
                            int min = Integer.parseInt((String)newValue);

                            if (min < 1 || min > 100)
                                displayMinimumErrorToast(R.string.minimum_error);
                            else
                                detect.setText("" + min);

                        }
                        catch (NumberFormatException e)
                        {
                            displayMinimumErrorToast(R.string.minimum_error);
                        }

                        return false;
                    }
                });
        
        period_sync = createCheckBoxPreference(R.string.auto_sync_period, engineSettings.periodicAllowed);
        screen.addPreference(period_sync);

        String days = ""+engineSettings.periodicSyncDaysLimits;
        period_days = createEditTextPreference(R.string.auto_sync_period_days, days);
        period_days.setSummary(R.string.auto_sync_period_days_summary);
        period_days.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
        
        period_days.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                // only called if user clicks positive button. So validate new text
                if (newValue instanceof String) {
                    try {
                        String v = (String) newValue;
                        int days = Integer.parseInt(v);

                        if (days <= 0) {
                            displayMinimumErrorToast(R.string.days_range_error);
                        } else if (days > 365) {
                            displayMinimumErrorToast(R.string.days_range_error);
                        } else {
                            period_days.setText(v);
                        }
                    } catch (NumberFormatException e) {
                        displayMinimumErrorToast(R.string.days_range_error);
                    }
                }
                return false;
            }
        });
        
        picker = new TimePickerPreference(this, null, engineSettings.periodicSyncHourOfDay, engineSettings.periodicSyncMinute);
        picker.setTitle(R.string.auto_sync_period_time);
        
        
        if(!engineSettings.periodicAllowed)
        {
        	period_days.setEnabled(false);
        	picker.setEnabled(false);
        }
        
//        picker.on
        
        picker.setOnPreferenceClickListener(new OnPreferenceClickListener()
        {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				// TODO Auto-generated method stub
				return false;
			}
		});
        screen.addPreference(period_days);
        screen.addPreference(picker);
        
        final Runnable period_run = new Runnable(){

			@Override
			public void run() {
				boolean checked = period_sync.isChecked();
				period_days.setEnabled(checked);
				picker.setEnabled(checked);
			}};
			
		period_sync.setOnPreferenceClickListener(new OnPreferenceClickListener(){

			@Override
			public boolean onPreferenceClick(Preference preference) {
				runOnUiThread(period_run);
				return true;
			}});
        
        return screen;
    }

    private void displayMinimumErrorToast(int textID)
    {
        // There's an error with the value entered.
        Context context = getApplicationContext();
        CharSequence text = getText(textID);
        int duration = Toast.LENGTH_LONG;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    private byte convertSyncModeValue(String value)
    {
        if (value.equalsIgnoreCase("2"))
            return EngineSettings.SYNC_MODE_AUTO;
        else if (value.equalsIgnoreCase("1"))
            return EngineSettings.SYNC_MODE_REMIND;
        else
            return EngineSettings.SYNC_MODE_OFF;
    }

    private byte convertConflictResolutionValue(String value)
    {
        if (value.equalsIgnoreCase("2"))
            return EngineSettings.CONFLICT_RES_SERVER_WINS;
        else if (value.equalsIgnoreCase("4"))
            return EngineSettings.CONFLICT_RES_CLIENT_WINS;
        else if (value.equalsIgnoreCase("8"))
            return EngineSettings.CONFLICT_RES_DUPLICATE;
        else
            return EngineSettings.CONFLICT_RES_IGNORE;
    }
}
