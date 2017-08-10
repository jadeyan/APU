/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.test.ui;

import net.cp.ac.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Window;
import android.webkit.WebView;

public class TermsAndConditionsActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.termsandconditions);

        WebView webview = (WebView)findViewById(R.id.webview);
        if (webview != null)
            webview.loadUrl(getResources().getString(R.string.config_app_termsandcondition));
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
        setResult(RESULT_OK);
        finish();
        return;
    }
}
