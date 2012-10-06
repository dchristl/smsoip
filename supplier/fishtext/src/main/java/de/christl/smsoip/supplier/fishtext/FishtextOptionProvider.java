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

package de.christl.smsoip.supplier.fishtext;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;


public class FishtextOptionProvider extends OptionProvider {
    private static final String PROVIDER_NAME = "Fishtext";

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }


    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }


    @Override
    public void createSpinner(SendActivity sendActivity, Spinner spinner) {
        final String[] arraySpinner = getArrayByResourceId(R.array.array_spinner);
        spinner.setVisibility(View.VISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(sendActivity, android.R.layout.simple_spinner_item, arraySpinner);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

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
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength <= 160) {
            return 1;
        }
        int smsCount = Math.round((textLength / 153));
        smsCount = textLength % 153 == 0 ? smsCount : smsCount + 1;
        return smsCount;
    }

    @Override
    public int getTextMessageLength() {
        return 153;
    }

    @Override
    public int getMaxMessageCount() {
        return 3;
    }
}
