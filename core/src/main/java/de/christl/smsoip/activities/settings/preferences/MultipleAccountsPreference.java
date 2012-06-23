package de.christl.smsoip.activities.settings.preferences;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.activities.settings.preferences.model.AccountModelsList;
import de.christl.smsoip.provider.SMSSupplier;

/**
 * Preference for managing multiple accounts
 * mechanism with adding number only gt 0 seems stupid on first view, but is caused by compatibilty to older versions
 */
public class MultipleAccountsPreference extends ListPreference {


    private SharedPreferences preferences;
    AccountModelsList accountModels = new AccountModelsList();
    private MultipleAccountsPreferenceAdapter listAdapter;
    private ProviderPreferences providerPreferences;
    private int defaultAccount;
    private Handler updateUIHandler = new Handler();
    private SharedPreferences.Editor editor;

    public MultipleAccountsPreference(ProviderPreferences providerPreferences, PreferenceManager preferences) {
        super(providerPreferences, null);
        this.providerPreferences = providerPreferences;
        this.preferences = preferences.getSharedPreferences();
        init();
    }

    private void init() {
        setPersistent(false);
        defaultAccount = preferences.getInt(ProviderPreferences.PROVIDER_DEFAULT_ACCOUNT, 0);
        setDefaultAccountInSummary();
        setDialogTitle(R.string.text_chooseAccount);
        setTitle(R.string.text_account_list);
        //needed by preference, but values will be filled later in cycle, so defined empty ones
        setEntryValues(new CharSequence[0]);
        setEntries(new CharSequence[0]);
    }

    private void setDefaultAccountInSummary() {
        String defaultAccountName = preferences.getString(ProviderPreferences.PROVIDER_USERNAME + (defaultAccount == 0 ? "" : "." + defaultAccount), getContext().getString(R.string.text_account_no_account));
        setSummary(String.format(getContext().getString(R.string.text_account_list_description), defaultAccountName));
    }

    private void fillAccountMap() {
        accountModels.clear();
        editor = getEditor();
        String userName = preferences.getString(ProviderPreferences.PROVIDER_USERNAME, null);
        if (userName != null) {
            String passWord = preferences.getString(ProviderPreferences.PROVIDER_PASS, null);
            editor.remove(ProviderPreferences.PROVIDER_USERNAME).remove(ProviderPreferences.PROVIDER_PASS); //will be marked for remove, commited only on positive result
            accountModels.put(userName, passWord);
            for (int i = 1; i < Integer.MAX_VALUE; i++) {
                String userKey = ProviderPreferences.PROVIDER_USERNAME + "." + i;
                userName = preferences.getString(userKey, null);
                if (userName != null) {
                    String passKey = ProviderPreferences.PROVIDER_PASS + "." + i;
                    passWord = preferences.getString(passKey, null);
                    accountModels.put(userName, passWord);
                    editor.remove(userKey).remove(passKey);
                } else {
                    break;
                }
            }
        }
        accountModels.addFakeAsLast(getContext().getString(R.string.text_account_add_account));

    }


    @Override
    protected void showDialog(Bundle state) {
        fillAccountMap();
        super.showDialog(state);
    }

    //
    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            AccountModelsList objects = listAdapter.getObjects();
            objects.removeFake();
            for (int i = 0, objectsSize = objects.size(); i < objectsSize; i++) {
                AccountModel accountModel = objects.get(i);
                editor.putString(ProviderPreferences.PROVIDER_USERNAME + (i == 0 ? "" : "." + i), accountModel.getUserName());
                editor.putString(ProviderPreferences.PROVIDER_PASS + (i == 0 ? "" : "." + i), accountModel.getPass());
            }
            defaultAccount = listAdapter.getDefaultAccount();
            editor.putInt(ProviderPreferences.PROVIDER_DEFAULT_ACCOUNT, defaultAccount);
            editor.commit();
            setDefaultAccountInSummary();
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
