package de.christl.smsoip.ui;

import de.christl.smsoip.activities.Receiver;

import java.util.ArrayList;

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
}
