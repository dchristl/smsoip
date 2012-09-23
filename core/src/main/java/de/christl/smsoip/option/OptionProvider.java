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

package de.christl.smsoip.option;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.view.View;
import android.widget.LinearLayout;
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
    private boolean accountChanged = false;

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
            userName = null;
            password = null;
        }

        accountChanged = true;
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
        return 5;
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

    protected final String getTextByResourceId(int resourceId, int quantity) {
        return SMSoIPApplication.getApp().getTextByResourceId(this, resourceId, quantity);
    }

    protected final Drawable getDrawble(int drawableId) {
        return SMSoIPApplication.getApp().getDrawable(this, drawableId);
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

    public abstract Drawable getIconDrawable();

    /**
     * get the count of the sms if they have not all the same size,
     * e.g. first sms is 160 signs and the second has only 150 signs
     * is only used for cherry sms, cause they have control characters will increase the size of the 2nd, 3rd,... message
     *
     * @param textLength
     * @return
     */
    public int getLengthDependentSMSCount(int textLength) {
        return 0;
    }

    /**
     * returns if the user has clicked on switch account button
     * <b>resetting this have to be done in plugin itself</b>
     *
     * @return
     */
    public boolean isAccountChanged() {
        return accountChanged;
    }

    public void setAccountChanged(boolean accountChanged) {
        this.accountChanged = accountChanged;
    }

    /**
     * call by some suppliers needed a special
     *
     * @param freeLayout
     */
    public void getFreeLayout(LinearLayout freeLayout) {
        //do nothing by default
    }

    /**
     * if on create method called after the activity was killed (or in background) this method will be called to restore the state
     * of the linear layout
     *
     * @param savedInstanceState
     */
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        //do nothing by default
    }

    /**
     * when activity gets paused this method will be called to save things in the linear layout
     *
     * @param outState
     */
    public void onActivityPaused(Bundle outState) {
        //do nothing by default
    }
}
