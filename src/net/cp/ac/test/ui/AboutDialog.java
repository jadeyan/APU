/**
 * Copyright 2004-2012 Critical Path, Inc. All Rights Reserved.
 */
package net.cp.ac.test.ui;

import net.cp.ac.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AboutDialog
{
    public static Dialog makeDialog(Context context)
    {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.about, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setIcon(R.drawable.ic_launcher_sync)
                .setTitle(context.getResources().getString(R.string.app_name))
                .setView(view)
                .setPositiveButton(android.R.string.ok, null)
                .create();

        TextView copyright = (TextView) view.findViewById(R.id.copyright);
        String txt = context.getResources().getString(R.string.copyright);
        final SpannableString s = new SpannableString(txt);

        copyright.setPadding(5, 5, 5, 5);
        Linkify.addLinks(s, Linkify.ALL);
        copyright.setText(s);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());

        PackageManager pm = context.getPackageManager();
        try
        {
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            TextView build = (TextView) view.findViewById(R.id.build);
            build.setText(context.getResources().getString(R.string.version)
                    + " " + pi.versionName
                    + context.getResources().getString(R.string.build)
                    + " " + Integer.toString(pi.versionCode));
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
        }
        return dialog;
    }
}
