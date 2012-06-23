/**
 * Copyright CMW Mobile.com, 2010. 
 */
package de.christl.smsoip.activities.settings.preferences;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.constant.Result;

import java.util.List;

/**
 * Adapter for managing the view of multiple accounts of one provider
 */
public class MultipleAccountsPreferenceAdapter extends ArrayAdapter<AccountModel> {


    private List<AccountModel> objects;
    private int defaultAccount;
    private MultipleAccountsPreference dialogPreference;

    /**
     * ImageArrayAdapter constructor.
     *
     * @param dialogPreference the context.
     * @param objects          to be displayed.
     */
    public MultipleAccountsPreferenceAdapter(MultipleAccountsPreference dialogPreference, List<AccountModel> objects, int defaultAccount) {
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

        if (item.getUserName().equals(getContext().getString(R.string.text_account_add_account))) {
            row = inflater.inflate(R.layout.lastlistem, parent, false);
            View addAccount = row.findViewById(R.id.addAccount);
            addAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUserNamePasswordDialog(null);
                }
            });
        } else {
            row = inflater.inflate(R.layout.defaultlistitem, parent, false);
            CheckedTextView checkedTextView = (CheckedTextView) row.findViewById(R.id.check);
            checkedTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    defaultAccount = position;
                    Dialog dialog = dialogPreference.getDialog();
                    dialogPreference.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    dialog.dismiss();
                }
            });
            checkedTextView.setText(item.getUserName());
            View removeAccountBtn = row.findViewById(R.id.removeAccount);
            removeAccountBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(getItem(position));

                }
            });
            checkedTextView.setChecked(position == defaultAccount);
            View checkAccountBtn = row.findViewById(R.id.checkAccount);
            checkAccountBtn.setOnClickListener(buildCheckCredentialsListener(position));
            View editAccount = row.findViewById(R.id.editAccount);
            editAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showUserNamePasswordDialog(item);
                }
            });
        }

        return row;
    }

    private View.OnClickListener buildCheckCredentialsListener(final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog progressDialog = new ProgressDialog(dialogPreference.getContext());
                progressDialog.setMessage(getContext().getString(R.string.text_pleaseWait));
                final AccountModel accountModel = getItem(position);
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Result login = dialogPreference.getSupplier().login(accountModel.getUserName(), accountModel.getPassWord());

                        Runnable runnable = new Runnable() {
                            public void run() {
                                progressDialog.setMessage(login.getUserText());
                            }
                        };
                        dialogPreference.getHandler().post(runnable);
                        try {
                            Thread.sleep(3000);
                            progressDialog.cancel();
                        } catch (InterruptedException e) {
                            Log.e(this.getClass().getCanonicalName(), "", e);
                        }
                    }
                }).start();

            }
        };
    }

    private void showUserNamePasswordDialog(final AccountModel accountModel) {
        final Dialog dialog = new Dialog(getContext());
        dialog.setContentView(R.layout.userpassinputs);
        dialog.setTitle(R.string.text_account_add_account);
        View okButton = dialog.findViewById(R.id.okButton);
        final EditText userInput = (EditText) dialog.findViewById(R.id.user);
        final EditText passInput = (EditText) dialog.findViewById(R.id.pass);
        if (accountModel != null) { // wants to edit
            userInput.setText(accountModel.getUserName());
            passInput.setText(accountModel.getPassWord());
        }
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userName = userInput.getText().toString();
                String pass = passInput.getText().toString();
                if (!userName.equals("") || !pass.equals("")) {
                    //add only if inputs done
                    if (accountModel == null) {
                        AccountModel newModel = new AccountModel(userName, pass);
                        insert(newModel, objects.size() - 1); //add before last (the fake add account one)
                    } else {
                        accountModel.setUserName(userName);
                        accountModel.setPassWord(userName);
                        notifyDataSetChanged();
                    }
                }
                dialog.dismiss();
            }
        });
        dialog.show();

    }


    public List<AccountModel> getObjects() {
        return objects;
    }

    public int getDefaultAccount() {
        return defaultAccount;
    }
}
