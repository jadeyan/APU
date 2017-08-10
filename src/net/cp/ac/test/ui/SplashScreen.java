/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import java.util.Timer;
import java.util.TimerTask;

import net.cp.ac.R;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
/**
 * This is the welcome activity of PBC Android App, with the standard launch mode.
 * used Timer and TimerTask instead of Thread to automatically get navigate to the MainActivity,
 * direct thread uses was causing the ANR on some devices so removed it in this implementation.
 *
 * @author rupesh_sasne
 */
public class SplashScreen extends Activity
{
    protected int _splashTime = 2000;

    protected Timer timer = new Timer();

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splashscreen);

        timer.schedule(new TimerTask(){
            @Override
            public void run() {
                startActivity(new Intent(getApplication(), MainActivity.class));
                finish();
            }
        }, _splashTime);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        super.onTouchEvent(event);
        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            timer.cancel();
            startActivity(new Intent(getApplication(), MainActivity.class));
            finish();
        }
        return true;
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
        timer.cancel();
        finish();
    }
}
