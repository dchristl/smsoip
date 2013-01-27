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
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Innosend options
 */
public class InnosendOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "Innosend";
    public static final String SENDER_RESOLVED_PREFIX = "sender_";
    private static final String SENDER_FREE_LAST_INPUT_PREFIX = "sender_free_last_input_";


    private int messageLength = 160;

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    public static final String PROVIDER_SHOW_SENDER = "provider.show.sender";
    private static final String STATE_SENDER_INPUT = "sender.input";
    private static final String STATE_CHECKBOX = "sender.checkbox";
    private int maxReceiverCount = 1;
    private int maxMessageCount = 1;
    private boolean senderVisible = false;


    private EditText senderText;
    private CheckBox freeInputCB;
    private TextView senderDisabledText;
    private String textBeforeActivityKilled;
    private Boolean checkBoxStateBeforeActivityKilled;
    private ViewGroup parentTableRow;

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
        listPref.setDialogTitle(getTextByResourceId(R.string.default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.default_type));
        listPref.setSummary(getTextByResourceId(R.string.default_type_long));
        listPref.setDefaultValue(typeArray[0]);
        out.add(listPref);
        CheckBoxPreference showSenderCB = new CheckBoxPreference(context);
        showSenderCB.setDefaultValue(true);
        showSenderCB.setKey(PROVIDER_SHOW_SENDER);
        showSenderCB.setTitle(getTextByResourceId(R.string.show_sender));
        showSenderCB.setSummary(getTextByResourceId(R.string.show_sender_description));
        out.add(showSenderCB);
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
            out = senderText.getText().toString();
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
        XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(R.layout.freelayout);
        LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren(freeLayout);
        buildLayoutsContent();
        boolean freeLayoutVisible = senderVisible && getSettings().getBoolean(PROVIDER_SHOW_SENDER, true);
        freeLayout.setVisibility(freeLayoutVisible ? View.VISIBLE : View.GONE);
    }


    @Override
    public void onAccountsChanged() {
        SharedPreferences.Editor edit = getSettings().edit();
        Map<Integer, AccountModel> accounts = getAccounts();
        Map<String, ?> allSettings = getSettings().getAll();
        Outer:
        for (Map.Entry<String, ?> stringEntry : allSettings.entrySet()) {
            if (stringEntry.getKey().startsWith(SENDER_RESOLVED_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_RESOLVED_PREFIX, "");
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                    if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
                        continue Outer;
                    }
                }
                edit.remove(stringEntry.getKey());
            }
            if (stringEntry.getKey().startsWith(SENDER_FREE_LAST_INPUT_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_FREE_LAST_INPUT_PREFIX, "");
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                    if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
                        continue Outer;
                    }
                }
                edit.remove(stringEntry.getKey());
            }
        }
        edit.commit();
    }

    /**
     * childs have to be resolved by its tree in structure, findViewById seems not to work on every device
     *
     * @param freeLayout
     */
    private void resolveChildren(ViewGroup freeLayout) {
        parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(1);
        freeInputCB = (CheckBox) parentTableRow.getChildAt(0);
        senderDisabledText = (TextView) parentLinearLayout.getChildAt(0);
        senderText = (EditText) parentLinearLayout.getChildAt(1);
        setInputFiltersForEditText();
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
    }

    private void setInputFiltersForEditText() {
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
        senderText.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
        senderText.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        senderText.setText(getSenderFromOptions());
    }


    private void buildLayoutsContent() {

        CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    senderText.setVisibility(View.VISIBLE);
                    senderDisabledText.setVisibility(View.GONE);
                } else {
                    senderText.setVisibility(View.GONE);
                    senderDisabledText.setVisibility(View.VISIBLE);
                }

            }


        };
        freeInputCB.setOnCheckedChangeListener(onCheckedChangeListener);

        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                freeInputCB.setChecked(!freeInputCB.isChecked());
            }
        };
        senderDisabledText.setOnClickListener(l);
        String senderFromOptions = getDefaultSender();
        if (senderFromOptions == null) {
            senderFromOptions = getTextByResourceId(R.string.automatic);
        }
        senderDisabledText.setText(senderFromOptions);
        parentTableRow.setOnClickListener(l);
        if (checkBoxStateBeforeActivityKilled != null) {
            freeInputCB.setChecked(checkBoxStateBeforeActivityKilled);

        }
        boolean checked = freeInputCB.isChecked();
        freeInputCB.setChecked(!checked); //force a recall of the listener to set correct visibility
        freeInputCB.setChecked(checked); //force a recall of the listener to set correct visibility
        if (textBeforeActivityKilled != null) {
            senderText.setText(textBeforeActivityKilled);

        }

        //reset all states to get fresh values
        checkBoxStateBeforeActivityKilled = null;
        textBeforeActivityKilled = null;
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
        saveState();
        outState.putString(STATE_SENDER_INPUT, textBeforeActivityKilled);
        outState.putBoolean(STATE_CHECKBOX, checkBoxStateBeforeActivityKilled);
    }

    public void writeFreeInputSender() {
        if (freeInputCB != null && freeInputCB.isChecked()) {
            String toWrite = senderText.getText().toString();
            String userName = getUserName();
            if (toWrite != null && !toWrite.equals("") && userName != null && !userName.equals("")) {
                SharedPreferences.Editor edit = getSettings().edit();
                edit.putString(SENDER_FREE_LAST_INPUT_PREFIX + userName, toWrite);
                edit.commit();
            }
        }
    }

    public void saveState() {
        textBeforeActivityKilled = senderText.getText().toString();
        checkBoxStateBeforeActivityKilled = freeInputCB.isChecked();
    }
}
