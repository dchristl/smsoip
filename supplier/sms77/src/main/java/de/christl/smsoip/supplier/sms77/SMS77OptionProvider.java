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

import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import de.christl.smsoip.option.OptionProvider;

/**
 *
 */
public class SMS77OptionProvider extends OptionProvider {
    private static final String PROVIDER_NAME = "SMS77";
    private EditText senderText;

    public SMS77OptionProvider() {
        super(PROVIDER_NAME);
    }

    @Override
    public Drawable getIconDrawable() {
        return getDrawble(R.drawable.icon);
    }

    @Override
    public void getFreeLayout(LinearLayout freeLayout) {
        XmlResourceParser freeLayoutRes = getLayoutResourceByResourceId(R.layout.freelayout);
        LayoutInflater.from(freeLayout.getContext()).inflate(freeLayoutRes, freeLayout);
        resolveChildren(freeLayout);
//        buildLayoutsContent();
        freeLayout.setVisibility(View.VISIBLE);
    }


    private void setInputFiltersForEditText() {
        int length = 16;
        InputFilter maxLengthFilter = new InputFilter.LengthFilter(length);//max 16 chars allowed
        InputFilter specialCharsFilter = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i))) {//only numbers or charcters allowed
                        return "";
                    }
                }
                return null;
            }
        };
        senderText.setFilters(new InputFilter[]{maxLengthFilter, specialCharsFilter});
        senderText.setText(getSenderFromOptions());
    }

    private CharSequence getSenderFromOptions() {
        return "";
    }

    /**
     * childs have to be resolved by its tree in structure, findViewById seems not to work on every device
     *
     * @param freeLayout
     */
    private void resolveChildren(ViewGroup freeLayout) {
        ViewGroup parentTableRow = (ViewGroup) ((ViewGroup) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(1)).getChildAt(0);
        senderText = (EditText) parentTableRow.getChildAt(0);
        setInputFiltersForEditText();
        //set the heading
        ((TextView) ((ViewGroup) freeLayout.getChildAt(0)).getChildAt(0)).setText(getTextByResourceId(R.string.sender));
    }
}
