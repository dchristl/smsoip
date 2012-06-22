package de.christl.smsoip.provider.versioned;

import de.christl.smsoip.constant.Result;

/**
 * Used for result after sending
 */
public class FireSMSResult {

    private String receiver;
    private Result result;

    public FireSMSResult(String receiver, Result result) {
        this.receiver = receiver;
        this.result = result;
    }

    public String getReceiver() {
        return receiver;
    }

    public Result getResult() {
        return result;
    }
}
