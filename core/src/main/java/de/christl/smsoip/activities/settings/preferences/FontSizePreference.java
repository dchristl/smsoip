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
import android.content.SharedPreferences;
import android.preference.DialogPreference;
import android.view.View;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;

/**
 * Preference for changing the font size in the input field
 */
public class FontSizePreference extends DialogPreference {

    private float currentValue;
    private TextView sampleView;

    public FontSizePreference(Context context) {
        super(context, null);
        init();
    }

    private void init() {
        setDefaultValue("1.0");
        setKey(SettingsConst.GLOBAL_FONT_SIZE_FACTOR);
        setDialogLayoutResource(R.layout.fontsizepreference);
        setTitle(R.string.font_size);
        setSummary(R.string.font_size_description);
        setDialogTitle(R.string.font_size);
        setDialogMessage(R.string.font_size_dialog_description);
        setNegativeButtonText(null);
        setPersistent(false);
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        currentValue = getSharedPreferences().getFloat(SettingsConst.GLOBAL_FONT_SIZE_FACTOR, 1.0f);
        sampleView = (TextView) view.findViewById(R.id.sample);
        sampleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentValue = 1.0f;
                updateSample();
            }
        });
        updateSample();

        View increaseButton = view.findViewById(R.id.increase);
        increaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentValue <= 3.0f) {
                    currentValue += 0.1;
                    updateSample();
                }
            }
        });
        View decreaseButton = view.findViewById(R.id.decrease);
        decreaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentValue >= 0.5f) {
                    currentValue -= 0.1;
                    updateSample();
                }
            }
        });


    }

    private void updateSample() {
        sampleView.setTextSize(currentValue * 15);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            SharedPreferences.Editor editor = getEditor();
            editor.putFloat(SettingsConst.GLOBAL_FONT_SIZE_FACTOR, currentValue);
            editor.commit();

        }
    }
}
