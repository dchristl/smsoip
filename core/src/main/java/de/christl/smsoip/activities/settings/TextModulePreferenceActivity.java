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
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.settings.preferences.TextModulePreference;
import de.christl.smsoip.activities.util.TextModuleUtil;

import java.util.Map;

/**
 * Handles the text modules (in preferences
 */
public class TextModulePreferenceActivity extends BackgroundPreferenceActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.module_preference);
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        PreferenceScreen textModuleExplanationPreference = getPreferenceManager().createPreferenceScreen(this);
        textModuleExplanationPreference.setTitle(R.string.modules_explanation);
        textModuleExplanationPreference.setSummary(R.string.modules_explanation_description);
        textModuleExplanationPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(TextModulePreferenceActivity.this);
                builder.setTitle(R.string.modules_explanation);
                builder.setMessage(R.string.modules_explanation_description_full);
                builder.setPositiveButton(R.string.ok, null);
                builder.show();
                return true;
            }
        });
        root.addPreference(textModuleExplanationPreference);
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
        Map<String, String> textModules = TextModuleUtil.getTextModules();
        for (Map.Entry<String, String> stringStringEntry : textModules.entrySet()) {
            String key = stringStringEntry.getKey();
            String value = stringStringEntry.getValue();
            TextModulePreference pref = new TextModulePreference(this, key, value, textModules);

            pref.setOnPreferenceChangeListener(onPreferenceChangeListener);
            root.addPreference(pref);
        }

        DialogPreference addNew = new TextModulePreference(this, textModules);
        addNew.setOnPreferenceChangeListener(onPreferenceChangeListener);
        root.addPreference(addNew);
        return root;
    }

}
