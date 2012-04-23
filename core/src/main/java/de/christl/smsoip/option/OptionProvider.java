package de.christl.smsoip.option;

import android.content.Context;
import android.content.SharedPreferences;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.provider.SMSSupplier;

/**
 * A provider for all options corresponding to a supplier
 */
public abstract class OptionProvider {

    String currProvider;
    private SharedPreferences settings;

    public abstract Class<? extends SMSSupplier> getSupplier();


    private interface OPTIONS {
        String USERNAME = "username";
        String PASSWORD = "password";
        String SIGNATURE = "signature";
    }


    String userName;
    String password;
    String signature;

    private static final String FILE_PREFIX = "options_";

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

    public void save() {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(OPTIONS.USERNAME, userName);
        editor.putString(OPTIONS.PASSWORD, password);
        editor.putString(OPTIONS.SIGNATURE, signature);
        editor.commit();
    }

    public void initOptions() {
        settings = SMSoIPApplication.getApp().getSharedPreferences(FILE_PREFIX + currProvider, Context.MODE_PRIVATE);
        userName = settings.getString(OPTIONS.USERNAME, "");
        password = settings.getString(OPTIONS.PASSWORD, "");
        signature = settings.getString(OPTIONS.SIGNATURE, "Sent by SMSoIP");
    }


    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getSignature() {
        return signature == null ? "" : signature;
    }

    public String getProviderName() {
        return currProvider;
    }


}
