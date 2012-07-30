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

package de.christl.smsoip.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.*;
import android.widget.CheckBox;
import android.widget.TableRow;
import android.widget.TextView;
import de.christl.smsoip.R;

/**
 * The view for number and name in contacts table
 */
public class ContactsTextView extends TextView {
    public ContactsTextView(Context context, final CheckBox cb) {
        super(context);
        setGravity(Gravity.CENTER);
        setTextSize(16);
        setPadding(5, 1, 5, 1);
        setBackgroundResource(R.drawable.tablecellcontent);
        setLayoutParams(new TableRow.LayoutParams(
                TableRow.LayoutParams.FILL_PARENT,
                TableRow.LayoutParams.FILL_PARENT));
        setWidth(getWidthBySize());
        setLines(1);
        setFocusable(true);
        setEllipsize(TextUtils.TruncateAt.END);
        View.OnClickListener checkBoxChangeListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cb.setChecked(!cb.isChecked());
            }
        };
        setClickable(true);
        setOnClickListener(checkBoxChangeListener);
    }


    private int getWidthBySize() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        return (int) (0.35 * display.getWidth());
    }
}
