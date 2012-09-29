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
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * Innosend options
 */
public class InnosendOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "Innosend";
    public static final String SENDER_RESOLVED_PREFIX = "sender_";
    private static final String SENDER_FREE_LAST_INPUT_PREFIX = "sender_free_last_input";


    private int messageLength = 160;

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private static final String STATE_SENDER_INPUT = "sender.input";
    private static final String STATE_CHECKBOX = "sender.checkbox";
    private int maxReceiverCount = 1;
    private int maxMessageCount = 1;
    private boolean senderVisible = false;


    private EditText sender;
    private TextView header;
    private LinearLayout wrapper;
    private CheckBox freeInputCB;
    private TextView senderDisabledText;
    private String textBeforeActivityKilled;
    private Boolean checkBoxStateBeforeActivityKilled;
    private CompoundButton.OnCheckedChangeListener onCheckedChangeListener;

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
                    case 0:  //FREE
                        maxReceiverCount = 1;
                        maxMessageCount = 1;
                        messageLength = 146;
                        senderVisible = false;
                        break;
                    case 1: //SPEED
                        senderVisible = false;
                        maxMessageCount = 1;
                        messageLength = 160;
                        maxReceiverCount = 500;
                        break;
                    case 2:  //POWER
                        senderVisible = true;
                        maxMessageCount = 1;
                        messageLength = 160;
                        maxReceiverCount = 500;
                        break;
                    case 3:   //TURBO
                        messageLength = 100;       //just for message length filter (will be calculated for own)
                        maxReceiverCount = 500;
                        maxMessageCount = 10;
                        senderVisible = true;
                        break;
                    case 4: //LANDLINE
                        maxMessageCount = 1;
                        messageLength = 160;
                        maxReceiverCount = 1;
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


    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 161) {
            return 1;
        } else {
            int smsCount = Math.round((textLength / 153));
            smsCount = textLength % 153 == 0 ? smsCount : smsCount + 1;
            return smsCount;
        }
    }

    public String getSenderFromFieldOrOptions() {
        String out = getDefaultSender();
        if (freeInputCB.isChecked()) {
            out = sender.getText().toString();
        }
        return out;

    }

    private String getDefaultSender() {
        return getSettings().getString(SENDER_RESOLVED_PREFIX + getUserName(), null);
    }

    public void writeDefaultSender(String number) {
        String userName = getUserName();
        if (number != null && !number.equals("") && userName != null && !userName.equals("")) {
            SharedPreferences.Editor edit = getSettings().edit();
            edit.putString(SENDER_RESOLVED_PREFIX + userName, number);
            edit.commit();
        }
    }

    public void resetSender() {
        String userName = getUserName();
        if (userName != null && !userName.equals("")) {
            SharedPreferences.Editor edit = getSettings().edit();
            edit.remove(SENDER_RESOLVED_PREFIX + userName);
            edit.commit();
        }
    }

    @Override

    public void getFreeLayout(LinearLayout freeLayout) {
        buildLayoutsContent(freeLayout.getContext());
        freeLayout.setOrientation(LinearLayout.VERTICAL);
        if (senderVisible) {
            if (checkBoxStateBeforeActivityKilled != null) {
                if (checkBoxStateBeforeActivityKilled) {
                    sender.setText(textBeforeActivityKilled);
                    setCheckBoxWithoutListener(true);
                    sender.setVisibility(View.VISIBLE);
                    senderDisabledText.setVisibility(View.GONE);
                } else {
                    freeInputCB.setChecked(false);
                }
            } else {
                if (!freeInputCB.isChecked()) {

                    sender.setVisibility(View.GONE);
                    senderDisabledText.setVisibility(View.VISIBLE);
                }
            }
            String senderFromOptions = getDefaultSender();
            if (senderFromOptions == null) {
                senderFromOptions = getTextByResourceId(R.string.text_automatic);
            }
            senderDisabledText.setText(senderFromOptions);
            freeLayout.addView(header);
            wrapper.removeAllViews();
            wrapper.addView(freeInputCB);
            wrapper.addView(sender);
            wrapper.addView(senderDisabledText);
            freeLayout.addView(wrapper);
        } else {                          //revert everything
            freeInputCB.setChecked(false);
            senderDisabledText.setText("");
            sender.setText("");
        }
    }

    private void setCheckBoxWithoutListener(boolean b) {
        freeInputCB.setOnCheckedChangeListener(null);
        freeInputCB.setChecked(b);
        freeInputCB.setOnCheckedChangeListener(onCheckedChangeListener);
    }


    private void buildLayoutsContent(Context context) {
        if (sender == null) {
            sender = new EditText(context);
            int length = 16;
            InputFilter maxLengthFilter = new InputFilter.LengthFilter(length);//max 16 chars allowed
            InputFilter specialCharsFilter = new InputFilter() {
                @Override
                public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                    for (int i = start; i < end; i++) {
                        if (!Character.isLetterOrDigit(source.charAt(i))) {//only numbers or charcters allowed
                            return "";
                        }
                    }
                    return null;
                }
            };
            sender.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
            sender.setMinEms(length);
            sender.setMaxEms(length);
        }

        if (senderDisabledText == null) {
            senderDisabledText = new TextView(context);
            senderDisabledText.setGravity(Gravity.LEFT);
        }

        if (header == null) {
            header = new TextView(context);
            header.setText(getTextByResourceId(R.string.text_sender));
            header.setGravity(Gravity.CENTER);
            header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        }

        if (wrapper == null) {
            wrapper = new LinearLayout(context);
        }

        if (freeInputCB == null) {
            freeInputCB = new CheckBox(context);
            onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        sender.setVisibility(View.VISIBLE);
                        senderDisabledText.setVisibility(View.GONE);
                        sender.setText(getSenderFromOptions());
                        //revert them, so on the next time it will handle as usual
                        checkBoxStateBeforeActivityKilled = null;
                        textBeforeActivityKilled = null;
                    } else {
                        sender.setVisibility(View.GONE);
                        senderDisabledText.setVisibility(View.VISIBLE);
                    }

                }


            };
            freeInputCB.setOnCheckedChangeListener(onCheckedChangeListener);
        }

    }

    private String getSenderFromOptions() {
        String out = getSettings().getString(SENDER_FREE_LAST_INPUT_PREFIX + getUserName(), null);
        if (out == null) {
            out = getDefaultSender();
            if (out == null) {
                out = "";
            }
        }
        return out;
    }

    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        textBeforeActivityKilled = savedInstanceState.getString(STATE_SENDER_INPUT);
        checkBoxStateBeforeActivityKilled = savedInstanceState.getBoolean(STATE_CHECKBOX);
    }


    @Override
    public void onActivityPaused(Bundle outState) {
        outState.putString(STATE_SENDER_INPUT, sender.getText().toString());
        outState.putBoolean(STATE_CHECKBOX, freeInputCB.isChecked());
    }

    public void writeFreeInputSender() {
        if (freeInputCB != null && freeInputCB.isChecked()) {
            String toWrite = sender.getText().toString();
            String userName = getUserName();
            if (toWrite != null && !toWrite.equals("") && userName != null && !userName.equals("")) {
                SharedPreferences.Editor edit = getSettings().edit();
                edit.putString(SENDER_FREE_LAST_INPUT_PREFIX + userName, toWrite);
                edit.commit();
            }
        }
    }

}
