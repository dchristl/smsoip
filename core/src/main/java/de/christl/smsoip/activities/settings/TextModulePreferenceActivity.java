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

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.settings.preferences.TextModulePreference;
import de.christl.smsoip.activities.util.TextModuleUtil;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.util.BitmapProcessor;

import java.util.Map;

/**
 * Handles the text modules (in preferences
 */
public class TextModulePreferenceActivity extends PreferenceActivity {

    private Drawable backgroundImage;

    @Override
    protected void onResume() {
        super.onResume();
        SMSoIPApplication.setCurrentActivity(this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.text_text_module_preference);
        setPreferenceScreen(initPreferences());
        backgroundImage = BitmapProcessor.getBackgroundImage(getResources().getConfiguration().orientation);
        getWindow().setBackgroundDrawable(backgroundImage);
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        Map<String, String> textModules = TextModuleUtil.getTextModules();
        //TODO add explanation preference
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        Preference.OnPreferenceChangeListener onPreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                TextModulePreferenceActivity correspondingActivity = TextModulePreferenceActivity.this;
                correspondingActivity.startActivity(correspondingActivity.getIntent());
                correspondingActivity.finish();
                return true;
            }
        };
        for (Map.Entry<String, String> stringStringEntry : textModules.entrySet()) {
            String key = stringStringEntry.getKey();
            String value = stringStringEntry.getValue();
            TextModulePreference pref = new TextModulePreference(this, key, value);

            pref.setOnPreferenceChangeListener(onPreferenceChangeListener);
            root.addPreference(pref);
        }

        DialogPreference addNew = new TextModulePreference(this);
        addNew.setOnPreferenceChangeListener(onPreferenceChangeListener);
        root.addPreference(addNew);
        return root;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundImage != null) {
            backgroundImage.setCallback(null);
        }
    }
}
