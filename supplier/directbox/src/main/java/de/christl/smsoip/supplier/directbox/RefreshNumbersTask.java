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

package de.christl.smsoip.supplier.directbox;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import de.christl.smsoip.constant.SMSActionResult;
import org.acra.ACRA;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;


public class RefreshNumbersTask extends AsyncTask<Void, String, SMSActionResult> {
    private TextView infoTextField;
    private String waitText;
    private Timer timer;
    private DirectboxOptionProvider directboxOptionProvider;
    private boolean update = true;

    public RefreshNumbersTask(DirectboxOptionProvider directboxOptionProvider) {
        this.directboxOptionProvider = directboxOptionProvider;
        this.infoTextField = directboxOptionProvider.getTextField();
//        waitText = directboxOptionProvider.getTextByResourceId(R.string.text_pleaseWait);
    }

    @Override
    protected SMSActionResult doInBackground(Void... params) {
        TimerTask task = new TimerTask() {
            private String dots = ".";

            @Override
            public void run() {
                if (dots.length() == 3) {
                    dots = ".";
                } else {
                    dots += ".";
                }
                if (!RefreshNumbersTask.this.isCancelled()) {
                    publishProgress(waitText + dots);
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
        try {
            return directboxOptionProvider.getDirectboxSupplier().resolveNumbers();
        } catch (SocketTimeoutException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            Log.e(this.getClass().getCanonicalName(), "", e);
            return SMSActionResult.NETWORK_ERROR();
        } catch (Exception e) {                                                      //for insurance
            ACRA.getErrorReporter().handleSilentException(e);
            return SMSActionResult.UNKNOWN_ERROR();
        }
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (update) {
            infoTextField.setText(values[0]);
        }
    }

    @Override
    protected void onCancelled() {
        if (timer != null) {
            timer.cancel();
        }
        update = false;
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(SMSActionResult smsActionResult) {
        update = false;
        timer.cancel();
        if (smsActionResult.isSuccess()) {
            directboxOptionProvider.refreshDropDownAfterSuccesfulUpdate();
        } else {
            infoTextField.setText(smsActionResult.getMessage());
        }
    }
}
