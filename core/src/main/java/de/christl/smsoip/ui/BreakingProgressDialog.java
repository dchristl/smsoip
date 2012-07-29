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
