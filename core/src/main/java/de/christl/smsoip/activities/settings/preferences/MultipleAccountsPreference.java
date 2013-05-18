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

package de.christl.smsoip.activities.settings.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.ProviderPreferences;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.activities.settings.preferences.model.AccountModelsList;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

/**
 * Preference for managing multiple accounts
 * mechanism with adding number only gt 0 seems stupid on first view, but is caused by compatibilty to older versions
 */
public class MultipleAccountsPreference extends ListPreference {


    private SharedPreferences preferences;
    private AccountModelsList accountModels = new AccountModelsList();
    private MultipleAccountsPreferenceAdapter listAdapter;
    private ProviderPreferences providerPreferences;
    private OptionProvider provider;
    private SharedPreferences.Editor editor;

    public MultipleAccountsPreference(ProviderPreferences providerPreferences, PreferenceManager preferences, OptionProvider provider) {
        super(providerPreferences, null);
        this.providerPreferences = providerPreferences;
        this.provider = provider;
        this.preferences = preferences.getSharedPreferences();
        init();
    }

    private void init() {
        setPersistent(false);
        setDefaultAccountInSummary();
        setDialogTitle(R.string.chooseAccount);
        setTitle(R.string.account_list);
        //needed by preference, but values will be filled later in cycle, so defined empty ones
        setEntryValues(new CharSequence[0]);
        setEntries(new CharSequence[0]);
    }

    private int getDefaultAccount() {
        return preferences.getInt(ProviderPreferences.PROVIDER_DEFAULT_ACCOUNT, 0);
    }

    private void setDefaultAccountInSummary() {
        int defaultAccount = getDefaultAccount();
        String defaultAccountName = preferences.getString(ProviderPreferences.PROVIDER_USERNAME + (defaultAccount == 0 ? "" : "." + defaultAccount), getContext().getString(R.string.account_no_account));
        setSummary(String.format(getContext().getString(R.string.account_list_description), defaultAccountName));
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
        accountModels.addFakeAsLast(getContext().getString(R.string.account_add_account));

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
            editor.putInt(ProviderPreferences.PROVIDER_DEFAULT_ACCOUNT, listAdapter.getDefaultAccount());
            editor.commit();
            setDefaultAccountInSummary();
            provider.onAccountsChanged();
        }
        ErrorReporterStack.put(LogConst.SHOW_USER_NAME_PASSWORD_DIALOG_CLOSED);
    }


    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MultipleAccountsPreference.this.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
            }
        });

        listAdapter = new MultipleAccountsPreferenceAdapter(this, accountModels, getDefaultAccount());
        builder.setAdapter(listAdapter, this);
        builder.setSingleChoiceItems(listAdapter, listAdapter.getDefaultAccount(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (listAdapter.getItem(which).getUserName().equals(getContext().getString(R.string.account_add_account))) {
                    showUserNamePasswordDialog(null);
                } else {
                    listAdapter.setDefaultAccount(which);
                }
            }
        });

    }

    public ExtendedSMSSupplier getSupplier() {
        return providerPreferences.getSmsSupplier();
    }

    public void showUserNamePasswordDialog(final AccountModel accountModel) {
        ErrorReporterStack.put(LogConst.SHOW_USER_NAME_PASSWORD_DIALOG);
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.userpassinputs);
        dialog.setTitle(R.string.account_add_account);
        View okButton = dialog.findViewById(R.id.okButton);
        final EditText userInput = (EditText) dialog.findViewById(R.id.user);
        final EditText passInput = (EditText) dialog.findViewById(R.id.pass);
        if (accountModel != null) { // wants to edit
            userInput.setText(accountModel.getUserName());
            passInput.setText(accountModel.getPass());
        }
        int passVisibility = getSupplier().getProvider().isPasswordVisible() ? View.VISIBLE : View.GONE;
        passInput.setVisibility(passVisibility);
        //overwrite the username and password labeltext
        TextView passwordLabel = (TextView) dialog.findViewById(R.id.passLabel);
        if (provider.getPasswordLabelText() != null) {
            passwordLabel.setText(provider.getPasswordLabelText());
        }
        passwordLabel.setVisibility(passVisibility);
        TextView userLabel = (TextView) dialog.findViewById(R.id.userLabel);
        if (provider.getUserLabelText() != null) {
            userLabel.setText(provider.getUserLabelText());
        }
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = userInput.getText().toString();
                String pass = passInput.getText().toString();
                if (!userName.equals("")) {  //user must be supplied, password can be null
                    //add only if inputs done
                    if (accountModel == null) {
                        AccountModel newModel = new AccountModel(userName, pass);
                        if (userNameAlreadyMaintained(newModel)) {
                            showToast(R.string.account_already_defined);
                            return;
                        } else {
                            listAdapter.insert(newModel, listAdapter.getObjects().size() - 1); //add before last (the fake add account one)
                        }
                    } else {
                        accountModel.setUserName(userName);
                        accountModel.setPassWord(pass);
                        if (userNameAlreadyMaintained(accountModel)) {
                            showToast(R.string.account_already_defined);
                            return;
                        }
                    }
                    listAdapter.notifyDataSetChanged();
                    dialog.dismiss();
                } else {
                    showToast(R.string.no_user_name);
                }
            }
        });
        dialog.show();
    }

    private void showToast(int toastTextId) {
        Toast.makeText(getContext(), toastTextId, Toast.LENGTH_SHORT).show();
    }

    private boolean userNameAlreadyMaintained(AccountModel model) {
        for (AccountModel accountModel : listAdapter.getObjects()) {
            String tmpUsername = accountModel.getUserName();
            String userName = model.getUserName();

            if (tmpUsername.equals(getContext().getString(R.string.account_add_account)) || accountModel.equals(model)) {
//                ignore the default account and the current selected
                continue;
            }
            if (tmpUsername.equalsIgnoreCase(userName)) {
                return true;
            }
        }
        return false;
    }
}
