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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.models.ErrorReporterStack;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.ui.BreakingProgressDialogFactory;
import org.acra.ACRA;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

/**
 * Update the informations in background
 */
public class BackgroundUpdateTask extends AsyncTask<Boolean, SMSActionResult, SMSActionResult> implements BreakableTask<SMSActionResult> {

    private SendActivity sendActivity;


    public static final int MAX_RETRIES = 10;
    private AlertDialog dialog;
    private boolean canceledByDialog = false;

    public BackgroundUpdateTask(SendActivity sendActivity) {
        this.sendActivity = sendActivity;
    }


    @Override
    protected synchronized SMSActionResult doInBackground(Boolean... params) {
        ErrorReporterStack.put(LogConst.BACKGROUND_UPDATE_STARTED);
        int retryCount = 0;
        SMSActionResult smsActionResult = null;
        while ((smsActionResult == null || (!smsActionResult.isSuccess() && retryCount < MAX_RETRIES)) && !isCancelled() && !canceledByDialog) {
            retryCount++;
            OptionProvider provider = sendActivity.getSmSoIPPlugin().getProvider();
            if (provider.hasAccounts() && provider.isCheckLoginButtonVisible()) {
                String userName = provider.getUserName();
                String pass = provider.isPasswordVisible() ? provider.getPassword() : "pass";//use a fake pass for checking
                if (userName == null || userName.trim().length() == 0 || pass == null || pass.trim().length() == 0) {
                    return SMSActionResult.NO_CREDENTIALS();
                }
            }
            try {
                if (params[0]) {
                    smsActionResult = sendActivity.getSmSoIPPlugin().getSupplier().refreshInfoTextOnRefreshButtonPressed();
                } else {
                    smsActionResult = sendActivity.getSmSoIPPlugin().getSupplier().refreshInfoTextAfterMessageSuccessfulSent();
                }
                if (smsActionResult != null && smsActionResult.isBreakingProgress()) {
                    canceledByDialog = true;
                    publishProgress(smsActionResult);
                }
            } catch (UnsupportedEncodingException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                smsActionResult = SMSActionResult.UNKNOWN_ERROR();
            } catch (SocketTimeoutException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                smsActionResult = SMSActionResult.TIMEOUT_ERROR();
            } catch (IOException e) {
                Log.e(this.getClass().getCanonicalName(), "", e);
                smsActionResult = SMSActionResult.NETWORK_ERROR();
            } catch (Exception e) {  //for insurance
                Log.e(this.getClass().getCanonicalName(), "", e);
                ACRA.getErrorReporter().handleSilentException(e);
                smsActionResult = SMSActionResult.UNKNOWN_ERROR();
            }
        }
        return smsActionResult;
    }

    @Override
    protected synchronized void onProgressUpdate(SMSActionResult... values) {
        SMSActionResult value = values[0];
        final BreakingProgressDialogFactory factory = value.getFactory();
        dialog = factory.create(sendActivity);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                new BreakingProgressAsyncTask<SMSActionResult>(BackgroundUpdateTask.this).execute(factory);

            }
        });
        dialog.show();
    }


    @Override
    protected void onPostExecute(SMSActionResult actionResult) {
        if (dialog == null || !dialog.isShowing()) {   //only do anything if dialog is not up
            if (!isCancelled() && (dialog == null || !dialog.isShowing())) {
                sendActivity.updateInfoText(actionResult.getMessage());
            } else {
                sendActivity.resetInfoText();
            }
        }
        ErrorReporterStack.put(LogConst.BACKGROUND_UPDATE_ON_POST_EXECUTE);
    }

    @Override
    public void afterChildHasFinished(SMSActionResult childResult) {
        onPostExecute(childResult);
    }
}
