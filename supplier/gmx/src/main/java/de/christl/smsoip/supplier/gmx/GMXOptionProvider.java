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
import android.content.SharedPreferences;
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
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

import java.util.*;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "GMX";
    public static final String PROVIDER_CHECKNOFREESMSAVAILABLE = "provider.checknofreesmsavailable";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private static final String SENDER_LAST_DD_PREFIX = "sender_last_dd_";
    private static final String SENDER_LAST_FT_PREFIX = "sender_last_ft_";

    private HashMap<Integer, String> adapterItems;
    private static final String SENDER_PREFIX = "sender_";
    private boolean showFreeText;
    private ViewGroup parentTableRow;
    private ImageButton refreshButton;
    private ProgressBar progressBar;
    private TextView infoTextField;
    private Spinner senderSpinner;
    private RefreshSenderTask refreshSenderTask;

    private GMXSupplier supplier;

    private Integer spinnerItem;

    private EditText senderFreeText;

    private static final String STATE_SPINNER = "gmx.state.checkbox";
    private static final String STATE_FREE_TEXT = "gmx.state.freetetx";
    private String freeTextContent;

    public GMXOptionProvider(GMXSupplier supplier) {
        super(PROVIDER_NAME);
        this.supplier = supplier;
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
        XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(resourceId);
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
        } else {
            senderFreeText.setText(getLastSenderFT());
        }
        freeTextContent = null;
        setInputFiltersForEditText();
    }

    private void resolveFreeLayoutsDropDownChildren() {
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(0);
        refreshButton = (ImageButton) parentTableRow.getChildAt(1);
        progressBar = (ProgressBar) parentLinearLayout.getChildAt(2);
        infoTextField = (TextView) parentLinearLayout.getChildAt(0);
        senderSpinner = (Spinner) parentLinearLayout.getChildAt(1);

    }

    private void buildContent(View view) {
        refreshAdapterItems();
        infoTextField.setText(getTextByResourceId(R.string.default_number));

        progressBar.setVisibility(View.GONE);
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
        refreshSpinner(view.getContext());
        if (adapterItems.size() == 0) {
            infoTextField.setVisibility(View.VISIBLE);
            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
            senderSpinner.setVisibility(View.GONE);
        } else {
            infoTextField.setVisibility(View.GONE);
            senderSpinner.setVisibility(View.VISIBLE);
        }
        senderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                spinnerItem = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });


        int lastSavedSender = getLastSenderDD();
        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION && spinnerItem < adapterItems.size()) {
            senderSpinner.setSelection(spinnerItem, true);
        } else if (lastSavedSender != Spinner.INVALID_POSITION && lastSavedSender < adapterItems.size()) {
            senderSpinner.setSelection(lastSavedSender, true);
        }

        //reset all states to get fresh values
        spinnerItem = null;
    }

    private void refreshAdapterItems() {
        adapterItems = new HashMap<Integer, String>();
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
                    char c = source.charAt(i);
                    if (!Character.isWhitespace(c) && !Character.isLetterOrDigit(c)) {//only numbers or charcters allowed
                        return "";
                    }
                }
                return null;
            }
        };
        senderFreeText.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
    }


    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        if (!showFreeText) {
            spinnerItem = savedInstanceState.getInt(STATE_SPINNER, Spinner.INVALID_POSITION);
        } else {
            freeTextContent = savedInstanceState.getString(STATE_FREE_TEXT);
        }
    }

    @Override
    public void onActivityPaused(Bundle outState) {
        if (!showFreeText) {
            spinnerItem = senderSpinner.getSelectedItemPosition();
            outState.putInt(STATE_SPINNER, spinnerItem);
        } else {
            freeTextContent = senderFreeText.getText().toString();
            outState.putString(STATE_FREE_TEXT, freeTextContent);
        }
    }


    public void setErrorMessageOnUpdate(String message) {
        progressBar.setVisibility(View.GONE);
        infoTextField.setVisibility(View.VISIBLE);
        infoTextField.setText(message);
    }

    public void refreshDropDownAfterSuccesfulUpdate() {
        progressBar.setVisibility(View.GONE);
        refreshAdapterItems();
        if (adapterItems.size() > 0) {
            senderSpinner.setVisibility(View.VISIBLE);
            refreshSpinner(senderSpinner.getContext());
        } else {
            infoTextField.setVisibility(View.VISIBLE);
            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
            senderSpinner.setVisibility(View.GONE);
        }
    }

    public GMXSupplier getSupplier() {
        return supplier;
    }

    public void saveNumbers(HashMap<Integer, String> numbers) {
        if (numbers.size() > 0) {
            removeOldNumbers();
            SharedPreferences.Editor edit = getSettings().edit();
            for (Map.Entry<Integer, String> integerStringEntry : numbers.entrySet()) {
                edit.putString(SENDER_PREFIX + getUserName() + "." + integerStringEntry.getKey(), integerStringEntry.getValue());
            }
            edit.commit();
        }
    }

    private void removeOldNumbers() {
        SharedPreferences.Editor edit = getSettings().edit();
        Map<String, ?> stringMap = getSettings().getAll();
        for (String key : stringMap.keySet()) {
            if (key.startsWith(SENDER_PREFIX + getUserName())) {
                edit.remove(key);
            }
            if (key.startsWith(SENDER_LAST_DD_PREFIX + getUserName())) {
                edit.remove(key);
            }
            if (key.startsWith(SENDER_LAST_FT_PREFIX + getUserName())) {
                edit.remove(key);
            }
        }
        edit.commit();
    }

    public void saveTemporaryState() {
        freeTextContent = senderFreeText == null ? null : senderFreeText.getText().toString();
        spinnerItem = senderSpinner == null ? null : senderSpinner.getSelectedItemPosition();

    }

    public int getSenderId() {
        Object selectedItem = senderSpinner.getSelectedItem();
        int out = -1;
        if (selectedItem != null) {
            for (Map.Entry<Integer, String> integerStringEntry : adapterItems.entrySet()) {
                String value = integerStringEntry.getValue();
                if (value != null && value.equals(selectedItem)) {
                    out = integerStringEntry.getKey();
                    break;
                }
            }
        }
        return out;
    }

    public String getSender() {
        return senderFreeText.getText().toString();
    }

    public void saveLastSender() {
        SharedPreferences.Editor edit = getSettings().edit();
        if (!showFreeText) {
            Object selectedItem = senderSpinner.getSelectedItemId();
            if (selectedItem != null) {
                edit.putInt(SENDER_LAST_DD_PREFIX + getUserName(), senderSpinner.getSelectedItemPosition());
            }
        } else {
            String lastFreeText = senderFreeText.getText().toString();
            edit.putString(SENDER_LAST_FT_PREFIX + getUserName(), lastFreeText);
        }
        edit.commit();

    }

    @Override
    public void onAccountsChanged() {
        super.onAccountsChanged();

        SharedPreferences.Editor edit = getSettings().edit();
        Map<Integer, AccountModel> accounts = getAccounts();
        Map<String, ?> allSettings = getSettings().getAll();
        Outer:
        for (Map.Entry<String, ?> stringEntry : allSettings.entrySet()) {

            //remove all claimed senders
            if (stringEntry.getKey().startsWith(SENDER_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_PREFIX, "").replaceAll("\\.\\d+$", "");
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                    if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
                        continue Outer;
                    }
                }
                edit.remove(stringEntry.getKey());
            }

            //remove last sender (dropdown)
            if (stringEntry.getKey().startsWith(SENDER_LAST_DD_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_LAST_DD_PREFIX, "");
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                    if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
                        continue Outer;
                    }
                }
                edit.remove(stringEntry.getKey());
            }
            //remove last sender (freetext)
            if (stringEntry.getKey().startsWith(SENDER_LAST_FT_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_LAST_FT_PREFIX, "");
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

    private int getLastSenderDD() {
        return getSettings().getInt(SENDER_LAST_DD_PREFIX + getUserName(), Spinner.INVALID_POSITION);
    }

    private String getLastSenderFT() {
        return getSettings().getString(SENDER_LAST_FT_PREFIX + getUserName(), "");
    }
}
