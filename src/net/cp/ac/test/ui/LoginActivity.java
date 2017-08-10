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
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity
{
    Logger logger;

    private EngineSettings engineSettings;

    private Button okButton;
    private Button cancelButton;
    private EditText userName;
    private EditText userPassword;
    private EditText serverAndPort;
//    private CheckBox sslEnable;

    public static final int DLG_TERMS = 0;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // get the logger if available
        logger = AppLogger.getUIInstance();

        // Get a copy of the EngineSettings, and force a reread as the login credentials might have changed.
        engineSettings = EngineSettings.getInstance(getApplicationContext(), logger);

        // add a customized title bar in layout
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setTheme(R.style.customTheme);

        setContentView(R.layout.login);

//        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.titlebar);

        okButton = (Button) findViewById(R.id.login_ok_btn);
        cancelButton = (Button) findViewById(R.id.login_cancel_btn);
        userName = (EditText) findViewById(R.id.login_acc_username);
        userPassword = (EditText) findViewById(R.id.login_acc_password);
        
        // server and port are not displayable to user
//        serverAndPort = (EditText) findViewById(R.id.login_server_hostname);
//        sslEnable = (CheckBox) findViewById(R.id.login_connect_using_ssl);

        if (userName != null)
            userName.setText(engineSettings.userName);

        if (userPassword != null)
            userPassword.setText(engineSettings.userPassword);

//        if(serverAndPort != null)
//            serverAndPort.setText(engineSettings.httpSyncServerAddress + ":" + engineSettings.httpSyncServerPort);

//        if(sslEnable != null)
//            sslEnable.setChecked(engineSettings.httpUseSSL);

        if (okButton != null)
        {
            okButton.setOnClickListener(new OnClickListener()
            {
                public void onClick(View v)
                {
                	InputMethodManager systemService = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
    				View currentFocus = LoginActivity.this.getCurrentFocus();
    				if(currentFocus != null) {
    					systemService.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
    				}
                    final String name = userName.getText().toString();
                    final String pass = userPassword.getText().toString();

                    if ((name != null && name.length() == 0) || (pass != null && pass.length() == 0))
                    {
                        // User still hasn't entered credentials. Show a toast
                        // to prompt them again
                        CharSequence text = getText(R.string.empty_credentials_error);
                        int duration = Toast.LENGTH_LONG;

                        Toast toast = Toast.makeText(getApplicationContext(), text, duration);
                        toast.show();
                    }
                    else
                    {
                        engineSettings.userName = EngineSettings.buildUsername(name, engineSettings.userDomainDefault);
                        engineSettings.userPassword = pass;

                        // server and port are not displayable to user
                        /*if(serverAndPort.getText().toString().trim().split(":").length == 2)
                        {
                            engineSettings.httpSyncServerAddress =
                                serverAndPort.getText().toString().trim().split(":")[0];

                            try
                            {
                                engineSettings.httpSyncServerPort =
                                    Integer.parseInt(serverAndPort.getText().toString().trim().split(":")[1]);
                            }
                            catch (Exception e)
                            {
                                CharSequence text = getText(R.string.port_number_should_be_a_decimal_number);
                                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                                return;
                            }

                        }
                        else
                        {
                            CharSequence text = getText(R.string.empty_server_name_and_port_error);
                            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG).show();
                            return;
                        }*/
//                        engineSettings.httpUseSSL = sslEnable.isChecked();
                        engineSettings.writeAllSettings();
                        setResult(RESULT_OK);
                        finish();
                    }
                }
            });
        }

        if (cancelButton != null)
        {
            cancelButton.setOnClickListener(new OnClickListener()
            {
                public void onClick(View v)
                {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {
        boolean supRetVal = super.onCreateOptionsMenu(menu);

        menu.add(0, 0, Menu.NONE, R.string.menu_terms_and_conditions);

        return supRetVal;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        Intent myIntent = null;

        switch (item.getItemId())
        {
        case 0:
            myIntent = new Intent(getApplication(), TermsAndConditionsActivity.class);
            if (myIntent != null)
            {
                if(logger != null)
                    logger.info("starting terms and conditions activity");

                startActivityForResult(myIntent, DLG_TERMS);
            }
            return true;
        }

        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode)
        {
            case DLG_TERMS:
            {
                if (resultCode != RESULT_OK)
                {
                    // They've cancelled on the terms and conditions page, so exit.
                    setResult(RESULT_CANCELED);
                    finish();
                }
            }
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR && keyCode == KeyEvent.KEYCODE_BACK
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

        setResult(RESULT_CANCELED);
        finish();
        return;
    }
}
