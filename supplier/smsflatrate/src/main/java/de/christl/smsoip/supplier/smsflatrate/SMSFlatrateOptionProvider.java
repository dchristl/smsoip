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
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

public class SMSFlatrateOptionProvider extends OptionProvider {

    public static final String PROVIDER_NAME = "SMSFlatrate";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private static final String SENDER_LAST_INPUT_PREFIX = "sender_last_input_";
    private static final String STATE_SENDER_INPUT = "state_sender_input";
    private EditText senderText;
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
        return 49;
    }

    @Override
    public String getUserLabelText() {
        return getTextByResourceId(R.string.appkey_description);
    }

    @Override
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
    public void getFreeLayout(LinearLayout freeLayout) {
        if (senderVisible) {
            XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(R.layout.freelayout);
            View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
            resolveChildren((ViewGroup) freeLayoutView);
        }
    }

    private void resolveChildren(ViewGroup freeLayout) {
        ViewGroup parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
        senderText = (EditText) parentTableRow.getChildAt(0);
        setInputFiltersForEditText();
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
                    if (dest.length() > 10) {
                        String spanContent = dest.toString();
                        boolean onlyDigits = true;
                        for (int z = 0; z < spanContent.length(); z++) {
                            char c = spanContent.charAt(i);
                            if (!Character.isDigit(c)) {
                                onlyDigits = false;
                                break;
                            }
                        }
                        if (!onlyDigits || !Character.isDigit(source.charAt(i))) {
                            return "";
                        }

                    }
                }
                return null;
            }
        };
        senderText.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
        senderText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        senderText.setText(getSenderFromOptions());
    }

    @Override
    public void onAccountsChanged() {
        super.onAccountsChanged();
        SharedPreferences.Editor edit = getSettings().edit();
        Map<Integer, AccountModel> accounts = getAccounts();
        Map<String, ?> allSettings = getSettings().getAll();
        Outer:
        for (Map.Entry<String, ?> stringEntry : allSettings.entrySet()) {
            if (stringEntry.getKey().startsWith(SENDER_LAST_INPUT_PREFIX)) {
                String label = stringEntry.getKey().replaceAll(SENDER_LAST_INPUT_PREFIX, "");
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                    if (label.equals(integerAccountModelEntry.getValue().getUserName())) {
                        continue Outer;
                    }
                }
                edit.remove(stringEntry.getKey());
            }
        }
        edit.commit();
    }

    @Override
    public void onActivityPaused(Bundle outState) {
        if (senderText != null && senderText.getVisibility() == View.VISIBLE) {
            String textBeforeActivityKilled = senderText.getText().toString();
            outState.putString(STATE_SENDER_INPUT, textBeforeActivityKilled);
        }
    }

    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        if (senderText != null && senderText.getVisibility() == View.VISIBLE) {
            String textBeforeActivityKilled = savedInstanceState.getString(STATE_SENDER_INPUT);
            senderText.setText(textBeforeActivityKilled);
        }
    }

    public void saveLastSender() {
        if (senderText != null && senderText.getVisibility() == View.VISIBLE) {
            String toWrite = senderText.getText().toString();
            String userName = getUserName();
            if (!toWrite.equals("") && userName != null && !userName.equals("")) {
                SharedPreferences.Editor edit = getSettings().edit();
                edit.putString(SENDER_LAST_INPUT_PREFIX + userName, toWrite);
                edit.commit();
            }
        }
    }

    private String getSenderFromOptions() {
        String out = getSettings().getString(SENDER_LAST_INPUT_PREFIX + getUserName(), null);
        if (out == null) {
            out = "";
        }
        return out;
    }

    public String getSender() {
        String out = "";
        if (senderText != null && senderText.getVisibility() == View.VISIBLE) {
            out = senderText.getText().toString();
        }
        return out;
    }
}
