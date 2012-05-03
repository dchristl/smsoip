package de.christl.smsoip.activities.settings;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.*;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
import android.widget.EditText;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.AdPreference;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.provider.SMSSupplier;

/**
 * Prefernces for one provider
 */
public class ProviderPreferences extends PreferenceActivity {
    public static final String SUPPLIER_CLASS_NAME = "supplierClassName";
    private SMSSupplier smsSupplier;
    private static final String PROVIDER_USERNAME = "provider.username";
    private static final String PROVIDER_PASS = "provider.password";
    private PreferenceManager preferenceManager;
    private Result result;
    final Handler updateUIHandler = new Handler();
    final Runnable updateRunnable = new Runnable() {
        public void run() {
            updateButtons();
        }
    };
    private EditTextPreference passwordPreference;
    private EditTextPreference userNamePreference;
    private PreferenceScreen checkCredentials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle extras = getIntent().getExtras();
        String supplierClassName = (String) extras.get(SUPPLIER_CLASS_NAME);
        smsSupplier = SMSoIPApplication.getApp().getInstance(supplierClassName);
        setTitle(getText(R.string.applicationName) + " - " + getText(R.string.text_provider_settings) + " (" + smsSupplier.getProvider().getProviderName() + ")");
        preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(smsSupplier.getClass().getCanonicalName() + "_preferences");
        preferenceManager.setSharedPreferencesMode(MODE_WORLD_READABLE);
        setPreferenceScreen(initPreferences());
    }

    private PreferenceScreen initPreferences() {
        PreferenceScreen root = preferenceManager.createPreferenceScreen(this);
        userNamePreference = new EditTextPreference(this);
        userNamePreference.setDialogTitle(R.string.text_username);
        userNamePreference.setKey(PROVIDER_USERNAME);
        userNamePreference.setTitle(R.string.text_username);
        root.addPreference(userNamePreference);
        passwordPreference = new EditTextPreference(this);
        EditText passwordPreferenceEditText = passwordPreference.getEditText();
        passwordPreferenceEditText.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordPreferenceEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordPreference.setDialogTitle(R.string.text_password);
        passwordPreference.setKey(PROVIDER_PASS);
        passwordPreference.setTitle(R.string.text_password);
        root.addPreference(passwordPreference);
        AdPreference adPreference = new AdPreference(this);
        root.addPreference(adPreference);
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setMessage(getString(R.string.text_checkCredentials));
        checkCredentials = getPreferenceManager().createPreferenceScreen(this);
        checkCredentials.setTitle(R.string.text_checkLogin);
        checkCredentials.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        checkLogin();
                        progressDialog.cancel();
                        updateUIHandler.post(updateRunnable);
                    }
                }).start();
                return false;
            }
        });
        root.addPreference(checkCredentials);
        return root;
    }

    private void updateButtons() {
        int color = result.equals(Result.NO_ERROR) ? Color.GREEN : Color.RED;
        Spannable newUserNameTitle = new SpannableString(userNamePreference.getTitle());
        newUserNameTitle.setSpan(new ForegroundColorSpan(color), 0, newUserNameTitle.length(), 0);
        userNamePreference.setTitle(newUserNameTitle);
        Spannable newPasswordTitle = new SpannableString(passwordPreference.getTitle());
        newPasswordTitle.setSpan(new ForegroundColorSpan(color), 0, newPasswordTitle.length(), 0);
        passwordPreference.setTitle(newPasswordTitle);
        checkCredentials.setSummary(result.getUserText());
    }

    private void checkLogin() {
        result = smsSupplier.login(userNamePreference.getText(), passwordPreference.getText());
    }
}
