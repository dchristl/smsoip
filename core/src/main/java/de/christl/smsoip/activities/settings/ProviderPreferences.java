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
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.settings.preferences.MultipleAccountsPreference;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.application.changelog.ChangeLog;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

import java.io.InputStream;
import java.util.List;

/**
 * Prefernces for one provider
 */
public class ProviderPreferences extends BackgroundPreferenceActivity {
    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    private SMSoIPPlugin smsSupplier;
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
        smsSupplier = SMSoIPApplication.getApp().getSMSoIPPluginBySupplierName(supplierClassName);
        provider = smsSupplier.getProvider();
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_provider_settings) + " (" + provider.getProviderName() + ")");
        preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(provider.getClass().getCanonicalName() + "_preferences");
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        setPreferenceScreen(initPreferences());
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
        final InputStream changelogInputStream = provider.getChangelogInputStream();
        if (changelogInputStream != null) {
            PreferenceScreen changelogIntent = getPreferenceManager().createPreferenceScreen(this);
            changelogIntent.setTitle(R.string.text_provider_changelog);
            changelogIntent.setSummary(R.string.text_provider_changelog_description);
            changelogIntent.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    ChangeLog cl = new ChangeLog(ProviderPreferences.this);

                    AlertDialog changelogDialogByView = cl.getChangelogDialogByView(changelogInputStream);
                    changelogDialogByView.show();
                    return true;
                }
            });
            root.addPreference(changelogIntent);
        }
        return root;
    }

    public ExtendedSMSSupplier getSmsSupplier() {
        return smsSupplier.getSupplier();
    }

}
