package de.christl.smsoip.constant;

import de.christl.smsoip.activities.Receiver;

/**
 * Used for result after sending
 */
public class FireSMSResult {

    private Receiver receiver;
    private Result result;

    public FireSMSResult(Receiver receiver, Result result) {
        this.receiver = receiver;
        this.result = result;
    }

    public Receiver getReceiver() {
        return receiver;
    }

    public Result getResult() {
        return result;
    }


    //    public static AllInOneFireSMSResult getAllInOneResult(Result result) {
//        return new AllInOneFireSMSResult(null, result);
//    }
//
//
//    /**
//     * simple subclass indicating result is valid for all numbers
//     */
//    private static class AllInOneFireSMSResult extends FireSMSResult {
//        public AllInOneFireSMSResult(String receiver, Result result) {
//            super(receiver, result);
//        }
//    }
}
