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
    public static final String GLOBAL_ENABLE_PROVIDER_OUPUT = "global.enable.propvider.output";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_program_settings));
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {

        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        EditTextPreference editTextPref = new EditTextPreference(this);
        editTextPref.setDialogTitle(R.string.text_signature);
        editTextPref.setKey(GLOBAL_SIGNATURE);
        editTextPref.setTitle(R.string.text_signature);
        editTextPref.setSummary(R.string.text_signature_description);
        root.addPreference(editTextPref);
        ListPreference listPref = new ListPreference(this);
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
        CheckBoxPreference enableProviderOutput = new CheckBoxPreference(this);
        enableProviderOutput.setKey(GLOBAL_ENABLE_PROVIDER_OUPUT);
        enableProviderOutput.setDefaultValue(true);
        enableProviderOutput.setTitle(R.string.text_enable_provider_output);
        enableProviderOutput.setSummary(R.string.text_enable_provider_output_description);
        root.addPreference(enableProviderOutput);
        PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(this);
        String uriString = Locale.getDefault().equals(Locale.GERMANY) ? "http://problemexterminator.blogspot.de/p/smsoip-de.html" : "http://problemexterminator.blogspot.de/p/smsoip.html";
        intentPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(uriString)));
        intentPref.setTitle(R.string.text_visit_project_page);
        intentPref.setSummary(R.string.text_visit_project_page_description);
        root.addPreference(intentPref);
        return root;
    }

    private Preference.OnPreferenceChangeListener getListener() {
        return new Preference.OnPreferenceChangeListener() {


            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                EditTextPreference editTextPreference = (EditTextPreference) preference;
                String value;
                try {
                    value = (String) newValue;
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


