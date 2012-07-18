package de.christl.smsoip.constant;

import de.christl.smsoip.activities.Receiver;

import java.util.ArrayList;

/**
 * Simple list for convenient access methods
 */
public class FireSMSResultList extends ArrayList<FireSMSResult> {

    private ArrayList<Receiver> successList = new ArrayList<Receiver>();
    private SendResult result = SendResult.NOT_YET_SET;
    private ArrayList<Receiver> errorList = new ArrayList<Receiver>();

    public FireSMSResultList(int capacity) {
        super(capacity);
    }

    public FireSMSResultList() {
    }

    public static enum SendResult {
        NOT_YET_SET, SUCCESS, ERROR, BOTH

    }


    @Override
    public boolean add(FireSMSResult object) {
        setSendResultAndFillLists(object);
        return super.add(object);
    }

    @Override
    public void add(int index, FireSMSResult object) {
        setSendResultAndFillLists(object);
        super.add(index, object);
    }

    private void setSendResultAndFillLists(FireSMSResult fireSMSResult) {
        Receiver receiver = fireSMSResult.getReceiver();
        if (fireSMSResult.getResult().isSuccess()) {
            switch (result) {
                case NOT_YET_SET:
                    result = SendResult.SUCCESS;
                    break;
                case ERROR:
                    result = SendResult.BOTH;
                    break;
                default:  //in case 2 or 3 result is already set correct
                    break;
            }
            if (receiver != null) {    //all in one result
                successList.add(receiver);
            }
        } else {
            switch (result) {
                case NOT_YET_SET:
                    result = SendResult.ERROR;
                    break;
                case SUCCESS:
                    result = SendResult.BOTH;
                    break;
                default:  //in case 1 or 3 result is already set correctly
                    break;
            }
            if (receiver != null) {
                errorList.add(receiver);
            }
        }

    }

    public SendResult getResult() {
        return result;
    }

    public ArrayList<Receiver> getSuccessList() {
        return successList;
    }

    public ArrayList<Receiver> getErrorList() {
        return errorList;
    }

    /**
     * method for getting a result for all receivers
     * <b>make sure this is is the only result in list</b>
     *
     * @param result
     * @return
     */
    public static FireSMSResultList getAllInOneResult(SMSActionResult result) {

        FireSMSResultList out = new FireSMSResultList(1);
        out.add(new FireSMSResult(null, result));
        return out;
    }

}
