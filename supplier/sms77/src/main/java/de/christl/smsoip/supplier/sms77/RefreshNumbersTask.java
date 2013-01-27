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

package de.christl.smsoip.supplier.sms77;

import android.os.AsyncTask;
import de.christl.smsoip.constant.SMSActionResult;

public class RefreshNumbersTask extends AsyncTask<Void, String, SMSActionResult> {
    private SMS77OptionProvider sms77OptionProvider;

    public RefreshNumbersTask(SMS77OptionProvider sms77OptionProvider) {
        //To change body of created methods use File | Settings | File Templates.
        this.sms77OptionProvider = sms77OptionProvider;
    }

    @Override
    protected SMSActionResult doInBackground(Void... params) {
        return SMSActionResult.NO_ERROR();
//        try {
//            return sms77OptionProvider.getSms77Supplier().resolveNumbers();
//        } catch (SocketTimeoutException e) {
//            return SMSActionResult.TIMEOUT_ERROR();
//        } catch (IOException e) {
//            return SMSActionResult.NETWORK_ERROR();
//        } catch (Exception e) {
//            ACRA.getErrorReporter().handleException(e);//for insurance
//            return SMSActionResult.UNKNOWN_ERROR();
//        }
    }


//    @Override
//    protected void onPostExecute(SMSActionResult smsActionResult) {
//        if (smsActionResult.isSuccess()) {
//            sms77OptionProvider.refreshDropDownAfterSuccesfulUpdate();
//        } else {
//            sms77OptionProvider.setErrorMessageOnUpdate(smsActionResult.getMessage());
//        }
//    }
}
