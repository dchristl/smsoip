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

package de.christl.smsoip.autosuggest;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;
import de.christl.smsoip.R;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.database.DatabaseHandler;
import de.christl.smsoip.ui.CheckForDuplicatesArrayList;

import java.util.ArrayList;
import java.util.List;

/**
 * Filed for finding numbers and names by userinputs
 */
public class NameNumberSuggestField extends MultiAutoCompleteTextView {


    private final CommaTokenizer tokenizer = new CommaTokenizer();

    private CheckForDuplicatesArrayList receiverList = new CheckForDuplicatesArrayList();
    List<ReceiverChangedListener> listenerList = new ArrayList<ReceiverChangedListener>();

    private final Object lock = new Object();
    private int maxReceiverCount;

    public NameNumberSuggestField(Context context) {
        super(context);
        init();
    }

    public NameNumberSuggestField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NameNumberSuggestField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        List<NameNumberEntry> allContactsWithPhoneNumber = DatabaseHandler.getAllContactsWithPhoneNumber(getContext());
        setAdapter(new NameNumberSuggestAdapter(getContext(), allContactsWithPhoneNumber));
        setTokenizer(tokenizer);
        setValidator(NumberUtils.getNumberValidator());
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                updateReceiverList();
            }
        });
    }

    private void updateReceiverList() {
        String[] split = getText().toString().split(",");
        CheckForDuplicatesArrayList tmpList = new CheckForDuplicatesArrayList();
        boolean addedTwice = false;
        boolean tooMuchReceivers = false;
        for (String s : split) {
            String currentPart = s.trim();
            String unknown = getContext().getString(R.string.text_unknown);
            if (NumberUtils.isCorrectNumberInInternationalStyle(currentPart)) {
                currentPart = NumberUtils.fixNumber(currentPart);
                Receiver receiver = DatabaseHandler.findContactByNumber(currentPart, getContext());
                if (receiver == null) {
                    receiver = new Receiver(unknown);
                    receiver.setReceiverNumber(currentPart, unknown);
                }
                if (tmpList.size()  < maxReceiverCount) {
                    addedTwice |= tmpList.addWithAlreadyInsertedCheck(receiver);
                } else {
                    tooMuchReceivers |= true;
                }
            } else if (NumberUtils.isCorrectNameNumber(currentPart)) {
                String name = currentPart.replaceAll(" \\(.*", "");
                String number = currentPart.replaceAll(".* \\(", "");
                Receiver receiver = new Receiver(name);
                receiver.setRawNumber(number, unknown); //number will be fixed automatically
                if (tmpList.size() < maxReceiverCount) {
                    addedTwice |= tmpList.addWithAlreadyInsertedCheck(receiver);
                } else {
                    tooMuchReceivers |= true;
                }

            }
        }

        synchronized (lock) {
            receiverList = tmpList;
        }
        if (tooMuchReceivers){
            updateTextContent();
        }
        for (ReceiverChangedListener receiverChangedListener : listenerList) {
            receiverChangedListener.onReceiverChanged(addedTwice, tooMuchReceivers);
        }
    }

    public void addReceiverChangedListener(ReceiverChangedListener listener) {
        listenerList.add(listener);
    }


    @Override
    /**
     * perform own validation cause issues with commatokenizer and endless loop
     */
    public void performValidation() {
        Validator v = getValidator();
        if (v == null || tokenizer == null) {
            return;
        }
        setText(NumberUtils.getValidatedString(getText().toString(), tokenizer, v));
        setSelection(getText().length());
    }

    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        NameNumberEntry currentEntry = (NameNumberEntry) selectedItem;
        return currentEntry.getFieldRepresantation();
    }


    public CheckForDuplicatesArrayList getReceiverList() {
        return receiverList;
    }

    public void clearReceiverList() {
        synchronized (lock) {
            receiverList.clear();
        }
        updateTextContent();
    }

    public void setReceiverList(CheckForDuplicatesArrayList receiverList) {
        synchronized (lock) {
            this.receiverList = receiverList;
        }
        updateTextContent();
    }

    public void append(Receiver receiver) {
        synchronized (lock) {
            receiverList.add(receiver);
        }
        updateTextContent();
    }

    public void updateTextContent() {
        StringBuilder builder = new StringBuilder();
        synchronized (lock) {
            for (Receiver receiver : receiverList) {
                String receiverNumber = receiver.getReceiverNumber();
                NameNumberEntry entry = new NameNumberEntry(receiver.getName(), receiverNumber, receiver.getNumberType());
                builder.append(tokenizer.terminateToken(convertSelectionToString(entry)));
            }
        }
        setText(builder.toString());
        performValidation();
    }

    public void setMaxReceivers(int maxReceiverCount) {
        this.maxReceiverCount = maxReceiverCount;
    }

    public interface ReceiverChangedListener {

        void onReceiverChanged(boolean addedTwice, boolean tooMuchReceivers);
    }
}
