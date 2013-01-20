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

package de.christl.smsoip.supplier.smsde;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

public class SMSDeOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "SMS.de";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    public static final String PROVIDER_SHOW_FLASH = "provider.show.flash";
    private int messageLength = 142;
    private int maxReceivers = 1;
    private boolean senderVisible = false;
    private CheckBox cb;
    private TextView textView;
    private Boolean checkBoxStateBeforeActivityKilled;
    private static final String STATE_CHECKBOX = "state.checkbox";
    private boolean reset = false;

    public SMSDeOptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public int getMaxReceiverCount() {
        return maxReceivers;
    }

    @Override
    public int getTextMessageLength() {
        return messageLength;
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
                    case 0:        //free
                        messageLength = 151;
                        maxReceivers = 1;
                        senderVisible = false;
                        break;
                    case 1:    //power sms 160
                    case 2:
                        messageLength = 160;
                        maxReceivers = 5;
                        senderVisible = true;
                        break;
                    default:                  //power sms 300
                        messageLength = 300;
                        maxReceivers = 5;
                        senderVisible = true;
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
        String[] prefArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(prefArray);
        listPref.setEntryValues(prefArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.default_type));
        listPref.setSummary(getTextByResourceId(R.string.default_type_long));
        listPref.setDefaultValue(prefArray[0]);
        out.add(listPref);
        CheckBoxPreference showSenderCB = new CheckBoxPreference(context);
        showSenderCB.setDefaultValue(true);
        showSenderCB.setKey(PROVIDER_SHOW_FLASH);
        showSenderCB.setTitle(getTextByResourceId(R.string.show_flash));
        showSenderCB.setSummary(getTextByResourceId(R.string.show_flash_description));
        out.add(showSenderCB);
        return out;
    }

    @Override
    public int getMaxMessageCount() {
        return 1;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        freeLayout.setOrientation(LinearLayout.HORIZONTAL);
        buildFreeLayoutContent(freeLayout.getContext());
        if (senderVisible && getSettings().getBoolean(PROVIDER_SHOW_FLASH, true)) {
            if (checkBoxStateBeforeActivityKilled != null) {
                cb.setChecked(checkBoxStateBeforeActivityKilled);
            }
            freeLayout.addView(cb);
            freeLayout.addView(textView);
        } else {
            cb.setChecked(false);
        }
        if (reset) {
            reset = false;
            cb.setChecked(false);
        }
    }

    public boolean isFlash() {
        return cb.isChecked();
    }

    private void buildFreeLayoutContent(Context context) {
        TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(0, 20, 0, 20);
        if (cb == null) {
            cb = new CheckBox(context);
            cb.setLayoutParams(layoutParams);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    checkBoxStateBeforeActivityKilled = null; //revert the setting, so next time it will work as usual
                }
            });
        }

        if (textView == null) {
            textView = new TextView(context);
            textView.setText(getTextByResourceId(R.string.send_as_flash));
            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    cb.setChecked(!cb.isChecked());
                }
            });
            textView.setLayoutParams(layoutParams);
        }
        ViewGroup parent = (ViewGroup) cb.getParent();
        if (parent != null) {
            parent.removeView(cb);
        }
        parent = (ViewGroup) textView.getParent();
        if (parent != null) {
            parent.removeView(textView);
        }
    }

    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        checkBoxStateBeforeActivityKilled = savedInstanceState.getBoolean(STATE_CHECKBOX);
    }

    @Override
    public void onActivityPaused(Bundle outState) {
        outState.putBoolean(STATE_CHECKBOX, cb.isChecked());
    }

    public void reset() {
        checkBoxStateBeforeActivityKilled = null;
        reset = true;
    }
}
