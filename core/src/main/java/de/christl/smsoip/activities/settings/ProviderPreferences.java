package de.christl.smsoip.activities.settings;

import android.os.Bundle;
import android.preference.*;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.AdPreference;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.SMSSupplier;

/**
 * Created with IntelliJ IDEA.
 * User: Danny
 * Date: 26.04.12
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
public class ProviderPreferences extends PreferenceActivity {
    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    private SMSSupplier smsSupplier;
    private OptionProvider provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_provider_settings) + " (" + provider.getProviderName() + ")");
        Bundle extras = getIntent().getExtras();
        String supplierClassName = (String) extras.get(SUPPLIER_CLASS_NAME);
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName);
        provider = smsSupplier.getProvider();
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        // Inline preferences


        EditTextPreference userName = new EditTextPreference(this);
        userName.setKey("checkbox_preference");
        userName.setTitle("R.string.title_checkbox_preference");
//        userNamePrefernce.setSummary("R.string.summary_checkbox_preference");
        root.addPreference(userName);
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        return root;
    }
}
