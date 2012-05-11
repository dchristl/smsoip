package de.christl.smsoip.activities;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.application.SMSoIPApplication;

/**
 * Class for one receiver
 */
public class Receiver {
    private final String pickedId;
    private final String name;
    private final String receiverNumber;
    private boolean enabled;
    private String rawNumber;

    public Receiver(String pickedId, String name, String rawNumber) {
        //To change body of created methods use File | Settings | File Templates.
        this.pickedId = pickedId;
        this.name = name;
        this.rawNumber = rawNumber;
        this.receiverNumber = fixNumber(rawNumber);
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

    public String getPickedId() {
        return pickedId;
    }

    private String fixNumber(String rawNumber) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp().getApplicationContext());
        String prefix = "";
        if (!rawNumber.startsWith("+") && !rawNumber.startsWith("00")) {
            String areaCode = settings.getString(GlobalPreferences.GLOBAL_AREA_CODE, "49");
            prefix = "00" + areaCode;
        }
        rawNumber = rawNumber.replaceFirst("^0", "");
        rawNumber = rawNumber.replaceFirst("\\+", "00");
        rawNumber = rawNumber.replaceAll("[^0-9]", "");
        return prefix + rawNumber;
    }

    public String getRawNumber() {
        return pickedId.equals("-1") ? receiverNumber : rawNumber;
    }
}
