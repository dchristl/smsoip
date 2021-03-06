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

import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;

/**
 * managing all broadcast preferences
 */
public class SMSReceiverPreference extends BackgroundPreferenceActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.sms_receiver_settings));
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        final CheckBoxPreference receiverActive = new CheckBoxPreference(this);
        receiverActive.setDefaultValue(true);
        receiverActive.setKey(SettingsConst.RECEIVER_ACTIVATED);
        receiverActive.setTitle(R.string.receiver_activated);
        receiverActive.setSummary(R.string.receiver_activated_description);
        final CheckBoxPreference showOnlyOneNotification = new CheckBoxPreference(this);
        showOnlyOneNotification.setDefaultValue(false);
        showOnlyOneNotification.setKey(SettingsConst.RECEIVER_ONLY_ONE_NOTFICATION);
        showOnlyOneNotification.setTitle(R.string.only_one_notfication);
        showOnlyOneNotification.setSummary(R.string.only_one_notfication_description);
        boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean(SettingsConst.RECEIVER_ACTIVATED, true);
        showOnlyOneNotification.setEnabled(enabled);
        final RingtonePreference ringtonePreference = new RingtonePreference(this);
        ringtonePreference.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
        ringtonePreference.setDefaultValue(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
        ringtonePreference.setTitle(R.string.choose_ringtone);
        ringtonePreference.setSummary(R.string.choose_ringtone_description);
        ringtonePreference.setKey(SettingsConst.RECEIVER_RINGTONE_URI);
        ringtonePreference.setEnabled(enabled);

        final CheckBoxPreference vibrateActive = new CheckBoxPreference(this);
        vibrateActive.setDefaultValue(true);
        vibrateActive.setKey(SettingsConst.RECEIVER_VIBRATE_ACTIVATED);
        vibrateActive.setTitle(R.string.receiver_vibrate_activated);
        vibrateActive.setSummary(R.string.receiver_vibrate_activated_description);
        vibrateActive.setEnabled(enabled);


        final CheckBoxPreference ledActive = new CheckBoxPreference(this);
        ledActive.setDefaultValue(true);
        ledActive.setKey(SettingsConst.RECEIVER_LED_ACTIVATED);
        ledActive.setTitle(R.string.receiver_led_activated);
        ledActive.setSummary(R.string.receiver_led_activated_description);
        ledActive.setEnabled(enabled);


        final CheckBoxPreference showDialog = new CheckBoxPreference(this);
        showDialog.setDefaultValue(true);
        showDialog.setKey(SettingsConst.RECEIVER_SHOW_DIALOG);
        showDialog.setTitle(R.string.show_message_dialog);
        showDialog.setSummary(R.string.show_message_dialog_description);
        showDialog.setEnabled(enabled);

        receiverActive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                showOnlyOneNotification.setEnabled((Boolean) newValue);
                ringtonePreference.setEnabled((Boolean) newValue);
                vibrateActive.setEnabled((Boolean) newValue);
                ledActive.setEnabled((Boolean) newValue);
                showDialog.setEnabled((Boolean) newValue);
                return true;
            }
        });
        root.addPreference(receiverActive);
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        root.addPreference(showDialog);
        root.addPreference(showOnlyOneNotification);
        root.addPreference(ringtonePreference);
        root.addPreference(vibrateActive);
        root.addPreference(ledActive);
        return root;
    }

}
