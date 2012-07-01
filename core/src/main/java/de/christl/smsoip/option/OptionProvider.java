package de.christl.smsoip.option;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.view.View;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A provider for all options corresponding to a supplier
 */
public abstract class OptionProvider {

    private String currProvider;


    private String userName;
    private String password;
    private SharedPreferences settings;
    private Map<Integer, AccountModel> accounts;
    private Integer currentAccountId = null;

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
        accounts = new HashMap<Integer, AccountModel>();
        int defaultAccount = settings.getInt(ProviderPreferences.PROVIDER_DEFAULT_ACCOUNT, 0);
        String user = settings.getString(ProviderPreferences.PROVIDER_USERNAME, null);
        if (user != null) {
            String pass = settings.getString(ProviderPreferences.PROVIDER_PASS, null);
            accounts.put(0, new AccountModel(user, pass));
            if (defaultAccount == 0) {
                userName = user;
                password = pass;
                currentAccountId = 0;
            }
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                String userKey = ProviderPreferences.PROVIDER_USERNAME + "." + i;
                user = settings.getString(userKey, null);
                if (user != null) {
                    String passKey = ProviderPreferences.PROVIDER_PASS + "." + i;
                    pass = settings.getString(passKey, null);
                    accounts.put(i, new AccountModel(user, pass));
                    if (defaultAccount == i) {
                        userName = user;
                        password = pass;
                        currentAccountId = i;
                    }
                } else {
                    break;
                }
            }
        }
    }

    public Map<Integer, AccountModel> getAccounts() {
        return accounts;
    }

    public Integer getCurrentAccountIndex() {
        return currentAccountId;
    }

    public void setCurrentAccountId(Integer currentAccountId) {
        this.currentAccountId = currentAccountId;
        AccountModel accountModel = accounts.get(currentAccountId);
        if (accountModel != null) {
            userName = accountModel.getUserName();
            password = accountModel.getPass();
        } else {  // this can happen if default account is removed in preferences and application was killed
            initOptions();
        }

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

    public boolean hasAccounts() {
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


    public final String[] getArrayByResourceId(int resourceId) {
        return SMSoIPApplication.getApp().getArrayByResourceId(this, resourceId);
    }


    public int getTextMessageLength() {
        return 160;
    }

    public void createSpinner(SendActivity sendActivity, Spinner spinner) {
        spinner.setVisibility(View.GONE);
    }

    /**
     * get the maximum count of messages can be send by this receiver
     *
     * @return
     */
    public int getMaxMessageCount() {
        return 20; //do not put more messages than Integer.Max_Value / messageLength, otherwise it will be negative and no input in the editfield will be possible
    }

}
