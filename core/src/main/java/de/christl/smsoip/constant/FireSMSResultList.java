package de.christl.smsoip.constant;

import de.christl.smsoip.activities.Receiver;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple list for convenient access methods
 */
public class FireSMSResultList extends ArrayList<FireSMSResult> {

    private List<Receiver> successList = new ArrayList<Receiver>();
    private SendResult result = SendResult.NOT_YET_SET;
    private List<Receiver> errorList = new ArrayList<Receiver>();

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

    public List<Receiver> getSuccessList() {
        return successList;
    }

    public List<Receiver> getErrorList() {
        return errorList;
    }

    private void setSuccessList(List<Receiver> successList) {
        this.successList = successList;
    }

    private void setErrorList(List<Receiver> errorList) {
        this.errorList = errorList;
    }

    /**
     * method for getting a result for all receivers
     * <b>make sure this is is the only result in list</b>
     *
     * @param result
     * @return
     * @deprecated Use version with receivers
     */
    @Deprecated
    public static FireSMSResultList getAllInOneResult(SMSActionResult result) {
        FireSMSResultList out = new FireSMSResultList(1);
        out.add(new FireSMSResult(null, result));
        return out;
    }

    public static FireSMSResultList getAllInOneResult(SMSActionResult result, List<Receiver> receivers) {
        FireSMSResultList out = new FireSMSResultList(1);
        out.add(new FireSMSResult(null, result));
        if (result.isSuccess()) {
            out.setSuccessList(receivers);
        } else {
            out.setErrorList(receivers);
        }
        return out;
    }

}
