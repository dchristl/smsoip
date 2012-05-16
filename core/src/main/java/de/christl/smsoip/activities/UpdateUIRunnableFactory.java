package de.christl.smsoip.activities;

/**
 * little helper for building updating the ui after sending or refreshing informations
 */
public class UpdateUIRunnableFactory {

    static Runnable getRunnable(final SendActivity sendActivity, final CharSequence resultMessage, final CharSequence infoText, final boolean successfulSent) {
        return new Runnable() {
            public void run() {
                sendActivity.showReturnMessage(resultMessage, infoText, successfulSent);
            }
        };
    }
}
