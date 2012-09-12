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

import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.preference.*;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.util.BitmapProcessor;

/**
 * managing all broadcast preferences
 */
public class SMSReceiverPreference extends PreferenceActivity {


    private Drawable backgroundImage;


    @Override
    protected void onResume() {
        super.onResume();
        SMSoIPApplication.setCurrentActivity(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_sms_receiver_settings));
        setPreferenceScreen(initPreferences());
        backgroundImage = BitmapProcessor.getBackgroundImage(getResources().getConfiguration().orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        final CheckBoxPreference receiverActive = new CheckBoxPreference(this);
        receiverActive.setDefaultValue(true);
        receiverActive.setKey(SettingsConst.RECEIVER_ACTIVATED);
        receiverActive.setTitle(R.string.text_receiver_activated);
        receiverActive.setSummary(R.string.text_receiver_activated_description);
        final CheckBoxPreference showOnlyOneNotification = new CheckBoxPreference(this);
        showOnlyOneNotification.setDefaultValue(false);
        showOnlyOneNotification.setKey(SettingsConst.RECEIVER_ONLY_ONE_NOTFICATION);
        showOnlyOneNotification.setTitle(R.string.text_only_one_notfication);
        showOnlyOneNotification.setSummary(R.string.text_only_one_notfication_description);
        boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean(SettingsConst.RECEIVER_ACTIVATED, true);
        showOnlyOneNotification.setEnabled(enabled);
        final RingtonePreference ringtonePreference = new RingtonePreference(this);
        ringtonePreference.setRingtoneType(RingtoneManager.TYPE_NOTIFICATION);
        ringtonePreference.setDefaultValue(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
        ringtonePreference.setTitle(R.string.text_choose_ringtone);
        ringtonePreference.setSummary(R.string.text_choose_ringtone_description);
        ringtonePreference.setKey(SettingsConst.RECEIVER_RINGTONE_URI);
        ringtonePreference.setEnabled(enabled);


        receiverActive.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                showOnlyOneNotification.setEnabled((Boolean) newValue);
                ringtonePreference.setEnabled((Boolean) newValue);
                return true;
            }
        });
        root.addPreference(receiverActive);
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        root.addPreference(showOnlyOneNotification);
        root.addPreference(ringtonePreference);
        return root;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        backgroundImage = BitmapProcessor.getBackgroundImage(newConfig.orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundImage.setCallback(null);
    }
}
