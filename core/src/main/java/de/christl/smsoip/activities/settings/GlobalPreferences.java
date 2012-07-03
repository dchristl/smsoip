package de.christl.smsoip.activities.settings;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.*;
import android.widget.Toast;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class GlobalPreferences extends PreferenceActivity {

    public static final String GLOBAL_SIGNATURE = "global.signature";
    public static final String GLOBAL_DEFAULT_PROVIDER = "global.default.provider";
    public static final String GLOBAL_AREA_CODE = "global.area.code";
    public static final String GLOBAL_ENABLE_NETWORK_CHECK = "global.enable.network.check";
    public static final String GLOBAL_ENABLE_INFO_UPDATE_ON_STARTUP = "global.enable.info.update";
    public static final String GLOBAL_ENABLE_PROVIDER_OUPUT = "global.enable.provider.output";
    public static final String GLOBAL_WRITE_TO_DATABASE = "global.write.to.database";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_program_settings));
        setPreferenceScreen(initPreferences());
        getWindow().setBackgroundDrawableResource(R.drawable.background_holo_dark);
    }

    private PreferenceScreen initPreferences() {

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        EditTextPreference editTextPref = new EditTextPreference(this);
        editTextPref.setDialogTitle(R.string.text_signature);
        editTextPref.setKey(GLOBAL_SIGNATURE);
        editTextPref.setTitle(R.string.text_signature);
        editTextPref.setSummary(R.string.text_signature_description);
        root.addPreference(editTextPref);
        final ListPreference listPref = new ListPreference(this);
        Map<String, ProviderEntry> providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        if (providerEntries.size() > 1) {
            Map<String, String> providersWithNames = new LinkedHashMap<String, String>();
            providersWithNames.put((String) getText(R.string.text_no_default_Provider), "");
            for (ProviderEntry providerEntry : providerEntries.values()) {
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

        CheckBoxPreference enableNetworkCheck = new CheckBoxPreference(this);
        enableNetworkCheck.setDefaultValue(true);
        enableNetworkCheck.setKey(GLOBAL_ENABLE_NETWORK_CHECK);
        enableNetworkCheck.setTitle(R.string.text_enable_network_check);
        enableNetworkCheck.setSummary(R.string.text_enable_network_check_description);
        root.addPreference(enableNetworkCheck);
        CheckBoxPreference enableInfoOnStartup = new CheckBoxPreference(this);
        enableInfoOnStartup.setDefaultValue(true);
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
        enableProviderOutput.setDefaultValue(true);
        enableProviderOutput.setEnabled(writeToDatabaseAvailable);
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
        PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(this);
        String uriString = Locale.getDefault().equals(Locale.GERMANY) ? "http://problemexterminator.blogspot.de/p/smsoip-de.html" : "http://problemexterminator.blogspot.de/p/smsoip.html";
        intentPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(uriString)));
        intentPref.setTitle(R.string.text_visit_project_page);
        intentPref.setSummary(R.string.text_visit_project_page_description);
        root.addPreference(intentPref);

        PreferenceScreen pluginIntent = getPreferenceManager().createPreferenceScreen(this);
        uriString = "market://search?q=pub:Danny Christl";
        pluginIntent.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(uriString)));
        pluginIntent.setTitle(R.string.text_visit_plugin_page);
        pluginIntent.setSummary(R.string.text_visit_plugin_page_description);
        root.addPreference(pluginIntent);
        return root;
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

}


