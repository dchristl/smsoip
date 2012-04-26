package de.christl.smsoip.activities.settings;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.AdPreference;
import de.christl.smsoip.application.ProviderEntry;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class GlobalPreferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_program_settings));
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {
        // Root
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);

        EditTextPreference editTextPref = new EditTextPreference(this);
        editTextPref.setDialogTitle(R.string.text_signature);
        editTextPref.setKey("global_signature");
        editTextPref.setTitle(R.string.text_signature);
        editTextPref.setSummary(R.string.text_signature_description);
        root.addPreference(editTextPref);

        ListPreference listPref = new ListPreference(this);
        List<ProviderEntry> providerEntries = SMSoIPApplication.getApp().getProviderEntries();
        if (providerEntries.size() > 1) {
            Map<String, String> providersWithNames = new LinkedHashMap<String, String>();
            providersWithNames.put((String) getText(R.string.text_no_default_Provider), "");
            for (ProviderEntry providerEntry : providerEntries) {
                providersWithNames.put(providerEntry.getProviderName(), providerEntry.getSupplierClassName());
            }
            listPref.setEntries(providersWithNames.keySet().toArray(new CharSequence[providersWithNames.size()]));
            listPref.setEntryValues(providersWithNames.values().toArray(new CharSequence[providersWithNames.size()]));
            listPref.setDialogTitle(R.string.text_default_provider);
            listPref.setKey("global_defaultProvider");
            listPref.setTitle(R.string.text_default_provider);
            listPref.setSummary(R.string.text_default_provider_description);
            root.addPreference(listPref);
        }
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);

        PreferenceScreen intentPref = getPreferenceManager().createPreferenceScreen(this);   //TODO correct this when website ready
        String uriString = Locale.getDefault().equals(Locale.GERMANY) ? "http://problemexterminator.blogspot.de" :
                "http://problemexterminator.blogspot.de/p/smsoip.html";
        intentPref.setIntent(new Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(uriString)));
        intentPref.setTitle(R.string.text_visit_project_page);
        intentPref.setSummary(R.string.text_visit_project_page_description);
        root.addPreference(intentPref);

        return root;


    }
}