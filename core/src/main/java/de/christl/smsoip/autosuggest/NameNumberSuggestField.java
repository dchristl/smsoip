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
import android.util.AttributeSet;
import android.widget.MultiAutoCompleteTextView;
import de.christl.smsoip.activities.Receiver;
import de.christl.smsoip.database.DatabaseHandler;

import java.util.List;

/**
 * Filed for finding numbers and names by userinputs
 */
public class NameNumberSuggestField extends MultiAutoCompleteTextView {


    private final CommaTokenizer tokenizer = new CommaTokenizer();

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

    }

    @Override
    protected CharSequence convertSelectionToString(Object selectedItem) {
        NameNumberEntry currentEntry = (NameNumberEntry) selectedItem;
        return currentEntry.getFieldRepresantation();
    }


    public void append(Receiver receiver) {
        String receiverNumber = receiver.getReceiverNumber();
        NameNumberEntry entry = new NameNumberEntry(receiver.getName(), receiverNumber, receiver.getNumberTypeMap().get(receiverNumber));
        setText(getText().toString() + convertSelectionToString(entry));
        performValidation();
    }

}
