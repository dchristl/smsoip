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

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.activities.settings.TextModulePreferenceActivity;
import de.christl.smsoip.activities.util.TextModuleUtil;

/**
 * DialogPreference for one text module
 */
public class TextModulePreference extends DialogPreference {


    private Context context;
    private final String key;
    private final String value;
    private EditText valueView;
    private EditText keyView;

    public TextModulePreference(Context context, String key, String value) {
        super(context, null);
        this.context = context;
        this.key = key;
        this.value = value;
        setTitle(key);
        setSummary(value);
        setNegativeButtonText(R.string.text_delete);
        init();
    }

    public TextModulePreference(TextModulePreferenceActivity context) {
        super(context, null);
        this.context = context;
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
                    if (!Character.isLetterOrDigit(source.charAt(i))) {//only numbers or charcters allowed
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
        if (which == DialogInterface.BUTTON_POSITIVE) {
            String newKey = keyView.getText().toString();
            String newValue = valueView.getText().toString();
            TextModuleUtil.updateValue(key, newKey, newValue);
            callChangeListener(null);
            if (key != null) {//new one does not need update
                setSummary(newValue);
                setTitle(newKey);
            }
        } else if (which == DialogInterface.BUTTON_NEGATIVE) {
            TextModuleUtil.removeKey(key);
            callChangeListener(null);
        }
        super.onClick(dialog, which);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        if (key == null) {
            View inflate = LayoutInflater.from(context).inflate(R.layout.lastlistem, null);
            ((TextView) inflate.findViewById(R.id.addText)).setText(R.string.text_add_text_module);
            return inflate;
        } else {
            return super.onCreateView(parent);
        }
    }

}
