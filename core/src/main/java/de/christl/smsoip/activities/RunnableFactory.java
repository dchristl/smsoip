package de.christl.smsoip.activities;

import android.app.ProgressDialog;
import android.os.Handler;
import de.christl.smsoip.constant.FireSMSResultList;
import de.christl.smsoip.constant.Result;
import de.christl.smsoip.constant.SMSActionResult;

/**
 * little helper for building updating the ui after sending or refreshing informations
 */
public class RunnableFactory {

    private SendActivity sendActivity;
    private ProgressDialog progressDialog;
    private Handler updateUIHandler;


    RunnableFactory(SendActivity sendActivity, ProgressDialog progressDialog) {
        this.sendActivity = sendActivity;
        this.progressDialog = progressDialog;
        updateUIHandler = new Handler();
    }

    /**
     * @param resultMessage
     * @param infoText
     * @param succesfulSent
     * @deprecated will be removed in future releases (for old send mechanism)
     */
    @Deprecated
    private Runnable getUpdateUIRunnable(final CharSequence resultMessage, final CharSequence infoText, final boolean succesfulSent) {
        return new Runnable() {
            public void run() {
                RunnableFactory.this.sendActivity.showReturnMessage(resultMessage, infoText, succesfulSent);
            }
        };
    }

    /**
     * @return the runnable for sending
     * @deprecated will be removed in future releases (for old send mechanism)
     */
    @Deprecated
    public Runnable getSendAndUpdateUIRunnable() {
        return new Runnable() {
            public void run() {
                Result sendResult = RunnableFactory.this.sendActivity.send();
                CharSequence resultMessage = sendResult.getUserText();
                boolean successfulSent = sendResult.equals(Result.NO_ERROR);
                CharSequence infoText = null;
                if (successfulSent) {
                    Result refreshResult = RunnableFactory.this.sendActivity.refreshInformations(true);
                    boolean successfulRefreshed = refreshResult.equals(Result.NO_ERROR);
                    if (successfulRefreshed) {
                        infoText = refreshResult.getUserText();
                    }
                }
                updateUIHandler.post(getUpdateUIRunnable(resultMessage, infoText, successfulSent));
                RunnableFactory.this.progressDialog.cancel();
            }
        };
    }

    /**
     * available since API level 14
     *
     * @return
     */
    public Runnable getFireSMSAndUpdateUIRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                FireSMSResultList fireSMSResults = RunnableFactory.this.sendActivity.sendByThread();
                CharSequence infoText = null;
                FireSMSResultList.SendResult result = fireSMSResults.getResult();
                if (result == FireSMSResultList.SendResult.BOTH || result == FireSMSResultList.SendResult.SUCCESS) { //success or both
                    SMSActionResult refreshResult = RunnableFactory.this.sendActivity.refreshInformationText(true);
                    if (refreshResult.isSuccess()) {
                        infoText = refreshResult.getMessage();
                    }
                }
                updateUIHandler.post(getUpdateUIRunnable(fireSMSResults, infoText == null ? null : infoText.toString()));
                RunnableFactory.this.progressDialog.cancel();
            }
        };
    }

    /**
     * since API level 14
     *
     * @param fireSMSResults
     * @param infoText
     * @return
     */
    private Runnable getUpdateUIRunnable(final FireSMSResultList fireSMSResults, final String infoText) {
        return new Runnable() {
            public void run() {
                RunnableFactory.this.sendActivity.showReturnMessage(fireSMSResults, infoText);
            }
        };
    }


    public Runnable getRefreshInfosAndUpdateUIRunnable() {
        return new Runnable() {
            public void run() {
                SMSActionResult result = RunnableFactory.this.sendActivity.refreshInformationText(false);
                CharSequence infoText = null;
                CharSequence messageText = null;
                if (result.isSuccess()) {
                    infoText = result.getMessage();
                } else {
                    messageText = result.getMessage();
                }
                updateUIHandler.post(getUpdateUIRunnable(messageText, infoText, false));
                progressDialog.cancel();
            }
        };
    }
}
