package de.christl.smsoip.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import com.mobclix.android.sdk.MobclixAdView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.application.ChangeLog;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.List;
import java.util.Vector;

/**
 *
 */
public class AllActivity extends Activity {
    public static final int EXIT = 0;
    static Vector<Activity> registeredActivities = new Vector<Activity>();

    static final int DIALOG_NO_NETWORK_ID = 0;
    private static boolean nwSettingsAlreadyShown = false;
    private boolean notLoadedDialogAlreadyShown = false;

    private static final String SAVED_INSTANCE_NWSETTINGSALREADYSHOWN = "network.settings.already.shown";
    private static final String SAVED_INSTANCE_NOTLOADEDDIALOGALREADYSHOWN = "not.loaded.dialog.already.shown";

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
        } else if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(GlobalPreferences.GLOBAL_ENABLE_NETWORK_CHECK, true)) {
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
        ChangeLog cl = new ChangeLog(this);
//        if (cl.firstRun()) {
        cl.getLogDialog().show();
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, EXIT, 0, getString(R.string.text_exit));
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

    private void showNotLoadedProvidersDialog(List<SMSSupplier> suppliers, String messageText) {
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
        messageText += "\n";
        for (SMSSupplier smsSupplier : suppliers) {
            messageText += smsSupplier.getProvider().getProviderName() + "\n";
        }
        builder.setMessage(messageText).setPositiveButton(getString(R.string.text_ok), dialogClickListener).show();
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

                Button cancel = (Button) dialog.findViewById(R.id.cancel);
                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        nwSettingsAlreadyShown = true;
                        dialog.dismiss();
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
                String APP_MARKET_URL = "market://search?q=pub:Danny Christl";

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(APP_MARKET_URL));
                AllActivity.this.startActivity(intent);
                killAll();
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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
        adView.setRefreshTime(5000);
        adView.addMobclixAdViewListener(new AdViewListener());
        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            adView.setVisibility(View.VISIBLE);
            adView.pause();
        } else {
            adView.setVisibility(View.GONE);
        }

    }

}
