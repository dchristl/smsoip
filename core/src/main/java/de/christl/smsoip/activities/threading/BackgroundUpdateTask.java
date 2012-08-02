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
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.constant.SMSActionResult;

/**
 * Created with IntelliJ IDEA.
 * User: Danny
 * Date: 02.08.12
 * Time: 19:24
 * To change this template use File | Settings | File Templates.
 */
public class BackgroundUpdateTask extends AsyncTask<Void, Void, SMSActionResult> {
    private SendActivity sendActivity;

    public BackgroundUpdateTask(SendActivity sendActivity) {
        this.sendActivity = sendActivity;
    }


    @Override
    protected SMSActionResult doInBackground(Void... params) {
        return sendActivity.getSmSoIPPlugin().getSupplier().refreshInfoTextOnRefreshButtonPressed();
    }

    @Override
    protected void onPostExecute(SMSActionResult actionResult) {
        if (actionResult != null && actionResult.isSuccess()) {
            final String infoText = actionResult.getMessage();
            sendActivity.updateInfoTextAndRefreshButton(infoText);
        } else {
            sendActivity.updateInfoTextAndRefreshButton(null);
        }
    }
}
