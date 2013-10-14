/*
 * Copyright (c) Danny Christl 2013.
 *      This file is part of SMSoIP.
 *
 *      SMSoIP is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      (at your option) any later version.
 *
 *      SMSoIP is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with SMSoIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.christl.smsoip.supplier.arcor;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

/**
 *
 */
public class ArcorOptionProvider extends OptionProvider {

    private static final String providerName = "Arcor";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    public static final String PROVIDER_SAVE_IN_SENT = "provider.saveInSent";

//    private int textMessageLength = 143;

    @Override
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
    }

    @Override
    public String getProviderName() {
        return providerName;
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
//                switch (position) {
//                    case 0:
//                        textMessageLength = 140;
//                        break;
//                    case 1:
//                        textMessageLength = 160;
//                        break;
//                }
                sendActivity.updateAfterReceiverCountChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, arraySpinner[0]));
        defaultPosition = (defaultPosition == -1) ? 0 : defaultPosition;
        spinner.setSelection(defaultPosition);
    }


    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
        final ListPreference listPref = new ListPreference(context);
        String[] typeArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(typeArray);
        listPref.setEntryValues(typeArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.default_type));
        listPref.setSummary(getTextByResourceId(R.string.default_type_long));
        listPref.setDefaultValue(typeArray[0]);
        out.add(listPref);
        CheckBoxPreference saveInSent = new CheckBoxPreference(context);
        saveInSent.setKey(PROVIDER_SAVE_IN_SENT);
        saveInSent.setTitle(getTextByResourceId(R.string.save_in_sent));
        saveInSent.setSummary(getTextByResourceId(R.string.save_in_sent_long));
        out.add(saveInSent);
        return out;
    }

    @Override
    public int getMaxReceiverCount() {
        return 10;
    }

    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 144) {
            return 1;
        } else if (textLength < 290) {
            return 2;
        } else {
            textLength -= 289;
            int smsCount = Math.round((textLength / 153));
            smsCount = textLength % 153 == 0 ? smsCount : smsCount + 1;
            return smsCount + 2;
        }
    }


    @Override
    public int getMaxMessageCount() {
        return 3;//just for marking red
    }

    @Override
    public int getTextMessageLength() {
        return 148;//just for marking red
    }

    @Override
    public int getMinimalCoreVersion() {
        return 50;
    }
}
