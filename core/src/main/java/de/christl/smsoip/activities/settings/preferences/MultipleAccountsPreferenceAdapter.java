/**
 * Copyright CMW Mobile.com, 2010. 
 */
package de.christl.smsoip.activities.settings.preferences;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
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
                    addValue();
                }
            });
        } else {
            row = inflater.inflate(R.layout.defaultlistitem, parent, false);
            CheckedTextView checkedTextView = (CheckedTextView) row.findViewById(R.id.check);
//            checkedTextView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    return false;   //show edit dialog
//                }
//            });
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
            checkAccountBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ProgressDialog progressDialog = new ProgressDialog(dialogPreference.getContext());
                    final AccountModel accountModel = getItem(position);
                    progressDialog.show();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Result login = dialogPreference.getSupplier().login(accountModel.getUserName(), accountModel.getPassWord());
                            progressDialog.cancel();
//                            updateUIHandler.post(updateRunnable);
                        }
                    }).start();

                }
            });
        }

        return row;
    }

    private void addValue() {
        AccountModel abc = new AccountModel(0, "BlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBlaBla", "blubb" + System.currentTimeMillis());
        insert(abc, 0);
    }

    public List<AccountModel> getObjects() {
        return objects;
    }

    public int getDefaultAccount() {
        return defaultAccount;
    }
}
