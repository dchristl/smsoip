package de.christl.smsoip.constant;

import java.util.ArrayList;

/**
 * Simple list for convenient access methods
 */
public class FireSMSResultList extends ArrayList<FireSMSResult> {

    public String getAllResultsMessage() {
        StringBuilder out = new StringBuilder();
        if (size() == 1) {  // nobody cares about extra Infos if only one message was sent
            out.append(get(0).getResult().getUserText());
        } else {
            for (FireSMSResult fireSMSResult : this) {
                out.append(fireSMSResult.getReceiver()).append(" -> ").append(fireSMSResult.getResult().getUserText()).append("\n");
            }
        }
        return out.toString();
    }

    public static enum SendResult {
        NOT_YET_SET, SUCCESS, ERROR, BOTH
    }

    SendResult result = SendResult.NOT_YET_SET;


    @Override
    public boolean add(FireSMSResult object) {
        setSendResult(object.getResult());
        return super.add(object);
    }

    @Override
    public void add(int index, FireSMSResult object) {
        setSendResult(object.getResult());
        super.add(index, object);
    }

    private void setSendResult(Result sendResult) {
        if (sendResult.equals(Result.NO_ERROR)) {
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
        }

    }

    public SendResult getResult() {
        return result;
    }
}
