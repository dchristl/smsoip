package de.christl.smsoip.option;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.view.View;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
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
    private SharedPreferences settings;


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
        settings = SMSoIPApplication.getApp().getSharedPreferences(getClass().getCanonicalName() + "_preferences", Context.MODE_PRIVATE);
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

    public SharedPreferences getSettings() {
        return settings;
    }

    public final String getTextByResourceId(int resourceId) {
        return SMSoIPApplication.getApp().getTextByResourceId(this, resourceId);
    }

    public final String getTextByResourceId(int resourceId, int quantity) {
        return SMSoIPApplication.getApp().getTextByResourceId(this, resourceId, quantity);
    }


    public String[] getArrayByResourceId(int resourceId) {
        return SMSoIPApplication.getApp().getArrayByResourceId(this, resourceId);
    }


    public int getTextMessageLength() {
        return 160;
    }

    public void createSpinner(SendActivity sendActivity, Spinner spinner) {
        spinner.setVisibility(View.GONE);
    }

}
