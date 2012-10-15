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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.option.OptionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DirectboxOptionProvider extends OptionProvider {

    private static String providerName = "DirectBOX";
    private TextView header;
    private LinearLayout wrapper;
    private TextView infoTextField;
    private CheckBox sourceIDCB;
    private ImageView refreshView;
    private RefreshNumbersTask refreshNumberTask;
    private DirectboxSupplier directboxSupplier;
    private Spinner numberSpinner;
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";

    private static final String SENDER_PREFIX = "sender_";
    private ArrayList<String> adapterItems;
    private static final String STATE_CHECKBOX = "state.checkbox";
    private static final String STATE_SPINNER = "state.checkbox";
    private Boolean checkBoxState;
    private Integer spinnerItem;

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

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        buildContent(freeLayout.getContext());
        freeLayout.setOrientation(LinearLayout.VERTICAL);
        freeLayout.addView(header);
        wrapper.removeAllViews();
        wrapper.addView(sourceIDCB);
        wrapper.addView(infoTextField);
        wrapper.addView(numberSpinner);
        wrapper.addView(refreshView);
        freeLayout.addView(wrapper);
    }

    private void buildContent(Context context) {
        refreshAdapterItems();
        String defaultSendType = getSettings().getString(PROVIDER_DEFAULT_TYPE, "");
        String[] spinnerItems = getArrayByResourceId(R.array.array_spinner);
        boolean startWithSourceIdentifier = defaultSendType.equals(spinnerItems[1]);

        if (header == null) {
            header = new TextView(context);
            header.setText(getTextByResourceId(R.string.text_sender));
            header.setGravity(Gravity.CENTER);
            header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        } else {
            ViewGroup parent = (ViewGroup) header.getParent();
            if (parent != null) {          //remove an already assigned view from its parent to avoid exception
                parent.removeView(header);
            }
        }
        if (wrapper == null) {
            wrapper = new LinearLayout(context);
        } else {
            ViewGroup parent = (ViewGroup) wrapper.getParent();
            if (parent != null) {
                parent.removeView(wrapper);
            }
        }
        if (infoTextField == null) {
            infoTextField = new TextView(context);
            infoTextField.setText(getTextByResourceId(R.string.text_without_si));
            infoTextField.setGravity(Gravity.LEFT);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(20, 0, 0, 0);
            layoutParams.weight = 2.0f;
            infoTextField.setLayoutParams(layoutParams);
            infoTextField.setEllipsize(TextUtils.TruncateAt.END);
            infoTextField.setSingleLine(true);
        }
        if (sourceIDCB == null) {
            sourceIDCB = new CheckBox(context);
            sourceIDCB.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        refreshView.setVisibility(View.VISIBLE);
                        if (adapterItems.size() == 0) {
                            infoTextField.setVisibility(View.VISIBLE);
                            infoTextField.setText(getTextByResourceId(R.string.text_not_yet_refreshed));
                            numberSpinner.setVisibility(View.GONE);
                        } else {
                            infoTextField.setVisibility(View.GONE);
                            numberSpinner.setVisibility(View.VISIBLE);
                        }
                    } else {
                        refreshView.setVisibility(View.GONE);
                        numberSpinner.setVisibility(View.GONE);
                        infoTextField.setVisibility(View.VISIBLE);
                        infoTextField.setText(getTextByResourceId(R.string.text_without_si));
                    }
                    checkBoxState = isChecked;

                }
            });
        }
        if (refreshView == null) {
            refreshView = new ImageView(context);
            refreshView.setVisibility(View.GONE);
            refreshView.setImageDrawable(getDrawble(R.drawable.btn_menu_view));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(20, 0, 0, 0);
            refreshView.setLayoutParams(layoutParams);
            refreshView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (refreshNumberTask != null) {
                        refreshNumberTask.cancel(true);
                    }
                    numberSpinner.setVisibility(View.GONE);
                    infoTextField.setVisibility(View.VISIBLE);
                    refreshNumberTask = new RefreshNumbersTask(DirectboxOptionProvider.this);
                    refreshNumberTask.execute(null, null);
                }
            });
        }
        //alway create a new spinner, otherwise data gets not updated
        numberSpinner = new Spinner(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(20, 0, 0, 0);
        numberSpinner.setLayoutParams(layoutParams);
        refreshSpinner(context);
        if (checkBoxState != null) {
            startWithSourceIdentifier = checkBoxState;
        }
        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION) {
            numberSpinner.setSelection(spinnerItem);
        }
        numberSpinner.setVisibility(startWithSourceIdentifier ? View.VISIBLE : View.GONE);
        sourceIDCB.setChecked(!startWithSourceIdentifier);//set it to "old" before to force a refresh by calling listener
        sourceIDCB.setChecked(startWithSourceIdentifier);//get this from the options
    }

    private void refreshSpinner(Context context) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, adapterItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        numberSpinner.setAdapter(adapter);
    }

    @Override
    public void setCurrentAccountId(Integer currentAccountId) {
        super.setCurrentAccountId(currentAccountId);
        resetState();
    }

    public void resetState() {
        checkBoxState = null;
        spinnerItem = null;
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

    public TextView getTextField() {
        return infoTextField;
    }

    public DirectboxSupplier getDirectboxSupplier() {
        return directboxSupplier;
    }

    public void refreshDropDownAfterSuccesfulUpdate() {
        refreshAdapterItems();
        if (adapterItems.size() > 0) {
            numberSpinner.setVisibility(View.VISIBLE);
            infoTextField.setVisibility(View.GONE);
            refreshSpinner(numberSpinner.getContext());
        } else {
            numberSpinner.setVisibility(View.GONE);
        }
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
        listPref.setDialogTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setKey(PROVIDER_DEFAULT_TYPE);
        listPref.setTitle(getTextByResourceId(R.string.text_default_type));
        listPref.setSummary(getTextByResourceId(R.string.text_default_type_long));
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
        outState.putInt(STATE_SPINNER, numberSpinner.getSelectedItemPosition());
        outState.putBoolean(STATE_CHECKBOX, sourceIDCB.isChecked());
    }

    public boolean isSIActivated() {
        return sourceIDCB.isChecked();
    }

    public String getSender() {
        Object selectedItem = numberSpinner.getSelectedItem();
        return selectedItem == null ? null : String.valueOf(selectedItem);
    }
}
