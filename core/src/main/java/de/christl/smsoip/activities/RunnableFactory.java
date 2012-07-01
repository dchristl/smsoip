package de.christl.smsoip.activities;

import android.app.ProgressDialog;
import android.os.Handler;
import de.christl.smsoip.constant.FireSMSResultList;
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


    /**
     * since API level 14
     */
    public Runnable getRefreshAndUpdateUIRunnable() {
        return new Runnable() {
            public void run() {
                final SMSActionResult result = RunnableFactory.this.sendActivity.refreshInformationText(false);
                Runnable runnable = new Runnable() {
                    public void run() {
                        RunnableFactory.this.sendActivity.updateInfoTextThroughRefresh(result);
                    }
                };
                updateUIHandler.post(runnable);
                progressDialog.cancel();
            }
        };
    }

    public void updateInfoTextInBackground() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                SMSActionResult actionResult = sendActivity.refreshInformationText(false);
                if (actionResult.isSuccess()) {
                    final String infoText = actionResult.getMessage();
                    Runnable runnable = new Runnable() {
                        public void run() {
                            sendActivity.updateInfoTextAndRefreshButton(infoText);
                        }
                    };
                    updateUIHandler.post(runnable);
                } else {
                    updateUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            sendActivity.updateInfoTextAndRefreshButton(null);

                        }
                    });
                }

            }
        });
        thread.start();
    }
}
