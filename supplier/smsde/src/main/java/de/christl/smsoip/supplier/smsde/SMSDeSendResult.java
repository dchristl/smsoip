package de.christl.smsoip.supplier.smsde;

/**
 * this is a holder for the returning result of SMS.de
 */
public class SMSDeSendResult {
    private final boolean success;
    private String message;

    public SMSDeSendResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }


    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
