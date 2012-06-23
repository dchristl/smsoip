package de.christl.smsoip.activities.settings.preferences;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.View;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.ArrayList;
import java.util.List;

/**
 * Preference for managing multiple accounts
 */
public class MultipleAccountsPreference extends ListPreference {

    public static final String PROVIDER_USERNAME = "provider.username";
    public static final String PROVIDER_PASS = "provider.password";
    public static final String PROVIDER_DEFAULT_ACCOUNT = "provider.default.number";
    private SharedPreferences preferences;
    List<AccountModel> accountModels = new ArrayList<AccountModel>();
    private MultipleAccountsPreferenceAdapter listAdapter;
    private ProviderPreferences providerPreferences;

    public MultipleAccountsPreference(ProviderPreferences providerPreferences, PreferenceManager preferences) {
        super(providerPreferences, null);
        this.providerPreferences = providerPreferences;
        this.preferences = preferences.getSharedPreferences();
        init();
    }

    private void init() {
        setPersistent(false);
        fillAccountMap();
        setDialogTitle(R.string.text_account_list);
        setTitle(R.string.text_account_list);
        setSummary(R.string.text_account_list_description);
        CharSequence[] keys = new CharSequence[accountModels.size()];
        CharSequence[] userNames = new CharSequence[accountModels.size()];
        for (int i = 0, accountModelsSize = accountModels.size(); i < accountModelsSize; i++) {
            AccountModel accountModel = accountModels.get(i);
            keys[i] = String.valueOf(accountModel.getIndex());
            userNames[i] = accountModel.getUserName();
        }
        setEntryValues(keys);
        setEntries(userNames);
    }

    private void fillAccountMap() {
        String userName = preferences.getString(PROVIDER_USERNAME, null);
        if (userName != null) {
            String passWord = preferences.getString(PROVIDER_PASS, null);
            accountModels.add(new AccountModel(0, userName, passWord));
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                userName = preferences.getString(PROVIDER_USERNAME + "." + i, null);
                passWord = preferences.getString(PROVIDER_PASS + "." + i, null);
                if (userName != null) {
                    accountModels.add(new AccountModel(i, userName, passWord));
                } else {
                    break;
                }
            }
        }
        accountModels.add(new AccountModel(-1, getContext().getString(R.string.text_account_add_account), "fake"));

    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);//update values
    }

    //
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            List<AccountModel> objects = listAdapter.getObjects();
//            persistBoolean() valuse
        }
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        int defaultAccount = preferences.getInt(PROVIDER_DEFAULT_ACCOUNT, 0);
        listAdapter = new MultipleAccountsPreferenceAdapter(this, accountModels, defaultAccount);
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    public SMSSupplier getSupplier() {
        return providerPreferences.getSmsSupplier();
    }

}
