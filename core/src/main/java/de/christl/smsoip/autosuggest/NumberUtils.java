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

package de.christl.smsoip.autosuggest;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.AutoCompleteTextView;
import android.widget.MultiAutoCompleteTextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.activities.settings.GlobalPreferences;
import de.christl.smsoip.application.SMSoIPApplication;
import de.christl.smsoip.database.DatabaseHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * little helper for number mapping
 */
public abstract class NumberUtils {

    private static final Pattern NAME_NUMBER_INPUT = Pattern.compile(".+ \\(00[0-9]+\\)");
    private static final Pattern NUMBER_INPUT = Pattern.compile("00[0-9]+");

    public static String fixNumber(String rawNumber) {
        String out = rawNumber;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(SMSoIPApplication.getApp().getApplicationContext());
        if (!out.startsWith("+") && !out.startsWith("00")) {   //area code not already added
            out = out.replaceFirst("^0", "");        //replace leading zero
            String areaCode = settings.getString(GlobalPreferences.GLOBAL_AREA_CODE, "49");
            String prefix = "00" + areaCode;
            out = prefix + out;
        } else {
            out = out.replaceFirst("\\+", "00");  //replace plus if there
        }
        return out.replaceAll("[^0-9]", "");   //clean up not numbervalues
    }

    public static AutoCompleteTextView.Validator getNumberValidator() {
        return new NameNumberValidator();
    }

    public static String getValidatedString(String text, MultiAutoCompleteTextView.Tokenizer tokenizer, AutoCompleteTextView.Validator v) {
        String[] split = text.split(",");
        StringBuilder builder = new StringBuilder();
        for (String token : split) {
            CharSequence toAppend = token.trim();
            if (toAppend.length() == 0) {
                continue;
            }
            if (!v.isValid(token)) {
                toAppend = v.fixText(toAppend);
            }
            builder.append(tokenizer.terminateToken(toAppend));

        }
        return builder.toString();
    }

    public static String getValidatedString(String text) {
        return getValidatedString(text, new MultiAutoCompleteTextView.CommaTokenizer(), new NameNumberValidator());
    }

    public static boolean isCorrectNumberInInternationalStyle(CharSequence text) {
        Matcher matcher = NUMBER_INPUT.matcher(text);
        return matcher.matches();
    }

    public static boolean isCorrectNameNumber(CharSequence text) {
        Matcher matcher = NAME_NUMBER_INPUT.matcher(text);
        return matcher.matches();
    }

    private static class NameNumberValidator implements AutoCompleteTextView.Validator {
        private final Pattern JUST_NUMBERS_OR_STARTED_WITH_PLUS = Pattern.compile("\\+?[0-9]+");

        @Override
        public boolean isValid(CharSequence text) {
            boolean matches = isCorrectNameNumber(text);
            if (!matches) {
                matches = isCorrectNumberInInternationalStyle(text);
            }
            return matches;
        }


        @Override
        public CharSequence fixText(CharSequence invalidText) {
            Matcher matcher = JUST_NUMBERS_OR_STARTED_WITH_PLUS.matcher(invalidText);
            boolean matches = matcher.matches();
            if (matches) {
                String fixedNumber = NumberUtils.fixNumber(invalidText.toString());
                Receiver receiver = DatabaseHandler.findContactByNumber(fixedNumber, SMSoIPApplication.getApp().getBaseContext());
                String textUnknown = SMSoIPApplication.getApp().getString(R.string.text_unknown);
                NameNumberEntry tmpEntry = new NameNumberEntry(textUnknown, fixedNumber, textUnknown);
                if (receiver != null) {
                    tmpEntry = new NameNumberEntry(receiver.getName(), receiver.getReceiverNumber(), receiver.getNumberType());
                }
                return tmpEntry.getFieldRepresantation();
            } else {
                return "";
            }
        }
    }
}
