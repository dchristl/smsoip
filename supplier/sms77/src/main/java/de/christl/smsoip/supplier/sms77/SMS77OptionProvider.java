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

import java.util.*;

/**
 *
 */
public class SMS77OptionProvider extends OptionProvider {
    private static final String PROVIDER_NAME = "SMS77";
    public static final String PROVIDER_DEFAULT_TYPE = "provider.defaulttype";
    private int messageCount = 10;
    private EditText senderFreeText;
    private boolean senderVisible = false;
    private boolean showFreeText = true;
    private ViewGroup parentTableRow;
    private ImageButton refreshButton;
    private ProgressBar progressBar;
    private TextView infoTextField;
    private Spinner senderSpinner;

    private Integer spinnerItem;
    private HashMap<Integer, String> adapterItems;
    private RefreshNumbersTask refreshNumberTask;

    private static final String SENDER_PREFIX = "sender_";
    private static final String SENDER_LAST_NUMBER_PREFIX = "sender_last_";

    public SMS77OptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        if (senderVisible) {
            int resourceId = showFreeText ? R.layout.freelayout_text : R.layout.freelayout_dropdown;
            XmlResourceParser freeLayoutRes = getXMLResourceByResourceId(resourceId);
            ViewGroup freeLayoutView = (ViewGroup) LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
            resolveChildren(freeLayout);
            if (showFreeText) {
                resolveFreeTextChildren();
            } else {
                resolveFreeLayoutsDropDownChildren();
                buildContent(freeLayoutView);
            }
        }
    }

    private void buildContent(View freeLayoutView) {
        refreshButton.setImageDrawable(getDrawble(R.drawable.btn_menu_view));
        View.OnClickListener refreshNumbersListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (refreshNumberTask != null) {
                    refreshNumberTask.cancel(true);
                }
                senderSpinner.setVisibility(View.GONE);
                infoTextField.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                refreshNumberTask = new RefreshNumbersTask(SMS77OptionProvider.this);
                refreshNumberTask.execute(null, null);
            }
        };
        refreshButton.setOnClickListener(refreshNumbersListener);
        infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
        infoTextField.setOnClickListener(refreshNumbersListener);

        refreshAdapterItems();
        refreshSpinner(freeLayoutView.getContext());
        if (adapterItems.size() == 0) {
            infoTextField.setVisibility(View.VISIBLE);
            infoTextField.setText(getTextByResourceId(R.string.not_yet_refreshed));
            senderSpinner.setVisibility(View.GONE);
        } else {
            infoTextField.setVisibility(View.GONE);
            senderSpinner.setVisibility(View.VISIBLE);
        }

        int lastSavedSender = getLastSender();
        if (spinnerItem != null && spinnerItem != Spinner.INVALID_POSITION && spinnerItem < adapterItems.size()) {
            senderSpinner.setSelection(spinnerItem, true);
        } else if (lastSavedSender != Spinner.INVALID_POSITION && lastSavedSender < adapterItems.size()) {
            senderSpinner.setSelection(lastSavedSender, true);
        }
        spinnerItem = null;
    }

    private void refreshSpinner(Context context) {
        List<String> values = new LinkedList<String>(adapterItems.values());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter.notifyDataSetChanged();
        senderSpinner.setAdapter(adapter);
    }

    private int getLastSender() {
        return getSettings().getInt(SENDER_LAST_NUMBER_PREFIX + getUserName(), Spinner.INVALID_POSITION);
    }

    private void resolveFreeLayoutsDropDownChildren() {
        LinearLayout parentLinearLayout = (LinearLayout) parentTableRow.getChildAt(0);
        refreshButton = (ImageButton) parentTableRow.getChildAt(1);
        progressBar = (ProgressBar) parentLinearLayout.getChildAt(2);
        infoTextField = (TextView) parentLinearLayout.getChildAt(0);
        senderSpinner = (Spinner) parentLinearLayout.getChildAt(1);

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

    private void resolveChildren(ViewGroup freeLayout) {
        if (refreshNumberTask != null) {
            refreshNumberTask.cancel(true);
        }
        parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
    }

    private void resolveFreeTextChildren() {
        senderFreeText = (EditText) parentTableRow.getChildAt(0);
//        if (freeTextContent != null) {
//            senderFreeText.setText(freeTextContent);
//            freeTextContent = null;
//        }
        setInputFiltersForEditText();
    }


    private void setInputFiltersForEditText() {
        int length = 16;
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(length);//max 16 chars allowed
        InputFilter specialCharsFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    char currentSource = source.charAt(i);
                    if (!Character.isLetterOrDigit(currentSource)) {//only numbers or charcters allowed
                        return "";
                    }
                    String currentString = dest.toString();
                    if (currentString.length() >= 11) {
                        //break if any charcters will be chosen

                        if (!Character.isDigit(currentSource)) {
                            return "";
                        }
                        if (Character.isDigit(currentSource)) {
                            for (int z = 0; z < currentString.length(); z++) {
                                if (!Character.isDigit(currentString.charAt(z))) {
                                    return "";
                                }

                            }

                        }

                    }
                }
                return null;
            }
        };
        senderFreeText.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
        senderFreeText.setText(getSenderFromOptions());
    }

    private CharSequence getSenderFromOptions() {
        return "";
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
                    default:
                    case 0:  //BASIC
                        senderVisible = false;
                        messageCount = 10;
                        showFreeText = false;
                        break;
                    case 1: //QUALITY (FreeText)
                        senderVisible = true;
                        showFreeText = true;
                        messageCount = 10;
                        break;
//                    case 2: //QUALITY (Freetext)
//                        senderVisible = true;
//                        showFreeText = true;
//                        messageCount = 10;
//                        break;
                    case 2:  //LANDLINE
                        senderVisible = false;
                        showFreeText = false;
                        messageCount = 1;
                        break;
                    case 3:   //FLASH
                        senderVisible = false;
                        showFreeText = false;
                        messageCount = 1;
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

    @Override
    public int getTextMessageLength() {
        return 160;    //just for inputfilter
    }

    @Override
    public int getMaxMessageCount() {
        return messageCount;  //just for inputfilter
    }


    @Override
    public int getLengthDependentSMSCount(int textLength) {
        if (textLength < 161) {
            return 1;
        } else {
            textLength -= 160;
            int smsCount = Math.round((textLength / 153));
            smsCount = textLength % 153 == 0 ? smsCount : smsCount + 1;
            return smsCount + 1;
        }
    }
}
