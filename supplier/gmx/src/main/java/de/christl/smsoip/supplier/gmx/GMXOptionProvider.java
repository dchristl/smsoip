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

package de.christl.smsoip.supplier.gmx;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
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

import java.util.*;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "GMX";
    public static final String PROVIDER_CHECKNOFREESMSAVAILABLE = "provider.checknofreesmsavailable";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";

    private HashMap<Integer, String> adapterItems;
    private static final String SENDER_PREFIX = "sender_";
    private boolean showFreeText;
    private ViewGroup parentTableRow;
    private CheckBox sourceIDCB;
    private ImageButton refreshButton;
    private ProgressBar progressBar;
    private TextView infoTextField;
    private Spinner senderSpinner;
    private RefreshSenderTask refreshSenderTask;

    private Boolean checkBoxState;
    private Integer spinnerItem;
    private EditText senderFreeText;

    private static final String STATE_CHECKBOX = "gmx.state.checkbox";
    private static final String STATE_SPINNER = "gmx.state.checkbox";
    private static final String STATE_FREE_TEXT = "gmx.state.freetetx";
    private String freeTextContent;

    public GMXOptionProvider() {
        super(PROVIDER_NAME);
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
        CheckBoxPreference checkNoFreeSMSAvailable = new CheckBoxPreference(context);
        checkNoFreeSMSAvailable.setKey(PROVIDER_CHECKNOFREESMSAVAILABLE);
        checkNoFreeSMSAvailable.setTitle(getTextByResourceId(R.string.check_no_free_available));
        checkNoFreeSMSAvailable.setSummary(getTextByResourceId(R.string.check_no_free_available_long));
        out.add(checkNoFreeSMSAvailable);
        return out;
    }

    @Override
    public int getMaxMessageCount() {
        return 5;
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
                    case 0:
                        showFreeText = false;
                        break;
                    case 1:
                        showFreeText = true;
                        break;
                }
                sendActivity.updateAfterReceiverCountChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        int defaultPosition = ((ArrayAdapter<String>) spinner.getAdapter()).getPosition(getSettings().getString(PROVIDER_DEFAULT_TYPE, arraySpinner[0]));
        defaultPosition = (defaultPosition == -1) ? 0 : defaultPosition;
        spinner.setSelection(defaultPosition);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 161) {
            return 0;  //will be claimed usual way
        } else if (textLength < 305) {
            return 2;
        } else {
            textLength -= 304;
            int smsCount = Math.round((textLength / 152));
            smsCount = textLength % 152 == 0 ? smsCount : smsCount + 1;
            return smsCount + 2;
        }
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        int resourceId = showFreeText ? R.layout.freelayout_text : R.layout.freelayout_dropdown;
        XmlResourceParser freeLayoutRes = getLayoutResourceByResourceId(resourceId);
        View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren(freeLayout);
        if (showFreeText) {
            resolveFreeTextChildren();
        } else {
            resolveFreeLayoutsDropDownChildren();
            buildContent(freeLayoutView);
        }
    }


    private void resolveChildren(ViewGroup freeLayout) {
        if (refreshSenderTask != null) {
            refreshSenderTask.cancel(true);
        }
        parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
    }

    private void resolveFreeTextChildren() {
        senderFreeText = (EditText) parentTableRow.getChildAt(0);
        if (freeTextContent != null) {
            senderFreeText.setText(freeTextContent);
            freeTextContent = null;
        }
        setInputFiltersForEditText();
    }

    private void resolveFreeLayoutsDropDownChildren() {
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(1);
        sourceIDCB = (CheckBox) parentTableRow.getChildAt(0);
        refreshButton = (ImageButton) parentTableRow.getChildAt(2);
        progressBar = (ProgressBar) parentLinearLayout.getChildAt(2);
        infoTextField = (TextView) parentLinearLayout.getChildAt(0);
        senderSpinner = (Spinner) parentLinearLayout.getChildAt(1);
    }

    private void buildContent(View view) {
        refreshAdapterItems();
        infoTextField.setText(getTextByResourceId(R.string.default_number));
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceIDCB.setChecked(true);
            }
        };
        infoTextField.setOnClickListener(l);
        parentTableRow.setOnClickListener(l);
        sourceIDCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    refreshButton.setVisibility(View.VISIBLE);
                    if (adapterItems.size() == 0) {
                        infoTextField.setVisibility(View.VISIBLE);
                        infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
                        senderSpinner.setVisibility(View.GONE);
                    } else {
                        infoTextField.setVisibility(View.GONE);
                        senderSpinner.setVisibility(View.VISIBLE);
                    }
                } else {
                    refreshButton.setVisibility(View.GONE);
                    senderSpinner.setVisibility(View.GONE);
                    infoTextField.setVisibility(View.VISIBLE);
                    infoTextField.setText(getTextByResourceId(R.string.default_number));
                }
            }
        });
        progressBar.setVisibility(View.GONE);
        refreshButton.setVisibility(View.GONE);
        refreshButton.setImageDrawable(getDrawble(R.drawable.btn_menu_view));
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (refreshSenderTask != null) {
                    refreshSenderTask.cancel(true);
                }
                senderSpinner.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                infoTextField.setVisibility(View.GONE);
                refreshSenderTask = new RefreshSenderTask(GMXOptionProvider.this);
                refreshSenderTask.execute(null, null);
            }
        });
        //alway create a new spinner, otherwise data gets not updated
        senderSpinner.setVisibility(sourceIDCB.isChecked() ? View.VISIBLE : View.GONE);
        refreshSpinner(view.getContext());
        senderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerItem = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (checkBoxState != null) {
            sourceIDCB.setChecked(checkBoxState);
        }
        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION && spinnerItem < adapterItems.size()) {
            senderSpinner.setSelection(spinnerItem, true);
        }
        boolean checked = sourceIDCB.isChecked();
        sourceIDCB.setChecked(!checked); //force a recall of the listener to set correct visibility
        sourceIDCB.setChecked(checked); //force a recall of the listener to set correct visibility
        //reset all states to get fresh values
        checkBoxState = null;
        spinnerItem = null;
    }

    private void refreshAdapterItems() {
        adapterItems = new HashMap<Integer, String>(5);
        Map<String, ?> stringMap = getSettings().getAll();
        for (Map.Entry<String, ?> stringEntry : stringMap.entrySet()) {
            String key = stringEntry.getKey();
            if (key.startsWith(SENDER_PREFIX + getUserName())) {
                int number = Integer.parseInt(key.replace(SENDER_PREFIX + getUserName() + ".", ""));
                adapterItems.put(number, String.valueOf(stringEntry.getValue()));
            }
        }
    }

    private void refreshSpinner(Context context) {
        List<String> values = new LinkedList<String>(adapterItems.values());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        senderSpinner.setAdapter(adapter);
    }


    private void setInputFiltersForEditText() {
        int length = 11;
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
        senderFreeText.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
        senderFreeText.setText(getSenderFromOptions());
    }

    private CharSequence getSenderFromOptions() {
        return "";   //TODO save
    }

    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        if (!showFreeText) {
            spinnerItem = savedInstanceState.getInt(STATE_SPINNER, Spinner.INVALID_POSITION);
            checkBoxState = savedInstanceState.getBoolean(STATE_CHECKBOX);
        } else {
            freeTextContent = savedInstanceState.getString(STATE_FREE_TEXT, null);
        }
    }

    @Override
    public void onActivityPaused(Bundle outState) {
        if (!showFreeText) {
            outState.putInt(STATE_SPINNER, senderSpinner.getSelectedItemPosition());
            outState.putBoolean(STATE_CHECKBOX, sourceIDCB.isChecked());
        } else {
            outState.putString(STATE_FREE_TEXT, senderFreeText.getText().toString());
        }
    }


}
