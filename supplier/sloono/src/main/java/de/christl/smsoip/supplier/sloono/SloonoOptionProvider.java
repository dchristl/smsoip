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
import android.graphics.drawable.Drawable;
import android.preference.ListPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import de.christl.smsoip.activities.SendActivity;
import de.christl.smsoip.option.OptionProvider;

import java.util.*;


public class SloonoOptionProvider extends OptionProvider {


    private static final String PROVIDER_NAME = "Sloono";

    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private TextView header;
    private LinearLayout wrapper;
    private TextView infoTextField;
    private CheckBox sourceIDCB;
    private ImageButton refreshView;
    private RefreshSenderTask refreshSenderTask;
    private SloonoSupplier supplier;
    ProgressBar progressBar;
    private Spinner senderSpinner;
    private static final String SENDER_PREFIX = "sender_";
    private HashMap<Integer, String> adapterItems;
    private boolean showSenders = true;

    private Boolean checkBoxState;
    private Integer spinnerItem;

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
        buildContent(freeLayout.getContext());
        if (showSenders) {
            freeLayout.setOrientation(LinearLayout.VERTICAL);
            freeLayout.addView(header);
            wrapper.removeAllViews();
            wrapper.addView(sourceIDCB);
            wrapper.addView(infoTextField);
            wrapper.addView(progressBar);
            wrapper.addView(senderSpinner);
            wrapper.addView(refreshView);
            freeLayout.addView(wrapper);
        }
    }

    private void buildContent(Context context) {
        refreshAdapterItems();

        if (header == null) {
            header = new TextView(context);
            header.setText(getTextByResourceId(R.string.sender));
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
            infoTextField.setText(getTextByResourceId(R.string.given_number));
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
                            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
                            senderSpinner.setVisibility(View.GONE);
                        } else {
                            infoTextField.setVisibility(View.GONE);
                            senderSpinner.setVisibility(View.VISIBLE);
                        }
                    } else {
                        refreshView.setVisibility(View.GONE);
                        senderSpinner.setVisibility(View.GONE);
                        infoTextField.setVisibility(View.VISIBLE);
                        infoTextField.setText(getTextByResourceId(R.string.given_number));
                    }
                    checkBoxState = isChecked;
                }
            });
        }
        if (progressBar == null) {
            progressBar = new ProgressBar(context);
            progressBar.setVisibility(View.GONE);
        }
        if (refreshView == null) {
            refreshView = new ImageButton(context);
            refreshView.setVisibility(View.GONE);
            refreshView.setImageDrawable(getDrawble(R.drawable.btn_menu_view));
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(20, 0, 0, 0);
            refreshView.setLayoutParams(layoutParams);
            refreshView.setOnClickListener(new View.OnClickListener() {
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
        }
        //alway create a new spinner, otherwise data gets not updated
        senderSpinner = new Spinner(context);
        senderSpinner.setVisibility(sourceIDCB.isChecked() ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(20, 0, 0, 0);
        senderSpinner.setLayoutParams(layoutParams);
        refreshSpinner(context);
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
        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION) {
            senderSpinner.setSelection(spinnerItem, true);
        }
        boolean checked = sourceIDCB.isChecked();
        sourceIDCB.setChecked(!checked); //force a recall of the listener to set correct visibility
        sourceIDCB.setChecked(checked); //force a recall of the listener to set correct visibility

    }


    private void refreshSpinner(Context context) {
        List<String> values = new LinkedList<String>(adapterItems.values());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        senderSpinner.setAdapter(adapter);
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

    public TextView getTextField() {
        return infoTextField;
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

}
