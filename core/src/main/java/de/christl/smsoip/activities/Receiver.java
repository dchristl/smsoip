package de.christl.smsoip.activities;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.application.SMSoIPApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for one receiver
 */
public class Receiver {
    private final String pickedId;
    private final String name;
    private String receiverNumber = null;
    private boolean enabled;
    private String rawNumber;
    private Map<String, String> numberTypeMap = new HashMap<String, String>();

    public Receiver(String pickedId, String name) {
        this.pickedId = pickedId;
        this.name = name;
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public void setReceiverNumber(String receiverNumber) {
        if (!numberTypeMap.containsKey(receiverNumber)) {
            throw new IllegalArgumentException(); //for insurance
        }
        this.receiverNumber = receiverNumber;
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

    public void addNumber(String rawNumber, String type) {
        numberTypeMap.put(fixNumber(rawNumber), type);
    }

    public Map<String, String> getNumberTypeMap() {
        return numberTypeMap;
    }

    private String fixNumber(String rawNumber) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp().getApplicationContext());
        if (!rawNumber.startsWith("+") && !rawNumber.startsWith("00")) {   //area code not already added
            rawNumber = rawNumber.replaceFirst("^0", "");        //replace leading zero
            String areaCode = settings.getString(GlobalPreferences.GLOBAL_AREA_CODE, "49");
            String prefix = "00" + areaCode;
            rawNumber = prefix + rawNumber;
        } else {
            rawNumber = rawNumber.replaceFirst("\\+", "00");  //replace plus if there
        }
        return rawNumber.replaceAll("[^0-9]", "");   //clean up not numbervalues
    }


    public String getRawNumber() {
        return pickedId.equals("-1") ? receiverNumber : rawNumber;
    }
}
