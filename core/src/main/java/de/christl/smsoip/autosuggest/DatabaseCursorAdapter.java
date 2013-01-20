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
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.widget.CursorAdapter;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import de.christl.smsoip.R;
import de.christl.smsoip.database.DatabaseHandler;

/**
 * Adapter for numbersuggestfiled to get directly to database
 */
public class DatabaseCursorAdapter extends CursorAdapter {


    private Context context;

    public DatabaseCursorAdapter(Context context, Cursor c) {
        super(context, c, true);
        this.context = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        parent = (ViewGroup) inflater.inflate(R.layout.namenumbersuggestitem, null);
        return parent;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        int numberCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        String number = cursor.getString(numberCol);
        TextView numberView = (TextView) view.findViewById(R.id.number);
        numberView.setText(NumberUtils.fixNumber(number));
        int nameCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
        int typeCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE);
        String name = cursor.getString(nameCol) + " (" + DatabaseHandler.translateTypeToString(context, cursor.getInt(typeCol)) + ")";
        TextView nameView = (TextView) view.findViewById(R.id.nameType);
        nameView.setText(name);
        TextView nameType = (TextView) view.findViewById(R.id.nameType);
        SpannableString spanString = new SpannableString(name);
        spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
        nameType.setText(spanString);

    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        return DatabaseHandler.getDBCursor(context, constraint);
    }
}
