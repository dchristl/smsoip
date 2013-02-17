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

package de.christl.smsoip.supplier.sample;

import android.util.Log;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;
import de.christl.smsoip.option.OptionProvider;
import de.christl.smsoip.provider.versioned.ExtendedSMSSupplier;
import de.christl.smsoip.ui.BreakingProgressDialogDismissListener;
import de.christl.smsoip.ui.BreakingProgressDialogFactory;

import java.io.IOException;
import java.util.List;

public class SampleSupplier implements ExtendedSMSSupplier {
    private SampleOptionProvider provider;

    public SampleSupplier() {
        provider = new SampleOptionProvider();
    }

    @Override
    public SMSActionResult checkCredentials(String userName, String password) throws IOException, NumberFormatException {

        BreakingProgressDialogFactory breakingProgressDialogFactory = new BreakingProgressDialogFactory();
        breakingProgressDialogFactory.setPositiveButtonText("OK");
        breakingProgressDialogFactory.setListener(new BreakingProgressDialogDismissListener() {
            @Override
            public SMSActionResult onPositiveButtonClicked() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                }
                return SMSActionResult.UNKNOWN_ERROR("Captcha error");
            }

            @Override
            public SMSActionResult onNegativeButtonClicked() {
                return null;
            }

            @Override
            public SMSActionResult onCancel() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    Log.e(this.getClass().getCanonicalName(), "", e);
                }
                return SMSActionResult.USER_CANCELED();
            }
        });
        return SMSActionResult.SHOW_DIALOG_RESULT(breakingProgressDialogFactory);
    }

    @Override
    public SMSActionResult refreshInfoTextOnRefreshButtonPressed() throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public SMSActionResult refreshInfoTextAfterMessageSuccessfulSent() throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public FireSMSResultList fireSMS(String smsText, List<Receiver> receivers, String spinnerText) throws IOException, NumberFormatException {
        return null;
    }

    @Override
    public OptionProvider getProvider() {
        return provider;
    }
}
