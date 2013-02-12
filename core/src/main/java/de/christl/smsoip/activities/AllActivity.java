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
 */

package de.christl.smsoip.activities;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Process;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.activities.threading.UpdateDeveloperInfoTask;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.application.changelog.ChangeLog;
import de.christl.smsoip.util.BitmapProcessor;
import org.acra.ACRA;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 */
public abstract class AllActivity extends SherlockFragmentActivity {
    public static final int EXIT = 0;
    private static List<Activity> registeredActivities = new ArrayList<Activity>();

    static final int DIALOG_NO_NETWORK_ID = 0;
    private static boolean nwSettingsAlreadyShown = false;
    private boolean notLoadedDialogAlreadyShown = false;

    private static final String SAVED_INSTANCE_NWSETTINGSALREADYSHOWN = "network.settings.already.shown";
    private static final String SAVED_INSTANCE_NOTLOADEDDIALOGALREADYSHOWN = "not.loaded.dialog.already.shown";
    private static AllActivity context;
    private Drawable backgroundImage;
    private Long lastMillis;

    protected AllActivity() {
        context = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        SMSoIPApplication.setCurrentActivity(this);
        backgroundImage = BitmapProcessor.getBackgroundImage(getResources().getConfiguration().orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        registeredActivities.add(this);
        SMSoIPApplication app = SMSoIPApplication.getApp();
        if (app.getPluginsToOld().size() > 0) {
            showNotLoadedProvidersDialog(app.getPluginsToOld(), getString(R.string.deprecated_providers));
        } else if (app.getPluginsToNew().size() > 0) {
            showNotLoadedProvidersDialog(app.getPluginsToNew(), getString(R.string.too_new_providers));
        } else if (app.getProviderEntries().size() == 0) {
            showNoProvidersDialog();
        } else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsConst.GLOBAL_ENABLE_NETWORK_CHECK, true)) {
            boolean networkEnabled = isNetworkDisabled();
            if (networkEnabled) {

                if (!nwSettingsAlreadyShown) {
                    showDialog(DIALOG_NO_NETWORK_ID);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.noNetworkAvailable), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
        if (savedInstanceState != null) {
            notLoadedDialogAlreadyShown = savedInstanceState.getBoolean(SAVED_INSTANCE_NOTLOADEDDIALOGALREADYSHOWN, false);
            nwSettingsAlreadyShown = savedInstanceState.getBoolean(SAVED_INSTANCE_NWSETTINGSALREADYSHOWN, false);
        }
        if (!isNetworkDisabled()) {
            new UpdateDeveloperInfoTask().execute(null, null);
        }
    }

    private boolean isNetworkDisabled() {
        ConnectivityManager mgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo mobileInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        return !wifiInfo.isConnected() && !mobileInfo.isConnected();
    }

    protected void showChangelogIfNeeded() {
        ChangeLog cl = new ChangeLog(this);
        if (cl.firstRunEver()) {
            cl.getWelcomeDialog().show();
        } else if (cl.firstRun()) {
            cl.getLogDialog().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, EXIT, Menu.CATEGORY_SECONDARY, getString(R.string.exit));
        item.setIcon(R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == EXIT) {
            killAll();
            return true;
        }
        return true;
    }

    private void showNotLoadedProvidersDialog(HashMap<String, SMSoIPPlugin> suppliers, String headline) {
        if (notLoadedDialogAlreadyShown) {
            return;
        }
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (SMSoIPApplication.getApp().getProviderEntries().size() == 0) {
                    showNoProvidersDialog();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        StringBuilder message = new StringBuilder(headline);
        message.append("\n");
        for (SMSoIPPlugin smsSupplier : suppliers.values()) {
            message.append(smsSupplier.getProviderName()).append("\n");
        }
        builder.setMessage(message).setPositiveButton(getString(R.string.ok), dialogClickListener).show();
        notLoadedDialogAlreadyShown = true;
    }

    public static void killAll() {
        for (Activity registeredActivity : registeredActivities) {
            nwSettingsAlreadyShown = false;
            registeredActivity.finish();
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            showQuitToast();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showQuitToast() {
        long currentMillis = System.currentTimeMillis();
        if (lastMillis == null || lastMillis + 2000 < currentMillis) {
            Toast.makeText(this, R.string.press_back_again, Toast.LENGTH_SHORT).show();
            lastMillis = currentMillis;
        } else {
            killAll();
        }

    }

    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
            case DIALOG_NO_NETWORK_ID:
                dialog = new Dialog(this);
                dialog.setContentView(R.layout.networkdialog);
                dialog.setTitle(getString(R.string.noNetworkAvailable));
                Button openNWSettingsButton = (Button) dialog.findViewById(R.id.openNetworkSettings);
                openNWSettingsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nwSettingsAlreadyShown = true;
                        startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                        dialog.dismiss();
                    }
                });
                Button wlan = (Button) dialog.findViewById(R.id.activateWLAN);
                wlan.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nwSettingsAlreadyShown = true;
                        WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
                        wm.setWifiEnabled(true);
                        dialog.dismiss();
                    }
                });
                dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        nwSettingsAlreadyShown = true;
                        new UpdateDeveloperInfoTask().execute(null, null);
                    }
                });
                break;
            default:
                dialog = null;
        }
        return dialog;
    }

    private void showNoProvidersDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_plugin_url)));
                    AllActivity.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Market not available on device
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.market_alternative)));
                    AllActivity.this.startActivity(intent);
                }
                try {
                    Process.killProcess(Process.myPid());
                } catch (Exception e) {
                    ACRA.getErrorReporter().handleSilentException(e);
                    killAll();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(getString(R.string.no_providers)).setPositiveButton(getString(R.string.ok), dialogClickListener).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_INSTANCE_NOTLOADEDDIALOGALREADYSHOWN, notLoadedDialogAlreadyShown);
        outState.putBoolean(SAVED_INSTANCE_NWSETTINGSALREADYSHOWN, nwSettingsAlreadyShown);
    }


    public static Context getActivity() {
        return context;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundImage.setCallback(null);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        backgroundImage = BitmapProcessor.getBackgroundImage(newConfig.orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }
}
