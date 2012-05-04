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
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.google.ads.AdRequest;
import com.google.ads.AdSize;
import com.google.ads.AdView;
import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.Vector;

/**
 *
 */
public class AllActivity extends Activity {
    public static final String PUBLISHER_ID = "a14f930decd44ce";
    public static final int EXIT = 0;
    static Vector<Activity> registeredActivities = new Vector<Activity>();

    static final int DIALOG_NO_NETWORK_ID = 0;
    private static boolean nwSettingsAlreadyShown = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, EXIT, 0, getString(R.string.text_exit));
        item.setIcon(R.drawable.closebutton);
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

    @Override
    protected void onStart() {
        super.onStart();
        registeredActivities.add(this);
        if (SMSoIPApplication.getApp().getDeprecatedPlugins().size() > 0) {
            showDeprecatedProvidersDialog();
        } else if (SMSoIPApplication.getApp().getProviderEntries().size() == 0) {
            showNoProvidersDialog();
        } else {

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
    }

    private void showDeprecatedProvidersDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (SMSoIPApplication.getApp().getProviderEntries().size() == 0) {
                    showNoProvidersDialog();
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String string = getString(R.string.text_deprecated_providers);
        string += "\n";
        for (SMSSupplier smsSupplier : SMSoIPApplication.getApp().getDeprecatedPlugins()) {
            string += smsSupplier.getProvider().getProviderName() + "\n";
        }
        builder.setMessage(string).setPositiveButton(getString(R.string.text_ok), dialogClickListener).show();

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

    public static void insertAds(LinearLayout layout, Activity activity) {

        // Create the adView
        AdView adView = new AdView(activity, AdSize.BANNER, PUBLISHER_ID);

        // Lookup your LinearLayout assuming it’s been given
        // the attribute android:id="@+id/mainLayout"

        // Add the adView to it
        layout.addView(adView);

        // Initiate a generic request to load it with an ad
        AdRequest adRequest = new AdRequest();
        adRequest.addTestDevice("9405EE5055BF04AE898858A2515B3588");
        adView.loadAd(adRequest);
    }
}
