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

package de.christl.smsoip.supplier.sms77;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SMS77OptionProvider extends OptionProvider {
    private static final String PROVIDER_NAME = "SMS77";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private EditText senderText;
    private boolean senderVisible = false;

    public SMS77OptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        XmlResourceParser freeLayoutRes = getLayoutResourceByResourceId(R.layout.freelayout);
        LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren(freeLayout);
//        buildLayoutsContent();
        freeLayout.setVisibility(senderVisible ? View.VISIBLE : View.GONE);
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
        senderText.setText(getSenderFromOptions());
    }

    private CharSequence getSenderFromOptions() {
        return "";
    }

    /**
     * childs have to be resolved by its tree in structure, findViewById seems not to work on every device
     *
     * @param freeLayout
     */
    private void resolveChildren(ViewGroup freeLayout) {
        ViewGroup parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        senderText = (EditText) parentTableRow.getChildAt(0);
        setInputFiltersForEditText();
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
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
                    case 0:  //BASIC
                        senderVisible = false;
                        break;
                    case 1: //QUALITY
                        senderVisible = true;
                        break;
                    case 2:  //LANDLINE
                        senderVisible = false;
                        break;
                    case 3:   //FLASH
                        senderVisible = false;
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
        return out;
    }
}
