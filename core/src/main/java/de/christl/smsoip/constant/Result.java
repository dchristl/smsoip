package de.christl.smsoip.constant;

import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 * dupported results for user output
 */
public enum Result {
    /**
     * all OK, add additional info for refresh ionformations
     */
    NO_ERROR(R.string.text_ok),
    NETWORK_ERROR(R.string.text_noNetwork),
    LOGIN_FAILED_ERROR(R.string.text_loginFailed),
    TIMEOUT_ERROR(R.string.text_timeOutReached),
    UNKNOWN_ERROR(R.string.text_unknown_error);


    private final int defaultText;
    private String alternateText = null;

    private Result(int defaultText) {
        this.defaultText = defaultText;
    }

    public Result setAlternateText(String alternateText) {
        this.alternateText = alternateText;
        return this;
    }

    public CharSequence getUserText() {
        if (alternateText != null) {
            return alternateText;
        } else {
            return SMSoIPApplication.getApp().getText(defaultText);
        }
    }
}
