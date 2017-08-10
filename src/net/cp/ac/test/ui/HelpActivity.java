/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */

package net.cp.ac.test.ui;

import net.cp.ac.R;
import net.cp.engine.EngineSettings;
import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HelpActivity extends Activity
{
//    private EngineSettings engineSettings;

    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.help);

        // get the settings
//        engineSettings = EngineSettings.getInstance();

        final WebView webview = (WebView)findViewById(R.id.help_webview);
        webview.getSettings().setJavaScriptEnabled(true);

        if (webview != null)
        {
//            webview.loadUrl(engineSettings.getHelpFileLocation());
        	String help_location = getApplication().getResources().getString(R.string.config_app_helpfile);
        	webview.loadUrl(help_location);

            webview.setWebViewClient(new WebViewClient()
            {
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    return super.shouldOverrideUrlLoading(webview, url);
                }
            });
        }
    }
}
