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

package de.christl.smsoip.activities.settings.preferences;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.activities.settings.TextModulePreferenceActivity;
import de.christl.smsoip.activities.util.TextModuleUtil;

import java.util.Map;

/**
 * DialogPreference for one text module
 */
public class TextModulePreference extends DialogPreference {


    private Context context;
    private final String key;
    private final String value;
    private Map<String, String> textModules;
    private EditText valueView;
    private EditText keyView;

    public TextModulePreference(Context context, String key, String value, Map<String, String> textModules) {
        super(context, null);
        this.context = context;
        this.key = key;
        this.value = value;
        this.textModules = textModules;
        setTitle(key);
        setSummary(value);
        setNegativeButtonText(R.string.delete);
        init();
    }

    public TextModulePreference(TextModulePreferenceActivity context, Map<String, String> textModules) {
        super(context, null);
        this.context = context;
        this.textModules = textModules;
        this.key = null;
        this.value = null;
        setNegativeButtonText(null);
        init();
    }

    private void init() {
        setDialogLayoutResource(R.layout.textmodule);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        keyView = (EditText) view.findViewById(R.id.key);
        InputFilter specialCharsFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i))) {//only numbers or characters allowed
                        return "";
                    }
                }
                return null;
            }
        };
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(15);
        keyView.setFilters(new InputFilter[]{specialCharsFilter, maxLengthFilter});
        keyView.setText(key);
        valueView = (EditText) view.findViewById(R.id.value);
        float fontSize = PreferenceManager.getDefaultSharedPreferences(getContext()).getFloat(SettingsConst.GLOBAL_FONT_SIZE_FACTOR, 1.0f) * 15;
        valueView.setTextSize(fontSize);
        valueView.setText(value);
    }


    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == DialogInterface.BUTTON_NEGATIVE) {
            TextModuleUtil.removeKey(key);
            callChangeListener(null);
        }

    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);
        keyView.requestFocus();
        Button pos = ((AlertDialog) getDialog()).getButton(AlertDialog.BUTTON_POSITIVE);
        pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newKey = keyView.getText().toString();
                if (newKey.length() < 2) {
                    Toast toast = Toast.makeText(getContext(), R.string.key_too_short, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (!newKey.equals(key) && textModules.containsKey(newKey)) {
                    Toast toast = Toast.makeText(getContext(), R.string.key_already_exist, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                if (valueView.getText().toString().trim().length() == 0) {
                    Toast toast = Toast.makeText(getContext(), R.string.empty_value, Toast.LENGTH_SHORT);
                    toast.show();
                    return;
                }
                String newValue = valueView.getText().toString();
                TextModuleUtil.updateValue(key, newKey, newValue);
                callChangeListener(null);
                if (key != null) {//new one does not need update
                    setSummary(newValue);
                    setTitle(newKey);
                }
                getDialog().dismiss();
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (key == null) {
            View inflate = LayoutInflater.from(context).inflate(R.layout.lastlistem, null);
            TextView viewById = (TextView) inflate.findViewById(R.id.addText);
            viewById.setText(R.string.add_module);
            ColorStateList defaultColor = new TextView(getContext()).getTextColors();
            viewById.setTextColor(defaultColor);
            return inflate;
        } else {
            return super.onCreateView(parent);
        }
    }

}
