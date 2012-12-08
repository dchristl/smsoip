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

package de.christl.smsoip.supplier.freenet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Class for Freenet options
 */
public class FreenetOptionProvider extends OptionProvider {

    private static final String PROVIDERNAME = "Freenet";
    private static final String DIVIDER = " | ";

    public static final String PROVIDER_SAVE_IN_SENT = "provider.saveInSent";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private static final String SENDER_PREFIX = "sender_";

    private Integer spinnerItem;
    private ProgressBar progressBar;
    private TextView infoTextField;
    private ImageButton refreshButton;
    private Map<String, String> adapterItems;
    private Spinner numberSpinner;
    private boolean showSenders = true;
    private RefreshNumbersTask refreshNumberTask;
    private FreenetSupplier freenetSupplier;

    public FreenetOptionProvider(FreenetSupplier freenetSupplier) {
        super(PROVIDERNAME);
        this.freenetSupplier = freenetSupplier;
    }


    @Override
    public String getProviderName() {
        return PROVIDERNAME;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
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
        checkNoFreeSMSAvailable.setKey(PROVIDER_SAVE_IN_SENT);
        checkNoFreeSMSAvailable.setTitle(getTextByResourceId(R.string.text_save_in_sent));
        checkNoFreeSMSAvailable.setSummary(getTextByResourceId(R.string.text_save_in_sent_long));
        out.add(checkNoFreeSMSAvailable);
        return out;
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
                    case 2:
                        showSenders = false;
                        break;
                    default:
                        showSenders = true;
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
    public void getFreeLayout(LinearLayout freeLayout) {
        if (showSenders) {
            XmlResourceParser freeLayoutRes = getLayoutResourceByResourceId(R.layout.freelayout);
            View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
            resolveChildren(freeLayout);
            buildContent(freeLayoutView);
        }

    }

    /**
     * childs have to be resolved by its tree in structure, findViewById seems not to work on every device
     *
     * @param freeLayout
     */
    private void resolveChildren(ViewGroup freeLayout) {
        ViewGroup parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(0);
        refreshButton = (ImageButton) parentTableRow.getChildAt(1);
        progressBar = (ProgressBar) parentLinearLayout.getChildAt(2);
        infoTextField = (TextView) parentLinearLayout.getChildAt(0);
        numberSpinner = (Spinner) parentLinearLayout.getChildAt(1);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
    }

    private void buildContent(View freeLayoutView) {
        refreshButton.setImageDrawable(getDrawble(R.drawable.btn_menu_view));
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (refreshNumberTask != null) {
                    refreshNumberTask.cancel(true);
                }
                numberSpinner.setVisibility(View.GONE);
                infoTextField.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                refreshNumberTask = new RefreshNumbersTask(FreenetOptionProvider.this);
                refreshNumberTask.execute(null, null);
            }
        });
        infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));

        refreshAdapterItems();
        refreshSpinner(freeLayoutView.getContext());
        if (adapterItems.size() == 0) {
            infoTextField.setVisibility(View.VISIBLE);
            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
            numberSpinner.setVisibility(View.GONE);
        } else {
            infoTextField.setVisibility(View.GONE);
            numberSpinner.setVisibility(View.VISIBLE);
        }


        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION && spinnerItem < adapterItems.size()) {
            numberSpinner.setSelection(spinnerItem, true);
        }
        spinnerItem = null;
    }

    @Override
    public void onAccountsChanged() {
        SharedPreferences.Editor edit = getSettings().edit();
        Map<Integer, AccountModel> accounts = getAccounts();
        Map<String, ?> allSettings = getSettings().getAll();
        Outer:
        for (Map.Entry<String, ?> stringEntry : allSettings.entrySet()) {
            if (stringEntry.getKey().startsWith(SENDER_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_PREFIX, "");
                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                    if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
                        continue Outer;
                    }
                }
                edit.remove(stringEntry.getKey());
            }
//            if (stringEntry.getKey().startsWith(SENDER_FREE_LAST_INPUT_PREFIX)) {
//                String currAccountName = stringEntry.getKey().replaceAll(SENDER_FREE_LAST_INPUT_PREFIX, "");
//                for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
//                    if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
//                        continue Outer;
//                    }
//                }
//                edit.remove(stringEntry.getKey());
//            }
        }
        edit.commit();
    }

    public String getSender() {
        Object selectedItem = numberSpinner.getSelectedItem();
        return selectedItem == null ? null : adapterItems.get(String.valueOf(selectedItem));
    }

    private void refreshAdapterItems() {
        adapterItems = new TreeMap<String, String>();
        Map<String, ?> stringMap = getSettings().getAll();
        for (Map.Entry<String, ?> stringEntry : stringMap.entrySet()) {
            String key = stringEntry.getKey();
            if (key.startsWith(SENDER_PREFIX + getUserName())) {
                String value = (String) stringEntry.getValue();
                adapterItems.put(value.replaceAll(Pattern.quote(DIVIDER) + ".*", ""), value.replaceAll(".*" + Pattern.quote(DIVIDER), ""));
            }
        }
    }

    private void refreshSpinner(Context context) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, adapterItems.keySet().toArray(new String[adapterItems.size()]));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        numberSpinner.setAdapter(adapter);
    }

    public void refreshDropDownAfterSuccesfulUpdate() {
        progressBar.setVisibility(View.GONE);
        refreshAdapterItems();
        if (adapterItems.size() > 0) {
            numberSpinner.setVisibility(View.VISIBLE);
            refreshSpinner(numberSpinner.getContext());
        } else {
            infoTextField.setVisibility(View.VISIBLE);
            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
            numberSpinner.setVisibility(View.GONE);
        }
    }

    public void setErrorMessageOnUpdate(String message) {
        progressBar.setVisibility(View.GONE);
        infoTextField.setVisibility(View.VISIBLE);
        infoTextField.setText(message);
    }

    public void saveNumbers(Map<String, String> numbers) {
        if (numbers.size() > 0) {
            removeOldNumbers();
            SharedPreferences.Editor edit = getSettings().edit();
            int i = 0;
            for (Map.Entry<String, String> stringStringEntry : numbers.entrySet()) {
                edit.putString(SENDER_PREFIX + getUserName() + "." + i, stringStringEntry.getKey() + DIVIDER + stringStringEntry.getValue());
                i++;
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
        }
        edit.commit();
    }

    public FreenetSupplier getFreenetSupplier() {
        return freenetSupplier;
    }
}
