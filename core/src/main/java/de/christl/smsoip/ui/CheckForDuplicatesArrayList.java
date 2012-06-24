package de.christl.smsoip.ui;

import de.christl.smsoip.activities.Receiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ArrayList that inicates if object added is already in
 */
public class CheckForDuplicatesArrayList extends ArrayList<Receiver> {

    public boolean addWithAlreadyInsertedCheck(Receiver receiver) {
        String newReceiverNumber = receiver.getReceiverNumber();
        boolean isAlreadyInserted = false;
        for (Receiver next : this) {
            if (next.getReceiverNumber().equals(newReceiverNumber)) {
                isAlreadyInserted = true;
                break;
            }
        }
        super.add(receiver);
        return isAlreadyInserted;
    }

    @Deprecated
    public List<String> getStringList() {
        List<String> out = new ArrayList<String>(this.size());
        for (Receiver receiver : this) {
            out.add(receiver.getReceiverNumber());
        }
        return out;
    }

    public Map<Integer, String> getReceiverIndexMap() {
        Map<Integer, String> out = new HashMap<Integer, String>();
        for (Receiver receiver : this) {
            out.put(indexOf(receiver), receiver.getReceiverNumber());
        }
        return out;
    }
}
