package de.christl.smsoip.constant;

import de.christl.smsoip.R;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.ui.BreakingProgressDialog;

/**
 * Result class for some actions in send activity
 */
public class SMSActionResult {
    private boolean success;
    private String message;
    private BreakingProgressDialog builder;

    public SMSActionResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    private SMSActionResult(boolean success, int messageId) {
        this.success = success;
        message = getDefaultText(messageId);
    }

    private SMSActionResult(int messageId) {
        message = getDefaultText(messageId);
    }

    private SMSActionResult(String message) {
        this.message = message;
    }

    public SMSActionResult(BreakingProgressDialog builder) {
        this.builder = builder;
    }

    public BreakingProgressDialog getBuilder() {
        return builder;
    }

    public SMSActionResult setMessage(String message) {
        this.message = message;
        return this;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    private String getDefaultText(int messageId) {
        return SMSoIPApplication.getApp().getString(messageId);
    }


    public static SMSActionResult NO_ERROR() {
        return new SMSActionResult(true, R.string.text_ok);
    }

    public static SMSActionResult LOGIN_SUCCESSFUL() {
        return new SMSActionResult(true, R.string.text_loginSuccessful);
    }

    public static SMSActionResult NETWORK_ERROR() {
        return new SMSActionResult(R.string.text_noNetwork);

    }

    public static SMSActionResult LOGIN_FAILED_ERROR() {
        return new SMSActionResult(R.string.text_loginFailed);
    }

    public static SMSActionResult TIMEOUT_ERROR() {
        return new SMSActionResult(R.string.text_timeOutReached);
    }

    public static SMSActionResult UNKNOWN_ERROR() {
        return new SMSActionResult(R.string.text_unknown_error);
    }

    public static SMSActionResult NO_ERROR(String message) {
        return new SMSActionResult(true, message);
    }

    public static SMSActionResult UNKNOWN_ERROR(String message) {
        return new SMSActionResult(message);
    }

    /**
     * little helper for cancelling without return message
     *
     * @return
     */
    public static SMSActionResult USER_CANCELED() {
        return new SMSActionResult(R.string.text_user_cancelled);
    }

    public static SMSActionResult SHOW_DIALOG_RESULT(BreakingProgressDialog dialog) {
        return new SMSActionResult(dialog);
    }

    public boolean isDialogResult() {
        return builder != null;
    }
}
