package de.christl.smsoip.activities;

import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.application.SMSoIPApplication;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for one receiver
 */
public class Receiver implements Serializable, Parcelable {
    private final String pickedId;
    private final String name;
    private int photoId;
    private String receiverNumber = null;
    private boolean enabled;
    private HashMap<String, String> fixedRawNumberMapping = new HashMap<String, String>();
    private HashMap<String, String> numberTypeMap = new HashMap<String, String>();

    public Receiver(String pickedId, String name, int photoId) {
        this.pickedId = pickedId;
        this.name = name;
        this.photoId = photoId;
        this.enabled = true;
    }

    public Receiver(String pickedId, String name) {
        this.pickedId = pickedId;
        this.name = name;
        this.enabled = true;
    }

    public void setNumberTypeMap(HashMap<String, String> numberTypeMap) {
        this.numberTypeMap = numberTypeMap;
    }

    public void setFixedRawNumberMapping(HashMap<String, String> fixedRawNumberMapping) {
        this.fixedRawNumberMapping = fixedRawNumberMapping;
    }

    public String getName() {
        return name;
    }

    public String getPickedId() {
        return pickedId;
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

    public void addNumber(String rawNumber, String type) {
        String fixedNumber = fixNumber(rawNumber);
        fixedRawNumberMapping.put(fixedNumber, rawNumber);
        numberTypeMap.put(fixedNumber, type);
    }

    /**
     * add a number as receivernumber and raw number
     *
     * @param rawNumber
     */
    public void addNumber(String rawNumber) {
        receiverNumber = fixNumber(rawNumber);
        fixedRawNumberMapping.put(receiverNumber, rawNumber);
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
        return fixedRawNumberMapping.get(receiverNumber);
    }

    public String getFixedNumberByRawNumber(String rawNumber) {
        for (Map.Entry<String, String> stringStringEntry : fixedRawNumberMapping.entrySet()) {
            if (stringStringEntry.getValue().equals(rawNumber)) {
                return stringStringEntry.getKey();
            }
        }
        return null;
    }

    public int getPhotoId() {
        return photoId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(pickedId);
        dest.writeString(name);
        dest.writeInt(photoId);
        dest.writeSerializable(fixedRawNumberMapping);
        dest.writeSerializable(numberTypeMap);
        dest.writeString(receiverNumber);
        dest.writeByte((byte) (enabled ? 1 : 0));

    }


    public static final Parcelable.Creator<Receiver> CREATOR = new Parcelable.Creator<Receiver>() {


        @Override
        public Receiver createFromParcel(Parcel source) {
            String pickedId = source.readString();
            String name = source.readString();
            int photoId = source.readInt();
            Receiver out = new Receiver(pickedId, name, photoId);

            HashMap<String, String> fixedRawNumberMapping = (HashMap<String, String>) source.readSerializable();
            HashMap<String, String> numberTypeMap = (HashMap<String, String>) source.readSerializable();
            out.setFixedRawNumberMapping(fixedRawNumberMapping);
            out.setNumberTypeMap(numberTypeMap);
            String receiverNumber = source.readString();
            out.setReceiverNumber(receiverNumber);
            boolean enabled = source.readByte() == 1;
            out.setEnabled(enabled);
            return out;
        }

        @Override
        public Receiver[] newArray(int size) {
            return new Receiver[size];
        }
    };

    @Override
    public String toString() {
        return "Receiver{" +
                "pickedId='" + pickedId + '\'' +
                ", name='" + name + '\'' +
                ", photoId=" + photoId +
                ", receiverNumber='" + receiverNumber + '\'' +
                ", enabled=" + enabled +
                ", fixedRawNumberMapping=" + fixedRawNumberMapping +
                ", numberTypeMap=" + numberTypeMap +
                '}';
    }

    public boolean isUnknown() {
        return pickedId.equals("-1");
    }
}
