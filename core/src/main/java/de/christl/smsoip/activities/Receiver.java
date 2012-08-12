/*
 * Copyright (c) Danny Christl 2012.
 *     This file is part of SMSoIP.
 *
 *     SMSoIP is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SMSoIP is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.activities;

import android.os.Parcel;
import android.os.Parcelable;
import de.christl.smsoip.autosuggest.NumberUtils;

import java.io.Serializable;

/**
 * Class for one receiver
 */
public class Receiver implements Serializable, Parcelable {
    private final String name;
    private int photoId;
    private String receiverNumber;
    private String numberType;
    private String rawNumber;
    private boolean enabled;


    public Receiver(String name, int photoId) {
        this.name = name;
        this.photoId = photoId;
        this.enabled = true;
    }

    public Receiver(String name) {
        this.name = name;
        this.enabled = true;
    }


    public String getName() {
        return name;
    }

    public String getRawNumber() {
        return rawNumber;
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


    public String getNumberType() {
        return numberType;
    }

    public void setReceiverNumber(String receiverNumber, String numberType) {
        this.receiverNumber = receiverNumber;
        this.numberType = numberType;
        this.rawNumber = receiverNumber;
    }

    public void setRawNumber(String rawNumber, String numberType) {
        this.rawNumber = rawNumber;
        this.numberType = numberType;
        receiverNumber = NumberUtils.fixNumber(rawNumber);
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

        dest.writeString(name);
        dest.writeInt(photoId);
        dest.writeString(numberType);
        dest.writeString(rawNumber);
        dest.writeByte((byte) (enabled ? 1 : 0));

    }


    public static final Parcelable.Creator<Receiver> CREATOR = new Parcelable.Creator<Receiver>() {


        @Override
        public Receiver createFromParcel(Parcel source) {
            String name = source.readString();
            int photoId = source.readInt();
            String numberType = source.readString();
            String rawNumber = source.readString();
            byte enabled = source.readByte();
            Receiver out = new Receiver(name, photoId);
            out.setRawNumber(rawNumber, numberType);
            out.setEnabled(enabled == 1);
            return out;
        }

        @Override
        public Receiver[] newArray(int size) {
            return new Receiver[size];
        }
    };


}
