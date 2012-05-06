package de.christl.smsoip.activities;

/**
 * Created with IntelliJ IDEA.
 * User: Danny
 * Date: 06.05.12
 * Time: 14:55
 * To change this template use File | Settings | File Templates.
 */
public class Receiver {
    private final String pickedId;
    private final String name;
    private final String receiverNumber;
    private boolean enabled;

    public Receiver(String pickedId, String name, String receiverNumber) {
        //To change body of created methods use File | Settings | File Templates.
        this.pickedId = pickedId;
        this.name = name;
        this.receiverNumber = receiverNumber;
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public String getReceiverNumber() {
        return receiverNumber;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
