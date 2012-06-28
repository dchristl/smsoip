package de.christl.smsoip.activities.settings;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.settings.preferences.MultipleAccountsPreference;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.List;

/**
 * Prefernces for one provider
 */
public class ProviderPreferences extends PreferenceActivity {
    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    private SMSSupplier smsSupplier;
    public static final String PROVIDER_USERNAME = "provider.username";
    public static final String PROVIDER_PASS = "provider.password";
    public static final String PROVIDER_DEFAULT_ACCOUNT = "provider.default.number";
    private PreferenceManager preferenceManager;
    private OptionProvider provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        String supplierClassName = (String) extras.get(SUPPLIER_CLASS_NAME);
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName);
        provider = smsSupplier.getProvider();
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_provider_settings) + " (" + provider.getProviderName() + ")");
        preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(provider.getClass().getCanonicalName() + "_preferences");
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        setPreferenceScreen(initPreferences());
        getWindow().setBackgroundDrawableResource(R.drawable.background_holo_dark);
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = preferenceManager.createPreferenceScreen(this);
        if (provider.hasAccounts()) {
            root.addPreference(new MultipleAccountsPreference(this, preferenceManager));
        }
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);

        List<Preference> additionalPreferences = provider.getAdditionalPreferences(this);
        if (additionalPreferences != null) {
            for (Preference additionalPreference : additionalPreferences) {
                root.addPreference(additionalPreference);
            }
        }
        return root;
    }

    public SMSSupplier getSmsSupplier() {
        return smsSupplier;
    }
}
