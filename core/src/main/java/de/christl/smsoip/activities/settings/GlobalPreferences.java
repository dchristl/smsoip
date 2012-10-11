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
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.Toast;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.settings.preferences.FontSizePreference;
import de.christl.smsoip.activities.threading.ProcessImageAndSetBackgroundTask;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.application.changelog.ChangeLog;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.util.BitmapProcessor;
import org.acra.ACRA;

import java.util.LinkedHashMap;
import java.util.Map;

public class GlobalPreferences extends BackgroundPreferenceActivity {

    private static final int ACTIVITY_SELECT_IMAGE = 10;

    private ProcessImageAndSetBackgroundTask processImageAndSetBackgroundTask;


    private Integer adjustment = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_program_settings));
        setPreferenceScreen(initPreferences());
        if (savedInstanceState != null) {
            adjustment = savedInstanceState.getInt(SettingsConst.EXTRA_ADJUSTMENT, 0);
        } else {
            adjustment = (Integer) getIntent().getExtras().get(SettingsConst.EXTRA_ADJUSTMENT);
        }
    }


    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        addBaseSettings(root);
        addBehaviourSettings(root);
        addLayoutSettings(root);
        addMiscellaneousSettings(root);
        return root;
    }

    private void addMiscellaneousSettings(PreferenceScreen root) {
        PreferenceCategory miscCategory = new PreferenceCategory(this);
        miscCategory.setTitle(R.string.text_category_stuff);
        root.addPreference(miscCategory);
        PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(this);
        String uriString = getString(R.string.homepage);
        intentPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(uriString)));
        intentPref.setTitle(R.string.text_visit_project_page);
        intentPref.setSummary(R.string.text_visit_project_page_description);
        root.addPreference(intentPref);

        PreferenceScreen youtubeIntent = getPreferenceManager().createPreferenceScreen(this);
        youtubeIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.youtube_alternative)));
                    GlobalPreferences.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Youtube app not installed on device
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.youtube_alternative)));
                    GlobalPreferences.this.startActivity(intent);
                }
                return true;
            }
        });
        youtubeIntent.setTitle(R.string.text_youtube_video);
        youtubeIntent.setSummary(R.string.text_youtube_video_description);
        root.addPreference(youtubeIntent);
        final int storeId = SMSoIPApplication.getApp().getStoreId();
        PreferenceScreen pluginIntent = getPreferenceManager().createPreferenceScreen(this);
        pluginIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {

                    String marketUrl = getResources().getStringArray(R.array.market_plugin_url)[storeId];
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
                    GlobalPreferences.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Market not available on device
                    String alternativeMarketUrl = getResources().getStringArray(R.array.market_alternative)[storeId];
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(alternativeMarketUrl));
                    GlobalPreferences.this.startActivity(intent);
                }
                return true;
            }
        });
        pluginIntent.setTitle(R.string.text_visit_plugin_page);
        String visitPluginDescription = getResources().getStringArray(R.array.visit_plugin_page_description)[storeId];
        pluginIntent.setSummary(visitPluginDescription);
        root.addPreference(pluginIntent);

        if (SMSoIPApplication.getApp().isAdsEnabled()) {
            PreferenceScreen adfreeIntent = getPreferenceManager().createPreferenceScreen(this);
            adfreeIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        String marketUrl = getResources().getStringArray(R.array.adfree_market_url)[storeId];
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
                        GlobalPreferences.this.startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        //Market not available on device
                        String marketUrlAlternative = getResources().getStringArray(R.array.adfree_alternative)[storeId];
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrlAlternative));
                        GlobalPreferences.this.startActivity(intent);
                    }
                    return true;
                }
            });
            adfreeIntent.setTitle(R.string.text_donate_plugin_page);
            adfreeIntent.setSummary(R.string.text_donate_plugin_page_description);
            root.addPreference(adfreeIntent);
        }
        PreferenceScreen welcomePref = getPreferenceManager().createPreferenceScreen(this);
        welcomePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                ChangeLog cl = new ChangeLog(GlobalPreferences.this);
                cl.getWelcomeDialog().show();
                return true;
            }
        });
        welcomePref.setTitle(R.string.text_reshow_welcome);
        welcomePref.setSummary(R.string.text_reshow_welcome_description);
        root.addPreference(welcomePref);
    }

    private void addLayoutSettings(PreferenceScreen root) {
        PreferenceCategory layoutCategory = new PreferenceCategory(this);
        layoutCategory.setTitle(R.string.text_category_layout);
        root.addPreference(layoutCategory);
        root.addPreference(new FontSizePreference(this));
        CheckBoxPreference enableCompactMode = new CheckBoxPreference(this);
        enableCompactMode.setDefaultValue(false);
        enableCompactMode.setKey(SettingsConst.GLOBAL_ENABLE_COMPACT_MODE);
        enableCompactMode.setTitle(R.string.text_enable_compact_mode);
        enableCompactMode.setSummary(R.string.text_enable_compact_mode_description);
        root.addPreference(enableCompactMode);
        PreferenceScreen backgroundImageIntent = getPreferenceManager().createPreferenceScreen(this);
        backgroundImageIntent.setTitle(R.string.text_background_image);
        backgroundImageIntent.setSummary(R.string.text_background_image_description);
        backgroundImageIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (BitmapProcessor.isBackgroundImageSet()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(GlobalPreferences.this);
                    builder.setTitle(R.string.text_background_image);
                    builder.setMessage(R.string.text_background_image_dialog);
                    builder.setPositiveButton(R.string.text_background_image_dialog_pick, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startImagePicker();
                            dialog.dismiss();
                        }
                    });
                    builder.setNegativeButton(R.string.text_background_image_dialog_reset, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            writeImageUriAndUpdateBackground(null);
                            dialog.dismiss();
                        }
                    });
                    builder.show();
                } else {
                    startImagePicker();
                }

                return true;
            }
        });
        root.addPreference(backgroundImageIntent);
    }

    private void addBehaviourSettings(PreferenceScreen root) {
        PreferenceCategory behaviourCategory = new PreferenceCategory(this);
        behaviourCategory.setTitle(R.string.text_category_behaviour);
        root.addPreference(behaviourCategory);
        PreferenceScreen receiverIntent = getPreferenceManager().createPreferenceScreen(this);
        receiverIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent pref = new Intent(GlobalPreferences.this, SMSReceiverPreference.class);
                startActivity(pref);
                return true;
            }
        });

        receiverIntent.setTitle(R.string.text_react_on_incoming_sms);
        receiverIntent.setSummary(R.string.text_react_on_incoming_sms_description);
        root.addPreference(receiverIntent);
        CheckBoxPreference enableNetworkCheck = new CheckBoxPreference(this);
        enableNetworkCheck.setDefaultValue(true);
        enableNetworkCheck.setKey(SettingsConst.GLOBAL_ENABLE_NETWORK_CHECK);
        enableNetworkCheck.setTitle(R.string.text_enable_network_check);
        enableNetworkCheck.setSummary(R.string.text_enable_network_check_description);
        root.addPreference(enableNetworkCheck);
        CheckBoxPreference enableInfoOnStartup = new CheckBoxPreference(this);
        enableInfoOnStartup.setDefaultValue(false);
        enableInfoOnStartup.setKey(SettingsConst.GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP);
        enableInfoOnStartup.setTitle(R.string.text_enable_info_update);
        enableInfoOnStartup.setSummary(R.string.text_enable_info_update_description);
        root.addPreference(enableInfoOnStartup);
        boolean writeToDatabaseAvailable = SMSoIPApplication.getApp().isWriteToDatabaseAvailable();
        CheckBoxPreference writeToDataBase = new CheckBoxPreference(this);
        writeToDataBase.setKey(SettingsConst.GLOBAL_WRITE_TO_DATABASE);
        writeToDataBase.setDefaultValue(writeToDatabaseAvailable);
        writeToDataBase.setEnabled(writeToDatabaseAvailable);
        writeToDataBase.setTitle(R.string.text_write_to_database);
        writeToDataBase.setSummary(writeToDatabaseAvailable ? R.string.text_write_to_database_description : R.string.text_not_supported_on_device);
        root.addPreference(writeToDataBase);
        final CheckBoxPreference enableProviderOutput = new CheckBoxPreference(this);
        enableProviderOutput.setKey(SettingsConst.GLOBAL_ENABLE_PROVIDER_OUPUT);
        enableProviderOutput.setDefaultValue(false);
        enableProviderOutput.setEnabled(writeToDatabaseAvailable && getPreferenceManager().getSharedPreferences().getBoolean(SettingsConst.GLOBAL_WRITE_TO_DATABASE, false));
        enableProviderOutput.setTitle(R.string.text_enable_provider_output);
        enableProviderOutput.setSummary(writeToDatabaseAvailable ? R.string.text_enable_provider_output_description : R.string.text_not_supported_on_device);
        root.addPreference(enableProviderOutput);

        writeToDataBase.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                enableProviderOutput.setEnabled(((Boolean) newValue));
                return true;
            }
        });
    }

    private void startImagePicker() {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, ACTIVITY_SELECT_IMAGE);
    }

    private void addBaseSettings(PreferenceScreen root) {
        PreferenceCategory mainCategory = new PreferenceCategory(this);
        mainCategory.setTitle(R.string.text_category_base);
        root.addPreference(mainCategory);
        final ListPreference listPref = new ListPreference(this);
        Map<String, SMSoIPPlugin> providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        if (providerEntries.size() > 1) {
            Map<String, String> providersWithNames = new LinkedHashMap<String, String>();
            providersWithNames.put((String) getText(R.string.text_no_default_Provider), "");
            for (SMSoIPPlugin providerEntry : providerEntries.values()) {
                providersWithNames.put(providerEntry.getProviderName(), providerEntry.getSupplierClassName());
            }
            listPref.setEntries(providersWithNames.keySet().toArray(new CharSequence[providersWithNames.size()]));
            listPref.setEntryValues(providersWithNames.values().toArray(new CharSequence[providersWithNames.size()]));
            listPref.setDialogTitle(R.string.text_default_provider);
            listPref.setKey(SettingsConst.GLOBAL_DEFAULT_PROVIDER);
            listPref.setTitle(R.string.text_default_provider);
            listPref.setSummary(R.string.text_default_provider_description);
            if (listPref.getValue() == null) {
                listPref.setValue("");    //set the value if nothing selected
            }
            root.addPreference(listPref);
        }
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        EditTextPreference defaultAreaCode = new EditTextPreference(this);
        defaultAreaCode.setDialogTitle(R.string.text_area_code);
        defaultAreaCode.setKey(SettingsConst.GLOBAL_AREA_CODE);
        defaultAreaCode.setTitle(R.string.text_area_code);
        defaultAreaCode.setDefaultValue("49");
        defaultAreaCode.setSummary(R.string.text_area_code_description);
        defaultAreaCode.setOnPreferenceChangeListener(getListener());
        root.addPreference(defaultAreaCode);
        PreferenceScreen textModulePreference = getPreferenceManager().createPreferenceScreen(this);
        textModulePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent pref = new Intent(GlobalPreferences.this, TextModulePreferenceActivity.class);
                startActivity(pref);
                return true;
            }
        });

        textModulePreference.setTitle(R.string.text_text_module_preference);
        textModulePreference.setSummary(R.string.text_text_module_preference_description);
        root.addPreference(textModulePreference);
    }

    private Preference.OnPreferenceChangeListener getListener() {
        return new Preference.OnPreferenceChangeListener() {


            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                String value;
                try {
                    value = newValue.toString();
                    value = value.replaceFirst("\\+", "");
                    value = value.replaceFirst("^0*", "");
                    Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    value = editTextPreference.getText();
                    String text = String.format(getString(R.string.text_reset_area_code), value);
                    Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT);
                    toast.show();
                }

                editTextPreference.setText(value);
                return false;
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case ACTIVITY_SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    try {
                        String selectedImage = imageReturnedIntent.getData().toString();
                        writeImageUriAndUpdateBackground(selectedImage);
                    } catch (Exception e) {
                        ACRA.getErrorReporter().handleSilentException(e);//TODO remove when stable
                    }
                }
        }
    }

    private void writeImageUriAndUpdateBackground(String selectedImage) {
        Toast toast = Toast.makeText(this, R.string.text_background_will_be_set, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.CENTER | Gravity.CENTER, 0, 0);
        toast.show();
        if (processImageAndSetBackgroundTask != null) {
            processImageAndSetBackgroundTask.cancel(true);
        }
        processImageAndSetBackgroundTask = new ProcessImageAndSetBackgroundTask();
        ErrorReporterStack.put(LogConst.PROCESS_IMAGE_AND_SET_BACKGROUND_TASK_CREATED_AND_STARTED);
        processImageAndSetBackgroundTask.execute(selectedImage, String.valueOf(adjustment));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SettingsConst.EXTRA_ADJUSTMENT, adjustment);
    }

}


