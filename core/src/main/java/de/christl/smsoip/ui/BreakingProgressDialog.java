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

package de.christl.smsoip.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import de.christl.smsoip.activities.AllActivity;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.SMSActionResult;

/**
 * Abstract class for showing extra dialog durring fire SMS phase
 */
public abstract class BreakingProgressDialog extends AlertDialog.Builder {

    private DialogInterface.OnDismissListener listener;
    protected int result = DialogInterface.BUTTON_NEGATIVE;


    public BreakingProgressDialog() {
        super(AllActivity.getActivity());
    }


    public final FireSMSResultList getFireSMSResults() {
        if (result == DialogInterface.BUTTON_POSITIVE) {
            return onPositiveResult();
        } else {
            return onNegativeResult();
        }
    }


    public void setResult(int result) {
        this.result = result;
    }

    protected FireSMSResultList onNegativeResult() {
        return FireSMSResultList.getAllInOneResult(SMSActionResult.USER_CANCELED(), null);
    }

    public void setListener(DialogInterface.OnDismissListener listener) {
        this.listener = listener;
    }

    @Override
    public AlertDialog create() {
        AlertDialog alertDialog = super.create();
        alertDialog.setOnDismissListener(listener);
        return alertDialog;
    }


    protected abstract FireSMSResultList onPositiveResult();

}
