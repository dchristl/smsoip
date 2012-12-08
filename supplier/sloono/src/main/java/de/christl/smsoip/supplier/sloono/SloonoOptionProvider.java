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

package de.christl.smsoip.supplier.sloono;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

import java.util.*;


public class SloonoOptionProvider extends OptionProvider {


    private static final String PROVIDER_NAME = "Sloono";

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private TextView infoTextField;
    private CheckBox sourceIDCB;
    private ImageButton refreshButton;
    private RefreshSenderTask refreshSenderTask;
    private SloonoSupplier supplier;
    ProgressBar progressBar;
    private Spinner senderSpinner;
    private static final String SENDER_PREFIX = "sender_";
    private HashMap<Integer, String> adapterItems;
    private boolean showSenders = true;
    private static final String STATE_CHECKBOX = "sloono.state.checkbox";
    private static final String STATE_SPINNER = "sloono.state.checkbox";

    private static final String PROVIDER_CHECK_FOR_OLD_SETTINGS_COUNT = "provider.check.for.old.settings.count";
    private static final int CHECK_FOR_OLD_SETTINGS = 10;

    private Boolean checkBoxState;
    private Integer spinnerItem;
    private ViewGroup parentTableRow;

    public SloonoOptionProvider(SloonoSupplier sloonoSupplier) {
        supplier = sloonoSupplier;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }


    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        XmlResourceParser freeLayoutRes = getLayoutResourceByResourceId(R.layout.freelayout);
        View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        freeLayout.setVisibility(showSenders ? View.VISIBLE : View.GONE);
        resolveChildren(freeLayout);
        buildContent(freeLayoutView);
        removeOldSettings();
    }

    private void removeOldSettings() {
        int checkOldSettingsCount = getSettings().getInt(PROVIDER_CHECK_FOR_OLD_SETTINGS_COUNT, 0);
        SharedPreferences.Editor edit = getSettings().edit();
        if (checkOldSettingsCount > CHECK_FOR_OLD_SETTINGS) {
            Map<Integer, AccountModel> accounts = getAccounts();
            Map<String, ?> allSettings = getSettings().getAll();
            Outer:
            for (Map.Entry<String, ?> stringEntry : allSettings.entrySet()) {
                String key = stringEntry.getKey();
                if (key.startsWith(SENDER_PREFIX)) {
                    String currAccountName = key.replaceAll(SENDER_PREFIX, "");
                    currAccountName = currAccountName.substring(0,currAccountName.lastIndexOf("."));
                    for (Map.Entry<Integer, AccountModel> integerAccountModelEntry : accounts.entrySet()) {
                        if (currAccountName.equals(integerAccountModelEntry.getValue().getUserName())) {
                            continue Outer;
                        }
                    }
                    edit.remove(key);
                }

            }
            edit.remove(PROVIDER_CHECK_FOR_OLD_SETTINGS_COUNT);
        } else {

            edit.putInt(PROVIDER_CHECK_FOR_OLD_SETTINGS_COUNT, ++checkOldSettingsCount);
        }
        edit.commit();
    }


    private void resolveChildren(ViewGroup freeLayout) {
        parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(1);
        sourceIDCB = (CheckBox) parentTableRow.getChildAt(0);
        refreshButton = (ImageButton) parentTableRow.getChildAt(2);
        progressBar = (ProgressBar) parentLinearLayout.getChildAt(2);
        infoTextField = (TextView) parentLinearLayout.getChildAt(0);
        senderSpinner = (Spinner) parentLinearLayout.getChildAt(1);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
    }

    private void buildContent(View view) {
        refreshAdapterItems();
        infoTextField.setText(getTextByResourceId(R.string.given_number));
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
                    infoTextField.setText(getTextByResourceId(R.string.given_number));
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
                refreshSenderTask = new RefreshSenderTask(SloonoOptionProvider.this);
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


    private void refreshSpinner(Context context) {
        List<String> values = new LinkedList<String>(adapterItems.values());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        senderSpinner.setAdapter(adapter);
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

    public SloonoSupplier getSloonoSupplier() {
        return supplier;
    }

    public void refreshDropDownAfterSuccesfulUpdate() {
        progressBar.setVisibility(View.GONE);
        refreshAdapterItems();
        if (adapterItems.size() > 0) {
            senderSpinner.setVisibility(View.VISIBLE);
            infoTextField.setVisibility(View.GONE);
            refreshSpinner(senderSpinner.getContext());
        } else {
            senderSpinner.setVisibility(View.GONE);
        }
    }

    public void saveSenders(HashMap<Integer, String> numbers) {
        removeOldNumbers();
        if (numbers.size() > 0) {
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
        }
        edit.commit();
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
                        showSenders = false;
                        break;
                    case 1:
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

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void setErrorTextAfterSenderUpdate(String message) {
        infoTextField.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        infoTextField.setText(message);
    }

    public String getSender() {
        String out = null;
        if (sourceIDCB.isChecked()) {
            if (senderSpinner.getSelectedItemId() != Spinner.INVALID_POSITION) {
                String selectedItem = (String) senderSpinner.getSelectedItem();
                for (Map.Entry<Integer, String> integerStringEntry : adapterItems.entrySet()) {
                    if (integerStringEntry.getValue().equals(selectedItem)) {
                        out = integerStringEntry.getKey().toString();
                        break;
                    }
                }
            } else {
                out = null;
            }
        } else {
            out = "1";
        }
        return out;
    }


    @Override
    public int getTextMessageLength() {
        return 100;    //just for inputfilter
    }

    @Override
    public int getMaxMessageCount() {
        return 16;  //just for inputfilter
    }


    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 161) {
            return 1;
        } else if (textLength < 609) {
            int smsCount = Math.round((textLength / 152));
            smsCount = textLength % 152 == 0 ? smsCount : smsCount + 1;
            return smsCount;
        } else if (textLength < 801) {
            return 5;
        } else {
            textLength -= 800;
            int smsCount = Math.round((textLength / 160));
            smsCount = textLength % 160 == 0 ? smsCount : smsCount + 1;
            return smsCount + 5;
        }
    }


    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        spinnerItem = savedInstanceState.getInt(STATE_SPINNER, Spinner.INVALID_POSITION);
        checkBoxState = savedInstanceState.getBoolean(STATE_CHECKBOX);
    }

    @Override
    public void onActivityPaused(Bundle outState) {
        saveState();
        outState.putInt(STATE_SPINNER, spinnerItem);
        outState.putBoolean(STATE_CHECKBOX, checkBoxState);
    }

    public void saveState() {
        checkBoxState = sourceIDCB.isChecked();
        spinnerItem = senderSpinner.getSelectedItemPosition();
    }
}
