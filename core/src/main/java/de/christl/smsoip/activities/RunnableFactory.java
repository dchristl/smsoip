package de.christl.smsoip.activities;

import android.app.ProgressDialog;
import android.os.Handler;
import de.christl.smsoip.constant.Result;

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

    private Runnable getUpdateUIRunnable(final CharSequence resultMessage, final CharSequence infoText) {
        return new Runnable() {
            public void run() {
                RunnableFactory.this.sendActivity.showReturnMessage(resultMessage, infoText);
            }
        };
    }

    public Runnable getSendAndUpdateUIRunnable() {
        return new Runnable() {
            public void run() {
                Result sendResult = RunnableFactory.this.sendActivity.send();
                CharSequence resultMessage = sendResult.getUserText();
                CharSequence infoText = null;
                boolean successfulSent = sendResult.equals(Result.NO_ERROR());
                boolean successfulRefreshed;
                if (successfulSent) {
                    Result refreshResult = RunnableFactory.this.sendActivity.refreshInformations(true);
                    successfulRefreshed = refreshResult.equals(Result.NO_ERROR());
                    if (successfulRefreshed) {
                        infoText = refreshResult.getUserText();
                    }
                }
                updateUIHandler.post(getUpdateUIRunnable(resultMessage, infoText));
                RunnableFactory.this.progressDialog.cancel();
            }
        };
    }

    public Runnable getRefreshInfosAndUpdateUIRunnable() {
        return new Runnable() {
            public void run() {
                Result result = RunnableFactory.this.sendActivity.refreshInformations(false);
                CharSequence infoText = null;
                CharSequence messageText = null;
                if (result.equals(Result.NO_ERROR())) {
                    infoText = result.getUserText();
                } else {
                    messageText = result.getUserText();
                }
                updateUIHandler.post(getUpdateUIRunnable(messageText, infoText));
                progressDialog.cancel();
            }
        };
    }
}
