package de.christl.smsoip.constant;

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
