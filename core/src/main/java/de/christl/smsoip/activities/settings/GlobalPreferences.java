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
import android.content.res.Configuration;
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
import de.christl.smsoip.util.BitmapProcessor;
import org.acra.ErrorReporter;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class GlobalPreferences extends PreferenceActivity {


    public static final String GLOBAL_SIGNATURE = "global.signature";
    public static final String GLOBAL_DEFAULT_PROVIDER = "global.default.provider";
    public static final String GLOBAL_AREA_CODE = "global.area.code";
    public static final String GLOBAL_ENABLE_NETWORK_CHECK = "global.enable.network.check";
    public static final String GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP = "global.update.info.startup";
    public static final String GLOBAL_ENABLE_COMPACT_MODE = "global.compact.mode";
    public static final String GLOBAL_ENABLE_PROVIDER_OUPUT = "global.enable.provider.output";
    public static final String GLOBAL_WRITE_TO_DATABASE = "global.write.to.database";
    public static final String GLOBAL_FONT_SIZE_FACTOR = "global.font.size.factor";
    private static final String APP_MARKET_URL = "market://search?q=SMSoIP";
    private static final String WEB_MARKET_URL = "https://play.google.com/store/search?q=SMSoIP";
    private static final int ACTIVITY_SELECT_IMAGE = 10;
    private ProcessImageAndSetBackgroundTask processImageAndSetBackgroundTask;


    public static final String EXTRA_ADJUSTMENT = "extra.adjustment";
    private Integer adjustment = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_program_settings));
        setPreferenceScreen(initPreferences());
        if (savedInstanceState != null) {
            adjustment = savedInstanceState.getInt(EXTRA_ADJUSTMENT, 0);
        } else {
            adjustment = (Integer) getIntent().getExtras().get(EXTRA_ADJUSTMENT);
        }
        getWindow().setBackgroundDrawable(BitmapProcessor.getBackgroundImage(getResources().getConfiguration().orientation));
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
        String uriString = Locale.getDefault().equals(Locale.GERMANY) ? "https://sites.google.com/site/smsoip/homepage-of-smsoip-deutsche-version" : "https://sites.google.com/site/smsoip/home";
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
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:tcr87tEPUao"));
                    GlobalPreferences.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Youtube app not installed on device
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=tcr87tEPUao"));
                    GlobalPreferences.this.startActivity(intent);
                }
                return true;
            }
        });
        youtubeIntent.setTitle(R.string.text_youtube_video);
        youtubeIntent.setSummary(R.string.text_youtube_video_description);
        root.addPreference(youtubeIntent);

        PreferenceScreen pluginIntent = getPreferenceManager().createPreferenceScreen(this);
        pluginIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(APP_MARKET_URL));
                    GlobalPreferences.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    //Market not available on device
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WEB_MARKET_URL));
                    GlobalPreferences.this.startActivity(intent);
                }
                return true;
            }
        });
        pluginIntent.setTitle(R.string.text_visit_plugin_page);
        pluginIntent.setSummary(R.string.text_visit_plugin_page_description);
        root.addPreference(pluginIntent);
    }

    private void addLayoutSettings(PreferenceScreen root) {
        PreferenceCategory layoutCategory = new PreferenceCategory(this);
        layoutCategory.setTitle(R.string.text_category_layout);
        root.addPreference(layoutCategory);
        root.addPreference(new FontSizePreference(this));
        CheckBoxPreference enableCompactMode = new CheckBoxPreference(this);
        enableCompactMode.setDefaultValue(false);
        enableCompactMode.setKey(GLOBAL_ENABLE_COMPACT_MODE);
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
        enableNetworkCheck.setKey(GLOBAL_ENABLE_NETWORK_CHECK);
        enableNetworkCheck.setTitle(R.string.text_enable_network_check);
        enableNetworkCheck.setSummary(R.string.text_enable_network_check_description);
        root.addPreference(enableNetworkCheck);
        CheckBoxPreference enableInfoOnStartup = new CheckBoxPreference(this);
        enableInfoOnStartup.setDefaultValue(false);
        enableInfoOnStartup.setKey(GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP);
        enableInfoOnStartup.setTitle(R.string.text_enable_info_update);
        enableInfoOnStartup.setSummary(R.string.text_enable_info_update_description);
        root.addPreference(enableInfoOnStartup);
        boolean writeToDatabaseAvailable = SMSoIPApplication.getApp().isWriteToDatabaseAvailable();
        CheckBoxPreference writeToDataBase = new CheckBoxPreference(this);
        writeToDataBase.setKey(GLOBAL_WRITE_TO_DATABASE);
        writeToDataBase.setDefaultValue(writeToDatabaseAvailable);
        writeToDataBase.setEnabled(writeToDatabaseAvailable);
        writeToDataBase.setTitle(R.string.text_write_to_database);
        writeToDataBase.setSummary(writeToDatabaseAvailable ? R.string.text_write_to_database_description : R.string.text_not_supported_on_device);
        root.addPreference(writeToDataBase);
        final CheckBoxPreference enableProviderOutput = new CheckBoxPreference(this);
        enableProviderOutput.setKey(GLOBAL_ENABLE_PROVIDER_OUPUT);
        enableProviderOutput.setDefaultValue(false);
        enableProviderOutput.setEnabled(writeToDatabaseAvailable && getPreferenceManager().getSharedPreferences().getBoolean(GLOBAL_WRITE_TO_DATABASE, false));
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
        EditTextPreference editTextPref = new EditTextPreference(this);
        editTextPref.setDialogTitle(R.string.text_signature);
        editTextPref.setKey(GLOBAL_SIGNATURE);
        editTextPref.setTitle(R.string.text_signature);
        editTextPref.setSummary(R.string.text_signature_description);
        root.addPreference(editTextPref);
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
            listPref.setKey(GLOBAL_DEFAULT_PROVIDER);
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
        defaultAreaCode.setKey(GLOBAL_AREA_CODE);
        defaultAreaCode.setTitle(R.string.text_area_code);
        defaultAreaCode.setDefaultValue("49");
        defaultAreaCode.setSummary(R.string.text_area_code_description);
        defaultAreaCode.setOnPreferenceChangeListener(getListener());
        root.addPreference(defaultAreaCode);
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
                        ErrorReporter.getInstance().handleSilentException(e);//TODO remove when stable
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
        processImageAndSetBackgroundTask = new ProcessImageAndSetBackgroundTask(this);
        processImageAndSetBackgroundTask.execute(selectedImage, String.valueOf(adjustment));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(EXTRA_ADJUSTMENT, adjustment);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        getWindow().setBackgroundDrawable(BitmapProcessor.getBackgroundImage(newConfig.orientation));
    }
}


