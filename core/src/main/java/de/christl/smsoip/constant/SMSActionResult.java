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
    private boolean retryMakesSense = false;

    private SMSActionResult(boolean success, boolean retryMakesSense, int messageId) {
        this.success = success;
        this.retryMakesSense = retryMakesSense;
        message = getDefaultText(messageId);
    }

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
        return new SMSActionResult(false, true, R.string.text_timeOutReached);
    }

    public static SMSActionResult UNKNOWN_ERROR() {
        return new SMSActionResult(false, true, R.string.text_unknown_error);
    }

    public static SMSActionResult NO_ERROR(String message) {
        return new SMSActionResult(true, message);
    }

    public static SMSActionResult UNKNOWN_ERROR(String message) {
        SMSActionResult smsActionResult = new SMSActionResult(message);
        smsActionResult.setRetryMakesSense(true);
        return smsActionResult;
    }

    public static SMSActionResult NO_CREDENTIALS() {
        return new SMSActionResult(R.string.text_noCredentials);
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

    public void setRetryMakesSense(boolean retryMakesSense) {
        this.retryMakesSense = retryMakesSense;
    }

    public boolean isRetryMakesSense() {
        return retryMakesSense;
    }
}
