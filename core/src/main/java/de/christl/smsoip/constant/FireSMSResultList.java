package de.christl.smsoip.constant;

import de.christl.smsoip.activities.Receiver;

import java.util.ArrayList;

/**
 * Simple list for convenient access methods
 */
public class FireSMSResultList extends ArrayList<FireSMSResult> {

    ArrayList<Receiver> successList = new ArrayList<Receiver>();
    ArrayList<Receiver> errorList = new ArrayList<Receiver>();


    public static enum SendResult {
        NOT_YET_SET, SUCCESS, ERROR, BOTH
    }

    SendResult result = SendResult.NOT_YET_SET;


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
        if (fireSMSResult.getResult().equals(Result.NO_ERROR)) {
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
            successList.add(fireSMSResult.getReceiver());
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
            errorList.add(fireSMSResult.getReceiver());
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
}
