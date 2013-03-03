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

import android.app.Dialog;
import android.os.AsyncTask;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.constant.FireSMSResultList;

public class BackgroundSendTask extends AsyncTask<Void, Void, FireSMSResultList> {


    private final SendActivity sendActivity;
    private final Dialog progressDialog;

    public BackgroundSendTask(SendActivity sendActivity, Dialog progressDialog) {
        this.sendActivity = sendActivity;
        this.progressDialog = progressDialog;
    }

    @Override
    protected FireSMSResultList doInBackground(Void... params) {
        return sendActivity.sendTextMessage();
    }

    @Override
    protected void onPostExecute(FireSMSResultList fireSMSResults) {
        super.onPostExecute(fireSMSResults);
        FireSMSResultList.SendResult sendResult = fireSMSResults.getResult();
        if (sendResult == FireSMSResultList.SendResult.BOTH || sendResult == FireSMSResultList.SendResult.SUCCESS) { //success or both
            sendActivity.refreshInformationText(false);
        }
        progressDialog.cancel();
        sendActivity.showReturnMessage(fireSMSResults);
    }
}
