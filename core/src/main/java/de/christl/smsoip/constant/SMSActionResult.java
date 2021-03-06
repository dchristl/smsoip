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
import de.christl.smsoip.ui.BreakingProgressDialogFactory;

/**
 * Result class for some actions in send activity
 */
public class SMSActionResult {
    private boolean success;
    private String message;
    private BreakingProgressDialogFactory builder;
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

    public SMSActionResult(BreakingProgressDialogFactory builder) {
        this.builder = builder;
    }

    public BreakingProgressDialogFactory getFactory() {
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
        return new SMSActionResult(true, R.string.ok);
    }

    public static SMSActionResult LOGIN_SUCCESSFUL() {
        return new SMSActionResult(true, R.string.loginSuccessful);
    }

    public static SMSActionResult NETWORK_ERROR() {
        return new SMSActionResult(R.string.noNetwork);

    }

    public static SMSActionResult LOGIN_FAILED_ERROR() {
        return new SMSActionResult(R.string.loginFailed);
    }

    public static SMSActionResult TIMEOUT_ERROR() {
        return new SMSActionResult(false, true, R.string.timeOutReached);
    }

    public static SMSActionResult UNKNOWN_ERROR() {
        return new SMSActionResult(false, true, R.string.unknown_error);
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
        return new SMSActionResult(R.string.noCredentials);
    }

    public static SMSActionResult USER_CANCELED() {
        SMSActionResult smsActionResult = new SMSActionResult(R.string.user_cancelled);
        smsActionResult.setRetryMakesSense(false);
        return smsActionResult;
    }

    public static SMSActionResult SHOW_DIALOG_RESULT(BreakingProgressDialogFactory dialog) {
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

    public boolean isBreakingProgress() {
        return builder != null;
    }
}
