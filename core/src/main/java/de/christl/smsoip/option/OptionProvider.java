package de.christl.smsoip.option;

import android.content.Context;
import android.content.SharedPreferences;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.provider.SMSSupplier;

/**
 * A provider for all options corresponding to a supplier
 */
public abstract class OptionProvider {

    String currProvider;

    public abstract Class<? extends SMSSupplier> getSupplier();





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


    public void initOptions() {
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


}
