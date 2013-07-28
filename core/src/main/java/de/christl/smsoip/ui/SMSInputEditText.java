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

package de.christl.smsoip.ui;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Map;

import de.christl.smsoip.R;
import de.christl.smsoip.activities.settings.SettingsConst;
import de.christl.smsoip.activities.util.TextModuleUtil;

/**
 * Edittext handling the sms text
 */
public class SMSInputEditText extends EditText {
    private Map<String, String> textModules;

    public SMSInputEditText(Context context) {
        super(context);
        init();
    }

    public SMSInputEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SMSInputEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /**
     * initializethe edittext
     */
    private void init() {
        float fontSize = PreferenceManager.getDefaultSharedPreferences(getContext()).getFloat(SettingsConst.GLOBAL_FONT_SIZE_FACTOR, 1.0f) * 15;
        ((TextView) findViewById(R.id.textInput)).setTextSize(fontSize);
        refreshTextModules();
    }

    /**
     * replace text by text modules
     */
    public void processReplacement() {
        int cursor = getSelectionStart();
        String input = getText().toString();
        String lastKey = input.substring(cursor - 1 < 0 ? 0 : cursor - 1, cursor);
        if (lastKey.equals(" ") || lastKey.equals("\n")) {
            String stringBefore = input.substring(0, cursor - 1);
            int lastSpace = stringBefore.lastIndexOf(" ");
            int lastNL = stringBefore.lastIndexOf("\n");
            int lastDivider = Math.max(lastNL, lastSpace);
            if (lastDivider == -1) {  //if the first word should replaced (normally does not start with space)
                lastDivider = 0;
            } else {
                lastDivider += 1;
            }
            String wordToCheck = input.substring(lastDivider, cursor - 1);

            if (textModules.containsKey(wordToCheck)) {
                wordToCheck = textModules.get(wordToCheck);
                String newText = input.substring(0, lastDivider) + wordToCheck + input.substring(cursor);
                if (lastKey.equals(" ")) {
                    newText += " ";
                }
                setText(newText);
                int newSelection = lastDivider + wordToCheck.length();
                setSelection(getText().length() < newSelection ? getText().length() : newSelection);
            }
        }
    }

    /**
     * reload the list of the text modules
     */
    public void refreshTextModules() {
        textModules = TextModuleUtil.getTextModules();
    }

    public Map<String, String> getTextModules() {
        return textModules;
    }

    /**
     * insert the text from "outside" like share
     *
     * @param textToInsert
     */
    public void insertText(String textToInsert) {
        int cursor = hasFocus() ? getSelectionStart() : getText().length();
        String input = getText().toString();
        String stringBefore = cursor == 0 ? "" : input.substring(0, cursor);
        String newText = stringBefore + textToInsert + input.substring(cursor);
        setText(newText);
        requestFocus();
        int newSelection = (stringBefore + textToInsert).length();
        setSelection(getText().length() < newSelection ? getText().length() : newSelection);

    }
}
