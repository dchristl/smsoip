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
import de.christl.smsoip.R;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.models.ErrorReporterStack;
import org.acra.ErrorReporter;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Update the informations in background
 */
public class BackgroundUpdateTask extends AsyncTask<Void, String, SMSActionResult> {
    private SendActivity sendActivity;
    private Timer timer;

    public BackgroundUpdateTask(SendActivity sendActivity) {
        this.sendActivity = sendActivity;
    }


    @Override
    protected SMSActionResult doInBackground(Void... params) {
        ErrorReporterStack.put("background update started");
        TimerTask task = new TimerTask() {
            private String dots = ".";

            @Override
            public void run() {
                if (dots.length() == 3) {
                    dots = ".";
                } else {
                    dots += ".";
                }
                publishProgress(dots);
            }

            @Override
            public boolean cancel() {
                publishProgress(null);
                return super.cancel();
            }
        };

        timer = new Timer();
        timer.schedule(task, 0, 500);
        try {
            return sendActivity.getSmSoIPPlugin().getSupplier().refreshInfoTextOnRefreshButtonPressed();
        } catch (Exception e) {    //TODO remove after stability improvements
            Log.e(this.getClass().getCanonicalName(), "", e);
            ErrorReporter.getInstance().handleSilentException(e);
            return SMSActionResult.UNKNOWN_ERROR();
        }
    }

    @Override
    protected void onProgressUpdate(String... dots) {
        if (dots != null) {
            sendActivity.updateInfoTextAndRefreshButton(sendActivity.getString(R.string.text_pleaseWait) + dots[0]);
        } else {
            sendActivity.updateInfoTextAndRefreshButton(null);
        }

    }


    @Override
    protected void onCancelled() {
        if (timer != null) {
            timer.cancel();
        }
        sendActivity.updateInfoTextAndRefreshButton(null);
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(SMSActionResult actionResult) {
        if (timer != null) {
            timer.cancel();
        }
        if (actionResult != null && actionResult.isSuccess()) {
            final String infoText = actionResult.getMessage();
            sendActivity.updateInfoTextAndRefreshButton(infoText);
        } else {
            sendActivity.updateInfoTextAndRefreshButton(null);
        }
        ErrorReporterStack.put("background update on post execute");
    }
}
