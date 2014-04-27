/*
 * Copyright (c) Danny Christl 2013.
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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.AsyncTask;

import org.acra.ACRA;

import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.ui.BreakingProgressDialogFactory;

public class BackgroundSendTask extends AsyncTask<Void, BreakingProgressDialogFactory, FireSMSResultList> implements BreakableTask<FireSMSResultList> {


    private final SendActivity sendActivity;
    private final Dialog progressDialog;
    private AlertDialog dialog;

    public BackgroundSendTask(SendActivity sendActivity, Dialog progressDialog) {
        this.sendActivity = sendActivity;
        this.progressDialog = progressDialog;
    }

    @Override
    protected FireSMSResultList doInBackground(Void... params) {
        FireSMSResultList fireSMSResults = sendActivity.sendTextMessage();
        if (fireSMSResults.getResult().equals(FireSMSResultList.SendResult.DIALOG)) {
            publishProgress(fireSMSResults.getBuilder());
        }
        return fireSMSResults;
    }

    @Override
    protected void onProgressUpdate(BreakingProgressDialogFactory... values) {
        final BreakingProgressDialogFactory factory = values[0];
        dialog = factory.create(progressDialog.getContext());
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                new BreakingProgressAsyncTask<FireSMSResultList>(BackgroundSendTask.this).execute(factory);

            }
        });
        dialog.show();
    }


    @Override
    protected void onPostExecute(FireSMSResultList fireSMSResults) {
        super.onPostExecute(fireSMSResults);
        FireSMSResultList.SendResult sendResult = fireSMSResults.getResult();
        if (dialog == null || !dialog.isShowing()) {
            if (sendResult == FireSMSResultList.SendResult.BOTH || sendResult == FireSMSResultList.SendResult.SUCCESS) { //success or both
                sendActivity.refreshInformationText(false);
            }
            try {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.cancel();
                }
            } catch (Exception e) {
                ACRA.getErrorReporter().handleSilentException(new IllegalArgumentException("Error dismissing dialog<" + progressDialog + ">", e));
            }
            sendActivity.showReturnMessage(fireSMSResults);
        }
    }

    @Override
    public void afterChildHasFinished(FireSMSResultList childResult) {
        onPostExecute(childResult);
    }
}
