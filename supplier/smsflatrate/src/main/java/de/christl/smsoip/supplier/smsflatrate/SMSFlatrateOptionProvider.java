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

package de.christl.smsoip.supplier.smsflatrate;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
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

public class SMSFlatrateOptionProvider extends OptionProvider {

    public static final String PROVIDER_NAME = "SMSFlatrate";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private boolean senderVisible = true;
    private int maxMessageCount = 7;

    public SMSFlatrateOptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public int getMaxMessageCount() {
        return maxMessageCount;
    }

    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 161) {
            return 1;
        } else if (textLength < 307) {
            return 2;
        } else {
            textLength -= 306;
            int smsCount = Math.round((textLength / 153));
            smsCount = textLength % 153 == 0 ? smsCount : smsCount + 1;
            return smsCount + 2;
        }
    }


    @Override
    public int getMinimalCoreVersion() {
        return 46;
    }

    //    @Override
    public String getUserLabelText() {
        return getTextByResourceId(R.string.appkey_description);
    }

    //    @Override
    public String getPasswordLabelText() {
        return getTextByResourceId(R.string.appkey);
    }

    @Override
    public int getMaxReceiverCount() {
        return 50;
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
                    case 0:  //SmartSMS
                        maxMessageCount = 7;
                        senderVisible = false;
                        break;
                    case 1: //Normal without SI
                        senderVisible = false;
                        maxMessageCount = 1;
                        break;
                    case 2:  //Normal with SI
                        senderVisible = true;
                        maxMessageCount = 7;
                        break;
                    case 3:   //HIGH Quality
                        maxMessageCount = 7;
                        senderVisible = true;
                        break;
                    default: //OTHER

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
    public List<Preference> getAdditionalPreferences(final Context context) {
        List<Preference> out = new ArrayList<Preference>();

        DialogPreference helpPreference = new EditTextPreference(context) {
            @Override
            protected void onClick() {
                //disable the default behaviour (no default dialog will be shown
            }
        };
        helpPreference.setTitle(getTextByResourceId(R.string.help));
        helpPreference.setSummary(getTextByResourceId(R.string.help_description));
        helpPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(getTextByResourceId(R.string.help));
                builder.setMessage(getTextByResourceId(R.string.helpText));
                builder.setPositiveButton("OK", null);
                builder.show();
                return true;
            }
        });
        out.add(helpPreference);
        ListPreference listPref = new ListPreference(context);
        String[] typeArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(typeArray);
        listPref.setEntryValues(typeArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.default_type));
        listPref.setSummary(getTextByResourceId(R.string.default_type_long));
        listPref.setDefaultValue(typeArray[0]);
        out.add(listPref);
//        CheckBoxPreference showSenderCB = new CheckBoxPreference(context);
//        showSenderCB.setDefaultValue(true);
//        showSenderCB.setKey(PROVIDER_SHOW_SENDER);
//        showSenderCB.setTitle(getTextByResourceId(R.string.show_sender));
//        showSenderCB.setSummary(getTextByResourceId(R.string.show_sender_description));
//        out.add(showSenderCB);
        return out;
    }
}
