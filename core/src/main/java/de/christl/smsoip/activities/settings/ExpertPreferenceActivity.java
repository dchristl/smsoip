/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.activities.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 *
 */
public class ExpertPreferenceActivity extends BackgroundPreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.expert_preferences);
        setPreferenceScreen(initPreferences());
//        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        SharedPreferences.Editor edit = defaultSharedPreferences.edit();
//        edit.remove(SettingsConst.CONVERSATION_COUNT);
//        edit.commit();
    }

    private PreferenceScreen initPreferences() {

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        EditTextPreference conversationCounter = new EditTextPreference(this);
        EditText editText = conversationCounter.getEditText();
        editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        InputFilter[] FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(2);
        editText.setFilters(FilterArray);
        conversationCounter.setDialogTitle(R.string.conversation_count);
        conversationCounter.setKey(SettingsConst.CONVERSATION_COUNT);
        conversationCounter.setTitle(R.string.conversation_count);
        conversationCounter.setDefaultValue("10");
        conversationCounter.setSummary(R.string.conversation_count_description);
        root.addPreference(conversationCounter);
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);

        CheckBoxPreference conversationOrderDownwards = new CheckBoxPreference(this);
        conversationOrderDownwards.setDefaultValue(true);
        conversationOrderDownwards.setKey(SettingsConst.CONVERSATION_ORDER);
        conversationOrderDownwards.setTitle(R.string.conversation_order_down);
        conversationOrderDownwards.setSummary(R.string.conversation_order_down_description);
        root.addPreference(conversationOrderDownwards);
        boolean writeToDatabaseAvailable = SMSoIPApplication.getApp().isWriteToDatabaseAvailable();
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        boolean writeEnabled = writeToDatabaseAvailable && sharedPreferences.getBoolean(SettingsConst.GLOBAL_WRITE_TO_DATABASE, false) && sharedPreferences.getBoolean(SettingsConst.GLOBAL_ENABLE_PROVIDER_OUPUT, false);

        if (writeToDatabaseAvailable) {
            EditTextPreference saveTemplateMulti = new EditTextPreference(this);
            saveTemplateMulti.setDialogTitle(R.string.output_template_multi);
            saveTemplateMulti.setKey(SettingsConst.OUTPUT_TEMPLATE_MULTI);
            saveTemplateMulti.setTitle(R.string.output_template_multi);
            saveTemplateMulti.setDefaultValue("%a (%u->%p):");
            saveTemplateMulti.setSummary(R.string.output_template_multi_description);
            saveTemplateMulti.setEnabled(writeEnabled);
            root.addPreference(saveTemplateMulti);

            EditTextPreference saveTemplateSingle = new EditTextPreference(this);
            saveTemplateSingle.setDialogTitle(R.string.output_template_single);
            saveTemplateSingle.setKey(SettingsConst.OUTPUT_TEMPLATE_SINGLE);
            saveTemplateSingle.setTitle(R.string.output_template_single);
            saveTemplateSingle.setDefaultValue("%a (%p):");
            saveTemplateSingle.setSummary(R.string.output_template_single_description);
            saveTemplateSingle.setEnabled(writeEnabled);
            root.addPreference(saveTemplateSingle);
        }
        return root;
    }

}
