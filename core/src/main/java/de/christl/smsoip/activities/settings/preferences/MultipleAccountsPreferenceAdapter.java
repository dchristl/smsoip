package de.christl.smsoip.activities.settings.preferences;

import android.app.Activity;
import android.app.ProgressDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.activities.settings.preferences.model.AccountModelsList;
import de.christl.smsoip.constant.Result;
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

        if (item.getUserName().equals(getContext().getString(R.string.text_account_add_account))) {
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
                    remove(getItem(position));

                }
            });
            View checkAccountBtn = row.findViewById(R.id.checkAccount);
            checkAccountBtn.setOnClickListener(buildCheckCredentialsListener(position));
            View editAccount = row.findViewById(R.id.editAccount);
            editAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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

    private View.OnClickListener buildCheckCredentialsListener(final int position) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final ProgressDialog progressDialog = new ProgressDialog(dialogPreference.getContext());
                progressDialog.setMessage(getContext().getString(R.string.text_checkCredentials));
                final AccountModel accountModel = getItem(position);
                progressDialog.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        final Result login = dialogPreference.getSupplier().login(accountModel.getUserName(), accountModel.getPass());

                        Runnable runnable = new Runnable() {
                            public void run() {
                                progressDialog.setMessage(login.getUserText());
                            }
                        };
                        dialogPreference.getHandler().post(runnable);
                        try {
                            Thread.sleep(2000);
                            progressDialog.cancel();
                        } catch (InterruptedException e) {
                            Log.e(this.getClass().getCanonicalName(), "", e);
                        }
                    }
                }).start();

            }
        };
    }


    public AccountModelsList getObjects() {
        return objects;
    }

}
