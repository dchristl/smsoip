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

import android.os.AsyncTask;
import android.util.Log;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.constant.LogConst;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.models.ErrorReporterStack;
import org.acra.ACRA;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;

/**
 * Update the informations in background
 */
public class BackgroundUpdateTask extends AsyncTask<Boolean, SMSActionResult, SMSActionResult> implements BreakableTask<SMSActionResult> {

    private SendActivity sendActivity;


    public static final int MAX_RETRIES = 20;

    public BackgroundUpdateTask(SendActivity sendActivity) {
        this.sendActivity = sendActivity;
    }


    @Override
    protected SMSActionResult doInBackground(Boolean... params) {
        ErrorReporterStack.put(LogConst.BACKGROUND_UPDATE_STARTED);
        int retryCount = 0;
        SMSActionResult smsActionResult = null;
        try {
            while ((smsActionResult == null || (!smsActionResult.isSuccess() && retryCount < MAX_RETRIES)) && !isCancelled()) {
                publishProgress(smsActionResult);
                retryCount++;
                String userName = sendActivity.getSmSoIPPlugin().getProvider().getUserName();
                String pass = sendActivity.getSmSoIPPlugin().getProvider().getPassword();
                if (userName == null || userName.trim().length() == 0 || pass == null || pass.trim().length() == 0) {
                    smsActionResult = SMSActionResult.NO_CREDENTIALS();  //TODO remove this, cause not valid everytime
                } else {
                    try {
                        if (params[0]) {
                            smsActionResult = sendActivity.getSmSoIPPlugin().getSupplier().refreshInfoTextOnRefreshButtonPressed();
                        } else {
                            smsActionResult = sendActivity.getSmSoIPPlugin().getSupplier().refreshInfoTextAfterMessageSuccessfulSent();
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
                    }
                }
            }
        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            ACRA.getErrorReporter().handleSilentException(e);
            smsActionResult = SMSActionResult.UNKNOWN_ERROR();
        }
        return smsActionResult;
    }

    @Override
    protected void onProgressUpdate(SMSActionResult... inProgress) {
        sendActivity.showUpdateProgressBar();
    }


    @Override
    protected void onCancelled() {
        super.onCancelled();
        sendActivity.updateInfoTextByCancel();
    }

    @Override
    protected void onPostExecute(SMSActionResult actionResult) {
        if (!isCancelled()) {
            sendActivity.updateInfoText(actionResult.getMessage());
        } else {
            sendActivity.updateInfoTextByCancel();
        }
        ErrorReporterStack.put(LogConst.BACKGROUND_UPDATE_ON_POST_EXECUTE);
    }

    @Override
    public void afterChildHasFinished(SMSActionResult childResult) {

    }
}
