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

package de.christl.smsoip.activities.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;

/**
 * managing all broadcast preferences
 */
public class SMSReceiverPreference extends PreferenceActivity {

    public static final String RECEIVER_ACTIVATED = "receiver.activated";
    public static final String RECEIVER_ONLY_ONE_NOTFICATION = "receiver.only.one.notification";
    public static final String RECEIVER_ABORT_BROADCAST = "receiver.abort.broadcast";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_sms_receiver_settings));
        setPreferenceScreen(initPreferences());
        getWindow().setBackgroundDrawableResource(R.drawable.background_holo_dark);
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        final CheckBoxPreference receiverActive = new CheckBoxPreference(this);
        receiverActive.setDefaultValue(true);
        receiverActive.setKey(RECEIVER_ACTIVATED);
        receiverActive.setTitle(R.string.text_receiver_activated);
        receiverActive.setSummary(R.string.text_receiver_activated_description);
        final CheckBoxPreference showOnlyOneNotification = new CheckBoxPreference(this);
        showOnlyOneNotification.setDefaultValue(false);
        showOnlyOneNotification.setKey(RECEIVER_ONLY_ONE_NOTFICATION);
        showOnlyOneNotification.setTitle(R.string.text_only_one_notfication);
        showOnlyOneNotification.setSummary(R.string.text_only_one_notfication_description);
        boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean(RECEIVER_ACTIVATED, true);
        showOnlyOneNotification.setEnabled(enabled);
        final CheckBoxPreference abortBroadcast = new CheckBoxPreference(this);
        abortBroadcast.setDefaultValue(false);
        abortBroadcast.setKey(RECEIVER_ABORT_BROADCAST);
        abortBroadcast.setTitle(R.string.text_abort_broadcast);
        abortBroadcast.setSummary(R.string.text_abort_broadcast_description);
        abortBroadcast.setEnabled(enabled);
        abortBroadcast.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (abortBroadcast.isChecked()) {
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(SMSReceiverPreference.this);
                    alertDialog.setTitle(R.string.text_warning);
                    alertDialog.setMessage(getString(R.string.text_warning_disable_notification));
                    alertDialog.setPositiveButton(R.string.text_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            abortBroadcast.setChecked(true);
                            dialog.dismiss();
                        }
                    });
                    alertDialog.setNegativeButton(R.string.text_cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            abortBroadcast.setChecked(false);
                            dialog.dismiss();
                        }
                    });
                    alertDialog.show();
                    return true;
                }
                return false;
            }

        });
        receiverActive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                showOnlyOneNotification.setEnabled((Boolean) newValue);
                abortBroadcast.setEnabled((Boolean) newValue);
                return true;
            }
        });
        root.addPreference(receiverActive);
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        root.addPreference(showOnlyOneNotification);
        root.addPreference(abortBroadcast);
        return root;
    }

}
