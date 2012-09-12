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
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.mobclix.android.sdk.MobclixAdView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.application.changelog.ChangeLog;
import de.christl.smsoip.util.BitmapProcessor;

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
    private static final String APP_MARKET_URL = "market://search?q=SMSoIP";
    private static final String WEB_MARKET_URL = "https://play.google.com/store/search?q=SMSoIP";
    private static AllActivity context;
    private Drawable backgroundImage;

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
        app.initProviders();
        if (app.getPluginsToOld().size() > 0) {
            showNotLoadedProvidersDialog(app.getPluginsToOld(), getString(R.string.text_deprecated_providers));
        } else if (app.getPluginsToNew().size() > 0) {
            showNotLoadedProvidersDialog(app.getPluginsToNew(), getString(R.string.text_too_new_providers));
        } else if (app.getProviderEntries().size() == 0) {
            showNoProvidersDialog();
        } else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(SettingsConst.GLOBAL_ENABLE_NETWORK_CHECK, true)) {
            ConnectivityManager mgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo wifiInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            NetworkInfo mobileInfo = mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if (!wifiInfo.isConnected() && !mobileInfo.isConnected()) {

                if (!nwSettingsAlreadyShown) {
                    showDialog(DIALOG_NO_NETWORK_ID);
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(), getText(R.string.text_noNetworkAvailable), Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        }
        if (savedInstanceState != null) {
            notLoadedDialogAlreadyShown = savedInstanceState.getBoolean(SAVED_INSTANCE_NOTLOADEDDIALOGALREADYSHOWN, false);
            nwSettingsAlreadyShown = savedInstanceState.getBoolean(SAVED_INSTANCE_NWSETTINGSALREADYSHOWN, false);
        }
    }

    protected void showChangelogIfNeeded() {
        ChangeLog cl = new ChangeLog(getApplicationContext());
        if (cl.firstRun()) {
            cl.getLogDialog().show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, EXIT, Menu.CATEGORY_SECONDARY, getString(R.string.text_exit));
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
        builder.setMessage(message).setPositiveButton(getString(R.string.text_ok), dialogClickListener).show();
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
            showQuitMessage();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showQuitMessage() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    killAll();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.text_sureQuit)).setPositiveButton(getString(R.string.text_yesOption), dialogClickListener)
                .setNegativeButton(getString(R.string.text_noOption), dialogClickListener).show();
    }

    protected Dialog onCreateDialog(int id) {
        final Dialog dialog;
        switch (id) {
            case DIALOG_NO_NETWORK_ID:
                dialog = new Dialog(this);
                dialog.setContentView(R.layout.networkdialog);
                dialog.setTitle(getString(R.string.text_noNetworkAvailable));
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
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(APP_MARKET_URL));
                    AllActivity.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Market not available on device
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_MARKET_URL));
                    AllActivity.this.startActivity(intent);
                }
                killAll();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setMessage(getString(R.string.text_no_providers)).setPositiveButton(getString(R.string.text_ok), dialogClickListener).show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_INSTANCE_NOTLOADEDDIALOGALREADYSHOWN, notLoadedDialogAlreadyShown);
        outState.putBoolean(SAVED_INSTANCE_NWSETTINGSALREADYSHOWN, nwSettingsAlreadyShown);
    }

    public static void insertAds(int adviewId, Activity activity) {
        MobclixAdView adView = (MobclixAdView) activity.findViewById(adviewId);
        adView.setRefreshTime(10000);
        adView.addMobclixAdViewListener(new AdViewListener());
        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            adView.setVisibility(View.VISIBLE);
            adView.pause();
        } else {
            adView.setVisibility(View.GONE);
        }

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
