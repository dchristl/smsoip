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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Update the informations in background
 */
public class BackgroundUpdateTask extends AsyncTask<Boolean, Boolean, SMSActionResult> {

    private SendActivity sendActivity;
    private Timer timer;

    private int retryCount = 0;

    public static final int MAX_RETRIES = 20;
    private boolean update = true;

    public BackgroundUpdateTask(SendActivity sendActivity) {
        this.sendActivity = sendActivity;
    }

    public BackgroundUpdateTask(SendActivity sendActivity, int retryCount) {
        this.sendActivity = sendActivity;
        this.retryCount = retryCount;
    }

    @Override
    protected SMSActionResult doInBackground(Boolean... params) {
        ErrorReporterStack.put(LogConst.BACKGROUND_UPDATE_STARTED);
        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                if (!BackgroundUpdateTask.this.isCancelled()) {
                    publishProgress(true);
                }
            }

            @Override
            public boolean cancel() {
                publishProgress(null, null);
                return super.cancel();
            }
        };

        timer = new Timer();
        timer.schedule(task, 0, 500);
        SMSActionResult smsActionResult;
        try {
            String userName = sendActivity.getSmSoIPPlugin().getProvider().getUserName();
            String pass = sendActivity.getSmSoIPPlugin().getProvider().getPassword();
            if (userName == null || userName.trim().length() == 0 || pass == null || pass.trim().length() == 0) {
                timer.cancel();
                smsActionResult = SMSActionResult.NO_CREDENTIALS();
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
        } catch (Exception e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            ACRA.getErrorReporter().handleSilentException(e);
            smsActionResult = SMSActionResult.UNKNOWN_ERROR();
        }
        return smsActionResult;
    }

    @Override
    protected void onProgressUpdate(Boolean... inProgress) {
        if (update) {
            if (inProgress != null) {
                sendActivity.updateInfoTextAndRefreshButton(null, true);
            } else {
                sendActivity.updateInfoTextAndRefreshButton(null, false);
            }
        }

    }


    @Override
    protected void onCancelled() {
        if (timer != null) {
            timer.cancel();
        }
        sendActivity.updateInfoTextAndRefreshButton(null, false);
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(SMSActionResult actionResult) {
        update = false;
        if (timer != null) {
            timer.cancel();
        }
        if (actionResult != null && actionResult.isSuccess() && !isCancelled()) {
            final String infoText = actionResult.getMessage();
            sendActivity.updateInfoTextAndRefreshButton(infoText, false);
        } else if (actionResult != null) {
            if (!isCancelled()) {
                this.cancel(true);
                if (timer != null) {
                    timer.cancel();
                }
                if (actionResult.isRetryMakesSense() && retryCount <= MAX_RETRIES) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Log.e(this.getClass().getCanonicalName(), "", e);
                    }
                    new BackgroundUpdateTask(sendActivity, retryCount + 1).execute(true);
                }
            }
            sendActivity.updateInfoTextAndRefreshButton(actionResult.getMessage(), false);
        }
        ErrorReporterStack.put(LogConst.BACKGROUND_UPDATE_ON_POST_EXECUTE);
    }

}