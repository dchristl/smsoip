package de.christl.smsoip.activities.settings.preferences;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.View;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.activities.settings.preferences.model.AccountModelsList;
import de.christl.smsoip.provider.SMSSupplier;

import java.util.List;

/**
 * Preference for managing multiple accounts
 */
public class MultipleAccountsPreference extends ListPreference {

    public static final String PROVIDER_USERNAME = "provider.username";
    public static final String PROVIDER_PASS = "provider.password";
    public static final String PROVIDER_DEFAULT_ACCOUNT = "provider.default.number";
    private SharedPreferences preferences;
    AccountModelsList accountModels = new AccountModelsList();
    private MultipleAccountsPreferenceAdapter listAdapter;
    private ProviderPreferences providerPreferences;
    private int defaultAccount;
    private Handler updateUIHandler = new Handler();

    public MultipleAccountsPreference(ProviderPreferences providerPreferences, PreferenceManager preferences) {
        super(providerPreferences, null);
        this.providerPreferences = providerPreferences;
        this.preferences = preferences.getSharedPreferences();
        init();
    }

    private void init() {
        setPersistent(false);
        defaultAccount = preferences.getInt(PROVIDER_DEFAULT_ACCOUNT, 0);
        String defaultAccountName = preferences.getString(PROVIDER_USERNAME + (defaultAccount == 0 ? "" : "." + defaultAccount), getContext().getString(R.string.text_account_no_account));
        fillAccountMap();
        setDialogTitle(R.string.text_account_list);
        setTitle(R.string.text_account_list);
        setSummary(String.format(getContext().getString(R.string.text_account_list_description), defaultAccountName));
        setEntryValues(accountModels.getKeys());
        setEntries(accountModels.getValues());
    }

    private void fillAccountMap() {
        String userName = preferences.getString(PROVIDER_USERNAME, null);
        if (userName != null) {
            String passWord = preferences.getString(PROVIDER_PASS, null);
            accountModels.put(userName, passWord);
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                userName = preferences.getString(PROVIDER_USERNAME + "." + i, null);
                passWord = preferences.getString(PROVIDER_PASS + "." + i, null);
                if (userName != null) {
                    accountModels.put(userName, passWord);
                } else {
                    break;
                }
            }
        }
        accountModels.addFakeAsLast(getContext().getString(R.string.text_account_add_account));

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
        } else {
//            accountModels.clear();
        }
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        listAdapter = new MultipleAccountsPreferenceAdapter(this, accountModels, defaultAccount);
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

    public SMSSupplier getSupplier() {
        return providerPreferences.getSmsSupplier();
    }

    public Handler getHandler() {
        return updateUIHandler;
    }
}
