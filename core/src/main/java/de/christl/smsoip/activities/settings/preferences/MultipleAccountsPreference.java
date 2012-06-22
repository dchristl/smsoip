package de.christl.smsoip.activities.settings.preferences;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ListAdapter;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.MultipleAccountsModel;

/**
 * Preference for managing multiple accounts
 */
public class MultipleAccountsPreference extends ListPreference {

    public static final String PROVIDER_USERNAME = "provider.username";
    public static final String PROVIDER_PASS = "provider.password";
    public static final String PROVIDER_DEFAULT_ACCOUNT = "provider.default.number";
    private SharedPreferences preferences;
    MultipleAccountsModel accountsModel = new MultipleAccountsModel();

    public MultipleAccountsPreference(ProviderPreferences providerPreferences, PreferenceManager preferences) {
        super(providerPreferences, null);
        this.preferences = preferences.getSharedPreferences();
        init();
    }

    private void init() {
        setPersistent(false);
        fillAccountMap();
        setDialogTitle(R.string.text_account_list);
        setTitle(R.string.text_account_list);
        setSummary(R.string.text_account_list_description);
        setEntryValues(accountsModel.getKeys());
        setEntries(accountsModel.getValues());
    }

    private void fillAccountMap() {
        String userName = preferences.getString(PROVIDER_USERNAME, null);
        if (userName != null) {
            String passWord = preferences.getString(PROVIDER_PASS, null);
            accountsModel.put(0, userName, passWord);
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                userName = preferences.getString(PROVIDER_USERNAME + "." + i, null);
                passWord = preferences.getString(PROVIDER_PASS + "." + i, null);
                if (userName != null) {
                    accountsModel.put(i, userName, passWord);
                } else {
                    break;
                }
            }
        }
        accountsModel.put(-1, getContext().getString(R.string.text_account_add_account), "fake");

    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);//update values
    }

    //
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
//            persistBoolean() valuse
        }
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        int defaultAccount = preferences.getInt(PROVIDER_DEFAULT_ACCOUNT, 0);
        ListAdapter listAdapter = new MultipleAccountsPreferenceAdapter(this, accountsModel.getOriginalValues(), defaultAccount);
        builder.setAdapter(listAdapter, this);
        super.onPrepareDialogBuilder(builder);
    }

}
