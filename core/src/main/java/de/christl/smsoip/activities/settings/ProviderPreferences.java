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

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.activities.settings.preferences.MultipleAccountsPreference;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.application.SMSoIPPlugin;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import org.acra.ACRA;
import org.acra.ErrorReporter;

import java.util.List;

/**
 * Preferences for one provider
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
        ErrorReporterStack.put("oncreate providerpreference");
        String supplierClassName;
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            supplierClassName = extras.getString(SUPPLIER_CLASS_NAME);
        } else {
            supplierClassName = savedInstanceState.getString(SUPPLIER_CLASS_NAME);
        }
        if (supplierClassName == null) {

            ErrorReporter errorReporter = ACRA.getErrorReporter();
            errorReporter.putCustomData("savedInstanceState", savedInstanceState == null ? null : savedInstanceState.toString());
            errorReporter.putCustomData("getIntent().getExtras()", getIntent().getExtras() == null ? null : getIntent().getExtras().toString());
            errorReporter.handleSilentException(new IllegalArgumentException("supplierClassName is null"));
            this.finish();
        }
        smsSupplier = SMSoIPApplication.getApp().getSMSoIPPluginBySupplierName(supplierClassName);
        provider = smsSupplier.getProvider();
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.provider_settings) + " (" + provider.getProviderName() + ")");
        preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(provider.getClass().getCanonicalName() + "_preferences");
        preferenceManager.setSharedPreferencesMode(MODE_PRIVATE);
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = preferenceManager.createPreferenceScreen(this);
        if (provider.hasAccounts()) {
            root.addPreference(new MultipleAccountsPreference(this, preferenceManager, provider));
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

    public ExtendedSMSSupplier getSmsSupplier() {
        return smsSupplier.getSupplier();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SUPPLIER_CLASS_NAME, smsSupplier.getSupplierClassName());
    }
}
