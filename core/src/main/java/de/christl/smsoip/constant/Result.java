package de.christl.smsoip.constant;

import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 * supported results for user output
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
            return getDefaultText();
        }
    }

    public CharSequence getDefaultText() {
        return SMSoIPApplication.getApp().getText(defaultText);
    }

    public static Result NO_ERROR() {
        return Result.NO_ERROR.setAlternateText(null);
    }

    public static Result LOGIN_SUCCESSFUL() {
        return Result.NO_ERROR.setAlternateText(SMSoIPApplication.getApp().getString(R.string.text_loginSuccessful));
    }

    public static Result NETWORK_ERROR() {
        return Result.NETWORK_ERROR.setAlternateText(null);

    }

    public static Result LOGIN_FAILED_ERROR() {
        return Result.LOGIN_FAILED_ERROR.setAlternateText(null);
    }

    public static Result TIMEOUT_ERROR() {
        return Result.TIMEOUT_ERROR.setAlternateText(null);
    }


    public static Result UNKNOWN_ERROR() {
        return Result.UNKNOWN_ERROR.setAlternateText(null);
    }


}
