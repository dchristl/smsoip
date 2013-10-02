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

package de.christl.smsoip.supplier.smsglobal;

import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Map;

import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

/**
 *
 */
public class SMSGlobalOptionProvider extends OptionProvider {
    private static final String providerName = "SMSGlobal";
    private static final String SENDER_LAST_INPUT_PREFIX = "sender_last_input_";
    private static final String STATE_SENDER_INPUT = "state_sender_input";
    private EditText senderText;

    @Override
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public int getMinimalCoreVersion() {
        return 49;
    }

    @Override
    public String getUserLabelText() {
        return getTextByResourceId(R.string.http_api_user);
    }

    @Override
    public String getPasswordLabelText() {
        return getTextByResourceId(R.string.http_api_pass);
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(R.layout.freelayout);
        View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren((ViewGroup) freeLayoutView);
    }

    private void resolveChildren(ViewGroup freeLayout) {
        ViewGroup parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
        senderText = (EditText) parentTableRow.getChildAt(0);
        setInputFiltersForEditText();
    }

    private void setInputFiltersForEditText() {
        int length = 20;
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(length);//max 20 chars allowed
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

    private String getSenderFromOptions() {
        String out = getSettings().getString(SENDER_LAST_INPUT_PREFIX + getUserName(), null);
        if (out == null) {
            out = "";
        }
        return out;
    }

    public String getSender() {
        String out = "";
        if (senderText != null) {
            out = senderText.getText().toString();
        }
        return out;
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
        if (senderText != null) {
            String textBeforeActivityKilled = senderText.getText().toString();
            outState.putString(STATE_SENDER_INPUT, textBeforeActivityKilled);
        }
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
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        if (senderText != null) {
            String textBeforeActivityKilled = savedInstanceState.getString(STATE_SENDER_INPUT);
            senderText.setText(textBeforeActivityKilled);
        }
    }

    public void saveLastSender() {
        if (senderText != null) {
            String toWrite = senderText.getText().toString();
            String userName = getUserName();
            if (!toWrite.equals("") && userName != null && !userName.equals("")) {
                SharedPreferences.Editor edit = getSettings().edit();
                edit.putString(SENDER_LAST_INPUT_PREFIX + userName, toWrite);
                edit.commit();
            }
        }
    }
}
