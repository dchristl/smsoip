package de.christl.smsoip.constant;

import de.christl.smsoip.activities.Receiver;

/**
 * Used for result after sending
 */
public class FireSMSResult {

    private Receiver receiver;
    private SMSActionResult result;

    public FireSMSResult(Receiver receiver, SMSActionResult result) {
        this.receiver = receiver;
        this.result = result;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public SMSActionResult getResult() {
        return result;
    }

}
