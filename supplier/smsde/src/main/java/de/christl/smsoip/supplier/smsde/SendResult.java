package de.christl.smsoip.supplier.smsde;

/**
 * this is a holder for the returning result of SMS.de
 */
public class SendResult {
    private final String number;
    private final boolean success;
    private String returnMessage;

    public SendResult(String number, boolean success, String returnMessage) {
        this.number = number;
        this.success = success;
        this.returnMessage = returnMessage;
    }

    public String getNumber() {
        return number;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getReturnMessage() {
        return returnMessage;
    }
}
