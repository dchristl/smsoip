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

package de.christl.smsoip.supplier.sloono;

import android.os.AsyncTask;
import android.util.Log;
import de.christl.smsoip.constant.SMSActionResult;
import org.acra.ACRA;

import java.io.IOException;
import java.net.SocketTimeoutException;


public class RefreshSenderTask extends AsyncTask<Void, Boolean, SMSActionResult> {
    private SloonoOptionProvider sloonoProvider;

    public RefreshSenderTask(SloonoOptionProvider sloonoProvider) {
        this.sloonoProvider = sloonoProvider;
    }

    @Override
    protected SMSActionResult doInBackground(Void... params) {

        try {
            return sloonoProvider.getSloonoSupplier().resolveNumbers();
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
    protected void onPostExecute(SMSActionResult smsActionResult) {
        if (smsActionResult.isSuccess()) {
            sloonoProvider.refreshDropDownAfterSuccesfulUpdate();
        } else {
           sloonoProvider.setErrorTextAfterSenderUpdate(smsActionResult.getMessage());
        }
    }
}
