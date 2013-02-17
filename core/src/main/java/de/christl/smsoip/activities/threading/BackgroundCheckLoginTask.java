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

package de.christl.smsoip.activities.threading;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.preferences.MultipleAccountsPreference;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

/**
 * checks the login in background in GlobalPreferences
 */
public class BackgroundCheckLoginTask extends AsyncTask<AccountModel, String, Void> {

    private MultipleAccountsPreference multiPreference;
    private ProgressDialog progressDialog;

    public BackgroundCheckLoginTask(MultipleAccountsPreference multiPreference) {
        this.multiPreference = multiPreference;
        progressDialog = new ProgressDialog(multiPreference.getContext());
    }

    @Override
    protected void onPreExecute() {
        ErrorReporterStack.put(LogConst.BACKGROUND_CHECK_LOGIN_TASK_STARTED);
        progressDialog.setMessage(multiPreference.getContext().getString(R.string.checkCredentials));
        progressDialog.show();
    }

    @Override
    protected Void doInBackground(AccountModel... accountModels) {
        ErrorReporterStack.put(LogConst.BACKGROUND_CHECK_LOGIN_TASK_RUNNING);
        ExtendedSMSSupplier supplier = multiPreference.getSupplier();
        AccountModel accountModel = accountModels[0];
        String userName = accountModel.getUserName();
        String pass = accountModel.getPass();
        SMSActionResult smsActionResult;
        if (userName == null || userName.trim().length() == 0 || pass == null || pass.trim().length() == 0) {
            smsActionResult = SMSActionResult.NO_CREDENTIALS();
        } else {
            try {
                smsActionResult = supplier.checkCredentials(userName, pass);
            } catch (UnsupportedEncodingException e) {
                smsActionResult = SMSActionResult.UNKNOWN_ERROR();
            } catch (SocketTimeoutException e) {
                smsActionResult = SMSActionResult.TIMEOUT_ERROR();
            } catch (IOException e) {
                smsActionResult = SMSActionResult.NETWORK_ERROR();
            }
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            publishProgress(smsActionResult.getMessage());
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        progressDialog.setMessage(values[0]);
    }

    @Override
    protected void onPostExecute(Void nothing) {
        ErrorReporterStack.put(LogConst.BACKGROUND_CHECK_LOGIN_TASK_ON_FINISH);
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.cancel();
        }
    }
}