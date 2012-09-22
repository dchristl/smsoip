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

package de.christl.smsoip.supplier.innosend;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Innosend options
 */
public class InnosendOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "Innosend";
    public static final String SENDER_PREFIX = "sender_";

    private int messageLength = 160;

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private int maxReceiverCount = 1;
    private int maxMessageCount = 1;

    private boolean accountChanged = false;

    public InnosendOptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public void createSpinner(final SendActivity sendActivity, Spinner spinner) {
        final String[] arraySpinner = getArrayByResourceId(R.array.array_spinner);
        spinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(sendActivity, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        maxReceiverCount = 1;
                        maxMessageCount = 1;
                        messageLength = 146;
                        break;
                    case 3:   //TURBO
                        messageLength = 1000;
                        maxReceiverCount = 500;
                        maxMessageCount = 1;       //TODO calculate for own
                        break;
                    default: //OTHER
                        maxMessageCount = 1;
                        messageLength = 160;
                        maxReceiverCount = 500;
                        break;

                }
                sendActivity.updateSMScounter();
                sendActivity.updateAfterReceiverCountChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //do nothing
            }
        });
        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, arraySpinner[0]));
        defaultPosition = (defaultPosition == -1) ? 0 : defaultPosition;
        spinner.setSelection(defaultPosition);
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
        ListPreference listPref = new ListPreference(context);
        String[] typeArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(typeArray);
        listPref.setEntryValues(typeArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setSummary(getTextByResourceId(R.string.text_default_type_long));
        listPref.setDefaultValue(typeArray[0]);
        out.add(listPref);
        return out;
    }


    @Override
    public int getMaxReceiverCount() {
        return maxReceiverCount;
    }

    @Override
    public int getTextMessageLength() {
        return messageLength;
    }

    @Override
    public int getMaxMessageCount() {
        return maxMessageCount;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }


    public boolean isAccountChanged() {
        return accountChanged;
    }

    public void setAccountChanged(boolean accountChanged) {
        this.accountChanged = accountChanged;
    }

//    @Override
//    public int getLengthDependentSMSCount(int textLength) {
//        if (textLength < 161) {
//            return 0;  //will be claimed usual way
//        } else if (textLength < 305) {
//            return 2;
//        } else {
//            textLength -= 304;
//            int smsCount = Math.round((textLength / 152));
//            smsCount = textLength % 152 == 0 ? smsCount : smsCount + 1;
//            return smsCount + 2;
//        }
//    }

    public String getSender() {
        return getSettings().getString(SENDER_PREFIX + getUserName(), "");
    }

    public void writeSender(String number) {
        String userName = getUserName();
        if (number != null && !number.equals("") && userName != null && !userName.equals("")) {
            SharedPreferences.Editor edit = getSettings().edit();
            edit.putString(SENDER_PREFIX + userName, number);
            edit.commit();
        }
    }

    public void resetSender() {
        String userName = getUserName();
        if (userName != null && !userName.equals("")) {
            SharedPreferences.Editor edit = getSettings().edit();
            edit.remove(SENDER_PREFIX + userName);
            edit.commit();
        }
    }
}
