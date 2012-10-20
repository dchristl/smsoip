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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.activities.settings.preferences.model.AccountModelsList;
import de.christl.smsoip.activities.threading.BackgroundCheckLoginTask;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.option.OptionProvider;


/**
 * Adapter for managing the view of multiple accounts of one provider
 */
public class MultipleAccountsPreferenceAdapter extends ArrayAdapter<AccountModel> {


    private AccountModelsList objects;
    private int defaultAccount;
    //    private int defaultAccount;
    private MultipleAccountsPreference dialogPreference;

    /**
     * ImageArrayAdapter constructor.
     *
     * @param dialogPreference the context.
     * @param objects          to be displayed.
     * @param defaultAccount
     */
    public MultipleAccountsPreferenceAdapter(MultipleAccountsPreference dialogPreference, AccountModelsList objects, int defaultAccount) {
        super(dialogPreference.getContext(), R.layout.defaultlistitem, objects);
        this.dialogPreference = dialogPreference;
        this.objects = objects;
        this.defaultAccount = defaultAccount;
    }

    /**
     * {@inheritDoc}
     */
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
        final AccountModel item = getItem(position);
        View row;

        if (item.getUserName().equals(getContext().getString(R.string.account_add_account))) {
            row = inflater.inflate(R.layout.lastlistem, parent, false);
        } else {
            row = inflater.inflate(R.layout.defaultlistitem, parent, false);
            CheckedTextView checkedTextView = (CheckedTextView) row.findViewById(R.id.check);
            checkedTextView.setChecked(position == defaultAccount);
            checkedTextView.setText(item.getUserName());
            View removeAccountBtn = row.findViewById(R.id.removeAccount);
            removeAccountBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ErrorReporterStack.put(LogConst.REMOVE_ACCOUNT_ON_CLICK);
                    remove(getItem(position));
                    if (position == defaultAccount) {
                        defaultAccount = 0; //set back to the first one
                    }

                }
            });
            View checkAccountBtn = row.findViewById(R.id.checkAccount);
            checkAccountBtn.setOnClickListener(buildCheckCredentialsListener(position));
            View editAccount = row.findViewById(R.id.editAccount);
            editAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ErrorReporterStack.put(LogConst.EDIT_ACCOUNT_ONCLICK);
                    dialogPreference.showUserNamePasswordDialog(item);
                }
            });
            //handle visibility by options
            OptionProvider provider = dialogPreference.getSupplier().getProvider();
            int chkBtnVisibility = provider.isCheckLoginButtonVisible() ? View.VISIBLE : View.GONE;
            checkAccountBtn.setVisibility(chkBtnVisibility);
        }
        return row;
    }

    @Override
    public void insert(AccountModel object, int index) {
        if (objects.size() == 1) {   //set the new account as default if none exist (the first is always the fake one)
            defaultAccount = index;
        }
        super.insert(object, index);
    }

    private View.OnClickListener buildCheckCredentialsListener(final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ErrorReporterStack.put(LogConst.CHECK_CREDENTIALS_ON_CLICK);
                final AccountModel accountModel = getItem(position);
                new BackgroundCheckLoginTask(dialogPreference).execute(accountModel);
            }
        };
    }


    public AccountModelsList getObjects() {
        return objects;
    }

    public void setDefaultAccount(int defaultAccount) {
        this.defaultAccount = defaultAccount;
        notifyDataSetChanged();
    }

    public int getDefaultAccount() {
        return defaultAccount;
    }
}
