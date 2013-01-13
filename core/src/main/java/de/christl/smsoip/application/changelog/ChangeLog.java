/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 *
 *     This file is modified version of Changelog, provided by Karsten Priegnitz
 *     original copyright:
 *
 * Copyright (C) 2011, Karsten Priegnitz
 *
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 *
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 */

package de.christl.smsoip.application.changelog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import de.christl.smsoip.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ChangeLog {

    private final Context context;
    private String lastVersion, thisVersion;

    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "global.version.key";

    /**
     * Constructor
     * <p/>
     * Retrieves the version names and stores the new version name in
     *
     * @param context
     */
    public ChangeLog(Context context) {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * Constructor
     * <p/>
     * Retrieves the version names and stores the new version name in
     * SharedPreferences
     *
     * @param context
     * @param sp      the shared preferences to store the last version name into
     */
    public ChangeLog(Context context, SharedPreferences sp) {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, "");
        try {
            this.thisVersion = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e) {
            this.thisVersion = "?";
        }

        // save new version number to preferences
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(VERSION_KEY, this.thisVersion);
        editor.commit();
    }

    /**
     * @return The version name of the last installation of this app (as
     *         described in the former manifest). This will be the same as
     *         returned by <code>getThisVersion()</code> the second time
     *         this version of the app is launched (more precisely: the
     *         second time ChangeLog is instantiated).
     */
    public String getLastVersion() {
        return this.lastVersion;
    }

    /**
     * @return The version name of this app as described in the manifest.
     * @see AndroidManifest.xml#android:versionName
     */
    public String getThisVersion() {
        return this.thisVersion;
    }

    /**
     * @return <code>true</code> if this version of your app is started the
     *         first time
     */
    public boolean firstRun() {
        return !this.lastVersion.equals(this.thisVersion);
    }

    /**
     * @return <code>true</code> if your app is started the first time ever.
     *         Also <code>true</code> if your app was deinstalled and
     *         installed again.
     */
    public boolean firstRunEver() {
        return "".equals(this.lastVersion);
    }

    /**
     * @return an AlertDialog displaying the changes since the previous
     *         installed version of your app (what's new).
     */
    public AlertDialog getLogDialog() {
        return this.getDialog(false);
    }

    public AlertDialog getWelcomeDialog() {
        return this.getDialog(true);
    }


    private AlertDialog getDialog(boolean full) {
        LayoutInflater factory = LayoutInflater.from(context);
        final View dialogView = factory.inflate(R.layout.changelogdialog, null);
        WebView wv = (WebView) dialogView.findViewById(R.id.webView);
        wv.setBackgroundColor(0); // transparent
        // wv.getSettings().setDefaultTextEncodingName("utf-8");
        wv.loadDataWithBaseURL(null, this.getLog(full, null), "text/html", "UTF-8", null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        int changelog_title = full ? R.string.welcome_title : R.string.changelog_title;
        builder.setTitle(context.getResources().getString(
                changelog_title))
                .setView(dialogView)
                .setPositiveButton(
                        context.getResources().getString(
                                R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    public AlertDialog getChangelogDialogByView(InputStream dialogContent) {
        LayoutInflater factory = LayoutInflater.from(context);
        final View dialogView = factory.inflate(R.layout.changelogdialog, null);
        WebView wv = (WebView) dialogView.findViewById(R.id.webView);
        wv.setBackgroundColor(0); // transparent
        // wv.getSettings().setDefaultTextEncodingName("utf-8");
        wv.loadDataWithBaseURL(null, this.getLog(true, dialogContent), "text/html", "UTF-8", null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this.context);
        builder.setTitle(context.getResources().getString(
                R.string.changelog_title))
                .setView(dialogView)
                .setPositiveButton(
                        context.getResources().getString(
                                R.string.ok),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        return builder.create();
    }

    /**
     * @return HTML displaying the changes since the previous
     *         installed version of your app (what's new)
     */
    public String getLog() {
        return this.getLog(false, null);
    }


    /**
     * modes for HTML-Lists (bullet, numbered)
     */
    private enum Listmode {
        NONE,
        ORDERED,
        UNORDERED,
    }

    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;
    private static final String EOCL = "END_OF_CHANGE_LOG";

    private String getLog(boolean full, InputStream logToShowIs) {
        sb = new StringBuffer();
        BufferedReader br = null;
        InputStreamReader streamReader = null;
        try {
            if (logToShowIs == null) {
                int logToShow = full ? R.raw.welcome : R.raw.changelog;
                logToShowIs = context.getResources().openRawResource(logToShow);
            }
            streamReader = new InputStreamReader(logToShowIs);
            br = new BufferedReader(streamReader);

            String line;
            boolean advanceToEOVS = false; // if true: ignore further version sections
            while ((line = br.readLine()) != null) {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$') {
                    // begin of a version section
                    this.closeList();
                    String version = line.substring(1).trim();
                    // stop output?
                    if (!full) {
                        if (this.lastVersion.equals(version)) {
                            advanceToEOVS = true;
                        } else if (version.equals(EOCL)) {
                            advanceToEOVS = false;
                        }
                    }
                } else if (!advanceToEOVS) {
                    switch (marker) {
                        case '%':
                            // line contains version title
                            this.closeList();
                            sb.append("<div class='title'>").append(line.substring(1).trim()).append("</div>\n");
                            break;
                        case '_':
                            // line contains version title
                            this.closeList();
                            sb.append("<div class='subtitle'>").append(line.substring(1).trim()).append("</div>\n");
                            break;
                        case '!':
                            // line contains free text
                            this.closeList();
                            sb.append("<div class='freetext'>").append(line.substring(1).trim()).append("</div>\n");
                            break;
                        case '#':
                            // line contains numbered list item
                            this.openList(Listmode.ORDERED);
                            sb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                            break;
                        case '*':
                            // line contains bullet list item
                            this.openList(Listmode.UNORDERED);
                            sb.append("<li>").append(line.substring(1).trim()).append("</li>\n");
                            break;
                        default:
                            // no special character: just use line as is
                            this.closeList();
                            sb.append(line).append("\n");
                    }
                }
            }
            this.closeList();

        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), e.getMessage());
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
                if (streamReader != null) {
                    streamReader.close();
                }
                if (logToShowIs != null) {
                    logToShowIs.close();
                }
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
            }
        }

        return sb.toString();
    }

    private void openList(Listmode listMode) {
        if (this.listMode != listMode) {
            closeList();
            if (listMode == Listmode.ORDERED) {
                sb.append("<div class='list'><ol>\n");
            } else if (listMode == Listmode.UNORDERED) {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList() {
        if (this.listMode == Listmode.ORDERED) {
            sb.append("</ol></div>\n");
        } else if (this.listMode == Listmode.UNORDERED) {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }


    /**
     * manually set the last version name - for testing purposes only
     *
     * @param lastVersion
     */
    void setLastVersion(String lastVersion) {
        this.lastVersion = lastVersion;
    }
}