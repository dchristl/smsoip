package de.christl.smsoip.option;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.List;

/**
 * A provider for all options corresponding to a supplier
 */
public abstract class OptionProvider {

    String currProvider;


    String userName;
    String password;


    /**
     * default constructor, use the other one for give a name to the provider otherwise classname will be used
     */
    public OptionProvider() {
        this(null);
    }

    /**
     * constructor with name of the provider
     *
     * @param providerName
     */
    protected OptionProvider(String providerName) {
        if (providerName == null) {
            currProvider = getClass().getCanonicalName();
        } else {
            currProvider = providerName;
        }
        initOptions();
    }


    private void initOptions() {
        SharedPreferences settings = SMSoIPApplication.getApp().getSharedPreferences(getClass().getCanonicalName() + "_preferences", Context.MODE_PRIVATE);
        userName = settings.getString(ProviderPreferences.PROVIDER_USERNAME, "");
        password = settings.getString(ProviderPreferences.PROVIDER_PASS, "");
    }


    public String getUserName() {
        return userName;
    }


    public String getPassword() {
        return password;
    }

    public String getProviderName() {
        return currProvider;
    }


    public List<Preference> getAdditionalPreferences(Context context) {
        return null;
    }

    public boolean isUsernameVisible() {
        return true;
    }

    public boolean isPasswordVisible() {
        return true;
    }

    public boolean isCheckLoginButtonVisible() {
        return true;
    }

    public int getMaxReceiverCount() {
        return Integer.MAX_VALUE;
    }

    public void refresh() {
        initOptions();
    }
}
