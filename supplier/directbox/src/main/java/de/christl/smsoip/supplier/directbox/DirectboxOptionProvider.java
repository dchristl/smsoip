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

package de.christl.smsoip.supplier.directbox;


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
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectboxOptionProvider extends OptionProvider {

    private static String providerName = "DirectBOX";
    private TextView infoTextField;
    private CheckBox sourceIDCB;
    private ImageButton refreshButton;
    private RefreshNumbersTask refreshNumberTask;
    private DirectboxSupplier directboxSupplier;
    private Spinner numberSpinner;
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";

    private static final String SENDER_PREFIX = "sender_";
    private ArrayList<String> adapterItems;
    private static final String STATE_CHECKBOX = "directbox.state.checkbox";
    private static final String STATE_SPINNER = "directbox.state.spinner";
    private Boolean checkBoxState;
    private Integer spinnerItem;
    private ProgressBar progressBar;
    private ViewGroup parentTableRow;

    public DirectboxOptionProvider(DirectboxSupplier directboxSupplier) {
        super(providerName);
        this.directboxSupplier = directboxSupplier;
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public int getMaxMessageCount() {
        return 1;
    }

    //removed, caused NPE on twice call, needs a research
//    @Override
//    protected Integer getChangelogResourceId() {
//        return R.raw.changelog;
//    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(R.layout.freelayout);
        View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren(freeLayout);
        buildContent(freeLayoutView);

    }

    /**
     * childs have to be resolved by its tree in structure, findViewById seems not to work on every device
     *
     * @param freeLayout
     */
    private void resolveChildren(ViewGroup freeLayout) {
        parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(1);
        sourceIDCB = (CheckBox) parentTableRow.getChildAt(0);
        refreshButton = (ImageButton) parentTableRow.getChildAt(2);
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
                refreshNumberTask = new RefreshNumbersTask(DirectboxOptionProvider.this);
                refreshNumberTask.execute(null, null);
            }
        });
        infoTextField.setText(getTextByResourceId(R.string.without_si));
        View.OnClickListener l = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sourceIDCB.setChecked(true);
            }
        };
        infoTextField.setOnClickListener(l);
        parentTableRow.setOnClickListener(l);

        refreshAdapterItems();
        refreshSpinner(freeLayoutView.getContext());
        sourceIDCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    refreshButton.setVisibility(View.VISIBLE);
                    if (adapterItems.size() == 0) {
                        infoTextField.setVisibility(View.VISIBLE);
                        infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
                        numberSpinner.setVisibility(View.GONE);
                    } else {
                        infoTextField.setVisibility(View.GONE);
                        numberSpinner.setVisibility(View.VISIBLE);
                    }
                } else {
                    refreshButton.setVisibility(View.GONE);
                    numberSpinner.setVisibility(View.GONE);
                    infoTextField.setVisibility(View.VISIBLE);
                    infoTextField.setText(getTextByResourceId(R.string.without_si));
                }


            }
        });
        String defaultSendType = getSettings().getString(PROVIDER_DEFAULT_TYPE, "");
        String[] spinnerItems = getArrayByResourceId(R.array.array_spinner);
        boolean startWithSourceIdentifier = defaultSendType.equals(spinnerItems[1]);
        sourceIDCB.setChecked(startWithSourceIdentifier);
        if (checkBoxState != null) {
            sourceIDCB.setChecked(checkBoxState);

        }
        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION && spinnerItem < adapterItems.size()) {
            numberSpinner.setSelection(spinnerItem, true);

        }
        boolean checked = sourceIDCB.isChecked();
        sourceIDCB.setChecked(!checked); //force a recall of the listener to set correct visibility
        sourceIDCB.setChecked(checked); //force a recall of the listener to set correct visibility

        //reset all states to get fresh values
        checkBoxState = null;
        spinnerItem = null;
    }

    private void refreshSpinner(Context context) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, adapterItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        numberSpinner.setAdapter(adapter);
    }


    private void refreshAdapterItems() {
        adapterItems = new ArrayList<String>();
        Map<String, ?> stringMap = getSettings().getAll();
        for (Map.Entry<String, ?> stringEntry : stringMap.entrySet()) {
            String key = stringEntry.getKey();
            if (key.startsWith(SENDER_PREFIX + getUserName())) {
                adapterItems.add((String) stringEntry.getValue());
            }
        }
    }


    public DirectboxSupplier getDirectboxSupplier() {
        return directboxSupplier;
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

    public void saveNumbers(List<String> numbers) {
        if (numbers.size() > 0) {
            removeOldNumbers();
            SharedPreferences.Editor edit = getSettings().edit();
            int i = 0;
            for (String number : numbers) {
                edit.putString(SENDER_PREFIX + getUserName() + "." + i, number);
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

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
        ListPreference listPref = new ListPreference(context);
        String[] prefArray = getArrayByResourceId(R.array.array_spinner);
        listPref.setEntries(prefArray);
        listPref.setEntryValues(prefArray);
        listPref.setDialogTitle(getTextByResourceId(R.string.default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.default_type));
        listPref.setSummary(getTextByResourceId(R.string.default_type_long));
        listPref.setDefaultValue(prefArray[0]);
        out.add(listPref);
        return out;
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

    public boolean isSIActivated() {
        return sourceIDCB.isChecked();
    }

    public String getSender() {
        Object selectedItem = numberSpinner.getSelectedItem();
        return selectedItem == null ? null : String.valueOf(selectedItem);
    }


    public void saveState() {
        checkBoxState = sourceIDCB.isChecked();
        spinnerItem = numberSpinner.getSelectedItemPosition();
    }

    @Override
    public void onAccountsChanged() {
        super.onAccountsChanged();
        SharedPreferences.Editor edit = getSettings().edit();
        Map<Integer, AccountModel> accounts = getAccounts();
        Map<String, ?> allSettings = getSettings().getAll();
        Outer:
        for (Map.Entry<String, ?> stringEntry : allSettings.entrySet()) {
            if (stringEntry.getKey().startsWith(SENDER_PREFIX)) {
                String currAccountName = stringEntry.getKey().replaceAll(SENDER_PREFIX, "").replaceAll("\\.\\d+$", ""); //replace everything ends with dot and number
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
}
