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
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.settings.preferences.model.AccountModel;
import de.christl.smsoip.option.OptionProvider;

import java.util.*;

/**
 *
 */
public class GMXOptionProvider extends OptionProvider {

    private static final String PROVIDER_NAME = "GMX";
    public static final String PROVIDER_CHECKNOFREESMSAVAILABLE = "provider.checknofreesmsavailable";
    private static final String SENDER_LAST_DD_PREFIX = "sender_last_dd_";

    private HashMap<Integer, String> adapterItems;
    private static final String SENDER_PREFIX = "sender_";
    private ViewGroup parentTableRow;
    private ImageButton refreshButton;
    private ProgressBar progressBar;
    private TextView infoTextField;
    private Spinner senderSpinner;
    private RefreshSenderTask refreshSenderTask;

    private GMXSupplier supplier;

    private Integer spinnerItem;


    private static final String STATE_SPINNER = "gmx.state.checkbox";

    public GMXOptionProvider(GMXSupplier supplier) {
        super(PROVIDER_NAME);
        this.supplier = supplier;
    }

    @Override
    public List<Preference> getAdditionalPreferences(Context context) {
        List<Preference> out = new ArrayList<Preference>();
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
    public Drawable getIconDrawable() {
        return getDrawable(R.drawable.icon);
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
        int resourceId = R.layout.freelayout_dropdown;
        XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(resourceId);
        View freeLayoutView = LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren(freeLayout);
        resolveFreeLayoutsDropDownChildren();
        buildContent(freeLayoutView);
    }


    private void resolveChildren(ViewGroup freeLayout) {
        if (refreshSenderTask != null) {
            refreshSenderTask.cancel(true);
        }
        parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
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

        progressBar.setVisibility(View.GONE);
        refreshButton.setImageDrawable(getDrawable(R.drawable.btn_menu_view));
        View.OnClickListener refreshListener = new View.OnClickListener() {
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
        };
        refreshButton.setOnClickListener(refreshListener);
        //alway create a new spinner, otherwise data gets not updated
        refreshSpinner(view.getContext());
        if (adapterItems.size() == 0) {
            infoTextField.setVisibility(View.VISIBLE);
            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
            infoTextField.setOnClickListener(refreshListener);
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


    @Override
    public void afterActivityKilledAndOnCreateCalled(Bundle savedInstanceState) {
        spinnerItem = savedInstanceState.getInt(STATE_SPINNER, Spinner.INVALID_POSITION);
    }

    @Override
    public void onActivityPaused(Bundle outState) {
        spinnerItem = senderSpinner.getSelectedItemPosition();
        outState.putInt(STATE_SPINNER, spinnerItem);
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
        }
        edit.commit();
    }

    public void saveTemporaryState() {
        spinnerItem = senderSpinner == null ? null : senderSpinner.getSelectedItemPosition();

    }

    public String getSender() {
        Object selectedItem = senderSpinner.getSelectedItem();
        String out = null;
        if (selectedItem != null) {
            out = String.valueOf(selectedItem);
        }
        return out;
    }


    public void saveLastSender() {
        SharedPreferences.Editor edit = getSettings().edit();
        Object selectedItem = senderSpinner.getSelectedItemId();
        if (selectedItem != null) {
            edit.putInt(SENDER_LAST_DD_PREFIX + getUserName(), senderSpinner.getSelectedItemPosition());
        }

        edit.commit();
        //reset all temporary variables
        spinnerItem = null;
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
        }
        edit.commit();
    }

    private int getLastSenderDD() {
        return getSettings().getInt(SENDER_LAST_DD_PREFIX + getUserName(), Spinner.INVALID_POSITION);
    }

    @Override
    public int getMinimalCoreVersion() {
        return 40;
    }
}
