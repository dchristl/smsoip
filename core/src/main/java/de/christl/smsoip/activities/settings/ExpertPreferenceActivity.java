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
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.InputFilter;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import com.google.ads.Ad;
import com.google.ads.AdListener;
import com.google.ads.AdRequest;
import com.google.ads.InterstitialAd;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.AdViewListener;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.threading.imexport.ExportSettingsTask;
import de.christl.smsoip.activities.threading.imexport.ImportSettingsTask;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.ui.ShowLastMessagesDialog;

/**
 *
 */
public class ExpertPreferenceActivity extends BackgroundPreferenceActivity implements AdListener {

    private InterstitialAd interstitial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.expert_preferences);
        setPreferenceScreen(initPreferences());
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

        root.addPreference(new AdPreference(this));
        if (SMSoIPApplication.getApp().isAdsEnabled()) {

            Preference bannerExchange = new Preference(this);
            bannerExchange.setTitle(R.string.ad_exchange);
            bannerExchange.setSummary(R.string.ad_exchange_description);
            bannerExchange.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Create the interstitial
                    interstitial = new InterstitialAd(ExpertPreferenceActivity.this, AdViewListener.ADMOB_PUBLISHER_ID);

                    AdRequest adRequest = new AdRequest();
                    adRequest.addTestDevice("E3234EBC64876258C233EAA63EE49966");
                    interstitial.loadAd(adRequest);

                    interstitial.setAdListener(ExpertPreferenceActivity.this);
                    return true;
                }
            });
            root.addPreference(bannerExchange);
        }
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

        DigitsKeyListener keyListener = DigitsKeyListener.getInstance("0123456789ABCDEFabcdef");
        EditTextPreference incomingColor = new EditTextPreference(this);
        EditText incomingEditText = incomingColor.getEditText();
        incomingEditText.setKeyListener(keyListener);
        FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(8);
        incomingEditText.setFilters(FilterArray);
        incomingColor.setDialogTitle(R.string.incoming_color);
        incomingColor.setKey(SettingsConst.INCOMING_COLOR);
        incomingColor.setTitle(R.string.incoming_color);
        incomingColor.setDefaultValue(ShowLastMessagesDialog.INCOMING_DEFAULT_COLOR);
        incomingColor.setSummary(R.string.incoming_color_description);
        root.addPreference(incomingColor);

        EditTextPreference incomingTextColor = new EditTextPreference(this);
        EditText incomingTextColorEditText = incomingTextColor.getEditText();
        incomingTextColorEditText.setKeyListener(keyListener);
        FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(8);
        incomingTextColorEditText.setFilters(FilterArray);
        incomingTextColor.setDialogTitle(R.string.incoming_text_color);
        incomingTextColor.setKey(SettingsConst.INCOMING_TEXT_COLOR);
        incomingTextColor.setTitle(R.string.incoming_text_color);
        incomingTextColor.setDefaultValue(ShowLastMessagesDialog.INCOMING_DEFAULT_TEXT_COLOR);
        incomingTextColor.setSummary(R.string.incoming_text_color_description);
        root.addPreference(incomingTextColor);

        EditTextPreference outgoingColor = new EditTextPreference(this);
        EditText outgoingEditText = outgoingColor.getEditText();
        outgoingEditText.setKeyListener(keyListener);
        outgoingEditText.setFilters(FilterArray);
        outgoingColor.setDialogTitle(R.string.outgoing_color);
        outgoingColor.setKey(SettingsConst.OUTGOING_COLOR);
        outgoingColor.setTitle(R.string.outgoing_color);
        outgoingColor.setDefaultValue(ShowLastMessagesDialog.OUTGOING_DEFAULT_COLOR);
        outgoingColor.setSummary(R.string.outgoing_color_description);
        root.addPreference(outgoingColor);


        EditTextPreference outgoingTextColor = new EditTextPreference(this);
        EditText outgoingTextColorEditText = outgoingTextColor.getEditText();
        outgoingTextColorEditText.setKeyListener(keyListener);
        FilterArray = new InputFilter[1];
        FilterArray[0] = new InputFilter.LengthFilter(8);
        outgoingTextColorEditText.setFilters(FilterArray);
        outgoingTextColor.setDialogTitle(R.string.outgoing_text_color);
        outgoingTextColor.setKey(SettingsConst.OUTGOING_TEXT_COLOR);
        outgoingTextColor.setTitle(R.string.outgoing_text_color);
        outgoingTextColor.setDefaultValue(ShowLastMessagesDialog.OUTGOING_DEFAULT_TEXT_COLOR);
        outgoingTextColor.setSummary(R.string.outgoing_text_color_description);
        root.addPreference(outgoingTextColor);

        Preference exportSettings = new Preference(this);
        exportSettings.setTitle(R.string.export_settings);
        exportSettings.setSummary(R.string.export_settings_description);
        exportSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ExportSettingsTask task = new ExportSettingsTask(ExpertPreferenceActivity.this);
                task.execute();
                return true;
            }
        });
        root.addPreference(exportSettings);
        Preference importSettings = new Preference(this);
        importSettings.setTitle(R.string.import_settings);
        importSettings.setSummary(R.string.import_settings_description);
        importSettings.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ImportSettingsTask task = new ImportSettingsTask(ExpertPreferenceActivity.this);
                task.execute();
                return true;
            }
        });
        root.addPreference(importSettings);
        return root;
    }


    @Override
    public void onReceiveAd(Ad ad) {
        if (ad == interstitial) {
            interstitial.show();
        }
    }

    @Override
    public void onFailedToReceiveAd(Ad ad, AdRequest.ErrorCode errorCode) {

    }

    @Override
    public void onPresentScreen(Ad ad) {

    }

    @Override
    public void onDismissScreen(Ad ad) {

    }

    @Override
    public void onLeaveApplication(Ad ad) {

    }
}
