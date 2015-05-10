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

package de.christl.smsoip.supplier.gmx;

import android.os.AsyncTask;
import de.christl.smsoip.constant.SMSActionResult;
import org.acra.ACRA;

import java.io.IOException;
import java.net.SocketTimeoutException;

public class RefreshSenderTask extends AsyncTask<Void, Boolean, SMSActionResult> {
    private GMXOptionProvider gmxOptionProvider;

    public RefreshSenderTask(GMXOptionProvider gmxOptionProvider) {
        this.gmxOptionProvider = gmxOptionProvider;
    }

    @Override
    protected SMSActionResult doInBackground(Void... params) {

        try {
            return gmxOptionProvider.getSupplier().resolveNumbers();
        } catch (SocketTimeoutException e) {
            return SMSActionResult.TIMEOUT_ERROR();
        } catch (IOException e) {
            return SMSActionResult.NETWORK_ERROR();
        } catch (Exception e) {                                                      //for insurance
            ACRA.getErrorReporter().handleSilentException(e);
            return SMSActionResult.UNKNOWN_ERROR();
        }
    }

    @Override
    protected void onPostExecute(SMSActionResult smsActionResult) {
        if (smsActionResult.isSuccess()) {
            gmxOptionProvider.refreshDropDownAfterSuccesfulUpdate();
        } else {
            gmxOptionProvider.setErrorMessageOnUpdate(smsActionResult.getMessage());
        }
    }
}